package ntt.security.ollamadrama.simple;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.config.PreparedQueries;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.OllamaUtils;


public class DebateTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(DebateTest.class);

	@Test
	public void simpleDebate_L() {
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();
		
		// Launch LLM singletons if needed
		settings.setOllama_models("llama3.3:latest" + "," + "gemma2:latest");
		OllamaService.getInstance(settings);
		
		OllamaUtils.llmKnowledgeShootout1v1(PreparedQueries.rules_for_knowledgeshootout_on_topic("France"),
				"llama3.3:70b", 
				"gemma2:27b",
				true);
	}

	@Test
	public void simpleDebate() {
		
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();
		
		// Launch LLM singletons if needed
		settings.setOllama_models(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M);
		OllamaService.getInstance(settings);
		
		OllamaUtils.llmKnowledgeShootout1v1(PreparedQueries.rules_for_knowledgeshootout_on_topic("France"),
				"gemma2:9b", 
				"gemma2:9b",
				true);
	}
	
}
