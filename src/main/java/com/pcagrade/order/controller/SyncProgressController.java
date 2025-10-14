package com.pcagrade.order.controller;

import com.pcagrade.order.model.SyncProgress;
import com.pcagrade.order.service.SyncProgressPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Controller for Server-Sent Events (SSE) sync progress streaming
 *
 * Provides real-time progress updates for synchronization operations
 */
@Slf4j
@RestController
@RequestMapping("/api/sync/progress")
@CrossOrigin(origins = "*") // Allow CORS for SSE
public class SyncProgressController {

    private final SyncProgressPublisher progressPublisher;

    public SyncProgressController(SyncProgressPublisher progressPublisher) {
        this.progressPublisher = progressPublisher;
    }

    /**
     * Stream sync progress events via Server-Sent Events
     *
     * GET /api/sync/progress/stream/{syncId}
     *
     * @param syncId The unique sync session ID
     * @return Flux stream of ServerSentEvent containing SyncProgress
     */
    @GetMapping(value = "/stream/{syncId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<SyncProgress>> streamProgress(@PathVariable String syncId) {
        log.info("📡 Client connected to SSE stream for sync: {}", syncId);

        return progressPublisher.createSyncSession(syncId)
                .filter(progress -> !"KEEP_ALIVE".equals(progress.getPhase())) // Filter out keep-alive events from data stream
                .map(progress -> ServerSentEvent.<SyncProgress>builder()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .event("progress")
                        .data(progress)
                        .build())
                .doOnSubscribe(subscription ->
                        log.info("✅ SSE stream started for sync: {}", syncId))
                .doOnComplete(() ->
                        log.info("✅ SSE stream completed for sync: {}", syncId))
                .doOnCancel(() ->
                        log.info("🔌 SSE stream cancelled for sync: {}", syncId))
                .doOnError(error ->
                        log.error("❌ SSE stream error for sync {}: {}", syncId, error.getMessage()));
    }

    /**
     * Check if a sync session is active
     *
     * GET /api/sync/progress/status/{syncId}
     *
     * @param syncId The sync session ID
     * @return Status of the sync session
     */
    @GetMapping("/status/{syncId}")
    public SyncSessionStatus getSessionStatus(@PathVariable String syncId) {
        boolean active = progressPublisher.isSessionActive(syncId);

        return new SyncSessionStatus(
                syncId,
                active,
                active ? "ACTIVE" : "NOT_FOUND"
        );
    }

    /**
     * Get statistics about active sync sessions
     *
     * GET /api/sync/progress/stats
     *
     * @return Statistics about sync sessions
     */
    @GetMapping("/stats")
    public SyncStats getStats() {
        int activeCount = progressPublisher.getActiveSessionCount();

        return new SyncStats(
                activeCount,
                activeCount > 0 ? "SYNCING" : "IDLE"
        );
    }

    // DTOs for responses

    public record SyncSessionStatus(
            String syncId,
            boolean active,
            String status
    ) {}

    public record SyncStats(
            int activeSessions,
            String systemStatus
    ) {}
}