package ntt.security.ollamadrama.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
import io.github.amithkoujalgi.ollama4j.core.models.Model;
import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessage;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatMessageRole;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestBuilder;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatRequestModel;
import io.github.amithkoujalgi.ollama4j.core.models.chat.OllamaChatResult;
import io.github.amithkoujalgi.ollama4j.core.utils.Options;
import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ChatInteraction;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.OllamaEnsemble;
import ntt.security.ollamadrama.objects.OllamaWrappedSession;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.response.StatementResponse;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.singletons.OllamaService;

public class OllamaUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaUtils.class);

	public static OllamaAPI createConnection(OllamaEndpoint _oep, long _timeout) {
		OllamaAPI ollamaAPI = null;
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				ollamaAPI = new OllamaAPI(_oep.getOllama_url());
				ollamaAPI.setRequestTimeoutSeconds(_timeout);
				if ((_oep.getOllama_username().length()>0) && (_oep.getOllama_password().length()>0)) ollamaAPI.setBasicAuth(_oep.getOllama_username(), _oep.getOllama_password());
				return ollamaAPI;
			} catch (Exception e) {
				if (e.getMessage().contains("Unrecognized field")) {
					LOGGER.warn("Got unrecognized field when interpreting JSON response, are you running latest version of ollama4j?");
					SystemUtils.sleepInSeconds(10);
					retryCounter++;
				} else {
					LOGGER.warn("Exception #1: " + e.getMessage());
					LOGGER.warn("Catch all error handler while connecting to " + _oep.getOllama_url());
					SystemUtils.sleepInSeconds(10);
					retryCounter++;
				}
			}
		}
		return ollamaAPI;
	}

	public static ArrayList<String> getModelsAvailable(OllamaAPI _ollamaAPI) {
		ArrayList<String> models = new ArrayList<String>();
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				for (Model m : _ollamaAPI.listModels()) {
					models.add(m.getName());
				}
				return models;
			} catch (Exception e) {
				LOGGER.warn("Exception: " + e.getMessage());
				LOGGER.warn("Catch all error handler while retrieving available models");
				SystemUtils.sleepInSeconds(10);
				retryCounter++;
			}
		}
		return models;
	}

	public static Boolean pullModel(OllamaAPI _ollamaAPI, String _modelname) {
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				long pull_ts1 = System.currentTimeMillis();
				_ollamaAPI.pullModel(_modelname);
				long pull_ts2 = System.currentTimeMillis();
				long pull_req_time = pull_ts2 - pull_ts1;
				LOGGER.info("We just attempted a pull of " + _modelname + " (request_time: " + pull_req_time + " ms)");
				if (pull_req_time < 1000L) {
					LOGGER.warn("Pull of model returned quickly and seems to have failed? Perhaps a new version of Ollama is needed?");
					SystemUtils.sleepInSeconds(5);
				}
				return true;
			} catch (Exception e) {
				if ((null != e) && "closed".equals(e.getMessage())) {
					LOGGER.warn("Got disconnected while performing a pull on " + _modelname + ", this happens so just wait for the reconnect");
					SystemUtils.sleepInSeconds(10);
				} else {
					LOGGER.warn("Exception: " + e.getMessage());
					LOGGER.warn("Catch all error handler while performing a pull on " + _modelname);
					SystemUtils.sleepInSeconds(10);
				}
				retryCounter++;
				LOGGER.info("pullModel retryCounter: " + retryCounter);
				SystemUtils.sleepInSeconds(3);
			}
		}
		return false;
	}

	public static boolean verifyModelAvailable(OllamaAPI _ollamaAPI, String _modelname) {
		boolean req_model_available = false;
		int retryCounter = 0;
		while (retryCounter < 3) {
			try {
				for (Model m : _ollamaAPI.listModels()) {
					if (_modelname.equals(m.getName())) {
						req_model_available = true;
					}
				}
				return req_model_available;
			} catch (Exception e) {
				LOGGER.warn("verifyModelAvailable() Exception: " + e.getMessage());
				LOGGER.warn("Catch all error handler while verifying existance of " + _modelname);
				retryCounter++;
			}
		}
		return req_model_available;
	}

	public static boolean verifyModelSanityUsingSingleWordResponse(String _ollamaurl, OllamaAPI _ollamaAPI, String _modelname, Options _options, String _question, String _answer, int _maxRetries, String autopull_max_llm_size) {
		boolean debug = true;
		boolean sanity_pass = false;
		int retryCounter = 0;
		while (retryCounter <= _maxRetries) {
			try {
				String raw_result = "";
				OllamaResult result = _ollamaAPI.generate(_modelname, _question, _options);
				if (null != result) {
					if (null != result.getResponse()) {
						if (!result.getResponse().isEmpty()) {
							raw_result = result.getResponse();
							raw_result = preprocessLLMresponse(raw_result);

							// tolerate chatter
							String res = raw_result.trim().split("\n")[0].split(",")[0].replace(".", "");
							if (_answer.equals(res)) {
								sanity_pass = true;
							} else {
								if (debug) LOGGER.info(_answer + " is not " + res);
							}
						}
					}
				}
				if (!sanity_pass) {
					LOGGER.warn("Unable to pass simple sanity check for " + _modelname);
					LOGGER.warn("raw_result: " + raw_result);
				}
				return sanity_pass;
			} catch (Exception e) {
				if (false ||
						e.getMessage().contains("null") || // 500 from ollama endpoint
						e.getMessage().contains("try pulling it first") ||
						e.getMessage().contains("not found") ||
						false) {
					if (OllamaUtils.is_skip_model_autopull(autopull_max_llm_size, _modelname)) {
						LOGGER.info("Ollamadrama is configured to skip autopull of the model " + _modelname + " (autopull_max_llm_size: " + autopull_max_llm_size + "), try to pull it manually once you have ensured your endpoint has enough VRAM. Or change the ollamadrama 'autopull_max_llm_size' configuration. ");
						return false;
					} else {
						LOGGER.info("Trying to autoheal and pull model " + _modelname);
						pullModel(_ollamaAPI, _modelname);
					}
				} else {
					LOGGER.warn("verifyModelSanityUsingSingleWordResponse() Exception #1: '" + e.getMessage() + "' while interacting with " + _ollamaurl);
					LOGGER.warn("Catch all error handler while verifying sanity of " + _modelname + 	", retryCounter=" + retryCounter);
				}
				SystemUtils.sleepInSeconds(10);
				retryCounter++;
			}
		}
		return sanity_pass;
	}

	private static String preprocessLLMresponse(String raw_result) {
		boolean debug = false;
		if (debug) System.out.println("raw_result: " + raw_result);

		// deepseek style
		if (raw_result.startsWith("<think>") && raw_result.contains("</think>")) {
			raw_result = raw_result.split("</think>")[1];
			while (raw_result.startsWith("\n")) {
				raw_result = raw_result.replaceFirst("\n", "");
			}
		}

		// exaone-deep being different
		if (raw_result.startsWith("<thought>") && raw_result.contains("</thought>")) {
			raw_result = raw_result.split("</thought>")[1];
			while (raw_result.startsWith("\n")) {
				raw_result = raw_result.replaceFirst("\n", "");
			}
		}

		// --- show the characters one by one ----------------------------------
		if (debug) {
			System.out.println("-------- character dump --------");
			for (int i = 0; i < raw_result.length(); i++) {
				int cp = raw_result.codePointAt(i);
				// If you might have supplementary characters, move the index correctly
				if (Character.isSupplementaryCodePoint(cp)) i++;

				System.out.printf(
						"%2d : U+%04X  (%s)%n",
						i,
						cp,
						switch (cp) {
						case '\n' -> "\\n";
						case '\r' -> "\\r";
						case 0xFEFF -> "BOM";
						default -> Character.isWhitespace(cp) ? "WS" : "'" + new String(Character.toChars(cp)) + "'";
						}
						);
			}
			System.out.println("length = " + raw_result.length());
		}

		return raw_result;
	}

	public static OllamaChatResult setChatSystemProfile(OllamaAPI _ollamaAPI, String _modelname, Options _options, String _statement, String _autopull_max_llm_size, long _timeout) {
		if (!_statement.endsWith(".")) _statement = _statement + "."; // quick patch
		OllamaChatResult chatResult = null;
		int retryCounter = 0;
		while (retryCounter < 5) {
			try {				
				OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(_modelname).withOptions(_options);
				OllamaChatRequestModel requestModel = builder.withMessage(OllamaChatMessageRole.SYSTEM,
						_statement).build();
				_ollamaAPI.setRequestTimeoutSeconds(_timeout);
				chatResult = _ollamaAPI.chat(requestModel);
				return chatResult;
			} catch (Exception e) {
				if (e.getMessage() != null) {
					LOGGER.warn("setChatSystemProfile() Exception: " + e.getMessage());
					LOGGER.warn("Catch all error handler while setting system profile for " + _modelname + ", retryCounter=" + retryCounter);
					if (e.getMessage().contains("try pulling it first")) {
						if (OllamaUtils.is_skip_model_autopull(_autopull_max_llm_size, _modelname)) {
							LOGGER.info("Ollamadrama is configured to skip autopull of the model " + _modelname + ", try to pull it manually once you have ensured your endpoint has enough VRAM. Or change the ollamadrama 'autopull_max_llm_size' configuration. ");
							return null;
						} else {
							LOGGER.info("Trying to autoheal and pull model " + _modelname);
							pullModel(_ollamaAPI, _modelname);
						}
					}
				}
				retryCounter++;
			}
		}
		return null;
	}

	public static String askGenericSingleWordQuestion(OllamaAPI _ollamaAPI, String _modelname, Options _options, String _question) {
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				while (retryCounter < 10) {
					OllamaResult result = _ollamaAPI.generate(_modelname, _question + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + Globals.THREAT_TEMPLATE, _options);
					if (null != result) {
						if (null != result.getResponse()) {
							if (!result.getResponse().isEmpty()) {
								//System.out.println("response: " + result.getResponse());
								if (result.getResponse().startsWith("{")) {
									return result.getResponse();
								}
							}
						}
					}
					SystemUtils.sleepInSeconds(1); // throttle
					retryCounter++;
				}
			} catch (Exception e) {
				LOGGER.warn("askGenericSingleWordQuestion() Exception: " + e.getMessage());
				LOGGER.warn("Catch all error handler while attempting to interact with " + _modelname);
				SystemUtils.sleepInSeconds(10);
				retryCounter++;
			}
		}
		return "";
	}

	public static ChatInteraction askRawChatQuestionWithCustomChatHistory(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _question, List<OllamaChatMessage> _customChatHistory, long _timeout) {
		if (!_question.endsWith(".")) _question = _question + "."; // quick patch
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				int counter = 0;
				while (true) {
					if (null != _chatResult) {
						OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(_modelname).withOptions(_options);
						OllamaChatRequestModel requestModel = null;
						if (null == _customChatHistory) {
							requestModel = builder.withMessages(_chatResult.getChatHistory()).withMessage(OllamaChatMessageRole.USER, _question).build();
						} else {
							requestModel = builder.withMessages(_customChatHistory).withMessage(OllamaChatMessageRole.USER, _question).build();
						}
						_ollamaAPI.setRequestTimeoutSeconds(_timeout);
						_chatResult = _ollamaAPI.chat(requestModel);
						if (null !=_chatResult) {						
							return new ChatInteraction(_chatResult, _chatResult.getResponse(), true);
						}
						counter++;
						if (counter > 5) System.out.println("DEBUG: struggling to get a proper reply with " + _modelname + " .. counter: " + counter + " .. \n<init_response>\n"  + "\n<fin_response>");
						if (counter > 10) return new ChatInteraction(_chatResult, "N/A", false);
						SystemUtils.sleepInSeconds(1); // throttle
					} else {
						LOGGER.error("You should never call askRawChatQuestion() with a null _chatResult");
						SystemUtils.halt();
					}
				}
			} catch (Exception e) {
				LOGGER.warn("askRawChatQuestion() Exception: " + e.getMessage() + " (timeout: " + _timeout + ")");
				LOGGER.warn("Catch all error handler while making chat session request towards " + _modelname + ", retryCounter=" + retryCounter);
				SystemUtils.sleepInSeconds(5);
				retryCounter++;
			}
		}
		return new ChatInteraction(_chatResult, "", false);
	}

	public static ChatInteraction askRawChatQuestion(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _question, long _timeout) {
		return askRawChatQuestionWithCustomChatHistory(_ollamaAPI, _modelname, _options, _chatResult, _question, null, _timeout);
	}

	public static ChatInteraction askChatQuestion(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _question, Integer _retryThreshold, long _timeout) {
		if (!_question.endsWith(".")) _question = _question + ".";
		int retryCounter = 0;
		while (retryCounter <= _retryThreshold) {
			try {
				int counter = 0;
				String addon = "";
				while (true) {
					OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(_modelname).withOptions(_options);
					OllamaChatRequestModel requestModel = builder.withMessages(_chatResult.getChatHistory()).withMessage(OllamaChatMessageRole.USER,
							_question + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_QUESTIONS + Globals.LOGIC_TEMPLATE + addon).build();
					_ollamaAPI.setRequestTimeoutSeconds(_timeout);
					_chatResult = _ollamaAPI.chat(requestModel);

					// debug
					if (retryCounter > 1) {
						System.out.println("_chatResult.getResponse(): " + _chatResult.getResponse() + "\n");
					}

					if (null !=_chatResult) {						
						if (_chatResult.getResponse().contains("{") && _chatResult.getResponse().contains("}")) {
							return new ChatInteraction(_chatResult, _chatResult.getResponse(), true);
						} else {
							LOGGER.info("Trying to poke the LLM to get it to align with the JSON protocol");
							addon = ". Make sure the response is JSON formatted";
						}
					}
					counter++;
					if (counter > 5) System.out.println("DEBUG: struggling to get a proper JSON reply with " + _modelname + " .. counter: " + counter + " .. \n<init_response>\n" + _chatResult.getResponse() + "\n<fin_response>");
					if (counter > 10) return new ChatInteraction(_chatResult, "N/A", false);
					SystemUtils.sleepInSeconds(1); // throttle
				}
			} catch (Exception e) {
				LOGGER.warn("askChatQuestion() Exception: " + e.getMessage() + " (timeout: " + _timeout + ")");
				LOGGER.warn("retryCounter(): Catch all error handler while making chat session request towards " + _modelname + ", retryCounter=" + retryCounter);
				SystemUtils.sleepInSeconds(10);
				retryCounter++;
			}
		}
		return new ChatInteraction(_chatResult, "", false);
	}

	public static ChatInteraction askChatQuestion(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _question, long _timeout) {
		return askChatQuestion(_ollamaAPI, _modelname, _options, _chatResult, _question, 10, _timeout);
	}

	public static ChatInteraction addCreativeStatementToExistingChat(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _statement, long _timeout) {
		if (!_statement.endsWith(".")) _statement = _statement + "."; // quick patch
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				int counter = 0;
				String addon = "";
				while (true) {
					OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(_modelname).withOptions(_options);
					OllamaChatRequestModel requestModel = builder.withMessages(_chatResult.getChatHistory()).withMessage(OllamaChatMessageRole.USER,
							_statement + addon).build();
					_ollamaAPI.setRequestTimeoutSeconds(_timeout);
					_chatResult = _ollamaAPI.chat(requestModel);

					if (null !=_chatResult) {
						return new ChatInteraction(_chatResult, _chatResult.getResponse(), true);
					}
					counter++;
					if (counter > 10) {
						LOGGER.error("Giving up");
						return null;
					}
					SystemUtils.sleepInSeconds(1); // throttle
				}
			} catch (Exception e) {
				LOGGER.warn("addStatementToExistingChat() Exception: " + e.getMessage());
				LOGGER.warn("addStatementToExistingChat(): Catch all error handler while making chat session request towards " + _modelname + ", retryCounter=" + retryCounter);
				SystemUtils.sleepInSeconds(5);
				retryCounter++;
			}
		}
		return new ChatInteraction(_chatResult, "", false);
	}

	public static ChatInteraction addStrictStatementToExistingChat(OllamaAPI _ollamaAPI, String _modelname, Options _options, OllamaChatResult _chatResult, String _statement, long _timeout) {
		if (!_statement.endsWith(".")) _statement = _statement + "."; // quick patch
		int retryCounter = 0;
		while (retryCounter < 10) {
			try {
				int counter = 0;
				String addon = "";
				while (true) {
					OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(_modelname).withOptions(_options);
					OllamaChatRequestModel requestModel = builder.withMessages(_chatResult.getChatHistory()).withMessage(OllamaChatMessageRole.USER,
							_statement + Globals.ENFORCE_SINGLE_KEY_JSON_RESPONSE_TO_STATEMENTS + addon).build();
					_ollamaAPI.setRequestTimeoutSeconds(_timeout);
					_chatResult = _ollamaAPI.chat(requestModel);

					if (null !=_chatResult) {

						// debug
						if (retryCounter > 1) {
							System.out.println("_chatResult.getResponse(): " + _chatResult.getResponse() + "\n");
						}

						if (_chatResult.getResponse().contains("{") && _chatResult.getResponse().contains("}")) {

							StatementResponse resp = JSONUtils.createPOJOFromJSONOpportunistic(_chatResult.getResponse(), StatementResponse.class);
							if (null == resp) {
								LOGGER.warn("Not able to map the statement response to StatementResponse");
								LOGGER.warn("Response: " + _chatResult.getResponse());
								addon = ". Make sure the response is JSON formatted";
							} else {
								if (resp.getResponse().contains("OKIDOKI")) {
									return new ChatInteraction(_chatResult, _chatResult.getResponse(), true);
								} else {
									LOGGER.warn("Struggling to get the model " + _modelname + " to understand the STATEMENT " +  _statement);
									LOGGER.warn("Reason given for failure to understand: " + resp.getExplanation());
									System.out.println("STATEMENT _chatResult.getResponse(): \n" + _chatResult.getResponse() + "\n");
								}
							}
						} else {
							addon = ". Make sure the response is JSON formatted";
						}
					}
					counter++;
					if (counter > 5) {
						LOGGER.warn("Struggling to get a valid reply with " + _modelname + " .. result: " + " .. \n<init_response>\n" + _chatResult.getResponse() + "\n<fin_response>");
						addon = ". Make sure you reply with OKIDOKI if you understand";
					}
					if (counter > 10) {
						LOGGER.error("Giving up");
						return null;
					}
					SystemUtils.sleepInSeconds(1); // throttle
				}
			} catch (Exception e) {
				LOGGER.warn("addStatementToExistingChat() Exception: " + e.getMessage());
				LOGGER.warn("addStatementToExistingChat(): Catch all error handler while making chat session request towards " + _modelname + ", retryCounter=" + retryCounter);
				SystemUtils.sleepInSeconds(5);
				retryCounter++;
			}
		}
		return new ChatInteraction(_chatResult, "", false);
	}

	public static Object cleanupSTRING(String _str) {
		return _str.replace(".", "");
	}

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		return strictEnsembleRun(_query, Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M, _settings, _hide_llm_reply_if_uncertain, _use_random_seed);
	}

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, String _models, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {

		// Launch singleton 
		OllamaService.getInstance(_models, _settings);

		// Populate an ensemble of agents
		OllamaEnsemble e1 = new OllamaEnsemble();
		for (String model_name: _models.split(",")) {

			// Launch strict agent per included model type
			OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, _hide_llm_reply_if_uncertain, _use_random_seed);
			LOGGER.info("Using " + a1.getEndpoint().getOllama_url() + " with model " + model_name);
			e1.addWrappedSession(new OllamaWrappedSession(a1, Globals.MODEL_PROBABILITY_THRESHOLDS.get(model_name)));
		}
		SingleStringEnsembleResponse ssr1 = e1.askChatQuestion(_query, _hide_llm_reply_if_uncertain, _settings.getOllama_timeout());
		return ssr1;
	}

	public static String creativeEnsembleRunEarlyExitOnFirst(String _query, String _models, OllamaDramaSettings _settings) {

		// Launch singleton 
		OllamaService.getInstance(_models, _settings);

		// Populate an ensemble of agents
		for (String model_name: _models.split(",")) {

			// Launch strict agent per included model type
			OllamaSession a1 = OllamaService.getCreativeSession(model_name);
			LOGGER.info("Using " + a1.getEndpoint().getOllama_url() + " with model " + model_name);
			String reply = a1.askRawChatQuestion(_query, _settings.getOllama_timeout());
			return reply;
		}
		return "";
	}

	public static SingleStringQuestionResponse strictEnsembleRunEarlyExitOnFirstConfident(String _query, String _models, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {

		System.out.println("timeout x: " + _settings.getOllama_timeout());

		
		// Launch singleton 
		OllamaService.getInstance(_models, _settings);

		// Populate an ensemble of agents
		for (String model_name: _models.split(",")) {

			// Launch strict agent per included model type
			OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, _hide_llm_reply_if_uncertain, _use_random_seed);
			LOGGER.info("Using " + a1.getEndpoint().getOllama_url() + " with model " + model_name);
			SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(_query, _hide_llm_reply_if_uncertain, _settings.getOllama_timeout());
			Integer probaThreshold = Globals.MODEL_PROBABILITY_THRESHOLDS.get(model_name);
			if (null == probaThreshold) probaThreshold = 55;
			if (ssr1.getProbability()>probaThreshold) return ssr1;

			System.out.println("\n" + model_name);
			ssr1.print();
		}
		return new SingleStringQuestionResponse();
	}

	public static SingleStringQuestionResponse strictEnsembleRunEarlyExitOnFirstConfident(String _query, String _models, OllamaDramaSettings _settings, boolean _hide_llm_reply_if_uncertain, Integer probaThreshold, boolean _use_random_seed) {

		// Launch singleton 
		OllamaService.getInstance(_models, _settings);

		// Populate an ensemble of agents
		for (String model_name: _models.split(",")) {

			// Launch strict agent per included model type
			OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, _hide_llm_reply_if_uncertain, _use_random_seed);
			LOGGER.info("Using " + a1.getEndpoint().getOllama_url() + " with model " + model_name);
			SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(_query, _hide_llm_reply_if_uncertain, _settings.getOllama_timeout());
			if (ssr1.getProbability()>probaThreshold) return ssr1;

			System.out.println("\n" + model_name);
			ssr1.print();
			System.out.println("\n");
		}
		return new SingleStringQuestionResponse();
	}

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, boolean _hide_llm_reply_if_uncertain) {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		return strictEnsembleRun(_query, Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP_M, ollama_settings, _hide_llm_reply_if_uncertain, false);
	}

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		return strictEnsembleRun(_query, Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_ANDUP_M, ollama_settings, _hide_llm_reply_if_uncertain, _use_random_seed);
	}

	public static SingleStringEnsembleResponse strictEnsembleRun(String _query, String _models, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.setOllama_models(_models); // specific wins over default
		ollama_settings.sanityCheck();

		return strictEnsembleRun(_query, _models, ollama_settings, _hide_llm_reply_if_uncertain, _use_random_seed);
	}

	public static void singleRun(String _model_name, String _query, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {

		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		// Launch singleton 
		OllamaService.getInstance(_model_name, ollama_settings);

		// Launch 3 agents, strict, creative, default
		OllamaSession agent_strict = OllamaService.getStrictProtocolSession(_model_name, _hide_llm_reply_if_uncertain, _use_random_seed);
		if (agent_strict.getOllamaAPI().ping()) System.out.println(" - STRICT ollama agent [" + _model_name + "] is operational");

		OllamaSession agent_creative = OllamaService.getCreativeSession(_model_name);
		if (agent_creative.getOllamaAPI().ping()) System.out.println(" - CREATIVE ollama agent [" + _model_name + "] is operational");

		OllamaSession agent_default = OllamaService.getDefaultSession(_model_name);
		if (agent_default.getOllamaAPI().ping()) System.out.println(" - DEFAULT ollama agent [" + _model_name + "] is operational");

		System.out.println("");

		SingleStringQuestionResponse reply_strict = agent_strict.askStrictChatQuestion(_query, _hide_llm_reply_if_uncertain, ollama_settings.getOllama_timeout());
		System.out.println("STRICT [" + _model_name + "]:\n-----------------");
		System.out.println("[" + reply_strict.getProbability() + "%] " + reply_strict.getResponse());
		System.out.println("motivation: " + reply_strict.getMotivation());
		System.out.println("assumptions_made: " + reply_strict.getAssumptions_made() + "\n");

		System.out.println("CREATIVE [" + _model_name + "]:\n-----------------");
		SingleStringQuestionResponse reply_creative = agent_creative.askStrictChatQuestion(_query, _hide_llm_reply_if_uncertain, ollama_settings.getOllama_timeout());
		System.out.println("[" + reply_creative.getProbability() + "%] " + reply_creative.getResponse());
		System.out.println("motivation: " + reply_creative.getMotivation());
		System.out.println("assumptions_made: " + reply_creative.getAssumptions_made() + "\n");

		System.out.println("DEFAULT [" + _model_name + "]:\n-----------------");
		String reply_default = agent_default.askGenericSingleWordQuestion(_query);
		System.out.println(reply_default);
	}

	public static ModelsScoreCard updateScoreCard(
			ModelsScoreCard scorecard,
			String _model_name,
			String _query_index,
			String _question,
			HashMap<String,Integer> _acceptable_answers,
			SingleStringQuestionResponse _r) {

		if (null == _r) {
			HashMap<String, HashMap<String, HashMap<HashMap<String, Integer>, SingleStringQuestionResponse>>> scorecard_t = scorecard.getScorecard();
			HashMap<String, HashMap<HashMap<String, Integer>, SingleStringQuestionResponse>> model_scorecard =  scorecard_t.get(_model_name);
			if (null == model_scorecard) model_scorecard = new HashMap<>();
			HashMap<HashMap<String, Integer>, SingleStringQuestionResponse> model_q = model_scorecard.get(_query_index);
			if (null == model_q) model_q = new HashMap<>();
			model_q.put(_acceptable_answers, null); // INVALID
			model_scorecard.put(_query_index, model_q);
			scorecard_t.put(_model_name, model_scorecard);
			scorecard.setScorecard(scorecard_t);
		} else {
			System.out.println("[" + _r.getProbability() + "%] " + _r.getResponse());
			System.out.println("motivation: " + _r.getMotivation());
			System.out.println("assumptions_made: " + _r.getAssumptions_made());
			System.out.println("tool_calls: " + _r.getTool_calls() + "\n");
			HashMap<String, HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>>> scorecard_t = scorecard.getScorecard();
			HashMap<String, HashMap<HashMap<String,Integer>, SingleStringQuestionResponse>> model_scorecard =  scorecard_t.get(_model_name);
			if (null == model_scorecard) model_scorecard = new HashMap<>();
			HashMap<HashMap<String,Integer>, SingleStringQuestionResponse> model_q = model_scorecard.get(_query_index);
			if (null == model_q) model_q = new HashMap<>();
			model_q.put(_acceptable_answers, _r);
			model_scorecard.put(_query_index, model_q);
			scorecard_t.put(_model_name, model_scorecard);
			scorecard.setScorecard(scorecard_t);
		}

		return scorecard;
	}

	public static void printChatHistory(OllamaSession a1) {
		System.out.println("");
		System.out.println("chat history: " + a1.getChatResult().getChatHistory().size());
		for (OllamaChatMessage mojo: a1.getChatResult().getChatHistory()) {
			System.out.println(" - " + mojo.getContent());
		}
		System.out.println("");
	}

	public static SingleStringQuestionResponse applyResponseSanity(SingleStringQuestionResponse _rx, String _model_name, boolean _hide_llm_reply_if_uncertain) {
		if (null == _rx) {
			_rx = new SingleStringQuestionResponse("JSONERROR", 0, "", "", "");
		} else {
			//String orig_resp = rx.getResponse();
			if (null == _rx.getProbability()) return new SingleStringQuestionResponse("JSONERROR", 0, "", "", "");
			if (null == _rx.getMotivation()) return new SingleStringQuestionResponse("JSONERROR", 0, "", "", "");
			if (null == _rx.getResponse()) return new SingleStringQuestionResponse("JSONERROR", 0, "", "", "");
			if (null == _rx.getAssumptions_made()) return new SingleStringQuestionResponse("JSONERROR", 0, "", "", "");

			if (_hide_llm_reply_if_uncertain) {
				Integer probaThreshold = Globals.MODEL_PROBABILITY_THRESHOLDS.get(_model_name);
				if (null == probaThreshold) probaThreshold = 55;
				if (_rx.getProbability() < probaThreshold) return new SingleStringQuestionResponse("LOWPROBA", _rx.getProbability(), _rx.getMotivation(), _rx.getAssumptions_made(), _rx.getTool_calls());
			}
		}
		return _rx;
	}

	public static HashMap<String, Integer> singleValScore(String _str, int _score) {
		HashMap<String, Integer> resp = new HashMap<String, Integer>();
		resp.put(_str, _score);
		return resp;
	}

	public static OllamaDramaSettings parseOllamaDramaConfigENV() {
		OllamaDramaSettings settings = new OllamaDramaSettings();
		String settings_env = System.getenv("OLLAMADRAMA_CONFIG");
		if (null == settings_env) {
			LOGGER.debug("OLLAMADRAMA_CONFIG env variable not set");
		} else {
			LOGGER.debug("OLLAMADRAMA_CONFIG env variable set");
			settings = JSONUtils.createPOJOFromJSONOpportunistic(settings_env, OllamaDramaSettings.class);
			if (null == settings) {
				LOGGER.warn("Unable to parse the provided OLLAMADRAMA_CONFIG");
			} else {
				if (null != settings.getOllama_username()) LOGGER.debug("Ollama username: " + "*".repeat(settings.getOllama_username().length()));
				if (null != settings.getOllama_password()) LOGGER.debug("Ollama password:" + StringUtils.repeat('_', settings.getOllama_password().length()));
				if (null != settings.getOllama_models()) LOGGER.debug("Ollama models:" + settings.getOllama_models());
				LOGGER.debug("Ollama timeout:" + settings.getOllama_timeout());
				if (null != settings.getThreadPoolCount()) LOGGER.debug("ThreadPoolCount:" + settings.getThreadPoolCount());
			}
		}
		return settings;
	}

	public static SingleStringEnsembleResponse merge(SingleStringEnsembleResponse _sser1, SingleStringEnsembleResponse _sser2) {
		SingleStringEnsembleResponse mergedResponse = _sser1;

		// agent responses,  these keys never overlap, instance based so just add sser2 over sser1
		HashMap<String, SingleStringQuestionResponse> newval = mergedResponse.getSession_responses();
		for (String s: _sser2.getSession_responses().keySet()) {
			SingleStringQuestionResponse val =  _sser2.getSession_responses().get(s);
			newval.put(s, val);
		}
		mergedResponse.setSession_responses(newval);

		// uniq responses
		HashMap<String, HashMap<String, Boolean>> newuniqreplies = mergedResponse.getUniq_replies();
		for (String s: _sser2.getUniq_replies().keySet()) {
			HashMap<String, Boolean> val_existing =  mergedResponse.getUniq_replies().get(s);
			HashMap<String, Boolean> val =  _sser2.getUniq_replies().get(s);
			if (null == val_existing) val_existing = new HashMap<String, Boolean>();
			for (String k: val.keySet()) {
				Boolean content = val.get(k);
				val_existing.put(k, content);
			}
			newuniqreplies.put(s, val_existing);
			mergedResponse.setUniq_replies(newuniqreplies);
		}

		// uniq confident responses
		HashMap<String, HashMap<String, Boolean>> newuniqcreplies = mergedResponse.getUniq_confident_replies();
		for (String s: _sser2.getUniq_confident_replies().keySet()) {
			HashMap<String, Boolean> val_existing =  mergedResponse.getUniq_confident_replies().get(s);
			HashMap<String, Boolean> val =  _sser2.getUniq_confident_replies().get(s);

			// new unique reply?
			if (null == val_existing) val_existing = new HashMap<String, Boolean>();
			for (String k: val.keySet()) {
				Boolean content = val.get(k);
				val_existing.put(k, content);
			}
			newuniqcreplies.put(s, val_existing);
			mergedResponse.setUniq_confident_replies(newuniqcreplies);
		}

		return mergedResponse;
	}

	public static SingleStringEnsembleResponse collectiveFullEnsembleRun(String _query, String _ollama_model_names, String _openai_model_names, OllamaDramaSettings _ollama_settings, boolean _printFirstrun, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		if (_ollama_settings.getOpenaikey().length() < 10) {
			LOGGER.error("To use OpenAI you need do define a valid API key");
			return new SingleStringEnsembleResponse();
		}
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(_query, _ollama_model_names, _hide_llm_reply_if_uncertain, _use_random_seed);
		SingleStringEnsembleResponse sser2 = OpenAIUtils.strictEnsembleRun(_query, _openai_model_names, _ollama_settings, _hide_llm_reply_if_uncertain);
		SingleStringEnsembleResponse sser = OllamaUtils.merge(sser1, sser2);
		if (_printFirstrun) sser.printEnsembleSummary();

		if (sser.getUniq_confident_replies().size() > 0) {
			LOGGER.info("We have at least 1 confident reply so running a collective round");
			StringBuffer sb = new StringBuffer();
			for (String conf_resp: sser.getUniq_confident_replies().keySet()) {
				sb.append(" - " + conf_resp + "\n");
			}

			SingleStringEnsembleResponse sser3 = OllamaUtils.strictEnsembleRun(
					_query 
					+ Globals.ENSEMBLE_LOOP_STATEMENT + "\n"
					+ sb.toString(),
					_ollama_model_names,
					_hide_llm_reply_if_uncertain,
					_use_random_seed);

			SingleStringEnsembleResponse sser4 = OpenAIUtils.strictEnsembleRun(_query 
					+ Globals.ENSEMBLE_LOOP_STATEMENT + "\n"
					+ sb.toString(), 
					_openai_model_names, _ollama_settings,
					_hide_llm_reply_if_uncertain);

			SingleStringEnsembleResponse sserc = OllamaUtils.merge(sser3, sser4);
			return sserc;
		} else {
			return sser;
		}

	}

	public static void llmKnowledgeShootout1v1(String _prompt, String model_name1, String model_name2) {

		boolean use_random_seed = true;
		boolean firstrun = true;
		long roundcounter = 0;
		boolean memoryloss = false;
		String qa_a1_summary = "";
		String qa_a2_summary = "";

		int a1_score = 0;
		int a2_score = 0;

		HashMap<String, String> a1_qa = new HashMap<>();
		HashMap<String, String> a2_qa = new HashMap<>();

		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.sanityCheck();

		String elevenlabsapikey = settings.getElevenlabs_apikey();
		String voice1 = settings.getElevenlabs_voice1();
		String voice2 = settings.getElevenlabs_voice2();

		// Launch LLM singletons if needed
		if (model_name1.equals(model_name2)) {
			settings.setOllama_models(model_name1);
		} else {
			settings.setOllama_models(model_name1 + "," + model_name2);
		}
		OllamaService.getInstance(settings);

		// Launch strict session
		OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name1, false, use_random_seed);
		if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name1 + "] is operational\n");

		OllamaSession a2 = OllamaService.getStrictProtocolSession(model_name2, false, use_random_seed);
		if (a2.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name2 + "] is operational\n");

		while (true) {

			if (firstrun) {
				SpeechUtils.justPlayAudioClipWithDefaults("My voice will represent the model " + model_name1 + " in this debate", elevenlabsapikey, voice1);
				SpeechUtils.justPlayAudioClipWithDefaults("My voice will represent the model " + model_name2 + " in this debate", elevenlabsapikey, voice2);
			}

			// Agent #1 - Create initial question
			String q1 = "";
			SingleStringQuestionResponse ssr1 = null;
			if (firstrun) {
				q1 = _prompt
						+ "\n\n"
						+ "You are up first so you can start off by asking a question\n";
			} else if (memoryloss) {
				if (qa_a1_summary.length() < 10) {
					System.out.println("chathistory_self_summary is not set?");
					System.exit(1);
				}
				if (qa_a2_summary.length() < 10) {
					System.out.println("chathistory_date_summary is not set?");
					System.exit(1);
				}
				a1 = OllamaService.getStrictProtocolSession(model_name1, false, use_random_seed);
				if (a1.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name1 + "] is operational\n");
				q1 = _prompt
						+ "\n\n"
						+ "Summary of your opponent performance so far:\n"
						+ qa_a2_summary
						+ "\n"
						+ "\n"
						+ "You are up so time to ask the next question\n";
			} else {
				q1 = "Your turn to ask a question\n";

			}
			System.out.println("\nTO a1: Prompt1\n" + "=".repeat(31) + "\n" + q1 + "-".repeat(31) + "\n");
			ssr1 = a1.askStrictChatQuestion(q1, false, settings.getOllama_timeout());
			ssr1 = OllamaUtils.applyResponseSanity(ssr1, a1.getModel_name(), false);
			ssr1.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr1.getResponse() + "." + ssr1.getMotivation(), elevenlabsapikey, voice1);

			// Agent #2 - Reply evaluation
			String q2 = "";
			SingleStringQuestionResponse ssr2 = null;
			if (firstrun) {
				q2 = _prompt
						+ "\n"
						+ "The first question has been sent to you: \n"
						+ "'" + ssr1.getResponse() + "'"
						+ "\n";
			} else if (memoryloss) {
				if (qa_a1_summary.length() < 10) {
					System.out.println("chathistory_self_summary is not set?");
					System.exit(1);
				}
				if (qa_a2_summary.length() < 10) {
					System.out.println("chathistory_date_summary is not set?");
					System.exit(1);
				}

				a2 = OllamaService.getStrictProtocolSession(model_name2, false, use_random_seed);
				if (a2.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name2 + "] is operational\n");
				q2 = _prompt
						+ "\n\n"
						+ "Summary of your opponent performance so far:\n"
						+ qa_a1_summary
						+ "\n"
						+ "You just got the next question sent to you\n"
						+ "'" + ssr1.getResponse() + "'"
						+ "\n";

			} else {
				q2 = "The following question was sent to you: \n"
						+ "'" + ssr1.getResponse() + "'"
						+ "\n";
			}
			System.out.println("\nTO a2: Prompt2\n" + "-".repeat(31) + "\n" + q2 + "-".repeat(31) + "\n");
			ssr2 = a2.askStrictChatQuestion(q2, false, settings.getOllama_timeout());
			ssr2 = OllamaUtils.applyResponseSanity(ssr2, a2.getModel_name(), false);
			ssr2.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr2.getResponse() + "." + ssr2.getMotivation(), elevenlabsapikey, voice2);

			// Update qa history for a2
			a2_qa.put(ssr1.getResponse(), ssr2.getResponse());

			// Check for response errors
			if (ssr2.getResponse().contains("FAILTOUNDERSTAND") || ssr2.getResponse().contains("JSONERROR")) {
				System.out.println(a2.getModel_name() + " just failed to align with the JSON protocol");
				SystemUtils.halt();
			}

			// Agent #1 - Reply evaluation
			String q1b = "Your opponent replied with the following answer: \n"
					+ "'" + ssr2.getResponse() + "'"
					+ "\n\n"
					+ "Evaluate this answer and in the next round you will get a question, just wait\n";
			System.out.println("\nTO a1: Prompt3\n" + "-".repeat(31) + "\n" + q1b + "-".repeat(31) + "\n");
			SingleStringQuestionResponse ssr1b = a1.askStrictChatQuestion(q1b, false, settings.getOllama_timeout());
			ssr1b = OllamaUtils.applyResponseSanity(ssr1b, a1.getModel_name(), false);
			ssr1b.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr1b.getResponse() + "." + ssr1b.getMotivation(), elevenlabsapikey, voice1);

			// Get the evaluated score
			int a2_reply_score = getReplyScore(ssr1b.getResponse());
			a2_score = a2_score + a2_reply_score;

			// Agent #2 - First question
			SingleStringQuestionResponse ssr2b = null;
			String q2b =  "It is now your turn to ask a question.\n";
			System.out.println("\nTO a2: Prompt4\n" + "-".repeat(31) + "\n" + q2b + "-".repeat(31) + "\n");
			ssr2b = a2.askStrictChatQuestion(q2b, false, settings.getOllama_timeout());
			ssr2b = OllamaUtils.applyResponseSanity(ssr2b, a2.getModel_name(), false);
			ssr2b.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr2b.getResponse() + "." + ssr2b.getMotivation(), elevenlabsapikey, voice2);

			// Agent #1 - Get first question
			String q1c = "A question has been sent to you: \n"
					+ "'" + ssr2b.getResponse() + "'"
					+ "\n";

			System.out.println("\nTO a1: Prompt5\n" + "-".repeat(31) + "\n" + q1c + "-".repeat(31) + "\n");
			SingleStringQuestionResponse ssr1c = a1.askStrictChatQuestion(q1c, false, settings.getOllama_timeout());
			ssr1c = OllamaUtils.applyResponseSanity(ssr1c, a1.getModel_name(), false);
			ssr1c.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr1c.getResponse() + "." + ssr1c.getMotivation(), elevenlabsapikey, voice1);

			// Check for response errors
			if (ssr1c.getResponse().contains("FAILTOUNDERSTAND") || ssr1c.getResponse().contains("JSONERROR")) {
				System.out.println(a1.getModel_name() + " just failed to align with the JSON protocol");
				SystemUtils.halt();
			}


			// Agent #2 - Reply evaluation
			String q2c = "Your opponent replied with the following answer: \n"
					+ "'" + ssr1c.getResponse() + "'"
					+ "\n\n"
					+ "Evaluate this answer and in the next round you will get a question, just wait\n";
			System.out.println("\nTO a2: Prompt6\n" + "-".repeat(31) + "\n" + q2c + "-".repeat(31) + "\n");
			SingleStringQuestionResponse ssr2c = a2.askStrictChatQuestion(q2c, false, settings.getOllama_timeout());
			ssr2c = OllamaUtils.applyResponseSanity(ssr2c, a2.getModel_name(), false);
			ssr2c.print();
			SpeechUtils.justPlayAudioClipWithDefaults(ssr2c.getResponse() + "." + ssr2c.getMotivation(), elevenlabsapikey, voice2);

			// Get the evaluated score
			int a1_reply_score = getReplyScore(ssr2c.getResponse());
			a1_score = a1_score + a1_reply_score;

			// Update qa history for a1
			a1_qa.put(ssr2b.getResponse(), ssr1c.getResponse());

			// Check for memory loss
			int chatsize_wordcount_a1 = a1.getChatSizeWordCount();
			int chatsize_wordcount_a2 = a2.getChatSizeWordCount();

			if (false ||
					(chatsize_wordcount_a1 > 6000) ||
					(chatsize_wordcount_a2 > 6000) ||
					false) {
				System.out.println("memory loss risk is high since chathistory wordcount exceeds threshold");
				memoryloss = true;

				// Launch strict session to summarize
				OllamaSession a3 = OllamaService.getStrictProtocolSession(model_name1, false, use_random_seed);
				if (a3.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name1 + "] is operational\n");

				String sum1 = "Create a minimal summary of facts based off of "
						+ "the following chat Q/A history. \n\n" + "=".repeat(31) + "\n" + createQAHistory(a1_qa) + "=".repeat(31) + "\n";
				System.out.println(sum1);
				SystemUtils.sleepInSeconds(50);
				SingleStringQuestionResponse ssrsuma1 = a3.askStrictChatQuestion(sum1, false, settings.getOllama_timeout());
				ssrsuma1 = OllamaUtils.applyResponseSanity(ssrsuma1, a1.getModel_name(), false);
				ssrsuma1.print();
				qa_a1_summary = ssrsuma1.getResponse();

				// Launch strict session to summarize
				OllamaSession a4 = OllamaService.getStrictProtocolSession(model_name1, false, use_random_seed);
				if (a4.getOllamaAPI().ping()) System.out.println(" - STRICT ollama session [" + model_name1 + "] is operational\n");

				String sum2 = "Create a minimal summary of facts based off of "
						+ "the following chat Q/A history. \n\n" + "=".repeat(31) + "\n" + createQAHistory(a2_qa) + "=".repeat(31) + "\n";
				System.out.println(sum2);
				SystemUtils.sleepInSeconds(50);
				SingleStringQuestionResponse ssrsuma2 = a4.askStrictChatQuestion(sum2, false, settings.getOllama_timeout());
				ssrsuma2 = OllamaUtils.applyResponseSanity(ssrsuma2, a1.getModel_name(), false);
				ssrsuma2.print();
				qa_a2_summary = ssrsuma2.getResponse();

			} else {
				memoryloss = false;
			}

			firstrun = false;

			roundcounter++;
			System.out.println("\nROUND " + roundcounter + "\n");
			System.out.println(" - a1_score: "  + a1_score);
			System.out.println(" - a2_score: "  + a2_score);
			System.out.println(" - ");
			System.out.println(" - chatsize_wordcount_a1: "  + chatsize_wordcount_a1);
			System.out.println(" - chatsize_wordcount_a2: "  + chatsize_wordcount_a2);
			//SystemUtils.sleepInSeconds(10);
		}

	}

	private static int getReplyScore(String response) {
		try {
			Integer reply_score = Integer.valueOf(response);
			if ((reply_score <= 10) && (reply_score >= 0)) {
				return reply_score;
			}
		} catch (Exception e) {
			LOGGER.error("Caught exception trying to interpret " + response + " as an int");
		}
		LOGGER.error("Failed to get a valid 0-10 score from " + response);
		SystemUtils.halt();
		return 0;
	}

	private static String createQAHistory(HashMap<String, String> hm) {
		StringBuffer sb = new StringBuffer();
		for (String q: hm.keySet()) {
			String a = hm.get(q);
			sb.append("Question: " + q + "\n");
			sb.append("Answer: " + a + "\n\n");
		}
		return sb.toString();
	}

	/** Ordered bucket of model sizes (smallest → largest). */
	public enum ModelSize { S, M, L, XL, XXL }

	/* ------------------------------------------------------------ *
	 *   Pre-computed look-up tables                                *
	 * ------------------------------------------------------------ */

	private static final Set<String> SIZE_S = parse(Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_S);

	private static final Set<String> SIZE_M = Stream.of(
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_M,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_M,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_M,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER4_M)
			.flatMap(s -> parse(s).stream())
			.collect(Collectors.toUnmodifiableSet());

	private static final Set<String> SIZE_L = Stream.of(
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_L,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_L)
			.flatMap(s -> parse(s).stream())
			.collect(Collectors.toUnmodifiableSet());

	private static final Set<String> SIZE_XL = Stream.of(
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER1_XL,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER2_XL,
			Globals.ENSEMBLE_MODEL_NAMES_OLLAMA_TIER3_XL)
			.flatMap(s -> parse(s).stream())
			.collect(Collectors.toUnmodifiableSet());

	private static Set<String> parse(String csv) {
		return Stream.of(csv.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	/* ------------------------------------------------------------ *
	 *   Public API                                                 *
	 * ------------------------------------------------------------ */

	/**
	 * @return {@code true} when {@code modelName} exceeds {@code maxSizeStr}
	 *         and should therefore be skipped.
	 *
	 * @throws IllegalArgumentException if {@code maxSizeStr} is not S, M, L, XL, or XXL
	 */
	public static boolean is_skip_model_autopull(String maxSizeStr, String modelName) {
		ModelSize maxSize  = toSize(maxSizeStr);
		ModelSize thisSize = resolveSize(modelName);

		// Skip when model is larger than the allowed maximum
		return thisSize.ordinal() > maxSize.ordinal();
	}

	/* ------------------------------------------------------------ *
	 *   Internals                                                  *
	 * ------------------------------------------------------------ */

	private static ModelSize resolveSize(String modelName) {
		if (SIZE_S.contains(modelName))  return ModelSize.S;
		if (SIZE_M.contains(modelName))  return ModelSize.M;
		if (SIZE_L.contains(modelName))  return ModelSize.L;
		if (SIZE_XL.contains(modelName)) return ModelSize.XL;
		return ModelSize.XXL;  // default / largest
	}

	private static ModelSize toSize(String str) {
		try {
			return ModelSize.valueOf(str);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown model size: " + str, ex);
		}
	}
}
