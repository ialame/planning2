package com.pcagrade.order.service;

import com.pcagrade.order.model.SyncProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to publish sync progress events via Server-Sent Events (SSE)
 * Uses Reactor's Sinks to create a non-blocking event stream
 */
@Slf4j
@Service
public class SyncProgressPublisher {

    // Map to store active sync sessions
    // Key: syncId, Value: Sink for that sync session
    private final Map<String, Sinks.Many<SyncProgress>> activeSyncs = new ConcurrentHashMap<>();

    // Keep-alive interval for SSE connections
    private static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(15);

    /**
     * Create a new sync session and return a Flux stream for it
     *
     * @param syncId Unique identifier for this sync session
     * @return Flux stream of SyncProgress events
     */
    public Flux<SyncProgress> createSyncSession(String syncId) {
        log.info("📡 Creating SSE stream for sync session: {}", syncId);

        // Create a multicast sink that can have multiple subscribers
        Sinks.Many<SyncProgress> sink = Sinks.many().multicast().onBackpressureBuffer();

        // Store the sink
        activeSyncs.put(syncId, sink);

        // Return a Flux that:
        // 1. Emits progress events from the sink
        // 2. Sends keep-alive events every 15 seconds
        // 3. Cleans up when completed or cancelled
        return sink.asFlux()
                .mergeWith(Flux.interval(KEEP_ALIVE_INTERVAL)
                        .map(i -> createKeepAlive(syncId)))
                .doOnCancel(() -> {
                    log.info("🔌 Client disconnected from sync session: {}", syncId);
                    cleanupSession(syncId);
                })
                .doOnComplete(() -> {
                    log.info("✅ Sync session completed: {}", syncId);
                    cleanupSession(syncId);
                })
                .doOnError(error -> {
                    log.error("❌ Error in sync session {}: {}", syncId, error.getMessage());
                    cleanupSession(syncId);
                });
    }

    /**
     * Publish a progress update for a sync session
     *
     * @param syncId The sync session ID
     * @param progress The progress update to publish
     */
    public void publishProgress(String syncId, SyncProgress progress) {
        Sinks.Many<SyncProgress> sink = activeSyncs.get(syncId);

        if (sink != null) {
            Sinks.EmitResult result = sink.tryEmitNext(progress);

            if (result.isFailure()) {
                log.warn("⚠️ Failed to emit progress for sync {}: {}", syncId, result);
            } else {
                log.debug("📊 Published progress for sync {}: {}% - {}",
                        syncId, progress.getPercentage(), progress.getMessage());
            }

            // If sync is completed or errored, complete the sink
            if (progress.isCompleted()) {
                sink.tryEmitComplete();
            }
        } else {
            log.warn("⚠️ No active sync session found for ID: {}", syncId);
        }
    }

    /**
     * Publish an error for a sync session
     *
     * @param syncId The sync session ID
     * @param errorMessage The error message
     */
    public void publishError(String syncId, String errorMessage) {
        Sinks.Many<SyncProgress> sink = activeSyncs.get(syncId);

        if (sink != null) {
            // Create error progress
            SyncProgress errorProgress = SyncProgress.error(syncId, "UNKNOWN", errorMessage);
            sink.tryEmitNext(errorProgress);
            sink.tryEmitComplete();
        }

        cleanupSession(syncId);
    }

    /**
     * Complete a sync session
     *
     * @param syncId The sync session ID
     */
    public void completeSession(String syncId) {
        Sinks.Many<SyncProgress> sink = activeSyncs.get(syncId);

        if (sink != null) {
            sink.tryEmitComplete();
        }

        cleanupSession(syncId);
    }

    /**
     * Check if a sync session is active
     *
     * @param syncId The sync session ID
     * @return true if the session exists
     */
    public boolean isSessionActive(String syncId) {
        return activeSyncs.containsKey(syncId);
    }

    /**
     * Get the number of active sync sessions
     *
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSyncs.size();
    }

    /**
     * Clean up a sync session
     *
     * @param syncId The sync session ID to clean up
     */
    private void cleanupSession(String syncId) {
        Sinks.Many<SyncProgress> removed = activeSyncs.remove(syncId);

        if (removed != null) {
            log.info("🧹 Cleaned up sync session: {}", syncId);
        }
    }

    /**
     * Create a keep-alive event to prevent SSE timeout
     *
     * @param syncId The sync session ID
     * @return A keep-alive progress event
     */
    private SyncProgress createKeepAlive(String syncId) {
        SyncProgress keepAlive = new SyncProgress();
        keepAlive.setSyncId(syncId);
        keepAlive.setPhase("KEEP_ALIVE");
        keepAlive.setMessage("Connection alive");
        return keepAlive;
    }

    /**
     * Clean up all sessions (for shutdown)
     */
    public void cleanupAllSessions() {
        log.info("🧹 Cleaning up all sync sessions: {}", activeSyncs.size());
        activeSyncs.keySet().forEach(this::completeSession);
        activeSyncs.clear();
    }
}