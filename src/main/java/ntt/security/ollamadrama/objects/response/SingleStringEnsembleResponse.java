package ntt.security.ollamadrama.objects.response;

import java.util.HashMap;

public class SingleStringEnsembleResponse {

	private HashMap<String, SingleStringQuestionResponse> session_responses = new HashMap<>();
	private HashMap<String, HashMap<String, Boolean>> uniq_replies = new HashMap<>();
	private HashMap<String, HashMap<String, Boolean>> uniq_confident_replies = new HashMap<>();

	public SingleStringEnsembleResponse() {
		super();
	}

	public void addReply(String _model_name, SingleStringQuestionResponse _reply_strict) {
		session_responses.put(_model_name, _reply_strict);
	}

	public HashMap<String, SingleStringQuestionResponse> getSession_responses() {
		return session_responses;
	}

	public void setSession_responses(HashMap<String, SingleStringQuestionResponse> agent_responses) {
		this.session_responses = agent_responses;
	}

	public HashMap<String, HashMap<String, Boolean>> getUniq_replies() {
		return uniq_replies;
	}

	public void setUniq_replies(HashMap<String, HashMap<String, Boolean>> uniq_replies) {
		this.uniq_replies = uniq_replies;
	}

	public HashMap<String, HashMap<String, Boolean>> getUniq_confident_replies() {
		return uniq_confident_replies;
	}

	public void setUniq_confident_replies(HashMap<String, HashMap<String, Boolean>> uniq_confident_replies) {
		this.uniq_confident_replies = uniq_confident_replies;
	}

	public void printEnsemble() {
		for (String model_name: this.getSession_responses().keySet()) {
			SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
			System.out.println("STRICT [" + model_name + "]:\n-----------------");
			System.out.println("[" + session_response.getProbability() + "%] " + session_response.getResponse());
			System.out.println("motivation: " + session_response.getMotivation());
			System.out.println("assumptions_made: " + session_response.getAssumptions_made() + "\n");
		}
		printEnsembleSummary();
	}

	public void printEnsembleSummary() {
		if (this.getUniq_replies().isEmpty()) {
			System.out.println("no uniq responses");
		} else {
			System.out.println("uniq responses: " + this.getUniq_replies().size());
			int i = 1;
			for (String r: this.getUniq_replies().keySet()) {
				if (!"".equals(r)) {
					System.out.println("response #" + i + ": " + r);
					HashMap<String, Boolean> models = this.getUniq_replies().get(r);
					for (String model: models.keySet()) {
						System.out.println(" - " + model);
					}
					i++;
				}
			}
		}

		System.out.println("");

		if (this.getUniq_confident_replies().isEmpty()) {
			System.out.println("no uniq confident responses");
		} else {
			System.out.println("uniq confident responses: " + this.getUniq_confident_replies().size());
			int i = 1;
			for (String r: this.getUniq_confident_replies().keySet()) {
				if (!"".equals(r)) {
					System.out.println("response #" + i + ": " + r);
					HashMap<String, Boolean> models = this.getUniq_confident_replies().get(r);
					for (String model: models.keySet()) {
						System.out.println(" - " + model);
					}
					i++;
				}
			}
		}

		System.out.println("");
	}

	public String getBestResponseWithFallback(int _confident_threshold) {
		String confident_response = "";
		int max_confident_reply_size = 0;
		if (this.getUniq_confident_replies().size() >= 1) {
			for (String resp: this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				if (llms.keySet().size() >= _confident_threshold) {
					if (llms.keySet().size() > max_confident_reply_size) {
						confident_response = resp;
						max_confident_reply_size = llms.keySet().size();
					}
				}
			}
		}

		// If we have multiple confident replies but none align, pick the most certain LLM
		if ((this.getUniq_confident_replies().size() >= 1) && (max_confident_reply_size == 1)) {
			int max_confident_percent = 0;
			for (String resp: this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name: llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response.getProbability() > max_confident_percent) {
						confident_response = session_response.getResponse();
						max_confident_percent = session_response.getProbability();
					}
				}
			}
		}

		return confident_response;
	}

	public String getBestResponse(int _confident_threshold) {
		String confident_response = "";
		int max_confident_reply_size = 0;
		if (this.getUniq_confident_replies().size() >= 1) {
			for (String resp: this.getUniq_confident_replies().keySet()) {
				if (!"".equals(resp)) {
					HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
					if (llms.keySet().size() >= _confident_threshold) {
						if (llms.keySet().size() > max_confident_reply_size) {
							confident_response = resp;
							max_confident_reply_size = llms.keySet().size();
						}
					}
				}
			}
		}
		return confident_response;
	}

	public String getBestResponseWithFallback() {
		String confident_response = "";
		int max_confident_reply_size = 0;
		if (this.getUniq_confident_replies().size() >= 1) {
			for (String resp: this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				if (llms.keySet().size() > max_confident_reply_size) {
					confident_response = resp;
					max_confident_reply_size = llms.keySet().size();
				}
			}
		}

		// If we have multiple confident replies but none align, pick the most certain LLM
		if ((this.getUniq_confident_replies().size() >= 1) && (max_confident_reply_size == 1)) {
			int max_confident_percent = 0;
			for (String resp: this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name: llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response.getProbability() > max_confident_percent) {
						confident_response = session_response.getResponse();
						max_confident_percent = session_response.getProbability();
					}
				}
			}
		}

		return confident_response;
	}

	public String getRandomBestConfidentMotivation() {
		String confident_motivation = "";

		// pick the motivation from the most certain LLM
		if (this.getUniq_confident_replies().size() >= 1) {
			int max_confident_percent = 0;
			for (String resp: this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name: llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response.getProbability() > max_confident_percent) {
						confident_motivation = session_response.getMotivation();
						max_confident_percent = session_response.getProbability();
					}
				}
			}
		}

		return confident_motivation;
	}

}
