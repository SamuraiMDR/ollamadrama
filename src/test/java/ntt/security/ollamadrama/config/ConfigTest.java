package ntt.security.ollamadrama.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.utils.JSONUtils;


public class ConfigTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigTest.class);

	@Test
	public void flushOllamaDramaConfig() {

		OllamaDramaSettings s = new OllamaDramaSettings();
		OllamaEndpoint ep = new OllamaEndpoint("http://some.ollama.endpoint.com:11434", "someuser", "somepassword");
		s.addOllamaCustomEndpoint(ep);
		
		String json = JSONUtils.createJSONFromPOJO(s);
		System.out.println(json);
		
		// Assert
		assertTrue("Make sure we have a sane JSON string", json.length()>10);
	}
	
}
