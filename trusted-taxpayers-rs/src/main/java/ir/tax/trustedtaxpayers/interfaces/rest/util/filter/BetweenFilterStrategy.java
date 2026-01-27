package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

public class BetweenFilterStrategy implements FilterStrategy {
    @Override
    public boolean supports(String operand) {
        if (operand == null) return false;
        String op = operand.trim().toUpperCase();
        return op.equals("BETWEEN");
    }

    @Override
    public String normalizedOperand() {
        return "BETWEEN";
    }

    @Override
    public String normalizeValue(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value).trim();
        // expected format: start,end
        return s;
    }
}


