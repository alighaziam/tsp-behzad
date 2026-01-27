package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.Map;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class NoopOcrProvider implements OcrProvider {
    @Override
    public boolean supports(String contentType, String filename) {
        // Only support if no other provider can handle it
        // This will be used as last resort fallback
        return true; // fallback - but should be tried last
    }

    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        String sample = "";
        try {
            byte[] bytes = file.getBytes();
            sample = Base64.getEncoder().encodeToString(bytes.length > 48 ? java.util.Arrays.copyOf(bytes, 48) : bytes);
        } catch (Exception ignored) {}
        return OcrUploadResponse.builder()
                .meta(Map.of("provider", "noop", "filename", file.getOriginalFilename(), "sample", sample))
                .ocr(OcrUploadResponse.OcrData.builder().textPreview("OCR disabled (noop)").build())
                .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                .build();
    }

    @Override
    public String getName() {
        return "noop";
    }
}


