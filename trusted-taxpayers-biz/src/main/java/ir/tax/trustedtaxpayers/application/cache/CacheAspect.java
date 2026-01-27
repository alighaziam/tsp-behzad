package ir.tax.trustedtaxpayers.application.cache;

import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AOP Aspect for Redis caching
 * این Aspect annotation های @Cacheable و @CacheEvict را پردازش می‌کند
 */
@Slf4j
@Aspect
@Component
public class CacheAspect {

	private final RedisTemplate<String, Object> redisTemplate;
	private final CacheToggleService toggleService;
	private final ir.tax.trustedtaxpayers.application.service.SettingService settingService;

	public CacheAspect(RedisTemplate<String, Object> redisTemplate, CacheToggleService toggleService,
			ir.tax.trustedtaxpayers.application.service.SettingService settingService) {
		this.redisTemplate = redisTemplate;
		this.toggleService = toggleService;
		this.settingService = settingService;
	}

	@Around("execution(* ir.tax.trustedtaxpayers.application.service..*(..)) " +
			"&& !within(ir.tax.trustedtaxpayers.application.cache..*) " +
			"&& !within(ir.tax.trustedtaxpayers.application.service.SettingService) " +
			"&& !within(ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl) " +
			"&& !within(ir.tax.trustedtaxpayers.application.service.CacheService)")
	public Object aroundServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

		// NoCache -> skip any caching logic
		if (method.isAnnotationPresent(NoCache.class)) {
			log.debug("Method {} has @NoCache annotation, skipping cache", method.getName());
			return joinPoint.proceed();
		}

		// Identify service and method path
		String classFqn = joinPoint.getTarget().getClass().getName();
		String methodPath = joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();

