package ntt.security.ollamadrama.singletons;

import java.util.ArrayList;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.utils.*;

public class OllamaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaService.class);

	private static volatile OllamaService single_instance = null;
	private static OllamaDramaSettings settings = new OllamaDramaSettings();

	private static final int threadPoolCount = 20;

	private static ArrayList<String> service_cnets;
	private static TreeMap<String, OllamaEndpoint> ollama_endpoints = new TreeMap<>();

	private OllamaService(OllamaDramaSettings _settings) {
		super();
		
		if (null == _settings) {
			// fallback to env variables
			_settings = ConfigUtils.parseConfigENV();
		}
		settings = _settings;

		// Block and find at least one ollama endpoint
		rescan(true);
	}

	public synchronized void rescan(boolean blockUntilReady) {
		ArrayList<String> serviceips = NetUtilsLocal.determineLocalIPv4s();
		if (serviceips.isEmpty()) {
			LOGGER.error("Unable to find any connected networks using NetUtils.determineLocalIPv4s()");
			SystemUtils.halt();
		}
		LOGGER.info("Owned ips: " + serviceips);

		ArrayList<String> service_cnets_temp = new ArrayList<>();
		for (String ip : serviceips) {
			String cnet = NetUtilsLocal.grabCnetworkSlice(ip);
			service_cnets_temp.add(cnet);
		}
		synchronized (OllamaService.class) {
			service_cnets = service_cnets_temp;
		}

		// before launch, make sure we can find at least one ollama server
		boolean found_ollamas = wireOllama(true);
		LOGGER.info("found_ollamas: " + found_ollamas);
	}

	public static boolean wireOllama(boolean blockUntilReady) {
		boolean found_ollamas = false;
		int ollama_attempt_counter = 0;
		boolean ollama_abort = false;
		while (!found_ollamas && !ollama_abort) {

			TreeMap<String, OllamaEndpoint> ollamas = new TreeMap<String, OllamaEndpoint>();

			if (settings.isOllama_scan()) {
				if (blockUntilReady) LOGGER.info("Looking for ollama servers .. " + service_cnets);
				ollamas = NetUtilsLocal.performTCPPortSweep(settings.getOllama_port(), service_cnets, 1, 255, settings.getOllama_timeout(), threadPoolCount, settings.getOllama_username(), settings.getOllama_password());
			} 

			if (ollamas.isEmpty() && (null == settings.getSatellites())) {
				LOGGER.warn("Unable to find any hosts listening on ollama port " + settings.getOllama_port() + ", sweeped the networks " + service_cnets + ", will keep trying");
				SystemUtils.sleepInSeconds(5);
			} else {
				if (blockUntilReady) LOGGER.info("activeHosts for ollama port " + settings.getOllama_port() + ": " + ollamas.keySet());
				TreeMap<String, OllamaEndpoint> verified_ollamas = new TreeMap<>();
				TreeMap<String, OllamaEndpoint> abandoned_ollamas = new TreeMap<>();

				if (null != settings.getSatellites() && !settings.getSatellites().isEmpty()) {
					LOGGER.info("We have defined satellite ollama endpoints, first entry is " + settings.getSatellites().get(0).getOllama_url() + "..");
					for (OllamaEndpoint oep: settings.getSatellites()) {
						if (null != abandoned_ollamas.get(oep.getOllama_url())) {
							LOGGER.info("We have defined satellite ollama endpoints, but its been abandoned .. " + oep.getOllama_url());
						} else {
							if (null == ollamas.get(oep.getOllama_url())) {
								LOGGER.info("Adding ollama endpoint " + oep.getOllama_url());
								ollamas.put(oep.getOllama_url(), oep);
							} else {
								LOGGER.info(oep.getOllama_url() + " is already in our list of candidates");
							}
						}
					}
				}

				for (String ollama_url: ollamas.keySet())  {
					OllamaEndpoint oep = ollamas.get(ollama_url);
					LOGGER.info("Connecting to to ollama URL " + oep.getOllama_url() + "..");

					OllamaAPI ollamaAPI = OllamaUtils.createConnection(oep, settings.getOllama_timeout());

					try {
						if (ollamaAPI.ping()) {
							LOGGER.info("The ollama URL " + oep.getOllama_url() + " replied with a proper ping");
							LOGGER.info("Models available on " + oep.getOllama_url() + ": " + OllamaUtils.getModelsAvailable(ollamaAPI).toString());
							LOGGER.info("Making sure we have the models we need on " + oep.getOllama_url() + " ..");
							boolean model_exists = false;

							for (String modelname: settings.getOllama_models().split(",")) {
								if (modelname.length()>3) {
									if (null == abandoned_ollamas.get(oep.getOllama_url())) {
										if (!OllamaUtils.verifyModelAvailable(ollamaAPI, modelname)) {
											if (null != Globals.MODEL_SKIP_AUTOPULL.get(modelname)) {
												LOGGER.warn("The model " + modelname + " is marked as L so wont be pulled automatically. Pull manually to qualify this Ollama endpoint.");
												LOGGER.warn("Skipping the ollama endpoint " + ollama_url);
												abandoned_ollamas.put(oep.getOllama_url(), oep);
											} else {
												LOGGER.warn("Unable to find required model " + modelname + " on " + oep.getOllama_url() + ", will try to pull. This may take some time ..");
												boolean success_pull = OllamaUtils.pullModel(ollamaAPI, modelname);
												if (success_pull) {
													if (OllamaUtils.verifyModelAvailable(ollamaAPI, modelname)) model_exists = true;
												}
											}
										} else {
											model_exists = true;
										}
										if (model_exists) {
											LOGGER.info("Performing simple sanity check on Ollama model " + modelname + " on " + oep.getOllama_url());
											if (!OllamaUtils.verifyModelSanityUsingSingleWordResponse(oep.getOllama_url(), ollamaAPI, modelname, Globals.createStrictOptionsBuilder(), "Is the capital city of France named Paris? You must reply with only a single word of Yes or No." + Globals.THREAT_TEMPLATE, "Yes", 1)) {
												LOGGER.warn("Unable to pass simple sanity check for " + modelname + " on " + oep.getOllama_url() + ". Abandoning the node for now.");	
												abandoned_ollamas.put(oep.getOllama_url(), oep);
												verified_ollamas.remove(oep.getOllama_url(), oep);
											} else {
												if (null == abandoned_ollamas.get(oep.getOllama_url())) {
													LOGGER.info("Verified ollama URL (with a functional instance of our preferred model " + modelname + ") found on: " + oep.getOllama_url());
													verified_ollamas.put(oep.getOllama_url(), oep);
												} else {
													LOGGER.info("ollama host (with a functional instance of our preferred model " + modelname + ") found on: " + oep.getOllama_url() + " but its ABANDONED");
												}
											}
										}
									} else {
										LOGGER.info("Skipping model check for " + modelname + " since Ollama node has been abandoned.");
									}
								}
							}
						}
					} catch (Exception e) {
						LOGGER.warn("Caught exception while attempting ping() against " + oep.getOllama_url() + ", exception: " + e.getMessage());
					}
				}

				if (verified_ollamas.isEmpty()) {
					LOGGER.warn("Found hosts listening on ollama port " + settings.getOllama_port() + ", but none of them seem to be running well behaving versions of our required models");
					SystemUtils.sleepInSeconds(5);
				} else {
					if (blockUntilReady) LOGGER.info("Verified ollama service endpoints (all models): " + verified_ollamas.keySet());
					ollama_endpoints = verified_ollamas;
					found_ollamas = true;
				}
			}

			ollama_attempt_counter++;

			if (!blockUntilReady && (ollama_attempt_counter>10)) {
				LOGGER.warn("Was attempting to update the list of ollama servers but found none");
				ollama_abort = true;
			}
		}

		return found_ollamas;
	}

	public static OllamaService getInstance(OllamaDramaSettings _settings) {
		if (single_instance == null) { // First check (no locking)
			synchronized (OllamaService.class) {
				if (single_instance == null) { // Second check (with locking)
					_settings.setOllama_timeout(300); // increased timeout for large context models
					single_instance = new OllamaService(_settings);
				}
			}
		}
		return single_instance;
	}

	public static OllamaService getInstance(String _model_names, OllamaDramaSettings _settings) {
		_settings.setOllama_models(_model_names);
		return getInstance(_settings);
	}

	public static OllamaService getInstance(String _model_names) {
		OllamaDramaSettings settings = new OllamaDramaSettings();
		settings.setOllama_models(_model_names);
		return getInstance(settings);
	}

	public static OllamaService getInstance() {
		return getInstance((OllamaDramaSettings) null);
	}

	public static OllamaDramaSettings getSettings() {
		return settings;
	}

	public static ArrayList<String> getService_cnets() {
		return service_cnets;
	}

	public static TreeMap<String, OllamaEndpoint> getollama_hosts() {
		return ollama_endpoints;
	}

	public static OllamaEndpoint getRandomActiveOllamaURL() {
		while (true) {
			int size = ollama_endpoints.size();
			while (ollama_endpoints.size() == 0) {
				LOGGER.warn("No Ollama hosts available ... blocking and rescanning in 5 seconds");
				SystemUtils.sleepInSeconds(5);
			}
			int selection = NumUtils.randomNumWithinRangeAsInt(1, size);
			int index = 1;
			for (String ollama_url: ollama_endpoints.keySet()) {
				OllamaEndpoint oep = ollama_endpoints.get(ollama_url);
				if (selection == index) return oep;
				index++;
			}

			LOGGER.warn("No Ollama hosts available ... blocking and rescanning in 5 seconds");
			SystemUtils.sleepInSeconds(5);
		}
	}

	public static OllamaSession getStrictProtocolSession(String _model_name, boolean hide_llm_reply_if_uncertain) {
		boolean model_exists = false;
		if (settings == null) {
			LOGGER.error("Is OllamaDrama initialized properly");
			SystemUtils.halt();
		}
		if (_model_name.length() <= 3) {
			LOGGER.error("Specified modename is invalid: " + _model_name);
			SystemUtils.halt();
		}
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings (" + _model_name + "), halting. Models listed in settings: " + settings.getOllama_models());
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createStrictOptionsBuilder(), OllamaService.getSettings(), 
				Globals.PROMPT_TEMPLATE_STRICT_SIMPLEOUTPUT + 
				Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS + 
				Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + 
				Globals.THREAT_TEMPLATE);
	}

	public static OllamaSession getStrictSession(String _model_name) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model: " + _model_name + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createStrictOptionsBuilder(), OllamaService.getSettings(), Globals.PROMPT_TEMPLATE_STRICT_COMPLEXOUTPUT);
	}

	public static OllamaSession getCreativeSession(String _model_name) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model: " + _model_name + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createCreativeOptionsBuilder(), OllamaService.getSettings(), Globals.PROMPT_TEMPLATE_CREATIVE);
	}

	public static OllamaSession getDefaultSession(String _model_name) {
		boolean model_exists = false;
		for (String existing_model: settings.getOllama_models().split(",")) {
			if (existing_model.equals(_model_name)) model_exists = true;
		}
		if (!model_exists) {
			LOGGER.error("Attempt to create session with model not listed in settings, halting (model: " + _model_name + ")");
			SystemUtils.halt();
		}
		return new OllamaSession(_model_name, OllamaService.getRandomActiveOllamaURL(),
				Globals.createDefaultOptionsBuilder(), OllamaService.getSettings(), "");
	}

}
