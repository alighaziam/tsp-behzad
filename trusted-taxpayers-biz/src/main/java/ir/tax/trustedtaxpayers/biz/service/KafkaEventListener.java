package ir.tax.trustedtaxpayers.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.*;

@Component
@ConditionalOnProperty(prefix = "application.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventListener {

    private final ObjectMapper objectMapper;
    private final InvoiceTransmissionService transmissionService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaEventListener.class);

    public KafkaEventListener(ObjectMapper objectMapper, InvoiceTransmissionService transmissionService) {
        this.objectMapper = objectMapper;
        this.transmissionService = transmissionService;
    }

    @KafkaListener(topics = "#{'${application.kafka.topics.main:tsp.events}'}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(@Payload String payload,
                          @Header(RECEIVED_TOPIC) String topic,
                          @Header(RECEIVED_PARTITION) int partition,
                          @Header(OFFSET) long offset) {
        java.util.Map<String, Object> map = parse(payload);
        if (map == null) {
            log.warn("Kafka message parse failed at {}-{}#{} - payload: {}", topic, partition, offset, payload);
            return;
        }
        Object invoiceIdValue = map.get("invoiceId");
        if (invoiceIdValue == null) {
            log.warn("Kafka message missing invoiceId at {}-{}#{} - payload: {}", topic, partition, offset, payload);
            return;
        }
        java.util.UUID invoiceId;
        try {
            invoiceId = java.util.UUID.fromString(String.valueOf(invoiceIdValue));
        } catch (Exception ex) {
            log.warn("Invalid invoiceId format at {}-{}#{} - value: {}", topic, partition, offset, invoiceIdValue);
            return;
        }
        try {
            InvoiceTransmissionService.TransmissionResult result = transmissionService.transmitInvoice(invoiceId);
            log.debug("Transmission result for {}: {}", invoiceId, result);
        } catch (Exception ex) {
            log.error("Transmission failed for {} from {}-{}#{}: {}", invoiceId, topic, partition, offset, ex.getMessage(), ex);
        }
    }

    private java.util.Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }
}


