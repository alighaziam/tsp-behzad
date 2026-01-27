package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

/**
 * OCR Provider که از API خارجی (OCR.space) استفاده می‌کند
 * این provider اولویت بالایی دارد و اگر فعال باشد، قبل از providers محلی استفاده می‌شود
 * 
 * OCR.space یک API رایگان است که:
 * - بدون نیاز به API Key (در حالت رایگان)
 * - پشتیبانی از فارسی
 * - محدودیت: 25,000 درخواست در روز (رایگان)
 * 
 * اگر این API کار نکرد یا در دسترس نبود، به providers محلی fallback می‌شود
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE - 5) // اولویت بالاتر از providers محلی، اما بعد از PdfOcrProvider برای PDFهای متنی
public class ExternalOcrProvider implements OcrProvider {
    
    private final RestTemplate restTemplate;
    
    // OCR.space API endpoint (رایگان و بدون نیاز به API Key)
    private static final String OCR_SPACE_API_URL = "https://api.ocr.space/parse/image";
    
    // فعال/غیرفعال بودن این provider (از طریق environment variable)
    @Value("${ocr.external.enabled:true}")
    private boolean enabled;
    
    // استفاده از API Key اختیاری (اگر داشته باشید، rate limit بالاتر است)
    @Value("${ocr.external.api-key:}")
    private String apiKey;
    
    public ExternalOcrProvider(RestTemplateBuilder restTemplateBuilder,
                                @Value("${ocr.external.timeout.connect:10000}") int connectTimeout,
                                @Value("${ocr.external.timeout.read:60000}") int readTimeout) {
        // ساخت RestTemplate با timeout مناسب برای API خارجی
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
    
    @Override
    public boolean supports(String contentType, String filename) {
        if (!enabled) {
            log.debug("ExternalOcrProvider is disabled");
            return false;
        }
        
        String ct = contentType != null ? contentType.toLowerCase() : "";
        String name = filename != null ? filename.toLowerCase() : "";
        
        // بررسی بر اساس Content-Type
        boolean supportedByContentType = ct.startsWith("image/") 
                || ct.equals("image/png")
                || ct.equals("image/jpeg")
                || ct.equals("image/jpg")
                || ct.equals("application/pdf");
        
        // بررسی بر اساس پسوند فایل (برای مواردی که Content-Type درست تشخیص داده نشده)
        boolean supportedByExtension = name.endsWith(".png") 
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".pdf");
        
        boolean isSupported = supportedByContentType || supportedByExtension;
        
        if (isSupported) {
            log.debug("ExternalOcrProvider supports file: contentType={}, filename={}", contentType, filename);
        }
        
        return isSupported;
    }
    
    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        if (!enabled) {
            throw new RuntimeException("ExternalOcrProvider is disabled");
        }
        
        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            log.info("ExternalOcrProvider: Attempting OCR via external API for file: {}", filename);
            
            // آماده‌سازی درخواست
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // تبدیل فایل به base64 یا استفاده از multipart
            try {
                // روش 1: استفاده از multipart/form-data (بهتر برای فایل‌های بزرگ)
                ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return filename != null ? filename : "file";
                    }
                };
                
                body.add("file", fileResource);
                
                // پارامترهای API
                body.add("language", "fas"); // فارسی
                body.add("isOverlayRequired", "false");
                body.add("detectOrientation", "true");
                body.add("scale", "true");
                body.add("OCREngine", "2"); // Engine 2 برای دقت بهتر
                
                // اگر API Key دارید، اضافه کنید
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    body.add("apikey", apiKey);
                    log.debug("Using API key for OCR.space");
                } else {
                    log.debug("Using OCR.space free tier (no API key)");
                }
                
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                
                // ارسال درخواست با retry mechanism
                log.debug("Sending OCR request to external API...");
                ResponseEntity<Map> response = null;
                Exception lastException = null;
                final int MAX_RETRIES = 3;
                final int INITIAL_RETRY_DELAY_MS = 1000; // 1 second
                int retryDelayMs = INITIAL_RETRY_DELAY_MS;
                
                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                    try {
                        response = restTemplate.exchange(
                                OCR_SPACE_API_URL,
                                HttpMethod.POST,
                                requestEntity,
                                Map.class
                        );
                        // اگر موفق بود، از loop خارج شو
                        break;
                    } catch (org.springframework.web.client.ResourceAccessException e) {
                        // خطای اتصال (timeout, connection refused, etc.)
                        lastException = e;
                        if (attempt < MAX_RETRIES) {
                            log.warn("External OCR API connection error (attempt {}/{}): {}. Retrying in {}ms...", 
                                    attempt, MAX_RETRIES, e.getMessage(), retryDelayMs);
                            try {
                                Thread.sleep(retryDelayMs);
                                retryDelayMs *= 2; // Exponential backoff
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Retry interrupted", ie);
                            }
                        } else {
                            log.error("External OCR API failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                        }
                    } catch (org.springframework.web.client.HttpServerErrorException e) {
                        // خطای سرور (5xx)
                        lastException = e;
                        if (attempt < MAX_RETRIES && e.getStatusCode().value() >= 500) {
                            log.warn("External OCR API server error (attempt {}/{}): {}. Retrying in {}ms...", 
                                    attempt, MAX_RETRIES, e.getStatusCode(), retryDelayMs);
                            try {
                                Thread.sleep(retryDelayMs);
                                retryDelayMs *= 2;
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Retry interrupted", ie);
                            }
                        } else {
                            log.error("External OCR API server error after {} attempts: {}", MAX_RETRIES, e.getStatusCode());
                            throw e; // Don't retry for non-5xx errors
                        }
                    } catch (Exception e) {
                        // سایر خطاها
                        lastException = e;
                        log.error("External OCR API unexpected error (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                        if (attempt < MAX_RETRIES) {
                            try {
                                Thread.sleep(retryDelayMs);
                                retryDelayMs *= 2;
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Retry interrupted", ie);
                            }
                        } else {
                            throw new RuntimeException("External OCR API failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), e);
                        }
                    }
                }
                
                if (response == null) {
                    throw new RuntimeException("External OCR API failed after " + MAX_RETRIES + " attempts", lastException);
                }
                
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();
                    
                    // استخراج متن از پاسخ
                    String extractedText = extractTextFromResponse(responseBody);
                    
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        log.info("ExternalOcrProvider: Successfully extracted {} characters via external API", extractedText.length());
                        log.debug("ExternalOcrProvider: First 200 chars: {}", 
                                extractedText.length() > 200 ? extractedText.substring(0, 200) + "..." : extractedText);
                        
                        return OcrUploadResponse.builder()
                                .meta(Map.of(
                                        "provider", "external-ocr-space",
                                        "filename", filename != null ? filename : "unknown",
                                        "contentType", contentType != null ? contentType : "unknown",
                                        "textLength", extractedText.length(),
                                        "fileSize", file.getSize(),
                                        "apiUsed", "ocr.space"
                                ))
                                .ocr(OcrUploadResponse.OcrData.builder()
                                        .textPreview(extractedText)
                                        .build())
                                .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                                .build();
                    } else {
                        log.warn("ExternalOcrProvider: API returned empty text");
                        throw new RuntimeException("External OCR API returned empty text");
                    }
                } else {
                    log.warn("ExternalOcrProvider: API returned non-success status: {}", response.getStatusCode());
                    throw new RuntimeException("External OCR API returned error: " + response.getStatusCode());
                }
                
            } catch (RestClientException e) {
                log.warn("ExternalOcrProvider: Failed to call external API: {}", e.getMessage());
                // اگر API خارجی کار نکرد، exception throw می‌کنیم تا provider بعدی امتحان شود
                throw new RuntimeException("External OCR API unavailable: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("ExternalOcrProvider: Unexpected error: {}", e.getMessage(), e);
                throw new RuntimeException("External OCR failed: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("ExternalOcrProvider failed: {}", e.getMessage(), e);
            // اگر خطا رخ داد، exception throw می‌کنیم تا provider بعدی امتحان شود
            throw new RuntimeException("Failed to extract text using external API: " + e.getMessage(), e);
        }
    }
    
    /**
     * استخراج متن از پاسخ API OCR.space
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            // ساختار پاسخ OCR.space:
            // {
            //   "ParsedResults": [
            //     {
            //       "ParsedText": "...",
            //       "TextOverlay": {...}
            //     }
            //   ],
            //   "OCRExitCode": 1,
            //   "IsErroredOnProcessing": false,
            //   "ErrorMessage": null
            // }
            
            Object parsedResultsObj = responseBody.get("ParsedResults");
            if (parsedResultsObj == null) {
                log.warn("ExternalOcrProvider: No ParsedResults in response");
                return null;
            }
            
            // بررسی خطا
            Object isErrored = responseBody.get("IsErroredOnProcessing");
            if (isErrored != null && Boolean.TRUE.equals(isErrored)) {
                String errorMessage = (String) responseBody.get("ErrorMessage");
                log.warn("ExternalOcrProvider: API returned error: {}", errorMessage);
                return null;
            }
            
            // استخراج متن از اولین نتیجه
            if (parsedResultsObj instanceof java.util.List) {
                java.util.List<Map<String, Object>> parsedResults = (java.util.List<Map<String, Object>>) parsedResultsObj;
                if (!parsedResults.isEmpty()) {
                    Map<String, Object> firstResult = parsedResults.get(0);
                    Object parsedText = firstResult.get("ParsedText");
                    if (parsedText != null) {
                        return parsedText.toString();
                    }
                }
            }
            
            log.warn("ExternalOcrProvider: Could not extract text from response structure");
            return null;
            
        } catch (Exception e) {
            log.error("ExternalOcrProvider: Error parsing API response: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getName() {
        return "external-ocr-space";
    }
}

