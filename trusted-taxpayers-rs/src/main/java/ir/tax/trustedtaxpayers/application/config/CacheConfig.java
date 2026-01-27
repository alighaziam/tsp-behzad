package ir.tax.trustedtaxpayers.application.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Spring Cache
 * این کلاس برای فعال‌سازی و پیکربندی سیستم Cache در Spring استفاده می‌شود.
 * 
 * Cache مزایای زیر را دارد:
 * - کاهش تعداد کوئری به دیتابیس
 * - بهبود سرعت پاسخ‌دهی API
 * - کاهش بار روی دیتابیس
 * 
 * Redis cache با GenericJackson2JsonRedisSerializer استفاده می‌شود.
 * CacheManager در RedisConfig تعریف شده است.
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Cache configuration uses Redis with GenericJackson2JsonRedisSerializer
    // CacheManager bean is defined in RedisConfig
}


