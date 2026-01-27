package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * موجودیت JPA برای ثبت درخواست‌های الگو و وضعیت گردش آن‌ها.
 * <p>
 * JPA entity representing lifecycle and metadata of pattern requests.
 */
@Entity
@Table(name = "PATTERN_REQUEST")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatternRequestEntity extends BaseEntity {

    /**
     * شماره الگوی مورد استفاده برای فرم.
     * <p>
     * Pattern number selected for the request.
     */
    @Column(name = "PATTERN_NUMBER", nullable = false)
    private Integer patternNumber;

    /**
     * وضعیت جاری درخواست در سامانه.
     * <p>
     * Current lifecycle status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private PatternRequestStatus status;

    /**
     * شناسه درخواست‌کننده (کلید کلاینت یا کاربر).
     * <p>
     * Identifier of the requester, typically client key or user id.
     */
    @Column(name = "REQUESTED_BY", length = 100)
    private String requestedBy;

    /**
     * متادیتای JSON ارسال‌شده همراه درخواست.
     * <p>
     * JSON metadata accompanying the request.
     */
    @Lob
    @Column(name = "REQUEST_METADATA")
    private String requestMetadata;

    /**
     * داده‌های خام ارسالی (سربرگ و اقلام) پیش از ارسال نهایی.
     * <p>
     * Raw payload submitted for validation (header and item sections).
     */
    @Lob
    @Column(name = "REQUEST_PAYLOAD")
    private String requestPayload;

    /**
     * پاسخ ثبت‌شده از سرویس‌های پایین‌دستی.
     * <p>
     * Serialized response from downstream services.
     */
    @Lob
    @Column(name = "RESPONSE_PAYLOAD")
    private String responsePayload;

    /**
     * شناسه مالی صادرشده برای درخواست.
     * <p>
     * Tax identifier generated upon submission.
     */
    @Column(name = "TAX_ID", length = 22)
    private String taxId;

    /**
     * شماره مرجع در سامانه امور مالیاتی.
     * <p>
     * Reference number provided by the tax system.
     */
    @Column(name = "REFERENCE_NUMBER", length = 50)
    private String referenceNumber;

    /**
     * کد خطای اعلام‌شده توسط سامانه مقصد.
     * <p>
     * Rejection code received from the external system.
     */
    @Column(name = "REJECTION_CODE", length = 50)
    private String rejectionCode;

    /**
     * توضیح خطای اعلام‌شده توسط سامانه مقصد.
     * <p>
     * Rejection description from the external system.
     */
    @Column(name = "REJECTION_DESCRIPTION", length = 500)
    private String rejectionDescription;

    /**
     * زمان ارسال درخواست به سامانه بیرونی.
     * <p>
     * Timestamp when the request was transmitted.
     */
    @Column(name = "SUBMITTED_AT")
    private OffsetDateTime submittedAt;

    /**
     * زمان تکمیل فرایند (موفق یا شکست).
     * <p>
     * Timestamp when the lifecycle completed (success or failure).
     */
    @Column(name = "COMPLETED_AT")
    private OffsetDateTime completedAt;

    /**
     * نشان‌دهنده ثبت به صورت معوق.
     * <p>
     * Indicates whether submission is deferred.
     */
    @Column(name = "DEFERRED_SUBMISSION", nullable = false)
    private boolean deferredSubmission;

    /**
     * نشان می‌دهد درخواست معوق تایید شده است یا خیر.
     * <p>
     * Flag marking deferred approval status.
     */
    @Column(name = "DEFERRED_APPROVED", nullable = false)
    private boolean deferredApproved;

    /**
     * شناسه سربرگ ایجادشده در سامانه بیرونی.
     * <p>
     * Header identifier returned by downstream systems.
     */
    @Convert(converter = RawUuidConverter.class)
    @Column(name = "HEADER_ID")
    private UUID headerId;
}
