package ir.tax.trustedtaxpayers.biz.service.ocr;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrProvider {
    boolean supports(String contentType, String filename);
    OcrUploadResponse extract(MultipartFile file);
    String getName();
}

