package ntt.security.ollamadrama.objects;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;

/**
 * Manages an ensemble of OpenAI sessions for consensus-based responses.
 * Multiple sessions can be queried simultaneously, with their responses
 * aggregated and analyzed for confidence and agreement.
 */
public class OpenAIEnsemble {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIEnsemble.class);

    private final ConcurrentHashMap<String, OpenAIWrappedSession> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new empty OpenAIEnsemble.
     */
    public OpenAIEnsemble() {
        LOGGER.debug("Created new OpenAIEnsemble instance");
    }

    /**
     * Adds a wrapped session to this ensemble.
     * If a session with the same UUID already exists, it will not be replaced.
     * 
     * @param wrapped_session the wrapped session to add
     * @throws IllegalArgumentException if wrapped_session is null
     */
    public void add_wrapped_session(OpenAIWrappedSession wrapped_session) {
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
     * @return ensemble response containing all replies and aggregated results
     * @throws IllegalArgumentException if question is null or empty
     */
    public SingleStringEnsembleResponse ask_chat_question(String question, 
                                                           boolean hide_llm_reply_if_uncertain) {
        validate_question(question);

        if (sessions.isEmpty()) {
            LOGGER.warn("No sessions in ensemble to query");
        }

        var ensemble_response = new SingleStringEnsembleResponse();
        var unique_replies = new HashMap<String, HashMap<String, Boolean>>();
        var unique_confident_replies = new HashMap<String, HashMap<String, Boolean>>();

        LOGGER.info("Querying {} OpenAI sessions in ensemble with question: {}", sessions.size(), question);

        for (var entry : sessions.entrySet()) {
            String uuid = entry.getKey();
            OpenAIWrappedSession wrapped_session = entry.getValue();
            
            process_session_response(
                    wrapped_session, 
                    uuid, 
                    question, 
                    hide_llm_reply_if_uncertain,
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
    private void process_session_response(OpenAIWrappedSession wrapped_session,
                                         String uuid,
                                         String question,
                                         boolean hide_llm_reply_if_uncertain,
                                         SingleStringEnsembleResponse ensemble_response,
                                         HashMap<String, HashMap<String, Boolean>> unique_replies,
                                         HashMap<String, HashMap<String, Boolean>> unique_confident_replies) {
        OpenAISession session = wrapped_session.getSession();
        String model_name = session.getModel_name();
        String session_key = model_name + "::" + uuid;

        try {
            SingleStringQuestionResponse response = session.askChatQuestion(
                    question, 
                    hide_llm_reply_if_uncertain);

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
            LOGGER.error("Error querying OpenAI session {}: {}", session_key, e.getMessage(), e);
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
                                         OpenAIWrappedSession wrapped_session) {
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
        LOGGER.info("Ensemble results: {} unique replies, {} confident replies", 
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
    public OpenAIWrappedSession get_session(String uuid) {
        return sessions.get(uuid);
    }

    /**
     * Removes a session from the ensemble.
     * 
     * @param uuid the UUID of the session to remove
     * @return the removed session, or null if not found
     */
    public OpenAIWrappedSession remove_session(String uuid) {
        OpenAIWrappedSession removed = sessions.remove(uuid);
        
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

    // ========== BACKWARD COMPATIBILITY WRAPPERS (DEPRECATED) ==========

    /**
     * @deprecated Use {@link #add_wrapped_session(OpenAIWrappedSession)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public void addWrappedSession(OpenAIWrappedSession wrapped_session) {
        add_wrapped_session(wrapped_session);
    }

    /**
     * @deprecated Use {@link #ask_chat_question(String, boolean)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public SingleStringEnsembleResponse askChatQuestion(String question, 
                                                        boolean hide_llm_reply_if_uncertain) {
        return ask_chat_question(question, hide_llm_reply_if_uncertain);
    }

    /**
     * @deprecated Use {@link #get_session_count()} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public int getSessionCount() {
        return get_session_count();
    }

    /**
     * @deprecated Use {@link #has_sessions()} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public boolean hasSessions() {
        return has_sessions();
    }

    /**
     * @deprecated Use {@link #get_session(String)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public OpenAIWrappedSession getSession(String uuid) {
        return get_session(uuid);
    }

    /**
     * @deprecated Use {@link #remove_session(String)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public OpenAIWrappedSession removeSession(String uuid) {
        return remove_session(uuid);
    }

    /**
     * @deprecated Use {@link #clear_sessions()} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public void clearSessions() {
        clear_sessions();
    }

    /**
     * @deprecated Use {@link #get_session_uuids()} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public java.util.Set<String> getSessionUuids() {
        return get_session_uuids();
    }
}