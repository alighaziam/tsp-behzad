package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    public InvoiceTransmissionService(
            ElectronicInvoiceHeaderRepository headerRepository,
            ElectronicInvoiceSystemRepository systemRepository,
            InvoiceSubmissionDelegate submissionDelegate) {
        this.headerRepository = headerRepository;
        this.systemRepository = systemRepository;
        this.submissionDelegate = submissionDelegate;
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
        ElectronicInvoiceHeaderEntity invoice = headerRepository.findById(invoiceId)
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
        
        // ارسال صورتحساب به سامانه مودیان
        try {
            TransmissionResponse response = submissionDelegate.submitInvoice(invoice);
            
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
            || "FAILED".equalsIgnoreCase(status);
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
            TransmissionResult result = transmitInvoice(invoiceId);
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
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        List<CompletableFuture<TransmissionResult>> futures = invoiceIds.stream()
            .map(invoiceId -> transmitInvoiceAsync(invoiceId)
                .thenApply(result -> {
                    if (result.isSuccessful()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                    builder.addResult(invoiceId, result);
                    return result;
                })
                .exceptionally(throwable -> {
                    failureCount.incrementAndGet();
                    TransmissionResult errorResult = new TransmissionResult(
                        invoiceId,
                        null,
                        "TRANSMISSION_ERROR",
                        "Failed: " + throwable.getMessage(),
                        null,
                        null,
                        null
                    );
                    builder.addResult(invoiceId, errorResult);
                    return errorResult;
                })
            )
            .toList();
        
        // انتظار برای تمام درخواست‌ها
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        BatchTransmissionResult result = builder
            .total(invoiceIds.size())
            .successful(successCount.get())
            .failed(failureCount.get())
            .build();
        
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
        
        TransmissionStatus currentStatus = getTransmissionStatus(invoiceId);
        
        // بررسی اینکه صورتحساب قابل retry باشد
        if (!"FAILED".equalsIgnoreCase(currentStatus.getStatus()) 
            && !"REJECTED".equalsIgnoreCase(currentStatus.getStatus())) {
            logger.warn("Invoice {} is not in retriable status: {}", invoiceId, currentStatus.getStatus());
            
            // ساخت نتیجه برای status غیرقابل retry
            ElectronicInvoiceHeaderEntity invoice = headerRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
            
            return new TransmissionResult(
                invoiceId,
                invoice.getTaxId(),
                "NOT_RETRIABLE",
                "Invoice status is not retriable: " + currentStatus.getStatus(),
                null,
                null,
                null
            );
        }
        
        // reset وضعیت به DRAFT برای ارسال مجدد
        ElectronicInvoiceSystemEntity systemEntity = systemRepository.findByHeaderId(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        
        systemEntity.setSts("DRAFT");
        systemEntity.setRejcode(null);
        systemEntity.setRejdesc(null);
        systemEntity.setUpdatedOn(OffsetDateTime.now());
        systemRepository.save(systemEntity);
        
        // تلاش برای ارسال مجدد
        return transmitInvoice(invoiceId);
    }
    
    /**
     * Retry انبوه برای تمام صورتحساب‌های FAILED یا REJECTED
     * 
     * @return نتیجه Retry انبوه
     */
    @Async("asyncExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<BatchTransmissionResult> retryAllFailedInvoicesAsync() {
        logger.info("Starting batch retry for all failed invoices");
        
        // دریافت تمام صورتحساب‌های FAILED
        List<ElectronicInvoiceSystemEntity> failedInvoices = systemRepository.findBySts("FAILED");
        
        // دریافت تمام صورتحساب‌های REJECTED
        List<ElectronicInvoiceSystemEntity> rejectedInvoices = systemRepository.findBySts("REJECTED");
        
        List<UUID> allFailedIds = failedInvoices.stream()
            .map(ElectronicInvoiceSystemEntity::getId)
            .toList();
        
        List<UUID> allRejectedIds = rejectedInvoices.stream()
            .map(ElectronicInvoiceSystemEntity::getId)
            .toList();
        
        List<UUID> allRetryIds = new java.util.ArrayList<>();
        allRetryIds.addAll(allFailedIds);
        allRetryIds.addAll(allRejectedIds);
        
        logger.info("Found {} FAILED and {} REJECTED invoices to retry",
            allFailedIds.size(), allRejectedIds.size());
        
        // استفاده از batch async transmission
        return transmitInvoicesBatchAsync(allRetryIds);
    }
    
    /**
     * واسط برای ارسال صورتحساب (برای جداسازی مسئولیت‌ها)
     */
    public interface InvoiceSubmissionDelegate {
        TransmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity invoice);
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

