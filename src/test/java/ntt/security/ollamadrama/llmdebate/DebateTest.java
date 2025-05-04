package ntt.security.ollamadrama.llmdebate;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.PreparedQueries;
import ntt.security.ollamadrama.utils.OllamaUtils;


public class DebateTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(DebateTest.class);

	@Test
	public void simpleDebate_XL_vs_L() {
		OllamaUtils.llmKnowledgeShootout1v1(PreparedQueries.rules_for_knowledgeshootout_on_topic("France"),
				"llama3.3:70b", 
				"gemma2:27b");
	}

	@Test
	public void simpleDebate_M_vs_M() {
		OllamaUtils.llmKnowledgeShootout1v1(PreparedQueries.rules_for_knowledgeshootout_on_topic("France"),
				"gemma2:9b", 
				"gemma2:9b");
	}
	
}
