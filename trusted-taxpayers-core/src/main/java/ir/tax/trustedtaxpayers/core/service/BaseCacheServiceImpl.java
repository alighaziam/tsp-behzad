package ir.tax.trustedtaxpayers.core.service;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Base Service با پشتیبانی از Cache
 * 
 * <p>ویژگی‌ها:</p>
 * <ul>
 *   <li>Redis dependency اختیاری است (اگر موجود نباشد، cache operations نادیده گرفته می‌شوند)</li>
 *   <li>پشتیبانی از TTL برای cache entries</li>
 *   <li>استفاده از SCAN به جای KEYS برای performance بهتر</li>
 * </ul>
 * 
 * @param <T> نوع Entity
 * @param <ID> نوع شناسه
 */
@Slf4j
public abstract class BaseCacheServiceImpl<T, ID> extends BaseServiceImpl<T, ID> {
	
	private static final int DEFAULT_SCAN_COUNT = 100;

	@Autowired(required = false)
	protected RedisTemplate<String, Object> redisTemplate;

	/**
	 * Constructor با dependency injection
	 * 
	 * @param repository Repository مربوط به Entity
	 */
	protected BaseCacheServiceImpl(BaseRepository<T, ID> repository) {
		super(repository);
	}
	
	/**
	 * بررسی اینکه Redis در دسترس است یا نه
	 * 
	 * @return true اگر Redis در دسترس باشد
	 */
	protected boolean isCacheAvailable() {
		return redisTemplate != null;
	}

	/**
	 * دریافت مقدار از cache
	 * 
	 * با استفاده از GenericJackson2JsonRedisSerializer با default typing،
	 * داده‌ها به صورت خودکار به نوع اصلی (Entity/DTO) deserialize می‌شوند
	 * و دیگر به LinkedHashMap تبدیل نمی‌شوند.
	 * 
	 * @param key کلید cache
	 * @param type نوع کلاس مورد انتظار
	 * @param <R> نوع بازگشتی
	 * @return مقدار cache شده یا null
	 */
	@SuppressWarnings("unchecked")
	protected <R> R getFromCache(String key, Class<R> type) {
		if (!isCacheAvailable()) {
			return null;
		}
		try {
			Object value = redisTemplate.opsForValue().get(key);
			if (value == null) {
				return null;
			}
			// با GenericJackson2JsonRedisSerializer با default typing،
			// مقدار باید به صورت خودکار به نوع صحیح deserialize شده باشد
			// اگر نوع مطابقت نداشته باشد، ClassCastException رخ می‌دهد که طبیعی است
			if (type.isInstance(value)) {
				return (R) value;
			} else {
				log.warn("Cached value for key {} is of type {} but expected type {}", 
					key, value.getClass().getName(), type.getName());
				return null;
			}
		} catch (ClassCastException e) {
			log.warn("Failed to cast cached value for key: {} to type: {}, error: {}", 
				key, type.getName(), e.getMessage());
			return null;
		} catch (Exception e) {
			log.warn("Failed to get from cache for key: {}, error: {}", key, e.getMessage());
			return null;
		}
	}

	/**
	 * ذخیره مقدار در cache
	 * 
	 * @param key کلید cache
	 * @param value مقدار برای ذخیره
	 * @param ttlSeconds زمان انقضا به ثانیه (0 = بدون انقضا)
	 */
	protected void putToCache(String key, Object value, long ttlSeconds) {
		if (!isCacheAvailable()) {
			return;
		}
		try {
			if (ttlSeconds > 0) {
				redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
				log.debug("Cached value for key: {} with TTL: {} seconds", key, ttlSeconds);
			} else {
				redisTemplate.opsForValue().set(key, value);
				log.debug("Cached value for key: {} without TTL", key);
			}
		} catch (Exception e) {
			log.warn("Failed to put to cache for key: {}, error: {}", key, e.getMessage());
		}
	}
	
	/**
	 * حذف یک کلید از cache
	 * 
	 * @param key کلید برای حذف
	 */
	protected void evictFromCache(String key) {
		if (!isCacheAvailable()) {
			return;
		}
		try {
			redisTemplate.delete(key);
			log.debug("Evicted cache for key: {}", key);
		} catch (Exception e) {
			log.warn("Failed to evict cache for key: {}, error: {}", key, e.getMessage());
		}
	}

	/**
	 * پاک کردن cache entries با استفاده از pattern
	 * از SCAN استفاده می‌کند برای performance بهتر در production
	 */
	protected void evictByPattern(String pattern) {
		if (redisTemplate == null) {
			return;
		}
		try {
			List<String> keysToDelete = scanKeys(pattern);
			if (!keysToDelete.isEmpty()) {
				redisTemplate.delete(keysToDelete);
				log.debug("Deleted {} cache entries for pattern: {}", keysToDelete.size(), pattern);
			}
		} catch (Exception e) {
			log.warn("Failed to evict cache by pattern: {}, error: {}", pattern, e.getMessage());
		}
	}

	/**
	 * استفاده از SCAN به جای KEYS برای عملکرد بهتر در production
	 * 
	 * @param pattern الگوی جستجو
	 * @return لیست کلیدهای پیدا شده
	 */
	private List<String> scanKeys(String pattern) {
		if (!isCacheAvailable()) {
			return new ArrayList<>();
		}
		
		List<String> keys = new ArrayList<>();
		try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
			.match(pattern)
			.count(DEFAULT_SCAN_COUNT)
			.build())) {
			cursor.forEachRemaining(keys::add);
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
			}
		}
		return keys;
	}
}

