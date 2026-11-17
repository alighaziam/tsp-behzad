package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing invoice transmission to external taxpayer system
 * سرویس مدیریت ارسال صورتحساب به سامانه مودیان
 * 
 * این سرویس مسئولیت‌های زیر را دارد:
 * - ارسال صورتحساب به سامانه مودیان
 * - نگهداری وضعیت ارسال در ELECTRONIC_INVOICE_SYSTEM
 * - مدیریت تغییر وضعیت‌ها (DRAFT -> SUBMITTED -> ACCEPTED/REJECTED)
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Service
public class InvoiceTransmissionService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceTransmissionService.class);
    
    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final ElectronicInvoiceSystemRepository systemRepository;
    private final InvoiceSubmissionDelegate submissionDelegate;
    private final ObjectProvider<InvoiceTransmissionService> selfProvider;
    
    public InvoiceTransmissionService(
            ElectronicInvoiceHeaderRepository headerRepository,
            ElectronicInvoiceSystemRepository systemRepository,
            InvoiceSubmissionDelegate submissionDelegate,
            ObjectProvider<InvoiceTransmissionService> selfProvider) {
        this.headerRepository = headerRepository;
        this.systemRepository = systemRepository;
        this.submissionDelegate = submissionDelegate;
        this.selfProvider = selfProvider;
    }
    
    /**
     * ارسال صورتحساب به سامانه مودیان و ثبت وضعیت
     * 
     * @param invoiceId شناسه صورتحساب
     * @return وضعیت نهایی ارسال
     */
    @Transactional
    public TransmissionResult transmitInvoice(UUID invoiceId) {
        logger.info("Starting transmission for invoice: {}", invoiceId);
        
        // دریافت صورتحساب از دیتابیس
        ElectronicInvoiceHeaderEntity invoice = headerRepository.findByIdForTransmission(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        // دریافت یا ساخت رکورد سیستم
        ElectronicInvoiceSystemEntity systemEntity = systemRepository.findByHeaderId(invoiceId)
            .orElseGet(() -> createSystemEntity(invoiceId));
        
        // بررسی وضعیت فعلی
        String currentStatus = systemEntity.getSts();
        if (currentStatus != null && isFinalStatus(currentStatus)) {
            logger.warn("Invoice {} already in final status: {}", invoiceId, currentStatus);
            return createResult(invoice, systemEntity, "ALREADY_FINAL",
                "Invoice already in final status: " + currentStatus);
        }
        
        // Persist a deterministic UID before the external call. If the database
        // transaction rolls back after an ambiguous network outcome, the same
        // invoice ID still produces the same packet UID on the next attempt.
        if (systemEntity.getUniqueId() == null) {
            systemEntity.setUniqueId(UUID.nameUUIDFromBytes(
                    ("tsp-invoice:" + invoiceId).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            systemEntity.setUpdatedOn(OffsetDateTime.now());
            systemRepository.saveAndFlush(systemEntity);
        }

        // ارسال صورتحساب به سامانه مودیان
        try {
            TransmissionResponse response = submissionDelegate.submitInvoice(invoice, systemEntity.getUniqueId().toString());
            
            // به‌روزرسانی وضعیت در دیتابیس
            updateSystemStatus(systemEntity, response);
            
            logger.info("Invoice {} transmission completed with status: {}", invoiceId, response.getStatus());
            
            return createResult(invoice, systemEntity, response.getStatus(), response.getMessage());
            
        } catch (Exception e) {
            logger.error("Failed to transmit invoice {}", invoiceId, e);
            
            // ثبت خطا در دیتابیس
            updateSystemStatusOnError(systemEntity, e.getMessage());
            
            return createResult(invoice, systemEntity, "TRANSMISSION_ERROR",
                "Failed to transmit: " + e.getMessage());
        }
    }
    
    /**
     * ایجاد رکورد سیستم برای صورتحساب جدید
     */
    private ElectronicInvoiceSystemEntity createSystemEntity(UUID invoiceId) {
        ElectronicInvoiceSystemEntity systemEntity = ElectronicInvoiceSystemEntity.builder()
            .sts("DRAFT")
            .build();
        systemEntity.setId(invoiceId);
        systemEntity.setCreatedOn(OffsetDateTime.now());
        return systemRepository.save(systemEntity);
    }
    
    /**
     * به‌روزرسانی وضعیت سیستم بر اساس پاسخ سامانه
     */
    private void updateSystemStatus(ElectronicInvoiceSystemEntity systemEntity, TransmissionResponse response) {
        systemEntity.setSts(response.getStatus());
        systemEntity.setSrvdt(OffsetDateTime.now());
        systemEntity.setUpdatedOn(OffsetDateTime.now());
        
        if ("REJECTED".equalsIgnoreCase(response.getStatus())) {
            systemEntity.setRejcode(response.getRejectionCode());
            systemEntity.setRejdesc(response.getRejectionDescription());
        } else {
            // در صورت موفقیت، فیلدهای رد را پاک می‌کنیم
            systemEntity.setRejcode(null);
            systemEntity.setRejdesc(null);
        }
        
        systemRepository.save(systemEntity);
    }
    
    /**
     * به‌روزرسانی وضعیت در صورت خطا
     */
    private void updateSystemStatusOnError(ElectronicInvoiceSystemEntity systemEntity, String errorMessage) {
        systemEntity.setSts("FAILED");
        systemEntity.setSrvdt(OffsetDateTime.now());
        systemEntity.setUpdatedOn(OffsetDateTime.now());
        systemEntity.setRejcode("TRANSMISSION_ERROR");
        systemEntity.setRejdesc(errorMessage);
        
        systemRepository.save(systemEntity);
    }
    
    /**
     * بررسی نهایی بودن وضعیت
     */
    private boolean isFinalStatus(String status) {
        return "ACCEPTED".equalsIgnoreCase(status) 
            || "REJECTED".equalsIgnoreCase(status)
            || "CANCELLED".equalsIgnoreCase(status)
            || "FAILED".equalsIgnoreCase(status)
            || "SUBMITTED".equalsIgnoreCase(status)
            || "PENDING".equalsIgnoreCase(status);
    }
    
    /**
     * ساخت نتیجه ارسال
     */
    private TransmissionResult createResult(ElectronicInvoiceHeaderEntity invoice, 
                                           ElectronicInvoiceSystemEntity systemEntity,
                                           String status, String message) {
        return new TransmissionResult(
            invoice.getId(),
            invoice.getTaxId(),
            status,
            message,
            systemEntity.getRejcode(),
            systemEntity.getRejdesc(),
            systemEntity.getSrvdt()
        );
    }
    
    /**
     * دریافت وضعیت فعلی ارسال
     */
    @Transactional(readOnly = true)
    public TransmissionStatus getTransmissionStatus(UUID invoiceId) {
        ElectronicInvoiceSystemEntity systemEntity = systemRepository.findByHeaderId(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        return new TransmissionStatus(
            invoiceId,
            systemEntity.getSts(),
            systemEntity.getSrvdt(),
            systemEntity.getRejcode(),
            systemEntity.getRejdesc()
        );
    }
    
    /**
     * ارسال Async صورتحساب به سامانه مودیان
     * 
     * @param invoiceId شناسه صورتحساب
     * @return Future که نتیجه ارسال را برمی‌گرداند
     */
    @Async("asyncExecutor")
    public CompletableFuture<TransmissionResult> transmitInvoiceAsync(UUID invoiceId) {
        logger.info("Starting async transmission for invoice: {}", invoiceId);
        try {
            TransmissionResult result = selfProvider.getObject().transmitInvoice(invoiceId);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            logger.error("Async transmission failed for invoice {}", invoiceId, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * ارسال Batch چندین صورتحساب به صورت async
     * 
     * @param invoiceIds لیست شناسه‌های صورتحساب
     * @return لیست نتایج ارسال (ترتیبی)
     */
    @Async("asyncExecutor")
    public CompletableFuture<BatchTransmissionResult> transmitInvoicesBatchAsync(List<UUID> invoiceIds) {
        logger.info("Starting batch transmission for {} invoices", invoiceIds.size());

        BatchTransmissionResult.Builder builder = new BatchTransmissionResult.Builder();
        int successful = 0;
        int failed = 0;

        // Run one async parent and process each invoice through its transactional proxy.
        // Scheduling child tasks on the same bounded executor can starve or reject work.
        for (UUID invoiceId : invoiceIds) {
            TransmissionResult result;
            try {
                result = selfProvider.getObject().transmitInvoice(invoiceId);
            } catch (Exception e) {
                logger.error("Batch transmission failed for invoice {}", invoiceId, e);
                result = new TransmissionResult(invoiceId, null, "TRANSMISSION_ERROR",
                        "Failed: " + e.getMessage(), null, null, null);
            }
            builder.addResult(invoiceId, result);
            if (result.isSuccessful()) {
                successful++;
            } else {
                failed++;
            }
        }

        BatchTransmissionResult result = builder.total(invoiceIds.size())
                .successful(successful).failed(failed).build();
        logger.info("Batch transmission completed: {} successful, {} failed out of {} total",
                result.getSuccessful(), result.getFailed(), result.getTotal());
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Retry ارسال برای صورتحساب‌های FAILED یا REJECTED
     * 
     * @param invoiceId شناسه صورتحساب
     * @return نتیجه ارسال مجدد
     */
    @Transactional
    public TransmissionResult retryFailedInvoice(UUID invoiceId) {
        logger.info("Retrying transmission for failed invoice: {}", invoiceId);

        // Lock in the same order as normal transmission: header, then system row.
        ElectronicInvoiceHeaderEntity invoice = headerRepository.findByIdForTransmission(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        ElectronicInvoiceSystemEntity systemEntity = systemRepository.findByHeaderIdForUpdate(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice transmission state not found: " + invoiceId));
        String currentStatus = systemEntity.getSts();

        if (!"FAILED".equalsIgnoreCase(currentStatus)
                && !"REJECTED".equalsIgnoreCase(currentStatus)) {
            logger.warn("Invoice {} is not in retriable status: {}", invoiceId, currentStatus);
            return new TransmissionResult(invoiceId, invoice.getTaxId(), "NOT_RETRIABLE",
                    "Invoice status is not retriable: " + currentStatus, null, null, null);
        }

        systemEntity.setSts("DRAFT");
        systemEntity.setRejcode(null);
        systemEntity.setRejdesc(null);
        systemEntity.setUpdatedOn(OffsetDateTime.now());
        systemRepository.save(systemEntity);

        return selfProvider.getObject().transmitInvoice(invoiceId);
    }

    @Async("asyncExecutor")
    public CompletableFuture<TransmissionResult> retryFailedInvoiceAsync(UUID invoiceId) {
        try {
            return CompletableFuture.completedFuture(selfProvider.getObject().retryFailedInvoice(invoiceId));
        } catch (Exception e) {
            logger.error("Async retry failed for invoice {}", invoiceId, e);
            return CompletableFuture.completedFuture(new TransmissionResult(
                    invoiceId, null, "TRANSMISSION_ERROR", "Retry failed: " + e.getMessage(),
                    null, null, null));
        }
    }

    /**
     * Retry انبوه برای تمام صورتحساب‌های FAILED یا REJECTED
     * 
     * @return نتیجه Retry انبوه
     */
    @Async("asyncExecutor")
    public CompletableFuture<BatchTransmissionResult> retryAllFailedInvoicesAsync() {
        logger.info("Starting batch retry for all failed invoices");

        List<ElectronicInvoiceSystemEntity> failedInvoices = systemRepository.findBySts("FAILED");
        List<ElectronicInvoiceSystemEntity> rejectedInvoices = systemRepository.findBySts("REJECTED");
        List<UUID> retryIds = new java.util.ArrayList<>();
        failedInvoices.stream().map(ElectronicInvoiceSystemEntity::getId).forEach(retryIds::add);
        rejectedInvoices.stream().map(ElectronicInvoiceSystemEntity::getId).forEach(retryIds::add);

        BatchTransmissionResult.Builder builder = new BatchTransmissionResult.Builder();
        int successful = 0;
        int failed = 0;
        for (UUID invoiceId : retryIds) {
            TransmissionResult result;
            try {
                result = selfProvider.getObject().retryFailedInvoice(invoiceId);
            } catch (Exception e) {
                logger.error("Batch retry failed for invoice {}", invoiceId, e);
                result = new TransmissionResult(invoiceId, null, "TRANSMISSION_ERROR",
                        "Retry failed: " + e.getMessage(), null, null, null);
            }
            builder.addResult(invoiceId, result);
            if (result.isSuccessful()) {
                successful++;
            } else {
                failed++;
            }
        }

        BatchTransmissionResult result = builder.total(retryIds.size())
                .successful(successful).failed(failed).build();
        logger.info("Batch retry completed: {} successful, {} failed out of {} total",
                result.getSuccessful(), result.getFailed(), result.getTotal());
        return CompletableFuture.completedFuture(result);
    }

    /**
     * واسط برای ارسال صورتحساب (برای جداسازی مسئولیت‌ها)
     */
    public interface InvoiceSubmissionDelegate {
        TransmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity invoice);

        default TransmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity invoice, String uid) {
            return submitInvoice(invoice);
        }
    }
    
    /**
     * پاسخ ارسال از سامانه مودیان
     */
    public static class TransmissionResponse {
        private final String status;
        private final String message;
        private final String rejectionCode;
        private final String rejectionDescription;
        
        public TransmissionResponse(String status, String message, String rejectionCode, String rejectionDescription) {
            this.status = status;
            this.message = message;
            this.rejectionCode = rejectionCode;
            this.rejectionDescription = rejectionDescription;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getRejectionCode() {
            return rejectionCode;
        }
        
        public String getRejectionDescription() {
            return rejectionDescription;
        }
        
        public static TransmissionResponse success(String status, String message) {
            return new TransmissionResponse(status, message, null, null);
        }
        
        public static TransmissionResponse failure(String rejectionCode, String rejectionDescription) {
            return new TransmissionResponse("REJECTED", "Submission rejected", rejectionCode, rejectionDescription);
        }

        public static TransmissionResponse retryableFailure(String code, String description) {
            return new TransmissionResponse("FAILED", "Submission outcome is not confirmed", code, description);
        }
    }
    
    /**
     * نتیجه عملیات ارسال
     */
    public static class TransmissionResult {
        private final UUID invoiceId;
        private final String taxId;
        private final String status;
        private final String message;
        private final String rejectionCode;
        private final String rejectionDescription;
        private final OffsetDateTime sentAt;
        
        public TransmissionResult(UUID invoiceId, String taxId, String status, String message,
                                 String rejectionCode, String rejectionDescription, OffsetDateTime sentAt) {
            this.invoiceId = invoiceId;
            this.taxId = taxId;
            this.status = status;
            this.message = message;
            this.rejectionCode = rejectionCode;
            this.rejectionDescription = rejectionDescription;
            this.sentAt = sentAt;
        }
        
        public UUID getInvoiceId() {
            return invoiceId;
        }
        
        public String getTaxId() {
            return taxId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getRejectionCode() {
            return rejectionCode;
        }
        
        public String getRejectionDescription() {
            return rejectionDescription;
        }
        
        public OffsetDateTime getSentAt() {
            return sentAt;
        }
        
        public boolean isSuccessful() {
            return "ACCEPTED".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status);
        }
    }
    
    /**
     * وضعیت ارسال
     */
    public static class TransmissionStatus {
        private final UUID invoiceId;
        private final String status;
        private final OffsetDateTime sentAt;
        private final String rejectionCode;
        private final String rejectionDescription;
        
        public TransmissionStatus(UUID invoiceId, String status, OffsetDateTime sentAt,
                                 String rejectionCode, String rejectionDescription) {
            this.invoiceId = invoiceId;
            this.status = status;
            this.sentAt = sentAt;
            this.rejectionCode = rejectionCode;
            this.rejectionDescription = rejectionDescription;
        }
        
        public UUID getInvoiceId() {
            return invoiceId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public OffsetDateTime getSentAt() {
            return sentAt;
        }
        
        public String getRejectionCode() {
            return rejectionCode;
        }
        
        public String getRejectionDescription() {
            return rejectionDescription;
        }
    }
    
    /**
     * نتیجه ارسال Batch
     */
    public static class BatchTransmissionResult {
        private final int total;
        private final int successful;
        private final int failed;
        private final java.util.Map<UUID, TransmissionResult> results;
        
        private BatchTransmissionResult(Builder builder) {
            this.total = builder.total;
            this.successful = builder.successful;
            this.failed = builder.failed;
            this.results = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(builder.results));
        }
        
        public int getTotal() {
            return total;
        }
        
        public int getSuccessful() {
            return successful;
        }
        
        public int getFailed() {
            return failed;
        }
        
        public java.util.Map<UUID, TransmissionResult> getResults() {
            return results;
        }
        
        /**
         * Builder برای BatchTransmissionResult
         */
        public static class Builder {
            private int total = 0;
            private int successful = 0;
            private int failed = 0;
            private final java.util.Map<UUID, TransmissionResult> results = new java.util.HashMap<>();
            
            public Builder addResult(UUID invoiceId, TransmissionResult result) {
                results.put(invoiceId, result);
                return this;
            }
            
            public Builder total(int total) {
                this.total = total;
                return this;
            }
            
            public Builder successful(int successful) {
                this.successful = successful;
                return this;
            }
            
            public Builder failed(int failed) {
                this.failed = failed;
                return this;
            }
            
            public BatchTransmissionResult build() {
                return new BatchTransmissionResult(this);
            }
        }
    }
}

