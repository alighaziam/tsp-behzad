package ir.tax.trustedtaxpayers.application.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import ir.tax.trustedtaxpayers.biz.service.PatternFacadeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * لیسنر Kafka برای پردازش ارسال‌های معوق و به‌روزرسانی وضعیت درخواست‌های الگو.
 * <p>
 * Kafka listener that processes deferred pattern submissions and updates request statuses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "application.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PatternSubmissionListener {

    /**
     * سرویس ارسال صورتحساب به سامانه مودیان.
     * <p>
     * Service used to transmit invoices to the external system.
     */
    private final InvoiceTransmissionService invoiceTransmissionService;
    /**
     * فاساد مدیریت وضعیت درخواست‌ها.
     * <p>
     * Facade used to update pattern request statuses.
     */
    private final PatternFacadeService patternFacadeService;
    /**
     * آبجکت مپ برای تبدیل پیام‌های JSON.
     * <p>
     * Object mapper for converting JSON messages.
     */
    private final ObjectMapper objectMapper;
    /**
     * تمپلیت کافکا جهت ارسال پیام به DLQ.
     * <p>
     * Kafka template to forward messages to the DLQ.
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * نام تاپیک صف خطاهای ارسال الگو.
     * <p>
     * Topic name for the pattern submission dead-letter queue.
     */
    @Value("${application.kafka.topics.pattern-submission-dlq:pattern-submissions-dlq}")
    private String dlqTopic;

    /**
     * پیام‌های ارسال الگو را مصرف کرده، آن‌ها را ارسال و نتیجه را ثبت می‌کند.
     * <p>
     * Consumes pattern submission messages, transmits them, and records the result.
     */
    @KafkaListener(topics = "${application.kafka.topics.pattern-submission:pattern-submissions}",
            groupId = "${application.kafka.consumers.pattern-submission.group:pattern-submission-group}")
    public void handleSubmission(ConsumerRecord<String, String> record) {
        String payload = record.value();
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String requestIdStr = (String) data.get("requestId");
            String headerIdStr = (String) data.get("headerId");
            if (requestIdStr == null || headerIdStr == null) {
                throw new IllegalArgumentException("Invalid payload: missing identifiers");
            }
            UUID requestId = UUID.fromString(requestIdStr);
            UUID headerId = UUID.fromString(headerIdStr);

            log.info("Processing queued pattern submission requestId={} headerId={}", requestId, headerId);
            InvoiceTransmissionService.TransmissionResult result = invoiceTransmissionService.transmitInvoice(headerId);
            patternFacadeService.applyTransmissionResult(requestId, headerId, result);
        } catch (Exception ex) {
            log.error("Failed to process pattern submission payload: {}", ex.getMessage(), ex);
            handleFailure(record.value(), ex);
        }
    }

    /**
     * در صورت بروز خطا، وضعیت درخواست را به شکست تغییر داده و پیام را به DLQ می‌فرستد.
     * <p>
     * On failure, updates the request status to failed and publishes the message to the DLQ.
     */
    private void handleFailure(String payload, Exception ex) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String requestIdStr = (String) data.get("requestId");
            String headerIdStr = (String) data.get("headerId");
            if (requestIdStr != null && headerIdStr != null) {
                UUID requestId = UUID.fromString(requestIdStr);
                UUID headerId = UUID.fromString(headerIdStr);
                patternFacadeService.applyTransmissionFailure(requestId, headerId, ex.getMessage());
            }
        } catch (Exception inner) {
            log.warn("Unable to update request status on failure: {}", inner.getMessage(), inner);
        }

        try {
            kafkaTemplate.send(dlqTopic, payload);
        } catch (Exception kafkaEx) {
            log.error("Failed to publish message to DLQ: {}", kafkaEx.getMessage(), kafkaEx);
        }
    }
}
