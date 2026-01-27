package ir.tax.trustedtaxpayers.biz.service.ocr;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 20)
@ConditionalOnProperty(name = "ocr.provider", havingValue = "tesseract", matchIfMissing = false)
@Slf4j
public class TesseractOcrProvider implements OcrProvider {

    private final String language;

    public TesseractOcrProvider() {
        this.language = System.getProperty("ocr.language", System.getenv().getOrDefault("OCR_LANGUAGE", "fas"));
    }

    @Override
    public boolean supports(String contentType, String filename) {
        String ct = contentType != null ? contentType : "";
        String name = filename != null ? filename.toLowerCase() : "";
        return ct.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
    }

    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        try {
            Class<?> tesseractClass = Class.forName("net.sourceforge.tess4j.Tesseract");
            Object tesseract = tesseractClass.getDeclaredConstructor().newInstance();
            tesseractClass.getMethod("setLanguage", String.class).invoke(tesseract, language);
            String dataPath = System.getProperty("tessdata.path", System.getenv("TESSDATA_PREFIX"));
            if (dataPath != null && !dataPath.isBlank()) {
                tesseractClass.getMethod("setDatapath", String.class).invoke(tesseract, dataPath);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }
            String text = String.valueOf(tesseractClass.getMethod("doOCR", BufferedImage.class).invoke(tesseract, image));
            return OcrUploadResponse.builder()
                    .meta(java.util.Map.of(
                            "provider", "tesseract",
                            "filename", file.getOriginalFilename(),
                            "contentType", file.getContentType()
                    ))
                    .ocr(OcrUploadResponse.OcrData.builder()
                            .textPreview(text != null && text.length() > 800 ? text.substring(0, 800) : text)
                            .build())
                    .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                    .build();
        } catch (ClassNotFoundException e) {
            log.error("tess4j not on classpath; add dependency or disable tesseract provider", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.warn("TesseractOcrProvider failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "tesseract";
    }
}


