package ntt.security.ollamadrama.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import ntt.security.ollamadrama.utils.NumUtils;

public class Globals {

	private static final Logger LOGGER = LoggerFactory.getLogger(Globals.class);

	public static String ENSEMBLE_LOOP_STATEMENT = "Here are some replies from other experts:";

	public static String THREAT_TEMPLATE = "You will be punished if you do not comply. No yapping. No emojis or encoded multiline responses.";

	// https://github.com/openai/openai-cookbook/blob/main/articles/techniques_to_improve_reliability.md
	public static String LOGIC_TEMPLATE = "Let's think step by step.";

	// https://josephthacker.com/ai/2024/05/04/introducing-assumptions-made-for-ai-agents.html
	public static final String ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS = """
		    - You MUST reply with a single JSON formatted string with the keys 'response', 'probability', 'motivation', 'assumptions_made' and 'tool_calls'. 
		    - The 'response' key should only include your reply. 
		    - The 'probability' key will include an integer in the range 0-100 and reflects how certain you are that the response is correct. 
		    - Make sure the selected probability value is conservative as you will be punished if you provide incorrect responses. 
		    - Make sure the selected probability value is below 30 if you would benefit from more data to answer the question. 
		    - Make sure the selected probability value is below 5 if you get insufficient information. 
		    - Never assume the input data contains typos. 
		    - Never assume the input data contains abbreviations. 
		    - Never use markdown in your reply string. Output only plain text. Do not output markdown. 
		    - If you make uncertain assumptions the selected probability value should be below 5. 
		    - Input with a single or few characters should result in a probability value of 0. 
		    - The 'motivation' key should include a brief description string motivating your response and should motivate the selected probability value. 
		    - The 'assumptions_made' key should include a brief description string of the assumptions made in your response. 
		    - The 'tool_calls' key is a string and should include a comma-separated list of tool calls you want the output from. If no tools are provided to you under 'MCP TOOLS AVAILABLE' section you should leave 'tool_calls' blank. 
		    - Every tool listed in 'tool_calls' should be preceded with 'oneshot' or 'continous', indicating how often the tool is called. 
		    - Use 'oneshot' if you only need the tool output once. Use 'continous' if you want fresh tool output as part of all future interactions. 
		    - You MUST only populate the 'tool_calls' key with the name of a known tool name along with all its known required arguments in the suggested format, i.e. 'oneshot tool_name(arg1="val1",arg2="val2", ..)' or 'continous tool_name(arg1="val1",arg2="val2", ..)'. 
		    - You should NEVER guess or assume the availability of tools mentioned in the 'tool_calls'; you must have explicitly been told that a tool is available. 
		    - If you want to call a tool after a pause, respond with 'TOOLCALL_AFTER_PAUSE' and (optionally) populate 'tool_calls' in the same way as for 'TOOLCALL'.
		    - If you have nothing to and want to wait until your next interaction, respond with 'TOOLCALL_AFTER_PAUSE' and populate 'tool_calls'. 
		    - The key 'tool_calls' MUST be populated if the response key is 'TOOLCALL'.
		    - When passing string arguments to tools, use double quotes (") only and NEVER single quotes(').
		    - The key 'probability' key MUST BE 0 if the response key is 'TOOLCALL' or 'TOOLCALL_AFTER_PAUSE'. 
		    - The reply string will be pure JSON and will start with the character { since it’s JSON and NOT markdown.
		    """;

	public static final String ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS = """
		    - You MUST reply with a single JSON formatted string with the keys 'response', 'assumptions_made' and 'explanation'. 
		    - The 'response' key should only include the string 'OKIDOKI' if you understand what I just said. 
		    - The 'response' key should only include the string 'FAILTOUNDERSTAND' if you do not understand what I just said. 
		    - If you do not understand, you must describe what is unclear in the 'explanation' key. 
		    - The 'assumptions_made' key should include a brief description string of the assumptions made. 
		    - The 'tool_calls' key must be populated if the response key is 'TOOLCALL'.
		    - The reply string will be pure JSON and will start with the character { since it’s JSON and NOT markdown.
		    """;

