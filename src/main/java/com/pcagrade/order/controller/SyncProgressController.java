package com.pcagrade.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for Server-Sent Events (SSE) to stream real-time sync progress
 */
@RestController
@RequestMapping("/api/sync/progress")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class SyncProgressController {

    // Store active SSE connections
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Stream real-time progress updates for a sync operation
     *
     * Note: SSE doesn't support Authorization headers, so we pass token as query param
     * Spring Security will validate it via SecurityConfig
     */
    @GetMapping(value = "/stream/{syncId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    //@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public SseEmitter streamProgress(
            @PathVariable String syncId,
            @RequestParam(required = false) String token) {

        log.info("📡 SSE connection request for syncId: {}", syncId);

        // Create SSE emitter with long timeout (30 minutes)
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // Register callbacks
        emitter.onCompletion(() -> {
            log.info("✅ SSE completed for syncId: {}", syncId);
            emitters.remove(syncId);
        });

        emitter.onTimeout(() -> {
            log.warn("⏱️ SSE timeout for syncId: {}", syncId);
            emitters.remove(syncId);
        });

        emitter.onError((error) -> {
            log.error("❌ SSE error for syncId: {}", syncId, error);
            emitters.remove(syncId);
        });

        // Store emitter
        emitters.put(syncId, emitter);

        // Send initial connection message
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "syncId", syncId,
                            "message", "Connected to progress stream"
                    ))
            );
        } catch (IOException e) {
            log.error("Failed to send initial SSE message", e);
        }

        log.info("✅ SSE connection established for syncId: {}", syncId);
        return emitter;
    }

    /**
     * Publish progress update to a specific sync operation
     * This method is called by the sync service
     */
    public void publishProgress(String syncId, Map<String, Object> progressData) {
        SseEmitter emitter = emitters.get(syncId);

        if (emitter == null) {
            log.debug("No emitter found for syncId: {}", syncId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(progressData)
            );

            log.debug("📊 Progress sent for syncId: {} - {}%",
                    syncId, progressData.get("percentage"));

        } catch (IOException e) {
            log.error("Failed to send progress for syncId: {}", syncId, e);
            emitter.completeWithError(e);
            emitters.remove(syncId);
        }
    }

    /**
     * Complete a sync operation stream
     */
    public void complete(String syncId, boolean success, String message) {
        SseEmitter emitter = emitters.get(syncId);

        if (emitter == null) {
            log.debug("No emitter found for syncId: {}", syncId);
            return;
        }

        try {
            // Send final progress update
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(Map.of(
                            "syncId", syncId,
                            "completed", true,
                            "success", success,
                            "message", message,
                            "percentage", 100
                    ))
            );

            // Complete the emitter
            emitter.complete();

            log.info("✅ SSE completed successfully for syncId: {}", syncId);

        } catch (IOException e) {
            log.error("Failed to complete SSE for syncId: {}", syncId, e);
            emitter.completeWithError(e);
        } finally {
            emitters.remove(syncId);
        }
    }

    /**
     * Get count of active connections (for monitoring)
     */
    @GetMapping("/active-count")
    //@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> getActiveCount() {
        return Map.of(
                "activeConnections", emitters.size(),
                "syncIds", emitters.keySet()
        );
    }
}