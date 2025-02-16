package ntt.security.ollamadrama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreparedQueries {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(PreparedQueries.class);
	
	// https://ollama.com/blog/embedding-models
	public static String askQuestionWithRAGPrefix (String _rag, String _prompt) {
		return "Using this data:\n" 
				+ "'''\n"
				+ _rag
				+ "'''\n"
				+ "\nRespond to this prompt: \n"
				+ "'''\n"
				+ _prompt
				+ "'''\n";
	}
	
	public static String askQuestionWithRAGSuffix (String _rag, String _prompt) {
		return "Respond to this prompt: \n"
				+ "'''\n"
				+ _prompt
				+ "'''\n"
				+ "Using this data:\n" 
				+ "'''\n"
				+ _rag
				+ "'''\n";
	}
	
	public static String askQuestionWithRAGRulesSuffix (String _rulesrag, String _prompt) {
		return "Respond to this prompt: \n"
				+ "'''\n"
				+ _prompt
				+ "'''\n"
				+ "Using these rules:\n" 
				+ "'''\n"
				+ _rulesrag
				+ "'''\n";
	}

	public static String rules_for_knowledgeshootout_on_topic (String _topic) {
		return "You are an expert on the topic '" + _topic + "' and about to prove yourself in a debate with an opponent.\n"
				+ "An overview of your task:\n"
				+ " - You will take turns with your opponent, answering questions on the topic and stating challenging questions of your own.\n"
				+ " - Both you and your opponent will be exposed to all questions and answers.\n"
				+ " - After you have stated a question you will be given the opponents reply before getting a question from your opponent.\n"
				+ " - When you receive your opponents answer you should give it a score between 0 and 10, where 10 represents a correct reply.\n"
				+ "\n"
				+ "Some rules for the debate:\n"
				+ " - You need to stay on topic.\n"
				+ " - Keep you questions and answers brief.\n"
				+ " - Increase the difficulty with every question.\n"
				+ "";
	}
	
}
