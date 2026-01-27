package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 9
 */
@Component
public class Pattern9Provider extends Pattern8to16Provider {
    
    public Pattern9Provider() {
        super(9, "الگوی نهم");
    }
}

