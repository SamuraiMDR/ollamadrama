package ntt.security.ollamadrama.objects;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.FilesUtils;
import ntt.security.ollamadrama.utils.NumUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.StringsUtils;

public class ModelsScoreCard {

	// modelname, query_index, acceptable_answers+scores, actual_answer
	private HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>>> scorecard = new HashMap<>();
	private HashMap<String, Integer> pos_scores = new HashMap<>();
	private HashMap<String, Integer> neg_scores = new HashMap<>();

	public ModelsScoreCard() {
		super();
	}

	public ModelsScoreCard(HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>>> scorecard) {
		super();
		this.scorecard = scorecard;
	}

	public HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>>> getScorecard() {
		return scorecard;
	}
	public void setScorecard(HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>>> scorecard) {
		this.scorecard = scorecard;
	}
	public HashMap<String, Integer> getPos_scores() {
		return pos_scores;
	}
	public void setPos_scores(HashMap<String, Integer> pos_scores) {
		this.pos_scores = pos_scores;
	}
	public HashMap<String, Integer> getNeg_scores() {
		return neg_scores;
	}
	public void setNeg_scores(HashMap<String, Integer> neg_scores) {
		this.neg_scores = neg_scores;
	}

	public void evaluate() {
		for (String modelName : getScorecard().keySet()) {
			System.out.println(modelName + ":");
			HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>> modelScorecard = getScorecard().get(modelName);
			evaluateModel(modelName, modelScorecard);
		}
	}

	private void evaluateModel(String modelName, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>> modelScorecard) {
		for (int qIndex = 1; qIndex <= modelScorecard.size(); qIndex++) {
			HashMap<HashMap<String, Integer>, SingleStringQuestionResponse> expectedVsActual = modelScorecard.get("q" + qIndex);
			evaluateQuestions(modelName, qIndex, expectedVsActual);
		}
	}

	private void evaluateQuestions(String modelName, int qIndex, HashMap<HashMap<String, Integer>, SingleStringQuestionResponse> expectedVsActual) {
		System.out.println(" * " + StringsUtils.cutAndPadStringToN("q" + qIndex, 2));
		for (Entry<HashMap<String, Integer>, SingleStringQuestionResponse> entry : expectedVsActual.entrySet()) {
			SingleStringQuestionResponse actual_response = entry.getValue();
			HashMap<String, Integer> acceptable = entry.getKey();
			updateScores(modelName, acceptable, actual_response.getResponse());
			System.out.println(StringsUtils.cutAndPadStringToN("  - actual_reply ", 28) + ": " + actual_response.getResponse());
			for (String expected_val: acceptable.keySet()) {
				Integer rec_score = acceptable.get(expected_val);
				System.out.println(StringsUtils.cutAndPadStringToN("  - acceptable_reply [" + rec_score + "] ", 28) + ": " + expected_val);
			}
		}
	}

	private void updateScores(String modelName, HashMap<String, Integer> acceptable, String actual) {
		int pos = pos_scores.getOrDefault(modelName, 0);
		int neg = neg_scores.getOrDefault(modelName, 0);

		boolean acceptable_found = false;
		for (String acceptable_val: acceptable.keySet()) {
			Integer reward = acceptable.get(acceptable_val);
			if (actual.equals(acceptable_val)) {
				pos = pos + reward;
				acceptable_found = true;
			}
		}

		if (!acceptable_found) {
			neg++;
		}

		pos_scores.put(modelName, pos);
		neg_scores.put(modelName, neg);
	}

