package ntt.security.ollamadrama.objects;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.utils.FilesUtils;
import ntt.security.ollamadrama.utils.NumUtils;
import ntt.security.ollamadrama.utils.StringsUtils;

public class ConfidenceThresholdCard {

	// modelname, query_index, acceptable_answers+scores, actual_answer
	private HashMap<String, HashMap<String, Integer>> should_be_100_proba = new HashMap<>();
	private HashMap<String, HashMap<String, Integer>> should_be_50_proba = new HashMap<>();
	private HashMap<String, HashMap<String, Integer>> should_be_0_proba = new HashMap<>();

	public ConfidenceThresholdCard() {
		super();
	}

	public HashMap<String, HashMap<String, Integer>> getShould_be_100_proba() {
		return should_be_100_proba;
	}

	public void setShould_be_100_proba(HashMap<String, HashMap<String, Integer>> should_be_100_proba) {
		this.should_be_100_proba = should_be_100_proba;
	}

	public HashMap<String, HashMap<String, Integer>> getShould_be_50_proba() {
		return should_be_50_proba;
	}

	public void setShould_be_50_proba(HashMap<String, HashMap<String, Integer>> should_be_50_proba) {
		this.should_be_50_proba = should_be_50_proba;
	}

	public HashMap<String, HashMap<String, Integer>> getShould_be_0_proba() {
		return should_be_0_proba;
	}

	public void setShould_be_0_proba(HashMap<String, HashMap<String, Integer>> should_be_0_proba) {
		this.should_be_0_proba = should_be_0_proba;
	}

	public void print() {
		System.out.println("");  // initial blank line
		
		System.out.println("----- p100 -----");
	
		// Print profile 100 replies for all models
		HashMap<String, HashMap<String, Integer>> results100 = getShould_be_100_proba();
		for (String model: results100.keySet()) {
			System.out.print(StringsUtils.cutAndPadStringToN(model, 30) + ":");
			HashMap<String, Integer> entries = results100.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				System.out.print(" " + StringsUtils.cutAndPadStringToN("" + val, 3));
			}
			System.out.println("");
		}
		
		System.out.println("----- p50 -----"); 
		
		// Print profile 50 replies for all models
		HashMap<String, HashMap<String, Integer>> results50 = getShould_be_50_proba();
		for (String model: results50.keySet()) {
			System.out.print(StringsUtils.cutAndPadStringToN(model, 30) + ":");
			HashMap<String, Integer> entries = results50.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				System.out.print(" " + StringsUtils.cutAndPadStringToN("" + val, 3));
			}
			System.out.println("");
		}
		
		System.out.println("----- p0 -----");
		
		// Print profile 0 replies for all models
		HashMap<String, HashMap<String, Integer>> results0 = getShould_be_0_proba();
		for (String model: results0.keySet()) {
			System.out.print(StringsUtils.cutAndPadStringToN(model, 30) + ":");
			HashMap<String, Integer> entries = results0.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				System.out.print(" " + StringsUtils.cutAndPadStringToN("" + val, 3));
			}
			System.out.println("");
		}
		System.out.println("-----");  
		
	}

	public void flushProbas(String _outfile) {
		FilesUtils.writeToFileUNIXNoException("modelname,queryindex,type,proba", _outfile);

		// Print profile 100 replies for all models
		HashMap<String, HashMap<String, Integer>> results100 = getShould_be_100_proba();
		for (String model: results100.keySet()) {
			HashMap<String, Integer> entries = results100.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				FilesUtils.appendToFileUNIXNoException(model + "," + q + ",t100," + val, _outfile);
			}
		}
		
		// Print profile 50 replies for all models
		HashMap<String, HashMap<String, Integer>> results50 = getShould_be_50_proba();
		for (String model: results50.keySet()) {
			HashMap<String, Integer> entries = results50.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				FilesUtils.appendToFileUNIXNoException(model + "," + q + ",t50," + val, _outfile);
			}
		}
		
		// Print profile 0 replies for all models
		HashMap<String, HashMap<String, Integer>> results0 = getShould_be_0_proba();
		for (String model: results0.keySet()) {
			HashMap<String, Integer> entries = results0.get(model);
			for (String q: entries.keySet()) {
				Integer val = entries.get(q);
				FilesUtils.appendToFileUNIXNoException(model + "," + q + ",t00," + val, _outfile);
			}
		}
		
	}

}
