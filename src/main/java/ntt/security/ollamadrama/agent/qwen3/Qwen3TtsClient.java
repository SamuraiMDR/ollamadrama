package ntt.security.ollamadrama.agent.qwen3;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * Java client for the Qwen3-TTS Gradio 5.x demo.
 *
 * Named endpoints (from /gradio_api/info):
 *   /run_voice_clone      params: ref_aud, ref_txt, use_xvec, text, lang_disp
 *   /save_prompt          params: ref_aud, ref_txt, use_xvec
 *   /load_prompt_and_gen  params: file_obj, text, lang_disp
 *
 * fn_index values are resolved dynamically from /gradio_api/info at startup
 * so that each named endpoint gets the correct index.
 *
 * Requirements: Java 11+. No external dependencies.
 */
public class Qwen3TtsClient {

    private final String baseUrl;
    private final HttpClient http;

    private static final String API_PREFIX = "/gradio_api";

    // voice name → server-side prompt file path returned by /save_prompt
    private final Map<String, String> promptCache = new LinkedHashMap<>();

    // api_name → fn_index, resolved from /gradio_api/info at startup
    private final Map<String, Integer> fnIndexMap = new LinkedHashMap<>();

    // Where the prompt cache is persisted between app restarts
    private Path cacheFile = Path.of("voices", "prompt_cache.json");

    public Qwen3TtsClient(String baseUrl) {
        this.baseUrl = baseUrl.replaceAll("/$", "");
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        loadFnIndexMap();
        loadCacheFromDisk();
    }

    /** Override the default cache file location (voices/prompt_cache.json). */
    public void setCacheFile(Path path) {
        this.cacheFile = path;
    }

    // =========================================================================
    // Prompt cache persistence
    // =========================================================================

