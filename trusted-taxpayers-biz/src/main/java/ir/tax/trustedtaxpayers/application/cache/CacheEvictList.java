package ir.tax.trustedtaxpayers.application.cache;

import java.lang.annotation.*;

/**
 * Container annotation for multiple @CacheEvict annotations
 * این annotation برای استفاده از چندین @CacheEvict روی یک متد استفاده می‌شود
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvictList {
	CacheEvict[] value();
}


