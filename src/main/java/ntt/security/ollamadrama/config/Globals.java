package ntt.security.ollamadrama.config;

import java.util.HashMap;

import io.github.amithkoujalgi.ollama4j.core.utils.Options;
import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;

public class Globals {

	public static String ENSEMBLE_LOOP_STATEMENT = "Here are some replies from other experts:";

	public static String THREAT_TEMPLATE = "You will be punished if you do not comply. No yapping. No emojis or encoded multiline responses.";

	// https://github.com/openai/openai-cookbook/blob/main/articles/techniques_to_improve_reliability.md
	public static String LOGIC_TEMPLATE = "Let's think step by step.";

	// https://josephthacker.com/ai/2024/05/04/introducing-assumptions-made-for-ai-agents.html
	public static String ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS = "You MUST reply with a single JSON formatted string with the keys 'response', 'probability', 'motivation' and 'assumptions_made'. "
			+ "The 'response' key should only include your reply. "
			+ "The 'probability' key will include an integer in the range 0-100 and reflects how certain you are that the response is correct. "
			+ "Make sure the selected probability value is very conservative as you will be punished if you provide incorrect responses."
			+ "Make sure the selected probability value is below 30 if you would benefit from more data to answer the question. "
			+ "Make sure the selected probability value is below 5 if you get insufficient information. "
			+ "Never assume the input data contains typos."
			+ "Never assume the input data contains abbreviations."
			+ "Never use markdown in your reply string. Output only plain text. Do not output markdown."
			+ "If you make uncertain assumptions the selected probability value should be below 5."
			+ "Input with a single or few characters should result in a probability value of 0."
			+ "The 'motivation' key should include a brief description string motivating your response and should motivate the selected probability value. "
			+ "The 'assumptions_made' key should include a brief description string of the assumptions made in your response. "
			+ "The reply string will be pure JSON will start with the character { since its JSON and NOT markdown.";

	public static String ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS = "You MUST reply with with a single JSON formatted string with the keys 'response', 'assumptions_made' and 'explanation'. "
			+ "The 'response' key should only include the string OKIDOKI if you understand what I just said. "
			+ "The 'response' key should only include the string FAILTOUNDERSTAND if you do not understand what I just said. "
			+ "If you do not understand you must describe what is unclear in the 'explanation' key. "
			+ "The 'assumptions_made' key should include a brief description string of the assumptions made. "
			+ "The reply string will be pure JSON will start with the character { since its JSON and NOT markdown.";

	public static String PROMPT_TEMPLATE_STRICT_SIMPLEOUTPUT = "You are a bot of few words and prefer to keep your answers as short as possible, often with just one word."
			+ "You never overestimate your confidence in your replies and make sure to always be honest about your limitations. ";

	public static String PROMPT_TEMPLATE_STRICT_COMPLEXOUTPUT = "You are a bot of few words and prefer to keep your answers as short as possible.";

	public static String PROMPT_TEMPLATE_CREATIVE = "You are a free thinking creative bot and always give descriptive and long winded answers. You NEVER reply to a question with just one word and always justify your answer with an explaination.";

	public static Options createStrictOptionsBuilder() {
		return new OptionsBuilder()
				.setMirostat(2) 		// allows perplexity/uncertainty control
				.setMirostatEta(0.5f) 	// learning rate (default 0.1)
				.setMirostatTau(1.0f) 	// lower = more focused and coherent text (default 5.0)
				.setTemperature(0.0f) 	// higher = more creative (default 0.8)
				.setTopK(0) 			// 0-100, higher = more creative (default 40)
				.setTopP(0.0f)			// 0.0-1.0, higher = more creative (default 0.9)
				.setSeed(42)			// Just make it fixed
				.setNumCtx(8*1024) 		// Size of the context window (default 2048), https://github.com/ollama/ollama/blob/main/docs/faq.md#how-can-i-specify-the-context-window-size
				// num_ctx does not seem to work over api, use https://www.reddit.com/r/ollama/comments/1e4hklk/how_does_num_predict_and_num_ctx_work/
				.build();
	}

	public static Options createDefaultOptionsBuilder() {
		return new OptionsBuilder().build();
	}

