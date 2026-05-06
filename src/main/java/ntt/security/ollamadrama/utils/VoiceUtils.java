package ntt.security.ollamadrama.utils;

import javax.sound.sampled.*;

import ntt.security.ollamadrama.agent.qwen3.Qwen3TtsClient;

import java.nio.file.*;
import java.security.*;
import java.util.logging.*;

/**
 * Utility wrapper around Qwen3TtsClient.
 *
 * - Holds a singleton Qwen3TtsClient instance.
 * - Caches generated WAV files in "output/" named {voiceName}_{sha256_16}.wav
 * - Plays WAV files via javax.sound.sampled (no external dependencies).
 */
public class VoiceUtils {

    private static final Logger LOGGER = Logger.getLogger(VoiceUtils.class.getName());

    private static final Path VOICES_DIR = Path.of("voices");
    private static final Path OUTPUT_DIR = Path.of("output");

    // Singleton client — initialized by registerQwen3Voices
    private static Qwen3TtsClient client = null;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Scans the "voices/" folder and ensures all voices are registered with the
     * TTS server. Voices already cached from a previous run are verified and
     * skipped if still valid — no unnecessary re-registration.
     *
     * @return true if at least one voice is ready.
     */
    public static boolean registerQwen3Voices(String serverUrl) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            client = new Qwen3TtsClient(serverUrl);
            client.registerVoices(VOICES_DIR, false);

            int count = client.getRegisteredVoices().size();
            if (count == 0) {
                LOGGER.warning("No voices available. Check that voices/ contains .wav and .ref files.");
                return false;
            }
            LOGGER.info(count + " voice(s) ready: " + client.getRegisteredVoices());
            return true;

        } catch (Exception e) {
            LOGGER.severe("registerQwen3Voices failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Synthesizes and plays the given text using the named voice.
     *
     * Cache: output/{voiceName}_{sha256_16(text)}.wav
     * If the file already exists it is played directly without calling the TTS server.
     *
     * @param serverUrl  TTS server URL (only used for lazy-init if not yet registered).
     * @param voiceName  Voice name matching a file in voices/ (without extension).
     * @param text       Text to synthesize and play.
     * @return true if audio was played successfully.
     */
    public static boolean playQwen3Sound(String serverUrl, String voiceName, String text) {
        try {
            Path wavFile = getQwen3Sound(serverUrl, voiceName, text);
            if (wavFile == null) {
                return false;
            }

            LOGGER.info("Playing: " + wavFile.getFileName());
            playWav(wavFile);
            return true;

        } catch (Exception e) {
            LOGGER.severe("playQwen3Sound failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Synthesizes the given text using the named voice and returns the file path.
     * Does NOT play the audio — use playQwen3Sound for that.
     *
     * Cache: output/{voiceName}_{sha256_16(text)}.wav
     * If the file already exists it is returned directly without calling the TTS server.
     *
     * @param serverUrl  TTS server URL (only used for lazy-init if not yet registered).
     * @param voiceName  Voice name matching a file in voices/ (without extension).
     * @param text       Text to synthesize.
     * @return Path to the generated WAV file, or null on failure.
     */
    public static Path getQwen3Sound(String serverUrl, String voiceName, String text) {
        try {
            if (client == null) {
                LOGGER.warning("Client not initialized — call registerQwen3Voices first.");
                if (!registerQwen3Voices(serverUrl)) return null;
            }

            Path wavFile = getCachedPath(voiceName, text);

            if (Files.exists(wavFile)) {
                LOGGER.info("Using cached file: " + wavFile.getFileName());
            } else {
                LOGGER.info("Generating: \"" + truncate(text, 60) + "\"");
                byte[] wav = client.generate(voiceName, text, "Auto");
                Files.write(wavFile, wav);
                LOGGER.info("Generated new file: " + wavFile.getFileName());
            }

            return wavFile;

        } catch (Exception e) {
            LOGGER.severe("__getQwen3Sound__ failed: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Cache path
    // -------------------------------------------------------------------------

    /**
     * Returns the cache path for a given voice + text combination.
     * Format: output/{voiceName}_{first 16 hex chars of SHA-256(text)}.wav
     */
    public static Path getCachedPath(String voiceName, String text) throws Exception {
        String hash = sha256Hex(text).substring(0, 16);
        return OUTPUT_DIR.resolve(voiceName + "_" + hash + ".wav");
    }

    // -------------------------------------------------------------------------
    // WAV playback (javax.sound.sampled — no external deps)
    // -------------------------------------------------------------------------

    private static void playWav(Path wavFile) throws Exception {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile.toFile())) {
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                AudioFormat pcm = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        format.getSampleRate(), 16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(), false);
                try (AudioInputStream converted = AudioSystem.getAudioInputStream(pcm, ais)) {
                    playStream(converted, pcm);
                }
            } else {
                playStream(ais, format);
            }
        }
    }

    private static void playStream(AudioInputStream ais, AudioFormat format) throws Exception {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format);
            line.start();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = ais.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
            }
            line.drain();
            line.stop();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}