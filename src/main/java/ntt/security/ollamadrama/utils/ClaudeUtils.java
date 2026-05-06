package ntt.security.ollamadrama.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ClaudeEnsemble;
import ntt.security.ollamadrama.objects.ClaudeWrappedSession;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.sessions.ClaudeSession;
import ntt.security.ollamadrama.singletons.ClaudeService;

public class ClaudeUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeUtils.class);

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, String _models, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain) {

		// Launch singleton
		ClaudeService.getInstance(_settings);

		// Populate an ensemble of agents
		ClaudeEnsemble e1 = new ClaudeEnsemble();
		for (String model_name : _models.split(",")) {

			// Launch strict agent per included model type
			ClaudeSession a1 = ClaudeService.getStrictSession(model_name, _settings);
			LOGGER.info("Using model " + model_name);
			e1.addWrappedSession(new ClaudeWrappedSession(a1, Globals.MODEL_PROBABILITY_THRESHOLDS.get(model_name)));
		}
		SingleStringEnsembleResponse ensemble_response = e1.askChatQuestion(_query, _hide_llm_reply_if_uncertain);
		return ensemble_response;
	}

}
