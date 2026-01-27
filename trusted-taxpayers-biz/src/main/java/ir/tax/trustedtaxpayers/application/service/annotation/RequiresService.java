package ir.tax.trustedtaxpayers.application.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for requiring a specific service access for a method or class.
 * If the taxpayer doesn't have access to the required service, a ServiceAccessException will be thrown.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresService {
    
    /**
     * Service code that is required (e.g., "INVOICE_SEND", "API_ACCESS")
     */
    String value();
    
    /**
     * Custom error message when service access is denied
     */
    String message() default "دسترسی به این سرویس برای شما فعال نیست";
    
    /**
     * Whether to check service limits (e.g., number of invoices)
     */
    boolean checkLimit() default false;
}

