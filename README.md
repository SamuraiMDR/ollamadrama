## OLLAMADRAMA

Ollamadrama is a simple Java wrapper library for working with Ollama endpoints

| ![alt text](https://github.com/SamuraiMDR/ollamadrama/blob/master/media/ollamadrama.gif?raw=true) |
| :--: |

### How to use

Launch multiple Ollama instances on your local C-network to make them available. Expose TCP port 11434 beyond localhost by using

   ```
	OLLAMA_HOST=<your ip>:11434 ollama serve
   ```

Make sure to use at least 'IP filtering' to only allow access from your application IP-address. See more under chapter "Security". If you are running your application on the same host as your Ollama endpoint it should work by just launching Ollama with default settings (binds to localhost). 

Add ollamadrama support in your application by specifying the LLM models you want available on all Ollama nodes. This will sweep your local network looking for Ollama servers to connect to and automatically attempt to perform a pull if the models are missing. Make sure you have space available for all the models you specify. 

   ```
   OllamaService.getInstance("openchat:7b,dolphin-mistral:7b");
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
Currently 'openchat:7b', 'gemma2:9b' and 'qwen2.5:7b' are the default models used if you omit an argument to getInstance(), ie

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

STRICT [openchat:7b::7c820ae7-dc37-4ddc-b0e0-af70f94a0a58]:
-----------------
[100%] 110
motivation: The user asked for a specific TCP port number used by the POP3 protocol.
assumptions_made: The user is asking about the TCP port number for the POP3 protocol.

STRICT [gemma2:9b::d16938f1-027b-451a-986c-f5fdbb15fdf3]:
-----------------
[95%] 110
motivation: This is common knowledge about POP3.
assumptions_made: You are asking for a standard TCP port used by the POP3 protocol.

STRICT [qwen2.5:7b::e174ffba-447b-4121-809d-e1a4d4560c7c]:
-----------------
[95%] 110
motivation: POP3 typically uses port 110 for TCP communication.
assumptions_made: No additional information was provided to verify this.

uniq responses: 1
response #1: 110
 - openchat:7b::7c820ae7-dc37-4ddc-b0e0-af70f94a0a58
 - gemma2:9b::d16938f1-027b-451a-986c-f5fdbb15fdf3
 - qwen2.5:7b::e174ffba-447b-4121-809d-e1a4d4560c7c

uniq confident responses: 1
response #1: 110
 - openchat:7b::7c820ae7-dc37-4ddc-b0e0-af70f94a0a58
 - gemma2:9b::d16938f1-027b-451a-986c-f5fdbb15fdf3
 - qwen2.5:7b::e174ffba-447b-4121-809d-e1a4d4560c7c
   ```	

You can also run a OllamaUtils.collectiveFullEnsembleRun() where two voting rounds are made. The confident responses from the first round are shared with all LLMs for the second round. Note that this 'LLM crowdsourcing' approach works quite well but introduces significant delays as each LLM is queried in sequence. 

### Requirements

Make sure that your Ollama nodes has enough VRAM for the selected models. Typically the 70B models require 40+ GB of VRAM. These larger models are specfically marked with 'L' in Globals and will not be automatically downloaded by OllamaDrama. This is done to avoid downloading multiple 40GB models as part of your first testrun. If they are specificed in OllamaDrama and available on your Ollama endpoint they will be used. 

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
	  <version>0.0.<latest-version></version>
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

### OpenAI comparison

If you define an OpenAI API key and set the 'use_openai' configuration variable to true you will be able to select from OpenAIs models as well. 

### Increasing the context window

Although Ollama supports setting num_ctx over the API, the setting does not override the default (2048) since it needs to be applied to the model before being loaded. Further details and discussions can be found here:
https://www.reddit.com/r/ollama/comments/1e4hklk/how_does_num_predict_and_num_ctx_work/

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
- chatGPT4: https://chatgpt.com/ (code)
- Ollama4j: https://amithkoujalgi.github.io/ollama4j (lib)
- Ollama: https://ollama.com (models/service)
