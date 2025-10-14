package ntt.security.ollamadrama.objects.sessions;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.utils.Options;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ChatInteraction;
import ntt.security.ollamadrama.objects.MCPTool;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.SessionType;
import ntt.security.ollamadrama.objects.ToolCallRequest;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.singletons.OllamaService;
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
	private OllamaAPI ollamaAPI;
	private OllamaChatResult chatResult;
	private Options options;
	private OllamaDramaSettings settings;
	private String uuid;
	private boolean initialized = false;
	private SessionType sessiontype;
	private int interactcounter = 0;
	private String initial_prompt = "";

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

	public int getInteractcounter() {
		return interactcounter;
	}

	public void setInteractcounter(int interactcounter) {
		this.interactcounter = interactcounter;
	}

	public String getInitial_prompt() {
		return initial_prompt;
	}

	public void setInitial_prompt(String initial_prompt) {
		this.initial_prompt = initial_prompt;
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

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _make_tools_available, int _max_recursive_toolcall_depth) {
		return askStrictChatQuestion(_question, false, 3000L, _make_tools_available, _max_recursive_toolcall_depth);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question,  int session_tokens_maxlen, boolean _make_tools_available) {
		return askStrictChatQuestion(_question, session_tokens_maxlen, false, 30, 120, _make_tools_available, 0, 0, 30);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, long _timeout) {
		return askStrictChatQuestion(_question, false, _timeout, false, 5);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, long _timeout) {
		return askStrictChatQuestion(_question, _hide_llm_reply_if_uncertain, _timeout, false, 5);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, int _retryThreshold, long _timeout, boolean _make_tools_available) {
		return askStrictChatQuestion(_question, 60000, _hide_llm_reply_if_uncertain, _retryThreshold, _timeout, _make_tools_available, 0, 5, 30);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, long _timeout, boolean _make_tools_available, int _max_recursive_toolcall_depth) {
		return askStrictChatQuestion(_question, 60000, _hide_llm_reply_if_uncertain, 30, _timeout, _make_tools_available, 0, _max_recursive_toolcall_depth, 30);
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, int session_tokens_maxlen, boolean _hide_llm_reply_if_uncertain, int _retryThreshold, long _timeout, boolean _make_tools_available, int _exec_depth_counter, int _max_recursive_toolcall_depth, int _toolcall_pausetime_in_seconds) {
		if (_max_recursive_toolcall_depth < 0) LOGGER.warn("No tools will be called if value of_max_recursive_toolcall_depth is not 1 or more");
		boolean debug = false;
		if (debug) System.out.println("interactcounter: " + interactcounter);

		this.interactcounter = interactcounter + 1;

		if (this.sessiontype == SessionType.STRICTPROTOCOL) {

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

							if (null != swr.getResponse()) {
								// PROTOCOL hack
								if (swr.getResponse().equals("FAILTOUNDERSTAND")) {
									swr.setProbability(0);
								}

								this.chatResult = ci.getChatResult();
								swr.setEmpty(false);
								swr = OllamaUtils.applyResponseSanity(swr, model_name, _hide_llm_reply_if_uncertain);

								boolean valid_tool_calls = true;
								if (!swr.getTool_calls().equals("")) {
									ArrayList<ToolCallRequest> tool_calls = MCPUtils.parseToolCalls(swr.getTool_calls());
									for (ToolCallRequest tcr: tool_calls) {
										MCPTool mcpTool = OllamaService.getMCPURLForTool(tcr.getToolname());
										if (null == mcpTool) {
											valid_tool_calls = false;
										}
									}
								} else {
									valid_tool_calls = false;
								}

								// print intermediate result to STDOUT
								swr.print();
								System.out.println("");

								if ("TOOLCALL_AFTER_PAUSE".equals(swr.getResponse()) && valid_tool_calls) {
									LOGGER.info("Pausing )" + _toolcall_pausetime_in_seconds + " before next round of toolcalls");
									SystemUtils.sleepInSeconds(_toolcall_pausetime_in_seconds);
								}

								if ("TOOLCALL_AFTER_PAUSE".equals(swr.getResponse()) && !valid_tool_calls) {

									// print intermediate result to STDOUT
									swr.print();
									System.out.println("");

									LOGGER.info("Pausing " + _toolcall_pausetime_in_seconds + " before next round");
									SystemUtils.sleepInSeconds(_toolcall_pausetime_in_seconds);
								}

								if (_make_tools_available && ("TOOLCALL".equals(swr.getResponse()) || "TOOLCALL_AFTER_PAUSE".equals(swr.getResponse()))) {

									if (!valid_tool_calls) {
										LOGGER.warn("Agent requested a tool_call which does not exist (" + swr.getTool_calls() + ")\n");
										return swr;
									} else {

										StringBuffer sb = new StringBuffer();

										LOGGER.debug("Tool Call Request: " + swr.getTool_calls());
										ArrayList<ToolCallRequest> tool_calls = MCPUtils.parseToolCalls(swr.getTool_calls());

										for (ToolCallRequest tcr: tool_calls) {
											if (!tcr.sanitycheck_pass()) {
												LOGGER.warn("Tool Call Request sanitycheck failed - name: " + tcr.getToolname() + " calltype: " + tcr.getCalltype() + " arguments:" + tcr.getArguments().toString());
												return swr;
											} else {

												// Find the MCP URL to call the tool
												MCPTool mcpTool = OllamaService.getMCPURLForTool(tcr.getToolname());
												if (null == mcpTool) {
													LOGGER.error("Agent requested a tool_call which does not exist (" + tcr.getToolname() + ")\n");
													SystemUtils.halt();
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
														LOGGER.debug("Trusted mcp toolname so allowing agent to run the tool call " + tcr.getToolname() + " arguments:" + tcr.getArguments().toString());
													} else {
														make_call = InteractUtils.getYNResponse("The agent is requesting to run the tool call " + tcr.getToolname() + " arguments:" + tcr.getArguments().toString() + ", press Y to allow and N to abort.", settings);
													}

													// Call tool
													if (make_call) {
														CallToolResult result = MCPUtils.callToolUsingMCPEndpoint(mcpURL, mcpPATH, tcr.getToolname(), tcr.getArguments(), 30L);
														String tool_response = "\nResponse from running tool_call " + tcr.getRawrequest() + ":\n\n" + MCPUtils.prettyPrint(result);
														if (null != tool_response) {
															// replace sequences of 10 or more consecutive whitespaces with a single space
															tool_response = tool_response.replaceAll("\\s{10,}", " ");
															//System.out.println(tool_response);
															sb.append(tool_response + "\n");
														}
													} else {
														LOGGER.info("Not making MCP call ..");
														sb.append("Your MCP Tool call to " + tcr.getToolname() + " was rejected\n");
													}

												} else {
													LOGGER.warn("Unable to call tool " + tcr.getToolname() + " with MCP URL " + mcpURL);
													return swr;
												}
											}
										}

										// check if we need to break due to token usage
										int chatsize_wordcount_a1 = this.getChatSizeWordCount();
										if (debug) LOGGER.info("session wordcount: " + chatsize_wordcount_a1);
										if (chatsize_wordcount_a1 > session_tokens_maxlen) {
											LOGGER.info("Breaking recursive toolcall since we have consumed roughly token " + chatsize_wordcount_a1);
											return swr;
										}

										// Early exit on recurisive tool calls
										if (_exec_depth_counter >= _max_recursive_toolcall_depth) {
											LOGGER.info("Breaking recursive toolcall since we are at depth " + _exec_depth_counter);
											return swr;
										}

										_exec_depth_counter++;
										//System.out.println("new query:\n\n" + _question + sb.toString());
										return askStrictChatQuestion(_question+ "\n\n" + sb.toString() , session_tokens_maxlen, _hide_llm_reply_if_uncertain, _retryThreshold, _timeout, _make_tools_available, _exec_depth_counter, _max_recursive_toolcall_depth, _toolcall_pausetime_in_seconds);
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