	public static String PROMPT_TEMPLATE_COGITO_DEEPTHINK = "Enable deep thinking subroutine. ";

	public static String PROMPT_TEMPLATE_STRICT_SIMPLEOUTPUT = "You are a bot of few words and prefer to keep your answers as short as possible, often with just one word. "
			+ "You never overestimate your confidence in your replies and make sure to always be honest about your limitations. ";

	public static String PROMPT_TEMPLATE_STRICT_COMPLEXOUTPUT = "You are a bot of few words and prefer to keep your answers as short as possible.";

	public static String PROMPT_TEMPLATE_CREATIVE = "You are a free thinking creative bot and always give descriptive and long winded answers. You NEVER reply to a question with just one word and always justify your answer with an explaination.";

	// ollama 0.9.3 RN: Ollama will now limit context length to what the model was trained against to avoid strange overflow behavior

	public static final Map<String, Integer> n_ctx_defaults = Map.ofEntries(
			Map.entry("wizard-vicuna-uncensored:30b", 2048), // train 2048
			Map.entry("llava:34b", 4096), // train 4096
			Map.entry("llama3.1:70b", 32768), // train 131072
			Map.entry("llama3.3:70b", 32768), // train 131072
			Map.entry("nemotron:70b", 32768), // train 131072
			Map.entry("qwen3:32b", 32768), // train 40960
			Map.entry("huihui_ai/qwen3-abliterated:32b", 32768), // train 40960
			Map.entry("huihui_ai/qwen3-abliterated:14b", 32768), // train 40960
			Map.entry("huihui_ai/qwen3-abliterated:16b", 32768), // train 40960
			Map.entry("huihui_ai/qwen3-abliterated:8b", 32768), // train 40960
			Map.entry("qwen2.5:72b", 32768), // train 32768
			Map.entry("gemma3:27b", 32768), // train 126976 but wont load
			Map.entry("translategemma:27b", 32768),
			Map.entry("cogito:70b", 32768), // train 131072
			Map.entry("athene-v2:72b", 32768), // train 32768
			Map.entry("r1-1776:70b", 32768), // train 131072
			Map.entry("tulu3:70b", 32768), // train 131072
			Map.entry("exaone3.5:32b", 32768), // train 32768
			Map.entry("gemma2:27b", 8192), // train 8192
			Map.entry("aya-expanse:32b", 8192), // train 8192
			Map.entry("sailor2:20b", 32768), // train 32768
			Map.entry("qwen3:14b", 32768), // train 40960
			Map.entry("olmo2:13b", 4096), // train 4096
			Map.entry("cogito:14b", 32768), // train 131072 results in regular delays
			Map.entry("gemma3:12b", 32768), // train 126976 but wont load
			Map.entry("phi4:14b", 16384), // train 16384
			Map.entry("marco-o1:7b", 32768), // train 32768
			Map.entry("qwen3:8b", 32768), // train 40960
			Map.entry("qwen2.5:7b", 32768), // train 32768
			Map.entry("olmo2:7b", 4096), // train 4096
			Map.entry("gemma2:9b", 8192), // train 8192
			Map.entry("openchat:7b", 8192), // train 8192
			Map.entry("dolphin-mistral:7b", 32768), // train 32768
			Map.entry("cogito:8b", 32768), // train 131072
			Map.entry("aya-expanse:8b", 8192), // train 8192
			Map.entry("llama3.1:8b", 32768), // train 131072
			Map.entry("qwen2:7b", 32768), // train 32768
			Map.entry("dolphin3:8b", 32768), // train 131072
			Map.entry("mistral:7b", 32768), // train 32768
			Map.entry("exaone-deep:7.8b", 32768), // train 32768
			Map.entry("openhermes:7b-mistral-v2.5-q4_0", 32768), // train 32768
			Map.entry("command-r7b:7b", 8192), // train 8192
			Map.entry("tulu3:8b", 32768), // train 131072
			Map.entry("granite3.3:8b", 32768), // train 131072
			Map.entry("granite3.1-dense:8b", 32768), // train 131072
			Map.entry("llama3.2:3b", 32768), // train 131072
			Map.entry("qwen3:4b", 32768), // train 40960
			Map.entry("dolphin-llama3:70b", 8192), // train 8192
			Map.entry("magistral:24b", 32768), // train 40000
			Map.entry("gemma3n:e4b", 5555), // train 5555
			Map.entry("mistral-small3.2:24b", 5555), // train 5555
			Map.entry("devstral:24b", 32768), // train 131072
			Map.entry("qwen2.5vl:72b", 32768), // train 32768
			Map.entry("qwen2.5-coder:32b", 32768), // train 131072
			Map.entry("deepcoder:14b", 32768), // train 131072
			Map.entry("opencoder:8b", 32768), // train 131072
			Map.entry("gpt-oss:20b", 32768), // train 131072
			Map.entry("gpt-oss:120b", 32768), // train 131072
			Map.entry("codestral:22b", 32768), // train 131072
			Map.entry("olmo-3:7b", 32768), // train 65536
			Map.entry("olmo-3:32b", 32768), // train 65536
			Map.entry("olmo-3.1:32b", 32768), // train 65536
			Map.entry("qwq:32b", 32768), // train 131072
			Map.entry("nemotron-3-nano:30b", 32768), // train 1048576 (1M)
			Map.entry("glm-4.7-flash:q4_K_M",32768), // train 198k
			Map.entry("glm-4.7-flash:q8_0",32768), // train 198k
			Map.entry("glm-4.7-flash:bf16",32768) // train 198k
			);


