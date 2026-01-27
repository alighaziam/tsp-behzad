package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing system settings
 * این سرویس برای مدیریت تنظیمات سیستم استفاده می‌شود.
 * 
 * توجه: این Service از annotation-based cache استفاده می‌کند.
 * CacheService به صورت lazy inject می‌شود برای جلوگیری از circular dependency.
 */
@Service
public class SettingService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<SettingEntity, UUID> {
    
    private final SettingRepository settingRepository;
    
    public SettingService(SettingRepository settingRepository) {
        super(settingRepository);
        this.settingRepository = settingRepository;
    }

    /**
     * دریافت همه تنظیمات با کش دستی
     * توجه: CacheAspect روی این سرویس اعمال نمی‌شود، بنابراین کش به صورت دستی انجام می‌شود
     */
    @Override
    @Transactional(readOnly = true)
    public List<SettingEntity> findAll() {
        final String cacheKey = "CACHE:settings:all";

        try {
            List<SettingEntity> cached = getFromCache(cacheKey, List.class);
            if (cached != null) {
                cached.forEach(this::ensureGroupType);
                return cached;
            }
        } catch (Exception e) {
            // اگر کش مشکل داشت، از دیتابیس بخوان
            // ignore cache errors
        }

        try {
            List<SettingEntity> all = settingRepository.findAll();
            all.forEach(this::ensureGroupType);

            long ttlSeconds = 300L; // Default TTL
            try {
                // استفاده از getValue با مقدار پیش‌فرض برای جلوگیری از exception
                String ttlValue = getValue("cache.ttl.settings", "300");
                if (ttlValue != null && !ttlValue.isBlank()) {
                    ttlSeconds = Long.parseLong(ttlValue.trim());
                }
            } catch (Exception e) {
                // اگر getValue مشکل داشت، از مقدار پیش‌فرض استفاده کن
                ttlSeconds = 300L;
            }

            try {
                putToCache(cacheKey, all, ttlSeconds);
            } catch (Exception e) {
                // اگر کش کردن مشکل داشت، نادیده بگیر - داده را برگردان
                // ignore cache errors
            }
            
            return all;
        } catch (Exception e) {
            // اگر findAll مشکل داشت، لیست خالی برگردان
            return List.of();
        }
    }

    /**
     * بهروزرسانی گروهی تنظیمات + invalidate و warmup کش
     */
    @Transactional
    public List<SettingEntity> updateAll(List<SettingEntity> settings) {
        settings.forEach(setting -> setting.setGroupType(resolveGroupType(setting.getKey(), setting.getGroupType())));

        List<SettingEntity> saved = settingRepository.saveAll(settings);
        // پاک کردن همه ورودیهای کش تنظیمات
        evictByPattern("CACHE:settings:*");
        // گرم کردن کش با صدا زدن findAll (که کششده است)
        findAll();
        return saved;
    }

    /**
     * دریافت تنظیمات بر اساس key
     * Cache می‌شود برای performance بهتر
     */
    @Cacheable(cacheName = "settings", keyPrefix = "key")
    @Transactional(readOnly = true)
    public Optional<SettingEntity> findByKey(String key) {
        return settingRepository.findByKey(key)
                .map(this::ensureGroupType);
    }

    /**
     * دریافت value تنظیمات
     */
    @Cacheable(cacheName = "settings", keyPrefix = "value")
    @Transactional(readOnly = true)
    public Optional<String> getValue(String key) {
        return settingRepository.findByKey(key)
                .map(this::ensureGroupType)
                .map(SettingEntity::getValue);
    }

    /**
     * دریافت value با مقدار پیش‌فرض
     */
    @Transactional(readOnly = true)
    public String getValue(String key, String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    /**
     * بررسی boolean value
     */
    @Cacheable(cacheName = "settings", keyPrefix = "bool")
    @Transactional(readOnly = true)
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return getValue(key)
                .map(value -> "true".equalsIgnoreCase(value.trim()))
                .orElse(defaultValue);
    }

    /**
     * بررسی اینکه آیا تنظیمات فعال است یا نه
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(String key) {
        return getBooleanValue(key, false);
    }

    /**
     * دریافت تنظیمات بر اساس path
     */
    @Cacheable(cacheName = "settings", keyPrefix = "path")
    @Transactional(readOnly = true)
    public List<SettingEntity> findByPath(String path) {
        return settingRepository.findByPath(path)
                .stream()
                .map(this::ensureGroupType)
                .toList();
    }

