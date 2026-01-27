package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "اطلاعات استفاده از خدمات")
public record ServiceUsageDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه مودی") UUID taxpayerId,
        @Schema(description = "شناسه اشتراک") UUID subscriptionId,
        @Schema(description = "شناسه خدمات") UUID serviceId,
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "تاریخ استفاده") LocalDate usageDate,
        @Schema(description = "تعداد استفاده") Integer usageCount,
        @Schema(description = "نوع استفاده") String usageType,
        @Schema(description = "شناسه مرجع") UUID referenceId,
        @Schema(description = "نوع مرجع") String referenceType,
        @Schema(description = "یادداشت‌ها") String notes,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn,
        @Schema(description = "تاریخ بروزرسانی") OffsetDateTime updatedOn
) {
}

