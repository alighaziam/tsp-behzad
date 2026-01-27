package ir.tax.trustedtaxpayers.application.rate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for applying rate limiting on controller/service methods.
 * If attributes are not provided, defaults are resolved from settings.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface RateLimited {

    /**
     * Optional logical name to group and fetch per-endpoint settings, e.g. "buyers.search".
     * If empty, the aspect may use controller method signature to derive a name.
     */
    String name() default "";

    // ===== Spec-style parameters (preferred) =====
    int perSecond() default 0;
    int perMinute() default 0;
    int perHour() default 0;
    boolean byIp() default true; // true => key by IP, false => key by USER
    String[] whitelist() default {};
    String[] blacklist() default {};

    /**
     * Maximum tokens in the bucket (capacity). If 0, read from settings.
     */
    long capacity() default 0L;

    /**
     * Tokens refilled per period. If 0, read from settings (usually equals capacity for fixed window semantics).
     */
    long refillTokens() default 0L;

    /**
     * Refill period length (unit defined by periodUnit). If 0, read from settings.
     */
    long period() default 0L;

    /**
     * Unit for period: SECONDS, MINUTES, HOURS. If EMPTY, read from settings.
     */
    TimeUnit periodUnit() default TimeUnit.EMPTY;

    /**
     * Keying strategy.
     */
    KeyStrategy keyStrategy() default KeyStrategy.IP;

    enum KeyStrategy {
        IP, USER, CUSTOM
    }

    enum TimeUnit {
        SECONDS, MINUTES, HOURS, EMPTY
    }
}