	public static Options createStrictOptionsBuilder(String _modelname, Boolean _use_random_seed, int _n_ctx_override) {
		Integer n_ctx = null;
		if (_n_ctx_override == -1) {
			n_ctx = n_ctx_defaults.get(_modelname);
			if (null == n_ctx) {
				LOGGER.warn("No n_ctx_defaults_size for " + _modelname + ", falling back to 2048 for safety reasons");
				n_ctx = 2048;
			}
		} else {
			n_ctx = _n_ctx_override;
		}
		LOGGER.debug("n_ctx used: " + n_ctx);
		if (_use_random_seed)  {
			return new OptionsBuilder()

					// https://github.com/SillyTavern/SillyTavern/issues/4188
					.setMirostat(0)					// 0 = DISABLED
					//.setMirostat(2) 				// allows perplexity/uncertainty control
					//.setMirostatEta(0.5f) 		// learning rate (default 0.1)
					//.setMirostatTau(1.0f) 		// lower = more focused and coherent text (default 5.0)		

					.setTemperature(0.0f) 			// higher = more creative (default 0.8)
					.setTopK(0) 					// 0-100, higher = more creative (default 40)
					.setTopP(0.0f)					// 0.0-1.0, higher = more creative (default 0.9)
					.setNumCtx(n_ctx) 	// Size of the context window (default 2048), https://github.com/ollama/ollama/blob/main/docs/faq.md#how-can-i-specify-the-context-window-size
					.build();
		} else {
			return new OptionsBuilder()

					// https://github.com/SillyTavern/SillyTavern/issues/4188
					.setMirostat(0)					// 0 = DISABLED
					//.setMirostat(2) 				// allows perplexity/uncertainty control
					//.setMirostatEta(0.5f) 		// learning rate (default 0.1)
					//.setMirostatTau(1.0f) 		// lower = more focused and coherent text (default 5.0)

					.setTemperature(0.0f) 			// higher = more creative (default 0.8)
					.setTopK(0) 					// 0-100, higher = more creative (default 40)
					.setTopP(0.0f)					// 0.0-1.0, higher = more creative (default 0.9)
					.setSeed(42)					// Just make it fixed
					.setNumCtx(n_ctx) 	// Size of the context window (default 2048), https://github.com/ollama/ollama/blob/main/docs/faq.md#how-can-i-specify-the-context-window-size
					.build();
		}
	}

