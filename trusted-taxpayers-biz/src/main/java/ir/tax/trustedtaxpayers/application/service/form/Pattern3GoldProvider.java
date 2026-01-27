package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 3: طلا، جواهر و پلاتین
 */
@Component
public class Pattern3GoldProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 3;
    }
    
    @Override
    public String getPatternName() {
        return "طلا، جواهر و پلاتین";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        // الگوی 3 فیلد اختصاصی Header ندارد
        return new ArrayList<>();
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = baseOrder;
        
        // عیار طلا - اجباری
        fields.add(numberField("body.cui", "عیار طلا", true, order++));
        
        // وزن خالص - اجباری
        fields.add(numberField("body.nw", "وزن خالص", true, order++));
        
        // ارزش ریالی کالا - اجباری
        fields.add(numberField("body.ssrv", "ارزش ریالی کالا", true, order++));

        // اجرت ساخت - اختیاری
        fields.add(numberField("body.consfee", "اجرت ساخت", false, order++));

        // حق‌العمل - اختیاری
        fields.add(numberField("body.bros", "حق‌العمل", false, order++));

        // سود فروشنده - اختیاری
        fields.add(numberField("body.spro", "سود فروشنده", false, order++));

        // مجموع اجرت، سود و حق‌العمل - اختیاری
        fields.add(numberField("body.tcpbs", "مجموع اجرت/سود/حق‌العمل", false, order++));
        
        return fields;
    }
}

