package ntt.security.ollamadrama.objects.sessions;

import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.JSONUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.SystemUtils;

public class OpenAISession {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAISession.class);

	private String model_name;
	private OpenAiService service;
	private OllamaDramaSettings settings;
	private String uuid;

	public OpenAISession(String _model_name, OpenAiService service, OllamaDramaSettings _settings) {
		super();

		this.model_name = _model_name;
		this.service = service;
		this.settings = _settings;	
		this.uuid = UUID.randomUUID().toString();
	}

	public String getModel_name() {
		return model_name;
	}

	public void setModel_name(String model_name) {
		this.model_name = model_name;
	}

	public SingleStringQuestionResponse askChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain) {
		if (!_question.endsWith("?")) _question = _question + "?";
		if (null == this.service) {
			LOGGER.warn("openAI service is null!");
			SingleStringQuestionResponse swr = OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
			return swr;
		} else {
			int retryCounter = 0;
			while (retryCounter <= 3) {

				try {

					// Define a user message (the user's question)
					ChatMessage userMessage = new ChatMessage();
					userMessage.setRole("user");
					userMessage.setContent(_question + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + Globals.LOGIC_TEMPLATE );
					
					// Create the chat completion request with the system and user messages
					ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
							.model(this.model_name)
							.messages(Arrays.asList(userMessage)) // Add system and user messages
							.temperature(0.0)  // Controls the creativity of the response (0.0 to 1.0)
							.topP(0.0)  // Controls nucleus sampling, balancing diversity (0.0 to 1.0)
							.build();

					String response = service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage().getContent();
					int firstBraceIndex = response.indexOf('{');
					String json = response.substring(firstBraceIndex);;
					
					SingleStringQuestionResponse swr = JSONUtils.createPOJOFromJSONOpportunistic(json, SingleStringQuestionResponse.class);
					if (null != swr) {

						if (null != swr.getResponse()) {
							
							// JSON protocol hack (LLM helper)
							if (swr.getResponse().equals("FAILTOUNDERSTAND")) {
								swr.setProbability(0);
							}

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
					}
					retryCounter++;
					if (retryCounter > 5) LOGGER.warn("Having problems getting a valid reply using this question: " + _question);

				} catch (Exception e) {
					LOGGER.warn("Exception: " + e.getMessage() + " when making query against model " + this.model_name);
					SystemUtils.sleepInSeconds(10); // extra API throttle
					retryCounter++;
				}

				SystemUtils.sleepInSeconds(1); // throttle
			}
		}
		
		return OllamaUtils.applyResponseSanity(null, model_name, _hide_llm_reply_if_uncertain);
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

}
