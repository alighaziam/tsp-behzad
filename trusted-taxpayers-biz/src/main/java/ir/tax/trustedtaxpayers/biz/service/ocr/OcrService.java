package ir.tax.trustedtaxpayers.biz.service.ocr;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    OcrUploadResponse process(MultipartFile file);
}


