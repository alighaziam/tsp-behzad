package ir.tax.trustedtaxpayers.application.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * پیاده‌سازی AuditorAware برای JPA Auditing
 * 
 * <p>این کلاس کاربر فعلی را از SecurityContext استخراج می‌کند
 * تا در فیلدهای @CreatedBy و @LastModifiedBy استفاده شود.</p>
 */
public class AuditorAwareImpl implements AuditorAware<String> {
    
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("system");
        }
        
        String username = authentication.getName();
        return Optional.of(username);
    }
}


