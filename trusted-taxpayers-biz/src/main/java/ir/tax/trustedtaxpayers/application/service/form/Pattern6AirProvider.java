package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 6: بلیط هواپیما
 */
@Component
public class Pattern6AirProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 6;
    }
    
    @Override
    public String getPatternName() {
        return "بلیط هواپیما";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // نوع پرواز - اجباری
        fields.add(comboboxField("ft", "نوع پرواز", true, order++, getFlightTypeOptions()));
        
        // شماره اقتصادی آژانس - اختیاری
        fields.add(textField("tinc", "شماره اقتصادی آژانس", false, order++, 14));
        
        return fields;
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        // الگوی 6 فیلد اختصاصی Body ندارد
        return new ArrayList<>();
    }
}

