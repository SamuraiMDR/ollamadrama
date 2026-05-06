package ntt.security.ollamadrama.strict.ensemblevotes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.singletons.ClaudeService;
import ntt.security.ollamadrama.utils.ClaudeUtils;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;

public class ClaudeStandardEnsemble {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeStandardEnsemble.class);

	@Test
	public void strict_ENSEMBLE_DOMAIN_Claude() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			ClaudeService.getInstance(settings);
			SingleStringEnsembleResponse sser = ClaudeUtils.strictEnsembleRun(
					"Is a known company or organization associated with the domain global.ntt? Reply with only the name.",
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					true);
			sser.printEnsemble();
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void strict_ENSEMBLE_Paris_Claude() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			SingleStringEnsembleResponse sser = ClaudeUtils.strictEnsembleRun(
					"Is Paris the capital of France? Answer with only Yes or No.",
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					true);
			sser.printEnsemble();

			/*
			uniq confident responses: 1
			response #1: Yes
			 - claude-opus-4-6::...
			 - claude-sonnet-4-6::...
			 - claude-haiku-4-5::...
			 */

			// Assert
			String best_response = sser.getBestResponse(1);
			assertEquals("All Claude models should agree on Paris", "Yes", best_response);
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void strict_ENSEMBLE_Domain2CompanyKnowledge_Claude() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			SingleStringEnsembleResponse sser = ClaudeUtils.strictEnsembleRun(
					"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					true);
			sser.printEnsemble();

			/*
			uniq confident responses: 1
			response #1: NTT
			 - claude-opus-4-6::...
			 - claude-sonnet-4-6::...
			 - claude-haiku-4-5::...
			 */

			// Assert
			String best_response = sser.getBestResponse(1);
			assertEquals("Make sure the winning company name is simply 'NTT'", "NTT", best_response);
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void strict_ENSEMBLE_OllamaClaudeCombo_Domain2CompanyKnowledge() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			SingleStringEnsembleResponse sser = OllamaDramaUtils.collectEnsembleVotes(
					"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					true, false);
			sser.printEnsemble();

			// Assert
			String best_response = sser.getBestResponse(5);
			assertEquals("Make sure the winning company name is simply 'NTT'", "NTT", best_response);
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void strict_ENSEMBLE_ClaudeOnly_CollectVotes() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			SingleStringEnsembleResponse sser = OllamaDramaUtils.collectClaudeEnsembleVotes(
					"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					true);
			sser.printEnsemble();

			/*
			uniq confident responses: 1
			response #1: NTT
			 - claude-opus-4-6::...
			 - claude-sonnet-4-6::...
			 - claude-haiku-4-5::...
			 */

			// Assert
			String best_response = sser.getBestResponse(1);
			assertEquals("Make sure the winning company name is simply 'NTT'", "NTT", best_response);
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

}
