package ntt.security.ollamadrama.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepared prompt templates for Ollama RAG and debate scenarios.
 */
public final class PreparedQueries {

    @SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(PreparedQueries.class);

    // Prevent instantiation – this is a utility class
    private PreparedQueries() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ──────────────────────────────────────────────────────────────
    // Helper for triple-quoted blocks 
    // ──────────────────────────────────────────────────────────────
    private static String triple_quote(String content) {
        return String.format("'''\n%s\n'''", content);
    }

    // ──────────────────────────────────────────────────────────────
    // RAG variants
    // ──────────────────────────────────────────────────────────────

    public static String rag_prefix(String rag_content, String prompt) {
        return String.format("""
            Using this data:
            %s
            
            Respond to this prompt: 
            %s
            """,
            triple_quote(rag_content),
            triple_quote(prompt)
        ).stripTrailing();
    }

    public static String rag_suffix(String rag_content, String prompt) {
        return String.format("""
            Respond to this prompt: 
            %s
            Using this data:
            %s
            """,
            triple_quote(prompt),
            triple_quote(rag_content)
        ).stripTrailing();
    }

    public static String rag_rules_suffix(String rules_content, String prompt) {
        return String.format("""
            Respond to this prompt: 
            %s
            Using these rules:
            %s
            """,
            triple_quote(prompt),
            triple_quote(rules_content)
        ).stripTrailing();
    }

    // ──────────────────────────────────────────────────────────────
    // Debate / Knowledge Shootout rules
    // ──────────────────────────────────────────────────────────────

    public static String rules_for_knowledgeshootout_on_topic(String topic) {
        return String.format("""
            You are an expert on the topic '%s' and about to prove yourself in a debate with an opponent.

            An overview of your task:
             - You will take turns with your opponent, answering questions on the topic and stating challenging questions of your own.
             - Both you and your opponent will be exposed to all questions and answers.
             - After you state a question you will receive the opponent's reply before asking your next question.
             - When you receive the opponent's answer you must give it a score between 0 and 10 (10 = perfect).

            Rules for the debate:
             - Stay strictly on topic.
             - Keep questions and answers concise.
             - Increase difficulty with every new question.
            """, topic).stripTrailing();
    }
}