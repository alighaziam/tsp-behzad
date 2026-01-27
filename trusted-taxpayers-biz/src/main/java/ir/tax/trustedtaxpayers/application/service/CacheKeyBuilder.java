package ir.tax.trustedtaxpayers.application.service;

/**
 * Builder for type-safe cache keys
 * این کلاس برای ساخت cache keys به صورت type-safe استفاده می‌شود.
 * 
 * استفاده:
 * CacheKeyBuilder.taxpayer("12345678901").build()
 * CacheKeyBuilder.taxpayer("exists").with("12345678901").build()
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public class CacheKeyBuilder {
    
    private final StringBuilder keyBuilder;
    
    private CacheKeyBuilder(String prefix) {
        this.keyBuilder = new StringBuilder(prefix);
    }
    
    public static CacheKeyBuilder taxpayer(String key) {
        return new CacheKeyBuilder(key);
    }
    
    public static CacheKeyBuilder template(String key) {
        return new CacheKeyBuilder(key);
    }
    
    public static CacheKeyBuilder buyer(String key) {
        return new CacheKeyBuilder(key);
    }
    
    public static CacheKeyBuilder user(String key) {
        return new CacheKeyBuilder(key);
    }
    
    public static CacheKeyBuilder setting(String key) {
        return new CacheKeyBuilder(key);
    }
    
    public CacheKeyBuilder with(String value) {
        if (value != null && !value.isEmpty()) {
            keyBuilder.append(":").append(value);
        }
        return this;
    }
    
    public CacheKeyBuilder with(Object value) {
        if (value != null) {
            keyBuilder.append(":").append(value);
        }
        return this;
    }
    
    public String build() {
        return keyBuilder.toString();
    }
    
    // Predefined key patterns
    
    public static String taxpayerExists(String taxpayerNo) {
        return CacheKeyBuilder.taxpayer("exists").with(taxpayerNo).build();
    }
    
    public static String buyerNationalId(String nationalId) {
        return CacheKeyBuilder.buyer("nationalId").with(nationalId).build();
    }
    
    public static String buyerExists(String nationalId) {
        return CacheKeyBuilder.buyer("exists:nationalId").with(nationalId).build();
    }
    
    public static String templateType(String typeCode) {
        return CacheKeyBuilder.template("type").with(typeCode).build();
    }
    
    public static String templateActive(String active) {
        return CacheKeyBuilder.template("active").with(active).build();
    }
    
    public static String settingValue(String key) {
        return CacheKeyBuilder.setting("value").with(key).build();
    }
    
    public static String settingBool(String key) {
        return CacheKeyBuilder.setting("bool").with(key).build();
    }
}


