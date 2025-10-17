package com.pcagrade.order.constants;

/**
 * Order Status Constants
 * Matches the frontend TypeScript constants in ORDER_STATUS
 * These values represent the actual workflow stages for Pokemon card orders
 */
public final class OrderStatusConstants {

    private OrderStatusConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========== ORDER STATUS VALUES ==========

    /**
     * 1 - A_RECEPTIONNER - To be received
     * Order package is waiting to be received
     */
    public static final int A_RECEPTIONNER = 1;

    /**
     * 9 - COLIS_ACCEPTE - Package accepted
     * The package has been accepted and logged into the system
     */
    public static final int COLIS_ACCEPTE = 9;

    /**
     * 10 - A_SCANNER - To be scanned
     * Cards need to be scanned into the system
     */
    public static final int A_SCANNER = 10;

    /**
     * 11 - A_OUVRIR - To be opened
     * Package needs to be opened
     */
    public static final int A_OUVRIR = 11;

    /**
     * 2 - A_NOTER - To be graded/evaluated
     * Cards need to be graded by noteurs (graders)
     * This is the DEFAULT status for new planning entries
     */
    public static final int A_NOTER = 2;

    /**
     * 3 - A_CERTIFIER - To be encapsulated/certified
     * Cards need to be encapsulated by certificateurs
     */
    public static final int A_CERTIFIER = 3;

    /**
     * 4 - A_PREPARER - To be prepared
     * Order needs to be prepared for shipping
     */
    public static final int A_PREPARER = 4;

    /**
     * 7 - A_DESCELLER - To be unsealed
     * Cards need to be unsealed from their protective cases
     */
    public static final int A_DESCELLER = 7;

    /**
     * 6 - A_VOIR - To be checked/reviewed
     * Order needs special attention or review
     */
    public static final int A_VOIR = 6;

    /**
     * 41 - A_DISTRIBUER - To be delivered
     * Order is ready to be delivered locally
     */
    public static final int A_DISTRIBUER = 41;

    /**
     * 42 - A_ENVOYER - To be sent
     * Order is ready to be sent via mail/courier
     */
    public static final int A_ENVOYER = 42;

    /**
     * 5 - ENVOYEE - Sent
     * Order has been sent to the customer
     */
    public static final int ENVOYEE = 5;

    /**
     * 8 - RECU - Received
     * Customer has received the order
     */
    public static final int RECU = 8;


    // ========== STATUS GROUPS ==========

    /**
     * Planning stages - statuses where work needs to be planned/scheduled
     * These are the active work stages for employees
     */
    public static final int[] PLANNING_STAGES = {
            A_SCANNER,      // 10
            A_OUVRIR,       // 11
            A_NOTER,        // 2
            A_CERTIFIER,    // 3
            A_PREPARER,     // 4
            A_DESCELLER,    // 7
            A_VOIR          // 6
    };

    /**
     * Pre-processing stages - before actual work begins
     */
    public static final int[] PRE_PROCESSING_STAGES = {
            A_RECEPTIONNER, // 1
            COLIS_ACCEPTE   // 9
    };

    /**
     * Post-processing stages - after work is complete
     */
    public static final int[] POST_PROCESSING_STAGES = {
            A_DISTRIBUER,   // 41
            A_ENVOYER,      // 42
            ENVOYEE,        // 5
            RECU            // 8
    };

    /**
     * Final/completed stages - order lifecycle is complete
     */
    public static final int[] COMPLETED_STAGES = {
            ENVOYEE,        // 5
            RECU            // 8
    };


    // ========== HELPER METHODS ==========

