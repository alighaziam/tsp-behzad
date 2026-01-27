package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.OutboxMessageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "application.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private final OutboxMessageRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OutboxDispatcher.class);

    @Value("${application.kafka.topics.main:tsp.events}")
    private String mainTopic;

    @Value("${application.kafka.topics.pattern-submission:pattern-submissions}")
    private String patternSubmissionTopic;

    // هر 5 ثانیه پیام‌های Due را ارسال می‌کند
    @Scheduled(fixedDelayString = "${application.outbox.dispatcher.fixed-delay-ms:5000}")
    public void dispatch() {
        List<OutboxMessageEntity> dueMessages = outboxRepository.findDuePending(OffsetDateTime.now());
        if (dueMessages.isEmpty()) {
            return;
        }
        for (OutboxMessageEntity msg : dueMessages) {
            try {
                if (msg.getPayload() == null || msg.getPayload().isBlank()) {
                    log.warn("Skipping outbox message {} with empty payload", msg.getId());
                    msg.setStatus("FAILED");
                    msg.setUpdatedAt(OffsetDateTime.now());
                    continue;
                }
                String topic = resolveTopic(msg.getEventType());
                kafkaTemplate.send(topic, msg.getAggregateId(), msg.getPayload()).get();
                msg.setStatus("SENT");
                msg.setUpdatedAt(OffsetDateTime.now());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                int attempts = msg.getAttempts() + 1;
                msg.setAttempts(attempts);
                long backoffMs = computeBackoffMillis(attempts);
                msg.setNextAttemptAt(OffsetDateTime.now().plusNanos(backoffMs * 1_000_000));
                msg.setStatus("PENDING");
                msg.setUpdatedAt(OffsetDateTime.now());
                log.warn("Dispatch interrupted for message {} - will retry in {} ms", msg.getId(), backoffMs);
            } catch (Exception ex) {
                int attempts = msg.getAttempts() + 1;
                msg.setAttempts(attempts);
                long backoffMs = computeBackoffMillis(attempts);
                msg.setNextAttemptAt(OffsetDateTime.now().plusNanos(backoffMs * 1_000_000));
                msg.setStatus("PENDING");
                msg.setUpdatedAt(OffsetDateTime.now());
                log.warn("Dispatch failed for message {} (attempt {}): {} - retry in {} ms",
                        msg.getId(), attempts, ex.getMessage(), backoffMs);
            }
        }
        outboxRepository.saveAll(dueMessages);
    }

    private long computeBackoffMillis(int attempts) {
        long base = 1000L;
        long max = 60000L;
        long val = (long) (base * Math.pow(2, Math.min(attempts, 6)));
        return Math.min(val, max);
    }

    private String resolveTopic(String eventType) {
        if ("PATTERN_SUBMISSION".equalsIgnoreCase(eventType)) {
            return patternSubmissionTopic;
        }
        return mainTopic;
    }
}


