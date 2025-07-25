package ntt.security.ollamadrama.objects.sessions;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessage;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;
import io.github.amithkoujalgi.ollama4j.core.utils.Options;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ChatInteraction;
import ntt.security.ollamadrama.objects.MCPTool;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.SessionType;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.InteractUtils;
import ntt.security.ollamadrama.utils.JSONUtils;
import ntt.security.ollamadrama.utils.MCPUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.SystemUtils;

public class OllamaSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaSession.class);

	private String model_name;
	private OllamaEndpoint endpoint;
	private OllamaAPI ollamaAPI;
	private OllamaChatResult chatResult;
	private Options options;
	private OllamaDramaSettings settings;
	private String uuid;
	private boolean initialized = false;
	private SessionType sessiontype;

	public OllamaSession(String _model_name, OllamaEndpoint _endpoint, Options _options, OllamaDramaSettings _settings, String _profilestatement, SessionType _sessiontype) {
		super();

		this.model_name = _model_name;
		this.endpoint = _endpoint;
		this.ollamaAPI = OllamaUtils.createConnection(_endpoint, _settings.getOllama_timeout());
		this.options = _options;
		this.settings = _settings;
		this.uuid = UUID.randomUUID().toString();
		this.sessiontype = _sessiontype;

		this.initialized = setChatSystemProfileStatement(_profilestatement, _settings.getAutopull_max_llm_size(), _settings.getOllama_timeout());		
	}

	public String getModel_name() {
		return model_name;
	}

	public void setModel_name(String model_name) {
		this.model_name = model_name;
	}

	public OllamaAPI getOllamaAPI() {
		return ollamaAPI;
	}

	public void setOllamaAPI(OllamaAPI ollamaAPI) {
		this.ollamaAPI = ollamaAPI;
	}

	public OllamaChatResult getChatResult() {
		return chatResult;
	}

	public void setChatResult(OllamaChatResult chatResult) {
		this.chatResult = chatResult;
	}

	public String askGenericSingleWordQuestion(String _question) {
		boolean success = false;
		while (!success) {
			String resp = OllamaUtils.askGenericSingleWordQuestion(this.ollamaAPI, this.model_name, this.options, _question);
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
				OllamaChatResult res = OllamaUtils.setChatSystemProfile(this.ollamaAPI, this.model_name, this.options, _profile_statement, _autopull_max_llm_size, _timeout);
				if (null != res) {
					this.chatResult = res;
					return true;
				}
				SystemUtils.sleepInSeconds(2); // throttle
				if (errorCount >= 2) {
					LOGGER.warn("Unable to set system profile for model " + this.model_name);
					return false;
				}
			}
		} else {
			LOGGER.warn("Profile statement already defined, ignoring");
		}
		return false;
	}

	public void provideChatStatement(String _statement, long _timeout) {
		if (!_statement.endsWith(".")) _statement = _statement + ".";
		if (null == this.chatResult) {
			LOGGER.warn("You need to initialize a chat session with a profile statement first");
		} else {
			if (this.getSessiontype() == SessionType.CREATIVE) {
				ChatInteraction ci = OllamaUtils.addCreativeStatementToExistingChat(this.ollamaAPI, this.model_name, this.options, this.chatResult, _statement, _timeout);
				if (null != ci) {
					LOGGER.debug("Successfully extended chat session with results from STATEMENT interaction");
					this.chatResult = ci.getChatResult();
				}
			} else {
				ChatInteraction ci = OllamaUtils.addStrictStatementToExistingChat(this.ollamaAPI, this.model_name, this.options, this.chatResult, _statement, _timeout);
				if (null != ci) {
					LOGGER.debug("Successfully extended chat session with results from STATEMENT interaction");
					this.chatResult = ci.getChatResult();
				}
			}
		}
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _make_tools_available) {
		return askStrictChatQuestion(_question, false, 3000L, _make_tools_available);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, long _timeout) {
		return askStrictChatQuestion(_question, false, _timeout, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, long _timeout) {
		return askStrictChatQuestion(_question, _hide_llm_reply_if_uncertain, _timeout, false);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, long _timeout, boolean _make_tools_available) {
		return askStrictChatQuestion(_question, _hide_llm_reply_if_uncertain, 30, _timeout, _make_tools_available);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, int _retryThreshold, long _timeout, boolean _make_tools_available) {

		if (!_question.endsWith("?")) _question = _question + "?";

		if (_make_tools_available) {
			String available_tool_summary = OllamaService.getAllAvailableMCPTools();
			_question = _question + "\n\n" + available_tool_summary;
		}

		if (this.sessiontype == SessionType.STRICTPROTOCOL) {

			if (!_question.contains("MCP TOOLS AVAILABLE")) _question = _question + "\n\nNO MCP TOOLS AVAILABLE."; 

			System.out.println(_question + "\n");

			if (null == this.chatResult) {
				LOGGER.warn("chatResult is null!");
				SingleStringQuestionResponse swr = OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
				return swr;
			} else {
				int retryCounter = 0;
				while (true) {
					ChatInteraction ci =  OllamaUtils.askChatQuestion(this.ollamaAPI, this.model_name, this.options, this.chatResult, _question, _timeout);
					if (null != ci) {
						String json = "";

						boolean debug = false;
						if (debug) System.out.println(ci.getResponse());

						// JSON markdown (LLM protocol helper hack)
						if (ci.getResponse().contains("{") && ci.getResponse().contains("}") && (json.split("\\}").length == 1) && (json.split("\\{").length == 1)) {
							json = "{" + ci.getResponse().split("\\{")[1];
							json = json.split("\\}")[0] + "}";
						} else {
							json = ci.getResponse();
						}

						// JSON newline fix
						json = json.replace("\n", " ").replace("\r", " ");

						// JSON reply check (LLM protocol helper hack)
						if (json.contains("{") && json.contains("}") && json.contains("\"response\": FAILTOUNDERSTAND,")) {
							json = json.replace("\"response\": FAILTOUNDERSTAND,","\"response\": \"FAILTOUNDERSTAND\",");
						} else if (json.contains("{") && json.contains("}") && json.contains("\"response\": OKIDOKI,")) {
							json = json.replace("\"response\": OKIDOKI,","\"response\": \"OKIDOKI\",");
						}

						SingleStringQuestionResponse swr = JSONUtils.createPOJOFromJSONOpportunistic(json, SingleStringQuestionResponse.class);
						if (null != swr) {

							if (null != swr.getResponse()) {
								// PROTOCOL hack
								if (swr.getResponse().equals("FAILTOUNDERSTAND")) {
									swr.setProbability(0);
								}

								this.chatResult = ci.getChatResult();
								swr.setEmpty(false);
								swr = OllamaUtils.applyResponseSanity(swr, model_name, _hide_llm_reply_if_uncertain);

								if (_make_tools_available && "TOOLCALL".equals(swr.getResponse())) {
									StringBuffer sb = new StringBuffer();
									ArrayList<String> tool_calls = parseToolCalls(swr.getTool_calls());
									for (String tool_call: tool_calls) {
										System.out.println(" - tool_call: " + tool_call);

										String toolname = MCPUtils.parseTool(tool_call);

										// Extract arguments from suggested tool call
										HashMap<String, Object> arguments = MCPUtils.parseArguments(tool_call);

										// Find the MCP URL to call the tool
										MCPTool mcpTool = OllamaService.getMCPURLForTool(toolname);
										if (null == mcpTool) {
											LOGGER.error("Agent requested a tool_call which does not exist\n");
											SystemUtils.halt();
										}
										String mcpURL = mcpTool.getEndpoint().getSchema() + "://" + mcpTool.getEndpoint().getHost() + ":" + mcpTool.getEndpoint().getPort();
										String mcpPATH = mcpTool.getEndpoint().getPath();

										if ( (toolname.length()>0) && (mcpURL.startsWith("http") && mcpPATH.startsWith("/"))) {

											boolean make_call = false;
											if (settings.isMcp_blind_trust()) {
												make_call = true;
												LOGGER.info("Blindly allowing agent to run the tool call " + tool_call);
											} else {
												make_call = InteractUtils.getYNResponse("The agent is requesting to run the tool call " + tool_call + ", press Y to allow and N to abort.", settings);
											}

											// Call tool
											if (make_call) {
												CallToolResult result = MCPUtils.callToolUsingMCPEndpoint(mcpURL, mcpPATH, toolname, arguments, 30L);
												String tool_response = "\nResponse from running tool_call " + tool_call + ":\n\n" + MCPUtils.prettyPrint(result);
												System.out.println(tool_response);
												sb.append(tool_response + "\n");
											} else {
												LOGGER.info("Not making MCP call ..");
											}

										} else {
											LOGGER.error("Unable to call tool " + toolname + " with MCP URL " + mcpURL);
											SystemUtils.halt();
										}
									}
									
									System.out.println("new query:\n\n" + _question + sb.toString());
									return askStrictChatQuestion( _question + sb.toString(), _hide_llm_reply_if_uncertain, _retryThreshold, _timeout, _make_tools_available);
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
					if (retryCounter > 5) LOGGER.warn("Having problems getting a valid reply using this question: " + _question);
					SystemUtils.sleepInSeconds(1); // throttle
				}
			}
		} else {
			LOGGER.error("You cannot ask STRICTPROTOCOL questions to a session of type " + this.getSessiontype());
			SystemUtils.halt();
		}
		return new SingleStringQuestionResponse();
	}

	private ArrayList<String> parseToolCalls(String tool_calls_csv) {
		ArrayList<String> tool_calls = new ArrayList<String>();

		if (tool_calls_csv == null || tool_calls_csv.trim().isEmpty()) {
			System.out.println("The tool_calls CSV string is empty.");
			System.exit(1);
		}

		// Regex to split on commas outside of parentheses and quotes
		String regex = ",\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)(?![^()]*\\))";
		String[] entries = tool_calls_csv.split(regex);

		// Print each entry on a new line, trimmed and clean
		for (String entry : entries) {
			String trimmedEntry = entry.trim();
			if (!trimmedEntry.isEmpty()) {
				tool_calls.add(trimmedEntry);
			}
		}

		return tool_calls;
	}

	public String askRawChatQuestion(String _question, long _timeout) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			return "";
		} else {
			int retryCounter = 0;
			while (true) {
				ChatInteraction ci =  OllamaUtils.askRawChatQuestion(this.ollamaAPI, this.model_name, this.options, this.chatResult, _question, _timeout);
				if (null != ci) return ci.getResponse();
				retryCounter++;
				if (retryCounter > 5) LOGGER.warn("Having problems getting a valid reply using this question: " + _question);
				SystemUtils.sleepInSeconds(1); // throttle
			}
		}
	}

	public String askRawChatQuestionWithCustomChatHistory(String _question, List<OllamaChatMessage> _customChatHistory, long _timeout) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			return "";
		} else {
			int retryCounter = 0;
			while (true) {
				ChatInteraction ci =  OllamaUtils.askRawChatQuestionWithCustomChatHistory(this.ollamaAPI, this.model_name, this.options, this.chatResult, _question, _customChatHistory, _timeout);
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

	public int getChatSizeCharCount() {
		try {
			StringBuffer sb = new StringBuffer();
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				sb.append(cm.getContent());
			}
			return sb.toString().length();
		} catch (Exception e) {
			return 0;
		}
	}

	public int getChatSizeWordCount() {
		int tokenCount = 0;
		try {
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				// Split the input into tokens based on whitespace and basic punctuation.
				String[] tokens = cm.getContent().split("\\s+|(?=[.,!?;:])|(?<=[.,!?;:])");
				tokenCount = tokenCount + tokens.length;
			}
			return tokenCount;
		} catch (Exception e) {
			return 0;
		}
	}

	public String getChatHistory() {
		StringBuffer sb = new StringBuffer();
		try {
			for (OllamaChatMessage cm: this.getChatResult().getChatHistory()) {
				sb.append(cm.getContent());
			}
			return sb.toString();
		} catch (Exception e) {
			return "";
		}
	}

	public SessionType getSessiontype() {
		return sessiontype;
	}

	public void setSessiontype(SessionType sessiontype) {
		this.sessiontype = sessiontype;
	}

}
