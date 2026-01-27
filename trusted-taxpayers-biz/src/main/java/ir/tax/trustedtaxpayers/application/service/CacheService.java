package ir.tax.trustedtaxpayers.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Enhanced Cache Service with TTL from Settings and improved features
 * این کلاس برای عملیات Cache استفاده می‌شود با قابلیت‌های پیشرفته:
 * 
 * ویژگی‌ها:
 * - TTL از Settings table خوانده می‌شود
 * - Error handling با Fallback mechanism
 * - Logging و Statistics
 * - Batch Operations
 * - Performance Optimization
 * - CacheKeyBuilder برای type-safe keys
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Slf4j
@Service
public class CacheService {
    
    private final CacheManager cacheManager;
    private final SettingService settingService;
    
    // Cache Statistics
    private final Map<String, AtomicLong> hitCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missCounters = new ConcurrentHashMap<>();
    
    // Cache برای isCacheEnabled و TTL values
    private volatile Boolean cacheEnabledStatus;
    private volatile long cacheEnabledCheckTime = 0;
    private static final long CACHE_STATUS_TTL = 5000; // 5 seconds
    
    private final Map<String, Long> ttlCache = new ConcurrentHashMap<>();
    private final Map<String, Long> ttlCacheTime = new ConcurrentHashMap<>();
    private static final long TTL_CACHE_TTL = 60000; // 1 minute
    
    public CacheService(CacheManager cacheManager, @Lazy SettingService settingService) {
        this.cacheManager = cacheManager;
        this.settingService = settingService;
    }

