package ntt.security.ollamadrama.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.enums.InteractMethod;

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

	public static boolean getYNResponse(String _q, OllamaDramaSettings _settings) {
		System.out.println(_q);

		// ── STDIN ────────────────────────────────────────────────────────────
		if (_settings.getInteract_method() == InteractMethod.STDIN) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
			System.out.print("Proceed? [y/n]: ");
			while (true) {
				try {
					while (true) {
						String input = reader.readLine();
						if (input != null && input.matches("[yY]")) {
							return true;
						} else if (input != null && input.matches("[nN]")) {
							return false;
						}
						System.out.print("Invalid input. Proceed? [y/n]: ");
						SystemUtils.sleepInSeconds(10);
					}
				} catch (Exception e) {
					LOGGER.error("Error reading input", e);
					SystemUtils.sleepInSeconds(10);
				}
			}
		}

		// ── FILE ─────────────────────────────────────────────────────────────
		while (true) {

			if (_settings.getInteract_method() == InteractMethod.FILE) {
				String folder = _settings.getInteract_filepath();
				java.nio.file.Path yes_file = java.nio.file.Path.of(folder, "y");
				java.nio.file.Path no_file  = java.nio.file.Path.of(folder, "n");
				java.nio.file.Path q_file   = java.nio.file.Path.of(folder, "q");

				// Clean up any leftover signal files from a previous question
				try { java.nio.file.Files.deleteIfExists(yes_file); } catch (Exception ignored) {}
				try { java.nio.file.Files.deleteIfExists(no_file);  } catch (Exception ignored) {}
				try { java.nio.file.Files.deleteIfExists(q_file);   } catch (Exception ignored) {}

				// Write the question to the q file so external tools can read it
				try {
					java.nio.file.Files.writeString(q_file, _q, StandardCharsets.UTF_8);
				} catch (Exception e) {
					LOGGER.error("Could not write question to {}", q_file, e);
				}

				// ── VOICE ─────────────────────────────────────────────────────────────
				if (_settings.isQwen3tts_enable()) {
					boolean registration_success = VoiceUtils.registerQwen3Voices(_settings.getQwen3tts_url());
					if (registration_success) {
						VoiceUtils.playQwen3Sound(_settings.getQwen3tts_url(), _settings.getQwen3tts_voice(), _q);
					}
				}

				LOGGER.info("Waiting for file-based y/n response in: {}", folder);
				LOGGER.info("  touch {}/y  → yes", folder);
				LOGGER.info("  touch {}/n  → no",  folder);

				while (true) {
					try {
						if (java.nio.file.Files.exists(yes_file)) {
							java.nio.file.Files.deleteIfExists(yes_file);
							java.nio.file.Files.deleteIfExists(q_file);
							LOGGER.info("Received YES via file signal");
							return true;
						}
						if (java.nio.file.Files.exists(no_file)) {
							java.nio.file.Files.deleteIfExists(no_file);
							java.nio.file.Files.deleteIfExists(q_file);
							LOGGER.info("Received NO via file signal");
							return false;
						}
					} catch (Exception e) {
						LOGGER.error("Error checking signal files in {}", folder, e);
					}
					SystemUtils.sleepInSeconds(2);
				}

				// ── Unknown / future methods ─────────────────────────────────────────
			} else {
				LOGGER.warn("Unsupported interact method: {} — retrying in 10s", _settings.getInteract_method());
				SystemUtils.sleepInSeconds(10);
			}
		}
	}

}