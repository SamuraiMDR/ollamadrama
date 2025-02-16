package ntt.security.ollamadrama.objects;

import java.util.HashMap;
import java.util.Map.Entry;

import ntt.security.ollamadrama.utils.StringsUtils;

public class ModelsScoreCard {

	// modelname, query_index, acceptable_answers+scores, actual_answer
	private HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, String>>> scorecard = new HashMap<>();
	private HashMap<String, Integer> pos_scores = new HashMap<>();
	private HashMap<String, Integer> neg_scores = new HashMap<>();

	public ModelsScoreCard() {
		super();
	}

	public ModelsScoreCard(HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, String>>> scorecard) {
		super();
		this.scorecard = scorecard;
	}

	public HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, String>>> getScorecard() {
		return scorecard;
	}
	public void setScorecard(HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, String>>> scorecard) {
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
			HashMap<String, HashMap<HashMap<String,Integer>, String>> modelScorecard = getScorecard().get(modelName);
			evaluateModel(modelName, modelScorecard);
		}
	}

	private void evaluateModel(String modelName, HashMap<String, HashMap<HashMap<String,Integer>, String>> modelScorecard) {
		for (int qIndex = 1; qIndex <= modelScorecard.size(); qIndex++) {
			HashMap<HashMap<String, Integer>, String> expectedVsActual = modelScorecard.get("q" + qIndex);
			evaluateQuestions(modelName, qIndex, expectedVsActual);
		}
	}

	private void evaluateQuestions(String modelName, int qIndex, HashMap<HashMap<String, Integer>, String> expectedVsActual) {
		System.out.println(" * " + StringsUtils.cutAndPadStringToN("q" + qIndex, 2));
		for (Entry<HashMap<String, Integer>, String> entry : expectedVsActual.entrySet()) {
			String actual = entry.getValue();
			HashMap<String, Integer> acceptable = entry.getKey();
			updateScores(modelName, acceptable, actual);

			System.out.println(StringsUtils.cutAndPadStringToN("  - actual_reply ", 28) + ": " + actual);
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

	public void print() {
		System.out.println("");  // initial blank line

		// 1. Print models with positive scores
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
								StringsUtils.cutAndPadStringToN(model_name, 40)
								+ " pos: " + StringsUtils.cutAndPadStringToN("" + pos, 3)
								+ " neg: " + StringsUtils.cutAndPadStringToN("" + neg, 3)
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
		for (String model_name : this.getScorecard().keySet()) {
			Integer pos = pos_scores.get(model_name);
			Integer neg = neg_scores.get(model_name);

			// Print if pos <= 0 or pos is null
			if (pos == null || pos <= 0) {
				System.out.println(
						StringsUtils.cutAndPadStringToN(model_name, 40)
						+ " pos: " + StringsUtils.cutAndPadStringToN("" + pos, 3)
						+ " neg: " + StringsUtils.cutAndPadStringToN("" + neg, 3)
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
