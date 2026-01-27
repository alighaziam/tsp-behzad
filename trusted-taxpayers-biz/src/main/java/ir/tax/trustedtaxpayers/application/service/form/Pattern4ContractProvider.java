package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 4: قرارداد پیمانکاری
 */
@Component
public class Pattern4ContractProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 4;
    }
    
    @Override
    public String getPatternName() {
        return "قرارداد پیمانکاری";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // شماره ثبت قرارداد - اجباری
        fields.add(textField("crn", "شماره ثبت قرارداد", true, order++, 12));
        
        // شناسه قبض - اختیاری
        fields.add(textField("billid", "شناسه قبض", false, order++, 19));
        
        return fields;
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        // الگوی 4 فیلد اختصاصی Body ندارد
        return new ArrayList<>();
    }
}

