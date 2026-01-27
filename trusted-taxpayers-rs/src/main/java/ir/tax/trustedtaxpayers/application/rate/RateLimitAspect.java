package ir.tax.trustedtaxpayers.application.rate;

import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@ConditionalOnProperty(name = "rate.limit.enabled", havingValue = "true")
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RedissonClient redissonClient;
    private final RateLimitToggleService toggleService;
    private final SettingService settingService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public RateLimitAspect(RedissonClient redissonClient,
                           RateLimitToggleService toggleService,
                           SettingService settingService,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        this.redissonClient = redissonClient;
        this.toggleService = toggleService;
        this.settingService = settingService;
        this.request = request;
        this.response = response;
    }

    @Around("within(ir.tax.trustedtaxpayers.interfaces.rest..*) && (execution(* *(..)))")
    public Object aroundControllers(ProceedingJoinPoint pjp) throws Throwable {
        if (!toggleService.isEnabled()) {
            return pjp.proceed();
        }

        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RateLimited annotation = resolveAnnotation(pjp, method);
        String path = request.getRequestURI();
        String classFqn = pjp.getTarget().getClass().getName();
        // Auto-upsert service identity and toggles
        upsertIfAbsent("SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.SERVICE);
        upsertIfAbsent("SERVICE_" + classFqn + "/path", path, SettingGroupType.SERVICE);
        upsertIfAbsent("RATE_LIMIT_SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.RATE_LIMIT);

        // Respect global and service/path toggles
        if (!isEnabledFlag("rate.limit.enabled", true, SettingGroupType.RATE_LIMIT)) {
            return pjp.proceed();
        }
        if (!isEnabledFlag("RATE_LIMIT_SERVICE_" + classFqn + "/enabled", true, SettingGroupType.RATE_LIMIT)) {
            return pjp.proceed();
        }
        if (!isEnabledFlag("RATE_LIMIT_" + path + "/enabled", true, SettingGroupType.RATE_LIMIT)) {
            return pjp.proceed();
        }

        // resolve limits from DB (auto-upsert from annotation if missing)
        int perSecond = resolveLimitFromDbOrAnnotation(buildKey(path, "perSecond"), annotation.perSecond(), SettingGroupType.RATE_LIMIT);
        int perMinute = resolveLimitFromDbOrAnnotation(buildKey(path, "perMinute"), annotation.perMinute(), SettingGroupType.RATE_LIMIT);
        int perHour = resolveLimitFromDbOrAnnotation(buildKey(path, "perHour"), annotation.perHour(), SettingGroupType.RATE_LIMIT);

        boolean byIp = resolveBooleanFromDbOrAnnotation(buildKey(path, "byIp"), annotation.byIp(), SettingGroupType.RATE_LIMIT);
        Set<String> whitelist = resolveSetFromDbOrAnnotation(buildKey(path, "whitelist"), annotation.whitelist(), SettingGroupType.RATE_LIMIT);
        Set<String> blacklist = resolveSetFromDbOrAnnotation(buildKey(path, "blacklist"), annotation.blacklist(), SettingGroupType.RATE_LIMIT);

        String principalKey = resolvePrincipalKey(byIp);
        if (whitelist.contains(principalKey)) {
            return pjp.proceed();
        }
        if (blacklist.contains(principalKey)) {
            throw new RateLimitExceededException("دسترسی شما مسدود است", 60);
        }

        WindowResult sec = enforceWindow(path, principalKey, perSecond, 1, TimeUnit.SECONDS);
        WindowResult min = enforceWindow(path, principalKey, perMinute, 60, TimeUnit.SECONDS);
        WindowResult hour = enforceWindow(path, principalKey, perHour, 3600, TimeUnit.SECONDS);

        WindowResult hdr = sec.active ? sec : (min.active ? min : (hour.active ? hour : null));
        if (hdr != null) {
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(hdr.limit));
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(Math.max(hdr.limit - hdr.value, 0)));
            response.setHeader("X-Rate-Limit-Reset", String.valueOf(Math.max(hdr.ttlSeconds, 1)));
        }

        return pjp.proceed();
    }

    private RateLimited resolveAnnotation(ProceedingJoinPoint pjp, Method method) {
        if (method.isAnnotationPresent(RateLimited.class)) {
            return method.getAnnotation(RateLimited.class);
        }
        Class<?> targetClass = pjp.getTarget().getClass();
        if (targetClass.isAnnotationPresent(RateLimited.class)) {
            return targetClass.getAnnotation(RateLimited.class);
        }
        // If not present, treat as default-limited endpoint by method name
        return defaultAnnotationInstance();
    }

    private RateLimited defaultAnnotationInstance() {
        return new RateLimited() {
            @Override public String name() { return ""; }
            @Override public int perSecond() { return 0; }
            @Override public int perMinute() { return 0; }
            @Override public int perHour() { return 0; }
            @Override public boolean byIp() { return true; }
            @Override public String[] whitelist() { return new String[0]; }
            @Override public String[] blacklist() { return new String[0]; }
            @Override public long capacity() { return 0; }
            @Override public long refillTokens() { return 0; }
            @Override public long period() { return 0; }
            @Override public TimeUnit periodUnit() { return TimeUnit.EMPTY; }
            @Override public KeyStrategy keyStrategy() { return KeyStrategy.IP; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimited.class; }
        };
    }

    private String resolvePrincipalKey(boolean byIp) {
        if (!byIp) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
                return auth.getName();
            }
            return "anonymous";
        }
        // Default IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // In case of multiple IPs, take the first
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        return request.getRemoteAddr();
    }

    private String buildKey(String path, String suffix) {
        return "RATE_LIMIT_" + path + "/" + suffix;
    }

    private boolean isEnabledFlag(String key, boolean defVal, SettingGroupType groupType) {
        return settingService.getValue(key).map(v -> {
            String n = v.trim().toLowerCase();
            return !("false".equals(n) || "0".equals(n) || "off".equals(n) || "disabled".equals(n));
        }).orElseGet(() -> {
            settingService.saveOrUpdate(key, String.valueOf(defVal), groupType);
            toggleService.invalidateCache();
            return defVal;
        });
    }

    private void upsertIfAbsent(String key, String value, SettingGroupType groupType) {
        if (settingService.getValue(key).isEmpty()) {
            settingService.saveOrUpdate(key, value, groupType);
            toggleService.invalidateCache();
        }
    }

    private int resolveLimitFromDbOrAnnotation(String key, int annoValue, SettingGroupType groupType) {
        return settingService.getValue(key).map(v -> {
            try { return Integer.parseInt(v.trim()); } catch (Exception e) { return 0; }
        }).orElseGet(() -> {
            if (annoValue > 0) {
                settingService.saveOrUpdate(key, String.valueOf(annoValue), groupType);
                toggleService.invalidateCache();
            }
            return Math.max(annoValue, 0);
        });
    }

    private boolean resolveBooleanFromDbOrAnnotation(String key, boolean annoValue, SettingGroupType groupType) {
        return settingService.getValue(key).map(v -> {
            String n = v.trim().toLowerCase();
            return !("false".equals(n) || "0".equals(n) || "off".equals(n) || "disabled".equals(n));
        }).orElseGet(() -> {
            settingService.saveOrUpdate(key, String.valueOf(annoValue), groupType);
            toggleService.invalidateCache();
            return annoValue;
        });
    }

    private Set<String> resolveSetFromDbOrAnnotation(String key, String[] annoValues, SettingGroupType groupType) {
        return settingService.getValue(key).map(this::parseCsvSet).orElseGet(() -> {
            if (annoValues != null && annoValues.length > 0) {
                String joined = String.join(",", annoValues);
                settingService.saveOrUpdate(key, joined, groupType);
                toggleService.invalidateCache();
                return new HashSet<>(Arrays.asList(annoValues));
            }
            return java.util.Collections.emptySet();
        });
    }

    private Set<String> parseCsvSet(String csv) {
        if (csv == null || csv.trim().isEmpty()) return java.util.Collections.emptySet();
        String[] parts = csv.split(",");
        Set<String> set = new HashSet<>();
        for (String p : parts) {
            String s = p.trim();
            if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    private WindowResult enforceWindow(String path, String principal, int limit, int windowSeconds, TimeUnit unit) {
        if (limit <= 0) return WindowResult.inactive();
        String key = buildCounterKey(path, principal, windowSeconds);
        RAtomicLong counter = redissonClient.getAtomicLong(key);
        long ttlMs = counter.remainTimeToLive();
        if (ttlMs <= 0) {
            counter.set(0);
            counter.expire(windowSeconds, unit);
        }
        long val = counter.incrementAndGet();
        long remainTtlSec = Math.max(counter.remainTimeToLive() / 1000, 1);
        if (val > limit) {
            throw new RateLimitExceededException("سقف فراخوانی مجاز رد شد", remainTtlSec);
        }
        return WindowResult.active(limit, val, remainTtlSec);
    }

    private String buildCounterKey(String path, String principal, int windowSeconds) {
        return "RL:" + path + ":" + principal + ":" + windowSeconds + "s";
    }

    private static class WindowResult {
        final boolean active;
        final int limit;
        final long value;
        final long ttlSeconds;
        private WindowResult(boolean active, int limit, long value, long ttlSeconds) {
            this.active = active; this.limit = limit; this.value = value; this.ttlSeconds = ttlSeconds;
        }
        static WindowResult inactive() { return new WindowResult(false, 0, 0, 0); }
        static WindowResult active(int limit, long value, long ttl) { return new WindowResult(true, limit, value, ttl); }
    }
}


