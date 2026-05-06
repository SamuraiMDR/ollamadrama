package ntt.security.ollamadrama.strict.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;

@SuppressWarnings("serial")
public class ClaudeGeneralKnowledgeTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeGeneralKnowledgeTest.class);

	@Test
	public void simpleDomain2CompanyKnowledge_ClaudeModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			settings.setUse_claude(true);

			HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
				this.put("Google", 2);
				this.put("google", 1);
				this.put("Google Inc.", 2);
				this.put("Google LLC", 2);
				this.put("Alphabet Inc.", 2);
				this.put("Alphabet", 2);
			}};

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForClaudeModels(
					false, // use MCP
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					"What company or organization is associated with google.com? Reply with only the name",
					acceptable_answers, true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			claude-opus-4-6                          pos: 2   neg: 0
			claude-sonnet-4-6                        pos: 2   neg: 0
			claude-haiku-4-5                         pos: 2   neg: 0
			-----
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for tier1 Claude model", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_CLAUDE_TIER1));
		} else {
			LOGGER.info("Claude key not set so skipping");
		}

	}

	@Test
	public void simpleParis_ClaudeModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			settings.setUse_claude(true);

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForClaudeModels(
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					"Is the capital city of France named Paris? Reply with Yes or No.",
					"Yes", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			claude-opus-4-6                          pos: 1   neg: 0
			claude-sonnet-4-6                        pos: 1   neg: 0
			claude-haiku-4-5                         pos: 1   neg: 0
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all Claude models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_CLAUDE_ALL));
		} else {
			LOGGER.info("Claude key not set so skipping");
		}

	}

	@Test
	public void simpleNonsenseInput_ClaudeModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			settings.setUse_claude(true);

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForClaudeModels(
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					"Is the capital city of OOikiOOA named Mo1rstiooooo? Reply with Yes or No.",
					"LOWPROBA", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			claude-opus-4-6                          pos: 1   neg: 0
			claude-sonnet-4-6                        pos: 1   neg: 0
			claude-haiku-4-5                         pos: 1   neg: 0
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all Claude models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_CLAUDE_ALL));
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void simpleStrawberryRCount_ClaudeModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			settings.setUse_claude(true);

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForClaudeModels(
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					"Count the number of R in strawberry. Answer with number only",
					"3", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			claude-opus-4-6                          pos: 1   neg: 0
			claude-sonnet-4-6                        pos: 1   neg: 0
			claude-haiku-4-5                         pos: 1   neg: 0
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all Claude models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_CLAUDE_ALL));
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

	@Test
	public void simpleStrawberryRCountDetailed_ClaudeModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getClaudekey().length() > 10) {
			settings.setUse_claude(true);

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForClaudeModels(
					Globals.MODEL_NAMES_CLAUDE_ALL,
					settings,
					"Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
					+ "Walk through each character in the word while counting and make note of the string index they occur at. "
					+ "Include the string index records in your motivation. Answer with number only",
					"3", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			claude-opus-4-6                          pos: 1   neg: 0
			claude-sonnet-4-6                        pos: 1   neg: 0
			claude-haiku-4-5                         pos: 1   neg: 0
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all Claude models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_CLAUDE_ALL));
		} else {
			LOGGER.info("Claude key not set so skipping");
		}
	}

}
