package ir.tax.trustedtaxpayers.application.service.form;

import org.springframework.stereotype.Component;

/**
 * Provider برای الگوی 15
 */
@Component
public class Pattern15Provider extends Pattern8to16Provider {
    
    public Pattern15Provider() {
        super(15, "الگوی پانزدهم");
    }
}

