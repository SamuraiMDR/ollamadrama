package ntt.security.ollamadrama.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;

/**
 * Manages an ensemble of Ollama sessions for consensus-based responses.
 * Multiple sessions can be queried simultaneously, with their responses
 * aggregated and analyzed for confidence and agreement.
 */
public class OllamaEnsemble {

    private static final Logger LOGGER = LoggerFactory.getLogger(OllamaEnsemble.class);

    private final Map<String, OllamaWrappedSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new empty OllamaEnsemble.
     */
    public OllamaEnsemble() {
        LOGGER.debug("Created new OllamaEnsemble instance");
    }

    /**
     * Adds a wrapped session to this ensemble.
     * If a session with the same UUID already exists, it will not be replaced.
     * 
     * @param wrapped_session the wrapped session to add
     * @throws IllegalArgumentException if wrapped_session is null
     */
    public void add_wrapped_session(OllamaWrappedSession wrapped_session) {
        Objects.requireNonNull(wrapped_session, "Wrapped session cannot be null");
        Objects.requireNonNull(wrapped_session.getSession(), "Session within wrapped session cannot be null");
        
        String uuid = wrapped_session.getSession().getUuid();
        
        sessions.putIfAbsent(uuid, wrapped_session);
        
        LOGGER.debug("Added session with UUID: {} (total sessions: {})", uuid, sessions.size());
    }

    /**
     * Asks a question to all sessions in the ensemble and aggregates responses.
     * 
     * @param question the question to ask
     * @param hide_llm_reply_if_uncertain whether to hide uncertain replies
     * @param timeout_ms timeout in milliseconds for each session
     * @return ensemble response containing all replies and aggregated results
     * @throws IllegalArgumentException if question is null or empty
     * @throws IllegalArgumentException if timeout_ms is negative
     */
    public SingleStringEnsembleResponse ask_chat_question(String question, 
                                                           boolean hide_llm_reply_if_uncertain,
                                                           long timeout_ms) {
        validate_question(question);
        validate_timeout(timeout_ms);

        if (sessions.isEmpty()) {
            LOGGER.warn("No sessions in ensemble to query");
        }

        var ensemble_response = new SingleStringEnsembleResponse();
        var unique_replies = new HashMap<String, HashMap<String, Boolean>>();
        var unique_confident_replies = new HashMap<String, HashMap<String, Boolean>>();

        LOGGER.info("Querying {} sessions in ensemble with question: {}", sessions.size(), question);

        for (var entry : sessions.entrySet()) {
            String uuid = entry.getKey();
            OllamaWrappedSession wrapped_session = entry.getValue();
            
            process_session_response(
                    wrapped_session, 
                    uuid, 
                    question, 
                    hide_llm_reply_if_uncertain,
                    timeout_ms, 
                    ensemble_response, 
                    unique_replies, 
                    unique_confident_replies);
        }

        ensemble_response.setUniq_replies(unique_replies);
        ensemble_response.setUniq_confident_replies(unique_confident_replies);

        log_ensemble_results(unique_replies, unique_confident_replies);

        return ensemble_response;
    }

    /**
     * Processes a single session's response and updates aggregated data.
     */
    private void process_session_response(OllamaWrappedSession wrapped_session,
                                         String uuid,
                                         String question,
                                         boolean hide_llm_reply_if_uncertain,
                                         long timeout_ms,
                                         SingleStringEnsembleResponse ensemble_response,
                                         HashMap<String, HashMap<String, Boolean>> unique_replies,
                                         HashMap<String, HashMap<String, Boolean>> unique_confident_replies) {
        OllamaSession session = wrapped_session.getSession();
        String model_name = session.getModel_name();
        String session_key = model_name + "::" + uuid;

        try {
            SingleStringQuestionResponse response = session.askStrictChatQuestion(
                    question, 
                    hide_llm_reply_if_uncertain, 
                    timeout_ms);

            ensemble_response.addReply(session_key, response);

            String response_text = response.getResponse();
            track_unique_reply(unique_replies, response_text, session_key);

            if (is_confident_response(response, wrapped_session)) {
                track_unique_reply(unique_confident_replies, response_text, session_key);
            }

            LOGGER.debug("Session {} responded: {} (confidence: {})", 
                    session_key, 
                    response_text, 
                    response.getProbability());

        } catch (Exception e) {
            LOGGER.error("Error querying session {}: {}", session_key, e.getMessage(), e);
        }
    }

