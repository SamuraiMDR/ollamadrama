package ntt.security.ollamadrama.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.config.PreparedQueries;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.singletons.OpenAIService;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;

@SuppressWarnings("serial")
public class OpenAIandOllama_KnowledgeTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIandOllama_KnowledgeTest.class);

	@Test
	public void simpleDomain2CompanyKnowledge_AllModels_XL() {

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

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIAndOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"What company or organization is associated with google.com? Reply with only the name",
					acceptable_answers,
					true, false);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			dolphin3:8b                              pos: 2   neg: 0  
			tulu3:70b                                pos: 2   neg: 0  
			nemotron:70b                             pos: 2   neg: 0  
			gpt-4-turbo                              pos: 2   neg: 0  
			qwen2.5:7b                               pos: 2   neg: 0  
			llama3.1:8b                              pos: 2   neg: 0  
			qwen2:7b                                 pos: 2   neg: 0  
			granite3.1-dense:8b                      pos: 2   neg: 0  
			tulu3:8b                                 pos: 2   neg: 0  
			sailor2:20b                              pos: 2   neg: 0  
			gpt-4o-mini                              pos: 2   neg: 0  
			exaone3.5:32b                            pos: 2   neg: 0  
			gemma2:9b                                pos: 2   neg: 0  
			qwen2.5:72b                              pos: 2   neg: 0  
			olmo2:7b                                 pos: 2   neg: 0  
			gemma2:27b                               pos: 2   neg: 0  
			mistral:7b                               pos: 2   neg: 0  
			llama3.3:70b                             pos: 2   neg: 0  
			llama3.1:70b                             pos: 2   neg: 0  
			marco-o1:7b                              pos: 2   neg: 0  
			gpt-4                                    pos: 2   neg: 0  
			gpt-3.5-turbo                            pos: 2   neg: 0  
			phi4:14b                                 pos: 2   neg: 0  
			dolphin-mistral:7b                       pos: 2   neg: 0  
			openhermes:7b-mistral-v2.5-q4_0          pos: 2   neg: 0  
			athene-v2:72b                            pos: 2   neg: 0  
			openchat:7b                              pos: 2   neg: 0  
			gpt-4o                                   pos: 2   neg: 0  
			-----
			llama3.2:3b                              pos: 0   neg: 1  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER2));
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER2));
		}

	}

	@Test
	public void simpleParis_AllModels_XL() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {

			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIAndOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"Is the capital city of France named Paris? Reply with Yes or No.",
					"Yes",
					true, false);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			dolphin3:8b                              pos: 1   neg: 0  
			tulu3:70b                                pos: 1   neg: 0  
			nemotron:70b                             pos: 1   neg: 0  
			gpt-4-turbo                              pos: 1   neg: 0  
			qwen2.5:7b                               pos: 1   neg: 0  
			llama3.1:8b                              pos: 1   neg: 0  
			qwen2:7b                                 pos: 1   neg: 0  
			granite3.1-dense:8b                      pos: 1   neg: 0  
			tulu3:8b                                 pos: 1   neg: 0  
			llama3.2:3b                              pos: 1   neg: 0  
			sailor2:20b                              pos: 1   neg: 0  
			gpt-4o-mini                              pos: 1   neg: 0  
			exaone3.5:32b                            pos: 1   neg: 0  
			gemma2:9b                                pos: 1   neg: 0  
			qwen2.5:72b                              pos: 1   neg: 0  
			olmo2:7b                                 pos: 1   neg: 0  
			gemma2:27b                               pos: 1   neg: 0  
			mistral:7b                               pos: 1   neg: 0  
			llama3.3:70b                             pos: 1   neg: 0  
			llama3.1:70b                             pos: 1   neg: 0  
			marco-o1:7b                              pos: 1   neg: 0  
			gpt-4                                    pos: 1   neg: 0  
			phi4:14b                                 pos: 1   neg: 0  
			dolphin-mistral:7b                       pos: 1   neg: 0  
			openhermes:7b-mistral-v2.5-q4_0          pos: 1   neg: 0  
			athene-v2:72b                            pos: 1   neg: 0  
			openchat:7b                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			-----
			gpt-3.5-turbo                            pos: 0   neg: 1  

			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER2));
		}

	}

	@Test
	public void simpleNonsenseInput_AllModels_XL() {

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIAndOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"Is the capital city of OOikiOOA named Mo1rstiooooo? Reply with Yes or No.",
					"LOWPROBA",
					true, false);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*		
			tulu3:70b                                pos: 1   neg: 0  
			nemotron:70b                             pos: 1   neg: 0  
			gpt-4-turbo                              pos: 1   neg: 0  
			qwen2.5:7b                               pos: 1   neg: 0  
			llama3.1:8b                              pos: 1   neg: 0  
			qwen2:7b                                 pos: 1   neg: 0  
			granite3.1-dense:8b                      pos: 1   neg: 0  
			llama3.2:3b                              pos: 1   neg: 0  
			sailor2:20b                              pos: 1   neg: 0  
			gpt-4o-mini                              pos: 1   neg: 0  
			exaone3.5:32b                            pos: 1   neg: 0  
			gemma2:9b                                pos: 1   neg: 0  
			qwen2.5:72b                              pos: 1   neg: 0  
			olmo2:7b                                 pos: 1   neg: 0  
			gemma2:27b                               pos: 1   neg: 0  
			mistral:7b                               pos: 1   neg: 0  
			llama3.3:70b                             pos: 1   neg: 0  
			llama3.1:70b                             pos: 1   neg: 0  
			marco-o1:7b                              pos: 1   neg: 0  
			gpt-4                                    pos: 1   neg: 0  
			gpt-3.5-turbo                            pos: 1   neg: 0  
			phi4:14b                                 pos: 1   neg: 0  
			dolphin-mistral:7b                       pos: 1   neg: 0  
			athene-v2:72b                            pos: 1   neg: 0  
			openchat:7b                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			-----
			dolphin3:8b                              pos: 0   neg: 1  
			tulu3:8b                                 pos: 0   neg: 1  
			openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER2));
		}
	}

	@Test
	public void simpleStrawberryRCountv1_AllModels_XL() {
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIAndOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"Count the number of R in strawberry. Answer with number only", 
					"3",
					true, false);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			gpt-4-turbo                              pos: 1   neg: 0  
			qwen2.5:7b                               pos: 1   neg: 0  
			sailor2:20b                              pos: 1   neg: 0  
			gemma2:9b                                pos: 1   neg: 0  
			olmo2:7b                                 pos: 1   neg: 0  
			mistral:7b                               pos: 1   neg: 0  
			marco-o1:7b                              pos: 1   neg: 0  
			dolphin-mistral:7b                       pos: 1   neg: 0  
			openchat:7b                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			-----
			dolphin3:8b                              pos: 0   neg: 1  
			tulu3:70b                                pos: 0   neg: 1  
			nemotron:70b                             pos: 0   neg: 1  
			llama3.1:8b                              pos: 0   neg: 1  
			qwen2:7b                                 pos: 0   neg: 1  
			granite3.1-dense:8b                      pos: 0   neg: 1  
			tulu3:8b                                 pos: 0   neg: 1  
			llama3.2:3b                              pos: 0   neg: 1  
			gpt-4o-mini                              pos: 0   neg: 1  
			exaone3.5:32b                            pos: 0   neg: 1  
			qwen2.5:72b                              pos: 0   neg: 1  
			gemma2:27b                               pos: 0   neg: 1  
			llama3.3:70b                             pos: 0   neg: 1  
			llama3.1:70b                             pos: 0   neg: 1  
			gpt-4                                    pos: 0   neg: 1  
			gpt-3.5-turbo                            pos: 0   neg: 1  
			phi4:14b                                 pos: 0   neg: 1  
			openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
			athene-v2:72b                            pos: 0   neg: 1   
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER1));
		}
	}

	@Test
	public void simpleStrawberryRCountv2_AllModels_XL() {
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		if (settings.getOpenaikey().length() > 10) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOpenAIAndOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					Globals.MODEL_NAMES_OPENAI_ALL,
					settings,
					"Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
					+ "Walk through each character in the word while counting and make note of the string index they occur at. "
					+ "Include the string index records in your motivation. Answer with number only", 
					"3", true, false);

			// Print the scorecard
			System.out.println("SCORECARD:");
			scorecard.evaluate();
			scorecard.print();

			/*
			dolphin3:8b                              pos: 1   neg: 0  
			gpt-4-turbo                              pos: 1   neg: 0  
			qwen2.5:7b                               pos: 1   neg: 0  
			llama3.1:8b                              pos: 1   neg: 0  
			granite3.1-dense:8b                      pos: 1   neg: 0  
			sailor2:20b                              pos: 1   neg: 0  
			exaone3.5:32b                            pos: 1   neg: 0  
			gemma2:9b                                pos: 1   neg: 0  
			qwen2.5:72b                              pos: 1   neg: 0  
			olmo2:7b                                 pos: 1   neg: 0  
			gemma2:27b                               pos: 1   neg: 0  
			mistral:7b                               pos: 1   neg: 0  
			llama3.3:70b                             pos: 1   neg: 0  
			llama3.1:70b                             pos: 1   neg: 0  
			marco-o1:7b                              pos: 1   neg: 0  
			athene-v2:72b                            pos: 1   neg: 0  
			openchat:7b                              pos: 1   neg: 0  
			gpt-4o                                   pos: 1   neg: 0  
			-----
			tulu3:70b                                pos: 0   neg: 1  
			nemotron:70b                             pos: 0   neg: 1  
			qwen2:7b                                 pos: 0   neg: 1  
			tulu3:8b                                 pos: 0   neg: 1  
			llama3.2:3b                              pos: 0   neg: 1  
			gpt-4o-mini                              pos: 0   neg: 1  
			gpt-4                                    pos: 0   neg: 1  
			gpt-3.5-turbo                            pos: 0   neg: 1  
			phi4:14b                                 pos: 0   neg: 1  
			dolphin-mistral:7b                       pos: 0   neg: 1  
			openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
			 */

			// Assert
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M));
			assertTrue("Make sure we have a clean sheet for all default ensemble models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OPENAI_TIER1));
		}
	}


}
