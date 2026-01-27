package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Fallback برای PDFهای تصویری که PdfOcrProvider نتوانست پردازش کند
public class FaTesseractOcrProvider implements OcrProvider {
    @Override
    public boolean supports(String contentType, String filename) {
        String ct = contentType != null ? contentType : "";
        String name = filename != null ? filename.toLowerCase() : "";
        // فقط PDFها را پشتیبانی می‌کند (به عنوان fallback برای PDFهای تصویری)
        // تصاویر توسط ImageOcrProvider پردازش می‌شوند
        return ct.equals("application/pdf") || name.endsWith(".pdf");
    }

    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        try {
            // تلاش برای بارگذاری Tesseract به صورت reflection تا وابستگی اختیاری باشد
            Class<?> tesseractCls = Class.forName("net.sourceforge.tess4j.Tesseract");
            Object tesseract = tesseractCls.getDeclaredConstructor().newInstance();
            // تنظیم زبان فارسی + انگلیسی برای OCR بهتر
            try {
                tesseractCls.getMethod("setLanguage", String.class).invoke(tesseract, "fas+eng");
                log.debug("FaTesseractOcrProvider: Set Tesseract language to fas+eng");
            } catch (NoSuchMethodException ignored) {
                // قدیمی‌تر: setLanguage(String)
            }
            
            // تنظیمات پیشرفته Tesseract برای دقت بهتر در PDFهای تصویری
            try {
                // PSM 6: Assume a single uniform block of text (بهترین برای فاکتورها)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_pageseg_mode", "6");
                
                // بهبود دقت برای متن فارسی - whitelist کامل‌تر با کاراکترهای بیشتر
                String persianChars = "۰۱۲۳۴۵۶۷۸۹ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی";
                String englishChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                String specialChars = ".,:;/-()[]{}%+×÷=<>|";
                String arabicChars = "أإآؤئ";
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_char_whitelist", persianChars + englishChars + specialChars + arabicChars);
                
                // افزایش DPI برای کیفیت بهتر (300 DPI برای اسناد با کیفیت بالا)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "user_defined_dpi", "300");
                
                // بهبود دقت برای تشخیص اعداد
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "classify_bln_numeric_mode", "1");
                
                // بهبود دقت برای جداول
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "preserve_interword_spaces", "1");
                
                // بهبود دقت برای متن فارسی (language-specific settings)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_create_hocr", "0");
                
                // فعال‌سازی DAWG برای بهبود دقت
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_system_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_freq_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_number_dawg", "1");
                
                log.debug("FaTesseractOcrProvider: Configured Tesseract with advanced optimized settings (PSM 6, DPI 300, enhanced whitelist, DAWG enabled)");
            } catch (Exception e) {
                log.debug("FaTesseractOcrProvider: Could not set all Tesseract variables: {}", e.getMessage());
                // Fallback to basic settings
                try {
                    tesseractCls.getMethod("setTessVariable", String.class, String.class)
                            .invoke(tesseract, "tessedit_pageseg_mode", "6");
                } catch (Exception e2) {
                    log.debug("FaTesseractOcrProvider: Could not set basic Tesseract variables: {}", e2.getMessage());
                }
            }
            
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            
            // این provider فقط برای PDFهای تصویری استفاده می‌شود
            // تصاویر (PNG, JPG) توسط ImageOcrProvider پردازش می‌شوند
            String text = "";
            // For PDFs, convert pages to images first using PDFBox
            try {
                Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
                Class<?> pdfRendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");
                Class<?> bufferedImageClass = Class.forName("java.awt.image.BufferedImage");
                Class<?> imageIOClass = Class.forName("javax.imageio.ImageIO");
                
                // Save PDF to temp file
                Path pdfTmp = Files.createTempFile("tsp-ocr-pdf-", ".pdf");
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, pdfTmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                
                try {
                    // Load PDF
                    Object document;
                    try {
                        Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
                        document = loaderClass.getMethod("loadPDF", java.io.File.class)
                                .invoke(null, pdfTmp.toFile());
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        document = pdDocumentClass.getMethod("load", java.io.File.class)
                                .invoke(null, pdfTmp.toFile());
                    }
                    
                    try {
                        int pageCount = (Integer) pdDocumentClass.getMethod("getNumberOfPages").invoke(document);
                        Object renderer = pdfRendererClass.getConstructor(pdDocumentClass).newInstance(document);
                        StringBuilder allText = new StringBuilder();
                        
                        // Extract text from each page
                        for (int page = 0; page < pageCount; page++) {
                            try {
                                Object image = pdfRendererClass.getMethod("renderImageWithDPI", int.class, float.class)
                                        .invoke(renderer, page, 300f);
                                
                                Path imgTmp = Files.createTempFile("tsp-ocr-page-", ".png");
                                try {
                                    // ImageIO.write signature: write(RenderedImage im, String formatName, File output)
                                    // Try different method signatures
                                    try {
                                        imageIOClass.getMethod("write", bufferedImageClass, String.class, java.io.File.class)
                                                .invoke(null, image, "PNG", imgTmp.toFile());
                                    } catch (NoSuchMethodException e) {
                                        // Try alternative signature: write(RenderedImage im, String formatName, OutputStream output)
                                        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imgTmp.toFile())) {
                                            imageIOClass.getMethod("write", bufferedImageClass, String.class, java.io.OutputStream.class)
                                                    .invoke(null, image, "PNG", fos);
                                        }
                                    }
                                    
                                    String pageText = String.valueOf(tesseractCls.getMethod("doOCR", java.io.File.class)
                                            .invoke(tesseract, imgTmp.toFile()));
                                    
                                    if (pageText != null && !pageText.trim().isEmpty()) {
                                        allText.append(pageText).append("\n");
                                    }
                                } finally {
                                    Files.deleteIfExists(imgTmp);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to extract text from PDF page {}: {}", page + 1, e.getMessage());
                            }
                        }
                        
                        text = allText.toString();
                        pdDocumentClass.getMethod("close").invoke(document);
                    } finally {
                        Files.deleteIfExists(pdfTmp);
                    }
                } catch (Exception e) {
                    log.warn("Failed to process PDF with Tesseract: {}", e.getMessage());
                    Files.deleteIfExists(pdfTmp);
                }
            } catch (ClassNotFoundException e) {
                log.info("PDFBox not available for PDF to image conversion");
                throw new RuntimeException("PDFBox not available for PDF processing", e);
            }

            return OcrUploadResponse.builder()
                    .meta(java.util.Map.of(
                            "provider", "tesseract-fa-pdf",
                            "filename", filename != null ? filename : "unknown",
                            "contentType", contentType != null ? contentType : "unknown",
                            "isPdf", true
                    ))
                    .ocr(OcrUploadResponse.OcrData.builder()
                            .textPreview(text) // Return full text, not limited preview
                            .build())
                    .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                    .build();
        } catch (ClassNotFoundException e) {
            log.info("Tesseract not found on classpath; skipping FaTesseractOcrProvider");
            throw new RuntimeException("Tesseract not available");
        } catch (Exception e) {
            log.warn("FaTesseractOcrProvider failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "tesseract-fa";
    }
}


