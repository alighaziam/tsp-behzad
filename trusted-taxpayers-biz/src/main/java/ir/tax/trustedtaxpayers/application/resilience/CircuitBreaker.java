package ir.tax.trustedtaxpayers.application.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CircuitBreaker {
    int failureThreshold() default 0;
    double failureRate() default 0.0;
    int windowSeconds() default 0;
    long openMillis() default 0L;
    int halfOpenMaxCalls() default 1;
    boolean enabled() default true;
    String fallbackMethod() default "";
}


