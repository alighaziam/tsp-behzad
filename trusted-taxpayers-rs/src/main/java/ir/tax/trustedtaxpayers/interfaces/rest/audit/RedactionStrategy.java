package ir.tax.trustedtaxpayers.interfaces.rest.audit;

import java.util.Map;

public interface RedactionStrategy {
    Map<String, Object> redact(Map<String, Object> input);
}