    /**
     * دریافت تنظیمات بر اساس URL
     */
    @Transactional(readOnly = true)
    public List<SettingEntity> findByUrl(String url) {
        return settingRepository.findByUrl(url)
                .stream()
                .map(this::ensureGroupType)
                .toList();
    }

    /**
     * دریافت تنظیمات بر اساس گروه
     */
    @Cacheable(cacheName = "settings", keyPrefix = "group")
    @Transactional(readOnly = true)
    public List<SettingEntity> findByGroupType(SettingGroupType groupType) {
        if (groupType == null) {
            return List.of();
        }
        return settingRepository.findByGroupType(groupType)
                .stream()
                .map(this::ensureGroupType)
                .toList();
    }

    /**
     * ذخیره یا به‌روزرسانی تنظیمات
     * Cache را invalidate می‌کند
     */
    @CacheEvict(cacheName = "settings", allEntries = true)
    @Transactional
    public SettingEntity saveOrUpdate(String key, String value) {
        return saveOrUpdate(key, value, null);
    }

    @CacheEvict(cacheName = "settings", allEntries = true)
    @Transactional
    public SettingEntity saveOrUpdate(String key, String value, SettingGroupType groupType) {
        Optional<SettingEntity> existing = settingRepository.findByKey(key);

        if (existing.isPresent()) {
            SettingEntity entity = existing.get();
            entity.setValue(value);
            entity.setGroupType(resolveGroupType(key, groupType != null ? groupType : entity.getGroupType()));
            return save(entity);
        } else {
            SettingEntity entity = SettingEntity.builder()
                    .key(key)
                    .value(value)
                    .groupType(resolveGroupType(key, groupType))
                    .build();
            // ID will be auto-generated by BaseEntity.ensureId()
            return save(entity);
        }
    }

    /**
     * به‌روزرسانی value تنظیمات
     */
    @CacheEvict(cacheName = "settings", allEntries = true)
    @Transactional
    public SettingEntity updateValue(String key, String value) {
        SettingEntity entity = settingRepository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        entity.setValue(value);
        entity.setGroupType(resolveGroupType(key, entity.getGroupType()));
        return save(entity);
    }

    /**
     * به‌روزرسانی pass
     */
    @CacheEvict(cacheName = "settings", allEntries = true)
    @Transactional
    public SettingEntity updatePass(String key, String pass) {
        SettingEntity entity = settingRepository.findByKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        entity.setPass(pass);
        entity.setGroupType(resolveGroupType(key, entity.getGroupType()));
        return save(entity);
    }

    @CacheEvict(cacheName = "settings", allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }

    private SettingEntity ensureGroupType(SettingEntity entity) {
        if (entity.getGroupType() == null) {
            entity.setGroupType(resolveGroupType(entity.getKey(), null));
        }
        return entity;
    }

    private SettingGroupType resolveGroupType(String key, SettingGroupType requested) {
        if (requested != null && requested != SettingGroupType.GENERAL) {
            return requested;
        }
        return guessGroupTypeByKey(key);
    }

    private SettingGroupType guessGroupTypeByKey(String key) {
        if (key == null) {
            return SettingGroupType.GENERAL;
        }
        String normalized = key.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("RATE_LIMIT") || normalized.startsWith("RATELIMIT") || normalized.contains("RATE_LIMIT")) {
            return SettingGroupType.RATE_LIMIT;
        }
        if (normalized.startsWith("CB_") || normalized.contains("CIRCUIT") || normalized.contains("BREAK")) {
            return SettingGroupType.CIRCUIT_BREAKER;
        }
        if (normalized.startsWith("REDIS") || normalized.contains("REDIS")) {
            return SettingGroupType.REDIS;
        }
        if (normalized.startsWith("CACHE") || normalized.contains("CACHE.")) {
            return SettingGroupType.CACHE;
        }
        if (normalized.startsWith("SERVICE_") || normalized.contains("SERVICE.")) {
            return SettingGroupType.SERVICE;
        }
        if (normalized.startsWith("SECURITY") || normalized.contains("SECURITY")) {
            return SettingGroupType.SECURITY;
        }
        return SettingGroupType.GENERAL;
    }

}


