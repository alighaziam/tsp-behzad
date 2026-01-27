package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.biz.service.ocr.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR service that tries multiple providers and extracts fields from text.
 */
@Service
@Slf4j
public class OcrService {

    private final List<OcrProvider> ocrProviders;
    private final FieldExtractionService fieldExtractionService;
    private final AdaptiveFieldExtractionService adaptiveExtractionService;
    private final OcrTextPostProcessor postProcessor;
    private final OcrFieldValidator fieldValidator;
    private final OcrConfidenceCalculator confidenceCalculator;
    private final OcrMetricsService metricsService;

    public OcrService(List<OcrProvider> ocrProviders,
                      FieldExtractionService fieldExtractionService,
                      AdaptiveFieldExtractionService adaptiveExtractionService,
                      OcrTextPostProcessor postProcessor,
                      OcrFieldValidator fieldValidator,
                      OcrConfidenceCalculator confidenceCalculator,
                      OcrMetricsService metricsService) {
        this.ocrProviders = ocrProviders;
        this.fieldExtractionService = fieldExtractionService;
        this.adaptiveExtractionService = adaptiveExtractionService;
        this.postProcessor = postProcessor;
        this.fieldValidator = fieldValidator;
        this.confidenceCalculator = confidenceCalculator;
        this.metricsService = metricsService;
    }