    /**
     * Check if a status is a planning stage (work to be done)
     */
    public static boolean isPlanningStage(int status) {
        for (int planningStatus : PLANNING_STAGES) {
            if (status == planningStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a status is a pre-processing stage
     */
    public static boolean isPreProcessingStage(int status) {
        for (int preStatus : PRE_PROCESSING_STAGES) {
            if (status == preStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a status is a post-processing stage
     */
    public static boolean isPostProcessingStage(int status) {
        for (int postStatus : POST_PROCESSING_STAGES) {
            if (status == postStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a status represents a completed order
     */
    public static boolean isCompleted(int status) {
        for (int completedStatus : COMPLETED_STAGES) {
            if (status == completedStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get display name for status (English)
     */
    public static String getDisplayNameEn(int status) {
        return switch (status) {
            case A_RECEPTIONNER -> "To be received";
            case COLIS_ACCEPTE -> "Package accepted";
            case A_SCANNER -> "To be scanned";
            case A_OUVRIR -> "To be opened";
            case A_NOTER -> "To be graded";
            case A_CERTIFIER -> "To be encapsulated";
            case A_PREPARER -> "To be prepared";
            case A_DESCELLER -> "To be unsealed";
            case A_VOIR -> "To be checked";
            case A_DISTRIBUER -> "To be delivered";
            case A_ENVOYER -> "To be sent";
            case ENVOYEE -> "Sent";
            case RECU -> "Received";
            default -> "Unknown";
        };
    }

    /**
     * Get display name for status (French)
     */
    public static String getDisplayNameFr(int status) {
        return switch (status) {
            case A_RECEPTIONNER -> "À réceptionner";
            case COLIS_ACCEPTE -> "Colis accepté";
            case A_SCANNER -> "À scanner";
            case A_OUVRIR -> "À ouvrir";
            case A_NOTER -> "À noter";
            case A_CERTIFIER -> "À certifier";
            case A_PREPARER -> "À préparer";
            case A_DESCELLER -> "À desceller";
            case A_VOIR -> "À voir";
            case A_DISTRIBUER -> "À distribuer";
            case A_ENVOYER -> "À envoyer";
            case ENVOYEE -> "Envoyée";
            case RECU -> "Reçu";
            default -> "Inconnu";
        };
    }

    /**
     * Get status code (constant name)
     */
    public static String getStatusCode(int status) {
        return switch (status) {
            case A_RECEPTIONNER -> "A_RECEPTIONNER";
            case COLIS_ACCEPTE -> "COLIS_ACCEPTE";
            case A_SCANNER -> "A_SCANNER";
            case A_OUVRIR -> "A_OUVRIR";
            case A_NOTER -> "A_NOTER";
            case A_CERTIFIER -> "A_CERTIFIER";
            case A_PREPARER -> "A_PREPARER";
            case A_DESCELLER -> "A_DESCELLER";
            case A_VOIR -> "A_VOIR";
            case A_DISTRIBUER -> "A_DISTRIBUER";
            case A_ENVOYER -> "A_ENVOYER";
            case ENVOYEE -> "ENVOYEE";
            case RECU -> "RECU";
            default -> "UNKNOWN";
        };
    }

    /**
     * Get all valid status values
     */
    public static int[] getAllStatuses() {
        return new int[] {
                A_RECEPTIONNER, COLIS_ACCEPTE, A_SCANNER, A_OUVRIR,
                A_NOTER, A_CERTIFIER, A_PREPARER, A_DESCELLER, A_VOIR,
                A_DISTRIBUER, A_ENVOYER, ENVOYEE, RECU
        };
    }


    // ========== USAGE EXAMPLES ==========

    /*
    // Example 1: Create planning with specific status
    Planning planning = Planning.builder()
        .orderId(orderId)
        .employeeId(employeeId)
        .status(OrderStatusConstants.A_NOTER)  // To be graded
        .delai("X")
        .build();

    // Example 2: Check if status needs planning
    if (OrderStatusConstants.isPlanningStage(order.getStatus())) {
        // This order needs to be planned for an employee
        createPlanningForOrder(order);
    }

    // Example 3: Filter orders by planning stages
    String sql = """
        SELECT * FROM card_order
        WHERE status IN (2, 3, 4, 6, 7, 10, 11)
          AND annulee = 0
        """;

    // Example 4: Get display name
    String displayName = OrderStatusConstants.getDisplayNameEn(
        OrderStatusConstants.A_NOTER
    );
    // Returns: "To be graded"
    */
}