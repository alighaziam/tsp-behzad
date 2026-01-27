package ir.tax.trustedtaxpayers.application.cache;

import java.lang.annotation.*;

/**
 * Annotation for evicting cache entries
 * استفاده از این annotation برای پاک کردن cache
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(CacheEvictList.class)
public @interface CacheEvict {
    
    /**
     * Cache name (optional)
     * اگر خالی باشد، از نام کلاس سرویس استفاده می‌شود
     */
    String cacheName() default "";
    
    /**
     * Key prefix for cache key (optional)
     * پیشوند برای کلید cache
     */
    String keyPrefix() default "";
    
    /**
     * Whether to evict all entries in the cache (default: false)
     * آیا تمام cache entries پاک شوند؟
     */
    boolean allEntries() default false;
}


