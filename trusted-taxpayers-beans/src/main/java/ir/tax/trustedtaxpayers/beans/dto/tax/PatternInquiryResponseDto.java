package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternInquiryResponseDto {

    @Schema(description = "شناسه درخواست")
    private UUID requestId;

    @Schema(description = "شماره الگو")
    private Integer patternNumber;

    @Schema(description = "وضعیت فعلی")
    private String status;

    @Schema(description = "شناسه یا نام third-party")
    private String requestedBy;

    @Schema(description = "کلید third-party")
    private String clientKey;

    @Schema(description = "نام third-party")
    private String clientName;

    @Schema(description = "شناسه یونیک third-party برای این درخواست")
    private String externalRequestId;

    @Schema(description = "آیا ارسال معوق است؟", example = "false")
    private boolean deferredSubmission;

    @Schema(description = "آیا درخواست معوق تایید شده است؟", example = "false")
    private boolean deferredApproved;

    @Schema(description = "شناسه مالیاتی در صورت تولید")
    private String taxId;

    @Schema(description = "referenceNumber سامانه امور مالیاتی در صورت وجود")
    private String referenceNumber;

    @Schema(description = "کد رد در صورت وجود")
    private String rejectionCode;

    @Schema(description = "شرح رد در صورت وجود")
    private String rejectionDescription;

    @Schema(description = "زمان ایجاد درخواست")
    private OffsetDateTime createdOn;

    @Schema(description = "زمان ارسال درخواست")
    private OffsetDateTime submittedAt;

    @Schema(description = "زمان تکمیل درخواست")
    private OffsetDateTime completedAt;
}
