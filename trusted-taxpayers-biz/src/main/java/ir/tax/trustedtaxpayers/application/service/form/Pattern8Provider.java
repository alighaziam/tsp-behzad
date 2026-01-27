package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 8
 */
@Component
public class Pattern8Provider extends Pattern8to16Provider {
    
    public Pattern8Provider() {
        super(8, "الگوی هشتم");
    }
}