    /**
     * Tracks a unique reply by adding the session that produced it.
     */
    private void track_unique_reply(HashMap<String, HashMap<String, Boolean>> reply_map,
                                   String response_text,
                                   String session_key) {
        reply_map.computeIfAbsent(response_text, k -> new HashMap<>())
                 .put(session_key, true);
    }

    /**
     * Determines if a response meets the confidence threshold.
     */
    private boolean is_confident_response(SingleStringQuestionResponse response,
                                         OllamaWrappedSession wrapped_session) {
        Integer probability = response.getProbability();
        
        if (probability == null) {
            return false;
        }

        return probability >= wrapped_session.getProbability_threshold();
    }

    /**
     * Logs the ensemble results for debugging.
     */
    private void log_ensemble_results(HashMap<String, HashMap<String, Boolean>> unique_replies,
                                     HashMap<String, HashMap<String, Boolean>> unique_confident_replies) {
        LOGGER.info("Ensemble results: {} unique reply values, {} confident reply values", 
                unique_replies.size(), 
                unique_confident_replies.size());

        if (LOGGER.isDebugEnabled()) {
            for (var entry : unique_replies.entrySet()) {
                LOGGER.debug("Reply '{}' from {} session(s)", 
                        entry.getKey(), 
                        entry.getValue().size());
            }
        }
    }

    /**
     * Validates the question parameter.
     */
    private void validate_question(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be null or empty");
        }
    }

    /**
     * Validates the timeout parameter.
     */
    private void validate_timeout(long timeout_ms) {
        if (timeout_ms < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
    }

    /**
     * Gets the number of sessions in this ensemble.
     * 
     * @return the number of sessions
     */
    public int get_session_count() {
        return sessions.size();
    }

    /**
     * Checks if this ensemble has any sessions.
     * 
     * @return true if the ensemble contains at least one session
     */
    public boolean has_sessions() {
        return !sessions.isEmpty();
    }

    /**
     * Gets a wrapped session by UUID.
     * 
     * @param uuid the session UUID
     * @return the wrapped session, or null if not found
     */
    public OllamaWrappedSession get_session(String uuid) {
        return sessions.get(uuid);
    }

    /**
     * Removes a session from the ensemble.
     * 
     * @param uuid the UUID of the session to remove
     * @return the removed session, or null if not found
     */
    public OllamaWrappedSession remove_session(String uuid) {
        OllamaWrappedSession removed = sessions.remove(uuid);
        
        if (removed != null) {
            LOGGER.debug("Removed session with UUID: {} (remaining: {})", uuid, sessions.size());
        }
        
        return removed;
    }

    /**
     * Clears all sessions from the ensemble.
     */
    public void clear_sessions() {
        int count = sessions.size();
        sessions.clear();
        LOGGER.debug("Cleared {} sessions from ensemble", count);
    }

    /**
     * Gets an unmodifiable view of all session UUIDs.
     * 
     * @return set of session UUIDs
     */
    public java.util.Set<String> get_session_uuids() {
        return java.util.Collections.unmodifiableSet(sessions.keySet());
    }

    public void addWrappedSession(OllamaWrappedSession wrapped_session) {
        add_wrapped_session(wrapped_session);
    }

    public SingleStringEnsembleResponse askChatQuestion(String question, 
                                                        boolean hide_llm_reply_if_uncertain,
                                                        long timeout_ms) {
        return ask_chat_question(question, hide_llm_reply_if_uncertain, timeout_ms);
    }

    public int getSessionCount() {
        return get_session_count();
    }

    public boolean hasSessions() {
        return has_sessions();
    }

    public OllamaWrappedSession getSession(String uuid) {
        return get_session(uuid);
    }

    public OllamaWrappedSession removeSession(String uuid) {
        return remove_session(uuid);
    }

    public void clearSessions() {
        clear_sessions();
    }

    public java.util.Set<String> getSessionUuids() {
        return get_session_uuids();
    }
}