package ir.tax.trustedtaxpayers.biz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.OutboxMessageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxMessageRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxMessageRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public UUID enqueue(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload, Map<String, Object> headers) {
        String payloadJson = toJson(payload);
        String headersJson = headers == null ? null : toJson(headers);
        UUID id = UUID.randomUUID();
        OutboxMessageEntity entity = OutboxMessageEntity.builder()
                .id(id)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadJson)
                .headers(headersJson)
                .status("PENDING")
                .attempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        outboxRepository.save(entity);
        return id;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }
}


