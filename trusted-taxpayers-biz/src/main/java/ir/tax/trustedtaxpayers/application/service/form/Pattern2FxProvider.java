package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider برای الگوی 2: فروش ارز
 */
@Component
public class Pattern2FxProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 2;
    }
    
    @Override
    public String getPatternName() {
        return "فروش ارز";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // کد گمرکی فروشنده
        fields.add(textField("scc", "کد گمرکی فروشنده", false, order++, 5));
        
        // شماره مجوز گمرکی فروشنده
        fields.add(textField("scln", "شماره مجوز گمرکی فروشنده", false, order++, 14));
        
        return fields;
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // نوع ارز - اجباری (exactly 3 uppercase letters)
        FormFieldDto cut = textField("body.cut", "نوع ارز", true, order++, 3);
        cut.setMinLength(3);
        cut.setPattern("^[A-Z]{3}$");
        fields.add(cut);
        
        // فی واحد ارزی - اجباری
        fields.add(numberField("body.cfee", "فی واحد ارزی", true, order++));
        
        // نرخ تبدیل - اجباری
        fields.add(numberField("body.exr", "نرخ تبدیل", true, order++));
        
        // نرخ خرید ارز - اختیاری
        fields.add(numberField("body.cpr", "نرخ خرید ارز", false, order++));
        
        // ماخذ مالیات بر ارزش افزوده - اختیاری
        fields.add(numberField("body.sovat", "ماخذ مالیات بر ارزش افزوده", false, order++));
        
        // ماخذ مالیات - اختیاری
        fields.add(numberField("body.vba", "ماخذ مالیات", false, order++));
        
        return fields;
    }
}

