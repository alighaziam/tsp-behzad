package ir.tax.trustedtaxpayers.application.cache;

import java.lang.annotation.*;

/**
 * Annotation for caching method results
 * استفاده از این annotation برای cache کردن نتیجه متدها
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
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
     * TTL in seconds (optional)
     * اگر 0 یا منفی باشد، از default TTL استفاده می‌شود
     * Time to live به ثانیه
     */
    long ttl() default 0;

    /**
     * Include method parameters in key generation (compatibility flag)
     * جهت سازگاری با کد موجود؛ در Aspect فعلی نادیده گرفته می‌شود
     */
    boolean includeParameters() default true;
}


