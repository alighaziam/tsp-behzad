package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

public class DefaultFilterStrategy implements FilterStrategy {
    private final String operand;

    public DefaultFilterStrategy(String operand) {
        this.operand = operand == null ? "=" : operand.trim();
    }

    @Override
    public boolean supports(String op) {
        return true;
    }

    @Override
    public String normalizedOperand() {
        return operand.toUpperCase();
    }

    @Override
    public String normalizeValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}


