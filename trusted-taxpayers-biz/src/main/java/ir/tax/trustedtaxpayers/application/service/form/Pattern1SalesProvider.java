package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider برای الگوی 1: فروش کالا و خدمات
 */
@Component
public class Pattern1SalesProvider extends AbstractPatternFieldProvider {
    
    @Override
    public Integer getSupportedPattern() {
        return 1;
    }
    
    @Override
    public String getPatternName() {
        return "فروش کالا و خدمات";
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        // الگوی 1 فیلد اختصاصی Header ندارد
        return new ArrayList<>();
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        // الگوی 1 فیلد اختصاصی Body ندارد - فقط فیلدهای مشترک
        return new ArrayList<>();
    }
}

