package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "پاسخ بررسی دسترسی دسته‌ای به خدمات")
public record BatchServiceAccessResponse(
        @Schema(description = "نقشه دسترسی (taxpayerId -> hasAccess)") Map<UUID, Boolean> accessMap,
        @Schema(description = "تعداد کل") Integer totalCount,
        @Schema(description = "تعداد دسترسی‌های موفق") Integer grantedCount,
        @Schema(description = "تعداد دسترسی‌های ناموفق") Integer deniedCount
) {
}

