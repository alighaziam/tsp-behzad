package ir.tax.trustedtaxpayers.application.cache;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for managing Redis cache toggle state
 * این سرویس وضعیت فعال/غیرفعال بودن Redis cache را مدیریت می‌کند
 * و از cache محلی برای کاهش query به دیتابیس استفاده می‌کند
 */
@Service
public class CacheToggleService {

	private static final Logger logger = LoggerFactory.getLogger(CacheToggleService.class);

	public static final String REDIS_ENABLED_KEY = "cache.redis.enabled";

	private final SettingRepository settingRepository;

	@Value("${application.cache.default-ttl-seconds:300}")
	private long defaultTtlSeconds;

	@Value("${application.cache.toggle-cache-ttl-seconds:60}")
	private long toggleCacheTtlSeconds;

	// In-memory cache برای وضعیت Redis
	private final AtomicReference<Boolean> cachedEnabledState = new AtomicReference<>(null);
	private final AtomicLong lastCacheUpdateTime = new AtomicLong(0);

	public CacheToggleService(SettingRepository settingRepository) {
		this.settingRepository = settingRepository;
	}

	public boolean isRedisEnabled() {
		long now = System.currentTimeMillis();
		long lastUpdate = lastCacheUpdateTime.get();
		Boolean cached = cachedEnabledState.get();

		// اگر cache معتبر است (کمتر از TTL گذشته)، از cache استفاده می‌کنیم
		if (cached != null && (now - lastUpdate) < (toggleCacheTtlSeconds * 1000)) {
			return cached;
		}

		// در غیر این صورت از دیتابیس می‌خوانیم
		boolean enabled = settingRepository.findByKey(REDIS_ENABLED_KEY)
			.map(this::resolveEnabledFromSetting)
			.orElse(true);

		// به‌روزرسانی cache
		cachedEnabledState.set(enabled);
		lastCacheUpdateTime.set(now);

		logger.debug("Redis enabled status refreshed: {} (cached for {}s)", enabled, toggleCacheTtlSeconds);
		return enabled;
	}

	/**
	 * پاک کردن cache برای اجبار به خواندن مجدد از دیتابیس
	 * مفید است وقتی که تنظیمات در دیتابیس تغییر می‌کند
	 */
	public void invalidateCache() {
		cachedEnabledState.set(null);
		lastCacheUpdateTime.set(0);
		logger.debug("Cache toggle cache invalidated");
	}

	public long getDefaultTtlSeconds() {
		return defaultTtlSeconds;
	}

	private boolean resolveEnabledFromSetting(SettingEntity s) {
		if (!s.getIsActive()) {
			logger.debug("Redis cache disabled: setting is not active");
			return false;
		}
		String value = s.getValue();
		if (value == null) {
			return true;
		}
		String norm = value.trim().toLowerCase(Locale.ROOT);
		if ("false".equals(norm) || "0".equals(norm) || "off".equals(norm) || "disabled".equals(norm)) {
			logger.debug("Redis cache disabled: value is '{}'", value);
			return false;
		}
		return true;
	}
}
