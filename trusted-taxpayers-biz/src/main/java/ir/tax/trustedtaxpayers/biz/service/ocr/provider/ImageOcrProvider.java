package ir.tax.trustedtaxpayers.biz.service.ocr.provider;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrProvider;
import ir.tax.trustedtaxpayers.biz.service.ocr.preprocessing.ImagePreprocessingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * OCR Provider برای تصاویر (PNG, JPG, JPEG)
 * از Tesseract OCR با پشتیبانی از زبان فارسی استفاده می‌کند
 * 
 * این provider اولویت بالایی دارد و برای تصاویر استفاده می‌شود
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // بعد از PdfOcrProvider، اما قبل از FaTesseractOcrProvider
@RequiredArgsConstructor
public class ImageOcrProvider implements OcrProvider {
    
    private final ImagePreprocessingConfig preprocessingConfig;
    
    // Cache کردن reflection classes برای بهبود performance
    private static Class<?> imageIOClass;
    private static Class<?> bufferedImageClass;
    private static Class<?> colorSpaceClass;
    private static Class<?> colorConvertOpClass;
    private static Class<?> rasterClass;
    private static Class<?> graphics2DClass;
    private static Class<?> renderingHintsClass;
    
    // Cache کردن field values
    private static Integer TYPE_BYTE_GRAY;
    private static Integer CS_GRAY;
    private static Object KEY_INTERPOLATION;
    private static Object VALUE_INTERPOLATION_BICUBIC;
    
    static {
        try {
            // Cache کردن classes در static initializer
            imageIOClass = Class.forName("javax.imageio.ImageIO");
            bufferedImageClass = Class.forName("java.awt.image.BufferedImage");
            colorSpaceClass = Class.forName("java.awt.color.ColorSpace");
            colorConvertOpClass = Class.forName("java.awt.image.ColorConvertOp");
            rasterClass = Class.forName("java.awt.image.Raster");
            graphics2DClass = Class.forName("java.awt.Graphics2D");
            renderingHintsClass = Class.forName("java.awt.RenderingHints");
            
            // Cache کردن field values
            TYPE_BYTE_GRAY = (Integer) bufferedImageClass.getField("TYPE_BYTE_GRAY").get(null);
            CS_GRAY = (Integer) colorSpaceClass.getField("CS_GRAY").getInt(null);
            KEY_INTERPOLATION = renderingHintsClass.getField("KEY_INTERPOLATION").get(null);
            VALUE_INTERPOLATION_BICUBIC = renderingHintsClass.getField("VALUE_INTERPOLATION_BICUBIC").get(null);
        } catch (Exception e) {
            // اگر classes در دسترس نباشند، در runtime بررسی می‌شود
            // استفاده از System.err به جای log در static block برای اطمینان
            System.err.println("ImageOcrProvider: Could not cache reflection classes: " + e.getMessage());
        }
    }
    
    @Override
    public boolean supports(String contentType, String filename) {
        String ct = contentType != null ? contentType.toLowerCase() : "";
        String name = filename != null ? filename.toLowerCase() : "";
        
        // بررسی بر اساس Content-Type
        boolean supportedByContentType = ct.startsWith("image/") 
                || ct.equals("image/png")
                || ct.equals("image/jpeg")
                || ct.equals("image/jpg");
        
        // بررسی بر اساس پسوند فایل (برای مواردی که Content-Type درست تشخیص داده نشده)
        boolean supportedByExtension = name.endsWith(".png") 
                || name.endsWith(".jpg") 
                || name.endsWith(".jpeg");
        
        boolean isSupported = supportedByContentType || supportedByExtension;
        
        if (isSupported) {
            log.debug("ImageOcrProvider supports file: contentType={}, filename={}", contentType, filename);
        }
        
        return isSupported;
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
                log.debug("ImageOcrProvider: Set Tesseract language to fas+eng");
            } catch (NoSuchMethodException e) {
                log.warn("ImageOcrProvider: setLanguage method not found, using default language");
            }
            
            // تنظیمات پیشرفته Tesseract برای دقت بهتر
            try {
                // PSM 6: Assume a single uniform block of text (بهترین برای فاکتورهای منظم)
                // PSM 11: Sparse text (برای فاکتورهای با ساختار نامنظم)
                // PSM 4: Assume a single column of text of variable sizes
                // PSM 3: Fully automatic page segmentation, but no OSD (بهترین برای فاکتورهای پیچیده)
                
                // PSM 6 برای فاکتورهای منظم (اولویت اول)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_pageseg_mode", "6");
                
                // بهبود دقت برای متن فارسی - whitelist کامل‌تر با کاراکترهای بیشتر
                String persianChars = "۰۱۲۳۴۵۶۷۸۹ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی";
                String englishChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                String specialChars = ".,:;/-()[]{}%+×÷=<>|";
                String arabicChars = "أإآؤئ"; // حروف عربی که ممکن است در OCR اشتباه خوانده شوند
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
                        .invoke(tesseract, "tessedit_create_hocr", "0"); // غیرفعال کردن hOCR برای سرعت بیشتر
                
                // تنظیمات RTL (راست به چپ) برای متن فارسی
                // در Tesseract، برای زبان فارسی، ترتیب به صورت خودکار RTL است
                // اما می‌توانیم با تنظیمات اضافی دقت را بهبود دهیم
                try {
                    // فعال‌سازی تشخیص خودکار جهت متن
                    tesseractCls.getMethod("setTessVariable", String.class, String.class)
                            .invoke(tesseract, "textord_really_old_xheight", "1");
                    log.debug("ImageOcrProvider: Enabled RTL text detection for Persian");
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not set RTL settings: {}", e.getMessage());
                }
                
                // بهبود دقت برای تشخیص حروف مشابه (مثل ق و ف)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_system_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_freq_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_unambig_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_punc_dawg", "1");
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "load_number_dawg", "1");
                
