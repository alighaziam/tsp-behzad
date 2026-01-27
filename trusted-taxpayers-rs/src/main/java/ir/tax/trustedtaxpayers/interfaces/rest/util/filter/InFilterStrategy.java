package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

import java.util.Arrays;
import java.util.stream.Collectors;

public class InFilterStrategy implements FilterStrategy {
    @Override
    public boolean supports(String operand) {
        if (operand == null) return false;
        String op = operand.trim().toUpperCase();
        return op.equals("IN");
    }

    @Override
    public String normalizedOperand() {
        return "IN";
    }

    @Override
    public String normalizeValue(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        // Accept either comma-separated or JSON-like arrays; normalize to comma-separated
        s = s.replace('[', ' ').replace(']', ' ').trim();
        return Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .collect(Collectors.joining(","));
    }
}


