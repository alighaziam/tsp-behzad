package ir.tax.trustedtaxpayers.interfaces.rest.audit;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class DefaultRedactionStrategy implements RedactionStrategy {
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(java.util.Arrays.asList(
            "password", "passwordHash", "secret", "token", "authorization", "auth", "apiKey"
    ));

    @Override
    public Map<String, Object> redact(Map<String, Object> input) {
        if (input == null) return Map.of();
        Map<String, Object> copy = new HashMap<>(input.size());
        for (Map.Entry<String, Object> e : input.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (key == null) continue;
            if (SENSITIVE_KEYS.contains(key)) {
                copy.put(key, "***");
            } else {
                copy.put(key, value);
            }
        }
        return copy;
    }
}


