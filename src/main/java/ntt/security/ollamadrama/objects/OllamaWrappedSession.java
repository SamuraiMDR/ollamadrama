package ntt.security.ollamadrama.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.utils.SystemUtils;

public class OllamaWrappedSession {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaWrappedSession.class);
	
	private OllamaSession session;
	private Integer probability_threshold;
	
	public OllamaWrappedSession(OllamaSession _session, Integer _probability_threshold) {
		super();
		this.session = _session;
		this.probability_threshold = _probability_threshold;
		
		// Sanity
		if (null == _session) {
			LOGGER.error("Provided agent cannot be null");
			SystemUtils.halt();
		}
		if (null == _probability_threshold) {
			LOGGER.info("Default probability_threshold (70) used for " + this.session.getModel_name());
			this.probability_threshold = 70;
		}
	}

	public OllamaSession getSession() {
		return session;
	}

	public void setAgent(OllamaSession agent) {
		this.session = agent;
	}

	public Integer getProbability_threshold() {
		return probability_threshold;
	}

	public void setProbability_threshold(Integer probability_threshold) {
		this.probability_threshold = probability_threshold;
	}

}
