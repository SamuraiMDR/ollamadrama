package ntt.security.ollamadrama.objects.sessions;

import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.JSONUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.SystemUtils;

public class ClaudeSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeSession.class);

	private String model_name;
	private AnthropicClient client;
	private OllamaDramaSettings settings;
	private String uuid;

	public ClaudeSession(String _model_name, AnthropicClient _client, OllamaDramaSettings _settings) {
		super();

		this.model_name = _model_name;
		this.client = _client;
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
			LOGGER.warn("Claude client is null!");
			return OllamaUtils.applyResponseSanity(null, this.model_name, _hide_llm_reply_if_uncertain);
		}

		int retryCounter = 0;
		while (retryCounter <= 3) {

			try {

				String prompt = _question + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + Globals.LOGIC_TEMPLATE;

				MessageCreateParams params = MessageCreateParams.builder()
						.model(this.model_name)
						.maxTokens(4096L)
						.addUserMessage(prompt)
						.build();

				Message message = client.messages().create(params);

				String response = message.content().stream()
						.flatMap(block -> block.text().stream())
						.map(textBlock -> textBlock.text())
						.collect(Collectors.joining());

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
