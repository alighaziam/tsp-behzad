package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO برای نمایش اطلاعات کامل یک سرویس/کنترلر
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "اطلاعات سرویس شامل تنظیمات RateLimit، CircuitBreaker و Cache")
public class ServiceInfoDto {
    
    @Schema(description = "نام کامل کلاس کنترلر (FQN)")
    private String classFqn;
    
    @Schema(description = "نام ساده کلاس")
    private String simpleClassName;
    
    @Schema(description = "مسیر پایه سرویس")
    private String servicePath;
    
    @Schema(description = "وضعیت فعال/غیرفعال سرویس")
    private Boolean serviceEnabled;
    
    @Schema(description = "وضعیت فعال/غیرفعال RateLimit برای این سرویس")
    private Boolean rateLimitEnabled;
    
    @Schema(description = "وضعیت فعال/غیرفعال CircuitBreaker برای این سرویس")
    private Boolean circuitBreakerEnabled;
    
    @Schema(description = "وضعیت فعال/غیرفعال Cache برای این سرویس")
    private Boolean cacheEnabled;
    
    @Schema(description = "تنظیمات RateLimit (اگر وجود دارد)")
    private RateLimitConfigDto rateLimitConfig;
    
    @Schema(description = "تنظیمات CircuitBreaker (اگر وجود دارد)")
    private CircuitBreakerConfigDto circuitBreakerConfig;
}