	public static Options createCreativeOptionsBuilder() {
		return new OptionsBuilder()
				.setMirostatEta(Float.MAX_VALUE) 	// learning rate (default 0.1)
				.setMirostatTau(Float.MAX_VALUE) 	// lower = more focused and coherent text (default 5.0)
				.setTemperature(1.0f) 				// higher = more creative (default 0.8)
				.setTopK(100) 						// 0-100, higher = more creative (default 40)
				.setTopP(1.0f)						// 0.0-1.0, higher = more creative (default 0.9)
				.setNumCtx(8*1024) 					// Size of the context window (default 2048), https://github.com/ollama/ollama/blob/main/docs/faq.md#how-can-i-specify-the-context-window-size
				// num_ctx does not seem to work over api, use https://www.reddit.com/r/ollama/comments/1e4hklk/how_does_num_predict_and_num_ctx_work/
				.build();
	}

	// models listed here will not be pulled automatically due to GPU requirements
	@SuppressWarnings("serial")
	public static HashMap<String, Boolean> ENSEMBLE_MODEL_NAMES_L = new HashMap<String, Boolean>() {{
		this.put("athene-v2:72b", true);
		this.put("qwen2.5:72b", true);
		this.put("llama3.1:70b", true);
		this.put("llama3.3:70b", true);
		this.put("nemotron:70b", true);
		this.put("llama2:70b", true);
		this.put("tulu3:70b", true);
		this.put("exaone3.5:32b", true);
		this.put("sailor2:20b", true);
	}};

	// T_T
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL = ""
			// tier1
			+ "llama3.1:405b"
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L = ""
			// tier1
			+ "llama3.1:70b,"
			+ "llama3.3:70b,"
			+ "nemotron:70b,"
			+ "qwen2.5:72b,"
			+ "gemma2:27b"
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L_MINIDIVERSE = ""
			// tier1
			+ "llama3.1:70b,"
			+ "qwen2.5:72b,"
			+ "gemma2:27b"
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M_MINIDIVERSE = ""
			// tier1
			+ "openchat:7b,"
			+ "qwen2.5:7b," 
			+ "gemma2:9b"
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M = ""
			// tier1
			+ "openchat:7b,"
			+ "marco-o1:7b,"
			+ "mistral:7b," 
			+ "qwen2.5:7b," 
			+ "olmo2:7b,"
			+ "gemma2:9b"
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L = ""
			// tier2
			+ "sailor2:20b,"
			+ "athene-v2:72b,"
			+ "tulu3:70b,"
			+ "exaone3.5:32b,"
			+ "phi4:14b"
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_L = ""
			// tier3
			+ "olmo2:13b"
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M = ""
			// tier2
			+ "llama3.1:8b"
			+"";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M = ""
			// tier3
			+ "qwen2:7b," 	
			+ "dolphin-mistral:7b,"
			+ "dolphin3:8b,"
			+ "openhermes:7b-mistral-v2.5-q4_0" 
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_S = ""
			// tier1
			+ "llama3.2:3b"
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M = ""
			// tier 4 - eval
			+ "tulu3:8b,"
			+ "granite3.1-dense:8b"
			+"";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_UNCENSORED_M = ""
			+ "llama2-uncensored:7b,"				// fails strict mode, no proper JSON
			+ "wizard-vicuna-uncensored:7b,"		// fails strict mode, no proper JSON
			+ "wizard-vicuna-uncensored:13b,"		// fails strict mode, no proper JSON
			+ "wizardlm-uncensored:13b" 			// fails strict mode, no proper JSON
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_UNCENSORED_L = ""
			+ "wizard-vicuna-uncensored:30b"
			//+ "llama2-uncensored:70b,"				// fails strict mode, no proper JSON
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_GUARDED = ""
			+ "shieldgemma:9b,"
			+ "shieldgemma:27b,"
			+ "llama-guard3:8b"
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP = ""
			// tier1
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","

			// tier2
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP_ML = ""
			// tier1
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","

			// tier2
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M + ","
			
			// uncensored
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_UNCENSORED_L
			+ "";

	public static String MODEL_NAMES_OLLAMA_ALL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M

			// broken uncensored
			// + "wizard-vicuna-uncensored:7b,"		// Fails to produce valid JSON
			// + "llama2-uncensored:7b,"			// fails with empty replies
			// + "wizardlm-uncensored:13b,"			// fails to follow protocol
			// + "wizard-vicuna-uncensored:13b,"	// always 0% prob

			// broken in ollama
			//+ "zephyr:7b," 						// zephyr gives errors over time with multiple ollama reloads, may be related to https://github.com/ollama/ollama/issues/1691
			//+ "opencoder:8b"						// fails on setting system profile
			//+ "command-r7b:7b,"					// recheck
			//+ "aya-expanse:32b,"					// recheck
			