    /**
     * Loads the prompt cache from voices/prompt_cache.json.
     * Format: one "voiceName=serverPath" entry per line.
     */
    private void loadCacheFromDisk() {
        if (!Files.exists(cacheFile)) {
            System.out.println("[TTS] No prompt cache found at " + cacheFile);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(cacheFile);
            int loaded = 0;
            for (String line : lines) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String name = line.substring(0, eq).strip();
                // Unescape \\ back to single \ (Windows paths stored escaped)
                String path = line.substring(eq + 1).strip().replace("\\\\", "\\");
                promptCache.put(name, path);
                loaded++;
            }
            System.out.println("[TTS] Loaded " + loaded + " cached prompt(s) from " + cacheFile);
        } catch (Exception e) {
            System.out.println("[TTS] WARNING: could not read cache file: " + e.getMessage());
        }
    }

    /** Persists the current prompt cache to disk. */
    private void saveCacheToDisk() {
        try {
            Files.createDirectories(cacheFile.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("# Qwen3-TTS prompt cache — auto-generated, do not edit").append("\n");
            for (Map.Entry<String, String> e : promptCache.entrySet()) {
                // Escape single backslashes to \\ so Windows paths survive round-trip
                String path = e.getValue().replace("\\", "\\\\");
                sb.append(e.getKey()).append("=").append(path).append("\n");
            }
            Files.writeString(cacheFile, sb.toString());
            System.out.println("[TTS] Cache saved to " + cacheFile);
        } catch (Exception e) {
            System.out.println("[TTS] WARNING: could not save cache file: " + e.getMessage());
        }
    }

    /**
     * Checks if a server-side prompt file is still accessible.
     * Uses a HEAD request to /gradio_api/file= — fast, no download.
     */
    private boolean isPromptAliveOnServer(String serverPath) {
        try {
            // Normalise Windows backslashes (single or double) to forward slashes for URL
            String normalised = serverPath.replace("\\\\", "/").replace("\\", "/");
            String url = baseUrl + API_PREFIX + "/file=" + normalised;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // =========================================================================
    // fn_index resolution
    // =========================================================================

    /**
     * Fetches /gradio_api/info and maps each named endpoint's api_name to its
     * fn_index. Gradio assigns fn_index in declaration order starting at 0.
     * Without the correct fn_index the server silently runs the wrong function.
     */
    private void loadFnIndexMap() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + API_PREFIX + "/info"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                System.out.println("[TTS] WARNING: could not fetch /info (HTTP "
                        + resp.statusCode() + "), fn_index defaults to 0");
                return;
            }

            // The named_endpoints object keys are the api_names in declaration order.
            // We parse them in order to assign fn_index 0, 1, 2, ...
            String body = resp.body();
            int searchFrom = 0;
            int index = 0;
            String marker = "\"named_endpoints\"";
            int namedStart = body.indexOf(marker);
            if (namedStart < 0) {
                System.out.println("[TTS] WARNING: no named_endpoints in /info response");
                return;
            }
            searchFrom = namedStart;

            // Find each "/<name>":{ key inside named_endpoints
            while (true) {
                int keyStart = body.indexOf("\"/", searchFrom);
                if (keyStart < 0) break;
                int keyEnd = body.indexOf("\"", keyStart + 1);
                if (keyEnd < 0) break;
                String apiName = body.substring(keyStart + 1, keyEnd);
                // Stop if we've left named_endpoints (hit unnamed_endpoints)
                if (body.substring(searchFrom, keyStart).contains("\"unnamed_endpoints\"")) break;
                fnIndexMap.put(apiName, index++);
                System.out.println("[TTS] Mapped fn_index=" + (index-1) + " -> " + apiName);
                searchFrom = keyEnd + 1;
            }

        } catch (Exception e) {
            System.out.println("[TTS] WARNING: fn_index resolution failed: " + e.getMessage()
                    + " — defaulting all to 0");
        }
    }

    private int fnIndex(String apiName) {
        return fnIndexMap.getOrDefault(apiName, 0);
    }

    // =========================================================================
    // Voice registration
    // =========================================================================

    /**
     * Scans a directory for .wav/.ref pairs and registers each voice with the
     * TTS server via /save_prompt. Already-registered voices are skipped.
     * Call once at application startup.
     */
    public void registerVoices(Path voicesDir, boolean xVectorOnly) throws Exception {
        if (!Files.isDirectory(voicesDir)) {
            System.out.println("[TTS] Voices folder not found: " + voicesDir.toAbsolutePath());
            return;
        }

        List<Path> wavFiles = new ArrayList<>();
        try (var stream = Files.list(voicesDir)) {
            stream.filter(p -> p.toString().toLowerCase().endsWith(".wav"))
                  .sorted()
                  .forEach(wavFiles::add);
        }

        if (wavFiles.isEmpty()) {
            System.out.println("[TTS] No .wav files found in " + voicesDir.toAbsolutePath());
            return;
        }

        System.out.println("[TTS] Checking " + wavFiles.size() + " voice(s) in "
                + voicesDir.toAbsolutePath());

        boolean cacheChanged = false;
        int newlyRegistered = 0;

        for (Path wav : wavFiles) {
            String voiceName = stripExtension(wav.getFileName().toString());

            // Check if we have a cached prompt and it is still alive on the server
            if (promptCache.containsKey(voiceName)) {
                String cached = promptCache.get(voiceName);
                if (isPromptAliveOnServer(cached)) {
                    System.out.println("[TTS]   " + voiceName + " - OK (cached)");
                    continue;
                } else {
                    System.out.println("[TTS]   " + voiceName + " - prompt expired (server restarted?), re-registering...");
                    promptCache.remove(voiceName);
                    cacheChanged = true;
                }
            }

            Path refFile = wav.resolveSibling(voiceName + ".ref");
            String refText = "";
            if (Files.exists(refFile)) {
                refText = Files.readString(refFile).strip();
            } else if (!xVectorOnly) {
                System.out.println("[TTS]   " + voiceName
                        + " - WARNING: no .ref file found, falling back to x-vector only");
            }

            System.out.print("[TTS]   " + voiceName + " - registering...");
            try {
                String promptPath = savePrompt(wav, refText, xVectorOnly || refText.isEmpty());
                promptCache.put(voiceName, promptPath);
                cacheChanged = true;
                newlyRegistered++;
                System.out.println(" OK");
            } catch (Exception e) {
                System.out.println(" FAILED: " + e.getMessage());
            }
        }

        if (cacheChanged) saveCacheToDisk();

        if (newlyRegistered > 0) {
            System.out.println("[TTS] Registered " + newlyRegistered + " new voice(s). "
                    + promptCache.size() + " total ready: " + promptCache.keySet());
        } else {
            System.out.println("[TTS] All voices already registered: " + promptCache.keySet());
        }
    }

    public Set<String> getRegisteredVoices() {
        return Collections.unmodifiableSet(promptCache.keySet());
    }

    // =========================================================================
    // Primary generation (uses cached prompt)
    // =========================================================================

    public byte[] generate(String voiceName, String targetText, String language) throws Exception {
        String promptPath = promptCache.get(voiceName);
        if (promptPath == null) {
            throw new IllegalArgumentException("Voice not registered: '" + voiceName
                    + "'. Available: " + promptCache.keySet());
        }
        return generateFromPrompt(promptPath, targetText, language);
    }

    // =========================================================================
    // /save_prompt
    // =========================================================================

    public String savePrompt(Path refAudio, String refText, boolean xVectorOnly) throws Exception {
        String uploadedPath = uploadFile(refAudio);

        String dataArray = "["
                + buildFileJson(uploadedPath, refAudio.getFileName().toString()) + ","
                + jsonString(refText) + ","
                + xVectorOnly
                + "]";

        return submitAndWait("/save_prompt", dataArray, randomSessionHash());
    }

    // =========================================================================
    // /load_prompt_and_gen
    // =========================================================================

    public byte[] generateFromPrompt(
            String promptServerPath, String targetText, String language) throws Exception {

        String dataArray = "["
                + buildFileJson(promptServerPath, "voice_prompt") + ","
                + jsonString(targetText) + ","
                + jsonString(language)
                + "]";

        String outputPath = submitAndWait("/load_prompt_and_gen", dataArray, randomSessionHash());
        return downloadFile(outputPath);
    }

    // =========================================================================
    // /run_voice_clone  (one-shot, no caching)
    // =========================================================================

    public byte[] generateVoiceClone(
            String targetText, String language,
            Path refAudio, String refText, boolean xVectorOnly) throws Exception {

        System.out.println("[TTS] Uploading: " + refAudio.getFileName());
        String uploadedPath = uploadFile(refAudio);
        System.out.println("[TTS] Uploaded -> " + uploadedPath);

        String dataArray = "["
                + buildFileJson(uploadedPath, refAudio.getFileName().toString()) + ","
                + jsonString(refText) + ","
                + xVectorOnly + ","
                + jsonString(targetText) + ","
                + jsonString(language)
                + "]";

        System.out.println("[TTS] Generating (one-shot)...");
        String outputPath = submitAndWait("/run_voice_clone", dataArray, randomSessionHash());
        System.out.println("[TTS] Done -> " + outputPath);
        return downloadFile(outputPath);
    }

    // =========================================================================
    // Upload
    // =========================================================================

    private String uploadFile(Path file) throws Exception {
        String boundary = "----GradioBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] fileBytes = Files.readAllBytes(file);
        String filename  = file.getFileName().toString();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String partHeader = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"files\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + guessMimeType(filename) + "\r\n\r\n";
        body.write(partHeader.getBytes());
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + API_PREFIX + "/upload"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Upload failed HTTP " + resp.statusCode() + ": " + resp.body());
        }
        return parseUploadResponse(resp.body());
    }

    private String parseUploadResponse(String json) {
        int pathIdx = json.indexOf("\"path\"");
        if (pathIdx >= 0) {
            int colon = json.indexOf(':', pathIdx);
            int q1    = json.indexOf('"', colon + 1);
            int q2    = findClosingQuote(json, q1 + 1);
            return json.substring(q1 + 1, q2);
        }
        return extractFirstJsonString(json);
    }

    // =========================================================================
    // Queue + SSE
    // =========================================================================

    private String submitAndWait(String apiName, String dataArray, String sessionHash)
            throws Exception {

        int idx = fnIndex(apiName);
        String joinPayload = "{"
                + "\"fn_index\":" + idx + ","
                + "\"api_name\":\"" + apiName + "\","
                + "\"data\":" + dataArray + ","
                + "\"session_hash\":\"" + sessionHash + "\""
                + "}";

        System.out.println("[TTS]   -> " + apiName + " (fn_index=" + idx + ")");

        HttpRequest joinReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + API_PREFIX + "/queue/join"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(joinPayload))
                .build();

        HttpResponse<String> joinResp = http.send(joinReq, HttpResponse.BodyHandlers.ofString());
        if (joinResp.statusCode() != 200) {
            throw new RuntimeException("Queue join failed HTTP "
                    + joinResp.statusCode() + ": " + joinResp.body());
        }

        String sseUrl = baseUrl + API_PREFIX + "/queue/data?session_hash=" + sessionHash;

        HttpRequest sseReq = HttpRequest.newBuilder()
                .uri(URI.create(sseUrl))
                .timeout(Duration.ofMinutes(10))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();

        HttpResponse<InputStream> sseResp = http.send(sseReq,
                HttpResponse.BodyHandlers.ofInputStream());
        if (sseResp.statusCode() != 200) {
            throw new RuntimeException("SSE stream failed HTTP " + sseResp.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(sseResp.body()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.isEmpty()) continue;

                String msg = extractJsonString(data, "msg");
                System.out.println("[TTS]     " + (msg != null ? msg : truncate(data, 80)));

                if ("process_completed".equals(msg)) {
                    // success:false means the server-side function threw an exception
                    if (data.contains("\"success\":false")) {
                        String err = extractJsonString(data, "error");
                        throw new RuntimeException("Server function failed: "
                                + (err != null && !err.equals("null") ? err : "(no error message) " + truncate(data, 200)));
                    }
                    return extractOutputPath(data);
                }
            }
        }
        throw new RuntimeException("SSE stream ended without process_completed.");
    }

    // =========================================================================
    // Download
    // =========================================================================

    private byte[] downloadFile(String serverPath) throws Exception {
        serverPath = serverPath.replace("\\\\", "/").replace("\\", "/");

        String[] urls = {
                baseUrl + API_PREFIX + "/file=" + serverPath,
                baseUrl + "/file=" + serverPath
        };
        for (String url : urls) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() == 200) {
                System.out.println("[TTS]     downloaded " + resp.body().length + " bytes");
                return resp.body();
            }
        }
        throw new RuntimeException("Could not download audio: " + serverPath);
    }

    // =========================================================================
    // JSON helpers
    // =========================================================================

    private String buildFileJson(String serverPath, String origName) {
        return "{\"path\":\"" + escapeJson(serverPath) + "\","
                + "\"orig_name\":\"" + escapeJson(origName) + "\","
                + "\"meta\":{\"_type\":\"gradio.FileData\"}}";
    }

    private String jsonString(String s) {
        return "\"" + escapeJson(s) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = findClosingQuote(json, q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private String extractOutputPath(String json) {
        int outputIdx = json.indexOf("\"output\"");
        if (outputIdx < 0) throw new RuntimeException("No 'output' in: " + truncate(json, 200));
        String afterOutput = json.substring(outputIdx);

        int pathIdx = afterOutput.indexOf("\"path\"");
        if (pathIdx >= 0) {
            int colon = afterOutput.indexOf(':', pathIdx);
            int q1    = afterOutput.indexOf('"', colon + 1);
            int q2    = findClosingQuote(afterOutput, q1 + 1);
            String path = afterOutput.substring(q1 + 1, q2);
            if (!path.isEmpty()) return path;
        }
        int urlIdx = afterOutput.indexOf("\"url\"");
        if (urlIdx >= 0) {
            int colon = afterOutput.indexOf(':', urlIdx);
            int q1    = afterOutput.indexOf('"', colon + 1);
            int q2    = findClosingQuote(afterOutput, q1 + 1);
            String url = afterOutput.substring(q1 + 1, q2);
            if (url.contains("/file=")) return url.substring(url.indexOf("/file=") + 6);
            if (!url.isEmpty()) return url;
        }
        throw new RuntimeException("Could not find output path in: " + truncate(json, 300));
    }

    private String extractFirstJsonString(String json) {
        int start = json.indexOf('"');
        if (start < 0) throw new RuntimeException("No string in: " + json);
        int end = findClosingQuote(json, start + 1);
        if (end < 0) throw new RuntimeException("Unterminated string in: " + json);
        return json.substring(start + 1, end);
    }

    private int findClosingQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) return i;
        }
        return -1;
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String randomSessionHash() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String guessMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".wav"))  return "audio/wav";
        if (lower.endsWith(".mp3"))  return "audio/mpeg";
        if (lower.endsWith(".flac")) return "audio/flac";
        return "application/octet-stream";
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}