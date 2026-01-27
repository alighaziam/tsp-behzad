package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

public interface FilterStrategy {
    boolean supports(String operand);
    String normalizedOperand();
    String normalizeValue(Object value);
}


