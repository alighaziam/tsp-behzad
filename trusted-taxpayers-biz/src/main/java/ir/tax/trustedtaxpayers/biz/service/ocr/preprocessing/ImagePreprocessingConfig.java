package ir.tax.trustedtaxpayers.biz.service.ocr.preprocessing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * تنظیمات پیش‌پردازش تصویر برای OCR
 */
@Data
@Component
@ConfigurationProperties(prefix = "ocr.preprocessing")
public class ImagePreprocessingConfig {
    
    /**
     * فعال/غیرفعال کردن کاهش نویز
     */
    private NoiseReduction noiseReduction = new NoiseReduction();
    
    /**
     * تنظیمات Thresholding
     */
    private Thresholding thresholding = new Thresholding();
    
    /**
     * تنظیمات عملیات مورفولوژیک
     */
    private Morphology morphology = new Morphology();
    
    /**
     * تنظیمات Deskewing (اصلاح کجی)
     */
    private Deskewing deskewing = new Deskewing();
    
    /**
     * تنظیمات بهبود کنتراست
     */
    private Contrast contrast = new Contrast();
    
    /**
     * تنظیمات Sharpening
     */
    private Sharpening sharpen = new Sharpening();
    
    @Data
    public static class NoiseReduction {
        /**
         * فعال/غیرفعال کردن کاهش نویز
         */
        private boolean enabled = true;
        
        /**
         * نوع فیلتر: MEDIAN (پیش‌فرض), GAUSSIAN
         */
        private String method = "MEDIAN";
        
        /**
         * اندازه kernel (باید فرد باشد: 3, 5, 7)
         */
        private int kernelSize = 3;
    }
    
    @Data
    public static class Thresholding {
        /**
         * روش thresholding: OTSU (پیش‌فرض), ADAPTIVE, AUTO
         * AUTO: برای تصاویر بزرگ از ADAPTIVE، برای کوچک از OTSU استفاده می‌کند
         */
        private String method = "AUTO";
        
        /**
         * اندازه بلوک برای Adaptive Thresholding (باید فرد باشد)
         */
        private int adaptiveBlockSize = 11;
        
        /**
         * ثابت C برای Adaptive Thresholding
         */
        private int adaptiveC = 2;
    }
    
    @Data
    public static class Morphology {
        /**
         * فعال/غیرفعال کردن عملیات مورفولوژیک
         */
        private boolean enabled = true;
        
        /**
         * نوع عملیات: CLOSE (پیش‌فرض), OPEN, DILATE, ERODE
         */
        private String operation = "CLOSE";
        
        /**
         * اندازه kernel (باید فرد باشد)
         */
        private int kernelSize = 2;
        
        /**
         * تعداد iterations
         */
        private int iterations = 1;
    }
    
    @Data
    public static class Deskewing {
        /**
         * فعال/غیرفعال کردن Deskewing
         */
        private boolean enabled = true;
        
        /**
         * حداکثر زاویه برای اصلاح (درجه)
         */
        private double maxAngle = 15.0;
    }
    
    @Data
    public static class Contrast {
        /**
         * فعال/غیرفعال کردن بهبود کنتراست
         */
        private boolean enabled = true;
        
        /**
         * روش بهبود: HISTOGRAM (پیش‌فرض), STRETCHING
         */
        private String method = "HISTOGRAM";
    }
    
    @Data
    public static class Sharpening {
        /**
         * فعال/غیرفعال کردن Sharpening
         */
        private boolean enabled = false;
        
        /**
         * شدت sharpening (0.0 تا 1.0)
         */
        private double strength = 0.5;
    }
}

