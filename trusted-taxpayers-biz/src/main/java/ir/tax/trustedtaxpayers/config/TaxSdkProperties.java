package ir.tax.trustedtaxpayers.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * تنظیمات SDK سامانه مودیان
 * Tax System SDK Properties
 */
@Data
@Validated
@ConfigurationProperties(prefix = "tax.sdk")
public class TaxSdkProperties {

    /**
     * آدرس پایه سرور مالیات
     * Base URL of tax server
     * 
     * Example for production: https://tp.tax.gov.ir/requestsmanager/api/self-tsp
     * Example for test: https://tp-nta.abriment.mohaymen.ir/requestsmanager/api/self-tsp
     */
    @NotBlank(message = "Tax SDK base URL must not be blank")
    private String baseUrl;

    /**
     * شناسه حافظه مالیاتی (Memory ID / Fiscal ID)
     * Tax memory identifier
     * 
     * Example: A111H3
     */
    @NotBlank(message = "Tax memory ID must not be blank")
    private String memoryId;

    /**
     * مسیر فایل کلید خصوصی (Private Key)
     * Path to private key file
     * 
     * Example: /path/to/private.pem or classpath:keys/private.pem
     */
    @NotBlank(message = "Private key path must not be blank")
    private String privateKeyPath;

    /**
     * رمز عبور کلید خصوصی (در صورت وجود)
     * Private key password (if exists)
     */
    private String privateKeyPassword;

    /**
     * شناسه مشتری (برای TSP با کلید خود)
     * Client ID (for TSP with own key)
     * 
     * Example: TP1101
     */
    private String clientId;

    /**
     * فعال یا غیرفعال بودن SDK
     * Enable or disable SDK
     */
    private boolean enabled = true;

    /**
     * تعداد تلاش مجدد در صورت خطا
     * Number of retry attempts on failure
     */
    private int retryAttempts = 3;

    /**
     * زمان انتظار بین تلاش‌های مجدد (میلی‌ثانیه)
     * Wait time between retry attempts (milliseconds)
     */
    private long retryDelayMs = 1000;

    /**
     * زمان انتظار برای درخواست‌ها (میلی‌ثانیه)
     * Request timeout (milliseconds)
     */
    private long requestTimeoutMs = 30000;
}