			// broken
			//+ "llama2:7b,"						// fails to reply with FAILTOUNDERSTAND on gibberish input
			//+ "llama2:70b,"						// often fails to provide valid JSON (missing quotes etc)
			//+ "solar-pro:22b"						// fails to follow protocol
			//+ "deepseek-r1:7b"					// fails to follow protocol, does its <think> thing
			//+ "deepseek-r1:70b,"					// fails to follow protocol, does its <think> thing
			//+ "falcon3:10b"						// fails at very basic logic
			//+ "llama3-gradient:8b,"				// fails to follow protocol
			//+ "internlm2:7b,"						// fails at very basic logic
			//+ "mistral-openorca:7b,"				// fails at very basic logic
			//+ "qwq:32b,"							// Unstable
			//+ "llama3:8b,"						// fails protocol at times
			//+ "wizardlm2:13b-fp16," 				// fails to produce clean json
			//+ "gemma:7b," 						// fails to follow protocol
			//+ "starling-lm:7b," 					// fails to follow protocol
			//+ "glm4:9b," 							// fails to follow protocol
			//+ "codegemma:7b,"						// fails to follow protocol
			//+ "vicuna:7b,"						// fails to follow protocol

			+ "";

	public static String MODEL_NAMES_OLLAMA_ALL_UP_TO_L = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_S

			+"";

	public static String MODEL_NAMES_OPENAI_TIER2 = ""
			+ "gpt-4o," 
			+ "gpt-4,"
			+ "";

	public static String MODEL_NAMES_OPENAI_ALL = ""
			// https://openai.com/api/pricing/

			// tier1
			//+ "o1"					// Your organization must qualify for at least usage tier 5 to access 'o1'
			//+ "o1-preview,"			// Your organization must qualify for at least usage tier 5 to access 'o1-preview'
			//+ "o1-mini,"				// Your organization must qualify for at least usage tier 5 to access 'o1-mini'
			//+ "o1-mini-2024-09-12,"	// ...

			// tier2
			+ "gpt-4o," 
			+ "gpt-4,"

			+ "gpt-4o-mini,"
			+ "gpt-4-turbo,"

			// tier3
			+ "gpt-3.5-turbo,"

			+ "";

	public static String MODEL_NAMES_OPENAI_CHEAP = ""
			// https://openai.com/api/pricing/
			+ "gpt-4o-mini,"
			+ "gpt-4o-mini-2024-07-18,"
			+ "";

	// an attempt to balance each models scale of probabilities
	@SuppressWarnings("serial")
	public static HashMap<String,Integer> MODEL_PROBABILITY_THRESHOLDS = new HashMap<String,Integer>() {{

		// ollama llms - humble
		this.put("llama3.1:8b", 14);
		this.put("llama3.1:70b", 14);
		this.put("llama3.2:3b", 14);
		this.put("llama3.3:70b", 14);
		this.put("nemotron:70b", 14);
		this.put("qwen2.5:70b", 14);
		this.put("qwen2.5:7b", 14);
		this.put("sailor2:20b", 14);
		this.put("qwen2.5:72b", 14);
		this.put("gemma2:27b", 14);

		// ollama llms - balanced
		this.put("marco-o1:7b", 44);
		this.put("athene-v2:72b", 44);
		this.put("tulu3:70b", 44);
		this.put("exaone3.5:32b", 44);
		this.put("phi4:14b", 44);
		this.put("openhermes:7b-mistral-v2.5-q4_0", 44);
		this.put("falcon3:10b", 44);
		this.put("olmo2:7b", 49);
		this.put("qwen2:7b", 49);
		this.put("dolphin3:8b", 49);
		this.put("openchat:7b", 49);
		this.put("gemma2:9b", 49);
		this.put("mistral:7b", 49);
		this.put("dolphin-mistral:7b", 49);
		this.put("granite3.1-dense:8b", 49);
		
		// ollama llms - optimistic
		this.put("tulu3:8b", 64);

		// openai llms - humble
		this.put("gpt-4o", 20);
		this.put("gpt-4-turbo", 20);
		this.put("gpt-3.5-turbo", 20);

		// openai llms - balanced
		this.put("gpt-4o-mini", 55);
		this.put("o1-preview", 55);
		this.put("o1-mini", 55);
		this.put("gpt-4", 65);
	}};

}
