package ntt.security.ollamadrama.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
 
/**
 * File-based distributed lock that prevents multiple agents from sending
 * requests to Ollama simultaneously.
 *
 * <p>All agents mount the same host directory (e.g. {@code ./ollama_lock:/ollama_lock}).
 * Before sending any request to Ollama, call {@link #acquire()} and release with
 * {@link #release()} in a finally block.
 *
 * <h3>Lock file format</h3>
 * <pre>
 *   persona=<agentname>
 *   acquired=2026-03-24T09:04:39Z
 * </pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * OllamaLock lock = new OllamaLock(Path.of("/ollama_lock"), "<agentname>");
 * lock.acquire();
 * try {
 *     // send to Ollama
 * } finally {
 *     lock.release();
 * }
 * }</pre>
 */
public class OllamaLock {
 
    private static final Logger log         = LoggerFactory.getLogger(OllamaLock.class);
    private static final String LOCK_FILE   = "lock";
    private static final long   POLL_MS     = 2_000;   // check every 2 seconds
    private static final long   STALE_MS    = 120 * 60 * 1000; // 120 min = stale lock
 
    private final Path   lock_file;
    private final String persona;
 
    /**
     * @param lock_dir Directory shared across all NPC containers (e.g. {@code /ollama_lock}).
     * @param persona  Name of this NPC — written into the lock file for diagnostics.
     */
    public OllamaLock(Path lock_dir, String persona) {
        this.lock_file = lock_dir.resolve(LOCK_FILE);
        this.persona   = persona;
 
        try {
            Files.createDirectories(lock_dir);
        } catch (IOException e) {
            log.warn("Could not create lock directory: {}", lock_dir, e);
        }
    }
 
    /**
     * Block until the lock is acquired. Polls every 2 seconds.
     * Automatically clears stale locks older than 120 minutes.
     */
    public void acquire() {
        log.debug("Waiting for Ollama lock (persona={})", persona);
        while (true) {
            clear_if_stale();
            if (try_acquire()) {
                log.debug("Ollama lock acquired (persona={})", persona);
                return;
            }
            log.debug("Ollama lock held by another NPC, waiting {}ms...", POLL_MS);
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
 
    /**
     * Release the lock. Should always be called in a finally block.
     * Only deletes the lock file if it is still owned by this persona.
     */
    public void release() {
        try {
            if (!Files.exists(lock_file)) return;
            String content = Files.readString(lock_file, StandardCharsets.UTF_8);
            if (content.contains("persona=" + persona)) {
                Files.deleteIfExists(lock_file);
                log.debug("Ollama lock released (persona={})", persona);
            } else {
                log.warn("Lock file not owned by us (persona={}), not deleting", persona);
            }
        } catch (IOException e) {
            log.warn("Failed to release Ollama lock", e);
        }
    }
 
    // ── Internals ─────────────────────────────────────────────────────────────
 
    private boolean try_acquire() {
        try {
            // ATOMIC: CREATE_NEW fails if file already exists — no race condition
            String content = "persona=" + persona + "\nacquired=" + Instant.now() + "\n";
            Files.writeString(lock_file, content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            return true;
        } catch (java.nio.file.FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            log.warn("Error trying to acquire lock", e);
            return false;
        }
    }
 
    private void clear_if_stale() {
        try {
            if (!Files.exists(lock_file)) return;
            long age_ms = System.currentTimeMillis()
                    - Files.getLastModifiedTime(lock_file).toMillis();
            if (age_ms > STALE_MS) {
                String content = Files.readString(lock_file, StandardCharsets.UTF_8);
                log.warn("Clearing stale Ollama lock (age={}min, content={})",
                        age_ms / 60_000, content.trim());
                Files.deleteIfExists(lock_file);
            }
        } catch (IOException e) {
            log.warn("Error checking stale lock", e);
        }
    }
}