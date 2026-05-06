package ntt.security.ollamadrama.objects.sessions;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.utils.Options;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ChatInteraction;
import ntt.security.ollamadrama.objects.MCPTool;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.SessionType;
import ntt.security.ollamadrama.objects.ToolCallRequest;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.DateUtils;
import ntt.security.ollamadrama.utils.FilesUtils;
import ntt.security.ollamadrama.utils.InteractUtils;
import ntt.security.ollamadrama.utils.JSONUtils;
import ntt.security.ollamadrama.utils.MCPUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.SystemUtils;

public class OllamaSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaSession.class);

	private String model_name;
	private OllamaEndpoint endpoint;
	private Ollama Ollama;
	private OllamaChatResult chatResult;
	private Options options;
	private OllamaDramaSettings settings;
	private String uuid;
	private boolean initialized = false;
	private SessionType sessiontype;
	private int interactcounter = 0;
	private String system_prompt = "";
	private boolean make_tools_available = false;
	private String sessionid = "";
	private String toolcall_history = "";

	// Runaway detection: tracks how often the model hit the num_predict cap
	// (a strong signal it was looping and only stopped by the output limit).
	private int runaway_count = 0;

	// Defaults
	private int DEFAULT_SESSION_TOKENS_MAXLEN = 32000;
	private long DEFAULT_TIMEOUT_IN_SECONDS = 300L;
	private int DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH = 5;

	// Trim policy
	private static final double TRIM_TRIGGER_RATIO = 0.9; // start trimming above this
	private static final double TRIM_TARGET_RATIO  = 0.75; // trim down to this

	// Runaway detection: if reported/estimated output tokens reach this
	// fraction of num_predict, we flag the response as a likely runaway.
	private static final double RUNAWAY_DETECTION_RATIO = 0.9;

	public OllamaSession(String _model_name, OllamaEndpoint _endpoint, Options _options, OllamaDramaSettings _settings, String _systemprompt, SessionType _sessiontype, boolean _make_tools_available) {
		super();

		this.model_name = _model_name;
		this.endpoint = _endpoint;
		this.Ollama = OllamaUtils.createConnection(_endpoint, _settings.getOllama_timeout());
		this.options = _options;
		this.settings = _settings;
		this.uuid = UUID.randomUUID().toString();
		this.system_prompt = _systemprompt;
		this.sessiontype = _sessiontype;
		this.make_tools_available = _make_tools_available;
		this.sessionid = UUID.randomUUID().toString();

		// Sanity-check Options for anti-runaway settings (num_predict,
		// repeat_penalty, repeat_last_n). We can't always introspect reliably
		// across ollama4j versions, so try a best-effort toString() inspection
		// and log a warning. The real fix is in the callsite that builds the
		// Options object.
		warnAboutSamplingOptions(_options);

		// Wait for our turn
		OllamaUtils.wait_for_our_turn(this.Ollama, model_name);

		LOGGER.info("Setting chat system profile of size: " + _systemprompt.length());
		this.initialized = setChatSystemProfileStatement(_systemprompt, _settings.getAutopull_max_llm_size(), _settings.getOllama_timeout());	
		LOGGER.info("DONE setting chat system profile");
	}

	/**
	 * Best-effort sanity check on the Options object. Logs warnings when key
	 * anti-runaway parameters are missing. Detection is by toString()
	 * inspection, which is fragile across ollama4j versions - if you've set
	 * the option but still get a warning, the detection just couldn't see it.
	 *
	 * Recommended OptionsBuilder snippet for chat sessions on the GB10 with
	 * abliterated Gemma-style models:
	 *
	 *   new OptionsBuilder()
	 *       .setTemperature(0.27f)
	 *       .setNumCtx(65536)
	 *       .setNumPredict(4096)        // hard cap on output, prevents 20-min hangs
	 *       .setRepeatPenalty(1.15f)    // wider than default 1.1 - reins in loops
	 *       .setRepeatLastN(512)        // default 64 is too narrow at large num_ctx
	 *       .setSeed(seed)              // randomize per-request to escape stuck states
	 *       .build();
	 */
	private void warnAboutSamplingOptions(Options opts) {
		try {
			if (opts == null) {
				LOGGER.warn("Options is null - num_predict, repeat_penalty etc. will default to model settings (often unbounded). This can cause runaway generation. Set them explicitly via OptionsBuilder.");
				return;
			}
			String dump = String.valueOf(opts);
			if (dump == null) return;
			String lower = dump.toLowerCase();

			if (!lower.contains("num_predict") && !lower.contains("numpredict")) {
				LOGGER.warn("Options does not appear to set num_predict. Without it, Ollama may run until num_ctx fills. Recommend OptionsBuilder.setNumPredict(4096) to bound output and prevent runaway generation. (Best-effort detection - ignore if you've set it.)");
			}
			if (!lower.contains("repeat_penalty") && !lower.contains("repeatpenalty")) {
				LOGGER.warn("Options does not appear to set repeat_penalty. Default 1.1 is often too lenient for abliterated/uncensored models that loop. Recommend OptionsBuilder.setRepeatPenalty(1.15f). (Best-effort detection - ignore if you've set it.)");
			}
			if (!lower.contains("repeat_last_n") && !lower.contains("repeatlastn")) {
				LOGGER.warn("Options does not appear to set repeat_last_n. Default 64 is too narrow when num_ctx is in the thousands; medium-range loops slip past it. Recommend OptionsBuilder.setRepeatLastN(512). (Best-effort detection - ignore if you've set it.)");
			}
		} catch (Exception e) {
			// Detection is best-effort only - never block construction on it.
			LOGGER.debug("Sampling options detection failed (non-fatal)", e);
		}
	}

	/**
	 * Best-effort extraction of an integer Options value (e.g. num_predict)
	 * from the Options object's string dump. Tolerates both snake_case and
	 * camelCase keys, and either ":" (JSON) or "=" (toString) separators.
	 *
	 * @return the parsed value, or -1 if not found
	 */
	private int extractIntOptionFromOptions(Options opts, String snake_key) {
		if (opts == null || snake_key == null) return -1;
		try {
			String dump = String.valueOf(opts);
			if (dump == null) return -1;
			String camel = snakeToCamel(snake_key);
			// Match either form: "num_predict": 4096  or  numPredict=4096
			java.util.regex.Pattern p = java.util.regex.Pattern.compile(
					"(?:\"" + java.util.regex.Pattern.quote(snake_key) + "\""
					+ "|\"" + java.util.regex.Pattern.quote(camel) + "\""
					+ "|\\b" + java.util.regex.Pattern.quote(snake_key) + "\\b"
					+ "|\\b" + java.util.regex.Pattern.quote(camel) + "\\b)"
					+ "\\s*[:=]\\s*(-?\\d+)"
			);
			java.util.regex.Matcher m = p.matcher(dump);
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		} catch (Exception e) {
			// best-effort
		}
		return -1;
	}

	private String snakeToCamel(String snake) {
		if (snake == null) return "";
		StringBuilder sb = new StringBuilder();
		boolean upper = false;
		for (int i = 0; i < snake.length(); i++) {
			char c = snake.charAt(i);
			if (c == '_') {
				upper = true;
			} else {
				sb.append(upper ? Character.toUpperCase(c) : c);
				upper = false;
			}
		}
		return sb.toString();
	}

	/**
	 * Try to read the actual eval_count (output token count) from the ollama4j
	 * response model via reflection. Returns -1 if unavailable on this version.
	 */
	private long extractEvalCountFromChatResult(OllamaChatResult result) {
		if (result == null) return -1L;
		try {
			Object responseModel = result.getResponseModel();
			if (responseModel == null) return -1L;
			for (String methodName : new String[] { "getEvalCount", "getEval_count" }) {
				try {
					java.lang.reflect.Method m = responseModel.getClass().getMethod(methodName);
					Object v = m.invoke(responseModel);
					if (v instanceof Number) {
						return ((Number) v).longValue();
					}
				} catch (NoSuchMethodException e) {
					// try next
				} catch (Exception e) {
					// reflection failure - try next
				}
			}
		} catch (Throwable t) {
			// fall through to fallback
		}
		return -1L;
	}

	/**
	 * Detects whether a chat response likely hit the num_predict cap (i.e. the
	 * model went into a degenerate loop and was only stopped by the output
	 * token limit). Uses ollama4j's reported eval_count when available, falls
	 * back to estimating tokens from response text length.
	 *
	 * Logs a WARN if a runaway is suspected and increments runaway_count.
	 *
	 * @return true if a runaway is suspected, false otherwise
	 */
	private boolean detectRunaway(ChatInteraction ci, String responseText, double execTimeSeconds) {
		int numPredict = extractIntOptionFromOptions(this.options, "num_predict");
		if (numPredict <= 0) {
			// Couldn't determine the cap - can't make a reliable call
			return false;
		}

		long evalCount = -1L;
		if (ci != null) {
			evalCount = extractEvalCountFromChatResult(ci.getChatResult());
		}

		int outputTokens;
		String source;
		if (evalCount > 0) {
			outputTokens = (int) evalCount;
			source = "ollama eval_count";
		} else {
			outputTokens = estimateTokenCount(responseText);
			source = "estimated from response text";
		}

		double ratio = (double) outputTokens / (double) numPredict;
		if (ratio >= RUNAWAY_DETECTION_RATIO) {
			this.runaway_count++;
			LOGGER.warn(String.format(
					"Suspected runaway generation: output tokens %d (%s) reached %d%% of num_predict=%d in %.2fs. "
					+ "Model likely looped and was stopped only by the output cap. "
					+ "Mitigations to consider: increase repeat_penalty (e.g. 1.15-1.2), widen repeat_last_n (e.g. 512), "
					+ "randomize seed on retry, or evaluate an alternative model. "
					+ "Total suspected runaways this session: %d",
					outputTokens, source, (int)(ratio * 100), numPredict, execTimeSeconds, this.runaway_count
			));
			return true;
		}
		return false;
	}

	public int getRunawayCount() {
		return this.runaway_count;
	}

	/**
	 * Best-effort clone of the given Options, with num_predict overridden.
	 * Used by output-cap overloads (e.g. summarization) to bump the output
	 * limit for a single call without disturbing the session-level Options.
	 *
	 * Reflection is unavoidable here: ollama4j's Options class doesn't expose
	 * an accessor for the underlying map across versions. We try common
	 * field names and a Map constructor; if none work we log a warning and
	 * return the original Options unchanged (so the call still succeeds, it
	 * just won't have the bumped cap).
	 *
	 * @param orig         the source Options (typically this.options)
	 * @param numPredict   the new num_predict value (use -1 for unbounded)
	 * @return a new Options with num_predict overridden, or orig on failure
	 */
	@SuppressWarnings("unchecked")
	private Options cloneOptionsWithNumPredict(Options orig, int numPredict) {
		if (orig == null) return null;

		java.util.Map<String, Object> copy = null;
		java.lang.reflect.Field foundField = null;

		// 1) Find and copy the underlying optionsMap.
		for (String fieldName : new String[] { "optionsMap", "options" }) {
			try {
				java.lang.reflect.Field f = orig.getClass().getDeclaredField(fieldName);
				f.setAccessible(true);
				Object val = f.get(orig);
				if (val instanceof java.util.Map) {
					copy = new java.util.HashMap<>((java.util.Map<String, Object>) val);
					foundField = f;
					break;
				}
			} catch (NoSuchFieldException e) {
				// try next field name
			} catch (Exception e) {
				LOGGER.debug("cloneOptionsWithNumPredict: reflection on field '" + fieldName + "' failed", e);
			}
		}

		if (copy == null) {
			LOGGER.warn("cloneOptionsWithNumPredict: no recognized internal map field on " + orig.getClass().getName() + " - num_predict override of " + numPredict + " will NOT apply. Falling back to session Options.");
			return orig;
		}

		copy.put("num_predict", numPredict);

		// 2a) Try a Map-accepting constructor.
		try {
			@SuppressWarnings("rawtypes")
			java.lang.reflect.Constructor ctor = orig.getClass().getDeclaredConstructor(java.util.Map.class);
			ctor.setAccessible(true);
			return (Options) ctor.newInstance(copy);
		} catch (NoSuchMethodException e) {
			// no Map ctor - try the no-arg ctor + field write
		} catch (Exception e) {
			LOGGER.debug("cloneOptionsWithNumPredict: Map ctor failed", e);
		}

		// 2b) Fall back to no-arg ctor + writing the field directly.
		try {
			java.lang.reflect.Constructor<? extends Options> ctor = orig.getClass().getDeclaredConstructor();
			ctor.setAccessible(true);
			Options nu = ctor.newInstance();
			foundField.set(nu, copy);
			return nu;
		} catch (Exception e) {
			LOGGER.warn("cloneOptionsWithNumPredict: could not construct cloned Options - num_predict override of " + numPredict + " will NOT apply", e);
		}

		return orig;
	}

	public String getModel_name() {
		return model_name;
	}

	public void setModel_name(String model_name) {
		this.model_name = model_name;
	}

	public Ollama getOllama() {
		return Ollama;
	}

	public void setOllama(Ollama Ollama) {
		this.Ollama = Ollama;
	}

	public OllamaChatResult getChatResult() {
		return chatResult;
	}

	public void setChatResult(OllamaChatResult chatResult) {
		this.chatResult = chatResult;
	}

	public int getInteractcounter() {
		return interactcounter;
	}

	public void setInteractcounter(int interactcounter) {
		this.interactcounter = interactcounter;
	}

	public String getSystem_prompt() {
		return system_prompt;
	}

	public void setSystem_prompt(String system_prompt) {
		this.system_prompt = system_prompt;
	}

	public String askGenericSingleWordQuestion(String _question) {
		boolean success = false;
		while (!success) {
			String resp = OllamaUtils.askGenericSingleWordQuestion(this.Ollama, this.model_name, this.options, _question);
			if (null != resp) {
				SingleStringQuestionResponse swr = JSONUtils.createPOJOFromJSONOpportunistic(resp, SingleStringQuestionResponse.class);
				if (null != swr) return swr.getResponse();
			}
			SystemUtils.sleepInSeconds(1); // throttle
		}
		return "";
	}

	public boolean setChatSystemProfileStatement(String _profile_statement, String _autopull_max_llm_size, long _timeout) {
		int errorCount = 0;

		if (null == this.chatResult) {
			while (null == this.chatResult) {
				OllamaChatResult res = OllamaUtils.setChatSystemProfile(this.Ollama, this.model_name, this.options, _profile_statement, _autopull_max_llm_size, _timeout);
				if (null != res) {
					//this.chatResult = res;
					// Lets not store the initial prompt reply as part of the session
					OllamaChatResponseModel res_model = res.getResponseModel();
					String raw_result = OllamaUtils.preprocess_llm_response(res.getResponseModel().getMessage().getResponse());
					OllamaChatMessage message = res_model.getMessage();
					message.setResponse(raw_result);
					res_model.setMessage(message);

					this.chatResult = new OllamaChatResult(res_model, res.getChatHistory());
					return true;
				}
				errorCount++;
				LOGGER.warn("Unable to set system profile for model " + this.model_name + ", errorCount: " + errorCount);
				SystemUtils.sleepInSeconds(10); // throttle
				if (errorCount >= 600) {
					LOGGER.warn("Unable to set system profile for model " + this.model_name + ", errorCount: " + errorCount + " so giving up ..");
					return false;
				}
			}
		} else {
			LOGGER.warn("Profile statement already defined, ignoring");
		}
		return false;
	}

	public void provideChatStatement(String _statement, long _timeout) {
		if (null == this.chatResult) {
			LOGGER.warn("You need to initialize a chat session with a profile statement first");
		} else {
			if (this.getSessiontype() == SessionType.CREATIVE) {
				ChatInteraction ci = OllamaUtils.addCreativeStatementToExistingChat(this.Ollama, this.model_name, this.options, this.chatResult, _statement, _timeout);
				if (null != ci) {
					LOGGER.debug("Successfully extended chat session with results from STATEMENT interaction");
					this.chatResult = ci.getChatResult();
				}
			} else {
				ChatInteraction ci = OllamaUtils.addStrictStatementToExistingChat(this.Ollama, this.model_name, this.options, this.chatResult, _statement, _timeout);
				if (null != ci) {
					LOGGER.debug("Successfully extended chat session with results from STATEMENT interaction");
					this.chatResult = ci.getChatResult();
				}
			}
		}
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt) {
		return askStrictChatQuestion(_prompt, false, DEFAULT_SESSION_TOKENS_MAXLEN, DEFAULT_TIMEOUT_IN_SECONDS, 5, 4, false, null, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, String _history_file) {
		return askStrictChatQuestion(_prompt, false, DEFAULT_SESSION_TOKENS_MAXLEN, DEFAULT_TIMEOUT_IN_SECONDS, 5, 4, false, _history_file, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, long _timeout, boolean _return_toolcall, String _history_file) {
		return askStrictChatQuestion(_prompt, false, DEFAULT_SESSION_TOKENS_MAXLEN,_timeout, DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH, 4, _return_toolcall, _history_file, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, long _timeout_seconds, int _max_recursive_toolcall_depth) {
		return askStrictChatQuestion(_prompt, false, DEFAULT_SESSION_TOKENS_MAXLEN,_timeout_seconds, _max_recursive_toolcall_depth, 4, false, null, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, int session_tokens_maxlen, int _max_recursive_toolcall_depth, int _toolcall_pausetime_in_seconds, String _history_file) {
		return askStrictChatQuestion(_prompt, false, DEFAULT_SESSION_TOKENS_MAXLEN, DEFAULT_TIMEOUT_IN_SECONDS, _max_recursive_toolcall_depth, _toolcall_pausetime_in_seconds, false, _history_file, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, int _max_recursive_toolcall_depth, String _history_file, boolean _unloadModelAfterChat) {
		return askStrictChatQuestion(_prompt, "", DEFAULT_SESSION_TOKENS_MAXLEN, false, 30, 120, 0, _max_recursive_toolcall_depth, 4, false, false, _history_file, _unloadModelAfterChat, false, null, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, boolean _hide_llm_reply_if_uncertain, long _timeout_seconds, String _history_file) {
		return askStrictChatQuestion(_prompt, _hide_llm_reply_if_uncertain, DEFAULT_SESSION_TOKENS_MAXLEN, _timeout_seconds, DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH, 4, false, _history_file, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, boolean _hide_llm_reply_if_uncertain, long _timeout_seconds, int _toolcall_pausetime_in_seconds, boolean _return_toolcall, String _history_file) {
		return askStrictChatQuestion(_prompt, _hide_llm_reply_if_uncertain, DEFAULT_SESSION_TOKENS_MAXLEN, _timeout_seconds, DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH, _toolcall_pausetime_in_seconds, _return_toolcall, _history_file, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, boolean _hide_llm_reply_if_uncertain, int _retryThreshold, long _timeout_seconds, String _history_file, boolean _unloadModelAfterChat) {
		return askStrictChatQuestion(_prompt, "", DEFAULT_SESSION_TOKENS_MAXLEN, _hide_llm_reply_if_uncertain, _retryThreshold, _timeout_seconds, 0, DEFAULT_MAX_RECURSIVE_TOOLCALL_DEPTH, 4, false, false, _history_file, _unloadModelAfterChat, false, null, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _prompt, boolean _hide_llm_reply_if_uncertain, int _session_tokens_maxlen, long _timeout_seconds, int _max_recursive_toolcall_depth, int _toolcall_pausetime_in_seconds, boolean _return_toolcall, String _history_file, boolean _unloadModelAfterChat) {
		return askStrictChatQuestion(_prompt, "", _session_tokens_maxlen, _hide_llm_reply_if_uncertain, 30, _timeout_seconds, 0, _max_recursive_toolcall_depth, _toolcall_pausetime_in_seconds, _return_toolcall, false, _history_file, _unloadModelAfterChat, false, null, false);
	}

	/**
	 * Convenience overload for tasks that legitimately need long output - e.g.
	 * summarizing chat history. Bumps num_predict for this single call only,
	 * leaving the session's standard Options untouched.
	 *
	 * Pass -1 for unbounded output (not recommended unless you have a hard
	 * client-side timeout). For typical summary work, 8192-16384 is a good
	 * starting point on the GB10.
	 *
	 * NOTE: This temporarily mutates this.options for the duration of the call
	 * and restores it in finally. Do not call concurrently from multiple
	 * threads against the same OllamaSession instance.
	 *
	 * @param _prompt              the prompt
	 * @param _numPredictOverride  num_predict to use just for this call (-1 = unbounded)
	 */
	public SingleStringQuestionResponse askStrictChatQuestionWithOutputCap(String _prompt, int _numPredictOverride) {
		return askStrictChatQuestionWithOutputCap(_prompt, _numPredictOverride, null);
	}

	/**
	 * Convenience overload with history file. See {@link #askStrictChatQuestionWithOutputCap(String, int)}.
	 */
	public SingleStringQuestionResponse askStrictChatQuestionWithOutputCap(String _prompt, int _numPredictOverride, String _history_file) {
		Options original = this.options;
		Options swapped = cloneOptionsWithNumPredict(original, _numPredictOverride);
		if (swapped == original) {
			// Clone failed - the warning was already logged. Run with the
			// session-default cap rather than aborting; output may truncate.
			LOGGER.warn("askStrictChatQuestionWithOutputCap: running with session-default num_predict because override clone failed. Output may truncate at the regular cap.");
			return askStrictChatQuestion(_prompt, _history_file);
		}
		try {
			LOGGER.info("askStrictChatQuestionWithOutputCap: temporarily overriding num_predict to " + _numPredictOverride + " for this call");
			this.options = swapped;
			return askStrictChatQuestion(_prompt, _history_file);
		} finally {
			this.options = original;
		}
	}

	public SingleStringQuestionResponse askStrictChatQuestion(final String _prompt, String _recursive_question, int session_tokens_maxlen, boolean _hide_llm_reply_if_uncertain, int _retryThreshold, long _timeout_seconds, int _exec_depth_counter, int _max_recursive_toolcall_depth, int _toolcall_pausetime_in_seconds, boolean _return_toolcall, boolean _halt_on_tool_error, String _history_file, boolean _unloadModelAfterQuery, boolean _debug, Map<String, Function<String, String>> _mcp_preprocess, boolean _prompt_logging) {
		if (_max_recursive_toolcall_depth < 0) LOGGER.warn("No tools will be called if value of_max_recursive_toolcall_depth is not 1 or more");
		String full_prompt = _prompt;
		String full_prompt_STDOUT = _prompt;
		if (!"".equals(_recursive_question)) {
			full_prompt = _prompt + "\n\n" + _recursive_question + "\n\nRemember:\n" + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_FOR_AGENTS + "\n\n" + OllamaService.getAllAvailableMCPTools();
			full_prompt_STDOUT = _prompt + "\n\n" + _recursive_question + "\n\nRemember:\n<JSON FORMAT NAG HERE>\n<MCP TOOLS LISTED AGAIN HERE>";
		}

		if (_debug) {
			if (this.interactcounter == 0) {
				System.out.println("this.interactcounter: " + this.interactcounter);
				System.out.println(this.getSystem_prompt() + "\n" + full_prompt_STDOUT + "\n\n");
			} else {
				System.out.println(full_prompt_STDOUT + "\n\n");
			}
		}

		// FIXED: include chat history in the surfaced estimate so it actually
		// reflects what gets sent to ollama (was previously missing history,
		// causing client logs to under-report by 2x or more).
		int initial_full_estimate = estimateTokenCount(this.getSystem_prompt())
				+ this.getChatSizeWordCount()
				+ estimateTokenCount(full_prompt);
		LOGGER.info("Estimated FULL context tokencount (system + history + new prompt) is " + initial_full_estimate);
		if (this.getToolcall_history().contains("-")) System.out.println("Toolcall history:" + this.getToolcall_history() + " (debug: " + _debug + ")\n");

		// full prompt at each depth
		if (_prompt_logging) {
		    String filepath = this.getSessionid() + "_prompt_at_depth_" + _exec_depth_counter + ".log";
		    LOGGER.info("Writing current prompt to " + filepath);
		    
		    StringBuffer logContent = new StringBuffer();
		    logContent.append("=== DEPTH " + _exec_depth_counter + " | " + DateUtils.epochInSecondsToUTC(System.currentTimeMillis()/1000L) + " ===\n\n");
		    logContent.append("=== SYSTEM PROMPT ===\n");
		    logContent.append(this.getSystem_prompt());
		    logContent.append("\n=== END SYSTEM PROMPT ===\n\n");
		    logContent.append("=== CHAT HISTORY (" + this.getChatSizeWordCount() + " est. tokens) ===\n");
		    logContent.append(this.getChatHistory());
		    logContent.append("\n=== END CHAT HISTORY ===\n\n");
		    logContent.append("=== FULL PROMPT SENT AT THIS DEPTH ===\n");
		    logContent.append(full_prompt_STDOUT);  // already assembled above, includes _recursive_question + JSON nag + MCP tools
		    logContent.append("\n=== END FULL PROMPT ===\n");
		    
		    FilesUtils.writeToFileUNIXNoException(logContent.toString(), filepath);
		}

		this.interactcounter = interactcounter + 1;
		if (this.sessiontype == SessionType.STRICTPROTOCOL) {

			if (null == this.chatResult) {
				LOGGER.warn("chatResult is null!");
				SingleStringQuestionResponse swr = OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
				return swr;
			} else {
				int retryCounter = 0;
				while (true) {
					// FIXED: log the FULL estimate (system + history + new prompt),
					// not just system + new prompt. Compare this number against
					// Ollama's server-side prompt= line to validate the estimator.
					int full_estimate = estimateTokenCount(this.getSystem_prompt())
							+ this.getChatSizeWordCount()
							+ estimateTokenCount(full_prompt);
					LOGGER.info("Execution timer start, new prompt to LLM of charsize " + full_prompt.length()
							+ ", new-prompt-only tokencount " + estimateTokenCount(full_prompt)
							+ ", FULL context tokencount (system + history + new) " + full_estimate);
					long startTime  = System.nanoTime();
					
				    // Ensure history doesnt overflow.
				    // FIXED: tighter thresholds (was 0.85/0.75, now TRIM_TRIGGER_RATIO/TRIM_TARGET_RATIO).
				    // FIXED: hoist invariants out of the trim loop instead of recomputing per iteration.
				    int system_tokens = estimateTokenCount(this.getSystem_prompt());
				    int prompt_tokens = estimateTokenCount(full_prompt);
				    int estimatedTotal = system_tokens + this.getChatSizeWordCount() + prompt_tokens;
				    int trimTrigger = (int)(session_tokens_maxlen * TRIM_TRIGGER_RATIO);
				    int trimTarget  = (int)(session_tokens_maxlen * TRIM_TARGET_RATIO);
				    if (estimatedTotal > trimTrigger) {
				    	List<OllamaChatMessage> history = this.chatResult.getChatHistory();
				        LOGGER.warn("Context " + estimatedTotal + " tokens exceeds " + (int)(TRIM_TRIGGER_RATIO * 100) + "% of limit " + session_tokens_maxlen + ", trimming chat history which consists of " + history.size() + " messages");
				        // Drop oldest non-system messages from the front until we're under the target
				        while (history.size() > 1) {
				            int check = system_tokens + this.getChatSizeWordCount() + prompt_tokens;
				            if (check <= trimTarget) break;
				            history.remove(0); // drop oldest message
				        }
				        LOGGER.info("Trimmed history to " + history.size() + " messages, estimated total now: "
				        		+ (system_tokens + this.getChatSizeWordCount() + prompt_tokens));
				    } else {
				    	 LOGGER.info("Context " + estimatedTotal + " tokens does not exceed " + (int)(TRIM_TRIGGER_RATIO * 100) + "% of limit " + session_tokens_maxlen + ", keeping chat history");
				    }
					
					ChatInteraction ci =  OllamaUtils.askChatQuestion(this.Ollama, this.model_name, this.options, this.chatResult, full_prompt, _timeout_seconds, _retryThreshold, _unloadModelAfterQuery);
					if (null != ci) {
						String json = "";

						//if (_debug) LOGGER.info("Raw response: " + ci.getResponse());

						// JSON markdown (LLM protocol helper hack)
						if (ci.getResponse().contains("{") && ci.getResponse().contains("}")) {
							json = "{" + ci.getResponse().split("\\{")[1];
							json = json.split("\\}")[0] + "}";
						} else {
							json = ci.getResponse();
						}

						// JSON newline fix
						json = json.replace("\n", " ").replace("\r", " ");

						// tool call forgiveness
						json = json.replace("\"tool_calls\":[]", "\"tool_calls\":\"\"").replace("\"tool_calls\": []", "\"tool_calls\":\"\"");

						// JSON reply check (LLM protocol helper hack)
						if (json.contains("{") && json.contains("}") && json.contains("\"response\": FAILTOUNDERSTAND,")) {
							json = json.replace("\"response\": FAILTOUNDERSTAND,","\"response\": \"FAILTOUNDERSTAND\",");
						} else if (json.contains("{") && json.contains("}") && json.contains("\"response\": OKIDOKI,")) {
							json = json.replace("\"response\": OKIDOKI,","\"response\": \"OKIDOKI\",");
						}

						SingleStringQuestionResponse swr = JSONUtils.createPOJOFromJSONOpportunistic(json, SingleStringQuestionResponse.class);
						if (null != swr) {

							long endTime = System.nanoTime();
							double executionTimeSeconds = (endTime - startTime) / 1_000_000_000.0;
							swr.setExec_time(executionTimeSeconds);

							// Detect runaway generation: if the model produced output up to (or
							// near) the num_predict cap, it was probably looping and we just
							// caught it at the limit. Logs a WARN and bumps runaway_count.
							// Non-fatal - we still return the (likely degraded) response.
							detectRunaway(ci, ci.getResponse(), executionTimeSeconds);

							if (null != swr.getResponse()) {
								// PROTOCOL hack
								if (swr.getResponse().equals("FAILTOUNDERSTAND")) {
									swr.setProbability(0); // we are not 100% sure on not understanding
								}
								if (swr.getResponse().equals("TOOLCALL") && swr.getTool_calls().equals("")) {
									// seen as: fictional non-existent tool required to provide a response, failtounderstand what to do without this tool
									swr.setResponse("FAILTOUNDERSTAND");
									swr.setProbability(0);
								}
								if (swr.getResponse().equals("OKIDOKI") && swr.getTool_calls().startsWith("oneshot")) {
									// we forgive ..
									swr.setResponse("TOOLCALL");
								}

								this.chatResult = ci.getChatResult();
								swr.setEmpty(false);
								swr = OllamaUtils.applyResponseSanity(swr, model_name, _hide_llm_reply_if_uncertain);

								// rewrites

								boolean valid_tool_calls = true;
								if (!swr.getTool_calls().equals("")) {
									LOGGER.debug("swr.getTool_calls(): " + swr.getTool_calls());
									ArrayList<ToolCallRequest> tool_calls = MCPUtils.parseToolCalls(swr.getTool_calls());
									for (ToolCallRequest tcr: tool_calls) {
										//System.out.println("tcr: " + tcr.toString());
										MCPTool mcpTool = OllamaService.getMCPURLForTool(tcr.getToolname());
										if (null == mcpTool) {
											LOGGER.warn(tcr.getToolname() + " is not a valid tool name");
											//System.out.println("DEBUG");
											//SystemUtils.sleepInSeconds(600);
											valid_tool_calls = false;
										}
									}
								} else {
									valid_tool_calls = false;
								}

								// print intermediate result to STDOUT
								System.out.println("");
								System.out.println(this.model_name);
								swr.print();
								System.out.println("");

								// exec_time debug
								if (_debug) {
									if (null != swr) {
										// dont count timeouts
										if (swr.getExec_time().longValue() < settings.getOllama_timeout()) {
											// prompt size vs exec_time
											String debug_file = "exec_time.csv";
											File f = new File(debug_file);
											if (!f.exists()) FilesUtils.writeToFileUNIXNoException("model,tokens_in_prompt,exec_time,sessionid",debug_file);
											int estimated_prompt_token_count = estimateTokenCount(this.getSystem_prompt() + this.getChatHistory() + full_prompt);
											FilesUtils.appendToFileUNIXNoException(model_name + "," + estimated_prompt_token_count + "," + swr.getExec_time() + "," + this.getSessionid(), debug_file);
										}
									}
								}

								if (_return_toolcall && swr.getResponse().startsWith("TOOLCALL")) {
									return swr;
								}

								if (this.isMake_tools_available() && ("TOOLCALL".equals(swr.getResponse()))) {

									if (!valid_tool_calls) {
										LOGGER.warn("Agent requested a tool_call which does not exist (" + swr.getTool_calls() + ")\n");
										return swr;
									} else {

										this.addToToolcall_history(swr.getTool_calls());
										StringBuffer sb_toolcallresulthistory = new StringBuffer();

										LOGGER.info("Tool Call Request: " + sanitizeToolCallQuotes(swr.getTool_calls()));
										ArrayList<ToolCallRequest> tool_calls = MCPUtils.parseToolCalls(sanitizeToolCallQuotes(swr.getTool_calls()));

										LOGGER.info("tool_calls size: " + tool_calls.size());
										for (ToolCallRequest tcr: tool_calls) {
											if (!tcr.sanitycheck_pass()) {
												LOGGER.warn("Tool Call Request sanitycheck failed - name: " + tcr.getToolname() + " calltype: " + tcr.getCalltype() + " arguments:" + tcr.getArguments().toString());
												return swr;
											} else {
												LOGGER.info("Valid toolcall request");

												// Find the MCP URL to call the tool
												MCPTool mcpTool = OllamaService.getMCPURLForTool(tcr.getToolname());
												if (null == mcpTool) {
													LOGGER.error("Agent requested a tool_call which does not exist (" + tcr.getToolname() + ")\n");
													if (_halt_on_tool_error) {
														LOGGER.error("Instructed to halt on error");
														SystemUtils.halt();
													}
												}
												String mcpURL = mcpTool.getEndpoint().getSchema() + "://" + mcpTool.getEndpoint().getHost() + ":" + mcpTool.getEndpoint().getPort();
												String mcpPATH = mcpTool.getEndpoint().getPath();

												if ( (tcr.getToolname().length()>0) && (mcpURL.startsWith("http") && mcpPATH.startsWith("/"))) {

													boolean make_call = false;
													if (OllamaService.isMatchingMCPTool(tcr.getToolname(), settings.getFiltered_mcp_toolnames_csv())) {
														make_call = false;
														LOGGER.info("Filtered mcp toolname so blocking agent to run the tool call " + tcr.getRawrequest());
													} else if (settings.isMcp_blind_trust()) {
														make_call = true;
														LOGGER.info("Blindly allowing agent to run the tool call " + tcr.getToolname());
													} else if (OllamaService.isMatchingMCPTool(tcr.getToolname(), settings.getTrusted_mcp_toolnames_csv())) {
														make_call = true;
														LOGGER.info("Trusted mcp toolname so allowing agent to run the tool call " + tcr.getToolname() + " arguments:" + tcr.getArguments().toString());
													} else {
														make_call = InteractUtils.getYNResponse("The agent is requesting to run the tool call " + tcr.getToolname() + " arguments:" + tcr.getArguments().toString() + ", press Y to allow and N to abort.", settings);
													}

													// Call tool
													if (make_call) {
														LOGGER.info("Making call to URL " + mcpURL);
														CallToolResult result = MCPUtils.callToolUsingMCPEndpoint(mcpURL, mcpPATH, tcr.getToolname(), tcr.getArguments(), 300L, _halt_on_tool_error);

														if (result.isError()) {
															LOGGER.warn("Will exit TOOLCALL LOOP since I got an error reply: " + result.toString());
															SystemUtils.sleepInSeconds(5);
															return swr;
														} else {

															String tool_response = "";

															// defined preprocessing
															if ((null != _mcp_preprocess) && (null != _mcp_preprocess.get(tcr.getToolname()))) {
																LOGGER.info("Applying preprocessing");
																tool_response = _mcp_preprocess.get(tcr.getToolname()).apply(MCPUtils.getRawText(result));
															} else {
																tool_response = "\nResponse from running tool_call " + tcr.getRawrequest() + ":\n\n" + MCPUtils.getRawText(result);
															}

															// Simple prompt injection checks
															if (settings.isMcp_enable_promptinject_protection()) {
																String tool_response_sanitized = OllamaUtils.sanitizePromptInjection(tool_response, "PROMPT_INJECTION_ATTACK_IDENTIFIED");
																if (!tool_response.equals(tool_response_sanitized)) {
																	LOGGER.warn("Prompt injection attack found in tool_response");
																	LOGGER.warn("tool_response: " + tool_response);
																	LOGGER.warn("NEW tool_response: " + tool_response_sanitized);
																	LOGGER.warn("Your MCP Tool call response from " + tcr.getToolname() + " seems to include a prompt injection attack so leaving recursive TOOLCALL loop");
																	swr.setResponse("PROMPT_INJECTION_ATTACK_IDENTIFIED");
																	swr.setPromptinject(true);
																	return swr;
																}
															}

															// Check for context overflows when received tool reply 
															int estimated_response_token_count = estimateTokenCount(tool_response);

															int estimated_sum_context_token_count = this.getChatSizeWordCount() + estimated_response_token_count;
															if (estimated_sum_context_token_count >= session_tokens_maxlen) {
																LOGGER.warn("Estimated tokencount in TOOLCALL response is " + estimated_response_token_count + ", so the full context " + estimated_sum_context_token_count + " exceeds the total context restriction of " + session_tokens_maxlen + ". Will abort recursive TOOLCALL run.");
																return swr;
															} else {
																LOGGER.warn("Estimated tokencount in TOOLCALL response is " + estimated_response_token_count + ", so the full context is roughly " + estimated_sum_context_token_count);
															}

															System.out.println("tool_response: " + tool_response);
															sb_toolcallresulthistory.append("### PREVIOUS PROMPT ###\n");
															sb_toolcallresulthistory.append("You were presented with the following question:\n" + _recursive_question + "\n\n");
															sb_toolcallresulthistory.append("You took the following TOOLCALL decision:\n" + swr.getOutputAsString() + "\n\n");
															sb_toolcallresulthistory.append("This TOOLCALL decision gave the following result:\n" + tool_response + "\n\n");
															sb_toolcallresulthistory.append("### END PREVIOUS PROMPT ###\n");

															// update history file with successful actions
															if (null != _history_file) {
																//FilesUtils.appendToFileUNIXNoException(swr.getOutputAsString() + "\n\n" + tool_response, _history_file);
															}

														}

													} else {
														LOGGER.info("Your MCP Tool call to " + tcr.getToolname() + " was rejected so leaving recursive TOOLCALL loop");
														return swr;
													}

												} else {
													LOGGER.warn("Unable to call tool " + tcr.getToolname() + " with MCP URL " + mcpURL);
													if (_halt_on_tool_error) {
														LOGGER.info("Instructed to halt on error");
														SystemUtils.halt();
													}
													return swr;
												}
											}
											LOGGER.info("toolname: " + tcr.getToolname());
										}

										// check if we need to break due to token usage
										int chatsize_wordcount_a1 = estimateTokenCount(
												this.getSystem_prompt()) // session init 
												+ this.getChatSizeWordCount()  // session history
												+ estimateTokenCount(sb_toolcallresulthistory.toString()); // session new toolcall reply data
										LOGGER.info("estimated session wordcount (including toolcall replies): " + chatsize_wordcount_a1);
										int headroom = (int)(session_tokens_maxlen * 0.8); // leave 20% for the LLM reply
										if (chatsize_wordcount_a1 > headroom) {
											LOGGER.info("Breaking recursive toolcall, estimated tokens: " + chatsize_wordcount_a1 + " (80% of session_tokens_maxlen: " + headroom + ")");
											return swr;
										}

										// Early exit on recurisive tool calls
										if (_exec_depth_counter >= _max_recursive_toolcall_depth) {
											LOGGER.info("Breaking recursive toolcall since we are at depth " + _exec_depth_counter);
											return swr;
										}

										if (tool_calls.isEmpty()) {
											LOGGER.info("Breaking recursive toolcall since we had no tools being called");
											return swr;
										}

										_exec_depth_counter++;
										LOGGER.info("Recursive call at depth " + _exec_depth_counter + " to askStrictChatQuestion(), now sleeping " + _toolcall_pausetime_in_seconds + " seconds until next toolcall");
										SystemUtils.sleepInSeconds(_toolcall_pausetime_in_seconds);

										//System.out.println("sb INIT:\n-------------------------------");
										//System.out.println(sb.toString());
										//System.out.println("sb INIT:\n-------------------------------");
										//System.exit(1);

										// Let's start over
										//this.clearChatHistory(); // reship full_prompt instead
										return askStrictChatQuestion(sb_toolcallresulthistory.toString() + "\n" + "### NEW PROMPT ###\n" + _prompt + "\n### END NEW PROMPT ###\n",_recursive_question, session_tokens_maxlen, _hide_llm_reply_if_uncertain, _retryThreshold, _timeout_seconds, _exec_depth_counter, _max_recursive_toolcall_depth, _toolcall_pausetime_in_seconds, _return_toolcall, _halt_on_tool_error, _history_file, _unloadModelAfterQuery, _debug, _mcp_preprocess, _prompt_logging);
									}
								}

								return swr;
							} else {
								LOGGER.warn("swr response is null, giving up with model " + this.getModel_name());
								LOGGER.warn("Received an invalid JSON reply: " + json);
								swr = OllamaUtils.applyResponseSanity(null, model_name, _hide_llm_reply_if_uncertain);
								return swr;
							}
						} else {
							LOGGER.warn("swr is null, giving up with model " + this.getModel_name());
							LOGGER.warn("Received an invalid JSON reply: " + json);
							swr = OllamaUtils.applyResponseSanity(null, model_name, _hide_llm_reply_if_uncertain);
							return swr;
						}
					}
					retryCounter++;
					if (retryCounter > 5) LOGGER.warn("Having problems getting a valid reply using this question: " + full_prompt);
					SystemUtils.sleepInSeconds(1); // throttle
				}
			}
		} else {
			LOGGER.error("You cannot ask STRICTPROTOCOL questions to a session of type " + this.getSessiontype());
			SystemUtils.halt();
		}
		return new SingleStringQuestionResponse();
	}


	public String askRawChatQuestion(String _untrusted_input_prompt, long _timeout_in_seconds) {
		if (!_untrusted_input_prompt.endsWith("?")) _untrusted_input_prompt = _untrusted_input_prompt + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			return "";
		} else {
			if (settings.isMcp_enable_promptinject_protection()) {
				String sanitized = OllamaUtils.sanitizePromptInjection(_untrusted_input_prompt, "PROMPT_INJECTION_ATTACK_IDENTIFIED");
				if (!sanitized.equals(_untrusted_input_prompt)) {
					LOGGER.warn("Prompt injection attack found in _untrusted_input_prompt");
					LOGGER.warn("_untrusted_input_prompt: " + _untrusted_input_prompt);
					LOGGER.info("sanitized output: " + sanitized);
					return "PROMPT_INJECTION_ATTACK_IDENTIFIED";
				}
			}

			while (true) {
				ChatInteraction ci =  OllamaUtils.askRawChatQuestion(this.Ollama, this.model_name, this.options, this.chatResult, _untrusted_input_prompt, _timeout_in_seconds);
				if (null != ci) {
					return ci.getResponse();
				} else {
					return null;
				}
			}
		}
	}

	public String askRawChatQuestionWithCustomChatHistory(String _question, List<OllamaChatMessage> _customChatHistory, long _timeout_in_seconds) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			return "";
		} else {
			int retryCounter = 0;
			while (true) {
				ChatInteraction ci =  OllamaUtils.askRawChatQuestionWithCustomChatHistory(this.Ollama, this.model_name, this.options, this.chatResult, _question, _customChatHistory, _timeout_in_seconds);
				if (null != ci) return ci.getResponse();
				retryCounter++;
				if (retryCounter > 5) LOGGER.warn("Having problems getting a valid reply using this question: " + _question);
				SystemUtils.sleepInSeconds(1); // throttle
			}
		}
	}

	public Options getOptions() {
		return options;
	}

	public void setOptions(Options options) {
		this.options = options;
	}

	public OllamaDramaSettings getSettings() {
		return settings;
	}

	public void setSettings(OllamaDramaSettings settings) {
		this.settings = settings;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public OllamaEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(OllamaEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Returns the text content of an OllamaChatMessage, regardless of which
	 * ollama4j version is on the classpath. Different versions expose chat
	 * text under different method names: older versions have only getResponse(),
	 * newer ones added getContent() / getMessage() / getText(). We try each
	 * via reflection and use the first non-empty result.
	 */
	private String getMessageText(OllamaChatMessage cm) {
		if (cm == null) return "";

		// Try alternative accessors via reflection - newer ollama4j versions
		// may expose message text under a different method name. Order matters:
		// most likely first.
		for (String methodName : new String[] { "getContent", "getMessage", "getText" }) {
			try {
				java.lang.reflect.Method m = cm.getClass().getMethod(methodName);
				Object result = m.invoke(cm);
				if (result instanceof String) {
					String s = (String) result;
					if (!s.isEmpty()) return s;
				}
			} catch (NoSuchMethodException e) {
				// not present on this version - try next
			} catch (Exception e) {
				// reflection failure - try next
			}
		}

		// Standard fallback: getResponse(). Always present in current ollama4j.
		try {
			String response = cm.getResponse();
			if (response != null) return response;
		} catch (Throwable t) {
			// ignore
		}

		return "";
	}

	public int getChatSizeCharCount() {
		try {
			StringBuffer sb = new StringBuffer();
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				sb.append(getMessageText(cm));
			}
			return sb.toString().length();
		} catch (Exception e) {
			LOGGER.warn("getChatSizeCharCount failed (returning 0)", e);
			return 0;
		}
	}

	public int getChatSizeWordCount() {
		int tokenCount = 0;
		try {
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				tokenCount = tokenCount + estimateTokenCount(getMessageText(cm));
			}
			return tokenCount;
		} catch (Exception e) {
			// FIXED: was silently returning 0, which made the trim threshold
			// always pass and let history grow unbounded. Log loudly now.
			LOGGER.warn("getChatSizeWordCount failed (returning 0 - trim logic will be wrong this round)", e);
			return 0;
		}
	}

	/**
	 * FIXED: retuned for dense JSON / tool-call / code content. Previously used
	 * chars/3.8 which was reasonable for plain English but undercounts MCP tool
	 * schemas badly (every {, ", :, , is often its own token).
	 *
	 * For Gemma-grade tokenizers we observe roughly:
	 *   - plain prose:  ~chars/4   (~1.4 tokens/word)
	 *   - mixed/JSON:   ~chars/3.0
	 *   - dense JSON:   ~chars/2.5
	 *
	 * We use chars/2.8 + words*1.6 + 50 buffer as a conservative upper bound.
	 * Better-but-not-perfect; for true accuracy plug in jtokkit or a Gemma
	 * tokenizer.
	 */
	public int estimateTokenCount(String text) {
		if (text == null || text.isEmpty()) return 0;

		int words = text.split("\\s+").length;
		int chars = text.length();

		return Math.max(
				(int) (words * 1.6),
				(int) (chars / 2.8)
				) + 50;
	}

	public String getChatHistory() {
		StringBuffer sb = new StringBuffer();
		try {
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				sb.append(getMessageText(cm));
			}
			return sb.toString();
		} catch (Exception e) {
			LOGGER.warn("getChatHistory failed (returning empty string)", e);
			return "";
		}
	}

	public void clearChatHistory() {
		try {
			this.getChatResult().getChatHistory().clear();
		} catch (Exception e) {
			LOGGER.warn("clearChatHistory failed", e);
		}
	}

	public SessionType getSessiontype() {
		return sessiontype;
	}

	public void setSessiontype(SessionType sessiontype) {
		this.sessiontype = sessiontype;
	}

	public boolean isMake_tools_available() {
		return make_tools_available;
	}

	public void setMake_tools_available(boolean make_tools_available) {
		this.make_tools_available = make_tools_available;
	}

	public static String sanitizeToolCallQuotes(String tool_calls_csv) {
	    if (tool_calls_csv == null) return tool_calls_csv;
	    // Replace key='value' with key="value"
	    return tool_calls_csv.replaceAll("='([^']*)'", "=\"$1\"");
	}

	public String getSessionid() {
		return sessionid;
	}

	public void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}

	public String getToolcall_history() {
		return toolcall_history;
	}

	public void addToToolcall_history(String _newentry) {
		this.toolcall_history = this.toolcall_history + "\n - " + DateUtils.epochInSecondsToUTC(System.currentTimeMillis()/1000L) + ": " + _newentry;
		System.out.println("toolcall_history for session " + this.getSessionid() + " : " + this.toolcall_history);
	}

}