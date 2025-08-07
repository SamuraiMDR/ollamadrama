package ntt.security.ollamadrama.modelscorecards;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;

public class OllamaConfidenceThresholdTuning {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaConfidenceThresholdTuning.class);

	@SuppressWarnings("serial")
	@Test
	public void threshold_tuning_OllamaModels_XL() {

		String outfile = "threshold_tuning.csv";
		int iterations = 1;
		
		/*
		// simple common knowledge fact
		for (int i=1; i<=iterations; i++) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					"Is the capital city of France named Paris? Reply with Yes or No.", 
					"Yes",
					false, true);
			// Print the scorecard
			System.out.println("MODEL POSITIVE PROBAS:");
			scorecard.evaluate();
			scorecard.print();
			scorecard.flushProbas("Capital France", outfile);
		}
		
		// easy domain specific fact (encrypted SMTP doubt may exist)
		for (int i=1; i<=iterations; i++) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					"What TCP port does the SMTP protocol typically use? Answer with only a number.", 
					"25",
					false, true);
			// Print the scorecard
			System.out.println("MODEL POSITIVE PROBABILITIES:");
			scorecard.evaluate();
			scorecard.print();
			scorecard.flushProbas("SMTP port", outfile);
		}
		*/
		
		// nonsense input
		HashMap<String, Integer> acceptable_answers_nonsenseinpu1 = new HashMap<String, Integer>() {{
			this.put("FAILTOUNDERSTAND", 2); 	// OK to be confused and not understand the question at all
			this.put("LOWPROBA", 1); 			// OK to set LOWPROBA since the provided prompt is pure jibberish
		}};
		for (int i=1; i<=iterations; i++) {
			ModelsScoreCard scorecard = OllamaDramaUtils.populateScorecardsForOllamaModels(
					true, // use MCP
					Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
					"bmn,alkwjhuwihjkl7777oodottodooo?", 
					acceptable_answers_nonsenseinpu1,
					false, true);
			// Print the scorecard
			System.out.println("MODEL POSITIVE PROBABILITIES:");
			scorecard.evaluate();
			scorecard.print();
			scorecard.flushProbas("Nonsense Prompt 1", outfile);
		}
		
		

		// Assert
		//assertTrue("Make sure we have a clean sheet for all tier1 M L XL models", scorecard.isCleanSheetPositive(Globals.MODEL_NAMES_OLLAMA_ALL_TIER1_M_L_XL));
	}


}
