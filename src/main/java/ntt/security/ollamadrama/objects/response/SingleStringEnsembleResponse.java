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

	public String getEnsemble() {
		StringBuffer sb = new StringBuffer();
		for (String model_name : this.getSession_responses().keySet()) {
			SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
			sb.append("STRICT [" + model_name + "]:\n-----------------" + "\n");
			sb.append("[" + session_response.getProbability() + "%] " + session_response.getResponse() + "\n");
			sb.append("motivation: " + session_response.getMotivation() + "\n");
			sb.append("assumptions_made: " + session_response.getAssumptions_made() + "\n\n");
		}
		return sb.toString();
	}

	public void printEnsemble() {
		System.out.println(getEnsemble());
		printEnsembleSummary();
	}

	public void printEnsembleSummary() {
		if (this.getUniq_replies().isEmpty()) {
			System.out.println("no uniq responses");
		} else {
			System.out.println("uniq responses: " + this.getUniq_replies().size());
			int i = 1;
			for (String r : this.getUniq_replies().keySet()) {
				if (!"".equals(r)) {
					System.out.println("response #" + i + ": " + r);
					HashMap<String, Boolean> models = this.getUniq_replies().get(r);
					for (String model : models.keySet()) {
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
			for (String r : this.getUniq_confident_replies().keySet()) {
				if (!"".equals(r)) {
					System.out.println("response #" + i + ": " + r);
					HashMap<String, Boolean> models = this.getUniq_confident_replies().get(r);
					for (String model : models.keySet()) {
						System.out.println(" - " + model);
					}
					i++;
				}
			}
		}

		System.out.println("");
	}

	// -------------------------------------------------------------------------
	// Private helper: deterministic winner selection with three-level tie-break:
	//   1. most confident votes >= _confident_threshold
	//   2. highest max-probability among supporting models
	//   3. lexicographic order on the response string
	// -------------------------------------------------------------------------
	private String selectWinningResponse(int _confident_threshold) {
		String winning_response = "";
		int max_vote_size = 0;
		int max_tiebreak_probability = -1;

		if (this.getUniq_confident_replies().isEmpty()) {
			return winning_response;
		}

		for (String resp : this.getUniq_confident_replies().keySet()) {
			if ("".equals(resp)) continue;

			HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
			int vote_count = llms.keySet().size();

			if (vote_count < _confident_threshold) continue;

			// Find the max probability among models that voted for this response
			int resp_max_probability = 0;
			for (String model_name : llms.keySet()) {
				SingleStringQuestionResponse sr = this.getSession_responses().get(model_name);
				if (sr != null && sr.getProbability() > resp_max_probability) {
					resp_max_probability = sr.getProbability();
				}
			}

			boolean is_better = false;
			if (vote_count > max_vote_size) {
				is_better = true;
			} else if (vote_count == max_vote_size) {
				if (resp_max_probability > max_tiebreak_probability) {
					is_better = true;
				} else if (resp_max_probability == max_tiebreak_probability) {
					// Final tiebreaker: lexicographic — fully deterministic regardless of map order
					is_better = resp.compareTo(winning_response) < 0;
				}
			}

			if (is_better) {
				winning_response = resp;
				max_vote_size = vote_count;
				max_tiebreak_probability = resp_max_probability;
			}
		}

		return winning_response;
	}

	// -------------------------------------------------------------------------
	// getBestResponse variants
	// -------------------------------------------------------------------------

	public String getBestResponseWithFallback(int _confident_threshold) {
		String confident_response = "";
		int max_confident_reply_size = 0;
		if (this.getUniq_confident_replies().size() >= 1) {
			for (String resp : this.getUniq_confident_replies().keySet()) {
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
			for (String resp : this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name : llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response != null && session_response.getProbability() > max_confident_percent) {
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
			for (String resp : this.getUniq_confident_replies().keySet()) {
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

	public String getBestResponse(int _confident_threshold, boolean _deterministic) {
		if (!_deterministic) {
			return getBestResponse(_confident_threshold);
		}
		return selectWinningResponse(_confident_threshold);
	}

	public String getBestResponseWithFallback() {
		String confident_response = "";
		int max_confident_reply_size = 0;
		if (this.getUniq_confident_replies().size() >= 1) {
			for (String resp : this.getUniq_confident_replies().keySet()) {
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
			for (String resp : this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name : llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response != null && session_response.getProbability() > max_confident_percent) {
						confident_response = session_response.getResponse();
						max_confident_percent = session_response.getProbability();
					}
				}
			}
		}

		return confident_response;
	}

	// -------------------------------------------------------------------------
	// getRandomBestConfidentMotivation variants
	// -------------------------------------------------------------------------

	public String getBestConfidentMotivation() {
		String confident_motivation = "";

		// Pick the motivation from the most certain LLM across all confident replies
		if (this.getUniq_confident_replies().size() >= 1) {
			int max_confident_percent = 0;
			for (String resp : this.getUniq_confident_replies().keySet()) {
				HashMap<String, Boolean> llms = this.getUniq_confident_replies().get(resp);
				for (String model_name : llms.keySet()) {
					SingleStringQuestionResponse session_response = this.getSession_responses().get(model_name);
					if (session_response != null && session_response.getProbability() > max_confident_percent) {
						confident_motivation = session_response.getMotivation();
						max_confident_percent = session_response.getProbability();
					}
				}
			}
		}

		return confident_motivation;
	}

	public String getRandomBestConfidentMotivation(int _confident_threshold) {
		String winning_response = getBestResponse(_confident_threshold);
		if ("".equals(winning_response)) {
			return "";
		}

		// Pick motivation from the highest-probability model that voted for the winner
		String confident_motivation = "";
		int max_confident_percent = 0;
		HashMap<String, Boolean> winning_llms = this.getUniq_confident_replies().get(winning_response);
		if (winning_llms == null) {
			return confident_motivation;
		}
		for (String model_name : winning_llms.keySet()) {
			SingleStringQuestionResponse sr = this.getSession_responses().get(model_name);
			if (sr != null && sr.getProbability() > max_confident_percent) {
				confident_motivation = sr.getMotivation();
				max_confident_percent = sr.getProbability();
			}
		}

		return confident_motivation;
	}

	public String getRandomBestConfidentMotivation(int _confident_threshold, boolean _deterministic) {
		if (!_deterministic) {
			return getRandomBestConfidentMotivation(_confident_threshold);
		}

		String winning_response = selectWinningResponse(_confident_threshold);
		if ("".equals(winning_response)) {
			return "";
		}

		// Pick motivation from the highest-probability model that voted for the winner
		String confident_motivation = "";
		int max_confident_percent = 0;
		HashMap<String, Boolean> winning_llms = this.getUniq_confident_replies().get(winning_response);
		if (winning_llms == null) {
			return confident_motivation;
		}
		for (String model_name : winning_llms.keySet()) {
			SingleStringQuestionResponse sr = this.getSession_responses().get(model_name);
			if (sr != null && sr.getProbability() > max_confident_percent) {
				confident_motivation = sr.getMotivation();
				max_confident_percent = sr.getProbability();
			}
		}

		return confident_motivation;
	}

}