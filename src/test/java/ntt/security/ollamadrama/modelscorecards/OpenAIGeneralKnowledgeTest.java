package ntt.security.ollamadrama.modelscorecards;

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
public class OpenAIGeneralKnowledgeTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIGeneralKnowledgeTest.class);

	@Test
	public void simpleDomain2CompanyKnowledge_OpenAIModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {

			HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
				this.put("Google", 2);
				this.put("google", 1);
				this.put("Google Inc.", 2);
				this.put("Google LLC", 2);
				this.put("Alphabet Inc.", 2);
				this.put("Alphabet", 2);
			}};

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIModels(
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"What company or organization is associated with google.com? Reply with only the name",
					acceptable_answers, true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			gpt-4o-mini                              pos: 2   neg: 0  
			gpt-4                                    pos: 2   neg: 0  
			gpt-3.5-turbo                            pos: 2   neg: 0  
			gpt-4-turbo                              pos: 2   neg: 0  
			gpt-4o                                   pos: 2   neg: 0  
			----- 
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER2));
		} else {
			LOGGER.info("OpenAI key not set so skipping");
		}

	}

	@Test
	public void simpleParis_OpenAIModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIModels(
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"Is the capital city of France named Paris? Reply with Yes or No.",
					"Yes", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			gpt-4o-mini                              pos: 1   neg: 0  
			gpt-4                                    pos: 1   neg: 0  
			gpt-4-turbo                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  

			gpt-3.5-turbo                            pos: 0   neg: 1  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
		} else {
			LOGGER.info("OpenAI key not set so skipping");
		}

	}

	@Test
	public void simpleNonsenseInput_OpenAIModels() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIModels(
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings, 
					"Is the capital city of OOikiOOA named Mo1rstiooooo? Reply with Yes or No.",
					"LOWPROBA", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*		
			gpt-4o-mini                              pos: 1   neg: 0  
			gpt-4                                    pos: 1   neg: 0  
			gpt-3.5-turbo                            pos: 1   neg: 0  
			gpt-4-turbo                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
		} else {
			LOGGER.info("OpenAI key not set so skipping");
		}
	}

	@Test
	public void simpleStrawberryRCountv1_OpenAIModels() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIModels(
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings, 
					"Count the number of R in strawberry. Answer with number only", 
					"3", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			gpt-4o-mini                              pos: 0   neg: 1  
			gpt-4                                    pos: 0   neg: 1  
			gpt-3.5-turbo                            pos: 0   neg: 1  
			gpt-4-turbo                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
		} else {
			LOGGER.info("OpenAI key not set so skipping");
		}
	}

	@Test
	public void simpleStrawberryRCountv2_OpenAIModels() {
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIModels(
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings, "Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
							+ "Walk through each character in the word while counting and make note of the string index they occur at. "
							+ "Include the string index records in your motivation. Answer with number only", 
							"3", true);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			gpt-4o-mini                              pos: 0   neg: 1  
			gpt-4                                    pos: 0   neg: 1  
			gpt-3.5-turbo                            pos: 0   neg: 1  
			gpt-4-turbo                              pos: 0   neg: 1  
			
			gpt-4o                                   pos: 1   neg: 0  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
		} else {
			LOGGER.info("OpenAI key not set so skipping");
		}
	}

}
