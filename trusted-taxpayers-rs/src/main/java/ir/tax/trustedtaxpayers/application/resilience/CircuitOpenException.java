package ir.tax.trustedtaxpayers.application.resilience;

public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String message) {
        super(message);
    }
}


