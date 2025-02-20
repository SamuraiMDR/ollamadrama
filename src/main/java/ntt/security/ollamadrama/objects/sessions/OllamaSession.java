package ntt.security.ollamadrama.objects.sessions;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessage;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;
import io.github.amithkoujalgi.ollama4j.core.utils.Options;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ChatInteraction;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.JSONUtils;
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

	public OllamaSession(String _model_name, OllamaEndpoint _endpoint, Options _options, OllamaDramaSettings _settings, String _profilestatement) {
		super();

		this.model_name = _model_name;
		this.endpoint = _endpoint;
		this.ollamaAPI = OllamaUtils.createConnection(_endpoint, _settings.getOllama_timeout());
		this.options = _options;
		this.settings = _settings;
		this.uuid = UUID.randomUUID().toString();

		this.initialized = setChatSystemProfileStatement(_profilestatement);		
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

	public boolean setChatSystemProfileStatement(String _profile_statement) {
		int errorCount = 0;
		if (null == this.chatResult) {
			while (null == this.chatResult) {
				OllamaChatResult res = OllamaUtils.setChatSystemProfile(this.ollamaAPI, this.model_name, this.options, _profile_statement);
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

	public void provideChatStatement(String _statement) {
		if (!_statement.endsWith(".")) _statement = _statement + ".";
		if (null == this.chatResult) {
			LOGGER.warn("You need to initialize a chat session with a profile statement first");
		} else {
			ChatInteraction ci = OllamaUtils.addStatementToExistingChat(this.ollamaAPI, this.model_name, this.options, this.chatResult, _statement);
			if (null != ci) {
				LOGGER.debug("Successfully extended chat session with results from STATEMENT interaction");
				this.chatResult = ci.getChatResult();
			}
		}
	}

	public SingleStringQuestionResponse askStrictChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			SingleStringQuestionResponse swr = OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
			return swr;
		} else {
			int retryCounter = 0;
			while (true) {
				ChatInteraction ci =  OllamaUtils.askChatQuestion(this.ollamaAPI, this.model_name, this.options, this.chatResult, _question);
				if (null != ci) {
					String json = "";

					// JSON markdown (LLM protocol helper hack)
					if (ci.getResponse().contains("{") && ci.getResponse().contains("}")) {
						json = "{" + ci.getResponse().split("\\{")[1];
						json = json.split("\\}")[0] + "}";
					} else {
						json = ci.getResponse();
					}
					
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
	}

	public String askRawChatQuestion(String _question) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.chatResult) {
			LOGGER.warn("chatResult is null!");
			return "";
		} else {
			int retryCounter = 0;
			while (true) {
				ChatInteraction ci =  OllamaUtils.askRawChatQuestion(this.ollamaAPI, this.model_name, this.options, this.chatResult, _question);
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

}
