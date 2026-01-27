package ir.tax.trustedtaxpayers.biz.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * سرویس جمع‌آوری و ردیابی Metrics برای OCR
 * این سرویس اطلاعات آماری درباره عملکرد OCR را جمع‌آوری می‌کند
 */
@Service
@Slf4j
public class OcrMetricsService {
    
    // Counters
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final LongAdder totalProcessingTime = new LongAdder();
    private final LongAdder totalTextLength = new LongAdder();
    private final LongAdder totalFieldsExtracted = new LongAdder();
    
    // Provider-specific metrics
    private final Map<String, AtomicLong> providerSuccessCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> providerFailureCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> providerProcessingTime = new ConcurrentHashMap<>();
    
    // Error tracking
    private final Map<String, AtomicLong> errorTypeCount = new ConcurrentHashMap<>();
    
    // Field extraction metrics
    private final Map<String, AtomicLong> fieldExtractionCount = new ConcurrentHashMap<>();
    
    /**
     * ثبت درخواست OCR جدید
     */
    public void recordRequest() {
        totalRequests.incrementAndGet();
    }
    
    /**
     * ثبت موفقیت درخواست OCR
     */
    public void recordSuccess(String providerName, long processingTimeMs, int textLength, int fieldsExtracted) {
        successfulRequests.incrementAndGet();
        totalProcessingTime.add(processingTimeMs);
        totalTextLength.add(textLength);
        totalFieldsExtracted.add(fieldsExtracted);
        
        // Provider-specific metrics
        providerSuccessCount.computeIfAbsent(providerName, k -> new AtomicLong(0)).incrementAndGet();
        providerProcessingTime.computeIfAbsent(providerName, k -> new LongAdder()).add(processingTimeMs);
        
        log.debug("OCR metrics: Success recorded for provider: {}, time: {}ms, text: {} chars, fields: {}", 
                providerName, processingTimeMs, textLength, fieldsExtracted);
    }
    
    /**
     * ثبت خطا در درخواست OCR
     */
    public void recordFailure(String providerName, String errorType, long processingTimeMs) {
        failedRequests.incrementAndGet();
        totalProcessingTime.add(processingTimeMs);
        
        // Provider-specific metrics
        providerFailureCount.computeIfAbsent(providerName, k -> new AtomicLong(0)).incrementAndGet();
        providerProcessingTime.computeIfAbsent(providerName, k -> new LongAdder()).add(processingTimeMs);
        
        // Error type tracking
        errorTypeCount.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("OCR metrics: Failure recorded for provider: {}, error: {}, time: {}ms", 
                providerName, errorType, processingTimeMs);
    }
    
    /**
     * ثبت استخراج فیلد
     */
    public void recordFieldExtraction(String fieldName) {
        fieldExtractionCount.computeIfAbsent(fieldName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * دریافت آمار کلی
     */
    public OcrMetrics getMetrics() {
        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();
        long totalTime = totalProcessingTime.sum();
        long totalText = totalTextLength.sum();
        long totalFields = totalFieldsExtracted.sum();
        
        double successRate = total > 0 ? (double) successful / total * 100 : 0;
        double failureRate = total > 0 ? (double) failed / total * 100 : 0;
        double avgProcessingTime = successful > 0 ? (double) totalTime / successful : 0;
        double avgTextLength = successful > 0 ? (double) totalText / successful : 0;
        double avgFieldsExtracted = successful > 0 ? (double) totalFields / successful : 0;
        
        return new OcrMetrics(
                total,
                successful,
                failed,
                successRate,
                failureRate,
                avgProcessingTime,
                avgTextLength,
                avgFieldsExtracted,
                providerSuccessCount,
                providerFailureCount,
                providerProcessingTime,
                errorTypeCount,
                fieldExtractionCount
        );
    }
    
    /**
     * کلاس برای نگهداری آمار OCR
     */
    public static class OcrMetrics {
        public final long totalRequests;
        public final long successfulRequests;
        public final long failedRequests;
        public final double successRate; // درصد
        public final double failureRate; // درصد
        public final double avgProcessingTimeMs;
        public final double avgTextLength;
        public final double avgFieldsExtracted;
        public final Map<String, AtomicLong> providerSuccessCount;
        public final Map<String, AtomicLong> providerFailureCount;
        public final Map<String, LongAdder> providerProcessingTime;
        public final Map<String, AtomicLong> errorTypeCount;
        public final Map<String, AtomicLong> fieldExtractionCount;
        
        public OcrMetrics(long totalRequests, long successfulRequests, long failedRequests,
                          double successRate, double failureRate, double avgProcessingTimeMs,
                          double avgTextLength, double avgFieldsExtracted,
                          Map<String, AtomicLong> providerSuccessCount,
                          Map<String, AtomicLong> providerFailureCount,
                          Map<String, LongAdder> providerProcessingTime,
                          Map<String, AtomicLong> errorTypeCount,
                          Map<String, AtomicLong> fieldExtractionCount) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.successRate = successRate;
            this.failureRate = failureRate;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
            this.avgTextLength = avgTextLength;
            this.avgFieldsExtracted = avgFieldsExtracted;
            this.providerSuccessCount = providerSuccessCount;
            this.providerFailureCount = providerFailureCount;
            this.providerProcessingTime = providerProcessingTime;
            this.errorTypeCount = errorTypeCount;
            this.fieldExtractionCount = fieldExtractionCount;
        }
        
        public String getSummary() {
            return String.format(
                    "OCR Metrics: Total=%d, Success=%d (%.1f%%), Failed=%d (%.1f%%), " +
                    "AvgTime=%.1fms, AvgText=%.0f chars, AvgFields=%.1f",
                    totalRequests, successfulRequests, successRate, failedRequests, failureRate,
                    avgProcessingTimeMs, avgTextLength, avgFieldsExtracted
            );
        }
    }
    
    /**
     * Reset metrics (برای testing)
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalProcessingTime.reset();
        totalTextLength.reset();
        totalFieldsExtracted.reset();
        providerSuccessCount.clear();
        providerFailureCount.clear();
        providerProcessingTime.clear();
        errorTypeCount.clear();
        fieldExtractionCount.clear();
    }
}

