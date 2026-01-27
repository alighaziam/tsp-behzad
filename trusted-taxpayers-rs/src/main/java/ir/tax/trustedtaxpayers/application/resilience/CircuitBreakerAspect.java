package ir.tax.trustedtaxpayers.application.resilience;

import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Aspect
@Component
@ConditionalOnBean(RedissonClient.class)
public class CircuitBreakerAspect {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerAspect.class);

    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final double DEFAULT_FAILURE_RATE = 0.5;
    private static final int DEFAULT_WINDOW_SECONDS = 60;
    private static final long DEFAULT_OPEN_MILLIS = 60_000L;
    private static final int DEFAULT_HALF_OPEN_MAX_CALLS = 1;

    private final RedissonClient redissonClient;
    private final SettingService settingService;
    private final HttpServletRequest request;

    public CircuitBreakerAspect(RedissonClient redissonClient, SettingService settingService, HttpServletRequest request) {
        this.redissonClient = redissonClient;
        this.settingService = settingService;
        this.request = request;
    }

    @Around("within(ir.tax.trustedtaxpayers.interfaces.rest..*) && execution(* *(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CircuitBreaker cb = resolveAnnotation(pjp, method);

        String path = request != null && request.getRequestURI() != null ? request.getRequestURI() : methodPath(pjp, method);
        String baseKey = "CB_" + path + "/";

        // service identity and toggles
        String classFqn = pjp.getTarget().getClass().getName();
        upsertIfAbsent("SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.SERVICE);
        upsertIfAbsent("SERVICE_" + classFqn + "/path", path, SettingGroupType.SERVICE);
        upsertIfAbsent("CB_SERVICE_" + classFqn + "/enabled", "true", SettingGroupType.CIRCUIT_BREAKER);

        // global/service/path enabled cascade
        if (!resolveBoolean("cb.enabled", true, SettingGroupType.CIRCUIT_BREAKER)) return pjp.proceed();
        if (!resolveBoolean("CB_SERVICE_" + classFqn + "/enabled", true, SettingGroupType.CIRCUIT_BREAKER)) return pjp.proceed();
        if (!resolveBoolean(baseKey + "enabled", cb.enabled(), SettingGroupType.CIRCUIT_BREAKER)) return pjp.proceed();

        int failureThreshold = resolveInt(baseKey + "failureThreshold", cb.failureThreshold(), DEFAULT_FAILURE_THRESHOLD, SettingGroupType.CIRCUIT_BREAKER);
        double failureRate = resolveDouble(baseKey + "failureRate", cb.failureRate(), DEFAULT_FAILURE_RATE, SettingGroupType.CIRCUIT_BREAKER);
        int windowSeconds = resolveInt(baseKey + "windowSeconds", cb.windowSeconds(), DEFAULT_WINDOW_SECONDS, SettingGroupType.CIRCUIT_BREAKER);
        long openMillis = resolveLong(baseKey + "openMillis", cb.openMillis(), DEFAULT_OPEN_MILLIS, SettingGroupType.CIRCUIT_BREAKER);
        int halfOpenMaxCalls = resolveInt(baseKey + "halfOpenMaxCalls", cb.halfOpenMaxCalls(), DEFAULT_HALF_OPEN_MAX_CALLS, SettingGroupType.CIRCUIT_BREAKER);

        RMap<String, String> state = redissonClient.getMap("cb:state:" + path);
        long now = System.currentTimeMillis();

        String currentState = state.getOrDefault("state", "CLOSED");
        long openUntil = safeParseLong(state.get("openUntil"), 0L);
        int halfOpenCalls = (int) safeParseLong(state.get("halfOpenCalls"), 0L);

        if ("OPEN".equals(currentState)) {
            if (now < openUntil) {
                // Circuit open -> fallback or exception
                return handleOpen(pjp, cb);
            } else {
                // move to HALF_OPEN
                state.put("state", "HALF_OPEN");
                state.put("halfOpenCalls", "0");
                currentState = "HALF_OPEN";
            }
        }

        if ("HALF_OPEN".equals(currentState)) {
            if (halfOpenCalls >= halfOpenMaxCalls) {
                // Too many trial calls, keep it OPEN again
                state.put("state", "OPEN");
                state.put("openUntil", String.valueOf(now + openMillis));
                return handleOpen(pjp, cb);
            }
        }

        String windowId = windowId(windowSeconds);
        RAtomicLong total = redissonClient.getAtomicLong("cb:counter:" + path + ":total:" + windowId);
        RAtomicLong fail = redissonClient.getAtomicLong("cb:counter:" + path + ":fail:" + windowId);
        ensureTtl(total, windowSeconds);
        ensureTtl(fail, windowSeconds);

        boolean success = false;
        try {
            Object result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable t) {
            fail.incrementAndGet();
            throw t;
        } finally {
            long totalVal = total.incrementAndGet();
            long failVal = fail.get();

            if ("HALF_OPEN".equals(currentState)) {
                if (success) {
                    halfOpenCalls++;
                    state.put("halfOpenCalls", String.valueOf(halfOpenCalls));
                    if (halfOpenCalls >= halfOpenMaxCalls) {
                        // close
                        state.put("state", "CLOSED");
                        state.put("halfOpenCalls", "0");
                    }
                } else {
                    // reopen
                    state.put("state", "OPEN");
                    state.put("openUntil", String.valueOf(now + openMillis));
                    state.put("halfOpenCalls", "0");
                }
            } else if ("CLOSED".equals(currentState)) {
                if (failVal >= failureThreshold && failureThreshold > 0) {
                    open(state, now, openMillis);
                } else if (totalVal > 0 && failureRate > 0.0) {
                    double rate = ((double) failVal) / ((double) totalVal);
                    if (rate >= failureRate) {
                        open(state, now, openMillis);
                    }
                }
            }
        }
    }

    private void open(RMap<String, String> state, long now, long openMillis) {
        state.put("state", "OPEN");
        state.put("openUntil", String.valueOf(now + openMillis));
        state.put("halfOpenCalls", "0");
    }

    private Object handleOpen(ProceedingJoinPoint pjp, CircuitBreaker cb) throws Throwable {
        if (cb.fallbackMethod() != null && !cb.fallbackMethod().isEmpty()) {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            Method fallback = pjp.getTarget().getClass().getMethod(cb.fallbackMethod(), sig.getMethod().getParameterTypes());
            return fallback.invoke(pjp.getTarget(), pjp.getArgs());
        }
        throw new CircuitOpenException("Circuit is open for this endpoint");
    }

    private CircuitBreaker resolveAnnotation(ProceedingJoinPoint pjp, Method method) {
        if (method.isAnnotationPresent(CircuitBreaker.class)) {
            return method.getAnnotation(CircuitBreaker.class);
        }
        Class<?> targetClass = pjp.getTarget().getClass();
        if (targetClass.isAnnotationPresent(CircuitBreaker.class)) {
            return targetClass.getAnnotation(CircuitBreaker.class);
        }
        return defaultAnnotation();
    }

    private CircuitBreaker defaultAnnotation() {
        return new CircuitBreaker() {
            @Override public int failureThreshold() { return 0; }
            @Override public double failureRate() { return 0.0; }
            @Override public int windowSeconds() { return 0; }
            @Override public long openMillis() { return 0L; }
            @Override public int halfOpenMaxCalls() { return DEFAULT_HALF_OPEN_MAX_CALLS; }
            @Override public boolean enabled() { return true; }
            @Override public String fallbackMethod() { return ""; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return CircuitBreaker.class; }
        };
    }

    private String methodPath(ProceedingJoinPoint pjp, Method method) {
        return pjp.getTarget().getClass().getSimpleName() + "." + method.getName();
    }

    private String windowId(int windowSeconds) {
        if (windowSeconds <= 0) return "0";
        long epochSec = Instant.now().getEpochSecond();
        long bucket = epochSec / windowSeconds;
        return windowSeconds + "s:" + bucket;
    }

    private void ensureTtl(org.redisson.api.RExpirable expirable, int windowSeconds) {
        if (windowSeconds <= 0) return;
        if (expirable.remainTimeToLive() <= 0) {
            expirable.expire(windowSeconds, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    private void upsertIfAbsent(String key, String value, SettingGroupType groupType) {
        if (settingService.getValue(key).isEmpty()) {
            settingService.saveOrUpdate(key, value, groupType);
        }
    }

    private boolean resolveBoolean(String key, boolean annoDefault, SettingGroupType groupType) {
        Optional<String> v = settingService.getValue(key);
        if (v.isPresent()) return parseBoolean(v.get());
        settingService.saveOrUpdate(key, String.valueOf(annoDefault), groupType);
        return annoDefault;
    }

    private int resolveInt(String key, int anno, int deflt, SettingGroupType groupType) {
        Optional<String> v = settingService.getValue(key);
        if (v.isPresent()) return safeParseInt(v.get(), deflt);
        int val = anno > 0 ? anno : deflt;
        settingService.saveOrUpdate(key, String.valueOf(val), groupType);
        return val;
    }

    private long resolveLong(String key, long anno, long deflt, SettingGroupType groupType) {
        Optional<String> v = settingService.getValue(key);
        if (v.isPresent()) return safeParseLong(v.get(), deflt);
        long val = anno > 0 ? anno : deflt;
        settingService.saveOrUpdate(key, String.valueOf(val), groupType);
        return val;
    }

    private double resolveDouble(String key, double anno, double deflt, SettingGroupType groupType) {
        Optional<String> v = settingService.getValue(key);
        if (v.isPresent()) return safeParseDouble(v.get(), deflt);
        double val = anno > 0 ? anno : deflt;
        settingService.saveOrUpdate(key, String.valueOf(val), groupType);
        return val;
    }

    private boolean parseBoolean(String s) {
        String n = s.trim().toLowerCase(Locale.ROOT);
        return !("false".equals(n) || "0".equals(n) || "off".equals(n) || "disabled".equals(n));
    }
    private int safeParseInt(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private long safeParseLong(String s, long def) { try { return Long.parseLong(s.trim()); } catch (Exception e) { return def; } }
    private double safeParseDouble(String s, double def) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; } }
}