    public Map<String, Object> parseInvoiceImage(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        metricsService.recordRequest();
        
        try {
            String filename = file != null ? file.getOriginalFilename() : "unknown";
            long size = file != null ? file.getSize() : 0;
            String mime = file != null ? file.getContentType() : "unknown";

            log.info("OCR pipeline invoked. file='{}' size={}B mime={}", filename, size, mime);

            String previewText = "";
            String contentType = mime != null ? mime : "";
            String fileName = filename != null ? filename : "";
            
            // Try each provider until one succeeds (skip test and noop providers unless explicitly enabled)
            for (OcrProvider provider : ocrProviders) {
                try {
                    if (provider != null && provider.supports(contentType, fileName)) {
                        String providerName = provider.getName();
                        
                        // Skip test provider unless explicitly enabled
                        if ("test".equals(providerName)) {
                            String testMode = System.getProperty("ocr.test.enabled", System.getenv("OCR_TEST_ENABLED"));
                            if (!"true".equalsIgnoreCase(testMode)) {
                                log.debug("Skipping test provider (not explicitly enabled)");
                                continue;
                            }
                            log.warn("TestOcrProvider is enabled - this will return sample data instead of real OCR!");
                        }
                        
                        // Skip noop provider unless it's the only one
                        if ("noop".equals(providerName) && ocrProviders.size() > 1) {
                            log.debug("Skipping noop provider, trying other providers first");
                            continue;
                        }
                        
                        log.info("Trying OCR provider: {} for file: {} (contentType: {}, size: {} bytes)", 
                                providerName, fileName, contentType, file.getSize());
                        var response = provider.extract(file);
                        if (response != null && response.getOcr() != null) {
                            String extractedText = response.getOcr().getTextPreview();
                            
                            // If textPreview is empty, try fullText as fallback
                            if ((extractedText == null || extractedText.isBlank()) && response.getOcr().getFullText() != null) {
                                extractedText = response.getOcr().getFullText();
                                log.debug("Using fullText as fallback for provider: {}", providerName);
                            }
                            
                            // Reject test provider's sample text if it looks like the default sample
                            if ("test".equals(providerName) && extractedText != null) {
                                if (extractedText.contains("شرکت نمونه") && extractedText.contains("مشتری نمونه") 
                                    && extractedText.contains("12345") && extractedText.contains("1234567890")) {
                                    log.warn("TestOcrProvider returned sample text - this is not real OCR! " +
                                            "Please install a real OCR provider (PDFBox for PDFs, Tesseract for images) " +
                                            "or set OCR_TEST_ENABLED=true to explicitly enable test mode.");
                                    // Continue to next provider instead of using test data
                                    continue;
                                }
                            }
                            
                            // Check if text is valid and non-empty
                            if (extractedText != null && !extractedText.isBlank() 
                                    && !extractedText.equals("OCR disabled (noop)")
                                    && !extractedText.contains("OCR provider not available")
                                    && extractedText.trim().length() > 10) { // At least 10 characters to be considered valid
                                previewText = extractedText;
                                log.info("Successfully extracted text using provider: {} (length: {})", providerName, previewText.length());
                                break; // Use the first successful provider
                            } else {
                                // If pdfbox returned empty text, it might be an image-based PDF
                                // Continue to try other providers (like Tesseract for images)
                                if ("pdfbox".equals(providerName) && (extractedText == null || extractedText.isBlank())) {
                                    log.warn("PdfOcrProvider returned empty text - PDF might be image-based. Trying other providers...");
                                } else {
                                    log.debug("Provider {} returned empty or placeholder text (length: {}), trying next provider", 
                                            providerName, extractedText != null ? extractedText.length() : 0);
                                }
                                // Continue to next provider
                            }
                        } else {
                            log.debug("Provider {} returned null response or null OCR data, trying next provider", providerName);
                        }
                    }
                } catch (Exception e) {
                    String providerName = provider != null ? provider.getName() : "unknown";
                    String errorDetails = getErrorDetails(e);
                    log.warn("OCR provider {} failed: {} - {}", providerName, e.getMessage(), errorDetails);
                    
                    // ثبت خطا در metrics
                    long processingTime = System.currentTimeMillis() - startTime;
                    String errorType = e.getClass().getSimpleName();
                    metricsService.recordFailure(providerName, errorType, processingTime);
                    
                    // اگر external provider خطا داد، به provider بعدی می‌رویم (fallback)
                    if ("external-ocr-space".equals(providerName)) {
                        log.info("External OCR API failed ({}), falling back to local providers (ImageOcrProvider, etc.)...", errorDetails);
                    } else if ("tesseract-image".equals(providerName)) {
                        log.info("ImageOcrProvider (Tesseract) failed ({}), trying other providers...", errorDetails);
                    }
                    // Continue to next provider - fallback mechanism
                }
            }
            
            if (previewText.isBlank()) {
                log.warn("No OCR provider was able to extract text from file: {}", filename);
                // ثبت خطای کلی
                long processingTime = System.currentTimeMillis() - startTime;
                metricsService.recordFailure("all", "NoTextExtracted", processingTime);
            } else {
                // Post-process the extracted text to correct common OCR errors
                if (postProcessor != null) {
                    String originalText = previewText;
                    previewText = postProcessor.postProcess(previewText);
                    previewText = postProcessor.normalizeForExtraction(previewText);
                    if (!originalText.equals(previewText)) {
                        log.debug("Post-processing changed text length: {} -> {}", originalText.length(), previewText.length());
                    }
                }
                
                log.info("Extracted text length: {} characters. Preview: {}", previewText.length(), 
                        previewText.length() > 100 ? previewText.substring(0, 100) + "..." : previewText);
                log.debug("Full extracted text (first 500 chars): {}", 
                        previewText.length() > 500 ? previewText.substring(0, 500) + "..." : previewText);
                log.debug("Full extracted text (last 500 chars): {}", 
                        previewText.length() > 500 ? "..." + previewText.substring(previewText.length() - 500) : previewText);
            }

            Map<String, Object> parsed = new HashMap<>();
            try {
                // Only attempt parsing if we have extracted text
                if (previewText != null && !previewText.isBlank()) {
                    // Use adaptive extraction service which learns from actual invoice structure
                    if (adaptiveExtractionService != null) {
                        log.info("Using AdaptiveFieldExtractionService for field extraction");
                        parsed = adaptiveExtractionService.extractAdaptive(previewText);
                        if (parsed == null) {
                            parsed = new HashMap<>();
                            log.warn("AdaptiveFieldExtractionService returned null, using empty map");
                        } else {
                            log.info("Adaptive field extraction result: {} fields extracted", parsed.size());
                            // Log extracted fields for debugging
                            int nonEmptyFields = 0;
                            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                                Object value = entry.getValue();
                                if (value != null && !value.toString().isBlank() && !(value instanceof List && ((List<?>) value).isEmpty())) {
                                    nonEmptyFields++;
                                    log.info("  - {}: {}", entry.getKey(), 
                                        value instanceof List ? ((List<?>) value).size() + " items" : 
                                        value.toString().length() > 100 ? value.toString().substring(0, 100) + "..." : value);
                                }
                            }
                            log.info("Adaptive extraction: {} non-empty fields out of {} total fields", nonEmptyFields, parsed.size());
                        }
                    } else if (fieldExtractionService != null) {
                        // Fallback to base extraction service
                        log.info("Using FieldExtractionService for field extraction");
                        parsed = fieldExtractionService.extract(previewText);
                        if (parsed == null) {
                            parsed = new HashMap<>();
                            log.warn("FieldExtractionService returned null, using empty map");
                        } else {
                            log.info("Field extraction result: {} fields extracted", parsed.size());
                            // Log extracted fields for debugging
                            int nonEmptyFields = 0;
                            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                                Object value = entry.getValue();
                                if (value != null && !value.toString().isBlank() && !(value instanceof List && ((List<?>) value).isEmpty())) {
                                    nonEmptyFields++;
                                    log.info("  - {}: {}", entry.getKey(), 
                                        value instanceof List ? ((List<?>) value).size() + " items" : 
                                        value.toString().length() > 100 ? value.toString().substring(0, 100) + "..." : value);
                                }
                            }
                            log.info("Base extraction: {} non-empty fields out of {} total fields", nonEmptyFields, parsed.size());
                        }
                    } else {
                        log.warn("No field extraction service available! Both FieldExtractionService and AdaptiveFieldExtractionService are null");
                    }
                } else {
                    log.warn("Cannot parse fields: previewText is empty or null. Text extraction may have failed.");
                }
                
                // اعتبارسنجی فیلدهای استخراج شده
                if (fieldValidator != null && !parsed.isEmpty()) {
                    OcrFieldValidator.ValidationResult validationResult = fieldValidator.validate(parsed);
                    if (!validationResult.isValid()) {
                        log.warn("OCR field validation found errors: {}", validationResult.getSummary());
                        // اضافه کردن validation result به parsed برای استفاده در frontend
                        Map<String, Object> validationMeta = new HashMap<>();
                        validationMeta.put("errors", validationResult.getErrors());
                        validationMeta.put("warnings", validationResult.getWarnings());
                        validationMeta.put("isValid", validationResult.isValid());
                        validationMeta.put("hasWarnings", validationResult.hasWarnings());
                        parsed.put("_validation", validationMeta);
                    } else if (validationResult.hasWarnings()) {
                        log.info("OCR field validation found warnings: {}", validationResult.getSummary());
                        Map<String, Object> validationMeta = new HashMap<>();
                        validationMeta.put("warnings", validationResult.getWarnings());
                        validationMeta.put("isValid", true);
                        validationMeta.put("hasWarnings", true);
                        parsed.put("_validation", validationMeta);
                    } else {
                        log.debug("OCR field validation passed");
                    }
                }
                
                // محاسبه confidence score برای فیلدهای استخراج شده
                if (confidenceCalculator != null && !parsed.isEmpty()) {
                    Map<String, Integer> fieldConfidences = confidenceCalculator.calculateConfidence(parsed);
                    int overallConfidence = confidenceCalculator.calculateOverallConfidence(fieldConfidences);
                    
                    log.info("OCR confidence scores calculated: overall={}%", overallConfidence);
                    log.debug("Field confidences: {}", fieldConfidences);
                    
                    // اضافه کردن confidence scores به parsed برای استفاده در frontend
                    Map<String, Object> confidenceMeta = new HashMap<>();
                    confidenceMeta.put("overall", overallConfidence);
                    confidenceMeta.put("fields", fieldConfidences);
                    parsed.put("_confidence", confidenceMeta);
                }
            } catch (Exception e) {
                log.error("Field extraction failed", e);
                parsed = new HashMap<>();
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("receivedAt", Instant.now().toString());
            meta.put("fileName", filename != null ? filename : "unknown");
            meta.put("sizeBytes", size);
            meta.put("contentType", mime != null ? mime : "unknown");
            
            Map<String, Object> ocr = new HashMap<>();
            ocr.put("textPreview", previewText); // Return full text, not limited preview
            ocr.put("fullText", previewText); // Include full text for analysis
            log.debug("OcrService: Setting textPreview and fullText with {} characters", previewText.length());
            
            // ثبت metrics
            long processingTime = System.currentTimeMillis() - startTime;
            int textLength = previewText != null ? previewText.length() : 0;
            int fieldsExtracted = parsed != null ? parsed.size() : 0;
            
            if (previewText != null && !previewText.isBlank()) {
                String providerName = (String) ocr.get("provider");
                if (providerName == null) {
                    providerName = "unknown";
                }
                metricsService.recordSuccess(providerName, processingTime, textLength, fieldsExtracted);
                
                // ثبت استخراج فیلدها
                if (parsed != null) {
                    parsed.forEach((key, value) -> {
                        if (value != null && !value.toString().isBlank() && !key.startsWith("_")) {
                            metricsService.recordFieldExtraction(key);
                        }
                    });
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("meta", meta);
            result.put("ocr", ocr);
            result.put("parsed", parsed);
            return result;
        } catch (Exception e) {
            log.error("Error in parseInvoiceImage", e);
            
            // ثبت خطا در metrics
            long processingTime = System.currentTimeMillis() - startTime;
            String errorType = e.getClass().getSimpleName();
            metricsService.recordFailure("system", errorType, processingTime);
            
            // Return a safe default response instead of throwing
            Map<String, Object> meta = new HashMap<>();
            meta.put("receivedAt", Instant.now().toString());
            meta.put("fileName", "unknown");
            meta.put("sizeBytes", 0L);
            meta.put("contentType", "unknown");
            meta.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            
            Map<String, Object> ocr = new HashMap<>();
            ocr.put("textPreview", "");
            
            Map<String, Object> result = new HashMap<>();
            result.put("meta", meta);
            result.put("ocr", ocr);
            result.put("parsed", new HashMap<>());
            return result;
        }
    }

    private static String preview(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
    
    /**
     * استخراج جزئیات خطا برای logging بهتر
     */
    private static String getErrorDetails(Exception e) {
        if (e == null) {
            return "Unknown error";
        }
        
        StringBuilder details = new StringBuilder();
        details.append("Type: ").append(e.getClass().getSimpleName());
        
        if (e instanceof org.springframework.web.client.RestClientException) {
            if (e instanceof org.springframework.web.client.ResourceAccessException) {
                details.append(" | Connection error");
                Throwable cause = e.getCause();
                if (cause != null) {
                    details.append(" | Cause: ").append(cause.getClass().getSimpleName());
                    if (cause.getMessage() != null) {
                        details.append(" (").append(cause.getMessage()).append(")");
                    }
                }
            } else if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpEx = (org.springframework.web.client.HttpClientErrorException) e;
                details.append(" | HTTP ").append(httpEx.getStatusCode().value());
                if (httpEx.getResponseBodyAsString() != null && !httpEx.getResponseBodyAsString().isBlank()) {
                    String body = httpEx.getResponseBodyAsString();
                    details.append(" | Response: ").append(body.length() > 100 ? body.substring(0, 100) + "..." : body);
                }
            } else if (e instanceof org.springframework.web.client.HttpServerErrorException) {
                org.springframework.web.client.HttpServerErrorException httpEx = (org.springframework.web.client.HttpServerErrorException) e;
                details.append(" | HTTP ").append(httpEx.getStatusCode().value());
            }
        } else if (e.getCause() != null) {
            details.append(" | Cause: ").append(e.getCause().getClass().getSimpleName());
            if (e.getCause().getMessage() != null) {
                String causeMsg = e.getCause().getMessage();
                details.append(" (").append(causeMsg.length() > 100 ? causeMsg.substring(0, 100) + "..." : causeMsg).append(")");
            }
        }
        
        return details.toString();
    }
}


