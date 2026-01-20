package ntt.security.ollamadrama.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;

@SuppressWarnings("serial")
public class OllamaGeneralKnowledgeTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaGeneralKnowledgeTest.class);

	@Test
	public void mcptools_missing_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 1); 	// OK to be confused
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
		        false,  // use MCP
		        Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, // qwen2.5:7b 
		        "What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.",
		        acceptable_answers,
		        false,      // hide_llm_reply_if_uncertain
		        false,      // use_random_seed
		        false,      // return toolcall
		        settings	// custom ollamadrama settings
		);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		tulu3:70b                                pos: 1   neg: 0   [XL ] [TIER2]
		nemotron:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		olmo-3:32b                               pos: 1   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 1   neg: 0   [XL ] [TIER1]
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		qwen2.5:72b                              pos: 1   neg: 0   [XL ] [TIER2]
		mistral:7b                               pos: 1   neg: 0   [M  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L  ] [TIER1]
		olmo-3:7b                                pos: 1   neg: 0   [M  ] [TIER4]
		llama3.3:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 1   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 1   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		gemma3:12b                               pos: 1   neg: 0   [L  ] [TIER2]
		phi4:14b                                 pos: 1   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 1   neg: 0   [XL ] [TIER1]
		athene-v2:72b                            pos: 1   neg: 0   [XL ] [TIER2]
		cogito:70b                               pos: 1   neg: 0   [XL ] [TIER2]
		-----
		NEGATIVE:
		nemotron-3-nano:30b                      pos: 0   neg: 1   [XL ] [TIER3]
		cogito:8b                                pos: 0   neg: 1   [M  ] [TIER4]
		tulu3:8b                                 pos: 0   neg: 1   [M  ] [TIER4]
		sailor2:20b                              pos: 0   neg: 1   [L  ] [TIER2]
		llava:34b                                pos: 0   neg: 1   [XL ] [TIER3]
		gpt-oss:20b                              pos: 0   neg: 1   [L  ] [TIER3]
		aya-expanse:8b                           pos: 0   neg: 1   [M  ] [TIER4]
		cogito:14b                               pos: 0   neg: 1   [L  ] [TIER1]
		marco-o1:7b                              pos: 0   neg: 1   [M  ] [TIER1]
		aya-expanse:32b                          pos: 0   neg: 1   [XL ] [TIER3]
		gemma3n:e4b                              pos: 0   neg: 1   [M  ] [TIER4]
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		qwen3:14b                                pos: 0   neg: 1   [L  ] [TIER1]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for XL tier1 models", scorecard.isCleanSheetPositive(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL));
	}
	
	@Test
	public void mcptools_available_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("TOOLCALL", 1); 	// The LLM should realize it can call a tool and make a request
		}};

		String prompt = """
		        What is the current temperature in Paris? Reply with only a number where the number is the temperature in celcius.
		        
		        MCP TOOLS AVAILABLE:
		        ---
		        Tool: fetch_temperature
		        Description: Fetches the current temperature of a City in Celcius or Fahrenheit
		        Inputs:
		         - location (string) [required]: The City of interest
		         - unit (string): The temperature unit
		        Example usage: fetch_temperature(location="New York", unit="Celcius")
		        """;

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
		        true,  // use MCP
		        Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, // qwen2.5:7b 
		        prompt,
		        acceptable_answers,
		        false,      // hide_llm_reply_if_uncertain
		        false,      // use_random_seed
		        false,      // return toolcall
		        settings	// custom ollamadrama settings
		);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		tulu3:70b                                pos: 1   neg: 0   [XL ] [TIER2]
		nemotron:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		tulu3:8b                                 pos: 1   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 1   neg: 0   [L  ] [TIER2]
		olmo-3:32b                               pos: 1   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 1   neg: 0   [XL ] [TIER1]
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		olmo-3.1:32b                             pos: 1   neg: 0   [XL ] [TIER3]
		qwen2.5:72b                              pos: 1   neg: 0   [XL ] [TIER1]
		gpt-oss:20b                              pos: 1   neg: 0   [L  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L  ] [TIER1]
		cogito:14b                               pos: 1   neg: 0   [L  ] [TIER1]
		r1-1776:70b                              pos: 1   neg: 0   [XL ] [TIER2]
		llama3.3:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 1   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 1   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		aya-expanse:32b                          pos: 1   neg: 0   [XL ] [TIER3]
		phi4:14b                                 pos: 1   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 1   neg: 0   [XL ] [TIER1]
		athene-v2:72b                            pos: 1   neg: 0   [XL ] [TIER2]
		cogito:70b                               pos: 1   neg: 0   [XL ] [TIER2]
		qwen3:14b                                pos: 1   neg: 0   [L  ] [TIER1]
		-----
		NEGATIVE:
		nemotron-3-nano:30b                      pos: 0   neg: 1   [XL ] [TIER3]
		cogito:8b                                pos: 0   neg: 1   [M  ] [TIER4]
		qwen2.5:7b                               pos: 0   neg: 1   [M  ] [TIER1]
		llama3.1:8b                              pos: 0   neg: 1   [M  ] [TIER3]
		granite3.3:8b                            pos: 0   neg: 1   [M  ] [TIER4]
		llava:34b                                pos: 0   neg: 1   [XL ] [TIER3]
		mistral:7b                               pos: 0   neg: 1   [M  ] [TIER3]
		aya-expanse:8b                           pos: 0   neg: 1   [M  ] [TIER4]
		olmo-3:7b                                pos: 0   neg: 1   [M  ] [TIER4]
		marco-o1:7b                              pos: 0   neg: 1   [M  ] [TIER1]
		gemma3:12b                               pos: 0   neg: 1   [L  ] [TIER2]
		gemma3n:e4b                              pos: 0   neg: 1   [M  ] [TIER4]
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for tier1 L+XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_L_XL));
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

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);

		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
		        false,  // use MCP
		        Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, // olmo-3.1:32b slow, Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
		        "What company or organization is associated with google.com? Reply with only the name",
		        acceptable_answers,
		        false,      // hide_llm_reply_if_uncertain
		        false,      // use_random_seed
		        false,      // return toolcall
		        settings	// custom ollamadrama settings
		);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		nemotron-3-nano:30b                      pos: 2   neg: 0   [XL ] [TIER3]
		cogito:8b                                pos: 2   neg: 0   [M  ] [TIER4]
		tulu3:70b                                pos: 2   neg: 0   [XL ] [TIER2]
		nemotron:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		qwen2.5:7b                               pos: 2   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 2   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 2   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 2   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 2   neg: 0   [M  ] [TIER4]
		tulu3:8b                                 pos: 2   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 2   neg: 0   [L  ] [TIER2]
		olmo-3:32b                               pos: 2   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 2   neg: 0   [XL ] [TIER1]
		gemma2:9b                                pos: 2   neg: 0   [M  ] [TIER1]
		qwen2.5:72b                              pos: 2   neg: 0   [XL ] [TIER1]
		mistral:7b                               pos: 2   neg: 0   [M  ] [TIER3]
		gpt-oss:20b                              pos: 2   neg: 0   [L  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 2   neg: 0   [L  ] [TIER1]
		aya-expanse:8b                           pos: 2   neg: 0   [M  ] [TIER4]
		cogito:14b                               pos: 2   neg: 0   [L  ] [TIER1]
		olmo-3:7b                                pos: 2   neg: 0   [M  ] [TIER4]
		r1-1776:70b                              pos: 2   neg: 0   [XL ] [TIER2]
		llama3.3:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 2   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 2   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		marco-o1:7b                              pos: 2   neg: 0   [M  ] [TIER1]
		aya-expanse:32b                          pos: 2   neg: 0   [XL ] [TIER3]
		phi4:14b                                 pos: 2   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 2   neg: 0   [XL ] [TIER1]
		gemma3n:e4b                              pos: 2   neg: 0   [M  ] [TIER4]
		openchat:7b                              pos: 2   neg: 0   [M  ] [TIER2]
		athene-v2:72b                            pos: 2   neg: 0   [XL ] [TIER2]
		cogito:70b                               pos: 2   neg: 0   [XL ] [TIER2]
		qwen3:14b                                pos: 2   neg: 0   [L  ] [TIER1]
		-----
		NEGATIVE:
		llava:34b                                pos: 0   neg: 1   [XL ] [TIER3]
		gemma3:12b                               pos: 0   neg: 1   [L  ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for tier1 models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleParisTemperature_OllamaModels_NEWMODELS() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false, // no mcp
				"qwen3:4b",  // qwen3:4b llama3.1:8b Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
				"What is the current temperature in Paris?", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	
		OllamaService.destroyInstance();
		
	}
	
	@Test
	public void simpleParisCapital_OllamaModels_NEWMODELS() {
		
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("Yes", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(false,
				"nemotron-3-nano:30b", // olmo-3.1:32b olmo-3:7b, olmo-3:32b huihui_ai/qwen3-abliterated:32b nemotron-3-nano:30b 
				"Is the capital city of France named Paris? Reply with Yes or No.", acceptable_answers, true, false, false, settings);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
		
		OllamaService.destroyInstance();
	}
	
	@Test
	public void simpleParisCapital_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("Yes", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false,
				"qwen3:4b",  // qwen3:4b llama3.1:8b Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
				"Is the capital city of France named Paris? Reply with Yes or No.", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		nemotron-3-nano:30b                      pos: 1   neg: 0   [XL] [TIER3]
		cogito:8b                                pos: 1   neg: 0   [M ] [TIER4]
		tulu3:70b                                pos: 1   neg: 0   [XL] [TIER2]
		nemotron:70b                             pos: 1   neg: 0   [XL] [TIER1]
		qwen2.5:7b                               pos: 1   neg: 0   [M ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M ] [TIER4]
		tulu3:8b                                 pos: 1   neg: 0   [M ] [TIER4]
		sailor2:20b                              pos: 1   neg: 0   [L ] [TIER2]
		olmo-3:32b                               pos: 1   neg: 0   [XL] [TIER3]
		gemma3:27b                               pos: 1   neg: 0   [XL] [TIER1]
		llava:34b                                pos: 1   neg: 0   [XL] [TIER3]
		gemma2:9b                                pos: 1   neg: 0   [M ] [TIER1]
		olmo-3.1:32b                             pos: 1   neg: 0   [XL] [TIER3]
		qwen2.5:72b                              pos: 1   neg: 0   [XL] [TIER1]
		mistral:7b                               pos: 1   neg: 0   [M ] [TIER3]
		gpt-oss:20b                              pos: 1   neg: 0   [XX] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L ] [TIER1]
		aya-expanse:8b                           pos: 1   neg: 0   [M ] [TIER4]
		cogito:14b                               pos: 1   neg: 0   [L ] [TIER1]
		olmo-3:7b                                pos: 1   neg: 0   [M ] [TIER4]
		r1-1776:70b                              pos: 1   neg: 0   [XL] [TIER2]
		llama3.3:70b                             pos: 1   neg: 0   [XL] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 1   neg: 0   [XL] [TIER2]
		qwq:32b                                  pos: 1   neg: 0   [XL] [TIER1]
		llama3.1:70b                             pos: 1   neg: 0   [XL] [TIER1]
		marco-o1:7b                              pos: 1   neg: 0   [M ] [TIER1]
		gemma3:12b                               pos: 1   neg: 0   [L ] [TIER2]
		aya-expanse:32b                          pos: 1   neg: 0   [XL] [TIER3]
		phi4:14b                                 pos: 1   neg: 0   [L ] [TIER2]
		qwen3:32b                                pos: 1   neg: 0   [XL] [TIER1]
		gemma3n:e4b                              pos: 1   neg: 0   [M ] [TIER4]
		openchat:7b                              pos: 1   neg: 0   [M ] [TIER2]
		athene-v2:72b                            pos: 1   neg: 0   [XL] [TIER2]
		cogito:70b                               pos: 1   neg: 0   [XL] [TIER2]
		qwen3:14b                                pos: 1   neg: 0   [L ] [TIER1]
		llama3.1:8b                              pos: 0   neg: 1   [M ] [TIER3]
		-----
		NEGATIVE:
		(qwen3:4b                                 pos: 0   neg: 1   [S ] [TIER1])
		 */
		
		OllamaService.destroyInstance();

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleNonsenseInput1_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 2); 			// OK to set LOWPROBA since OOikiOOA is completely fictional
			this.put("No", 1);		 			// OK based on its knowledge, but LOWPROBA preferred
			this.put("NO", 1);		 			// OK based on its knowledge, but LOWPROBA preferred
		}};

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(false, Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, "Is the capital city of OOikiOOA named Mo1rstiooooo? Reply with Yes or No.", acceptable_answers, true, false, false, settings);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*		
		POSITIVE:
		nemotron-3-nano:30b                      pos: 2   neg: 0   [XL ] [TIER3]
		cogito:8b                                pos: 2   neg: 0   [M  ] [TIER4]
		tulu3:70b                                pos: 2   neg: 0   [XL ] [TIER2]
		nemotron:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		qwen2.5:7b                               pos: 2   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 2   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 2   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 2   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 2   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 2   neg: 0   [L  ] [TIER2]
		olmo-3:32b                               pos: 2   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 2   neg: 0   [XL ] [TIER1]
		gemma2:9b                                pos: 2   neg: 0   [M  ] [TIER1]
		huihui_ai/qwen3-abliterated:14b          pos: 2   neg: 0   [L  ] [TIER1]
		aya-expanse:8b                           pos: 2   neg: 0   [M  ] [TIER4]
		cogito:14b                               pos: 2   neg: 0   [L  ] [TIER1]
		olmo-3:7b                                pos: 2   neg: 0   [M  ] [TIER4]
		r1-1776:70b                              pos: 2   neg: 0   [XL ] [TIER2]
		llama3.3:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 2   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 2   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 2   neg: 0   [XL ] [TIER1]
		marco-o1:7b                              pos: 2   neg: 0   [M  ] [TIER1]
		gemma3:12b                               pos: 2   neg: 0   [L  ] [TIER2]
		phi4:14b                                 pos: 2   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 2   neg: 0   [XL ] [TIER1]
		cogito:70b                               pos: 2   neg: 0   [XL ] [TIER2]
		qwen3:14b                                pos: 2   neg: 0   [L  ] [TIER1]
		-----
		tulu3:8b                                 pos: 1   neg: 0   [M  ] [TIER4]
		llava:34b                                pos: 1   neg: 0   [XL ] [TIER3]
		qwen2.5:72b                              pos: 1   neg: 0   [XL ] [TIER1]
		mistral:7b                               pos: 1   neg: 0   [M  ] [TIER3]
		gpt-oss:20b                              pos: 1   neg: 0   [L  ] [TIER3]
		aya-expanse:32b                          pos: 1   neg: 0   [XL ] [TIER3]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		athene-v2:72b                            pos: 1   neg: 0   [XL ] [TIER2]
		-----
		NEGATIVE:
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleNonsenseInput2_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA since the provided prompt is pure jibberish
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(false, Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, "bmn,alkwjhuwihjkl7777oodottodooo?", acceptable_answers, true, false, false, settings);

		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*		
		POSITIVE:
		nemotron-3-nano:30b                      pos: 1   neg: 0   [XL ] [TIER3]
		cogito:8b                                pos: 1   neg: 0   [M  ] [TIER4]
		tulu3:70b                                pos: 1   neg: 0   [XL ] [TIER2]
		nemotron:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		tulu3:8b                                 pos: 1   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 1   neg: 0   [L  ] [TIER2]
		olmo-3:32b                               pos: 1   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 1   neg: 0   [XL ] [TIER1]
		llava:34b                                pos: 1   neg: 0   [XL ] [TIER3]
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		qwen2.5:72b                              pos: 1   neg: 0   [XL ] [TIER1]
		mistral:7b                               pos: 1   neg: 0   [M  ] [TIER3]
		gpt-oss:20b                              pos: 1   neg: 0   [L  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L  ] [TIER1]
		aya-expanse:8b                           pos: 1   neg: 0   [M  ] [TIER4]
		cogito:14b                               pos: 1   neg: 0   [L  ] [TIER1]
		olmo-3:7b                                pos: 1   neg: 0   [M  ] [TIER4]
		r1-1776:70b                              pos: 1   neg: 0   [XL ] [TIER2]
		llama3.3:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 1   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 1   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		marco-o1:7b                              pos: 1   neg: 0   [M  ] [TIER1]
		gemma3:12b                               pos: 1   neg: 0   [L  ] [TIER2]
		aya-expanse:32b                          pos: 1   neg: 0   [XL ] [TIER3]
		phi4:14b                                 pos: 1   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 1   neg: 0   [XL ] [TIER1]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		openchat:7b                              pos: 1   neg: 0   [M  ] [TIER2]
		athene-v2:72b                            pos: 1   neg: 0   [XL ] [TIER2]
		cogito:70b                               pos: 1   neg: 0   [XL ] [TIER2]
		qwen3:14b                                pos: 1   neg: 0   [L  ] [TIER1]
		-----
		NEGATIVE:
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

	@Test
	public void simpleStrawberryRCountVague_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("3", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false, // no mcp
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_M, 
				"Count the number of R in strawberry. Answer with number only", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		olmo-3:7b                                pos: 1   neg: 0   [M  ] [TIER4]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		marco-o1:7b                              pos: 1   neg: 0   [M  ] [TIER1]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		openchat:7b                              pos: 1   neg: 0   [M  ] [TIER2]
		-----
		NEGATIVE:
		cogito:8b                                pos: 0   neg: 1   [M  ] [TIER4]
		mistral:7b                               pos: 0   neg: 1   [M  ] [TIER3]
		aya-expanse:8b                           pos: 0   neg: 1   [M  ] [TIER4]
		tulu3:8b                                 pos: 0   neg: 1   [M  ] [TIER4]
		 */

		OllamaService.destroyInstance();
		
	}

	@Test
	public void simpleStrawberryRCount_OllamaModels_XL() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("3", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false, // no mcp
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 
				"Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
				+ "Walk through each character in the word while counting and make note of the string index they occur at. "
				+ "Include the string index records in your motivation. Answer with number only", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		nemotron-3-nano:30b                      pos: 1   neg: 0   [XL ] [TIER3]
		cogito:8b                                pos: 1   neg: 0   [M  ] [TIER4]
		nemotron:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 1   neg: 0   [L  ] [TIER2]
		olmo-3:32b                               pos: 1   neg: 0   [XL ] [TIER3]
		gemma3:27b                               pos: 1   neg: 0   [XL ] [TIER1]
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		gpt-oss:20b                              pos: 1   neg: 0   [L  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L  ] [TIER1]
		aya-expanse:8b                           pos: 1   neg: 0   [M  ] [TIER4]
		cogito:14b                               pos: 1   neg: 0   [L  ] [TIER1]
		llama3.3:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		huihui_ai/qwen3-abliterated:32b          pos: 1   neg: 0   [XL ] [TIER2]
		qwq:32b                                  pos: 1   neg: 0   [XL ] [TIER1]
		llama3.1:70b                             pos: 1   neg: 0   [XL ] [TIER1]
		marco-o1:7b                              pos: 1   neg: 0   [M  ] [TIER1]
		gemma3:12b                               pos: 1   neg: 0   [L  ] [TIER2]
		phi4:14b                                 pos: 1   neg: 0   [L  ] [TIER2]
		qwen3:32b                                pos: 1   neg: 0   [XL ] [TIER1]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		qwen3:14b                                pos: 1   neg: 0   [L  ] [TIER1]
		-----
		NEGATIVE:
		tulu3:70b                                pos: 0   neg: 1   [XL ] [TIER2]
		tulu3:8b                                 pos: 0   neg: 1   [M  ] [TIER4]
		llava:34b                                pos: 0   neg: 1   [XL ] [TIER3]
		qwen2.5:72b                              pos: 0   neg: 1   [XL ] [TIER2]
		mistral:7b                               pos: 0   neg: 1   [M  ] [TIER3]
		olmo-3:7b                                pos: 0   neg: 1   [M  ] [TIER4]
		r1-1776:70b                              pos: 0   neg: 1   [XL ] [TIER2]
		aya-expanse:32b                          pos: 0   neg: 1   [XL ] [TIER3]
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		athene-v2:72b                            pos: 0   neg: 1   [XL ] [TIER2]
		cogito:70b                               pos: 0   neg: 1   [XL ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleStrawberryRCount_OllamaModels_L() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("3", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false, // no mcp
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L, 
				"Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
				+ "Walk through each character in the word while counting and make note of the string index they occur at. "
				+ "Include the string index records in your motivation. Answer with number only", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		cogito:8b                                pos: 1   neg: 0   [M  ] [TIER4]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		gpt-oss:20b                              pos: 1   neg: 0   [L  ] [TIER3]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		huihui_ai/qwen3-abliterated:14b          pos: 1   neg: 0   [L  ] [TIER1]
		aya-expanse:8b                           pos: 1   neg: 0   [M  ] [TIER4]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		cogito:14b                               pos: 1   neg: 0   [L  ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		sailor2:20b                              pos: 1   neg: 0   [L  ] [TIER2]
		marco-o1:7b                              pos: 1   neg: 0   [M  ] [TIER1]
		gemma3:12b                               pos: 1   neg: 0   [L  ] [TIER2]
		phi4:14b                                 pos: 1   neg: 0   [L  ] [TIER2]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		qwen3:14b                                pos: 1   neg: 0   [L  ] [TIER1]
		-----
		NEGATIVE:
		mistral:7b                               pos: 0   neg: 1   [M  ] [TIER3]
		olmo-3:7b                                pos: 0   neg: 1   [M  ] [TIER4]
		tulu3:8b                                 pos: 0   neg: 1   [M  ] [TIER4]
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 L models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}
	
	@Test
	public void simpleStrawberryRCount_OllamaModels_M() {
		
		HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
			this.put("3", 1);
		}};
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				false, // no mcp
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_M, 
				"Count the number of 'r' characters in the string 's t r a w b e r r y', which contains 10 characters from the alphabet in total. "
				+ "Walk through each character in the word while counting and make note of the string index they occur at. "
				+ "Include the string index records in your motivation. Answer with number only", 
				acceptable_answers,
				false, false, false, settings);
		
		// Print the scorecard
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();

		/*
		POSITIVE:
		gemma2:9b                                pos: 1   neg: 0   [M  ] [TIER1]
		cogito:8b                                pos: 1   neg: 0   [M  ] [TIER4]
		qwen2.5:7b                               pos: 1   neg: 0   [M  ] [TIER1]
		qwen3:8b                                 pos: 1   neg: 0   [M  ] [TIER1]
		llama3.1:8b                              pos: 1   neg: 0   [M  ] [TIER3]
		aya-expanse:8b                           pos: 1   neg: 0   [M  ] [TIER4]
		qwen3:4b                                 pos: 1   neg: 0   [S  ] [TIER1]
		granite3.3:8b                            pos: 1   neg: 0   [M  ] [TIER4]
		marco-o1:7b                              pos: 1   neg: 0   [M  ] [TIER1]
		gemma3n:e4b                              pos: 1   neg: 0   [M  ] [TIER4]
		-----
		NEGATIVE:
		mistral:7b                               pos: 0   neg: 1   [M  ] [TIER3]
		olmo-3:7b                                pos: 0   neg: 1   [M  ] [TIER4]
		tulu3:8b                                 pos: 0   neg: 1   [M  ] [TIER4]
		openchat:7b                              pos: 0   neg: 1   [M  ] [TIER2]
		 */

		OllamaService.destroyInstance();
		
		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

}
