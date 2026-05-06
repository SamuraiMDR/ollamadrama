package ntt.security.ollamadrama.objects.sessions;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.JSONUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.SystemUtils;

public class OpenAISession {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAISession.class);

	private String model_name;
	private OpenAIClient client;
	private OllamaDramaSettings settings;
	private String uuid;

	public OpenAISession(String _model_name, OpenAIClient client, OllamaDramaSettings _settings) {
		super();

		this.model_name = _model_name;
		this.client = client;
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
		if (null == this.client) {
			LOGGER.warn("openAI client is null!");
			SingleStringQuestionResponse swr = OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
			return swr;
		} else {
			int retryCounter = 0;
			while (retryCounter <= 3) {

				try {

					String user_content = _question
							+ Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS
							+ Globals.LOGIC_TEMPLATE;

					ChatCompletionCreateParams.Builder param_builder = ChatCompletionCreateParams.builder()
							.model(this.model_name)
							.addUserMessage(user_content);

					// o1/o3/o4 reasoning models reject temperature and top_p (only the default 1.0
					// is accepted). Setting them yields HTTP 400. Standard chat models — including
					// gpt-4*, gpt-5* — accept both, so we keep the deterministic 0.0 there.
					if (!isReasoningModel(this.model_name)) {
						param_builder = param_builder
								.temperature(0.0)
								.topP(0.0);
					}

					ChatCompletionCreateParams params = param_builder.build();

					ChatCompletion completion = client.chat().completions().create(params);
					String response = completion.choices().get(0).message().content().orElse("");
					int firstBraceIndex = response.indexOf('{');
					if (firstBraceIndex < 0) {
						LOGGER.warn("No JSON found in response from model " + this.model_name);
						retryCounter++;
						SystemUtils.sleepInSeconds(1);
						continue;
					}
					String json = response.substring(firstBraceIndex);

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
					}
					retryCounter++;
					if (retryCounter >= 3) LOGGER.warn("Having problems getting a valid reply using this question: " + _question);

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

	public static boolean isReasoningModel(String model_name) {
		if (model_name == null) return false;
		String name = model_name.trim().toLowerCase(java.util.Locale.ROOT);
		return name.startsWith("o1")
				|| name.startsWith("o3")
				|| name.startsWith("o4");
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
