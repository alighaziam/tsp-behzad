package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 5: قبوض خدماتی
 */
@Component
public class Pattern5UtilityProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 5;
    }
    
    @Override
    public String getPatternName() {
        return "قبوض خدماتی";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // شناسه قبض - اجباری
        fields.add(textField("billid", "شناسه قبض", true, order++, 19));
        
        // شناسه یکتای پرداخت - اختیاری
        fields.add(textField("billingId", "شناسه یکتای پرداخت", false, order++, 50));
        
        return fields;
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        // الگوی 5 فیلد اختصاصی Body ندارد
        return new ArrayList<>();
    }
}

