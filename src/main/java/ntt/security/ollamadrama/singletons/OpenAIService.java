package ntt.security.ollamadrama.singletons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.service.OpenAiService;

import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;
import ntt.security.ollamadrama.utils.*;

public class OpenAIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIService.class);

    private static volatile OpenAIService single_instance = null;
    private static OllamaDramaSettings settings = new OllamaDramaSettings();

    private OpenAIService(OllamaDramaSettings _settings) {
        super();

        if (null == _settings) _settings = new OllamaDramaSettings();
        settings = _settings;
    }

    public static OpenAIService getInstance(OllamaDramaSettings _settings) {
        if (single_instance == null) { 
            synchronized (OpenAIService.class) {
                if (single_instance == null) { 
                    single_instance = new OpenAIService(_settings);
                }
            }
        }
        return single_instance;
    }

    public static OpenAIService getInstance() {
        if (single_instance == null) { 
            synchronized (OpenAIService.class) {
                if (single_instance == null) { 
                    single_instance = new OpenAIService(new OllamaDramaSettings());
                }
            }
        }
        return single_instance;
    }

    public static OpenAISession getStrictSession(String _model_name, OllamaDramaSettings _settings) {
        if (settings == null) {
            LOGGER.error("Is OllamaDrama initialized properly");
            SystemUtils.halt();
        }
        OpenAiService service = new OpenAiService(settings.getOpenaikey());
        return new OpenAISession(_model_name, service, _settings);
    }
}
