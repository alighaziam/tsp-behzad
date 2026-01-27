package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Test OCR provider that returns sample invoice text for testing field extraction.
 * This provider is only used when no real OCR provider (like Tesseract) is available.
 */
@Slf4j
// @Component - DISABLED: This provider returns fake test data. Use real OCR providers instead.
// Uncomment and set OCR_TEST_ENABLED=true only for testing field extraction logic.
@Order(Ordered.LOWEST_PRECEDENCE - 5) // Try before noop but after real providers
public class TestOcrProvider implements OcrProvider {
    
    // Sample invoice text in Persian for testing
    private static final String SAMPLE_INVOICE_TEXT = """
        صورتحساب الکترونیکی
        شماره فاکتور: 12345
        تاریخ: 1403/08/25
        
        فروشنده:
        نام: شرکت نمونه
        شناسه ملی: 1234567890
        
        خریدار:
        نام: مشتری نمونه
        شناسه ملی خریدار: 9876543210
        
        جمع کل: 1000000
        مبلغ کل: 1000000
        
        اقلام:
        کالای 1 - تعداد: 10 - قیمت: 100000
        """;

    @Override
    public boolean supports(String contentType, String filename) {
        // Check if test mode is explicitly enabled
        // By default, test provider is DISABLED to allow real OCR providers to work
        String testMode = System.getProperty("ocr.test.enabled", System.getenv("OCR_TEST_ENABLED"));
        if (!"true".equalsIgnoreCase(testMode)) {
            log.debug("TestOcrProvider disabled (test mode not enabled). Set ocr.test.enabled=true to enable.");
            return false;
        }
        
        log.warn("TestOcrProvider is enabled! This will return sample data instead of real OCR. " +
                "Disable by setting ocr.test.enabled=false or removing the environment variable.");
        
        // Only support if explicitly enabled
        String ct = contentType != null ? contentType : "";
        String name = filename != null ? filename.toLowerCase() : "";
        return ct.startsWith("image/") || ct.equals("application/pdf") 
                || name.endsWith(".jpg") || name.endsWith(".jpeg") 
                || name.endsWith(".png") || name.endsWith(".pdf");
    }

    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        log.info("TestOcrProvider: Returning sample invoice text for testing");
        return OcrUploadResponse.builder()
                .meta(Map.of(
                        "provider", "test",
                        "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "test.pdf",
                        "contentType", file.getContentType() != null ? file.getContentType() : "application/pdf",
                        "note", "This is a test provider returning sample data"
                ))
                .ocr(OcrUploadResponse.OcrData.builder()
                        .textPreview(SAMPLE_INVOICE_TEXT)
                        .build())
                .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                .build();
    }

    @Override
    public String getName() {
        return "test";
    }
}

