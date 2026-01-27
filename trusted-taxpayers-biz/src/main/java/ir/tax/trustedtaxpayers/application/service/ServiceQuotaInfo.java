package ir.tax.trustedtaxpayers.application.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * اطلاعات سهمیه و محدودیت خدمات
 */
public record ServiceQuotaInfo(
        UUID taxpayerId,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        BigDecimal limitValue,
        String limitUnit,
        Long used,
        Long remaining,
        Double usagePercentage,
        String status,
        Boolean isUnlimited
) {
    public static ServiceQuotaInfo noAccess() {
        return new ServiceQuotaInfo(
            null, null, null, null, null, null, 
            0L, 0L, 0.0, "NO_ACCESS", false
        );
    }

    public static ServiceQuotaInfo unlimited() {
        return new ServiceQuotaInfo(
            null, null, null, null, null, "UNLIMITED",
            0L, Long.MAX_VALUE, 0.0, "OK", true
        );
    }
}

