package ntt.security.ollamadrama.singletons;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.service.OpenAiService;

import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;
import ntt.security.ollamadrama.utils.*;

/**
 * Singleton service for managing OpenAI API connections and sessions.
 * This service handles OpenAI session creation with configured settings.
 */
public class OpenAIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIService.class);

    private static volatile OpenAIService single_instance = null;
    private static OllamaDramaSettings settings = new OllamaDramaSettings();

    /**
     * Private constructor for singleton pattern.
     * 
     * @param settings_param the settings to use, or null for defaults
     */
    private OpenAIService(OllamaDramaSettings settings_param) {
        settings = Objects.requireNonNullElseGet(settings_param, OllamaDramaSettings::new);
    }

    /**
     * Gets the singleton instance with custom settings.
     * 
     * @param settings_param custom settings, or null for defaults
     * @return the singleton instance
     */
    public static OpenAIService getInstance(OllamaDramaSettings settings_param) {
        if (single_instance == null) {
            synchronized (OpenAIService.class) {
                if (single_instance == null) {
                    single_instance = new OpenAIService(settings_param);
                    LOGGER.info("Created new OpenAIService instance");
                }
            }
        } else {
            LOGGER.debug("Returning existing OpenAIService instance");
        }
        
        return single_instance;
    }

    /**
     * Gets the singleton instance with default settings.
     * 
     * @return the singleton instance
     */
    public static OpenAIService getInstance() {
        return getInstance(null);
    }

    /**
     * Creates a new strict OpenAI session with specified model and settings.
     * 
     * @param model_name the OpenAI model to use (e.g., "gpt-4", "gpt-3.5-turbo")
     * @param settings_param the settings containing the OpenAI API key
     * @return a new OpenAISession configured for strict protocol
     * @throws IllegalStateException if settings are not properly initialized
     * @throws IllegalArgumentException if model_name is null or empty
     */
    public static OpenAISession get_strict_session(String model_name, OllamaDramaSettings settings_param) {
        validate_settings(settings_param);
        validate_model_name(model_name);
        
        String api_key = settings_param.getOpenaikey();
        validate_api_key(api_key);

        var service = new OpenAiService(api_key);
        
        LOGGER.info("Creating strict OpenAI session with model: {}", model_name);
        return new OpenAISession(model_name, service, settings_param);
    }

    /**
     * Creates a new strict OpenAI session using the singleton's settings.
     * 
     * @param model_name the OpenAI model to use
     * @return a new OpenAISession configured for strict protocol
     */
    public static OpenAISession get_strict_session(String model_name) {
        return get_strict_session(model_name, settings);
    }

    /**
     * Validates that settings are properly initialized.
     * 
     * @param settings_param the settings to validate
     * @throws IllegalStateException if settings are null
     */
    private static void validate_settings(OllamaDramaSettings settings_param) {
        if (settings_param == null) {
            LOGGER.error("OllamaDrama settings not properly initialized");
            SystemUtils.halt();
            throw new IllegalStateException("Settings cannot be null");
        }
    }

    /**
     * Validates that the model name is not null or empty.
     * 
     * @param model_name the model name to validate
     * @throws IllegalArgumentException if model_name is invalid
     */
    private static void validate_model_name(String model_name) {
        if (model_name == null || model_name.trim().isEmpty()) {
            LOGGER.error("Invalid model name: {}", model_name);
            SystemUtils.halt();
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
    }

    /**
     * Validates that the API key is not null or empty.
     * 
     * @param api_key the API key to validate
     * @throws IllegalArgumentException if api_key is invalid
     */
    private static void validate_api_key(String api_key) {
        if (api_key == null || api_key.trim().isEmpty()) {
            LOGGER.error("OpenAI API key is not configured");
            SystemUtils.halt();
            throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
        }
    }

    /**
     * Gets the current settings.
     * 
     * @return the current OllamaDramaSettings
     */
    public static OllamaDramaSettings get_settings() {
        return settings;
    }

    /**
     * Sets the settings (for testing purposes).
     * 
     * @param new_settings the new settings to use
     */
    static void set_settings(OllamaDramaSettings new_settings) {
        settings = Objects.requireNonNull(new_settings, "Settings cannot be null");
    }

    // ========== BACKWARD COMPATIBILITY WRAPPERS (DEPRECATED) ==========

    /**
     * @deprecated Use {@link #get_strict_session(String, OllamaDramaSettings)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public static OpenAISession getStrictSession(String model_name, OllamaDramaSettings settings_param) {
        return get_strict_session(model_name, settings_param);
    }

    /**
     * @deprecated Use {@link #get_strict_session(String)} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public static OpenAISession getStrictSession(String model_name) {
        return get_strict_session(model_name);
    }

    /**
     * @deprecated Use {@link #get_settings()} instead.
     */
    @Deprecated(since = "1.0", forRemoval = false)
    public static OllamaDramaSettings getSettings() {
        return get_settings();
    }
}