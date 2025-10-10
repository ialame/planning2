package com.pcagrade.order.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Utility class for Planning operations
 * Contains helper methods for mapping and validation
 */
public final class PlanningUtils {

    private static final Logger log = LoggerFactory.getLogger(PlanningUtils.class);

    // Valid delai codes
    private static final Set<String> VALID_DELAI_CODES = Set.of("X", "F+", "F", "C", "E");

    // Valid status codes
    private static final Set<Integer> VALID_STATUS_CODES = Set.of(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 41, 42
    );

    // Default values
    public static final String DEFAULT_DELAI = "C";
    public static final int DEFAULT_STATUS = 2; // A_NOTER
    public static final int MINUTES_PER_CARD = 3;

    // Private constructor to prevent instantiation
    private PlanningUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== DELAI MAPPING ==========

    /**
     * Get delai code from order data
     * @param order Order data as Map
     * @return Valid delai code (X, F+, F, C, E)
     */
    public static String getDelaiForPlanning(Map<String, Object> order) {
        if (order == null) {
            log.warn("⚠️ Null order provided, using default delai '{}'", DEFAULT_DELAI);
            return DEFAULT_DELAI;
        }

        String orderDelai = (String) order.get("delai");
        return validateDelai(orderDelai);
    }

    /**
     * Validate and normalize delai code
     * @param delai Delai code to validate
     * @return Valid delai code or default
     */
    public static String validateDelai(String delai) {
        if (delai == null || delai.trim().isEmpty()) {
            log.debug("Empty delai provided, using default '{}'", DEFAULT_DELAI);
            return DEFAULT_DELAI;
        }

        String normalized = delai.trim().toUpperCase();

        if (VALID_DELAI_CODES.contains(normalized)) {
            return normalized;
        }

        log.warn("⚠️ Invalid delai '{}', using default '{}'", delai, DEFAULT_DELAI);
        return DEFAULT_DELAI;
    }

    /**
     * Check if delai code is valid
     */
    public static boolean isValidDelai(String delai) {
        return delai != null && VALID_DELAI_CODES.contains(delai.trim().toUpperCase());
    }

    // ========== STATUS MAPPING ==========

    /**
     * Get status code from order data
     * @param order Order data as Map
     * @return Valid status code
     */
    public static int getStatusForPlanning(Map<String, Object> order) {
        if (order == null) {
            return DEFAULT_STATUS;
        }

        Object statusObj = order.get("status");
        if (statusObj instanceof Integer) {
            return validateStatus((Integer) statusObj);
        }

        return DEFAULT_STATUS;
    }

    /**
     * Validate status code
     * @param status Status code to validate
     * @return Valid status code or default
     */
    public static int validateStatus(Integer status) {
        if (status == null) {
            return DEFAULT_STATUS;
        }

        if (VALID_STATUS_CODES.contains(status)) {
            return status;
        }

        log.warn("⚠️ Invalid status '{}', using default '{}'", status, DEFAULT_STATUS);
        return DEFAULT_STATUS;
    }

    /**
     * Check if status code is valid
     */
    public static boolean isValidStatus(Integer status) {
        return status != null && VALID_STATUS_CODES.contains(status);
    }

    /**
     * Check if status is a planning stage (work to be done)
     */
    public static boolean isPlanningStage(Integer status) {
        if (status == null) return false;
        // Status values: 2, 3, 4, 6, 7, 10, 11
        return status == 2 || status == 3 || status == 4 ||
                status == 6 || status == 7 || status == 10 || status == 11;
    }

    // ========== DURATION CALCULATION ==========

    /**
     * Calculate duration in minutes based on card count
     * @param cardCount Number of cards
     * @return Duration in minutes
     */
    public static int calculateDuration(int cardCount) {
        return calculateDuration(cardCount, MINUTES_PER_CARD);
    }