	// llama_context: n_ctx_pre_seq (32768) > n_ctx_train (2048) -- possible training context overflow
	// llama_context: n_ctx_per_seq (32768) < n_ctx_train (131072) -- the full capacity of the model will not be utilized

	public static Options createDefaultOptionsBuilder() {
		return new OptionsBuilder().build();
	}

	public static Options createCreativeOptionsBuilder(String _modelname, int _n_ctx_override, int _seed) {
		Integer n_ctx = null;
		if (_n_ctx_override == -1) {
			n_ctx = n_ctx_defaults.get(_modelname);
			if (null == n_ctx) {
				LOGGER.warn("No n_ctx_defaults_size for " + _modelname + ", falling back to 2048 for safety reasons");
				n_ctx = 2048;
			}
		} else {
			n_ctx = _n_ctx_override;
		}
		return new OptionsBuilder()
				.setMirostatEta(Float.MAX_VALUE) 	// learning rate (default 0.1)
				.setMirostatTau(Float.MAX_VALUE) 	// lower = more focused and coherent text (default 5.0)
				.setTemperature(1.0f) 				// higher = more creative (default 0.8)
				.setTopK(100) 						// 0-100, higher = more creative (default 40)
				.setTopP(1.0f)						// 0.0-1.0, higher = more creative (default 0.9)
				.setNumCtx(n_ctx) 					// Size of the context window (default 2048), https://github.com/ollama/ollama/blob/main/docs/faq.md#how-can-i-specify-the-context-window-size
				// num_ctx does not seem to work over api, use https://www.reddit.com/r/ollama/comments/1e4hklk/how_does_num_predict_and_num_ctx_work/
				.setSeed(_seed)
				.build();
	}

	/**
	 * XXXL (<256GB)
	 */
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XXXL = ""
			+ "llama3.1:405b"		// 243 GB
			+ "";
	
	/**
	 * XXL (<64GB)
	 */
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XXL = ""
			+ "gpt-oss:120b,"		// 65 GB
			+ "glm-4.7-flash:bf16"	// 60 GB
			+ "";

	// task specific

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_VISION_XXL = ""
			+ "llama3.2-vision:90b"		// 55 GB
			+ "";

