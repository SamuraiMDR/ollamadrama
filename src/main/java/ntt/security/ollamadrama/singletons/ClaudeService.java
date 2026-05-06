package ntt.security.ollamadrama.singletons;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.sessions.ClaudeSession;
import ntt.security.ollamadrama.utils.SystemUtils;

/**
 * Singleton service for managing Claude API connections and sessions.
 * This service handles Claude session creation with configured settings.
 */
public class ClaudeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeService.class);

    private static volatile ClaudeService single_instance = null;
    private static OllamaDramaSettings settings = new OllamaDramaSettings();

    /**
     * Private constructor for singleton pattern.
     *
     * @param settings_param the settings to use, or null for defaults
     */
    private ClaudeService(OllamaDramaSettings settings_param) {
        settings = Objects.requireNonNullElseGet(settings_param, OllamaDramaSettings::new);
    }

    /**
     * Gets the singleton instance with custom settings.
     *
     * @param settings_param custom settings, or null for defaults
     * @return the singleton instance
     */
    public static ClaudeService getInstance(OllamaDramaSettings settings_param) {
        if (single_instance == null) {
            synchronized (ClaudeService.class) {
                if (single_instance == null) {
                    single_instance = new ClaudeService(settings_param);
                    LOGGER.info("Created new ClaudeService instance");
                }
            }
        } else {
            LOGGER.debug("Returning existing ClaudeService instance");
        }

        return single_instance;
    }

    /**
     * Gets the singleton instance with default settings.
     *
     * @return the singleton instance
     */
    public static ClaudeService getInstance() {
        return getInstance(null);
    }

    /**
     * Creates a new strict Claude session with specified model and settings.
     *
     * @param model_name     the Claude model to use (e.g., "claude-opus-4-6", "claude-sonnet-4-6")
     * @param settings_param the settings containing the Claude API key
     * @return a new ClaudeSession configured for strict protocol
     * @throws IllegalStateException    if settings are not properly initialized
     * @throws IllegalArgumentException if model_name is null or empty
     */
    public static ClaudeSession get_strict_session(String model_name, OllamaDramaSettings settings_param) {
        validate_settings(settings_param);
        validate_model_name(model_name);

        String api_key = settings_param.getClaudekey();
        validate_api_key(api_key);

        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(api_key)
                .build();

        LOGGER.info("Creating strict Claude session with model: {}", model_name);
        return new ClaudeSession(model_name, client, settings_param);
    }

    /**
     * Creates a new strict Claude session using the singleton's settings.
     *
     * @param model_name the Claude model to use
     * @return a new ClaudeSession configured for strict protocol
     */
    public static ClaudeSession get_strict_session(String model_name) {
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
            LOGGER.error("Claude API key is not configured");
            SystemUtils.halt();
            throw new IllegalArgumentException("Claude API key cannot be null or empty");
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

    public static ClaudeSession getStrictSession(String model_name, OllamaDramaSettings settings_param) {
        return get_strict_session(model_name, settings_param);
    }

    public static ClaudeSession getStrictSession(String model_name) {
        return get_strict_session(model_name);
    }

    public static OllamaDramaSettings getSettings() {
        return get_settings();
    }

}
