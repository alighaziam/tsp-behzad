package ir.tax.trustedtaxpayers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * سرویس مدیریت Redis برای عملیات مدیریتی مانند flush
 * 
 * این سرویس برای پاک کردن تمام داده‌های Redis استفاده می‌شود
 * که پس از تغییر serializer لازم است تا داده‌های قدیمی با serializer اشتباه پاک شوند.
 */
@Slf4j
@Service
public class RedisManagementService {
	
	private final RedisTemplate<String, Object> redisTemplate;

	public RedisManagementService(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * پاک کردن تمام داده‌های Redis (FLUSHALL)
	 * 
	 * ⚠️ هشدار: این عملیات تمام داده‌های Redis را پاک می‌کند!
	 * 
	 * @return تعداد کلیدهای پاک شده
	 */
	public long flushAll() {
		try {
			// استفاده از SCAN برای پیدا کردن تمام کلیدها
			List<String> allKeys = scanAllKeys();
			
			if (allKeys.isEmpty()) {
				log.info("No keys found in Redis to flush");
				return 0;
			}
			
			// حذف تمام کلیدها
			long deletedCount = redisTemplate.delete(allKeys);
			log.info("Flushed Redis: deleted {} keys out of {} total keys", deletedCount, allKeys.size());
			
			return deletedCount;
		} catch (Exception e) {
			log.error("Failed to flush Redis: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to flush Redis", e);
		}
	}

	/**
	 * پاک کردن تمام کلیدهای cache (با pattern CACHE:*)
	 * 
	 * @return تعداد کلیدهای پاک شده
	 */
	public long flushCache() {
		try {
			List<String> cacheKeys = scanKeys("CACHE:*");
			
			if (cacheKeys.isEmpty()) {
				log.info("No cache keys found in Redis to flush");
				return 0;
			}
			
			long deletedCount = redisTemplate.delete(cacheKeys);
			log.info("Flushed cache: deleted {} keys", deletedCount);
			
			return deletedCount;
		} catch (Exception e) {
			log.error("Failed to flush cache: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to flush cache", e);
		}
	}

	/**
	 * دریافت تعداد کلیدهای موجود در Redis
	 * 
	 * @return تعداد کلیدها
	 */
	public long getKeyCount() {
		try {
			List<String> allKeys = scanAllKeys();
			return allKeys.size();
		} catch (Exception e) {
			log.error("Failed to count keys: {}", e.getMessage(), e);
			return -1;
		}
	}

	/**
	 * SCAN تمام کلیدها با استفاده از pattern "*"
	 * 
	 * @return لیست تمام کلیدها
	 */
	private List<String> scanAllKeys() {
		return scanKeys("*");
	}

	/**
	 * استفاده از SCAN به جای KEYS برای عملکرد بهتر در production
	 * 
	 * @param pattern الگوی جستجو
	 * @return لیست کلیدهای پیدا شده
	 */
	private List<String> scanKeys(String pattern) {
		List<String> keys = new ArrayList<>();
		try {
			try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
				.match(pattern)
				.count(1000) // تعداد بیشتر برای سرعت بهتر
				.build())) {
				cursor.forEachRemaining(keys::add);
			}
			log.debug("Scanned {} keys for pattern: {}", keys.size(), pattern);
		} catch (Exception e) {
			log.warn("SCAN failed for pattern: {}, falling back to KEYS. Error: {}", pattern, e.getMessage());
			// Fallback to KEYS if SCAN fails (for older Redis versions or connection issues)
			try {
				Set<String> keysFromKeys = redisTemplate.keys(pattern);
				if (keysFromKeys != null) {
					keys.addAll(keysFromKeys);
					log.debug("Found {} keys using KEYS fallback for pattern: {}", keys.size(), pattern);
				}
			} catch (Exception fallbackException) {
				log.error("Both SCAN and KEYS failed for pattern: {}", pattern, fallbackException);
				throw new RuntimeException("Failed to scan Redis keys", fallbackException);
			}
		}
		return keys;
	}
}