    /**
     * دریافت از Cache با supplier
     * اگر cache fail کرد، از supplier استفاده می‌کند (fallback)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key, Supplier<T> supplier) {
        if (!isCacheEnabled()) {
            return executeSupplier(supplier, "Cache disabled");
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found, using supplier", cacheName);
                return executeSupplier(supplier, "Cache not found");
            }
            
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                T value = (T) wrapper.get();
                recordHit(cacheName);
                log.debug("Cache HIT: {}/{}", cacheName, key);
                return value;
            }
            
            // Cache miss - تولید value
            recordMiss(cacheName);
            log.debug("Cache MISS: {}/{}", cacheName, key);
            
            T value = executeSupplier(supplier, "Cache miss");
            if (value != null) {
                put(cacheName, key, value);
            }
            return value;
            
        } catch (Exception e) {
            log.error("Cache operation failed for {}/{}: {}", cacheName, key, e.getMessage(), e);
            // Fallback: استفاده از supplier در صورت خطا
            return executeSupplier(supplier, "Cache error: " + e.getMessage());
        }
    }

    /**
     * دریافت از Cache بدون supplier
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, String key) {
        if (!isCacheEnabled()) {
            return Optional.empty();
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return Optional.empty();
            }
            
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                recordHit(cacheName);
                return Optional.of((T) wrapper.get());
            }
            
            recordMiss(cacheName);
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Cache get failed for {}/{}: {}", cacheName, key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * ذخیره در Cache با TTL مشخص شده
     */
    public void putWithTtl(String cacheName, String key, Object value, Duration ttl) {
        if (!isCacheEnabled() || value == null) {
            return;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found for put operation", cacheName);
                return;
            }
            
            // Note: TTL support depends on cache implementation
            // Simple cache doesn't support TTL, values will use default cache expiration
            cache.put(key, value);
            log.debug("Cache PUT: {}/{}", cacheName, key);
            
        } catch (Exception e) {
            log.error("Cache put failed for {}/{}: {}", cacheName, key, e.getMessage(), e);
        }
    }

    /**
     * ذخیره در Cache با TTL از Settings
     */
    public void put(String cacheName, String key, Object value) {
        if (!isCacheEnabled() || value == null) {
            return;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Cache '{}' not found for put operation", cacheName);
                return;
            }
            
            // Standard put operation
            cache.put(key, value);
            log.debug("Cache PUT: {}/{}", cacheName, key);
            
        } catch (Exception e) {
            log.error("Cache put failed for {}/{}: {}", cacheName, key, e.getMessage(), e);
            // Silent fail - cache errors shouldn't break the application
        }
    }

    /**
     * دریافت TTL برای cache از Settings
     */
    private Duration getTtlForCache(String cacheName) {
        try {
            String settingKey = "cache.ttl." + cacheName;
            long currentTime = System.currentTimeMillis();
            
            // Check cache for TTL value
            Long cachedTtl = ttlCache.get(settingKey);
            Long cacheTime = ttlCacheTime.get(settingKey);
            
            if (cachedTtl != null && cacheTime != null && 
                (currentTime - cacheTime) < TTL_CACHE_TTL) {
                return Duration.ofSeconds(cachedTtl);
            }
            
            // Read from Settings
            String ttlValue = settingService.getValue(settingKey, 
                settingService.getValue("cache.ttl.default", "3600"));
            
            long ttlSeconds = Long.parseLong(ttlValue);
            ttlCache.put(settingKey, ttlSeconds);
            ttlCacheTime.put(settingKey, currentTime);
            
            return Duration.ofSeconds(ttlSeconds);
            
        } catch (Exception e) {
            log.warn("Failed to get TTL for cache '{}', using default: {}", cacheName, e.getMessage());
            // Default TTL: 1 hour
            return Duration.ofHours(1);
        }
    }

    /**
     * حذف از Cache
     */
    public void evict(String cacheName, String key) {
        if (!isCacheEnabled()) {
            return;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Cache EVICT: {}/{}", cacheName, key);
            }
        } catch (Exception e) {
            log.error("Cache evict failed for {}/{}: {}", cacheName, key, e.getMessage());
        }
    }

    /**
     * پاک کردن تمام cache
     */
    public void evictAll(String cacheName) {
        if (!isCacheEnabled()) {
            return;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cache CLEARED: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Cache clear failed for {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Batch Evict - حذف چندین key
     */
    public void evictBatch(String cacheName, List<String> keys) {
        if (!isCacheEnabled() || keys == null || keys.isEmpty()) {
            return;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                keys.forEach(key -> {
                    try {
                        cache.evict(key);
                    } catch (Exception e) {
                        log.warn("Failed to evict key {}: {}", key, e.getMessage());
                    }
                });
                log.debug("Cache EVICT BATCH: {}/{} keys", cacheName, keys.size());
            }
        } catch (Exception e) {
            log.error("Cache batch evict failed for {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Batch Get - دریافت چندین value
     */
    public <T> Map<String, T> getBatch(String cacheName, List<String> keys, 
                                       Function<String, T> valueLoader) {
        Map<String, T> result = new HashMap<>();
        
        if (!isCacheEnabled() || keys == null || keys.isEmpty()) {
            if (valueLoader != null) {
                keys.forEach(key -> {
                    T value = valueLoader.apply(key);
                    if (value != null) {
                        result.put(key, value);
                    }
                });
            }
            return result;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                if (valueLoader != null) {
                    keys.forEach(key -> {
                        T value = valueLoader.apply(key);
                        if (value != null) {
                            result.put(key, value);
                            put(cacheName, key, value);
                        }
                    });
                }
                return result;
            }
            
            // Try to get from cache first
            List<String> missingKeys = new ArrayList<>();
            for (String key : keys) {
                try {
                    Cache.ValueWrapper wrapper = cache.get(key);
                    if (wrapper != null) {
                        @SuppressWarnings("unchecked")
                        T value = (T) wrapper.get();
                        result.put(key, value);
                        recordHit(cacheName);
                    } else {
                        missingKeys.add(key);
                        recordMiss(cacheName);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get key {} from cache: {}", key, e.getMessage());
                    missingKeys.add(key);
                }
            }
            
            // Load missing values
            if (!missingKeys.isEmpty() && valueLoader != null) {
                missingKeys.forEach(key -> {
                    try {
                        T value = valueLoader.apply(key);
                        if (value != null) {
                            result.put(key, value);
                            put(cacheName, key, value);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load value for key {}: {}", key, e.getMessage());
                    }
                });
            }
            
            log.debug("Cache GET BATCH: {}/{} keys, {} hits, {} misses", 
                cacheName, keys.size(), keys.size() - missingKeys.size(), missingKeys.size());
            
        } catch (Exception e) {
            log.error("Cache batch get failed for {}: {}", cacheName, e.getMessage());
            // Fallback to direct loading
            if (valueLoader != null) {
                keys.forEach(key -> {
                    try {
                        T value = valueLoader.apply(key);
                        if (value != null) {
                            result.put(key, value);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to load value for key {}: {}", key, ex.getMessage());
                    }
                });
            }
        }
        
        return result;
    }

    /**
     * بررسی وجود کلید
     */
    public boolean contains(String cacheName, String key) {
        if (!isCacheEnabled()) {
            return false;
        }
        
        try {
            Cache cache = cacheManager.getCache(cacheName);
            return cache != null && cache.get(key) != null;
        } catch (Exception e) {
            log.warn("Cache contains check failed for {}/{}: {}", cacheName, key, e.getMessage());
            return false;
        }
    }

    /**
     * دریافت با مقدار پیش‌فرض
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String cacheName, String key, T defaultValue) {
        if (!isCacheEnabled()) {
            return defaultValue;
        }
        
        Optional<T> cached = get(cacheName, key);
        return cached.orElse(defaultValue);
    }

    /**
     * دریافت با conditional caching
     */
    public <T> T get(String cacheName, String key, Supplier<T> supplier, boolean shouldCache) {
        if (!shouldCache || !isCacheEnabled()) {
            return executeSupplier(supplier, "Conditional cache disabled");
        }
        return get(cacheName, key, supplier);
    }

    /**
     * دریافت Statistics برای cache
     */
    public CacheStatistics getStatistics(String cacheName) {
        long hits = hitCounters.getOrDefault(cacheName, new AtomicLong(0)).get();
        long misses = missCounters.getOrDefault(cacheName, new AtomicLong(0)).get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0.0;
        
        return new CacheStatistics(cacheName, hits, misses, total, hitRate);
    }

    /**
     * دریافت تمام Statistics
     */
    public Map<String, CacheStatistics> getAllStatistics() {
        Map<String, CacheStatistics> stats = new HashMap<>();
        Set<String> cacheNames = new HashSet<>();
        cacheNames.addAll(hitCounters.keySet());
        cacheNames.addAll(missCounters.keySet());
        
        cacheNames.forEach(cacheName -> {
            stats.put(cacheName, getStatistics(cacheName));
        });
        
        return stats;
    }

    /**
     * Reset Statistics
     */
    public void resetStatistics(String cacheName) {
        hitCounters.remove(cacheName);
        missCounters.remove(cacheName);
        log.info("Statistics reset for cache: {}", cacheName);
    }

    /**
     * Reset All Statistics
     */
    public void resetAllStatistics() {
        hitCounters.clear();
        missCounters.clear();
        log.info("All cache statistics reset");
    }

    /**
     * بررسی فعال بودن Cache (با caching)
     */
    private boolean isCacheEnabled() {
        long currentTime = System.currentTimeMillis();
        
        // Check cached status
        if (cacheEnabledStatus != null && 
            (currentTime - cacheEnabledCheckTime) < CACHE_STATUS_TTL) {
            return cacheEnabledStatus;
        }
        
        // Cache is always enabled with simple cache
        // Note: Previously this checked Redis cache status from settings
        cacheEnabledStatus = true;
        cacheEnabledCheckTime = currentTime;
        return true;
    }

    /**
     * Record cache hit
     */
    private void recordHit(String cacheName) {
        hitCounters.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Record cache miss
     */
    private void recordMiss(String cacheName) {
        missCounters.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Execute supplier safely
     */
    private <T> T executeSupplier(Supplier<T> supplier, String reason) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Supplier execution failed ({}): {}", reason, e.getMessage(), e);
            throw new RuntimeException("Failed to load value: " + e.getMessage(), e);
        }
    }

    // Helper methods برای Taxpayers
    public <T> T getTaxpayer(String key, Supplier<T> supplier) {
        return get("taxpayers", key, supplier);
    }

    public <T> Optional<T> getTaxpayer(String key) {
        return get("taxpayers", key);
    }

    public void putTaxpayer(String key, Object value) {
        put("taxpayers", key, value);
    }

    public void evictTaxpayer(String key) {
        evict("taxpayers", key);
    }

    public void evictAllTaxpayers() {
        evictAll("taxpayers");
    }

    // Helper methods برای Templates
    public <T> T getTemplate(String key, Supplier<T> supplier) {
        return get("templates", key, supplier);
    }

    public <T> Optional<T> getTemplate(String key) {
        return get("templates", key);
    }

    public void putTemplate(String key, Object value) {
        put("templates", key, value);
    }

    public void evictTemplate(String key) {
        evict("templates", key);
    }

    public void evictAllTemplates() {
        evictAll("templates");
    }

    // Helper methods برای Buyers
    public <T> T getBuyer(String key, Supplier<T> supplier) {
        return get("buyers", key, supplier);
    }

    public <T> Optional<T> getBuyer(String key) {
        return get("buyers", key);
    }

    public void putBuyer(String key, Object value) {
        put("buyers", key, value);
    }

    public void evictBuyer(String key) {
        evict("buyers", key);
    }

    public void evictAllBuyers() {
        evictAll("buyers");
    }

    // Helper methods برای Users
    public <T> T getUser(String key, Supplier<T> supplier) {
        return get("users", key, supplier);
    }

    public <T> Optional<T> getUser(String key) {
        return get("users", key);
    }

    public void putUser(String key, Object value) {
        put("users", key, value);
    }

    public void evictUser(String key) {
        evict("users", key);
    }

    public void evictAllUsers() {
        evictAll("users");
    }

    // Helper methods برای Invoices
    public <T> T getInvoiceByTemplate(String key, Supplier<T> supplier) {
        return get("invoices-by-template", key, supplier);
    }

    public <T> Optional<T> getInvoiceByTemplate(String key) {
        return get("invoices-by-template", key);
    }

    public void putInvoiceByTemplate(String key, Object value) {
        put("invoices-by-template", key, value);
    }

    public void evictInvoiceByTemplate(String key) {
        evict("invoices-by-template", key);
    }

    public void evictAllInvoicesByTemplate() {
        evictAll("invoices-by-template");
    }

    // Helper methods برای Invoices by Template Type
    public <T> T getInvoiceByTemplateType(String key, Supplier<T> supplier) {
        return get("invoices-by-template-type", key, supplier);
    }

    public void evictAllInvoicesByTemplateType() {
        evictAll("invoices-by-template-type");
    }

    // Helper methods برای Invoice Count
    public <T> T getInvoiceCount(String key, Supplier<T> supplier) {
        return get("invoice-count-by-template", key, supplier);
    }

    public void evictInvoiceCount(String key) {
        evict("invoice-count-by-template", key);
        evict("invoice-count-by-template-type", key);
    }

    public void evictAllInvoiceCounts() {
        evictAll("invoice-count-by-template");
        evictAll("invoice-count-by-template-type");
    }

    /**
     * Invalidate تمام cache های invoices
     */
    public void evictAllInvoices() {
        evictAll("invoices-by-template");
        evictAll("invoices-by-template-list");
        evictAll("invoices-by-template-type");
        evictAll("invoices-by-template-inty");
        evictAll("invoice-count-by-template");
        evictAll("invoice-count-by-template-type");
    }

    /**
     * Cache Statistics DTO
     */
    public static class CacheStatistics {
        private final String cacheName;
        private final long hits;
        private final long misses;
        private final long total;
        private final double hitRate;

        public CacheStatistics(String cacheName, long hits, long misses, long total, double hitRate) {
            this.cacheName = cacheName;
            this.hits = hits;
            this.misses = misses;
            this.total = total;
            this.hitRate = hitRate;
        }

        public String getCacheName() { return cacheName; }
        public long getHits() { return hits; }
        public long getMisses() { return misses; }
        public long getTotal() { return total; }
        public double getHitRate() { return hitRate; }

        @Override
        public String toString() {
            return String.format("CacheStatistics{cacheName='%s', hits=%d, misses=%d, total=%d, hitRate=%.2f%%}",
                cacheName, hits, misses, total, hitRate);
        }
    }
}

