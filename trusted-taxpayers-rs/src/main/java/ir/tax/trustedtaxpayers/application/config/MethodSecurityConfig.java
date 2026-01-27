package ir.tax.trustedtaxpayers.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * پیکربندی Method Security برای @PreAuthorize
 * Method Security Configuration for @PreAuthorize
 * 
 * مشابه samt-git/contract-rs/config/MethodSecurityConfig
 */
@Configuration
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
public class MethodSecurityConfig {

    /**
     * Method Security Expression Handler
     * بدون prefix برای role ها (مثل samt)
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = 
            new DefaultMethodSecurityExpressionHandler();
        
        // حذف prefix پیش‌فرض "ROLE_" - مشابه samt
        expressionHandler.setDefaultRolePrefix("");
        
        return expressionHandler;
    }
}


