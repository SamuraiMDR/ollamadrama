## OLLAMADRAMA

Ollamadrama is a simple Java client application wrapper, built on a wrapper (ollama4j) for an LLM wrapper (ollama).  

| ![alt text](https://github.com/SamuraiMDR/ollamadrama/blob/master/media/ollamadrama.gif?raw=true) |
| :--: |

### Why use it?

Ollamadrama makes it easy to:

* Obtain insights into an LLM's level of certainty in its response, along with the underlying reasoning
* Compare model performance on a specific topic using 'scorecards'
* Perform ensemble voting, i.e., require '5 out of 9 LLMs to give the same answer'
* Recover from Ollama endpoint connectivity failures
* Allow controlled tool calls using Model Context Protocol (MCP)

### End-to-end example using MCP tool calls

Setup the MCP fetch() server:
   
```bash
mkdir mcp-server-fetch
cd mcp-server-fetch
```

Create a Dockerfile with the following content:

```dockerfile
FROM python:3.11-slim
RUN apt-get update && apt-get install -y gcc g++ git curl && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir mcp-server-fetch mcp-proxy
EXPOSE 8080
ENTRYPOINT ["mcp-proxy", "--host=0.0.0.0", "--port=8080", "--"]
CMD ["python", "-m", "mcp_server_fetch"]
```

Build and run the container:

```bash
docker build -t mcp-server-fetch .
docker run --network=host -p 8080:8080 mcp-server-fetch
```

You now have an MCP endpoint exposed to the network which is able to retrieve website content as a tool call. **Security Warning**: Make sure that only trusted sources are able to access this MCP server as there are no authentication requirements to make a call. 

Clone the ollamadrama repo into your IDE of choice, then run `simple_HTTP_MCP_Tool_Test()` (src/test/java/ntt/security/ollamadrama/mcp) as a unit test. With a 16GB GPU card this unit test works well with models such as `qwen3:14b` or `cogito:14b`. The first lines of code simply launch OllamaDrama, find an ollama endpoint and make sure that the selected model is available and working:

```java
OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
settings.setOllama_models("qwen3:14b");
settings.sanityCheck();
OllamaService.getInstance(settings);
```

Next we use OllamaDrama to ask if a website is accessible, and make sure to make any discovered MCP tools available to the LLM: 

```java
boolean make_tools_available = true;
SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion("Is the site https://www.ntt.com accessible?", make_tools_available);
ssr1.print();
```

When running this unit test you will see output similar to the following: 

```
... log_level="INFO" wireOllamas()
... log_level="INFO" Owned ips: [127.0.0.1, 192.168.100.12]
... log_level="INFO" Looking for ollama servers .. [127.0.0, 192.168.100]
... log_level="INFO" activeHosts for ollama port 11434: [http://127.0.0.1:11434]
... log_level="INFO" The ollama URL http://127.0.0.1:11434 replied with a proper ping
... log_level="INFO" Making sure we have the models we need on http://127.0.0.1:11434 ..
... log_level="INFO" Performing simple sanity check on Ollama model qwen3:14b on http://127.0.0.1:11434
... log_level="INFO" Verified ollama URL (with a functional instance of our preferred model (qwen3:14b) found on: http://127.0.0.1:11434
```

Here the local network is scanned for ollama ports, and 1 endpoint is found. Next the local network is scanned for the defined MCP ports, and 3 tools are indexed by ollamadrama:

```
log_level="INFO" wireMCPs()
... log_level="INFO" activeHosts for MCP ports [8080, 9000]: [192.168.100.203::9000, 192.168.100.95::8080, 192.168.100.95::9000]
... log_level="INFO" Found MCP tool fetch
... log_level="INFO" Found MCP tool get_current_time
... log_level="INFO" Found MCP tool convert_time
```

The ollama session towards the qwen3:14b model is launched, and all detected tools are listed automatically as part of the prompt since we set `make_tools_available` to true:

```
- STRICT ollama session [qwen3:14b] is operational
Is the site https://www.ntt.com accessible?

MCP TOOLS AVAILABLE:

---
Tool: fetch
Description: Fetches a URL from the internet and optionally extracts its contents as markdown
Inputs:
- url (string) [required]: URL to fetch
- max_length (integer): Maximum number of characters to return.
- start_index (integer): On return output starting at this character index, useful if a previous fetch was truncated and more context is required.
- raw (boolean): Get the actual HTML content of the requested page, without simplification.
Example usage: fetch(url="...", max_length=5000, start_index=0, raw=false)

---
Tool: get_current_time
Description: Get current time in a specific timezones
Inputs:
- timezone (string) [required]: IANA timezone name (e.g., 'America/New_York', 'Europe/London'). Use 'Etc/UTC' as local timezone if no timezone provided by the user.
Example usage: get_current_time(timezone="...")

---
Tool: convert_time
Description: Convert time between timezones
Inputs:
- source_timezone (string) [required]: Source IANA timezone name (e.g., 'America/New_York', 'Europe/London'). Use 'Etc/UTC' as local timezone if no source timezone provided by the user.
- time (string) [required]: Time to convert in 24-hour format (HH:MM)
- target_timezone (string) [required]: Target IANA timezone name (e.g., 'Asia/Tokyo', 'America/San_Francisco'). Use 'Etc/UTC' as local timezone if no target timezone provided by the user.
Example usage: convert_time(source_timezone="...", time="...", target_timezone="...")
```

The LLM replies with 'TOOLCALL' and suggests running the fetch() tool with the site url as argument. Since we have not allowed tools to be blindly trusted, we need to confirm that the tool call can take place:

```
... log_level="INFO" The agent is requesting to run the tool call fetch(url="https://www.ntt.com",max_length=1000,start_index=0,raw=false), press Y to allow and N to abort.
Proceed? [y/n]: y
```

The suggested tool call is then executed, the result appended to the prompt and then sent back to the LLM. 

```
Response from running tool_call fetch(url="https://www.ntt.com",max_length=1000,start_index=0,raw=false):

* content type     : text
* content text     :
----------------------------
Contents of https://www.ntt.com/:
NTTドコモビジネス(旧:NTTコミュニケーションズ) オフィシャルサイト

2025年7月より、NTTコミュニケーションズは NTTドコモビジネスに社名を変更しました

* オンラインショップ 別ウィンドウで開きます。
* + ご契約中のお客さま

   - ×閉じる
   - サービス別サポート情報 (サポートサイト)
   - ご契約中サービスの一元管理 (NTTドコモビジネス ビジネスポータル)
   - NTTドコモビジネス Web明細 (ビリングステーション)
   - NTTドコモ(携帯回線) 料金分析 (ご利用料金管理サービス)
   - NTTドコモ(携帯回線) Web明細 (My docomo)
+ 個人のお客さま
...
```

The final reply back from the LLM is a clear 'Yes' along with underlying reasoning:

```
response         : Yes
probability      : 95%
motivation       : The fetch tool successfully retrieved partial content from the site, indicating it is accessible.
assumptions_made : The truncation error does not affect the accessibility status, only the completeness of the content retrieved.
tool_calls       : 
```

**Note**: MCP tool support has recently been added and may be unstable for a few releases. 

### How to use OllamaDrama (LOCALLY)

Clone this repo into your IDE of choice, then run `simpleStrawberryRCount_OllamaModels_M()` (src/test/java/ntt/security/ollamadrama/modelscorecards) as a unit test. This will automatically find your local Ollama instance, download all medium-sized LLMs and produce a 'Strawberry scorecard' for each model. This assumes you have 8GB GPU VRAM and disk storage available locally. 

### How to use it (NETWORK)

Launch multiple Ollama instances on your local C-network to make them available. Expose TCP port 11434 beyond localhost by using:

```bash
OLLAMA_HOST=<your ip>:11434 ollama serve
```

**Security Warning**: Make sure to use at least 'IP filtering' to only allow access from your application IP-address. See more under the "Security" section. If you are running your application on the same host as your Ollama endpoint you can just launch Ollama with default settings (binds to localhost).

Add ollamadrama support in your application by specifying the LLM models you want available on all Ollama nodes. This will sweep your local network looking for Ollama servers to connect to (and automatically attempt to perform a pull if the models are missing). Make sure you have disk space available for all the models you specify, along with GPU VRAM to host the models.

The most commonly used local models at 7-9b require an 8 GB GPU card, which allows the '_M' tiered models to run:

* `ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M`
* `ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M`
* `ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M`
* `ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M`

**Note**: The tier list does not reflect the true capabilities of the model, but rather the ability to follow instructions and perform well in our selected set of generic 'model scorecards'.

The `phi:14b` model needs just beyond 8GB, which places it in the '_L' tiered group. A cost-effective way to run '_L' models is using a single 'Nvidia 4060/5060 Ti 16 GB' card. The '_XL' models are suited for setups with 48 GB VRAM (such as 2x4090 cards), and this style of grouping continues with '_XXL', '_S' etc.

To use 'openchat:7b':

```java
OllamaService.getInstance("openchat:7b");
```

Launch a session using custom settings or choose from the 'Strict', 'Creative' or 'Default' templates:

```java
OllamaSession s1 = OllamaService.getStrictProtocolSession("openchat:7b");
if (s1.getOllamaAPI().ping()) {
    System.out.println(" - STRICT ollama protocol session [" + MODEL_NAME + "] is operational");
}

OllamaSession s2 = OllamaService.getStrictSession("openchat:7b");
if (s2.getOllamaAPI().ping()) {
    System.out.println(" - STRICT ollama session [" + MODEL_NAME + "] is operational");
}

OllamaSession s3 = OllamaService.getCreativeSession("openchat:7b");
if (s3.getOllamaAPI().ping()) {
    System.out.println(" - CREATIVE ollama session [" + MODEL_NAME + "] is operational");
}

OllamaSession s4 = OllamaService.getDefaultSession("openchat:7b");
if (s4.getOllamaAPI().ping()) {
    System.out.println(" - DEFAULT ollama session [" + MODEL_NAME + "] is operational");
}
```

Feed the sessions with facts/statements:

```java
s1.provideChatStatement("My name is Thor");
```
 
Then ask questions where the replies will be structured with 'probability', 'motivation' and 'assumptions_made':

```java
SingleStringQuestionResponse ssqr1 = s1.askStrictChatQuestion("What is my name?");
ssqr1.print();
```

Output:

```
response         : Thor
probability      : 100%
motivation       : The input data provided your name as Thor, so the response is based on that information.
assumptions_made : 
```

**Note**: Not all models can be instructed to adopt the defined JSON-based interaction protocol. The models which are known to work properly are listed in the `Globals` class.

Currently `marco-o1:7b`, `gemma2:9b` and `qwen2.5:7b` (`ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_M`) are the default models used if you omit an argument to `getInstance()`, i.e.:

```java
OllamaService.getInstance();
```

### Ensemble voting

If you want an ensemble vote using the default models above, run:

```java
OllamaUtils.strictEnsembleRun("What TCP port does the POP3 protocol typically use? Answer with only a number.");
```

Which results in the following console output:

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

In the above result `qwen2.5:7b` gets the correct answer and seems confident, but only gives a 30% probability score. We attempt to compensate for this by having a 'per model threshold' which allows you to handle models which are 'too humble' or 'too optimistic' in their answers (`MODEL_PROBABILITY_THRESHOLDS`). 

You can also run `OllamaUtils.collectiveFullEnsembleRun()` where two voting rounds are made. The confident responses from the first round are shared with all LLMs for the second round. **Note**: This 'LLM crowdsourcing' approach works quite well but introduces significant delays as each LLM is queried in sequence.

Another example where Ollama and OpenAI models are used together:

```java
SingleStringEnsembleResponse sser = OllamaUtils.collectiveFullEnsembleRun(
    "What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
    Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
    Globals.MODEL_NAMES_OPENAI_ALL,
    ollama_settings, 
    true, 
    true
);
sser.printEnsemble();
```

Output:

```
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

The winning (most common) 'NTT' result can be retrieved by simply calling:

```java
sser1.getBestResponse();
```

### Voice support

Ollamadrama includes optional voice support from ElevenLabs using the `net.andrewcpu` package. For a simple test, add the elevenlabs API key (`elevenlabs_apikey`) and define two voices (`elevenlabs_voice1`, `elevenlabs_voice2`). Then run the unit tests which match your LLM size support in `src/main/java/ntt/security/ollamadrama/llmdebate`. This will give you a narrated contest on the selected topic (by default set to 'France'). 

### Requirements

* Java 17+
* Maven
* Ollama endpoint
* Sufficient GPU VRAM for the selected models

### Building the library

```bash
git clone https://github.com/SamuraiMDR/ollamadrama.git
cd ollamadrama
mvn clean package install
```

### Using the library

Maven:

```xml
<dependency>
  <groupId>ntt.security</groupId>
  <artifactId>ollamadrama</artifactId>
  <version>0.<latest-version-here></version>
</dependency>
```

### API Migration Guide (v0.x to v1.0)

Starting with version 1.0, OllamaDrama has adopted snake_case naming conventions for better consistency. All original camelCase methods remain available but are marked as deprecated. You can migrate gradually:

**Old (Deprecated but still works):**
```java
OllamaService.getStrictProtocolSession("openchat:7b");
OllamaUtils.parseOllamaDramaConfigENV();
session.askChatQuestion("What is my name?");
```

**New (Recommended):**
```java
OllamaService.get_strict_protocol_session("openchat:7b");
OllamaUtils.parse_ollama_drama_config_env();
session.ask_chat_question("What is my name?");
```

All deprecated methods will show warnings in your IDE to help you migrate. Full backward compatibility is maintained.

### Security

The Ollama server does not yet support 'HTTPS' or 'Basic Authentication', which means IP filtering is the most effective way of providing an additional layer of security. There are third-party Ollama proxies available which provide 'Basic Authentication'. With this setup you can launch ollamadrama using a custom Settings object:

```java
OllamaDramaSettings s = new OllamaDramaSettings();
s.setOllama_username("username");
s.setOllama_password("password");

// Launch service 
OllamaService.getInstance(s);
```

### Remote Ollama nodes

If you have remote Ollama servers you can add them as satellites in the following way:

```java
OllamaDramaSettings s = new OllamaDramaSettings();
OllamaEndpoint ep = new OllamaEndpoint("http://some.ollama.endpoint.com:11434", "someuser", "somepassword");
s.addSatellite(ep);

// Launch service 
OllamaService.getInstance(s);
```

You can also do this by setting the environment variable `OLLAMADRAMA_CONFIG` to a JSON in the format:

```json
{
  "ollama_models": "openchat:latest,gemma2:latest",
  "ollama_password": "",
  "ollama_port": 11434,
  "ollama_timeout": 60,
  "ollama_username": "",
  "openaikey": "",
  "release": "Lili",
  "satellites": [
    {
      "ollama_password": "",
      "ollama_url": "http://some.ollama.endpoint.com:11434",
      "ollama_username": ""
    }
  ],
  "threadPoolCount": 20,
  "ollama_scan": true,
  "use_openai": false
}
```

Then load it programmatically:

```java
OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
ollama_settings.sanityCheck();

// Launch singleton 
OllamaService.getInstance(selected_model, ollama_settings);
```

### Scanning for ollama nodes

If you want/need to disable the Ollama autodiscovery (TCP 11434 local network scanning) you can set the `ollama_scan` configuration variable to false.

### OpenAI support

If you define an OpenAI API key (`openaikey`) and set the `use_openai` configuration variable to true you will be able to select from OpenAI's models as well.

### Increasing the context window

Ollamadrama keeps an index of the known `n_ctx_train` for each supported model and specifies this context window size when a model instance is created. This makes it easy to compare model performance using the various memory loss tests available at `/src/test/java/ntt/security/ollamadrama/memoryloss`. 

With 128 GB RAM and 2x 4090 GPUs, the test `checkMemoryLengthForAllEnsembleModels_RandomWords_CSV()` resulted in a 20k 3-gram memory for the model `cogito:14b`, while memory limitations kicked in for `llama3.1:70b`, `nemotron:70b`, `cogito:70b`, `tulu3:70b` and `gemma3:12b` at 16k (lower for the rest of the models). 

### Best Practices

1. **Security**: Always use IP filtering when exposing Ollama endpoints on a network
2. **Resource Management**: Monitor GPU VRAM usage when running multiple models
3. **Error Handling**: OllamaDrama includes automatic retry logic for connection failures
4. **Model Selection**: Start with smaller models (7-9B) to test before scaling up
5. **Ensemble Voting**: Use at least 3 models for meaningful consensus
6. **MCP Tools**: Always review and approve tool calls before execution in production

### Troubleshooting

**"No Ollama hosts currently available"**
- Ensure Ollama is running and accessible on port 11434
- Check firewall rules if using network discovery
- Verify `OLLAMA_HOST` is set correctly if not using localhost

**"Unable to pass simple sanity check"**
- Model may not be compatible with strict protocol
- Check if model supports JSON output
- Try a different model from the Tier 1 list

**High memory usage**
- Reduce context window size in settings
- Use smaller models or fewer concurrent sessions
- Monitor with `nvidia-smi` for GPU memory

### Contributing

Contributions are welcome! Please ensure:
- Code follows Java 17+ standards
- New methods use snake_case naming
- Backward compatibility is maintained with deprecated wrappers
- Tests are included for new features

### Credits

* chatGPT4: <https://chatgpt.com/> (code assistance)
* Ollama4j: <https://amithkoujalgi.github.io/ollama4j> (library)
* Ollama: <https://ollama.com> (models/service)
* Model Context Protocol: <https://modelcontextprotocol.io/> (MCP specification)

### License

[Include your license information here]

### Changelog

**v1.0** (Current)
- Adopted snake_case naming convention for all public methods
- Added full backward compatibility with deprecated camelCase methods
- Enhanced thread safety with ReentrantReadWriteLock
- Improved Java 17 features usage
- Better error handling and logging
- Added comprehensive JavaDoc documentation

**v0.x** (Previous)
- Initial release with camelCase naming
- Basic ensemble voting and MCP support