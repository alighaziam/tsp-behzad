package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Highest priority for PDF files - try this first
public class PdfOcrProvider implements OcrProvider {
    
    @Override
    public boolean supports(String contentType, String filename) {
        String ct = contentType != null ? contentType : "";
        String name = filename != null ? filename.toLowerCase() : "";
        return ct.equals("application/pdf") || name.endsWith(".pdf");
    }

    @Override
    public OcrUploadResponse extract(MultipartFile file) {
        try {
            // تلاش برای استفاده از PDFBox اگر موجود باشد
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> textStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");
            
            // بارگذاری PDF - در PDFBox 3.x از Loader.loadPDF استفاده می‌کنیم
            // برای جلوگیری از NoSuchFileException با lazy loading، از RandomAccessReadBuffer استفاده می‌کنیم
            // که PDF را در memory نگه می‌دارد و نیاز به فایل موقت ندارد
            Object document = null;
            java.nio.file.Path tempFile = null;
            try {
                // روش 1: استفاده از RandomAccessReadBuffer برای PDFBox 3.x (در memory)
                try {
                    Class<?> randomAccessReadBufferClass = Class.forName("org.apache.pdfbox.io.RandomAccessReadBuffer");
                    Class<?> memoryUsageSettingClass = Class.forName("org.apache.pdfbox.io.MemoryUsageSetting");
                    
                    // خواندن کل فایل به memory
                    byte[] pdfBytes = file.getBytes();
                    Object memoryUsage = memoryUsageSettingClass.getMethod("setupMainMemoryOnly").invoke(null);
                    
                    // ایجاد RandomAccessReadBuffer از byte array
                    Object buffer = randomAccessReadBufferClass.getConstructor(byte[].class)
                            .newInstance(pdfBytes);
                    
                    // بارگذاری PDF از buffer
                    Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
                    document = loaderClass.getMethod("loadPDF", randomAccessReadBufferClass, memoryUsageSettingClass)
                            .invoke(null, buffer, memoryUsage);
                    log.debug("Using PDFBox 3.x Loader.loadPDF with RandomAccessReadBuffer (in-memory)");
                } catch (Exception e) {
                    // روش 2: استفاده از فایل موقت (fallback)
                    log.debug("RandomAccessReadBuffer not available, using temp file: {}", e.getMessage());
                    
                    // ایجاد فایل موقت
                    tempFile = java.nio.file.Files.createTempFile("pdf-ocr-", ".pdf");
                    
                    // کپی فایل به فایل موقت
                    try (java.io.InputStream inputStream = file.getInputStream();
                         java.io.OutputStream outputStream = java.nio.file.Files.newOutputStream(tempFile)) {
                        inputStream.transferTo(outputStream);
                    }
                    
                    // تلاش برای استفاده از Loader.loadPDF (PDFBox 3.x)
                    try {
                        Class<?> loaderClass = Class.forName("org.apache.pdfbox.Loader");
                        document = loaderClass.getMethod("loadPDF", java.io.File.class)
                                .invoke(null, tempFile.toFile());
                        log.debug("Using PDFBox 3.x Loader.loadPDF with temp file");
                    } catch (ClassNotFoundException | NoSuchMethodException e2) {
                        // اگر Loader موجود نبود، از PDDocument.load استفاده می‌کنیم (PDFBox 2.x)
                        log.debug("Loader not available, using PDDocument.load: {}", e2.getMessage());
                        document = pdDocumentClass.getMethod("load", java.io.File.class)
                                .invoke(null, tempFile.toFile());
                    }
                }
                
                // دریافت تعداد صفحات
                int pageCount = (Integer) pdDocumentClass.getMethod("getNumberOfPages").invoke(document);
                log.info("PdfOcrProvider: PDF has {} pages", pageCount);
                
                // استخراج متن از همه صفحات
                Object stripper = textStripperClass.getDeclaredConstructor().newInstance();
                
                // تنظیمات برای RTL (راست به چپ) - برای متن فارسی
                // setSortByPosition(false) برای حفظ ترتیب طبیعی متن (RTL)
                // در PDF های فارسی، معمولاً ترتیب طبیعی بهتر از sort by position است
                try {
                    textStripperClass.getMethod("setSortByPosition", boolean.class).invoke(stripper, false);
                    log.debug("PdfOcrProvider: Set sortByPosition to false for RTL text");
                } catch (Exception e) {
                    log.debug("PdfOcrProvider: Could not set sortByPosition: {}", e.getMessage());
                }
                
                // تنظیم برای خواندن همه صفحات
                if (pageCount > 0) {
                    textStripperClass.getMethod("setStartPage", int.class).invoke(stripper, 1);
                    textStripperClass.getMethod("setEndPage", int.class).invoke(stripper, pageCount);
                    log.info("PdfOcrProvider: Set page range: 1 to {} (all pages)", pageCount);
                    
                    // بررسی اینکه آیا تنظیمات اعمال شده
                    int startPage = (Integer) textStripperClass.getMethod("getStartPage").invoke(stripper);
                    int endPage = (Integer) textStripperClass.getMethod("getEndPage").invoke(stripper);
                    log.debug("PdfOcrProvider: Confirmed page range - start: {}, end: {}", startPage, endPage);
                }
                
                // استخراج متن از همه صفحات
                // توجه: PDFBox 3.x ممکن است lazy loading استفاده کند و بعد از getText() به فایل دسترسی نیاز داشته باشد
                log.debug("PdfOcrProvider: Starting text extraction from all pages (RTL mode)...");
                String text = (String) textStripperClass.getMethod("getText", pdDocumentClass)
                        .invoke(stripper, document);
                log.debug("PdfOcrProvider: Text extraction completed");
                
                // اصلاح ترتیب RTL برای متن فارسی
                if (text != null && !text.isBlank()) {
                    text = fixRtlTextOrder(text);
                    log.debug("PdfOcrProvider: Applied RTL text order correction");
                }
                
                if (text == null) {
                    text = "";
                }
                
                // لاگ جزئیات بیشتر
                log.info("PdfOcrProvider: Successfully extracted {} characters from {} pages", text.length(), pageCount);
                if (text.length() > 0) {
                    log.debug("PdfOcrProvider: First 200 chars: {}", text.length() > 200 ? text.substring(0, 200) + "..." : text);
                    log.debug("PdfOcrProvider: Last 200 chars: {}", text.length() > 200 ? "..." + text.substring(text.length() - 200) : text);
                } else {
                    log.warn("PdfOcrProvider: Extracted text is empty! This PDF might contain only images. " +
                            "Attempting OCR on PDF pages using Tesseract...");
                }
                
                // اگر متن خالی است، تلاش برای استخراج از تصاویر با Tesseract
                // توجه: document را هنوز نبسته‌ایم چون برای OCR نیاز داریم
                if (text == null || text.trim().isEmpty()) {
                    log.info("PdfOcrProvider: Text is empty, attempting OCR on PDF pages using Tesseract...");
                    try {
                        String ocrText = extractTextFromImages(document, pdDocumentClass, pageCount);
                        if (ocrText != null && !ocrText.trim().isEmpty()) {
                            text = ocrText;
                            log.info("PdfOcrProvider: Successfully extracted {} characters using Tesseract OCR", text.length());
                        } else {
                            log.warn("PdfOcrProvider: Tesseract OCR also returned empty text");
                        }
                    } catch (Exception ocrException) {
                        log.warn("PdfOcrProvider: OCR extraction failed: {}", ocrException.getMessage());
                        // ادامه با متن خالی
                    }
                }
                
                // بستن document بعد از تمام عملیات
                // این مهم است که document را قبل از حذف فایل موقت ببندیم
                pdDocumentClass.getMethod("close").invoke(document);
                document = null; // Mark as closed
                
                // اطمینان از اینکه text null نیست
                if (text == null) {
                    text = "";
                }
                
                // لاگ نهایی
                log.info("PdfOcrProvider: Final text length: {} characters", text.length());
                if (text.length() > 0) {
                    log.debug("PdfOcrProvider: Final text preview (first 500): {}", 
                            text.length() > 500 ? text.substring(0, 500) + "..." : text);
                }
                
                return OcrUploadResponse.builder()
                        .meta(Map.of(
                                "provider", text.length() > 0 ? "pdfbox" : "pdfbox-empty",
                                "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf",
                                "contentType", file.getContentType() != null ? file.getContentType() : "application/pdf",
                                "textLength", text.length(),
                                "pageCount", pageCount
                        ))
                        .ocr(OcrUploadResponse.OcrData.builder()
                                .textPreview(text) // Return full text, not just preview
                                .build())
                        .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                        .build();
            } catch (Exception e) {
                // بستن document در صورت خطا
                if (document != null) {
                    try {
                        pdDocumentClass.getMethod("close").invoke(document);
                    } catch (Exception ignored) {}
                }
                throw e;
            } finally {
                // حذف فایل موقت بعد از بستن document
                // این مهم است که فایل را بعد از بستن document حذف کنیم
                // چون PDFBox 3.x ممکن است lazy loading استفاده کند
                if (tempFile != null) {
                    try {
                        java.nio.file.Files.deleteIfExists(tempFile);
                        log.debug("PdfOcrProvider: Temp file deleted: {}", tempFile);
                    } catch (Exception e) {
                        log.warn("PdfOcrProvider: Failed to delete temp file {}: {}", tempFile, e.getMessage());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.info("PDFBox not found on classpath; PDF text extraction not available. Add dependency: org.apache.pdfbox:pdfbox");
            // اگر PDFBox موجود نباشد، exception throw می‌کنیم تا provider بعدی امتحان شود
            throw new RuntimeException("PDFBox library not available", e);
        } catch (Exception e) {
            log.error("PdfOcrProvider failed: {}", e.getMessage(), e);
            // اگر خطا رخ داد، exception throw می‌کنیم تا provider بعدی امتحان شود
            // اما اگر فقط متن خالی است (نه خطا)، response با متن خالی برمی‌گردانیم
            // تا OcrService بتواند provider بعدی را امتحان کند
            if (e.getMessage() != null && e.getMessage().contains("empty") || e.getMessage().contains("blank")) {
                // اگر خطا مربوط به متن خالی است، response خالی برمی‌گردانیم
                log.warn("PdfOcrProvider: Text extraction returned empty result, allowing other providers to try");
                return OcrUploadResponse.builder()
                        .meta(Map.of(
                                "provider", "pdfbox-empty",
                                "filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.pdf",
                                "contentType", file.getContentType() != null ? file.getContentType() : "application/pdf",
                                "textLength", 0,
                                "error", "Text extraction returned empty - PDF might be image-based"
                        ))
                        .ocr(OcrUploadResponse.OcrData.builder()
                                .textPreview("") // Empty text to allow other providers to try
                                .build())
                        .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                        .build();
            }
            throw new RuntimeException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "pdfbox";
    }
    
    /**
     * اصلاح ترتیب متن برای RTL (راست به چپ)
     * این متد ترتیب خطوط و کاراکترها را برای متن فارسی اصلاح می‌کند
     */
    private String fixRtlTextOrder(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // تقسیم به خطوط
        String[] lines = text.split("[\r\n]+");
        StringBuilder result = new StringBuilder(text.length());
        
        for (String line : lines) {
            if (line.isBlank()) {
                result.append("\n");
                continue;
            }
            
            // بررسی اینکه آیا خط شامل کاراکترهای فارسی است
            boolean hasPersian = line.matches(".*[\\u0600-\\u06FF].*");
            
            if (hasPersian) {
                // برای خطوط فارسی، ترتیب را حفظ می‌کنیم (RTL طبیعی)
                // اما ممکن است نیاز به اصلاح ترتیب کلمات باشد
                String fixedLine = fixRtlLineOrder(line);
                result.append(fixedLine);
            } else {
                // برای خطوط انگلیسی/عددی، ترتیب را حفظ می‌کنیم
                result.append(line);
            }
            
            result.append("\n");
        }
        
        // حذف خط جدید آخر
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        
        return result.toString();
    }
    
    /**
     * اصلاح ترتیب کلمات در یک خط برای RTL
     */
    private String fixRtlLineOrder(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }
        
        // اگر خط شامل کاراکترهای فارسی است، ترتیب را حفظ می‌کنیم
        // PDFBox معمولاً ترتیب را به درستی حفظ می‌کند، اما ممکن است نیاز به اصلاح باشد
        
        // حذف فاصله‌های اضافی
        line = line.replaceAll("\\s+", " ").trim();
        
        // اگر خط با کاراکتر فارسی شروع می‌شود، ترتیب درست است
        // اگر با کاراکتر انگلیسی/عددی شروع می‌شود، ممکن است نیاز به معکوس کردن باشد
        if (line.length() > 0) {
            char firstChar = line.charAt(0);
            boolean startsWithPersian = (firstChar >= '\u0600' && firstChar <= '\u06FF');
            
            if (!startsWithPersian && line.matches(".*[\\u0600-\\u06FF].*")) {
                // خط شامل فارسی است اما با کاراکتر غیرفارسی شروع می‌شود
                // ممکن است نیاز به اصلاح باشد، اما معمولاً PDFBox ترتیب را درست حفظ می‌کند
                // در اینجا فقط whitespace را normalize می‌کنیم
            }
        }
        
        return line;
    }
    
    /**
     * استخراج متن از تصاویر PDF با استفاده از Tesseract OCR
     */
    private String extractTextFromImages(Object document, Class<?> pdDocumentClass, int pageCount) throws Exception {
        try {
            Class<?> tesseractCls = Class.forName("net.sourceforge.tess4j.Tesseract");
            Object tesseract = tesseractCls.getDeclaredConstructor().newInstance();
            tesseractCls.getMethod("setLanguage", String.class).invoke(tesseract, "fas+eng");
            
            // تنظیمات پیشرفته Tesseract برای دقت بهتر
            try {
                // PSM 6: Assume a single uniform block of text
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_pageseg_mode", "6");
                
                // بهبود دقت برای متن فارسی - whitelist کامل‌تر با کاراکترهای بیشتر
                String persianChars = "۰۱۲۳۴۵۶۷۸۹ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی";
                String englishChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                String specialChars = ".,:;/-()[]{}%+×÷=<>|";
                String arabicChars = "أإآؤئ";
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_char_whitelist", persianChars + englishChars + specialChars + arabicChars);
                
                // افزایش DPI برای کیفیت بهتر
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "user_defined_dpi", "300");
                
                // بهبود دقت برای تشخیص اعداد
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "classify_bln_numeric_mode", "1");
                
                // بهبود دقت برای جداول
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "preserve_interword_spaces", "1");
                
                // بهبود دقت برای متن فارسی
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_create_hocr", "0");
                
                // فعال‌سازی DAWG برای بهبود دقت
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_system_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_freq_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_number_dawg", "1");
                
                log.debug("PdfOcrProvider: Configured Tesseract with advanced optimized settings for image-based PDF");
            } catch (Exception e) {
                log.debug("PdfOcrProvider: Could not set all Tesseract variables: {}", e.getMessage());
                // Fallback to basic settings
                try {
                    tesseractCls.getMethod("setTessVariable", String.class, String.class)
                            .invoke(tesseract, "tessedit_pageseg_mode", "6");
                } catch (Exception e2) {
                    log.debug("PdfOcrProvider: Could not set basic Tesseract variables: {}", e2.getMessage());
                }
            }
            
            // کلاس‌های PDFBox برای تبدیل صفحات به تصویر
            Class<?> pdfRendererClass = Class.forName("org.apache.pdfbox.rendering.PDFRenderer");
            Class<?> bufferedImageClass = Class.forName("java.awt.image.BufferedImage");
            Class<?> imageIOClass = Class.forName("javax.imageio.ImageIO");
            
            Object renderer = pdfRendererClass.getConstructor(pdDocumentClass).newInstance(document);
            
            // استخراج متن از همه صفحات و merge کردن
            StringBuilder allText = new StringBuilder();
            int successfulPages = 0;
            
            for (int page = 0; page < pageCount; page++) {
                try {
                    log.debug("PdfOcrProvider: Processing page {}/{} with Tesseract OCR", page + 1, pageCount);
                    
                    // تبدیل صفحه به تصویر
                    Object image = pdfRendererClass.getMethod("renderImageWithDPI", int.class, float.class)
                            .invoke(renderer, page, 300f); // 300 DPI برای کیفیت بهتر
                    
                    // ذخیره موقت تصویر
                    java.nio.file.Path tempImage = java.nio.file.Files.createTempFile("pdf-page-", ".png");
                    try {
                        // ImageIO.write signature: write(RenderedImage im, String formatName, File output)
                        // Try different method signatures
                        try {
                            imageIOClass.getMethod("write", bufferedImageClass, String.class, java.io.File.class)
                                    .invoke(null, image, "PNG", tempImage.toFile());
                        } catch (NoSuchMethodException e) {
                            // Try alternative signature: write(RenderedImage im, String formatName, OutputStream output)
                            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempImage.toFile())) {
                                imageIOClass.getMethod("write", bufferedImageClass, String.class, java.io.OutputStream.class)
                                        .invoke(null, image, "PNG", fos);
                            }
                        }
                        
                        // OCR روی تصویر
                        String pageText = (String) tesseractCls.getMethod("doOCR", java.io.File.class)
                                .invoke(tesseract, tempImage.toFile());
                        
                        if (pageText != null && !pageText.trim().isEmpty()) {
                            // اضافه کردن separator بین صفحات
                            if (allText.length() > 0) {
                                allText.append("\n--- Page ").append(page + 1).append(" ---\n");
                            }
                            allText.append(pageText);
                            successfulPages++;
                            log.debug("PdfOcrProvider: Extracted {} characters from page {}", pageText.length(), page + 1);
                        }
                    } finally {
                        java.nio.file.Files.deleteIfExists(tempImage);
                    }
                } catch (Exception e) {
                    log.warn("PdfOcrProvider: Failed to extract text from page {}: {}", page + 1, e.getMessage());
                }
            }
            
            log.info("PdfOcrProvider: Successfully extracted text from {}/{} pages", successfulPages, pageCount);
            String extractedText = allText.toString();
            
            // اصلاح ترتیب RTL برای متن فارسی
            if (extractedText != null && !extractedText.isBlank()) {
                extractedText = fixRtlTextOrder(extractedText);
                log.debug("PdfOcrProvider: Applied RTL text order correction to OCR text");
            }
            
            return extractedText;
        } catch (ClassNotFoundException e) {
            log.info("Tesseract or PDFBox rendering classes not found; OCR extraction not available");
            return "";
        }
    }
}

