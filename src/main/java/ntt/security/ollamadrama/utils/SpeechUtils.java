package ntt.security.ollamadrama.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javazoom.jl.player.Player;
import net.andrewcpu.elevenlabs.ElevenLabs;
import net.andrewcpu.elevenlabs.model.voice.Voice;
import net.andrewcpu.elevenlabs.model.voice.VoiceSettings;

public class SpeechUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(SpeechUtils.class);

	public static void justPlayAudioClipWithDefaults(String _text, String _apikey, String _voice) {

		Double voice_stability = 0.54d; 				// (unstable under 30%)
		Double voice_similarity = 0.55d;
		Double voice_style = 0.46d; 					// exaggeration (unstable above 50%)
		String voice_model = "eleven_multilingual_v2"; 	// eleven_monolingual_v1, eleven_multilingual_v2
		
		if (_apikey.length()>5) {
			byte[] audio_data = null;
			try {
				LOGGER.info(_text);
				LOGGER.info("Generating audio clip..");
				audio_data = generateElevenLabsAudioData(_text, _apikey, voice_model, voice_stability, voice_similarity, voice_style, _voice);
				if (null != audio_data) {
					LOGGER.info("Playing audio clip..");
					Player player = createPlayerFromAudioData(audio_data);
					player.play();
				} else {
					LOGGER.error("audio_data is null!?");
					SystemUtils.halt();
				}
			} catch (Exception e) {
				LOGGER.error("Caught exception while attempting to play audio clip: " + e.getMessage());
			}
		}
	}
	
	public static void justPlayAudioClip(String _text, String _apikey, String _voice_model, Double _voice_stability, Double _voice_similarity, Double _voice_style, String _voice) {
		if (_apikey.length()>5) {
			byte[] audio_data = null;
			try {
				LOGGER.info(_text);
				LOGGER.info("Generating audio clip..");
				audio_data = generateElevenLabsAudioData(_text, _apikey, _voice_model, _voice_stability, _voice_similarity, _voice_style, _voice);
				if (null != audio_data) {
					LOGGER.info("Playing audio clip..");
					Player player = createPlayerFromAudioData(audio_data);
					player.play();
				} else {
					LOGGER.error("audio_data is null!?");
					SystemUtils.halt();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	public static byte[] generateElevenLabsAudioData(String _text, String _speechKeyElevelLabs, String _model, Double _stability, Double _similarity, Double _style, String _voiceID) {
		ElevenLabs.setApiKey(_speechKeyElevelLabs);
		ElevenLabs.setDefaultModel(_model);

		List<Voice> voices = null;
		boolean voice_access = false;

		while (!voice_access) {
			try {
				voices = Voice.getVoices();
				if (null != voices) {
					voice_access = true;
				} else {
					LOGGER.error("Unable to get voices from elevenlabs");
					SystemUtils.halt();
				}
			} catch (Exception e) {
				LOGGER.warn("Exception: " + e.getMessage());
				SystemUtils.sleepInSeconds(5);
			}
		}

		String voice_id = "";
		for (Voice v: voices) {
			LOGGER.debug("- name: " + v.getName() + " id: " + v.getVoiceId());
			if (v.getName().equals(_voiceID)) {
				voice_id = v.getVoiceId();
			}
		}

		if ("".equals(voice_id)) {
			LOGGER.error("Cannot find voice " + _voiceID);
			SystemUtils.halt();
		}

		Voice voice = Voice.getVoice(voice_id, true);
		VoiceSettings vs = new VoiceSettings(_stability, _similarity, _style, true);
		voice.updateVoiceSettings(vs);

		while (true) {
			try (InputStream is = voice.generateStream(_text);
					ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

				// Copy the MP3 data from the InputStream into a byte[]
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}

				return baos.toByteArray();
			} catch (Exception e) {
				if (e.getMessage().contains("Failed to process JSON")) {
					LOGGER.info("Failed to process JSON from on ElevenLabs, lets sleep 30s and retry ..");
					SystemUtils.sleepInSeconds(30);

					// ...
					// generateStream() exception: {"detail":{"status":"system_busy","message":"We are sorry, the system is experiencing heavy traffic, please try again. Higher subscriptions have higher priority."}}
					// generateStream() exception: {"detail":{"status":"quota_exceeded","message":"This request exceeds your  quota of 40000. You have 3 credits remaining, while 14 credits are required for this request."}}

				} else {
					LOGGER.info("Caught unknown exception: " + e.getMessage());
					SystemUtils.sleepInSeconds(5);
					System.exit(1);
				}
			}
		}

	}

	/**
	 * Given the MP3 data in a byte[], create a new Player object from that data.
	 */
	public static Player createPlayerFromAudioData(byte[] mp3Data) {
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(mp3Data);
			return new Player(bais);
		} catch (Exception e) {
			LOGGER.error("Error creating Player from mp3Data", e);
			return null;
		}
	}
}