		// Auto-upsert service identity and toggles into DB
		upsertIfAbsent("SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.SERVICE);
		upsertIfAbsent("SERVICE_" + classFqn + "/path", methodPath, SettingGroupType.SERVICE);
		upsertIfAbsent("CACHE_SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.CACHE);
		upsertIfAbsent("CACHE_" + methodPath + "/enabled", "true", SettingGroupType.CACHE);

		// If disabled by flags -> bypass
		if (!resolveBoolean("cache.enabled", true, SettingGroupType.CACHE)) {
			log.trace("Global cache disabled via settings, executing method: {}", method.getName());
			return joinPoint.proceed();
		}
		if (!resolveBoolean("CACHE_SERVICE_" + classFqn + "/enabled", true, SettingGroupType.CACHE)) {
			log.trace("Cache disabled for service via settings: {}", classFqn);
			return joinPoint.proceed();
		}
		if (!resolveBoolean("CACHE_" + methodPath + "/enabled", true, SettingGroupType.CACHE)) {
			log.trace("Cache disabled for method via settings: {}", methodPath);
			return joinPoint.proceed();
		}

		// If globally disabled (legacy toggle) -> bypass
		if (!toggleService.isRedisEnabled()) {
			log.trace("Redis cache is globally disabled, executing method: {}", method.getName());
			return joinPoint.proceed();
		}

		try {
			// Evict after successful execution
			if (method.isAnnotationPresent(CacheEvict.class) || method.isAnnotationPresent(CacheEvictList.class)) {
				// Support repeatable CacheEvict
				CacheEvict[] evicts = method.getAnnotationsByType(CacheEvict.class);
				Object result = joinPoint.proceed();
				for (CacheEvict evict : evicts) {
					clearCache(evict, joinPoint);
				}
				return result;
			}

			// Cacheable: try cache, otherwise proceed and store
			if (method.isAnnotationPresent(Cacheable.class)) {
				return handleCacheable(joinPoint, method, method.getAnnotation(Cacheable.class));
			}
		} catch (Exception e) {
			log.warn("Cache operation failed for method: {}, proceeding without cache. Error: {}", 
				method.getName(), e.getMessage());
			// در صورت خطا در cache، متد را بدون cache اجرا می‌کنیم
			return joinPoint.proceed();
		}

		return joinPoint.proceed();
	}

	private void upsertIfAbsent(String key, String value, SettingGroupType groupType) {
		try {
			if (settingService.getValue(key).isEmpty()) {
				settingService.saveOrUpdate(key, value, groupType);
			}
		} catch (DataAccessException e) {
			// Database table might not exist during initialization (e.g., Flyway not run yet)
			// Log and continue with default behavior
			log.debug("Cannot access SETTINGS table during initialization for key: {}. Error: {}. Using default behavior.", 
				key, e.getMessage());
		} catch (Exception e) {
			// Other unexpected errors - log but don't fail the application
			log.warn("Unexpected error accessing SETTINGS for key: {}. Error: {}. Using default behavior.", 
				key, e.getMessage());
		}
	}

	private boolean resolveBoolean(String key, boolean defaultVal, SettingGroupType groupType) {
		try {
			return settingService.getValue(key)
				.map(v -> {
					String n = v.trim().toLowerCase();
					return !("false".equals(n) || "0".equals(n) || "off".equals(n) || "disabled".equals(n));
				})
				.orElseGet(() -> {
					try {
						settingService.saveOrUpdate(key, String.valueOf(defaultVal), groupType);
					} catch (DataAccessException e) {
						// Database table might not exist during initialization
						log.debug("Cannot save setting during initialization for key: {}. Using default value: {}.", 
							key, defaultVal);
					} catch (Exception e) {
						log.warn("Unexpected error saving setting for key: {}. Using default value: {}.", 
							key, defaultVal);
					}
					return defaultVal;
				});
		} catch (DataAccessException e) {
			// Database table might not exist during initialization (e.g., Flyway not run yet)
			// Return default value and log
			log.debug("Cannot access SETTINGS table during initialization for key: {}. Using default value: {}. Error: {}", 
				key, defaultVal, e.getMessage());
			return defaultVal;
		} catch (Exception e) {
			// Other unexpected errors - return default and log
			log.warn("Unexpected error accessing SETTINGS for key: {}. Using default value: {}. Error: {}", 
				key, defaultVal, e.getMessage());
			return defaultVal;
		}
	}

	private Object handleCacheable(ProceedingJoinPoint joinPoint, Method method, Cacheable cacheable) throws Throwable {
		String resolvedCacheName = resolveCacheName(cacheable.cacheName(), joinPoint.getTarget().getClass());
		String key = buildKey(resolvedCacheName, cacheable.keyPrefix(), joinPoint.getArgs());
		log.debug("Cache lookup for key: {}", key);

		try {
			Object cached = redisTemplate.opsForValue().get(key);
			if (cached != null) {
				log.debug("Cache HIT for key: {}", key);
				return cached;
			}
		} catch (Exception e) {
			log.warn("Failed to read from cache for key: {}, error: {}", key, e.getMessage());
		}

		log.debug("Cache MISS for key: {}, executing method", key);
		Object result = joinPoint.proceed();

		try {
			long ttl = cacheable.ttl() > 0 ? cacheable.ttl() : toggleService.getDefaultTtlSeconds();
			if (ttl > 0) {
				redisTemplate.opsForValue().set(key, result, ttl, TimeUnit.SECONDS);
				log.debug("Cached result with TTL {}s for key: {}", ttl, key);
			} else {
				redisTemplate.opsForValue().set(key, result);
				log.debug("Cached result without TTL for key: {}", key);
			}
		} catch (Exception e) {
			log.warn("Failed to write to cache for key: {}, error: {}", key, e.getMessage());
			// در صورت خطا در نوشتن cache، نتیجه را برمی‌گردانیم (بدون cache)
		}

		return result;
	}

	private void clearCache(CacheEvict evict, ProceedingJoinPoint joinPoint) {
		String cacheName = resolveCacheName(evict.cacheName(), joinPoint.getTarget().getClass());
		String pattern = evict.allEntries() ?
			getNamespace(cacheName) + ":*" :
			getNamespace(cacheName) + ":" + nullToEmpty(evict.keyPrefix()) + ":*";

		log.debug("Clearing cache with pattern: {}", pattern);

		try {
			List<String> keysToDelete = scanKeys(pattern);
			if (!keysToDelete.isEmpty()) {
				redisTemplate.delete(keysToDelete);
				log.debug("Deleted {} cache entries for pattern: {}", keysToDelete.size(), pattern);
			}
		} catch (Exception e) {
			log.warn("Failed to clear cache for pattern: {}, error: {}", pattern, e.getMessage());
		}
	}

	/**
	 * استفاده از SCAN به جای KEYS برای عملکرد بهتر در production
	 */
	private List<String> scanKeys(String pattern) {
		List<String> keys = new ArrayList<>();
		try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
			.match(pattern)
			.count(100)
			.build())) {
			cursor.forEachRemaining(keys::add);
		} catch (Exception e) {
			log.warn("SCAN failed for pattern: {}, falling back to KEYS. Error: {}", pattern, e.getMessage());
			// Fallback to KEYS if SCAN fails (for older Redis versions or connection issues)
			var keysFromKeys = redisTemplate.keys(pattern);
			if (keysFromKeys != null) {
				keys.addAll(keysFromKeys);
			}
		}
		return keys;
	}

	private String buildKey(String cacheName, String keyPrefix, Object[] args) {
		String namespace = getNamespace(cacheName);
		String prefix = nullToEmpty(keyPrefix);
		String argsHash = computeHash(args);
		return namespace + ":" + prefix + (prefix.isEmpty() ? "" : ":") + argsHash;
	}

	private String resolveCacheName(String annotationValue, Class<?> targetClass) {
		if (annotationValue != null && !annotationValue.isEmpty()) {
			return annotationValue;
		}
		return targetClass.getSimpleName();
	}

	/**
	 * استفاده از SHA-256 برای کاهش collision risk
	 */
	private String computeHash(Object[] args) {
		if (args == null || args.length == 0) {
			return "0";
		}

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			String argsString = Arrays.deepToString(args);
			byte[] hashBytes = digest.digest(argsString.getBytes(StandardCharsets.UTF_8));
			
			StringBuilder hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			log.warn("SHA-256 not available, falling back to hashCode", e);
			return String.valueOf(Arrays.deepHashCode(args));
		}
	}

	private String getNamespace(String cacheName) {
		return "CACHE:" + cacheName;
	}

	private String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}