                // بهبود دقت برای جداول و ساختارهای منظم
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "textord_min_linesize", "2.5");
                
                // بهبود دقت برای تشخیص جهت متن (RTL)
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "textord_really_old_xheight", "0");
                
                log.debug("ImageOcrProvider: Configured Tesseract with advanced optimized settings (PSM 6, DPI 300, enhanced whitelist, DAWG enabled)");
            } catch (Exception e) {
                log.debug("ImageOcrProvider: Could not set all Tesseract variables: {}", e.getMessage());
                // اگر برخی تنظیمات کار نکرد، حداقل تنظیمات اصلی را اعمال می‌کنیم
                try {
                    tesseractCls.getMethod("setTessVariable", String.class, String.class)
                            .invoke(tesseract, "tessedit_pageseg_mode", "6");
                    tesseractCls.getMethod("setTessVariable", String.class, String.class)
                            .invoke(tesseract, "user_defined_dpi", "300");
                } catch (Exception e2) {
                    log.debug("ImageOcrProvider: Could not set basic Tesseract variables: {}", e2.getMessage());
                }
            }
            
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            
            // ذخیره تصویر به فایل موقت برای OCR
            Path tempImage = Files.createTempFile("tsp-ocr-img-", 
                    getFileExtension(filename));
            java.io.File processedImageFile = null;
            
            try {
                // کپی فایل به فایل موقت
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, tempImage, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                
                log.debug("ImageOcrProvider: Processing image file: {} (size: {} bytes)", 
                        filename, file.getSize());
                
                // پیش‌پردازش تصویر برای بهبود دقت OCR
                processedImageFile = preprocessImage(tempImage.toFile());
                
                // انجام OCR روی تصویر پیش‌پردازش شده
                // امتحان چندین PSM mode و انتخاب بهترین نتیجه
                String text = extractTextWithMultiplePSM(tesseract, tesseractCls, processedImageFile);
                
                if (text == null) {
                    text = "";
                }
                
                // اصلاح ترتیب RTL برای متن فارسی
                if (text != null && !text.isBlank()) {
                    text = fixRtlTextOrder(text);
                    log.debug("ImageOcrProvider: Applied RTL text order correction");
                }
                
                log.info("ImageOcrProvider: Successfully extracted {} characters from image", text.length());
                if (text.length() > 0) {
                    log.debug("ImageOcrProvider: First 200 chars: {}", 
                            text.length() > 200 ? text.substring(0, 200) + "..." : text);
                } else {
                    log.warn("ImageOcrProvider: Extracted text is empty from image: {}", filename);
                }
                
                return OcrUploadResponse.builder()
                        .meta(Map.of(
                                "provider", "tesseract-image",
                                "filename", filename != null ? filename : "unknown",
                                "contentType", contentType != null ? contentType : "unknown",
                                "textLength", text.length(),
                                "fileSize", file.getSize()
                        ))
                        .ocr(OcrUploadResponse.OcrData.builder()
                                .textPreview(text) // Return full text
                                .fullText(text) // Include full text for analysis
                                .build())
                        .parsed(OcrUploadResponse.ParsedData.builder().items(java.util.List.of()).build())
                        .build();
            } finally {
                // حذف فایل‌های موقت
                try {
                    Files.deleteIfExists(tempImage);
                } catch (Exception e) {
                    log.warn("ImageOcrProvider: Failed to delete temp file {}: {}", tempImage, e.getMessage());
                }
                if (processedImageFile != null && processedImageFile.exists() && !processedImageFile.equals(tempImage.toFile())) {
                    try {
                        if (processedImageFile.delete()) {
                            log.debug("ImageOcrProvider: Deleted processed image file: {}", processedImageFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        log.warn("ImageOcrProvider: Failed to delete processed image file {}: {}", 
                                processedImageFile.getAbsolutePath(), e.getMessage());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            log.info("Tesseract not found on classpath; ImageOcrProvider not available. " +
                    "Add dependency: net.sourceforge.tess4j:tess4j");
            // Throw exception to allow fallback to other providers
            throw new RuntimeException("Tesseract library not available", e);
        } catch (Exception e) {
            log.error("ImageOcrProvider failed for file {}: {}", 
                    file.getOriginalFilename(), e.getMessage(), e);
            // Throw exception to allow fallback to other providers (like ExternalOcrProvider)
            throw new RuntimeException("Failed to extract text from image: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "tesseract-image";
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
                // Tesseract معمولاً ترتیب را به درستی حفظ می‌کند
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
        
        // حذف فاصله‌های اضافی
        line = line.replaceAll("\\s+", " ").trim();
        
        // Tesseract برای زبان فارسی معمولاً ترتیب را به درستی حفظ می‌کند
        // اما ممکن است نیاز به اصلاح whitespace باشد
        return line;
    }
    
    /**
     * استخراج پسوند فایل برای نام فایل موقت
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return ".tmp";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return ".tmp";
    }
    
    /**
     * پیش‌پردازش تصویر برای بهبود دقت OCR
     * شامل: تبدیل به grayscale، بهبود کنتراست، کاهش نویز
     */
    private java.io.File preprocessImage(java.io.File imageFile) {
        try {
            // استفاده از cached classes برای بهبود performance
            Class<?> imageIOClassRef = imageIOClass != null ? imageIOClass : 
                                      Class.forName("javax.imageio.ImageIO");
            Class<?> bufferedImageClassRef = bufferedImageClass != null ? bufferedImageClass : 
                                            Class.forName("java.awt.image.BufferedImage");
            Class<?> colorSpaceClassRef = colorSpaceClass != null ? colorSpaceClass : 
                                         Class.forName("java.awt.color.ColorSpace");
            Class<?> colorConvertOpClassRef = colorConvertOpClass != null ? colorConvertOpClass : 
                                             Class.forName("java.awt.image.ColorConvertOp");
            
            // خواندن تصویر
            Object originalImage = imageIOClassRef.getMethod("read", java.io.File.class)
                    .invoke(null, imageFile);
            
            if (originalImage == null) {
                log.warn("ImageOcrProvider: Could not read image, using original file");
                return imageFile;
            }
            
            // بررسی نوع تصویر
            int imageType = (Integer) bufferedImageClassRef.getMethod("getType").invoke(originalImage);
            int width = (Integer) bufferedImageClassRef.getMethod("getWidth").invoke(originalImage);
            int height = (Integer) bufferedImageClassRef.getMethod("getHeight").invoke(originalImage);
            
            log.debug("ImageOcrProvider: Original image - type: {}, size: {}x{}", imageType, width, height);
            
            // اگر تصویر از قبل grayscale است، فقط کنتراست را بهبود می‌دهیم
            Object processedImage = originalImage;
            
            // تبدیل به grayscale اگر رنگی است
            int grayTypeValue = TYPE_BYTE_GRAY != null ? TYPE_BYTE_GRAY : 
                               (Integer) bufferedImageClassRef.getField("TYPE_BYTE_GRAY").get(null);
            if (imageType != grayTypeValue) {
                try {
                    int csGrayValue = CS_GRAY != null ? CS_GRAY : 
                                     (Integer) colorSpaceClassRef.getField("CS_GRAY").getInt(null);
                    Object grayColorSpace = colorSpaceClassRef.getMethod("getInstance", int.class)
                            .invoke(null, csGrayValue);
                    Object colorConvert = colorConvertOpClassRef.getConstructor(colorSpaceClassRef, colorSpaceClassRef)
                            .newInstance(grayColorSpace, null);
                    processedImage = colorConvertOpClassRef.getMethod("filter", 
                            bufferedImageClassRef, bufferedImageClassRef)
                            .invoke(colorConvert, originalImage, null);
                    log.debug("ImageOcrProvider: Converted image to grayscale");
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not convert to grayscale: {}, using original", e.getMessage());
                    processedImage = originalImage;
                }
            }
            
            // Deskewing (اصلاح کجی) - قبل از سایر پردازش‌ها
            if (preprocessingConfig.getDeskewing().isEnabled()) {
                try {
                    processedImage = deskewImage(processedImage, bufferedImageClassRef, 
                            preprocessingConfig.getDeskewing().getMaxAngle());
                    log.debug("ImageOcrProvider: Applied deskewing");
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not deskew image: {}", e.getMessage());
                }
            }
            
            // کاهش نویز
            if (preprocessingConfig.getNoiseReduction().isEnabled()) {
                try {
                    String method = preprocessingConfig.getNoiseReduction().getMethod();
                    int kernelSize = preprocessingConfig.getNoiseReduction().getKernelSize();
                    if ("MEDIAN".equalsIgnoreCase(method)) {
                        processedImage = applyMedianFilter(processedImage, bufferedImageClassRef, kernelSize);
                        log.debug("ImageOcrProvider: Applied median filter for noise reduction");
                    }
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not apply noise reduction: {}", e.getMessage());
                }
            }
            
            // بهبود کنتراست
            if (preprocessingConfig.getContrast().isEnabled()) {
                try {
                    String method = preprocessingConfig.getContrast().getMethod();
                    if ("HISTOGRAM".equalsIgnoreCase(method)) {
                        processedImage = enhanceContrastWithHistogram(processedImage, bufferedImageClassRef);
                        log.debug("ImageOcrProvider: Enhanced image contrast with histogram equalization");
                    } else {
                        processedImage = enhanceContrast(processedImage, bufferedImageClassRef);
                        log.debug("ImageOcrProvider: Enhanced image contrast with simple stretching");
                    }
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not enhance contrast: {}", e.getMessage());
                }
            }
            
            // Sharpening (قبل از binarization)
            if (preprocessingConfig.getSharpen().isEnabled()) {
                try {
                    processedImage = sharpenImage(processedImage, bufferedImageClassRef, 
                            preprocessingConfig.getSharpen().getStrength());
                    log.debug("ImageOcrProvider: Applied sharpening");
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not sharpen image: {}", e.getMessage());
                }
            }
            
            // تبدیل به تصویر دو رنگ (Binarization)
            try {
                processedImage = binarizeImageWithConfig(processedImage, bufferedImageClassRef, 
                        preprocessingConfig.getThresholding());
                log.debug("ImageOcrProvider: Applied binarization");
            } catch (Exception e) {
                log.debug("ImageOcrProvider: Could not binarize image: {}", e.getMessage());
            }
            
            // اعمال عملیات مورفولوژیک
            if (preprocessingConfig.getMorphology().isEnabled()) {
                try {
                    String operation = preprocessingConfig.getMorphology().getOperation();
                    int kernelSize = preprocessingConfig.getMorphology().getKernelSize();
                    int iterations = preprocessingConfig.getMorphology().getIterations();
                    for (int i = 0; i < iterations; i++) {
                        processedImage = applyMorphology(processedImage, bufferedImageClassRef, operation, kernelSize);
                    }
                    log.debug("ImageOcrProvider: Applied morphological operations ({} iterations)", iterations);
                } catch (Exception e) {
                    log.debug("ImageOcrProvider: Could not apply morphology: {}", e.getMessage());
                }
            }
            
            // تغییر اندازه تصویر
            try {
                processedImage = scaleImageIfNeeded(processedImage, bufferedImageClassRef);
                log.debug("ImageOcrProvider: Scaled image if needed");
            } catch (Exception e) {
                log.debug("ImageOcrProvider: Could not scale image: {}", e.getMessage());
            }
            
            // ذخیره تصویر پیش‌پردازش شده
            java.io.File processedFile = java.io.File.createTempFile("tsp-ocr-processed-", ".png");
            imageIOClassRef.getMethod("write", bufferedImageClassRef, String.class, java.io.File.class)
                    .invoke(null, processedImage, "PNG", processedFile);
            
            log.debug("ImageOcrProvider: Preprocessed image saved to: {}", processedFile.getAbsolutePath());
            return processedFile;
            
        } catch (ClassNotFoundException e) {
            log.debug("ImageOcrProvider: Image processing classes not available, using original image");
            return imageFile;
        } catch (Exception e) {
            log.warn("ImageOcrProvider: Image preprocessing failed: {}, using original image", e.getMessage());
            return imageFile;
        }
    }
    
    /**
     * بهبود کنتراست تصویر با Histogram Equalization
     * این روش توزیع شدت پیکسل‌ها را یکنواخت می‌کند و کنتراست را بهبود می‌دهد
     */
    private Object enhanceContrastWithHistogram(Object image, Class<?> bufferedImageClass) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        int grayType = TYPE_BYTE_GRAY != null ? TYPE_BYTE_GRAY : 
                      (Integer) bufferedImageClass.getField("TYPE_BYTE_GRAY").get(null);
        Object enhancedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClassRef = rasterClass != null ? rasterClass : 
                                  Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object enhancedRaster = bufferedImageClass.getMethod("getRaster").invoke(enhancedImage);
        
        // خواندن پیکسل‌ها
        int[] pixels = (int[]) rasterClassRef.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        // محاسبه histogram
        int[] histogram = new int[256];
        int totalPixels = 0;
        for (int pixel : pixels) {
            if (pixel >= 0 && pixel < 256) {
                histogram[pixel]++;
                totalPixels++;
            }
        }
        
        if (totalPixels == 0) {
            return image;
        }
        
        // محاسبه cumulative distribution function (CDF)
        int[] cdf = new int[256];
        cdf[0] = histogram[0];
        for (int i = 1; i < 256; i++) {
            cdf[i] = cdf[i - 1] + histogram[i];
        }
        
        // محاسبه lookup table برای histogram equalization
        int[] lookupTable = new int[256];
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                cdfMin = cdf[i];
                break;
            }
        }
        
        // اعمال histogram equalization formula
        for (int i = 0; i < 256; i++) {
            if (cdf[i] > 0) {
                // فرمول: h(v) = round((cdf(v) - cdf_min) / (M*N - cdf_min) * 255)
                lookupTable[i] = (int) Math.round(((double)(cdf[i] - cdfMin) / (totalPixels - cdfMin)) * 255.0);
                lookupTable[i] = Math.max(0, Math.min(255, lookupTable[i]));
            } else {
                lookupTable[i] = 0;
            }
        }
        
        // اعمال lookup table به پیکسل‌ها
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (pixel < 0) pixel = 0;
            if (pixel > 255) pixel = 255;
            pixels[i] = lookupTable[pixel];
        }
        
        // تنظیم پیکسل‌های بهبود یافته
        rasterClassRef.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(enhancedRaster, 0, 0, width, height, pixels);
        
        return enhancedImage;
    }
    
    /**
     * بهبود کنتراست تصویر (روش ساده - Contrast Stretching)
     * Fallback method در صورت عدم موفقیت Histogram Equalization
     */
    private Object enhanceContrast(Object image, Class<?> bufferedImageClass) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // ایجاد تصویر جدید با همان ابعاد
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object enhancedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        // دریافت raster برای دسترسی به پیکسل‌ها
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object enhancedRaster = bufferedImageClass.getMethod("getRaster").invoke(enhancedImage);
        
        // محاسبه histogram برای بهبود کنتراست
        int[] histogram = new int[256];
        int[] pixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        for (int pixel : pixels) {
            if (pixel >= 0 && pixel < 256) {
                histogram[pixel]++;
            }
        }
        
        // محاسبه min و max برای stretching
        int min = 0, max = 255;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = 255; i >= 0; i--) {
            if (histogram[i] > 0) {
                max = i;
                break;
            }
        }
        
        // اعمال contrast stretching
        if (max > min) {
            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                if (pixel < 0) pixel = 0;
                if (pixel > 255) pixel = 255;
                int stretched = ((pixel - min) * 255) / (max - min);
                if (stretched < 0) stretched = 0;
                if (stretched > 255) stretched = 255;
                pixels[i] = stretched;
            }
        }
        
        // تنظیم پیکسل‌های بهبود یافته
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(enhancedRaster, 0, 0, width, height, pixels);
        
        return enhancedImage;
    }
    
    /**
     * تبدیل تصویر به تصویر دو رنگ (Binarization) با استفاده از تنظیمات
     */
    private Object binarizeImageWithConfig(Object image, Class<?> bufferedImageClass, 
                                          ImagePreprocessingConfig.Thresholding config) throws Exception {
        String method = config.getMethod();
        
        if ("ADAPTIVE".equalsIgnoreCase(method)) {
            return applyAdaptiveThreshold(image, bufferedImageClass, 
                    config.getAdaptiveBlockSize(), config.getAdaptiveC());
        } else if ("OTSU".equalsIgnoreCase(method)) {
            return applyOtsuThreshold(image, bufferedImageClass);
        } else {
            // AUTO: انتخاب خودکار بر اساس اندازه تصویر
            int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
            int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
            
            if (width > 1000 || height > 1000) {
                return applyAdaptiveThreshold(image, bufferedImageClass, 
                        config.getAdaptiveBlockSize(), config.getAdaptiveC());
            } else {
                return applyOtsuThreshold(image, bufferedImageClass);
            }
        }
    }
    
    /**
     * تبدیل تصویر به تصویر دو رنگ (Binarization) - متد قدیمی برای backward compatibility
     */
    private Object binarizeImage(Object image, Class<?> bufferedImageClass) throws Exception {
        return binarizeImageWithConfig(image, bufferedImageClass, preprocessingConfig.getThresholding());
    }
    
    /**
     * اعمال Otsu Thresholding
     */
    private Object applyOtsuThreshold(Object image, Class<?> bufferedImageClass) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object binarizedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object binarizedRaster = bufferedImageClass.getMethod("getRaster").invoke(binarizedImage);
        
        int[] pixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int threshold = calculateOtsuThreshold(pixels);
        
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (pixel < 0) pixel = 0;
            if (pixel > 255) pixel = 255;
            pixels[i] = (pixel < threshold) ? 0 : 255;
        }
        
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(binarizedRaster, 0, 0, width, height, pixels);
        
        return binarizedImage;
    }
    
    /**
     * اعمال Adaptive Thresholding (block-based local thresholding)
     * برای تصاویر با نور نامناسب یا سایه‌های محلی مفید است
     * 
     * @param blockSize اندازه بلوک برای محاسبه threshold محلی (باید فرد باشد: 3, 5, 7, 11, ...)
     * @param cConstant ثابت برای تنظیم threshold (معمولاً 2)
     */
    private Object applyAdaptiveThreshold(Object image, Class<?> bufferedImageClass, int blockSize, int cConstant) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // اطمینان از اینکه blockSize فرد است
        if (blockSize % 2 == 0) {
            blockSize++;
        }
        
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object binarizedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object binarizedRaster = bufferedImageClass.getMethod("getRaster").invoke(binarizedImage);
        
        int[] originalPixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int[] binarizedPixels = new int[width * height];
        int halfBlock = blockSize / 2;
        
        // محاسبه threshold محلی برای هر پیکسل
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int pixel = originalPixels[idx];
                if (pixel < 0) pixel = 0;
                if (pixel > 255) pixel = 255;
                
                // محاسبه میانگین محلی در بلوک
                long sum = 0;
                int count = 0;
                
                for (int dy = -halfBlock; dy <= halfBlock; dy++) {
                    for (int dx = -halfBlock; dx <= halfBlock; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int localIdx = ny * width + nx;
                            int localPixel = originalPixels[localIdx];
                            if (localPixel >= 0 && localPixel < 256) {
                                sum += localPixel;
                                count++;
                            }
                        }
                    }
                }
                
                // محاسبه threshold محلی
                int localThreshold = count > 0 ? (int) (sum / count) - cConstant : 128;
                localThreshold = Math.max(0, Math.min(255, localThreshold));
                
                // اعمال threshold
                binarizedPixels[idx] = (pixel < localThreshold) ? 0 : 255;
            }
        }
        
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(binarizedRaster, 0, 0, width, height, binarizedPixels);
        
        return binarizedImage;
    }
    
    /**
     * اعمال Median Filter برای کاهش نویز (به خصوص salt-and-pepper noise)
     * Median filter بهتر از Gaussian blur برای حفظ لبه‌ها است
     * 
     * @param kernelSize اندازه kernel (باید فرد باشد: 3, 5, 7)
     */
    private Object applyMedianFilter(Object image, Class<?> bufferedImageClass, int kernelSize) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // اطمینان از اینکه kernelSize فرد است
        if (kernelSize % 2 == 0) {
            kernelSize++;
        }
        if (kernelSize < 3) {
            kernelSize = 3;
        }
        
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object filteredImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object filteredRaster = bufferedImageClass.getMethod("getRaster").invoke(filteredImage);
        
        int[] originalPixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int[] filteredPixels = new int[width * height];
        int halfKernel = kernelSize / 2;
        
        // اعمال median filter
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // جمع‌آوری مقادیر در kernel
                int[] values = new int[kernelSize * kernelSize];
                int count = 0;
                
                for (int dy = -halfKernel; dy <= halfKernel; dy++) {
                    for (int dx = -halfKernel; dx <= halfKernel; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int idx = ny * width + nx;
                            int pixel = originalPixels[idx];
                            if (pixel >= 0 && pixel < 256) {
                                values[count++] = pixel;
                            }
                        }
                    }
                }
                
                // محاسبه median
                if (count > 0) {
                    // مرتب‌سازی برای پیدا کردن median
                    java.util.Arrays.sort(values, 0, count);
                    int median = values[count / 2];
                    filteredPixels[y * width + x] = median;
                } else {
                    filteredPixels[y * width + x] = originalPixels[y * width + x];
                }
            }
        }
        
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(filteredRaster, 0, 0, width, height, filteredPixels);
        
        return filteredImage;
    }
    
    /**
     * اعمال عملیات مورفولوژیک برای بهبود کیفیت متن
     * 
     * @param operation نوع عملیات: "DILATE", "ERODE", "OPEN", "CLOSE"
     * @param kernelSize اندازه kernel (باید فرد باشد)
     */
    private Object applyMorphology(Object image, Class<?> bufferedImageClass, String operation, int kernelSize) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        if (kernelSize % 2 == 0) {
            kernelSize++;
        }
        if (kernelSize < 3) {
            kernelSize = 3;
        }
        
        Object result = image;
        
        // OPEN = Erosion سپس Dilation
        if ("OPEN".equals(operation)) {
            result = applyErosion(result, bufferedImageClass, kernelSize);
            result = applyDilation(result, bufferedImageClass, kernelSize);
        }
        // CLOSE = Dilation سپس Erosion
        else if ("CLOSE".equals(operation)) {
            result = applyDilation(result, bufferedImageClass, kernelSize);
            result = applyErosion(result, bufferedImageClass, kernelSize);
        }
        // DILATE = ضخیم‌تر کردن متن
        else if ("DILATE".equals(operation)) {
            result = applyDilation(result, bufferedImageClass, kernelSize);
        }
        // ERODE = نازک‌تر کردن متن
        else if ("ERODE".equals(operation)) {
            result = applyErosion(result, bufferedImageClass, kernelSize);
        }
        
        return result;
    }
    
    /**
     * اعمال Dilation (ضخیم‌تر کردن متن - مفید برای اتصال حروف جدا شده)
     */
    private Object applyDilation(Object image, Class<?> bufferedImageClass, int kernelSize) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object dilatedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object dilatedRaster = bufferedImageClass.getMethod("getRaster").invoke(dilatedImage);
        
        int[] originalPixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int[] dilatedPixels = new int[width * height];
        int halfKernel = kernelSize / 2;
        
        // Dilation: حداکثر مقدار در kernel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maxValue = 0;
                
                for (int dy = -halfKernel; dy <= halfKernel; dy++) {
                    for (int dx = -halfKernel; dx <= halfKernel; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int idx = ny * width + nx;
                            int pixel = originalPixels[idx];
                            if (pixel > maxValue) {
                                maxValue = pixel;
                            }
                        }
                    }
                }
                
                dilatedPixels[y * width + x] = maxValue;
            }
        }
        
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(dilatedRaster, 0, 0, width, height, dilatedPixels);
        
        return dilatedImage;
    }
    
    /**
     * اعمال Erosion (نازک‌تر کردن متن - مفید برای حذف نویز کوچک)
     */
    private Object applyErosion(Object image, Class<?> bufferedImageClass, int kernelSize) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        int grayType = bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object erodedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClass = Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object erodedRaster = bufferedImageClass.getMethod("getRaster").invoke(erodedImage);
        
        int[] originalPixels = (int[]) rasterClass.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int[] erodedPixels = new int[width * height];
        int halfKernel = kernelSize / 2;
        
        // Erosion: حداقل مقدار در kernel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int minValue = 255;
                
                for (int dy = -halfKernel; dy <= halfKernel; dy++) {
                    for (int dx = -halfKernel; dx <= halfKernel; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            int idx = ny * width + nx;
                            int pixel = originalPixels[idx];
                            if (pixel < minValue) {
                                minValue = pixel;
                            }
                        }
                    }
                }
                
                erodedPixels[y * width + x] = minValue;
            }
        }
        
        rasterClass.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(erodedRaster, 0, 0, width, height, erodedPixels);
        
        return erodedImage;
    }
    
    /**
     * محاسبه threshold با استفاده از روش Otsu (پیاده‌سازی کامل)
     * الگوریتم Otsu thresholding بهینه threshold را با حداکثر کردن variance بین کلاس‌ها پیدا می‌کند
     */
    private int calculateOtsuThreshold(int[] pixels) {
        // محاسبه histogram
        int[] histogram = new int[256];
        int totalPixels = 0;
        for (int pixel : pixels) {
            if (pixel >= 0 && pixel < 256) {
                histogram[pixel]++;
                totalPixels++;
            }
        }
        
        if (totalPixels == 0) {
            return 128; // threshold پیش‌فرض
        }
        
        // محاسبه probability برای هر سطح شدت
        double[] probability = new double[256];
        for (int i = 0; i < 256; i++) {
            probability[i] = (double) histogram[i] / totalPixels;
        }
        
        // محاسبه cumulative probability و mean
        double[] cumulativeProb = new double[256];
        double[] cumulativeMean = new double[256];
        cumulativeProb[0] = probability[0];
        cumulativeMean[0] = 0.0;
        
        for (int i = 1; i < 256; i++) {
            cumulativeProb[i] = cumulativeProb[i - 1] + probability[i];
            cumulativeMean[i] = cumulativeMean[i - 1] + (i * probability[i]);
        }
        
        // محاسبه global mean
        double globalMean = cumulativeMean[255];
        
        // پیدا کردن threshold بهینه با حداکثر کردن between-class variance
        double maxVariance = 0.0;
        int optimalThreshold = 128;
        
        for (int t = 0; t < 256; t++) {
            if (cumulativeProb[t] > 0.0 && cumulativeProb[t] < 1.0) {
                double mean1 = cumulativeMean[t] / cumulativeProb[t];
                double mean2 = (globalMean - cumulativeMean[t]) / (1.0 - cumulativeProb[t]);
                
                // Between-class variance
                double variance = cumulativeProb[t] * (1.0 - cumulativeProb[t]) * 
                                 (mean1 - mean2) * (mean1 - mean2);
                
                if (variance > maxVariance) {
                    maxVariance = variance;
                    optimalThreshold = t;
                }
            }
        }
        
        // اطمینان از اینکه threshold در محدوده معقول است
        return Math.max(50, Math.min(200, optimalThreshold));
    }
    
    /**
     * تغییر اندازه تصویر اگر خیلی کوچک یا بزرگ است
     * تصاویر کوچک (کمتر از 300px) را بزرگ می‌کند و تصاویر خیلی بزرگ را کوچک می‌کند
     */
    private Object scaleImageIfNeeded(Object image, Class<?> bufferedImageClass) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // حداقل اندازه برای OCR خوب: 300px
        int minSize = 300;
        // حداکثر اندازه برای عملکرد بهتر: 2000px
        int maxSize = 2000;
        
        int newWidth = width;
        int newHeight = height;
        boolean needsScaling = false;
        
        // اگر تصویر خیلی کوچک است، بزرگ می‌کنیم
        if (width < minSize || height < minSize) {
            double scale = Math.max((double) minSize / width, (double) minSize / height);
            newWidth = (int) (width * scale);
            newHeight = (int) (height * scale);
            needsScaling = true;
            log.debug("ImageOcrProvider: Scaling up small image from {}x{} to {}x{}", width, height, newWidth, newHeight);
        }
        // اگر تصویر خیلی بزرگ است، کوچک می‌کنیم
        else if (width > maxSize || height > maxSize) {
            double scale = Math.min((double) maxSize / width, (double) maxSize / height);
            newWidth = (int) (width * scale);
            newHeight = (int) (height * scale);
            needsScaling = true;
            log.debug("ImageOcrProvider: Scaling down large image from {}x{} to {}x{}", width, height, newWidth, newHeight);
        }
        
        if (!needsScaling) {
            return image;
        }
        
        // استفاده از Graphics2D برای تغییر اندازه با کیفیت بالا
        Class<?> graphics2DClassRef = graphics2DClass != null ? graphics2DClass : 
                                      Class.forName("java.awt.Graphics2D");
        Class<?> renderingHintsClassRef = renderingHintsClass != null ? renderingHintsClass : 
                                          Class.forName("java.awt.RenderingHints");
        
        int grayType = TYPE_BYTE_GRAY != null ? TYPE_BYTE_GRAY : 
                      (Integer) bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object scaledImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(newWidth, newHeight, grayType);
        
        Object graphics = bufferedImageClass.getMethod("createGraphics").invoke(scaledImage);
        
        // تنظیم کیفیت بالا برای scaling (استفاده از cached values)
        Object keyInterpolation = KEY_INTERPOLATION != null ? KEY_INTERPOLATION : 
                                 renderingHintsClassRef.getField("KEY_INTERPOLATION").get(null);
        Object valueInterpolationBicubic = VALUE_INTERPOLATION_BICUBIC != null ? VALUE_INTERPOLATION_BICUBIC : 
                                          renderingHintsClassRef.getField("VALUE_INTERPOLATION_BICUBIC").get(null);
        graphics2DClassRef.getMethod("setRenderingHint", 
                renderingHintsClassRef, renderingHintsClassRef)
                .invoke(graphics, keyInterpolation, valueInterpolationBicubic);
        
        // رسم تصویر با اندازه جدید
        graphics2DClassRef.getMethod("drawImage", 
                bufferedImageClass, int.class, int.class, int.class, int.class, null)
                .invoke(graphics, image, 0, 0, newWidth, newHeight, null);
        
        graphics2DClassRef.getMethod("dispose").invoke(graphics);
        
        return scaledImage;
    }
    
    /**
     * استخراج متن با امتحان چندین PSM mode و انتخاب بهترین نتیجه
     * PSM modes مختلف برای انواع مختلف اسناد مناسب هستند
     */
    private String extractTextWithMultiplePSM(Object tesseract, Class<?> tesseractCls, java.io.File imageFile) {
        // لیست PSM modes برای امتحان (به ترتیب اولویت)
        // PSM 6: Assume a single uniform block of text (بهترین برای فاکتورهای منظم)
        // PSM 3: Fully automatic page segmentation, but no OSD
        // PSM 11: Sparse text (برای فاکتورهای با ساختار نامنظم)
        // PSM 4: Assume a single column of text of variable sizes
        int[] psmModes = {6, 3, 11, 4};
        
        String bestText = "";
        int bestLength = 0;
        int bestPSM = 6; // پیش‌فرض
        
        for (int psm : psmModes) {
            try {
                // تنظیم PSM mode
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_pageseg_mode", String.valueOf(psm));
                
                // استخراج متن
                String text = String.valueOf(tesseractCls.getMethod("doOCR", java.io.File.class)
                        .invoke(tesseract, imageFile));
                
                if (text != null) {
                    text = text.trim();
                    int textLength = text.length();
                    
                    // اگر متن استخراج شده طولانی‌تر است، آن را به عنوان بهترین انتخاب می‌کنیم
                    // همچنین بررسی می‌کنیم که متن خالی نباشد
                    if (textLength > bestLength && textLength > 10) {
                        bestText = text;
                        bestLength = textLength;
                        bestPSM = psm;
                        log.debug("ImageOcrProvider: PSM {} produced {} characters (new best)", psm, textLength);
                    } else {
                        log.debug("ImageOcrProvider: PSM {} produced {} characters", psm, textLength);
                    }
                }
            } catch (Exception e) {
                log.debug("ImageOcrProvider: Failed to extract text with PSM {}: {}", psm, e.getMessage());
            }
        }
        
        // بازگرداندن PSM به حالت پیش‌فرض
        try {
            tesseractCls.getMethod("setTessVariable", String.class, String.class)
                    .invoke(tesseract, "tessedit_pageseg_mode", String.valueOf(bestPSM));
        } catch (Exception e) {
            log.debug("ImageOcrProvider: Could not reset PSM mode: {}", e.getMessage());
        }
        
        if (bestText.isEmpty()) {
            log.warn("ImageOcrProvider: All PSM modes failed, trying default PSM 6");
            try {
                tesseractCls.getMethod("setTessVariable", String.class, String.class)
                        .invoke(tesseract, "tessedit_pageseg_mode", "6");
                bestText = String.valueOf(tesseractCls.getMethod("doOCR", java.io.File.class)
                        .invoke(tesseract, imageFile));
            } catch (Exception e) {
                log.error("ImageOcrProvider: Default PSM also failed: {}", e.getMessage());
            }
        } else {
            log.info("ImageOcrProvider: Best result from PSM {} with {} characters", bestPSM, bestLength);
        }
        
        return bestText != null ? bestText : "";
    }
    
    /**
     * Deskewing: اصلاح کجی تصویر با استفاده از Projection Profile
     * زاویه تصویر را با محاسبه projection profile در زوایای مختلف پیدا می‌کند
     * 
     * @param maxAngle حداکثر زاویه برای اصلاح (درجه)
     */
    private Object deskewImage(Object image, Class<?> bufferedImageClass, double maxAngle) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        Class<?> rasterClassRef = rasterClass != null ? rasterClass : 
                                  Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        
        int[] pixels = (int[]) rasterClassRef.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        // تبدیل به binary برای محاسبه بهتر projection
        int[] binaryPixels = new int[pixels.length];
        int threshold = calculateOtsuThreshold(pixels);
        for (int i = 0; i < pixels.length; i++) {
            binaryPixels[i] = (pixels[i] < threshold) ? 1 : 0;
        }
        
        // جستجوی زاویه بهینه در محدوده -maxAngle تا +maxAngle
        double bestAngle = 0.0;
        double minVariance = Double.MAX_VALUE;
        double step = 0.5; // گام 0.5 درجه
        
        for (double angle = -maxAngle; angle <= maxAngle; angle += step) {
            double variance = calculateProjectionVariance(binaryPixels, width, height, angle);
            if (variance < minVariance) {
                minVariance = variance;
                bestAngle = angle;
            }
        }
        
        // اگر زاویه قابل توجهی پیدا شد (بیش از 0.5 درجه)، تصویر را می‌چرخانیم
        if (Math.abs(bestAngle) > 0.5) {
            log.debug("ImageOcrProvider: Detected skew angle: {} degrees, applying correction", bestAngle);
            return rotateImage(image, bufferedImageClass, -bestAngle);
        }
        
        return image;
    }
    
    /**
     * محاسبه variance از projection profile در یک زاویه مشخص
     * هر چه variance کمتر باشد، projection یکنواخت‌تر است (متن صاف‌تر)
     */
    private double calculateProjectionVariance(int[] binaryPixels, int width, int height, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        
        // محاسبه projection در راستای افقی (horizontal projection)
        int[] projection = new int[height];
        
        for (int y = 0; y < height; y++) {
            int sum = 0;
            for (int x = 0; x < width; x++) {
                // محاسبه مختصات چرخیده
                int rotatedX = (int) (x * cos - y * sin);
                int rotatedY = (int) (x * sin + y * cos);
                
                if (rotatedX >= 0 && rotatedX < width && rotatedY >= 0 && rotatedY < height) {
                    int idx = rotatedY * width + rotatedX;
                    if (idx >= 0 && idx < binaryPixels.length) {
                        sum += binaryPixels[idx];
                    }
                }
            }
            projection[y] = sum;
        }
        
        // محاسبه variance
        double mean = 0.0;
        for (int value : projection) {
            mean += value;
        }
        mean /= projection.length;
        
        double variance = 0.0;
        for (int value : projection) {
            variance += (value - mean) * (value - mean);
        }
        variance /= projection.length;
        
        return variance;
    }
    
    /**
     * چرخش تصویر با زاویه مشخص
     */
    private Object rotateImage(Object image, Class<?> bufferedImageClass, double angleDegrees) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // محاسبه ابعاد جدید بعد از چرخش
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.abs(Math.cos(angleRadians));
        double sin = Math.abs(Math.sin(angleRadians));
        
        int newWidth = (int) (width * cos + height * sin);
        int newHeight = (int) (width * sin + height * cos);
        
        int grayType = TYPE_BYTE_GRAY != null ? TYPE_BYTE_GRAY : 
                      (Integer) bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object rotatedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(newWidth, newHeight, grayType);
        
        Class<?> graphics2DClassRef = graphics2DClass != null ? graphics2DClass : 
                                      Class.forName("java.awt.Graphics2D");
        Class<?> affineTransformClass = Class.forName("java.awt.geom.AffineTransform");
        
        Object graphics = bufferedImageClass.getMethod("createGraphics").invoke(rotatedImage);
        
        // تنظیم کیفیت بالا
        Class<?> renderingHintsClassRef = renderingHintsClass != null ? renderingHintsClass : 
                                          Class.forName("java.awt.RenderingHints");
        Object keyInterpolation = KEY_INTERPOLATION != null ? KEY_INTERPOLATION : 
                                 renderingHintsClassRef.getField("KEY_INTERPOLATION").get(null);
        Object valueInterpolationBicubic = VALUE_INTERPOLATION_BICUBIC != null ? VALUE_INTERPOLATION_BICUBIC : 
                                          renderingHintsClassRef.getField("VALUE_INTERPOLATION_BICUBIC").get(null);
        graphics2DClassRef.getMethod("setRenderingHint", 
                renderingHintsClassRef, renderingHintsClassRef)
                .invoke(graphics, keyInterpolation, valueInterpolationBicubic);
        
        // ایجاد AffineTransform برای چرخش حول مرکز تصویر جدید
        Object transform = affineTransformClass.getMethod("getRotateInstance", double.class, double.class, double.class)
                .invoke(null, angleRadians, newWidth / 2.0, newHeight / 2.0);
        
        graphics2DClassRef.getMethod("setTransform", affineTransformClass)
                .invoke(graphics, transform);
        
        // رسم تصویر چرخیده در مرکز
        int offsetX = (newWidth - width) / 2;
        int offsetY = (newHeight - height) / 2;
        graphics2DClassRef.getMethod("drawImage", 
                bufferedImageClass, int.class, int.class, null)
                .invoke(graphics, image, offsetX, offsetY, null);
        
        graphics2DClassRef.getMethod("dispose").invoke(graphics);
        
        return rotatedImage;
    }
    
    /**
     * Sharpening: افزایش وضوح تصویر با Unsharp Masking
     * 
     * @param strength شدت sharpening (0.0 تا 1.0)
     */
    private Object sharpenImage(Object image, Class<?> bufferedImageClass, double strength) throws Exception {
        int width = (Integer) bufferedImageClass.getMethod("getWidth").invoke(image);
        int height = (Integer) bufferedImageClass.getMethod("getHeight").invoke(image);
        
        // محدود کردن strength به محدوده 0.0 تا 1.0
        strength = Math.max(0.0, Math.min(1.0, strength));
        
        int grayType = TYPE_BYTE_GRAY != null ? TYPE_BYTE_GRAY : 
                      (Integer) bufferedImageClass.getField("TYPE_BYTE_GRAY").getInt(null);
        Object sharpenedImage = bufferedImageClass.getConstructor(int.class, int.class, int.class)
                .newInstance(width, height, grayType);
        
        Class<?> rasterClassRef = rasterClass != null ? rasterClass : 
                                  Class.forName("java.awt.image.Raster");
        Object originalRaster = bufferedImageClass.getMethod("getRaster").invoke(image);
        Object sharpenedRaster = bufferedImageClass.getMethod("getRaster").invoke(sharpenedImage);
        
        int[] originalPixels = (int[]) rasterClassRef.getMethod("getPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(originalRaster, 0, 0, width, height, new int[width * height]);
        
        int[] sharpenedPixels = new int[width * height];
        
        // Unsharp Masking kernel (Laplacian-based)
        // Kernel: [0, -1, 0; -1, 5, -1; 0, -1, 0]
        // با تنظیم strength: original + strength * (original - blurred)
        double[][] kernel = {
            {0, -strength, 0},
            {-strength, 1 + 4 * strength, -strength},
            {0, -strength, 0}
        };
        
        // اعمال convolution
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double sum = 0.0;
                
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int px = x + kx;
                        int py = y + ky;
                        int idx = py * width + px;
                        
                        if (idx >= 0 && idx < originalPixels.length) {
                            int pixel = originalPixels[idx];
                            if (pixel < 0) pixel = 0;
                            if (pixel > 255) pixel = 255;
                            sum += pixel * kernel[ky + 1][kx + 1];
                        }
                    }
                }
                
                int sharpened = (int) Math.round(sum);
                sharpened = Math.max(0, Math.min(255, sharpened));
                sharpenedPixels[y * width + x] = sharpened;
            }
        }
        
        // کپی کردن لبه‌ها (بدون تغییر)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 || y == height - 1 || x == 0 || x == width - 1) {
                    int idx = y * width + x;
                    if (idx < originalPixels.length) {
                        sharpenedPixels[idx] = originalPixels[idx];
                    }
                }
            }
        }
        
        rasterClassRef.getMethod("setPixels", int.class, int.class, int.class, int.class, int[].class)
                .invoke(sharpenedRaster, 0, 0, width, height, sharpenedPixels);
        
        return sharpenedImage;
    }
}


