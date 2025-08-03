package ntt.security.ollamadrama.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;

@SuppressWarnings("serial")
public class OllamaGeneralKnowledgeTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaGeneralKnowledgeTest.class);

	@Test
	public void mcptools_missing_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 1); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA since OOikiOOA is completely fictional
		}};

		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.",
				acceptable_answers,
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		tulu3:70b                                pos: 1   neg: 0  
		cogito:8b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		qwen3:4b                                 pos: 1   neg: 0  
		tulu3:8b                                 pos: 1   neg: 0  
		granite3.3:8b                            pos: 1   neg: 0  
		llama3.2:3b                              pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		exaone-deep:7.8b                         pos: 1   neg: 0  
		llava:34b                                pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		cogito:14b                               pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		olmo2:13b                                pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		qwen2.5vl:72b                            pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		aya-expanse:32b                          pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		phi4:14b                                 pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		openchat:7b                              pos: 1   neg: 0  
		cogito:70b                               pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		olmo2:7b                                 pos: 0   neg: 1  
		aya-expanse:8b                           pos: 0   neg: 1  
		gemma3n:e4b                              pos: 0   neg: 1 
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void mcptools_available_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("TOOLCALL", 1); 	// The LLM should realize it can and should reach out to an available tool
		}};

		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, // Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL cogito:14b
				"What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius."
				+ "\n\n"
				+ "MCP TOOLS AVAILABLE:\n"
				+ "---\n"
				+ "Tool: fetch_temperature\n"
				+ "Description: Fetches the current temperature of a City in Celcius or Fahrenheit\n"
				+ "Inputs:\n" 
				+ "  - location (string) [required]: The City of interest\n"
				+ "  - unit (string): The temperature unit\n"
				+ "Example usage: fetch_temperature(location=\"New York\", unit=\"Celcius\")\n",
				acceptable_answers,
				false, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		tulu3:70b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		tulu3:8b                                 pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		llava:34b                                pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		cogito:14b                               pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		qwen2.5vl:72b                            pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		aya-expanse:32b                          pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		phi4:14b                                 pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		cogito:70b                               pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		cogito:8b                                pos: 0   neg: 1  
		qwen2.5:7b                               pos: 0   neg: 1  
		qwen3:4b                                 pos: 0   neg: 1  
		granite3.3:8b                            pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		olmo2:7b                                 pos: 0   neg: 1  
		mistral:7b                               pos: 0   neg: 1  
		aya-expanse:8b                           pos: 0   neg: 1  
		olmo2:13b                                pos: 0   neg: 1  
		marco-o1:7b                              pos: 0   neg: 1  
		gemma3n:e4b                              pos: 0   neg: 1  
		openchat:7b                              pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_L_XL));
	}
	
	@Test
	public void simpleDomain2CompanyKnowledge_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("Google", 2);
			this.put("google", 1);
			this.put("Google Inc.", 2);
			this.put("Google LLC", 2);
			this.put("Alphabet Inc.", 2);
			this.put("Alphabet", 2);
		}};

		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
				"What company or organization is associated with google.com? Reply with only the name",
				acceptable_answers,
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		dolphin3:8b                              pos: 2   neg: 0  
		wizard-vicuna-uncensored:30b             pos: 2   neg: 0  
		tulu3:70b                                pos: 2   neg: 0  
		cogito:8b                                pos: 2   neg: 0  
		nemotron:70b                             pos: 2   neg: 0  
		qwen3:8b                                 pos: 2   neg: 0  
		qwen2.5:7b                               pos: 2   neg: 0  
		llama3.1:8b                              pos: 2   neg: 0  
		qwen2:7b                                 pos: 2   neg: 0  
		granite3.1-dense:8b                      pos: 2   neg: 0  
		qwen3:4b                                 pos: 2   neg: 0  
		tulu3:8b                                 pos: 2   neg: 0  
		granite3.3:8b                            pos: 2   neg: 0  
		gemma3:27b                               pos: 2   neg: 0  
		sailor2:20b                              pos: 2   neg: 0  
		command-r7b:7b                           pos: 2   neg: 0  
		llava:34b                                pos: 2   neg: 0  
		exaone3.5:32b                            pos: 2   neg: 0  
		gemma2:9b                                pos: 2   neg: 0  
		qwen2.5:72b                              pos: 2   neg: 0  
		olmo2:7b                                 pos: 2   neg: 0  
		gemma2:27b                               pos: 2   neg: 0  
		mistral:7b                               pos: 2   neg: 0  
		aya-expanse:8b                           pos: 2   neg: 0  
		cogito:14b                               pos: 2   neg: 0  
		llama3.3:70b                             pos: 2   neg: 0  
		r1-1776:70b                              pos: 2   neg: 0  
		olmo2:13b                                pos: 2   neg: 0  
		llama3.1:70b                             pos: 2   neg: 0  
		marco-o1:7b                              pos: 2   neg: 0  
		gemma3:12b                               pos: 2   neg: 0  
		aya-expanse:32b                          pos: 2   neg: 0  
		qwen3:32b                                pos: 2   neg: 0  
		phi4:14b                                 pos: 2   neg: 0  
		dolphin-mistral:7b                       pos: 2   neg: 0  
		openhermes:7b-mistral-v2.5-q4_0          pos: 2   neg: 0  
		athene-v2:72b                            pos: 2   neg: 0  
		openchat:7b                              pos: 2   neg: 0  
		cogito:70b                               pos: 2   neg: 0  
		qwen3:14b                                pos: 2   neg: 0  
		-----
		llama3.2:3b                              pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleParis_OllamaModels_NEW() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				"llama3.1:70b,cogito:14b", // cogito:14b 
				"Is the capital city of France named Paris? Reply with Yes or No.", 
				"Yes",
				true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}
	
	@Test
	public void simpleParis_OllamaModels_XL() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
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
		cogito:8b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		qwen2:7b                                 pos: 1   neg: 0  
		qwen3:4b                                 pos: 1   neg: 0  
		tulu3:8b                                 pos: 1   neg: 0  
		granite3.3:8b                            pos: 1   neg: 0  
		llama3.2:3b                              pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		exaone-deep:7.8b                         pos: 1   neg: 0  
		command-r7b:7b                           pos: 1   neg: 0  
		llava:34b                                pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		gemma2:27b                               pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		aya-expanse:8b                           pos: 1   neg: 0  
		cogito:14b                               pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		olmo2:13b                                pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		aya-expanse:32b                          pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		phi4:14b                                 pos: 1   neg: 0  
		dolphin-mistral:7b                       pos: 1   neg: 0  
		openhermes:7b-mistral-v2.5-q4_0          pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		openchat:7b                              pos: 1   neg: 0  
		cogito:70b                               pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleNonsenseInput1_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 2); 			// OK to set LOWPROBA since OOikiOOA is completely fictional
			this.put("No", 1);		 			// OK based on its knowledge, but LOWPROBA preferred
		}};

		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, "Is the capital city of OOikiOOA named Mo1rstiooooo? Reply with Yes or No.", acceptable_answers, true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*		
		tulu3:70b                                pos: 2   neg: 0  
		cogito:8b                                pos: 2   neg: 0  
		nemotron:70b                             pos: 2   neg: 0  
		qwen2.5:7b                               pos: 2   neg: 0  
		llama3.1:8b                              pos: 2   neg: 0  
		qwen2:7b                                 pos: 2   neg: 0  
		granite3.3:8b                            pos: 2   neg: 0  
		llama3.2:3b                              pos: 2   neg: 0  
		gemma3:27b                               pos: 2   neg: 0  
		sailor2:20b                              pos: 2   neg: 0  
		llava:34b                                pos: 2   neg: 0  
		exaone3.5:32b                            pos: 2   neg: 0  
		gemma2:9b                                pos: 2   neg: 0  
		qwen2.5:72b                              pos: 2   neg: 0  
		olmo2:7b                                 pos: 2   neg: 0  
		gemma2:27b                               pos: 2   neg: 0  
		mistral:7b                               pos: 2   neg: 0  
		aya-expanse:8b                           pos: 2   neg: 0  
		cogito:14b                               pos: 2   neg: 0  
		llama3.3:70b                             pos: 2   neg: 0  
		olmo2:13b                                pos: 2   neg: 0  
		llama3.1:70b                             pos: 2   neg: 0  
		gemma3:12b                               pos: 2   neg: 0  
		phi4:14b                                 pos: 2   neg: 0  
		dolphin-mistral:7b                       pos: 2   neg: 0  
		openhermes:7b-mistral-v2.5-q4_0          pos: 2   neg: 0  
		athene-v2:72b                            pos: 2   neg: 0  
		openchat:7b                              pos: 2   neg: 0  
		cogito:70b                               pos: 2   neg: 0  
		-----
		dolphin3:8b                              pos: 1   neg: 0  
		wizard-vicuna-uncensored:30b             pos: 1   neg: 0  
		granite3.1-dense:8b                      pos: 1   neg: 0  
		tulu3:8b                                 pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		aya-expanse:32b                          pos: 1   neg: 0  
		-----
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleNonsenseInput2_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA since the provided prompt is pure jibberish
		}};
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, "bmn,alkwjhuwihjkl7777oodottodooo?", acceptable_answers, true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*		
		dolphin3:8b                              pos: 1   neg: 0  
		wizard-vicuna-uncensored:30b             pos: 1   neg: 0  
		tulu3:70b                                pos: 1   neg: 0  
		cogito:8b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		qwen2:7b                                 pos: 1   neg: 0  
		granite3.1-dense:8b                      pos: 1   neg: 0  
		tulu3:8b                                 pos: 1   neg: 0  
		granite3.3:8b                            pos: 1   neg: 0  
		llama3.2:3b                              pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		exaone-deep:7.8b                         pos: 1   neg: 0  
		command-r7b:7b                           pos: 1   neg: 0  
		llava:34b                                pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		gemma2:27b                               pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		aya-expanse:8b                           pos: 1   neg: 0  
		cogito:14b                               pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		olmo2:13b                                pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		aya-expanse:32b                          pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		phi4:14b                                 pos: 1   neg: 0  
		dolphin-mistral:7b                       pos: 1   neg: 0  
		openhermes:7b-mistral-v2.5-q4_0          pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		openchat:7b                              pos: 1   neg: 0  
		cogito:70b                               pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		qwen3:4b                                 pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleStrawberryRCountVague_OllamaModels_XL() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
				"Count the number of R in strawberry. Answer with number only", "3", true, false);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		command-r7b:7b                           pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		dolphin-mistral:7b                       pos: 1   neg: 0  
		openchat:7b                              pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		dolphin3:8b                              pos: 0   neg: 1  
		wizard-vicuna-uncensored:30b             pos: 0   neg: 1  
		tulu3:70b                                pos: 0   neg: 1  
		cogito:8b                                pos: 0   neg: 1  
		nemotron:70b                             pos: 0   neg: 1  
		llama3.1:8b                              pos: 0   neg: 1  
		qwen2:7b                                 pos: 0   neg: 1  
		granite3.1-dense:8b                      pos: 0   neg: 1  
		qwen3:4b                                 pos: 0   neg: 1  
		tulu3:8b                                 pos: 0   neg: 1  
		granite3.3:8b                            pos: 0   neg: 1  
		llama3.2:3b                              pos: 0   neg: 1  
		gemma3:27b                               pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		llava:34b                                pos: 0   neg: 1  
		exaone3.5:32b                            pos: 0   neg: 1  
		qwen2.5:72b                              pos: 0   neg: 1  
		gemma2:27b                               pos: 0   neg: 1  
		mistral:7b                               pos: 0   neg: 1  
		aya-expanse:8b                           pos: 0   neg: 1  
		cogito:14b                               pos: 0   neg: 1  
		llama3.3:70b                             pos: 0   neg: 1  
		olmo2:13b                                pos: 0   neg: 1  
		llama3.1:70b                             pos: 0   neg: 1  
		gemma3:12b                               pos: 0   neg: 1  
		aya-expanse:32b                          pos: 0   neg: 1  
		phi4:14b                                 pos: 0   neg: 1  
		openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
		athene-v2:72b                            pos: 0   neg: 1  
		cogito:70b                               pos: 0   neg: 1  
		 */

	}

	@Test
	public void simpleStrawberryRCount_OllamaModels_XL() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
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
		cogito:8b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		granite3.1-dense:8b                      pos: 1   neg: 0  
		qwen3:4b                                 pos: 1   neg: 0  
		granite3.3:8b                            pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		aya-expanse:8b                           pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		wizard-vicuna-uncensored:30b             pos: 0   neg: 1  
		tulu3:70b                                pos: 0   neg: 1  
		qwen2:7b                                 pos: 0   neg: 1  
		tulu3:8b                                 pos: 0   neg: 1  
		llama3.2:3b                              pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		command-r7b:7b                           pos: 0   neg: 1  
		llava:34b                                pos: 0   neg: 1  
		gemma2:27b                               pos: 0   neg: 1  
		cogito:14b                               pos: 0   neg: 1  
		olmo2:13b                                pos: 0   neg: 1  
		aya-expanse:32b                          pos: 0   neg: 1  
		phi4:14b                                 pos: 0   neg: 1  
		dolphin-mistral:7b                       pos: 0   neg: 1  
		openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
		openchat:7b                              pos: 0   neg: 1  
		cogito:70b                               pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleStrawberryRCount_OllamaModels_L() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L, 
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
		cogito:8b                                pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		qwen3:4b                                 pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		command-r7b:7b                           pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		aya-expanse:8b                           pos: 1   neg: 0  
		cogito:14b                               pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		openchat:7b                              pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		qwen2:7b                                 pos: 0   neg: 1  
		tulu3:8b                                 pos: 0   neg: 1  
		granite3.3:8b                            pos: 0   neg: 1  
		llama3.2:3b                              pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		olmo2:13b                                pos: 0   neg: 1  
		phi4:14b                                 pos: 0   neg: 1  
		dolphin-mistral:7b                       pos: 0   neg: 1  
		openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleStrawberryRCount_OllamaModels_M() {
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_M, 
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
		cogito:8b                                pos: 1   neg: 0  
		nemotron:70b                             pos: 1   neg: 0  
		qwen3:8b                                 pos: 1   neg: 0  
		qwen2.5:7b                               pos: 1   neg: 0  
		llama3.1:8b                              pos: 1   neg: 0  
		granite3.1-dense:8b                      pos: 1   neg: 0  
		qwen3:4b                                 pos: 1   neg: 0  
		granite3.3:8b                            pos: 1   neg: 0  
		gemma3:27b                               pos: 1   neg: 0  
		sailor2:20b                              pos: 1   neg: 0  
		exaone3.5:32b                            pos: 1   neg: 0  
		gemma2:9b                                pos: 1   neg: 0  
		qwen2.5:72b                              pos: 1   neg: 0  
		olmo2:7b                                 pos: 1   neg: 0  
		mistral:7b                               pos: 1   neg: 0  
		aya-expanse:8b                           pos: 1   neg: 0  
		llama3.3:70b                             pos: 1   neg: 0  
		r1-1776:70b                              pos: 1   neg: 0  
		llama3.1:70b                             pos: 1   neg: 0  
		marco-o1:7b                              pos: 1   neg: 0  
		gemma3:12b                               pos: 1   neg: 0  
		qwen3:32b                                pos: 1   neg: 0  
		athene-v2:72b                            pos: 1   neg: 0  
		qwen3:14b                                pos: 1   neg: 0  
		-----
		wizard-vicuna-uncensored:30b             pos: 0   neg: 1  
		tulu3:70b                                pos: 0   neg: 1  
		qwen2:7b                                 pos: 0   neg: 1  
		tulu3:8b                                 pos: 0   neg: 1  
		llama3.2:3b                              pos: 0   neg: 1  
		exaone-deep:7.8b                         pos: 0   neg: 1  
		command-r7b:7b                           pos: 0   neg: 1  
		llava:34b                                pos: 0   neg: 1  
		gemma2:27b                               pos: 0   neg: 1  
		cogito:14b                               pos: 0   neg: 1  
		olmo2:13b                                pos: 0   neg: 1  
		aya-expanse:32b                          pos: 0   neg: 1  
		phi4:14b                                 pos: 0   neg: 1  
		dolphin-mistral:7b                       pos: 0   neg: 1  
		openhermes:7b-mistral-v2.5-q4_0          pos: 0   neg: 1  
		openchat:7b                              pos: 0   neg: 1  
		cogito:70b                               pos: 0   neg: 1  
		 */

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

}