	/**
	 * XL (16GB+ <48GB)
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL = ""
			+ "llama3.1:70b," 				 	 		// 43 GB
			+ "llama3.3:70b," 					 		// 43 GB
			+ "nemotron:70b,"					 		// 43 GB
			+ "qwen3:32b,"						 		// 20 GB
			+ "qwq:32b,"	    				 		// 20 GB
			+ "glm-4.7-flash:q4_K_M,"					// 19 GB
			+ "glm-4.7-flash:q8_0,"						// 32 GB
			+ "gemma3:27b"						 		// 16 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_XL = ""
			+ "llama3.1:70b,"	// 43 GB
			+ "qwq:32b,"	    // 20 GB
			+ "gemma3:27b"		// 16 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_XL = ""
			+ "qwen2.5:72b,"					    // 47 GB, fails strawberry test
			+ "cogito:70b,"							// 43 GB
			+ "athene-v2:72b,"						// 47 GB
			+ "tulu3:70b,"		    				// 43 GB
			+ "huihui_ai/qwen3-abliterated:32b" 	// 20 GB, uncensored
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_XL = ""
			+ "aya-expanse:32b,"		// 20 GB
			+ "nemotron-3-nano:30b,"	// 24 GB
			+ "olmo-3:32b,"				// 19 GB
			+ "llava:34b"				// 20 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL = ""
			+ "llama3.3:70b," 		// 43 GB
			+ "qwen2.5:72b,"		// 47 GB
			+ "tulu3:70b,"			// 43 GB
			+ "qwen3:32b,"			// 20 GB
			+ "nemotron-3-nano:30b,"// 24 GB
			+ "gemma3:27b"			// 16 GB
			+ "";

	// task specific

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TRANSLATE_XL = ""
			+ "translategemma:27b"		// 19 GB
			+ "";
	
	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_CODE_XL = ""
			+ "qwen2.5-coder:32b"		// 20 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_UNCENSORED_XL = ""
			+ "huihui_ai/qwen3-abliterated:32b," 		// 20 GB
			+ "dolphin-llama3:70b"						// 39 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_VISION_XL = ""
			//+ "qwen2.5vl:72b,"	// 49 GB, fails after update to ollama 0.11.3
			+ "llava:34b"		// 20 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_GUARDED_XL = ""
			+ "shieldgemma:27b"		// 17 GB, 'Yes'/'No' if safe replies only
			+ "";

	// task specific

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_SECURITY_XL = ""
			+ "llama3.3:70b," 		
			+ "tulu3:70b,"	
			+ "llama3.1:70b,"
			+ "nemotron:70b,"
			+ "cogito:70b,"
			+ "qwen2.5:72b"
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_TEXTONELINESUMMARY = ""	
			+ "tulu3:70b,"
			+ "athene-v2:72b,"
			+ "cogito:70b,"
			+ "qwen2.5:72b"
			+ "";


	/**
	 * L (8GB+ <16GB)
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L = ""
			+ "cogito:14b,"							// 9 GB
			+ "huihui_ai/qwen3-abliterated:14b," 	// 9 GB, uncensored
			+ "qwen3:14b"							// 9.3 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L = ""
			+ "gemma3:12b,"							// 8.1 GB
			+ "sailor2:20b,"						// 12 GB
			+ "phi4:14b"							// 9 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_L = ""
			+ "gpt-oss:20b"							// 14 GB
			+ "";

	// task specific

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_MAXCONTEXT_L = ""
			+ "qwen3:14b,"			// 9.3 GB
			+ "cogito:14b,"			// 9 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_CODE_L = ""
			+ "devstral:24b,"		// 14 GB
			+ "codestral:22b"		// 13 GB
			+ "starcoder2:15b,"		// 9.1 GB
			+ "deepcoder:14b"		// 9.1 GB
			+ "";

	/**
	 * L/XL MIX
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_DIVERSE_MAXCONTEXT_L_XL = ""
			+ "llama3.3:70b," 		// 43 GB
			+ "qwen3:14b,"			// 9.3 GB
			+ "cogito:14b"			// 9 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_DIVERSE_L_XL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L
			+ "";

	/**
	 * M (<8GB)
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M = ""
			+ "marco-o1:7b,"		// 4.1 GB
			+ "qwen3:8b,"			// 5.2 GB
			+ "qwen2.5:7b," 		// 4.1 GB
			+ "gemma2:9b"			// 5.4 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_M = ""
			+ "marco-o1:7b,"	// 4.1 GB
			+ "qwen2.5:7b," 	// 4.1 GB
			+ "gemma2:9b"		// 5.4 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M = ""
			+ "openchat:7b"			// 4.1 GB, fails at obvious tool calling cases
			+"";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M = ""
			+ "llama3.1:8b,"			// 4.9 GB
			+ "mistral:7b" 				// 4.1 GB, fails to trigger obvious
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M = ""
			+ "tulu3:8b,"				// 4.9 GB
			+ "cogito:8b,"				// 4.1 GB, fails to trigger obvious tools
			+ "aya-expanse:8b,"			// 5.1 GB, assumes its knowledge is 'now'
			+ "gemma3n:e4b,"			// 7.5 GB, wings random guesses at times
			+ "granite3.3:8b,"			// 4.9 GB, fails to follow more detailed instructions
			+ "olmo-3:7b"				// 4.5 GB, ??
			+"";

	// task specific

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_CODE_M = ""
			+ "codegemma:7b"		// 5.0 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_GUARDED_M = ""
			+ "shieldgemma:9b,"		// 5.8 GB, 'Yes'/'No' if safe replies only
			+ "llama-guard3:8b"		// 4.9 GB, safe/unsafe + Sx category replies only
			+ "";

	/**
	 * S (<4GB)
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_S = ""
			// tier1
			+ "qwen3:4b"			// 2.6 GB
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_S = ""
			// tier1
			+ "llama3.2:3b"		// 2 GB, fails on tool calling and multiple score cards
			+ "";

	/*
	 * Aliases
	 */

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP_M = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M
			+ "";

