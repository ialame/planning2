package com.pcagrade.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model representing sync progress events
 * Used for Server-Sent Events (SSE) to track synchronization progress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncProgress {

    /**
     * Unique ID for this sync session
     */
    private String syncId;

    /**
     * Type of sync operation: ALL, ORDERS, CARDS, INCREMENTAL
     */
    private String operationType;

    /**
     * Current operation being performed
     * Examples: "Fetching orders from API", "Processing cards batch 1/10"
     */
    private String currentOperation;

    /**
     * Progress percentage (0-100)
     */
    private int percentage;

    /**
     * Number of items processed so far
     */
    private int itemsProcessed;

    /**
     * Total number of items to process
     */
    private int totalItems;

    /**
     * Current phase of the operation
     * Examples: "FETCHING", "PROCESSING", "SAVING", "COMPLETED", "ERROR"
     */
    private String phase;

    /**
     * Status message for the user
     */
    private String message;

    /**
     * Timestamp of this progress update
     */
    private LocalDateTime timestamp;

    /**
     * Whether the sync is completed
     */
    private boolean completed;

    /**
     * Whether there was an error
     */
    private boolean error;

    /**
     * Error message if any
     */
    private String errorMessage;

    /**
     * Estimated time remaining in seconds (optional)
     */
    private Integer estimatedSecondsRemaining;

    // Factory methods for common progress events

    public static SyncProgress starting(String syncId, String operationType, String message) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation(message);
        progress.setPhase("STARTING");
        progress.setPercentage(0);
        progress.setMessage(message);
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(false);
        progress.setError(false);
        return progress;
    }

    public static SyncProgress fetching(String syncId, String operationType, String message) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation(message);
        progress.setPhase("FETCHING");
        progress.setPercentage(5);
        progress.setMessage(message);
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(false);
        progress.setError(false);
        return progress;
    }

    public static SyncProgress processing(String syncId, String operationType, String message,
                                          int processed, int total) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation(message);
        progress.setPhase("PROCESSING");
        progress.setItemsProcessed(processed);
        progress.setTotalItems(total);

        // Calculate percentage (10% for fetching, 80% for processing, 10% for completion)
        int processingPercentage = total > 0 ? (int) ((processed * 80.0) / total) : 0;
        progress.setPercentage(10 + processingPercentage);

        progress.setMessage(String.format("%s (%d/%d)", message, processed, total));
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(false);
        progress.setError(false);

        // Estimate remaining time
        if (processed > 0 && total > processed) {
            // Rough estimate: assume constant rate
            progress.setEstimatedSecondsRemaining((total - processed) / Math.max(1, processed / 10));
        }

        return progress;
    }

    public static SyncProgress saving(String syncId, String operationType, String message) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation(message);
        progress.setPhase("SAVING");
        progress.setPercentage(95);
        progress.setMessage(message);
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(false);
        progress.setError(false);
        return progress;
    }

    public static SyncProgress completed(String syncId, String operationType, String message,
                                         int totalProcessed) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation("Completed");
        progress.setPhase("COMPLETED");
        progress.setPercentage(100);
        progress.setItemsProcessed(totalProcessed);
        progress.setTotalItems(totalProcessed);
        progress.setMessage(message);
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(true);
        progress.setError(false);
        return progress;
    }

    public static SyncProgress error(String syncId, String operationType, String errorMessage) {
        SyncProgress progress = new SyncProgress();
        progress.setSyncId(syncId);
        progress.setOperationType(operationType);
        progress.setCurrentOperation("Error occurred");
        progress.setPhase("ERROR");
        progress.setMessage("Synchronization failed");
        progress.setErrorMessage(errorMessage);
        progress.setTimestamp(LocalDateTime.now());
        progress.setCompleted(true);
        progress.setError(true);
        return progress;
    }
}