package ir.tax.trustedtaxpayers.biz.service.ocr.impl;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrFieldExtractionService;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class PluggableOcrService implements OcrService {

    private final List<OcrProvider> providers;
    private final OcrFieldExtractionService extractionService;

    @Override
    public OcrUploadResponse process(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        for (OcrProvider provider : providers) {
            try {
                if (provider.supports(contentType, filename)) {
                    log.info("Using OCR provider: {}", provider.getName());
                    OcrUploadResponse resp = provider.extract(file);
                    // اگر فیلدی استخراج نشده ولی متن موجود است، با regex استخراج کن
                    if (resp != null && (resp.getParsed() == null
                            || (resp.getParsed().getInvoiceNumber() == null
                            && resp.getParsed().getSellerTaxId() == null
                            && resp.getParsed().getBuyerTaxId() == null
                            && resp.getParsed().getTotalAmount() == null))) {
                        String text = resp.getOcr() != null ? resp.getOcr().getTextPreview() : null;
                        if (text != null && !text.isBlank()) {
                            var parsed = extractionService.extract(text);
                            return OcrUploadResponse.builder()
                                    .meta(resp.getMeta())
                                    .ocr(resp.getOcr())
                                    .parsed(parsed)
                                    .build();
                        }
                    }
                    return resp;
                }
            } catch (Exception e) {
                log.warn("Provider {} failed: {}", provider.getName(), e.getMessage());
            }
        }
        // fallback: noop
        return OcrUploadResponse.builder()
                .meta(java.util.Map.of(
                        "provider", "noop",
                        "filename", filename,
                        "contentType", contentType
                ))
                .ocr(OcrUploadResponse.OcrData.builder().textPreview("OCR provider not available").build())
                .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                .build();
    }
}


