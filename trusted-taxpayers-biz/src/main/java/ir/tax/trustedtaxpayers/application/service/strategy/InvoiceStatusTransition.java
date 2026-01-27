package ir.tax.trustedtaxpayers.application.service.strategy;

import java.util.Set;

public class InvoiceStatusTransition {
    public static final String DRAFT = "DRAFT";
    public static final String ISSUED = "ISSUED";
    public static final String CANCELED = "CANCELED";
    public static final String REFUNDED = "REFUNDED";

    private static final Set<String> FINAL_STATES = Set.of(CANCELED, REFUNDED);
    private static final Set<String> ALLOWED_FROM_DRAFT = Set.of(ISSUED, CANCELED);
    private static final Set<String> ALLOWED_FROM_ISSUED = Set.of(REFUNDED);

    public static boolean canTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        if (currentStatus.equals(newStatus)) return true;
        if (FINAL_STATES.contains(currentStatus)) return false;
        if (DRAFT.equals(currentStatus)) return ALLOWED_FROM_DRAFT.contains(newStatus);
        if (ISSUED.equals(currentStatus)) return ALLOWED_FROM_ISSUED.contains(newStatus);
        return false;
    }

    public static boolean isEditable(String status) {
        return DRAFT.equals(status);
    }

    public static void validateTransition(String currentStatus, String newStatus) {
        if (!canTransition(currentStatus, newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", currentStatus, newStatus)
            );
        }
    }
}