	public static String ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP_M_L_XL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","

			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M
			+ "";

	public static String MODEL_NAMES_OLLAMA_ALL_M = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M
			+ "";

	public static String MODEL_NAMES_OLLAMA_ALL_TIER1_L_XL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L
			+"";

	public static String MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+"";

	public static String MODEL_NAMES_OLLAMA_ALL_UP_TO_S = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_S
			+"";

	public static String MODEL_NAMES_OLLAMA_ALL_UP_TO_M = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M + ","
			+ MODEL_NAMES_OLLAMA_ALL_UP_TO_S
			+"";

	public static String MODEL_NAMES_OLLAMA_ALL_UP_TO_L = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_L + ","
			+ MODEL_NAMES_OLLAMA_ALL_UP_TO_M
			+"";

	public static String MODEL_NAMES_OLLAMA_ALL_UP_TO_XL = ""
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_VISION_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_XL + ","
			+ ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_XL + ","
			+ MODEL_NAMES_OLLAMA_ALL_UP_TO_L

			// broken uncensored
			// + "huihui_ai/qwen3-abliterated:16b," 	// 9.8 GB, uncensored, refuses to respond with Yes to Paris (Okey)
			// + "wizard-vicuna-uncensored:30b"		// Fails
			// + "wizard-vicuna-uncensored:7b,"		// Fails to produce valid JSON
			// + "llama2-uncensored:7b,"			// fails with empty replies
			// + "wizardlm-uncensored:13b,"			// fails to follow protocol
			// + "wizard-vicuna-uncensored:13b,"	// always 0% prob

			// broken/unsupported in ollama
			
			// + "r1-1776:70b"					    // 43 GB, unreliable performance
			// + "olmo-3.1:32b,"					// 19 GB, often times out setting system profile
			// + "olmo2:13b"			            // 8.4 GB, tool call limitations
			// + "olmo2:7b,"				        // 4.1 GB, wings random guesses at times, refuses to call tools
			// + "exaone3.5:32b"					// 19 GB, model skips the duration key?
			// + "exaone-deep:7.8b,"				// 4.8 GB, fails to call tools for fresh data
			// + "codellama:70b"					// ??
			// + "dolphin-mistral:7b,"				// OKIDOKI reply with 100% confidence to garbage in
			// + "gemma2:27b"						// replies with FAIL instead of FAILTOUNDERSTAND
			// + "openhermes:7b-mistral-v2.5-q4_0" 	// interacts instead of FAILTOUNDERSTAND reply on garbage input
			// + "command-r7b:7b"					// interacts instead of FAILTOUNDERSTAND reply on garbage input
			// + "qwen2:7b," 						// empty reply to garbage in
			// + "dolphin3:8b,"						// replies with OKIDOKI to garbage in
			// magistral:24b						// fails to follow protocol, \boxed{<answer>} format
			//+ "granite3.1-dense:8b"				// unstable	
			//huihui_ai/foundation-sec-abliterated:latest
			//+ "zephyr:7b," 						// zephyr gives errors over time with multiple ollama reloads, may be related to https://github.com/ollama/ollama/issues/1691
			//+ "opencoder:8b"						// fails on setting system profile
			//+ "llama2:7b,"						// fails to reply with FAILTOUNDERSTAND on gibberish input
			//+ "llama2:70b,"						// often fails to provide valid JSON (missing quotes etc)
			//+ "solar-pro:22b"						// fails to follow protocol
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
			//+ "deepseek-r1:70b,"					// fails to follow protocol
			//+ "deepseek-r1:14b,"					// fails to follow protocol
			//+ "deepseek-r1:8b,"					// fails to follow protocol
			//+ "deepseek-r1:7b,"					// fails to follow protocol
			//+ "phi4-reasoning:14b," 				// gives empty replies