    /**
     * Calculate duration with custom time per card
     * @param cardCount Number of cards
     * @param minutesPerCard Time per card in minutes
     * @return Duration in minutes
     */
    public static int calculateDuration(int cardCount, int minutesPerCard) {
        if (cardCount < 0) {
            log.warn("⚠️ Negative card count {}, using 0", cardCount);
            cardCount = 0;
        }
        if (minutesPerCard < 1) {
            log.warn("⚠️ Invalid minutesPerCard {}, using default {}",
                    minutesPerCard, MINUTES_PER_CARD);
            minutesPerCard = MINUTES_PER_CARD;
        }
        return cardCount * minutesPerCard;
    }

    /**
     * Get card count from order data
     */
    public static int getCardCount(Map<String, Object> order) {
        if (order == null) {
            return 0;
        }

        Object cardCountObj = order.get("cardCount");
        if (cardCountObj instanceof Integer) {
            return Math.max(0, (Integer) cardCountObj);
        }

        // Try other possible field names
        cardCountObj = order.get("nombreCartes");
        if (cardCountObj instanceof Integer) {
            return Math.max(0, (Integer) cardCountObj);
        }

        cardCountObj = order.get("card_count");
        if (cardCountObj instanceof Integer) {
            return Math.max(0, (Integer) cardCountObj);
        }

        log.warn("⚠️ No card count found in order, using 0");
        return 0;
    }

    // ========== PRIORITY LEVEL ==========

    /**
     * Get priority level from delai code (1=highest, 5=lowest)
     * @param delai Delai code
     * @return Priority level
     */
    public static int getPriorityLevel(String delai) {
        if (delai == null) return 4;

        return switch (delai.toUpperCase()) {
            case "X" -> 1;      // EXCELSIOR
            case "F+" -> 2;     // FAST_PLUS
            case "F" -> 3;      // FAST
            case "C" -> 4;      // CLASSIC
            case "E" -> 5;      // ECONOMY
            default -> 4;
        };
    }

    /**
     * Get display name for delai code
     */
    public static String getDelaiDisplayName(String delai) {
        if (delai == null) return "Classic";

        return switch (delai.toUpperCase()) {
            case "X" -> "Excelsior";
            case "F+" -> "Fast Plus";
            case "F" -> "Fast";
            case "C" -> "Classic";
            case "E" -> "Economy";
            default -> "Classic";
        };
    }

    /**
     * Get display name for status code
     */
    public static String getStatusDisplayName(Integer status) {
        if (status == null) return "Unknown";

        return switch (status) {
            case 1 -> "To be received";
            case 2 -> "To be graded";
            case 3 -> "To be encapsulated";
            case 4 -> "To be prepared";
            case 5 -> "Sent";
            case 6 -> "To be checked";
            case 7 -> "To be unsealed";
            case 8 -> "Received";
            case 9 -> "Package accepted";
            case 10 -> "To be scanned";
            case 11 -> "To be opened";
            case 41 -> "To be delivered";
            case 42 -> "To be sent";
            default -> "Unknown";
        };
    }

    // ========== VALIDATION ==========

    /**
     * Validate planning data before saving
     * @param orderId Order ID
     * @param employeeId Employee ID
     * @param cardCount Card count
     * @param delai Delai code
     * @param status Status code
     * @return true if valid
     */
    public static boolean validatePlanningData(
            String orderId,
            String employeeId,
            int cardCount,
            String delai,
            int status
    ) {
        if (orderId == null || orderId.trim().isEmpty()) {
            log.error("❌ Invalid planning: orderId is null or empty");
            return false;
        }

        if (employeeId == null || employeeId.trim().isEmpty()) {
            log.error("❌ Invalid planning: employeeId is null or empty");
            return false;
        }

        if (cardCount < 0) {
            log.error("❌ Invalid planning: cardCount is negative");
            return false;
        }

        if (!isValidDelai(delai)) {
            log.error("❌ Invalid planning: delai '{}' is not valid", delai);
            return false;
        }

        if (!isValidStatus(status)) {
            log.error("❌ Invalid planning: status '{}' is not valid", status);
            return false;
        }

        return true;
    }
}