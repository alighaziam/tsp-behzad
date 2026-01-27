package ir.tax.trustedtaxpayers.biz.service.ocr.impl;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NoopOcrService implements OcrService {

    @Override
    public OcrUploadResponse process(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            String base64Sample = Base64.getEncoder().encodeToString(
                    bytes.length > 64 ? slice(bytes, 64) : bytes
            );

            String preview = "OCR preview is not enabled. Received file: " + file.getOriginalFilename()
                    + " (" + file.getSize() + " bytes, contentType=" + file.getContentType() + ")";

            return OcrUploadResponse.builder()
                    .meta(Map.of(
                            "filename", file.getOriginalFilename(),
                            "size", file.getSize(),
                            "contentType", file.getContentType(),
                            "sampleBase64", base64Sample
                    ))
                    .ocr(OcrUploadResponse.OcrData.builder()
                            .textPreview(preview)
                            .build())
                    .parsed(OcrUploadResponse.ParsedData.builder()
                            .sellerName(null)
                            .sellerTaxId(null)
                            .buyerName(null)
                            .buyerTaxId(null)
                            .pattern(null)
                            .items(List.of())
                            .invoiceNumber(null)
                            .issueDate(null)
                            .totalAmount(null)
                            .taxpayerId(null)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Failed to process OCR upload", e);
            // Return minimal error-like response to client
            return OcrUploadResponse.builder()
                    .meta(Map.of(
                            "filename", file != null ? file.getOriginalFilename() : "unknown",
                            "error", e.getMessage()
                    ))
                    .ocr(OcrUploadResponse.OcrData.builder()
                            .textPreview("خطا در پردازش فایل برای OCR")
                            .build())
                    .parsed(OcrUploadResponse.ParsedData.builder()
                            .items(List.of())
                            .build())
                    .build();
        }
    }

    private byte[] slice(byte[] source, int maxLen) {
        int len = Math.min(source.length, maxLen);
        byte[] out = new byte[len];
        System.arraycopy(source, 0, out, 0, len);
        return out;
    }
}


