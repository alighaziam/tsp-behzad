package ir.tax.trustedtaxpayers.interfaces.rest.listeners;

import ir.tax.trustedtaxpayers.interfaces.rest.events.EntityCreatedEvent;
import ir.tax.trustedtaxpayers.interfaces.rest.events.EntityUpdatedEvent;
import ir.tax.trustedtaxpayers.interfaces.rest.events.EntityDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener {
    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);
    private final ir.tax.trustedtaxpayers.interfaces.rest.audit.DbAuditLogger dbAuditLogger;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public AuditEventListener(ir.tax.trustedtaxpayers.interfaces.rest.audit.DbAuditLogger dbAuditLogger) {
        this.dbAuditLogger = dbAuditLogger;
    }

    @EventListener
    public <T> void onCreated(EntityCreatedEvent<T> event) {
        log.info("Entity created: {}", event.getEntityClass().getSimpleName());
        persist("CREATE", event.getEntityClass(), null, event.getEntity());
    }

    @EventListener
    public <T, ID> void onUpdated(EntityUpdatedEvent<T, ID> event) {
        log.info("Entity updated: {} id={}", event.getEntityClass().getSimpleName(), event.getId());
        persist("UPDATE", event.getEntityClass(), String.valueOf(event.getId()), event.getEntity());
    }

    @EventListener
    public <T, ID> void onDeleted(EntityDeletedEvent<T, ID> event) {
        log.info("Entity deleted: {} id={}", event.getEntityClass().getSimpleName(), event.getId());
        persist("DELETE", event.getEntityClass(), String.valueOf(event.getId()), null);
    }

    private <T> void persist(String action, Class<T> entityClass, String id, T entity) {
        try {
            java.util.Map<String, Object> asMap = entity == null ? java.util.Map.of() : objectMapper.convertValue(entity, java.util.Map.class);
            dbAuditLogger.log(action, entityClass.getSimpleName(), id, asMap);
        } catch (Exception e) {
            log.warn("Audit persist failed: {}", e.getMessage());
        }
    }
}


