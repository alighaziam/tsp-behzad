package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

public class EqualsFilterStrategy implements FilterStrategy {
    @Override
    public boolean supports(String operand) {
        if (operand == null) return true;
        String op = operand.trim().toUpperCase();
        return op.equals("=") || op.equals("EQ") || op.equals("EQUALS");
    }

    @Override
    public String normalizedOperand() {
        return "=";
    }

    @Override
    public String normalizeValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}


