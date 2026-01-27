package ir.tax.trustedtaxpayers.application.service.form;

import org.springframework.stereotype.Component;

/**
 * Provider برای الگوی 10
 */
@Component
public class Pattern10Provider extends Pattern8to16Provider {
    
    public Pattern10Provider() {
        super(10, "الگوی دهم");
    }
}

