package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * موجودیت پیام اوتباکس برای پیاده‌سازی الگوی outbox.
 * <p>
 * Outbox message entity used for the outbox pattern implementation.
 */
@Entity
@Table(name = "OUTBOX_MESSAGES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessageEntity {
    /**
     * شناسه یکتای پیام.
     * <p>
     * Unique identifier of the message.
     */
    @Id
    @Column(name = "ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID id;

    /**
     * نوع منبع (aggregate) که پیام به آن تعلق دارد.
     * <p>
     * Aggregate type the event belongs to.
     */
    @Column(name = "AGGREGATE_TYPE", length = 100, nullable = false)
    private String aggregateType;

    /**
     * شناسه منبع مرتبط.
     * <p>
     * Aggregate identifier associated with the event.
     */
    @Column(name = "AGGREGATE_ID", length = 100, nullable = false)
    private String aggregateId;

    /**
     * نوع رویداد ذخیره‌شده.
     * <p>
     * Type of the event captured.
     */
    @Column(name = "EVENT_TYPE", length = 100, nullable = false)
    private String eventType;

    /**
     * محتوای رویداد به‌صورت JSON.
     * <p>
     * Event payload serialized as JSON.
     */
    @Lob
    @Column(name = "PAYLOAD", nullable = false)
    private String payload;

    /**
     * هدرهای اضافه برای پیام (اختیاری).
     * <p>
     * Optional headers attached to the message.
     */
    @Lob
    @Column(name = "HEADERS")
    private String headers;

    /**
     * وضعیت پردازش پیام (PENDING, SENT, FAILED).
     * <p>
     * Processing status of the message (PENDING, SENT, FAILED).
     */
    @Column(name = "STATUS", length = 20, nullable = false)
    private String status; // PENDING, SENT, FAILED

    /**
     * تعداد تلاش‌های انجام‌شده.
     * <p>
     * Number of attempts made to deliver the message.
     */
    @Column(name = "ATTEMPTS", nullable = false)
    private int attempts;

    /**
     * زمان برنامه‌ریزی‌شده برای تلاش بعدی.
     * <p>
     * Scheduled time for the next attempt.
     */
    @Column(name = "NEXT_ATTEMPT_AT")
    private OffsetDateTime nextAttemptAt;

    /**
     * زمان ایجاد پیام.
     * <p>
     * Timestamp when the message was created.
     */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * زمان آخرین به‌روزرسانی پیام.
     * <p>
     * Timestamp of the last update.
     */
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;
}


