package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternRequestEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PatternRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * جاب زمان‌بندی‌شده برای پردازش درخواست‌های الگوی معوق تایید شده.
 * <p>
 * Scheduled job that dispatches approved deferred pattern requests.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatternDeferredSubmissionJob {

    /**
     * ریپازیتوری فراخوانی درخواست‌های معوق.
     * <p>
     * Repository used to load deferred requests.
     */
    private final PatternRequestRepository patternRequestRepository;
    /**
     * سرویس ارسال صورتحساب به سامانه مودیان.
     * <p>
     * Service responsible for transmitting invoices to the external system.
     */
    private final InvoiceTransmissionService invoiceTransmissionService;
    /**
     * فاساد برای اعمال نتایج و خطاها روی درخواست‌ها.
     * <p>
     * Facade used to apply transmission results and failures.
     */
    private final PatternFacadeService patternFacadeService;

    /**
     * حداکثر تعداد درخواست‌هایی که در هر اجرای جاب پردازش می‌شوند.
     * <p>
     * Maximum number of requests processed per job execution.
     */
    @Value("${application.pattern.deferred-dispatch.batch-size:25}")
    private int batchSize;

    /**
     * درخواست‌های معوق تایید شده را یافته، ارسال می‌کند و نتیجه را ثبت می‌کند.
     * <p>
     * Finds approved deferred requests, transmits them, and records outcomes.
     */
    @Scheduled(fixedDelayString = "${application.pattern.deferred-dispatch.fixed-delay-ms:60000}")
    public void dispatchApproved() {
        List<PatternRequestEntity> requests = patternRequestRepository
                .findTop100ByDeferredSubmissionTrueAndDeferredApprovedTrueAndStatusOrderByCreatedOnAsc(
                        PatternRequestStatus.DEFERRED);

        if (requests.isEmpty()) {
            return;
        }

        int processed = 0;
        for (PatternRequestEntity entity : requests) {
            if (processed >= batchSize) {
                break;
            }
            UUID requestId = entity.getId();
            if (entity.getHeaderId() == null) {
                log.warn("Skipping deferred pattern request {} due to missing headerId", requestId);
                continue;
            }
            try {
                InvoiceTransmissionService.TransmissionResult result =
                        invoiceTransmissionService.transmitInvoice(entity.getHeaderId());
                patternFacadeService.applyTransmissionResult(requestId, entity.getHeaderId(), result);
                processed++;
            } catch (Exception ex) {
                log.error("Deferred submission failed for request {}: {}", requestId, ex.getMessage(), ex);
                patternFacadeService.applyTransmissionFailure(requestId, entity.getHeaderId(), ex.getMessage());
            }
        }
    }
}
