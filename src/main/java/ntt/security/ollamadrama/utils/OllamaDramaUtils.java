package ntt.security.ollamadrama.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.ConfidenceThresholdCard;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.objects.response.SingleStringQuestionResponse;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.objects.sessions.OpenAISession;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.singletons.OpenAIService;

public class OllamaDramaUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(OllamaDramaUtils.class);
	private static final Random RANDOM = new Random();

	public static SingleStringEnsembleResponse strictCollectiveEnsembleRun(String _query, String _models, boolean _printFirstrun, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();
		return collectIterativeEnsembleVotes(_query, _models, ollama_settings, _printFirstrun, _hide_llm_reply_if_uncertain, _use_random_seed);
	}


	public static SingleStringEnsembleResponse collectEnsembleVotes(String _query, String _ollama_model_names, String _openai_model_names, OllamaDramaSettings _ollama_settings, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		if (_ollama_settings.getOpenaikey().length() < 10) {
			LOGGER.error("To use OpenAI you need do define a valid API key");
			return new SingleStringEnsembleResponse();
		}
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(_query, _ollama_model_names, _hide_llm_reply_if_uncertain, _use_random_seed);
		SingleStringEnsembleResponse sser2 = OpenAIUtils.strictEnsembleRun(_query, _openai_model_names, _ollama_settings, _hide_llm_reply_if_uncertain);
		SingleStringEnsembleResponse sser = OllamaUtils.merge(sser1, sser2);
		return sser;
	}

	public static SingleStringEnsembleResponse collectIterativeEnsembleVotes(String _query, String _models, OllamaDramaSettings _settings, boolean _printFirstrun, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		OllamaService.getInstance(_settings);
		OpenAIService.getInstance(_settings);
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				_query, 
				_models,
				_hide_llm_reply_if_uncertain, _use_random_seed);
		if (_printFirstrun) sser1.printEnsembleSummary();
		if (sser1.getUniq_confident_replies().size() > 0) {
			if (sser1.getUniq_confident_replies().size() == 1) {
				LOGGER.info("We have " + sser1.getUniq_confident_replies().size() + " confident reply so running a collective round");
			} else {
				LOGGER.info("We have " + sser1.getUniq_confident_replies().size() + " confident replies so running a collective round");
			}
			StringBuffer sb = new StringBuffer();
			for (String conf_resp: sser1.getUniq_confident_replies().keySet()) {
				sb.append(" - " + conf_resp + "\n");
			}

			SingleStringEnsembleResponse sser2 = OllamaUtils.strictEnsembleRun(
					_query 
					+ Globals.ENSEMBLE_LOOP_STATEMENT + "\n"
					+ sb.toString(),
					_models,
					_hide_llm_reply_if_uncertain, _use_random_seed);
			return sser2;
		} else {
			return sser1;
		}

	}

	public static ConfidenceThresholdCard populateConfidencecardsForOllamaModels(ConfidenceThresholdCard confidencecard, String _queryid, boolean _use_mcp, String _models, String _question, HashMap<String, Integer> _acceptable_answers, String _type_of_question) {
		OllamaDramaSettings settings = OllamaUtils.parse_ollama_drama_config_env();
		if (_use_mcp) {
			settings.setMcp_scan(true);
		} else {
			settings.setMcp_scan(false);
		}
		settings.setOllama_models(_models);
		settings.sanityCheck();
		OllamaService.getInstance(settings);
		for (String model_name: _models.split(",")) {
			if (model_name.length()>=3) {
				int querysubindex = 1;

				try {

					// Launch strict session
					OllamaSession a1 = OllamaService.get_strict_protocol_session(model_name, false, true, _use_mcp);
					if (a1.getOllama().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

					// Make query
					String q1 = _question;
					System.out.println("Question: " + q1);
					SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, false, settings.getOllama_timeout(), null);

					// we need to have an acceptable answer
					if (null ==_acceptable_answers.get(ssr1.getResponse())) {
						LOGGER.error("The model " + model_name + " got this wrong and replied with " + ssr1.getResponse() + ", should it even be part of this test?");
						SystemUtils.halt();
					}
					confidencecard = OllamaUtils.update_confidence_card(confidencecard, model_name, _queryid + "_" + querysubindex, q1, _type_of_question, _acceptable_answers, ssr1);

				} catch (Exception e) {
					LOGGER.error("Caught exception: " + e.getMessage());
				}
			}
		}
		return confidencecard;
	}

	public static ModelsScoreCard populateScorecardsForOllamaModels(boolean _use_mcp, String _models, String _question, HashMap<String, Integer> _acceptable_answers, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed, boolean _return_toolcall, OllamaDramaSettings _settings) {
		ModelsScoreCard scorecard = new ModelsScoreCard();
		OllamaDramaSettings settings = null;
		if (null != _settings) {
			settings = _settings;
		} else {
			settings = OllamaUtils.parse_ollama_drama_config_env();
		}
		if (_use_mcp) {
			settings.setMcp_scan(true);
		} else {
			settings.setMcp_scan(false);
		}
		settings.setOllama_models(_models);
		settings.sanityCheck();
		OllamaService.getInstance(settings);
		for (String model_name: _models.split(",")) {
			if (model_name.length()>=3) {
				int queryindex = 1;

				try {

					// Launch strict session
					OllamaSession a1 = OllamaService.get_strict_protocol_session(model_name, _hide_llm_reply_if_uncertain, _use_random_seed, _use_mcp);
					if (a1.getOllama().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");
					
					// Make query
					String q1 = _question;
					System.out.println("Question: " + q1);
					SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, _hide_llm_reply_if_uncertain, settings.getOllama_timeout(), 10, _return_toolcall, null);
					ssr1 = OllamaUtils.apply_response_sanity(ssr1, model_name, _hide_llm_reply_if_uncertain);
					scorecard = OllamaUtils.update_score_card(scorecard, model_name, "q" + queryindex, q1, _acceptable_answers, ssr1, false);

				} catch (Exception e) {
					LOGGER.error("Caught exception: " + e.getMessage());
				}

			}
		}
		return scorecard;
	}

	public static ModelsScoreCard populateProbabilityForOllamaModels(String _models, String _question, HashMap<String, Integer> _acceptable_answers, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed, long _timeout, boolean _return_toolcall, OllamaDramaSettings _settings) {
		ModelsScoreCard scorecard = new ModelsScoreCard();
		OllamaDramaSettings settings = null;
		if (null != _settings) {
			settings = _settings;
		} else {
			settings = OllamaUtils.parse_ollama_drama_config_env();
		}
		settings.setOllama_models(_models);
		settings.sanityCheck();
		OllamaService.getInstance(settings);
		for (String model_name: _models.split(",")) {
			if (model_name.length()>=3) {
				int queryindex = 1;

				try {

					// Launch strict session
					OllamaSession a1 = OllamaService.get_strict_protocol_session(model_name, _hide_llm_reply_if_uncertain, _use_random_seed, false);
					if (a1.getOllama().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");

					// Make query
					String q1 = _question;
					System.out.println("Question: " + q1);
					SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, _hide_llm_reply_if_uncertain, _timeout, 10, _return_toolcall, null);
					ssr1 = OllamaUtils.apply_response_sanity(ssr1, model_name, _hide_llm_reply_if_uncertain);
					scorecard = OllamaUtils.update_score_card(scorecard, model_name, "q" + queryindex, q1, _acceptable_answers, ssr1, false);

				} catch (Exception e) {
					LOGGER.error("Caught exception: " + e.getMessage());
				}

			}
		}
		return scorecard;
	}

	public static ModelsScoreCard populateScorecardsForOpenAIModels(boolean _use_mcp, String _models, OllamaDramaSettings _settings, String _question, HashMap<String, Integer> _acceptable_answers, boolean _hide_llm_reply_if_uncertain) {
		ModelsScoreCard scorecard = new ModelsScoreCard();
		if (_use_mcp) {
			_settings.setMcp_scan(true);
		} else {
			_settings.setMcp_scan(false);
		}
		OpenAIService.getInstance(_settings);
		for (String model_name: _models.split(",")) {
			int queryindex = 1;
			if (_settings.isUse_openai()) {
				OpenAISession a1 = OpenAIService.getStrictSession(model_name, _settings);
				System.out.println(" - STRICT openai session [" + model_name + "] is operational\n");
				String q1 = _question;
				System.out.println("Question: " + q1);
				SingleStringQuestionResponse ssr1 = a1.askChatQuestion(q1, _hide_llm_reply_if_uncertain);
				ssr1 = OllamaUtils.applyResponseSanity(ssr1, model_name, _hide_llm_reply_if_uncertain);
				scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q" + queryindex, q1, _acceptable_answers, ssr1);
			} else {
				LOGGER.info("Configured not to use OpenAI");
			}
		}
		return scorecard;
	}

	public static ModelsScoreCard populateScorecardsForOllamaModels(boolean _use_mcp, String _models, String _question, String _expectedresponse, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed, boolean _return_toolcall) {
		return populateScorecardsForOllamaModels(_use_mcp, _models, _question, OllamaUtils.singleValScore(_expectedresponse, 1), _hide_llm_reply_if_uncertain, _use_random_seed, _return_toolcall, null);
	}


	public static ModelsScoreCard populateScorecardsForOpenAIModels(String _models, OllamaDramaSettings _settings, String _question, String _expectedresponse, boolean _hide_llm_reply_if_uncertain) {
		return populateScorecardsForOpenAIModels(_settings.isMcp_scan(), _models, _settings, _question, OllamaUtils.singleValScore(_expectedresponse, 1), _hide_llm_reply_if_uncertain);
	}

	public static ModelsScoreCard populateScorecardsForOpenAIAndOllamaModels(boolean _use_mcp, String _models, String _openaimodels, OllamaDramaSettings _settings, String _question, String _expectedresponse, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		return populateScorecardsForOpenAIAndOllamaModels(_use_mcp, _models, _openaimodels, _settings, _question, OllamaUtils.singleValScore(_expectedresponse, 1), _hide_llm_reply_if_uncertain, _use_random_seed);
	}

	public static ModelsScoreCard populateScorecardsForOpenAIAndOllamaModels(boolean _use_mcp, String _models, String _openaimodels, OllamaDramaSettings _settings, String _question, HashMap<String, Integer> _acceptable_answers, boolean _hide_llm_reply_if_uncertain, boolean _use_random_seed) {
		ModelsScoreCard scorecard = new ModelsScoreCard();
		_settings.setOllama_models(_models);
		OllamaService.getInstance(_settings);
		for (String model_name: _models.split(",")) {
			int queryindex = 1;

			try {
				OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, _hide_llm_reply_if_uncertain, _use_random_seed, false);
				if (a1.getOllama().ping()) System.out.println(" - STRICT ollama session [" + model_name + "] is operational\n");
				String q1 = _question;
				System.out.println("Question: " + q1);
				SingleStringQuestionResponse ssr1 = a1.askStrictChatQuestion(q1, _hide_llm_reply_if_uncertain, _settings.getOllama_timeout(), null);
				ssr1 = OllamaUtils.applyResponseSanity(ssr1, model_name, _hide_llm_reply_if_uncertain);
				scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q" + queryindex, q1, _acceptable_answers, ssr1);

			} catch (Exception e) {
				LOGGER.error("Caught exception: " + e.getMessage());
			}
		}
		OpenAIService.getInstance(_settings);
		for (String model_name: _openaimodels.split(",")) {
			int queryindex = 1;

			// Launch strict session
			if (_settings.isUse_openai()) {
				OpenAISession a1 = OpenAIService.getStrictSession(model_name, _settings);
				System.out.println(" - STRICT openai session [" + model_name + "] is operational\n");

				// Make query
				String q1 = _question;
				System.out.println("Question: " + q1);
				SingleStringQuestionResponse ssr1 = a1.askChatQuestion(q1, _hide_llm_reply_if_uncertain);
				ssr1 = OllamaUtils.applyResponseSanity(ssr1, model_name, _hide_llm_reply_if_uncertain);

				ssr1.print();

				scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q" + queryindex, q1, _acceptable_answers, ssr1);
			} else {
				LOGGER.info("Configured not to use OpenAI");
			}
		}
		return scorecard;
	}

	@SuppressWarnings({"serial"})
	public static ModelsScoreCard performMemoryTestUsingRandomWordNeedleTest(String _models, int _nrErrorsToAllow, boolean _use_random_seed, String _tag) {
		String needle_word = "needle";
		boolean apply_earlyexit_after_errorthreshold = true;
		int len_random_word = 3; // o1 - one token typically corresponds to around 3–4 characters of text on average
		ModelsScoreCard scorecard = new ModelsScoreCard();

		// launch ollamadrama
		OllamaDramaSettings settings = new OllamaDramaSettings();
		settings.setOllama_models(_models);
		settings.setMcp_scan(false);
		OllamaService.getInstance(settings);

		// iterate through the needle test for each model with default n_ctx
		String maxcapFilePath = "model_maxcap.csv";
		FilesUtils.writeToFileUNIXNoException("tag,model,n_ctx,expected,actual,context_wordcount,context_charcount,errorcount", maxcapFilePath);
		for (String model_name: _models.split(",")) {
			Integer n_ctx = Globals.n_ctx_defaults.get(model_name);
			int maxCharsCounted = 0;
			int errorcounter = 0;
			String bagOfWords = "";
			for (int x=1000; x<=200000; x+=1000) {

				// only go to next x if sucessful
				boolean passed_x_level = false;
				int trycounter = 0;
				while (!passed_x_level && trycounter<=_nrErrorsToAllow) {
					trycounter++;
					if (false ||
							(apply_earlyexit_after_errorthreshold && (errorcounter <= _nrErrorsToAllow)) ||
							!apply_earlyexit_after_errorthreshold ||
							false) {

						try {
							OllamaSession a1 = OllamaService.get_strict_protocol_session(model_name, false, _use_random_seed, false);
							if (a1.getOllama().ping()) System.out.println(" - STRICT ollama agent [" + model_name + "] is operational\n");

							int needle_pos = NumUtils.randomNumWithinRangeAsInt(20, x-20);

							String firstWord =  generateRandomWord(len_random_word);
							bagOfWords = firstWord;
							String lastWord = "";
							for (int i=2; i<=(x+2); i++) {
								String newWord = generateRandomWord(len_random_word);
								if (i == needle_pos) {
									bagOfWords = bagOfWords + " " + needle_word;
								} else {
									bagOfWords = bagOfWords + " " + newWord;
								}
								lastWord = newWord;
							}

							System.out.println("** Will test the LLM by placing the needle at word #" + needle_pos + " (bagsize: " + x + ")");
							String qx = bagOfWords + "\n" + "Above is a list of individual words separated by space and ordered from left to right. The first word is " + firstWord + " and the last word is " + lastWord 
									+ ". Your task is to find a lowercase English word from this list with length " + needle_word.length() + " characters. Make sure to walk through and "
									+ "evaluate each candidate word and make sure your answer only consists of the single word. You will be punished if you answer with a word which is not " + needle_word.length() + " characters long and exists in the list."
									+ " The list is complete and contains no typos so do not make any excuses about the list being too long to process manually. "
									+ "I promise that you will find one of the following words in the list if you really put your mind to it: 'breeze', '" + needle_word + "', 'puzzle', 'glance' and 'borrow'";
							System.out.println("Question: " + StringsUtils.cutAndPadStringToN(qx, len_random_word*16) + "..." + StringsUtils.getLastNCharacters(qx, 400));

							SingleStringQuestionResponse rx = a1.askStrictChatQuestion(qx, false, 1, 120000L, null); // 1 try, 2 min timeout, no history file
							if (rx != null) {
								System.out.println("Current bagsize: " + x);
								System.out.println("Current needle word location: " + needle_pos);
								System.out.println("Response: " + rx.getResponse());
								System.out.println("Response proba: " + rx.getProbability() + "%");
								System.out.println("Response motivation: " + rx.getMotivation());
								System.out.println("Response assumptions: " + rx.getAssumptions_made());
								System.out.println("Previous errorcount: " + errorcounter);
								if (rx.getResponse().equals("JSONERROR")) {
									// likely LLM restart due to mem starvation
									errorcounter = Integer.MAX_VALUE;
								} else {
									if (rx.getResponse().equals("" + needle_word)) {
										passed_x_level = true;
										if (errorcounter <= _nrErrorsToAllow) {
											try {
												if (x > maxCharsCounted) maxCharsCounted = x;
											} catch (Exception e) {
												// ignore
											}
										}
									} else {
										errorcounter++;
									}
								}
								int chatsize_charcount = a1.getChatSizeCharCount();
								int chatsize_wordcount = a1.getChatSizeWordCount();
								FilesUtils.appendToFileUNIXNoException(_tag + "," + model_name + "," + n_ctx + "," + x + "," + maxCharsCounted + "," + chatsize_wordcount + "," + chatsize_charcount + "," + errorcounter, maxcapFilePath);;
							} else {
								// likely LLM restart due to mem starvasion
								errorcounter = Integer.MAX_VALUE;
							}

						} catch (Exception e) {
							LOGGER.error("Caught exception: " + e.getMessage());
						}
					}
				}
			}
			String maxCharsCountedSTR = maxCharsCounted + "";
			HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
				this.put(maxCharsCountedSTR, 1);
			}};
			SingleStringQuestionResponse rf = new SingleStringQuestionResponse(String.valueOf(maxCharsCounted), 100, "", "", "");
			scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q1", "Max chars counted?", acceptable_answers, rf);
		}
		return scorecard;
	}

	@SuppressWarnings({"serial"})
	public static ModelsScoreCard performMemoryTestUsingRandomWords(String _models, int _nrErrorsToAllow, boolean _use_random_seed, String _tag) {
		boolean apply_earlyexit_after_errorthreshold = true;
		int len_random_word = 3; // o1 - one token typically corresponds to around 3–4 characters of text on average
		ModelsScoreCard scorecard = new ModelsScoreCard();
		OllamaService.getInstance(_models);
		String maxcapFilePath = "model_maxcap.csv";
		FilesUtils.writeToFileUNIXNoException("tag,model,n_ctx,expected,actual,context_wordcount,context_charcount,errorcount", maxcapFilePath);
		// Populate an ensemble of agents
		for (String model_name: _models.split(",")) {
			Integer n_ctx = Globals.n_ctx_defaults.get(model_name);
			int maxCharsCounted = 0;
			int errorcounter = 0;
			String bagOfWords = "";
			for (int x=1000; x<=40000; x+=1000) {
				if (false ||
						(apply_earlyexit_after_errorthreshold && (errorcounter <= _nrErrorsToAllow)) ||
						!apply_earlyexit_after_errorthreshold ||
						false) {
					try {
						OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, false, _use_random_seed, false);
						if (a1.getOllama().ping()) System.out.println(" - STRICT ollama agent [" + model_name + "] is operational\n");
						String firstWord = generateRandomWord(len_random_word);
						bagOfWords = firstWord;
						String lastWord = "";
						for (int i=0; i<=x; i++) {
							String newWord = generateRandomWord(len_random_word);
							lastWord = newWord;
							bagOfWords = bagOfWords + " " + newWord;
						}
						String qx = bagOfWords + "\n" + "In the list of ordered (from left to right) random words above the last word is " + lastWord 
								+ ". As you can see each word consists of " + len_random_word + " uppercase letters. What is the first word? Make sure to walk through and "
								+ "evaluate each candidate word and make sure answer consists of " + len_random_word + " uppercase letters.";
						System.out.println("Question: " + StringsUtils.cutAndPadStringToN(qx, len_random_word) + " ... " + StringsUtils.getLastNCharacters(qx, 400));
						SingleStringQuestionResponse rx = a1.askStrictChatQuestion(qx, false, 5, null); // dont retry
						if (rx != null) {
							System.out.println("Current bagsize: " + x);
							System.out.println("Current firstWord: " + firstWord);
							System.out.println("Response: " + rx.getResponse());
							System.out.println("Response motivation: " + rx.getMotivation());
							System.out.println("Response assumptions: " + rx.getAssumptions_made());
							System.out.println("Previous errorcount: " + errorcounter);
							if (rx.getResponse().equals("JSONERROR")) {
								// likely LLM restart due to mem starvation
								errorcounter = Integer.MAX_VALUE;
							} else {
								if (rx.getResponse().equals(firstWord)) {
									if (errorcounter <= _nrErrorsToAllow) {
										try {
											if (x > maxCharsCounted) {
												maxCharsCounted = x;
											}
										} catch (Exception e) {
											// ignore
										}
									}
								} else {
									errorcounter++;
								}
							}
							int chatsize_charcount = a1.getChatSizeCharCount();
							int chatsize_wordcount = a1.getChatSizeWordCount();
							FilesUtils.appendToFileUNIXNoException(_tag + "," + model_name + "," + n_ctx + "," + x + "," + maxCharsCounted + "," + chatsize_wordcount + "," + chatsize_charcount + "," + errorcounter, maxcapFilePath);;
						} else {
							// likely LLM restart due to mem starvasion
							errorcounter = Integer.MAX_VALUE;
						}

					} catch (Exception e) {
						LOGGER.error("Caught exception: " + e.getMessage());
					}
				}
			}
			String maxCharsCountedSTR = maxCharsCounted + "";
			HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
				this.put(maxCharsCountedSTR, 1);
			}};
			SingleStringQuestionResponse rf = new SingleStringQuestionResponse(String.valueOf(maxCharsCounted), 100, "", "", "");
			scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q1", "Max chars counted?", acceptable_answers, rf);
		}
		return scorecard;
	}


	public static String generateRandomWord(int _wordLength) {
		StringBuilder word = new StringBuilder(_wordLength);
		for (int i = 0; i < _wordLength; i++) {
			char randomChar = (char) ('a' + RANDOM.nextInt(26)); 	// Generate lowercase letter
			randomChar = Character.toUpperCase(randomChar); 		// make uppercase
			word.append(randomChar);
		}
		return word.toString();
	}

	@SuppressWarnings({"serial"})
	public static ModelsScoreCard performMemoryTestUsingSequenceOfNumbers(String _models, int _nrErrorsToAllow, boolean _use_random_seed, long _timeout) {

		ModelsScoreCard scorecard = new ModelsScoreCard();
		OllamaService.getInstance(_models);

		String maxcapFilePath = "model_maxcap.csv";
		FilesUtils.writeToFileUNIXNoException("model,expected,actual,context_wordcount,context_charcount,errorcount", maxcapFilePath);

		// Populate an ensemble of agents
		for (String model_name: _models.split(",")) {
			int maxCharsCounted = 0;
			int diff = 0;
			int errorcounter = 0;
			int firstnum = 4;

			String bagOfCharacters = "";
			for (int x=20; x<=30000; x+=20) {

				try {
				
				// Launch strict agent per test
				OllamaSession a1 = OllamaService.getStrictProtocolSession(model_name, true, _use_random_seed, false);
				if (a1.getOllama().ping()) System.out.println(" - STRICT ollama agent [" + model_name + "] is operational\n");

				bagOfCharacters = "";
				for (int i=firstnum; i<=x; i++) {
					bagOfCharacters = bagOfCharacters + " " + i;
				}
				bagOfCharacters = bagOfCharacters.replaceFirst(" ", "");

				String qx = bagOfCharacters + "\n" + "What is the smallest number you can find in the numbers provided above?";
				//String qx = bagOfCharacters + "\n" + "How many repetitions of the word 'cat' can you find in my input?";
				System.out.println("Question: " + qx);
				SingleStringQuestionResponse rx = a1.askStrictChatQuestion(qx, true, _timeout, null);

				if (rx != null) {
					System.out.println("Current x: " + x);
					System.out.println("Actual answer: " + firstnum);
					System.out.println("Response: " + rx.getResponse());
					System.out.println("Response motivation: " + rx.getMotivation());
					System.out.println("Response assumptions: " + rx.getAssumptions_made());
					System.out.println("Previous errorcount: " + errorcounter);

					try {
						int responseint = Integer.valueOf(rx.getResponse());
						diff = x - responseint;
						System.out.println("Current diff: " + diff);
					} catch (Exception e) {
						// ignore
					}

					if (rx.getResponse().equals("" + firstnum)) {
						// Allow x misses in total?
						if (errorcounter <= _nrErrorsToAllow) {
							try {
								if (x > maxCharsCounted) {
									maxCharsCounted = x;
								}
							} catch (Exception e) {
								// ignore
							}
						}
					} else {
						errorcounter++;
					}

					// context window size
					int chatsize_charcount = a1.getChatSizeCharCount();
					int chatsize_wordcount = a1.getChatSizeWordCount();

					FilesUtils.appendToFileUNIXNoException(model_name + "," + x + "," + maxCharsCounted + "," + chatsize_wordcount + "," + chatsize_charcount + "," + errorcounter, maxcapFilePath);;

				}
				
				} catch (Exception e) {
					LOGGER.error("Caught exception: " + e.getMessage());
				}
			}
			String maxCharsCountedSTR = maxCharsCounted + "";

			HashMap<String, Integer> acceptable_answers = new HashMap<String, Integer>() {{
				this.put(maxCharsCountedSTR, 1);
			}};

			SingleStringQuestionResponse rf = new SingleStringQuestionResponse(String.valueOf(maxCharsCounted), 100, "", "", "");
			scorecard = OllamaUtils.updateScoreCard(scorecard, model_name, "q1", "Max chars counted?", acceptable_answers, rf);

		}
		return scorecard;
	}

}
