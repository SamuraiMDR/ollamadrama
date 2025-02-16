package ntt.security.ollamadrama.ensemblevotes;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.singletons.OpenAIService;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.OpenAIUtils;


public class TCPIPTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(TCPIPTest.class);

	@Test
	public void strict_ENSAMBLE_Ollama_POP3() {
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				"What TCP port does the POP3 protocol typically use? Answer with only a number.",
				Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M_MINIDIVERSE, true);
		sser1.printEnsemble();
		
		/*
		uniq confident responses: 1
		response #1: 110
		 - openchat:7b::7c820ae7-dc37-4ddc-b0e0-af70f94a0a58
		 - gemma2:9b::d16938f1-027b-451a-986c-f5fdbb15fdf3
		 - qwen2.5:7b::e174ffba-447b-4121-809d-e1a4d4560c7c
		 */
	}

}
