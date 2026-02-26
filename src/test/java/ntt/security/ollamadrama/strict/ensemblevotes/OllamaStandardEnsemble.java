package ntt.security.ollamadrama.strict.ensemblevotes;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.utils.OllamaUtils;


public class OllamaStandardEnsemble {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaStandardEnsemble.class);

	@Test
	public void strict_ENSAMBLE_Ollama_POP3() {
		
		String selected_models = Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_MINIDIVERSE_L;
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setOllama_scan(false);
		settings.setOllama_models(selected_models);
		settings.setAutopull_max_llm_size("XL");
		
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun( 
				"What TCP port does the POP3 protocol typically use? Answer with only a number.",
				selected_models, settings, true, false);
		sser1.printEnsemble();
		
		/*
		uniq confident responses: 1
		response #1: 110
		 - qwen2.5:7b::262b41c9-9e5a-49f5-bf5b-062f78da30a7
		 - gemma2:9b::9531e75a-5e69-41e2-8d4f-33574a855606
		 - marco-o1:7b::ae397a4a-58a2-4ba7-8727-ef06e6d970da
		 */
		
	}

}
