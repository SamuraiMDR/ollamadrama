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

	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_RandomWords_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingRandomWords(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L, 5);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}

	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_SequenceOfNumbers_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingSequenceOfNumbers(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L, 3);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}
}
