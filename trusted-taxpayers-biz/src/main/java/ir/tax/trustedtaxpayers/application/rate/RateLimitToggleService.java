package ir.tax.trustedtaxpayers.application.rate;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.SettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class RateLimitToggleService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitToggleService.class);

    // Global keys
    public static final String ENABLED_KEY = "rate.limit.enabled";
    public static final String DEFAULT_CAPACITY_KEY = "rate.limit.default.capacity";
    public static final String DEFAULT_REFILL_TOKENS_KEY = "rate.limit.default.refill.tokens";
    public static final String DEFAULT_PERIOD_SECONDS_KEY = "rate.limit.default.period.seconds";
    public static final String DEFAULT_KEY_STRATEGY_KEY = "rate.limit.default.key.strategy"; // IP|USER
    public static final String GLOBAL_WHITELIST_KEY = "rate.limit.whitelist"; // comma list (IPs or users)
    public static final String GLOBAL_BLACKLIST_KEY = "rate.limit.blacklist"; // comma list

    // Per-endpoint prefix: rate.limit.endpoint.{name}.*
    public static final String ENDPOINT_PREFIX = "rate.limit.endpoint.";

    private final SettingRepository settingRepository;

    // Simple local cache with TTL
    private final ConcurrentHashMap<String, Object> localCache = new ConcurrentHashMap<>();
    private final AtomicLong lastCacheUpdateTime = new AtomicLong(0);

    @Value("${application.rate.toggle-cache-ttl-seconds:30}")
    private long toggleCacheTtlSeconds;

    public RateLimitToggleService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public boolean isEnabled() {
        refreshCacheIfExpired();
        return getBoolean(ENABLED_KEY, true);
    }

    public RateLimitConfig getDefaultConfig() {
        refreshCacheIfExpired();
        long capacity = getLong(DEFAULT_CAPACITY_KEY, 100L);
        long refill = getLong(DEFAULT_REFILL_TOKENS_KEY, capacity);
        long seconds = getLong(DEFAULT_PERIOD_SECONDS_KEY, 60L);
        RateLimited.KeyStrategy strategy = parseKeyStrategy(getString(DEFAULT_KEY_STRATEGY_KEY, "IP"));
        Set<String> whitelist = parseCsv(getString(GLOBAL_WHITELIST_KEY, ""));
        Set<String> blacklist = parseCsv(getString(GLOBAL_BLACKLIST_KEY, ""));
        return RateLimitConfig.builder()
                .enabled(isEnabled())
                .capacity(capacity)
                .refillTokens(refill)
                .period(Duration.ofSeconds(seconds))
                .keyStrategy(strategy)
                .whitelist(whitelist)
                .blacklist(blacklist)
                .build();
    }

    public RateLimitConfig getEndpointConfig(String name) {
        if (name == null || name.isEmpty()) {
            return getDefaultConfig();
        }
        refreshCacheIfExpired();
        String base = ENDPOINT_PREFIX + name + ".";
        long capacity = getLong(base + "capacity", 0L);
        long refill = getLong(base + "refill.tokens", 0L);
        long seconds = getLong(base + "period.seconds", 0L);
        String strategyStr = getString(base + "key.strategy", "");
        Set<String> whitelist = parseCsv(getString(base + "whitelist", ""));
        Set<String> blacklist = parseCsv(getString(base + "blacklist", ""));

        RateLimitConfig defaults = getDefaultConfig();
        return RateLimitConfig.builder()
                .enabled(defaults.isEnabled())
                .capacity(capacity > 0 ? capacity : defaults.getCapacity())
                .refillTokens(refill > 0 ? refill : defaults.getRefillTokens())
                .period(seconds > 0 ? Duration.ofSeconds(seconds) : defaults.getPeriod())
                .keyStrategy(strategyStr.isEmpty() ? defaults.getKeyStrategy() : parseKeyStrategy(strategyStr))
                .whitelist(whitelist.isEmpty() ? defaults.getWhitelist() : whitelist)
                .blacklist(blacklist.isEmpty() ? defaults.getBlacklist() : blacklist)
                .build();
    }

    public void invalidateCache() {
        localCache.clear();
        lastCacheUpdateTime.set(0);
    }

    private void refreshCacheIfExpired() {
        long now = System.currentTimeMillis();
        if ((now - lastCacheUpdateTime.get()) > (toggleCacheTtlSeconds * 1000)) {
            localCache.clear();
            lastCacheUpdateTime.set(now);
        }
    }

    private boolean getBoolean(String key, boolean def) {
        String v = getString(key, null);
        if (v == null) return def;
        String n = v.trim().toLowerCase(Locale.ROOT);
        return !("false".equals(n) || "0".equals(n) || "off".equals(n) || "disabled".equals(n));
    }

    private long getLong(String key, long def) {
        String v = getString(key, null);
        if (v == null) return def;
        try { return Long.parseLong(v.trim()); } catch (Exception e) { return def; }
    }

    private String getString(String key, String def) {
        Object cached = localCache.get(key);
        if (cached instanceof String) {
            return (String) cached;
        }
        Optional<SettingEntity> fromDb = settingRepository.findByKey(key);
        String value = fromDb.map(SettingEntity::getValue).orElse(def);
        if (value != null) {
            localCache.put(key, value);
        }
        return value;
    }

    private Set<String> parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private RateLimited.KeyStrategy parseKeyStrategy(String s) {
        if (s == null) return RateLimited.KeyStrategy.IP;
        try {
            return RateLimited.KeyStrategy.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            logger.warn("Unknown key strategy '{}', defaulting to IP", s);
            return RateLimited.KeyStrategy.IP;
        }
    }
}


