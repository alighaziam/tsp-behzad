package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider برای الگوی 7: صادرات
 */
@Component
public class Pattern7ExportProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 7;
    }
    
    @Override
    public String getPatternName() {
        return "صادرات";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // شماره کوتاژ اظهارنامه گمرکی - اختیاری
        fields.add(textField("cdcn", "شماره کوتاژ اظهارنامه گمرکی", false, order++, 14));
        
        // تاریخ کوتاژ اظهارنامه گمرکی - اختیاری
        fields.add(datetimeField("cdcd", "تاریخ کوتاژ اظهارنامه گمرکی", false, order++));
        
        // مجموع وزن خالص - اختیاری
        fields.add(numberField("tonw", "مجموع وزن خالص", false, order++));
        
        // مجموع ارزش ریالی - اختیاری
        fields.add(numberField("torv", "مجموع ارزش ریالی", false, order++));
        
        // مجموع ارزش ارزی - اختیاری
        fields.add(numberField("tocv", "مجموع ارزش ارزی", false, order++));
        
        // شماره بارنامه - اختیاری
        fields.add(textField("lno", "شماره بارنامه", false, order++, 50));
        
        // شماره بارنامه مرجع - اختیاری
        fields.add(textField("lrno", "شماره بارنامه مرجع", false, order++, 50));
        
        // کشور و شهر مبدا - اختیاری
        fields.add(textField("ocu", "کشور مبدا", false, order++, 3));
        fields.add(textField("oci", "شهر مبدا", false, order++, 100));
        
        // کشور و شهر مقصد - اختیاری
        fields.add(textField("dco", "کشور مقصد", false, order++, 3));
        fields.add(textField("dci", "شهر مقصد", false, order++, 100));
        
        // شناسه‌های فرستنده و گیرنده - اختیاری
        fields.add(textField("tid", "شناسه فرستنده", false, order++, 12));
        fields.add(textField("rid", "شناسه گیرنده", false, order++, 12));
        
        // نوع بارنامه/حمل - اختیاری
        fields.add(comboboxField("lt", "نوع حمل", false, order++, getShippingTypeOptions()));
        
        // شماره ناوگان و راننده - اختیاری
        fields.add(textField("cno", "شماره ناوگان", false, order++, 50));
        fields.add(textField("did", "شناسه راننده", false, order++, 12));
        
        return fields;
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // کد HS قلم کالا - اختیاری
        fields.add(textField("body.hs", "کد HS قلم کالا", false, order++, 20));
        
        // وزن خالص - اختیاری
        fields.add(numberField("body.nw", "وزن خالص", false, order++));
        
        // ارزش ریالی کالا - اختیاری
        fields.add(numberField("body.ssrv", "ارزش ریالی کالا", false, order++));
        
        // ارزش ارزی کالا - اختیاری
        fields.add(numberField("body.sscv", "ارزش ارزی کالا", false, order++));
        
        return fields;
    }

    private List<Map<String, Object>> getShippingTypeOptions() {
        return List.of(
                Map.of("value", 1, "label", "حمل و نقل جاده‌ای"),
                Map.of("value", 2, "label", "حمل و نقل ریلی"),
                Map.of("value", 3, "label", "حمل و نقل هوایی"),
                Map.of("value", 4, "label", "حمل و نقل دریایی")
        );
    }
}

