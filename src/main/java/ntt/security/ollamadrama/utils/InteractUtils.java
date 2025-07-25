package ntt.security.ollamadrama.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.OllamaDramaSettings;

public class InteractUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(InteractUtils.class);

	/**
	 * Prompts the user for input and returns it in lowercase.
	 * Returns an empty string if input is null.
	 */
	public static String getReturn() {
		LOGGER.info("Proceed?");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			String input = reader.readLine();
			return input != null ? input.toLowerCase() : "";
		} catch (Exception e) {
			LOGGER.error("Error reading input", e);
			return "";
		}
	}

	/**
	 * Prompts the user for a 'y' or 'n' response and returns true for 'y' and false for 'n'.
	 * Loops until a valid input is received.
	 * @param settings 
	 */
	public static boolean getYNResponse(String _q, OllamaDramaSettings _settings) {

		System.out.println(_q);
		
		if (_settings.getElevenlabs_apikey().length()>0) {
			String elevenlabsapikey = _settings.getElevenlabs_apikey();
			String voice1 = _settings.getElevenlabs_voice1();
			SpeechUtils.justPlayAudioClipWithDefaults(_q, elevenlabsapikey, voice1);
		}

		System.out.print("Proceed? [y/n]: ");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			while (true) {
				String input = reader.readLine();
				if (input != null && input.matches("[yY]")) {
					return true;
				} else if (input != null && input.matches("[nN]")) {
					return false;
				}
				System.out.print("Invalid input. Proceed? [y/n]: ");
			}
		} catch (Exception e) {
			LOGGER.error("Error reading input", e);
			return false; // Default to false on error
		}
	}

}