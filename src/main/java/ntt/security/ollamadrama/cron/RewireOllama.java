package ntt.security.ollamadrama.cron;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.singletons.OllamaService;


public class RewireOllama {

	private static final Logger LOGGER = LoggerFactory.getLogger(RewireOllama.class);

	private Timer timer;

	public RewireOllama(final int initalDelayInSeconds, final int checkIntervalInSeconds) {
		this.timer = new Timer();
		this.timer.schedule(new RewireTask(), initalDelayInSeconds*1000, checkIntervalInSeconds * 1000);
	}

	class RewireTask extends TimerTask {

		@Override
		public void run() {
			LOGGER.info("RewireTask()");
			
			// Before update, make sure we can find at least one ollama server
			boolean found_ollamas = OllamaService.wireOllama(false);
			LOGGER.debug("found_ollamas: " + found_ollamas);
		}

	}
}