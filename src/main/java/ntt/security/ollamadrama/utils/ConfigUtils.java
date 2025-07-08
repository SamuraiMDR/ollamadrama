package ntt.security.ollamadrama.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.OllamaDramaSettings;

public class ConfigUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUtils.class);

	public static OllamaDramaSettings parseConfigENV() {
		OllamaDramaSettings settings = new OllamaDramaSettings();
		String settings_env = System.getenv("OLLAMADRAMACONFIG");
		if (null == settings_env) {
			LOGGER.info("OLLAMADRAMACONFIG env variable not set");
		} else {
			LOGGER.info("OLLAMADRAMACONFIG env variable set");
			settings = JSONUtils.createPOJOFromJSONOpportunistic(settings_env, OllamaDramaSettings.class);
			if (null == settings) {
				LOGGER.warn("Unable to parse the provided OLLAMADRAMACONFIG");
			} else {
				if (null != settings.getOllama_username()) LOGGER.info("Ollama username: " + settings.getOllama_username());
				if (null != settings.getOllama_password()) LOGGER.info("Ollama password:" + StringUtils.repeat('_', settings.getOllama_password().length()));
				if (null != settings.getOllama_models()) LOGGER.info("Ollama models:" + settings.getOllama_models());
				LOGGER.info("Ollama timeout:" + settings.getOllama_timeout());
				if (null != settings.getThreadPoolCount()) LOGGER.info("ThreadPoolCount:" + settings.getThreadPoolCount());
			}
		}
		return settings;
	}
}
