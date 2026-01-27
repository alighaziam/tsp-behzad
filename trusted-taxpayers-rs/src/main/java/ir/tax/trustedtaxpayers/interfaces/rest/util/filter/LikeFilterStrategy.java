package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

public class LikeFilterStrategy implements FilterStrategy {
    @Override
    public boolean supports(String operand) {
        if (operand == null) return false;
        String op = operand.trim().toUpperCase();
        return op.equals("LIKE") || op.equals("CONTAINS");
    }

    @Override
    public String normalizedOperand() {
        return "LIKE";
    }

    @Override
    public String normalizeValue(Object value) {
        String s = value == null ? "" : String.valueOf(value);
        if (!s.contains("%")) {
            return "%" + s + "%";
        }
        return s;
    }
}


