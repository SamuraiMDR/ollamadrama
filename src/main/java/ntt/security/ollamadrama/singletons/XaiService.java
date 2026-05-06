package ntt.security.ollamadrama.singletons;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;
import ntt.security.ollamadrama.utils.SystemUtils;

/**
 * Singleton service for managing xAI (Grok) API connections and sessions.
 *
 * xAI exposes an OpenAI-compatible Chat Completions API at https://api.x.ai/v1, so we
 * reuse the existing {@link OpenAISession} verbatim — only the underlying
 * {@link OpenAIClient} is built against a different base URL and authenticated with the
 * xAI key from {@link OllamaDramaSettings#getXaikey()}.
 *
 * Use this service when {@code use_xai = true} in config; instantiate sessions with
 * Grok model identifiers such as {@code grok-2-latest}, {@code grok-3}, {@code grok-4}.
 */
public class XaiService {

    public static final String XAI_BASE_URL = "https://api.x.ai/v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(XaiService.class);

    private static volatile XaiService single_instance = null;
    private static OllamaDramaSettings settings = new OllamaDramaSettings();

    private XaiService(OllamaDramaSettings settings_param) {
        settings = Objects.requireNonNullElseGet(settings_param, OllamaDramaSettings::new);
    }

    public static XaiService getInstance(OllamaDramaSettings settings_param) {
        if (single_instance == null) {
            synchronized (XaiService.class) {
                if (single_instance == null) {
                    single_instance = new XaiService(settings_param);
                    LOGGER.info("Created new XaiService instance");
                }
            }
        }
        return single_instance;
    }

    public static XaiService getInstance() {
        return getInstance(null);
    }

    public static OpenAISession get_strict_session(String model_name, OllamaDramaSettings settings_param) {
        validate_settings(settings_param);
        validate_model_name(model_name);

        String api_key = settings_param.getXaikey();
        validate_api_key(api_key);

        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(api_key)
                .baseUrl(XAI_BASE_URL)
                .build();

        LOGGER.info("Creating strict xAI session with Grok model: {}", model_name);
        return new OpenAISession(model_name, client, settings_param);
    }

    public static OpenAISession get_strict_session(String model_name) {
        return get_strict_session(model_name, settings);
    }

    public static OpenAISession getStrictSession(String model_name, OllamaDramaSettings settings_param) {
        return get_strict_session(model_name, settings_param);
    }

    public static OpenAISession getStrictSession(String model_name) {
        return get_strict_session(model_name);
    }

    public static OllamaDramaSettings get_settings() { return settings; }
    public static OllamaDramaSettings getSettings() { return settings; }

    private static void validate_settings(OllamaDramaSettings settings_param) {
        if (settings_param == null) {
            LOGGER.error("OllamaDrama settings not properly initialized");
            SystemUtils.halt();
            throw new IllegalStateException("Settings cannot be null");
        }
    }

    private static void validate_model_name(String model_name) {
        if (model_name == null || model_name.trim().isEmpty()) {
            LOGGER.error("Invalid model name: {}", model_name);
            SystemUtils.halt();
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
    }

    private static void validate_api_key(String api_key) {
        if (api_key == null || api_key.trim().isEmpty()) {
            LOGGER.error("xAI API key is not configured (set xaikey in OLLAMADRAMACONFIG)");
            SystemUtils.halt();
            throw new IllegalArgumentException("xAI API key cannot be null or empty");
        }
    }

    static void set_settings(OllamaDramaSettings new_settings) {
        settings = Objects.requireNonNull(new_settings, "Settings cannot be null");
    }
}