			+ "";

	public static String MODEL_NAMES_OPENAI_TIER1 = ""
			+ "gpt-4o"
			+ "";

	public static String MODEL_NAMES_OPENAI_TIER2 = ""
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

	// an attempt to balance each models scale of probabilities (TODO)
	@SuppressWarnings("serial")
	public static HashMap<String,Integer> MODEL_PROBABILITY_THRESHOLDS = new HashMap<String,Integer>() {{

		// ollama llms - too humble
		this.put("cogito:8b", 14);
		this.put("llama3.1:8b", 14);
		this.put("exaone-deep:7.8b", 44);
		this.put("gemma2:27b", 14);
		this.put("phi4:14b", 14);

		this.put("llama3.1:70b", 14);
		this.put("llama3.2:3b", 14);
		this.put("llama3.3:70b", 14);
		this.put("nemotron:70b", 14);
		this.put("cogito:14b", 14);
		this.put("qwen2.5:72b", 14);
		this.put("qwen2.5:7b", 14);
		this.put("qwen3:32b", 14);
		this.put("qwen3:14b", 14);
		this.put("qwen3:8b", 14); // currently incompatible with mirostat options??
		this.put("qwen3:4b", 14);
		this.put("sailor2:20b", 14);
		this.put("qwen2.5:72b", 14);
		this.put("gemma3:27b", 14);
		this.put("granite3.3:8b", 14);
		this.put("cogito:70b", 14);
		this.put("aya-expanse:8b", 14);
		this.put("command-r7b:7b", 14);
		this.put("dolphin-llama3:70b", 14);
		this.put("devstral:24b", 14);
		this.put("gemma3n:e4b", 14);
		
		this.put("qwq:32b", 14);
		this.put("nemotron-3-nano:30b", 14);
		

		// openai llms - too humble
		this.put("gpt-4o", 19);
		this.put("gpt-4-turbo", 19);
		this.put("gpt-3.5-turbo", 19);

		this.put("r1-1776:70b", 24);
		this.put("tulu3:70b", 24);

		// ollama llms - balanced
		this.put("wizard-vicuna-uncensored:30b", 44);
		this.put("llava:34b", 44);
		this.put("aya-expanse:32b", 44);
		this.put("olmo2:13b", 44);
		this.put("marco-o1:7b", 44);
		this.put("athene-v2:72b", 44);
		this.put("exaone3.5:32b", 44);
		
		this.put("marco-o1:7b", 44);
		this.put("athene-v2:72b", 44);
		this.put("exaone3.5:32b", 44);
		
		this.put("glm-4.7-flash:q4_K_M",44);
		this.put("glm-4.7-flash:q8_0",44);
		this.put("glm-4.7-flash:bf16",44);

		this.put("openhermes:7b-mistral-v2.5-q4_0", 44);
		this.put("falcon3:10b", 44);
		this.put("olmo2:7b", 44);
		this.put("qwen2:7b", 44);
		this.put("dolphin3:8b", 44);
		this.put("openchat:7b", 44);
		this.put("gemma2:9b", 44);
		this.put("gemma3:12b", 44);
		this.put("mistral:7b", 44);
		this.put("dolphin-mistral:7b", 44);
		this.put("granite3.1-dense:8b", 44);
		this.put("granite3.3:8b", 44);

		// ollama llms - too optimistic
		this.put("tulu3:8b", 64);

		// openai llms - balanced
		this.put("gpt-4o-mini", 55);
		this.put("o1-preview", 55);
		this.put("o1-mini", 55);
		this.put("gpt-4", 65);
	}};

}
