package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO برای تنظیمات RateLimit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "تنظیمات RateLimit برای یک مسیر")
public class RateLimitConfigDto {
    
    @Schema(description = "تعداد درخواست مجاز در ثانیه")
    private Integer perSecond;
    
    @Schema(description = "تعداد درخواست مجاز در دقیقه")
    private Integer perMinute;
    
    @Schema(description = "تعداد درخواست مجاز در ساعت")
    private Integer perHour;
    
    @Schema(description = "آیا بر اساس IP محدود شود؟ (در غیر این صورت بر اساس User)")
    private Boolean byIp;
    
    @Schema(description = "لیست IP/User های whitelist (comma-separated)")
    private String whitelist;
    
    @Schema(description = "لیست IP/User های blacklist (comma-separated)")
    private String blacklist;
}

