package ir.tax.trustedtaxpayers.enums;

/**
 * گروه‌بندی تنظیمات سیستم برای تفکیک راحت‌تر RateLimit، CircuitBreaker، Redis و ...
 */
public enum SettingGroupType {
    RATE_LIMIT,
    CIRCUIT_BREAKER,
    REDIS,
    CACHE,
    SERVICE,
    SECURITY,
    GENERAL
}

