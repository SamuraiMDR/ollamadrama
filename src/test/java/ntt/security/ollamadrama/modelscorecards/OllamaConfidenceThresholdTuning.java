package ntt.security.ollamadrama.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.ConfidenceThresholdCard;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;

public class OllamaConfidenceThresholdTuning {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaConfidenceThresholdTuning.class);

	@SuppressWarnings("serial")
	@Test
	public void thresholdtuning_OllamaModels_XL() {

		ConfidenceThresholdCard confidencecard = new ConfidenceThresholdCard();

		String outfile = "threshold_tuning.csv";
		int iterations = 1;

		// 0%, super low proba
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>();
			for (int o=1; o<=1000; o++) {
				acceptable_answers_input.put("" + o, 1);
			}
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_numberguess1_1000",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What number between 1 and 1000 am I thinking of right now? You MUST guess and just provide a number even if the probability of getting it right is low.", 
					acceptable_answers_input,
					"0");
		}
		
		// 1%, low proba
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>();
			for (int o=1; o<=100; o++) {
				acceptable_answers_input.put("" + o, 1);
			}
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_numberguess1_100",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What number between 1 and 100 am I thinking of right now? You MUST guess and just provide a number even if the probability of getting it right is low.", 
					acceptable_answers_input,
					"0");
		}
		
		// 10% proba
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>();
			for (int o=1; o<=10; o++) {
				acceptable_answers_input.put("" + o, 1);
			}
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_numberguess1_10",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What number between 1 and 10 am I thinking of right now? You MUST guess and just provide a number even if the probability of getting it right is low.", 
					acceptable_answers_input,
					"0");
		}
		
		// 20% proba
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>();
			for (int o=1; o<=5; o++) {
				acceptable_answers_input.put("" + o, 1);
			}
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_numberguess1_5",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What number between 1 and 5 am I thinking of right now? You MUST guess and just provide a number even if the probability of getting it right is low.", 
					acceptable_answers_input,
					"0");
		}
		
		// random 50% choice, choice random/opinionated so should be close to 50%
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("Yes", 1); 
				this.put("No", 1); 
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t50_faircoin",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"I just flipped a fair coin. Did it land on heads? You MUST take a guess and answer with 'Yes' or 'No'.", 
					acceptable_answers_input,
					"50");
		}
		
		// opinionated, range close to 50%
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("flight", 1); 
				this.put("invisibility", 1); 
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t50_superpower",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What's the ultimate superpower: flight or invisibility? Both answers are acceptable since this is subjective but you MUST answer with 'flight' or 'invisibility'.", 
					acceptable_answers_input,
					"50");
		}

		// simple common knowledge fact
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("Yes", 1);
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t100_paris",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"Is the capital city of France named Paris? Reply with Yes or No.", 
					acceptable_answers_input,
					"100");
		}

		// tcp/ip knowledge
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("25", 1);
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t100_smtpport",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What TCP port does the SMTP protocol typically use? Answer with only a number.", 
					acceptable_answers_input,
					"100");
		}

		// security knowledge (not 100 but close)
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("Reconnaissance", 2);
				this.put("Discovery", 1);
				this.put("Lateral Movement", 1);
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t100_mitre",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What MITRE tactic matches the string 'TCP Portscan'? You MUST choose from the following list: "
					+ "'Reconnaissance',"
					+ "'Resource Development',"
					+ "'Initial Access',"
					+ "'Execution',"
					+ "'Persistence',"
					+ "'Privilege Escalation',"
					+ "'Defense Evasion',"
					+ "'Credential Access',"
					+ "'Discovery',"
					+ "'Lateral Movement',"
					+ "'Collection',"
					+ "'Command and Control',"
					+ "'Exfiltration',"
					+ "'Impact'."
					+ ""
					+ "Dont forget the importance of sticking to the provided list."
					+ "You will be punished if you do not select from the provided list." , 
					acceptable_answers_input,
					"100");
		}

		// 0%, nonsense
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
				this.put("LOWPROBA", 1); 			// OK to set LOWPROBA 
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_nonsense",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"bmn,alkwjhuwihjkl7777oodottodooo?", 
					acceptable_answers_input,
					"0");
		}
		
		// 0%, super super low proba
		for (int i=1; i<=iterations; i++) {
			HashMap<String, Integer> acceptable_answers_input = new HashMap<String, Integer>() {{
				this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
				this.put("LOWPROBA", 1); 			// OK to set LOWPROBA 
			}};
			confidencecard = OllamaDramaUtils.populateConfidencecardsForOllamaModels(
					confidencecard,
					"t0_nrleaves",
					false, // use mcp
					Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_DIVERSE_XL, // MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
					"What is the exact number of leaves on the tree outside my window? Remember to output a number or FAILTOUNDERSTAND if you are not capable to answer.", 
					acceptable_answers_input,
					"0");
		}
		
		// Print the confidencecard
		System.out.println("Model threshold tuning:");
		confidencecard.print();
		confidencecard.flushProbas(outfile);

	}


	@SuppressWarnings("serial")
	@Test
	public void nonsensetest_OllamaModels_XL() {

		// nonsense input
		HashMap<String, Integer> acceptable_answers_nonsenseinpu1 = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA since the provided prompt is pure jibberish
		}};
		ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
				true, // use MCP
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
				"bmn,alkwjhuwihjkl7777oodottodooo?", 
				acceptable_answers_nonsenseinpu1,
				false, true);
		// Print the scorecard
		scorecard.evaluate();
		scorecard.print();

		// Assert
		assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}

}
