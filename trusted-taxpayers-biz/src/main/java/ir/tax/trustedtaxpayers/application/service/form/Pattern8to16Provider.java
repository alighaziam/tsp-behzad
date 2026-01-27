package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider پایه برای الگوهای 8 تا 16: الگوهای عمومی
 * این کلاس abstract است و باید توسط کلاس‌های فرزند extend شود
 */
public abstract class Pattern8to16Provider extends AbstractPatternFieldProvider {
    
    protected final Integer patternNumber;
    protected final String patternName;
    
    // Constructor برای الگوهای 8-16
    protected Pattern8to16Provider(Integer patternNumber, String patternName) {
        this.patternNumber = patternNumber;
        this.patternName = patternName;
    }
    
    @Override
    public Integer getSupportedPattern() {
        return patternNumber;
    }
    
    @Override
    public String getPatternName() {
        return patternName;
    }
    
    @Override
    public List<FormFieldDto> getHeaderSpecificFields(int baseOrder) {
        // الگوهای 8-16 در حال حاضر فیلد اختصاصی Header ندارند
        // اما می‌توانند در آینده توسعه یابند
        return new ArrayList<>();
    }
    
    @Override
    public List<FormFieldDto> getBodySpecificFields(int baseOrder) {
        // الگوهای 8-16 در حال حاضر فیلد اختصاصی Body ندارند
        // اما می‌توانند در آینده توسعه یابند
        return new ArrayList<>();
    }
}