	public void flushProbas(String _question, String _outfile) {
		for (String model_name : this.getScorecard().keySet()) {
			HashMap<String, HashMap<HashMap<String, Integer>, SingleStringQuestionResponse>> c1 = this.getScorecard().get(model_name);
			for (String question_index: c1.keySet()) {
				HashMap<HashMap<String, Integer>, SingleStringQuestionResponse> c2 = c1.get(question_index);
				if (null != c2) {
					for (HashMap<String, Integer> k2: c2.keySet()) {
						SingleStringQuestionResponse ssqr = c2.get(k2);
						File f = new File(_outfile);
						if (!f.exists()) FilesUtils.writeToFileUNIXNoException("model_name,question,probability", _outfile);
						int proba_with_jitter = ssqr.getProbability() + NumUtils.randomNumWithinRangeAsInt(0, 3);
						if (proba_with_jitter > 100) proba_with_jitter = 100;
						if (proba_with_jitter < 0) proba_with_jitter = 0;
						FilesUtils.appendToFileUNIXNoException(model_name + "," + _question + "," + proba_with_jitter, _outfile);
					}
				}
			}
		}
	}

	public void print() {
		System.out.println("");  // initial blank line

		// 1. Print models with positive scores
		System.out.println("POSITIVE:");
		HashMap<String, Boolean> reported = new HashMap<>();
		for (int threshold = 10; threshold > 0; threshold--) {
			int matches = 0;
			for (String model_name : this.getScorecard().keySet()) {
				if (null == reported.get(model_name)) {
					Integer pos = pos_scores.get(model_name);
					Integer neg = neg_scores.get(model_name);

					// Check if the model has a positive score
					if (pos != null && pos >= threshold) {
						System.out.println(
								StringsUtils.cutAndPadStringToN(model_name, 45)
								+ " pos: " + StringsUtils.cutAndPadStringToN("" + pos, 3)
								+ " neg: " + StringsUtils.cutAndPadStringToN("" + neg, 3)
								+ " [" + StringsUtils.cutAndPadStringToN(OllamaUtils.resolve_size(model_name).toString(),3) + "] [" + StringsUtils.cutAndPadStringToN(OllamaUtils.resolve_tier(model_name).toString(), 5) + "]"
								);
						reported.put(model_name, true);
						matches++;
					}
				}
			}

			// 2. Print delimiter
			if (matches>0) System.out.println("-----");  

		}

		// 3. Print models that are negative (<= 0 or null)
		System.out.println("NEGATIVE:");
		for (String model_name : this.getScorecard().keySet()) {
			Integer pos = pos_scores.get(model_name);
			Integer neg = neg_scores.get(model_name);

			// Print if pos <= 0 or pos is null
			if (pos == null || pos <= 0) {
				System.out.println(
						StringsUtils.cutAndPadStringToN(model_name, 45)
						+ " pos: " + StringsUtils.cutAndPadStringToN("" + pos, 3)
						+ " neg: " + StringsUtils.cutAndPadStringToN("" + neg, 3)
						+ " [" + StringsUtils.cutAndPadStringToN(OllamaUtils.resolve_size(model_name).toString(),3) + "] [" + StringsUtils.cutAndPadStringToN(OllamaUtils.resolve_tier(model_name).toString(), 5) + "]"
						);
			}
		}
	}

	public boolean isCleanSheetPositive(String _models) {
		HashMap<String, Boolean> selected_clean = new HashMap<>();
		for (String model_name: _models.split(",")) {
			selected_clean.put(model_name, true);
		}
		boolean result = true;
		for (String model_name: this.getScorecard().keySet()) {
			if (null != selected_clean.get(model_name)) {
				Integer pos = pos_scores.get(model_name);
				Integer neg = neg_scores.get(model_name);
				if ( (pos >= 0) && (neg == 0)) {
					// all ok
				} else {
					result = false;
				}
			}
		}
		return result;
	}

	public boolean isCleanSheetNegative(String _models) {
		HashMap<String, Boolean> selected_clean = new HashMap<>();
		for (String model_name: _models.split(",")) {
			selected_clean.put(model_name, true);
		}
		boolean result = true;
		for (String model_name: this.getScorecard().keySet()) {
			if (null != selected_clean.get(model_name)) {
				Integer pos = pos_scores.get(model_name);
				Integer neg = neg_scores.get(model_name);
				if ( (pos == 0) && (neg > 0)) {
					// all ok
				} else {
					result = false;
				}
			}
		}
		return result;
	}

}
