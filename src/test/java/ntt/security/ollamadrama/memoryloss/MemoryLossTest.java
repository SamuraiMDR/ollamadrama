package ntt.security.ollamadrama.memoryloss;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;

public class MemoryLossTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLossTest.class);

	// Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL
	
	
	@Test
	public void checkMemoryLengthForAllEnsembleModels_FindTheNeedleTest_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingRandomWordNeedleTest(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 3, true);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}
	
	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_RandomWords_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingRandomWords(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 3, true);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}

	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_SequenceOfNumbers_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingSequenceOfNumbers(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 3, true, 900L);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}
}
