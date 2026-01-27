package ir.tax.trustedtaxpayers.interfaces.rest.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class DbAuditLogger {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RedactionStrategy redactionStrategy;

    public DbAuditLogger(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, RedactionStrategy redactionStrategy) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.redactionStrategy = redactionStrategy;
    }

    public void log(String action, String entityName, String entityId, Map<String, Object> payload) {
        try {
            Map<String, Object> safe = redactionStrategy.redact(payload);
            String traceId = MDC.get("traceId");
            String username = MDC.get("username");
            java.util.Map<String, Object> envelope = new java.util.HashMap<>();
            envelope.put("traceId", traceId);
            envelope.put("username", username);
            envelope.put("payload", safe);
            String json = objectMapper.writeValueAsString(envelope);
            jdbcTemplate.update(
                    "INSERT INTO AUDIT_LOGS (ACTION, ENTITY_NAME, ENTITY_ID, DETAILS, EVENT_TIME) VALUES (?,?,?,?,?)",
                    action, entityName, entityId, json, java.sql.Timestamp.from(OffsetDateTime.now().toInstant())
            );
        } catch (Exception e) {
            // Fail-safe: do not break business flow on audit failure
            org.slf4j.LoggerFactory.getLogger(DbAuditLogger.class).warn("Audit log failed: {}", e.getMessage());
        }
    }
}


