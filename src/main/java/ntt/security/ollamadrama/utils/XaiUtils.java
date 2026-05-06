package ntt.security.ollamadrama.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.OpenAIEnsemble;
import ntt.security.ollamadrama.objects.OpenAIWrappedSession;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;
import ntt.security.ollamadrama.singletons.XaiService;

/**
 * Convenience wrappers for running ensemble queries against xAI (Grok).
 *
 * Mirrors {@link OpenAIUtils} but routes session construction through {@link XaiService}
 * so the underlying OpenAI-compatible client is pointed at https://api.x.ai/v1 with the
 * caller's xAI API key. The session class itself ({@link OpenAISession}) is unchanged —
 * the wire protocol is identical.
 */
public class XaiUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(XaiUtils.class);

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, String _models, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain) {
		XaiService.getInstance(_settings);

		OpenAIEnsemble e1 = new OpenAIEnsemble();
		for (String model_name : _models.split(",")) {
			if (model_name == null || model_name.trim().isEmpty()) continue;
			OpenAISession a1 = XaiService.getStrictSession(model_name.trim(), _settings);
			LOGGER.info("Using xAI model {}", model_name.trim());
			e1.addWrappedSession(new OpenAIWrappedSession(a1, Globals.MODEL_PROBABILITY_THRESHOLDS.get(model_name.trim())));
		}
		return e1.askChatQuestion(_query, _hide_llm_reply_if_uncertain);
	}

}
