## OLLAMADRAMA

Ollamadrama is a simple Java client application wrapper, built on a wrapper (ollama4j) for a an LLM wrapper (ollama).  

| ![alt text](https://github.com/SamuraiMDR/ollamadrama/blob/master/media/ollamadrama.gif?raw=true) |
| :--: |

### Why use it?

Ollamadrama makes it easy to

* obtain insights into an LLM’s level of certainty in its response, along with the underlying reasoning
* compare model performance on a specific topic using 'scorecards'
* perform ensamble voting, ie require '5 out of 9 LLMs needs to give the same answer'
* recover and handle ollama endpoint connectivity failures

### How to use it

Launch multiple Ollama instances on your local C-network to make them available. Expose TCP port 11434 beyond localhost by using

   ```
 OLLAMA_HOST=<your ip>:11434 ollama serve
   ```

Make sure to use at least 'IP filtering' to only allow access from your application IP-address. See more under chapter "Security". If you are running your application on the same host as your Ollama endpoint you can just launch Ollama with default settings (binds to localhost).

Add ollamadrama support in your application by specifying the LLM models you want available on all Ollama nodes. This will sweep your local network looking for Ollama servers to connect to (and automatically attempt to perform a pull if the models are missing). Make sure you have disk space available for all the models you specify, along with GPU VRAM to host the models.

The most commonly used local models at 7-9b requires a 8 GB GPU card, which allows the '_M' tiered models to run:

* ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M
* ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M
* ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M
* ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M

Note that the tier list does not reflect the true capabilities of the model, but rather the ability to follow instructions and perform well in our selected set of generic 'model scorecards'.

The phi:14b model needs just beyond 8GB, which places is in the '_L' tiered group. A cost-effective way to run '_L' models is using a single 'Nvidia 4060 Ti 16 GB' card. The '_XL' models are suited for setups with 48 GB VRAM (such as 2x4090 cards), and then this style of grouping continues with '_XXL', 'XXXL' etc.

To use 'openchat:7b':

   ```
   OllamaService.getInstance("openchat:7b");
   ```

Launch a session using custom settings or choose from the 'Strict', 'Creative' or 'Default' templates:

   ```
   OllamaSession s1 = OllamaService.getStrictProtocolSession("openchat:7b");
   if (s1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama protocol session [" 
   + MODEL_NAME + "] is operational");

   OllamaSession s2 = OllamaService.getStrictSession("openchat:7b");
   if (s2.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" 
   + MODEL_NAME + "] is operational");
   
   OllamaSession s3 = OllamaService.getCreativeSession("openchat:7b");
   if (s3.getOllamaAPI().ping()) System.out.println(" - CREATIVE ollama session [" 
   + MODEL_NAME + "] is operational");
   
   OllamaSession s4 = OllamaService.getDefaultSession("openchat:7b");
   if (s4.getOllamaAPI().ping()) System.out.println(" - DEFAULT ollama session [" 
   + MODEL_NAME + "] is operational");
   ```

Feed the sessions with facts/statements:

   ```
   s1.provideChatStatement("My name is Thor");
   ```
 
Then ask questions where the replies will be structured with 'probability', 'motivation' and 'assumptions_made'.

   ```
   SingleStringResponse ssr1 = s1.askStrictChatQuestion("What is my name?");
   ssr1.print();
   ```

   ```
 response         : Thor
 probability      : 100%
 motivation       : The input data provided your name as Thor, so the response is based on that information.
 assumptions_made : 
   ```

Note that not all models can be instructed to adopt the defined JSON-based interaction protocol. The models which are known to work properly are listed in the Global class.
Currently 'marco-o1:7b', 'gemma2:9b' and 'qwen2.5:7b' (ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_M) are the default models used if you omit an argument to getInstance(), ie

   ```
   OllamaService.getInstance();
   ```

### Ensemble voting

If you want an ensemble vote using the default models above, run:

   ```
  OllamaUtils.ensembleRun("What TCP port does the POP3 protocol typically use? Answer with only a number.");
   ```

which results in the following console output:

   ```
What TCP port does the POP3 protocol typically use? Answer with only a number.

STRICT [qwen2.5:7b::262b41c9-9e5a-49f5-bf5b-062f78da30a7]:
-----------------
[30%] 110
motivation: Commonly used port for POP3 is 110.
assumptions_made: No additional information provided.

STRICT [gemma2:9b::9531e75a-5e69-41e2-8d4f-33574a855606]:
-----------------
[95%] 110
motivation: This is common knowledge about POP3.
assumptions_made: User is asking about the standard port for POP3

STRICT [marco-o1:7b::ae397a4a-58a2-4ba7-8727-ef06e6d970da]:
-----------------
[95%] 110
motivation: POP3 typically uses port 110 for communication.
assumptions_made: Assumed that the user is referring to standard POP3 protocol without considering secure variants.

uniq responses: 1
response #1: 110
 - qwen2.5:7b::262b41c9-9e5a-49f5-bf5b-062f78da30a7
 - gemma2:9b::9531e75a-5e69-41e2-8d4f-33574a855606
 - marco-o1:7b::ae397a4a-58a2-4ba7-8727-ef06e6d970da

uniq confident responses: 1
response #1: 110
 - qwen2.5:7b::262b41c9-9e5a-49f5-bf5b-062f78da30a7
 - gemma2:9b::9531e75a-5e69-41e2-8d4f-33574a855606
 - marco-o1:7b::ae397a4a-58a2-4ba7-8727-ef06e6d970da
   ``` 

In the above result qwen2.5:7b gets the correct answer and seems confident, but only gives a 30% probability score. We attempt to compensate for this by having a 'per model threshold' which allows you to handle models which are 'too humble' or 'too optimistic' in their answers (MODEL_PROBABILITY_THRESHOLDS). 

You can also run a OllamaUtils.collectiveFullEnsembleRun() where two voting rounds are made. The confident responses from the first round are shared with all LLMs for the second round. Note that this 'LLM crowdsourcing' approach works quite well but introduces significant delays as each LLM is queried in sequence.

Another example where Ollama and OpenAI models are used together:

   ```
 SingleStringEnsembleResponse sser = OllamaUtils.collectiveFullEnsembleRun(
   "What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
   Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
   Globals.MODEL_NAMES_OPENAI_ALL,
   ollama_settings, true, true);
 sser.printEnsemble();

      ...

uniq confident responses: 9
response #1: NIPPON TELEGRAPH AND TELEPHONE
 - openhermes:7b-mistral-v2.5-q4_0::85d7af98-7238-4996-81c9-49320b46e907
response #2: NTT CORPORATION
 - gpt-4-turbo::5bf417f7-123a-492f-b40f-2223a92246ea
response #3: NTT LTD.
 - gpt-4::6ef810ed-5752-4a9a-beec-6cffecaaf566
response #4: NTT LIMITED
 - exaone3.5:32b::1949f946-b1aa-434c-9365-2ef677290615
 - nemotron:70b::94da5c79-3c1b-4444-b75e-e076e9b07ba5
response #5: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
 - dolphin-mistral:7b::fdafeb01-c606-40ce-b789-e20a245276dd
response #6: NTT COMMUNICATIONS CORPORATION
 - aya-expanse:32b::5d12e34c-1482-4fe0-a53e-8fdbb3c3bd35
response #7: NIPPON TELEGRAPH AND TELEPHONE EXCHANGE
 - openchat:7b::1164cfdc-2d07-444e-b4bf-a61ffe6bbf8d
response #8: NTT
 - llama3.1:8b::abe5564e-8aaa-4fec-8360-9ec031f770b4
 - gpt-3.5-turbo::9e27b8b1-5b2f-4024-bc48-606d41348304
 - gpt-4o::d1df788e-9dd2-49bc-82fb-901baf7cbcb9
 - sailor2:20b::5497b38b-32e0-47bb-bd2c-fc0fe3cee114
 - qwen2.5:72b::44e81a3b-cb95-4b10-93b7-07691f87feec
 - athene-v2:72b::bc7bd144-6c86-4cdc-93c2-c688191b9960
 - wizard-vicuna-uncensored:30b::dec9b7cc-f533-4612-811f-6f706b2011f7
 - olmo2:7b::5c8cdfe0-d3f5-46c1-b6c1-e47a468d9537
 - marco-o1:7b::da07e8a7-4e0b-4406-9f5c-50f6b2c1e8ec
 - tulu3:70b::18728afb-816d-4fcd-b3b1-6db52ae88363
 - llama3.1:70b::303edbcb-afaa-4adc-8964-2aff9a5e6a2a
 - gemma2:9b::5725ab79-f4d8-4e13-8011-02d5062f029e
 - qwen2.5:7b::25f859a8-5f34-42e2-b4e7-bf55447ed9d0
 - llama3.3:70b::3581d10d-5ce1-4d4e-9f42-371be619b5c3
 - mistral:7b::d2acab90-c9e8-4bf5-b563-e6fdc56dd5ba
 - tulu3:8b::28a07d21-5ee3-4b82-b9ac-8ea65d3a3517
 - gemma2:27b::3fecdf08-0c8c-439c-8252-a7dd4f78d875
 - aya-expanse:8b::f9a84f36-8f2c-4e85-a78b-862c0a5ca443
response #9: NTT GLOBAL
 - granite3.1-dense:8b::795bf40c-6487-4d4c-8044-10e4d7d59e82

   ```

The winning (most common) 'NTT' result can be retrieved by simply calling

   ```
   sser1.getBestResponse();
   ```

### Requirements

* Java 17+
* Maven
* Ollama endpoint
* Enough GPU VRAM for the selected models

### Building the library

   ```
   git clone https://github.com/SamuraiMDR/ollamadrama.git
   mvn clean package install
   ```

### Using the library

Maven:

   ```
 <dependency>
   <groupId>ntt.security</groupId>
   <artifactId>ollamadrama</artifactId>
   <version>0.<latest-version-here></version>
 </dependency>
   ```

### Security

The Ollama server does not yet support 'HTTPS' or 'Basic Authentication', which means IP Filtering is the most effective way of providing an additional layer of security. There are third party Ollama proxies available which provide 'Basic Authentication'. With this setup you can launch ollamadrama using a custom Settings object:

   ```s
 Settings s = new Settings();
 s.setOllama_username("username");
 s.setOllama_password("password");
  
 // Launch service 
 OllamaService.getInstance(s);
   ```

### Remote Ollama nodes

If you have remote Ollama servers you can add them as satellites in the following way:

   ```
 Settings s = new Settings();
 OllamaCustomEndpoint ep = new OllamaCustomEndpoint("http://some.ollama.endpoint.com:11434", "somuser", "somepassword");
 s.addOllamaCustomEndpoint(ep);
  
 // Launch service 
 OllamaService.getInstance(s);
   ```

You can also do this by setting the environment variable OLLAMADRAMA_CONFIG to a JSON in the format:

   ```
{"ollama_models":"openchat:latest,gemma2:latest,","ollama_password":"","ollama_port":11434,"ollama_timeout":60,"ollama_username":"","openaikey":"","release":"Lili","satellites":[{"ollama_password":"","ollama_url":"http://some.ollama.endpoint.com:11434","ollama_username":""}],"threadPoolCount":20,"ollama_scan":true,"use_openai":false}
   ```

   ```
 OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
 ollama_settings.sanityCheck();

 // Launch singleton 
 OllamaService.getInstance(selected_model, ollama_settings);
   ```

### Scanning for ollama nodes

If you want/need to disable the Ollama autodiscovery (TCP 11434 local network scanning) you can set the 'ollama_scan' configuration variable to false.

### OpenAI support

If you define an OpenAI API key ('openaikey') and set the 'use_openai' configuration variable to true you will be able to select from OpenAIs models as well.

### Increasing the context window

Although Ollama supports setting num_ctx over the API, the setting does not override the default (2048) since it needs to be applied to the model before being loaded. Further details and discussions can be found here:
<https://www.reddit.com/r/ollama/comments/1e4hklk/how_does_num_predict_and_num_ctx_work/>

To create 8192 versions of the Ollama models which support this larger context window, go to the scripts/maxctx folder and run

   ```
create_maxctx_model_versions.sh
   ```

If you run 'ollama list' after this  command you will find the '-maxctx' versions listed:

* llama3.1.70b-maxctx
* nemotron-maxctx
* llama3.3-maxctx
* gemma2-maxctx

### Credits

- chatGPT4: <https://chatgpt.com/> (code)
* Ollama4j: <https://amithkoujalgi.github.io/ollama4j> (lib)
* Ollama: <https://ollama.com> (models/service)
