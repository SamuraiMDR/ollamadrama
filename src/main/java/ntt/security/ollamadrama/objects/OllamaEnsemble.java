package ntt.security.ollamadrama.objects;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;

public class OllamaEnsemble {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaEnsemble.class);

	private HashMap<String, OllamaWrappedSession> sessions = new HashMap<>();

	public void addWrappedSession(OllamaWrappedSession _ollamaWrappedSession) {
		if (null == sessions.get(_ollamaWrappedSession.getSession().getUuid())) {
			sessions.put(_ollamaWrappedSession.getSession().getUuid(), _ollamaWrappedSession);
		}
	}

	public SingleStringEnsembleResponse askChatQuestion(String _question, boolean _hide_llm_reply_if_uncertain, long _timeout) {

		SingleStringEnsembleResponse ensemble_response = new SingleStringEnsembleResponse();

		HashMap<String, HashMap<String, Boolean>> uniq_replies = new HashMap<>();
		HashMap<String, HashMap<String, Boolean>> uniq_confident_replies = new HashMap<>();

		for (String uuid: sessions.keySet()) {
			OllamaWrappedSession wa1 = sessions.get(uuid);
			OllamaSession a1 = wa1.getSession();
			String model_name = a1.getModel_name();

			SingleStringQuestionResponse a1_reply = a1.askStrictChatQuestion(_question, _hide_llm_reply_if_uncertain, _timeout);
			ensemble_response.addReply(model_name + "::" + uuid, a1_reply);

			HashMap<String, Boolean> c1 = uniq_replies.get(a1_reply.getResponse());
			if (null == c1) c1 = new HashMap<>();
			c1.put(model_name + "::" + uuid, true);
			uniq_replies.put(a1_reply.getResponse(), c1);

			if (null != a1_reply.getProbability()) {
				if (a1_reply.getProbability() >= wa1.getProbability_threshold()) {
					HashMap<String, Boolean> c2 = uniq_confident_replies.get(a1_reply.getResponse());
					if (null == c2) c2 = new HashMap<>();
					c2.put(model_name + "::" + uuid, true);
					uniq_confident_replies.put(a1_reply.getResponse(), c2);
				}
			}
		}

		ensemble_response.setUniq_confident_replies(uniq_confident_replies);
		ensemble_response.setUniq_replies(uniq_replies);

		return ensemble_response;
	}

}
