package ntt.security.ollamadrama.builddate;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.utils.OllamaUtils;

public class BuildDateTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(BuildDateTest.class);

	//@Ignore
	@Test
	public void findBuildDateForAllModels_Test_CSV() {
		
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				"What is your knowledge cutoff date (or training completion date) of your model? Provide the most "
				+ "precise information available. If you do not know, try to provide a rough estimate.",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL);
		sser1.printEnsemble();
		
	}
	
}
