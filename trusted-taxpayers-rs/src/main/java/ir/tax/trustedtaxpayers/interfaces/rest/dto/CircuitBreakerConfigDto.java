package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO برای تنظیمات CircuitBreaker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "تنظیمات CircuitBreaker برای یک مسیر")
public class CircuitBreakerConfigDto {
    
    @Schema(description = "حد آستانه تعداد خطاها")
    private Integer failureThreshold;
    
    @Schema(description = "نرخ خطا (مثلاً 0.5 برای 50%)")
    private Double failureRate;
    
    @Schema(description = "مدت زمان پنجره بررسی (ثانیه)")
    private Integer windowSeconds;
    
    @Schema(description = "مدت زمان باز بودن circuit (میلی‌ثانیه)")
    private Long openMillis;
    
    @Schema(description = "حداکثر تعداد درخواست در حالت half-open")
    private Integer halfOpenMaxCalls;
    
    @Schema(description = "وضعیت فعال/غیرفعال")
    private Boolean enabled;
}

