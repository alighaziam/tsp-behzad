package ir.tax.trustedtaxpayers.application.validation;

import ir.tax.trustedtaxpayers.application.exception.BusinessException;
import ir.tax.trustedtaxpayers.application.exception.ErrorCode;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateRuleEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRuleRepository;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * اعتبارسنجی Template برای صورتحساب‌های الکترونیکی
 * بررسی قواعد و فیلدهای اجباری بر اساس Template
 */
@Component
public class TemplateValidator {

    private final TemplateRepository templateRepository;
    private final TemplateRuleRepository templateRuleRepository;

    public TemplateValidator(TemplateRepository templateRepository, TemplateRuleRepository templateRuleRepository) {
        this.templateRepository = templateRepository;
        this.templateRuleRepository = templateRuleRepository;
    }

    /**
     * اعتبارسنجی کامل صورتحساب بر اساس Template
     * 
     * @param headerEntity صورتحساب برای اعتبارسنجی
     * @throws BusinessException در صورت عدم تطابق با قواعد Template
     */
    public void validate(ElectronicInvoiceHeaderEntity headerEntity) {
        if (headerEntity.getTemplateCode() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "کد الگو اجباری است");
        }

        TemplateEntity template = templateRepository.findByCode(headerEntity.getTemplateCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED, 
                        "الگو با کد " + headerEntity.getTemplateCode() + " یافت نشد"));

        if (!template.getIsActive()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                    "الگو با کد " + headerEntity.getTemplateCode() + " فعال نیست");
        }

        // بررسی فیلدهای اجباری
        /*validateRequiredFields(headerEntity, template);*/

        // بررسی قواعد Template
        validateTemplateRules(headerEntity, template.getCode());
    }

    /**
     * بررسی فیلدهای اجباری Template
     */
   /* private void validateRequiredFields(ElectronicInvoiceHeaderEntity headerEntity, TemplateEntity template) {
        String requiredFieldsStr = template.getRequiredFields();
        if (requiredFieldsStr == null || requiredFieldsStr.trim().isEmpty()) {
            return;
        }

        List<String> requiredFields = Arrays.stream(requiredFieldsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> missingFields = new ArrayList<>();

        for (String fieldName : requiredFields) {
            if (!isFieldPresent(headerEntity, fieldName)) {
                missingFields.add(fieldName);
            }
        }

        if (!missingFields.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                    "فیلدهای اجباری زیر برای این الگو وجود ندارند: " + String.join(", ", missingFields));
        }
    }

    /**
     * بررسی قواعد Template
     */
    private void validateTemplateRules(ElectronicInvoiceHeaderEntity headerEntity, String templateCode) {
        List<TemplateRuleEntity> rules = templateRuleRepository.findByTemplateCodeAndActive(templateCode, "Y");
        
        for (TemplateRuleEntity rule : rules) {
            validateRule(headerEntity, rule);
        }
    }

    /**
     * بررسی یک قاعده خاص
     */
    private void validateRule(ElectronicInvoiceHeaderEntity headerEntity, TemplateRuleEntity rule) {
        String fieldName = rule.getFieldName();
        String ruleType = rule.getRuleType();
        String ruleParams = rule.getRuleParams();

        Object fieldValue = getFieldValue(headerEntity, fieldName);

        switch (ruleType.toUpperCase()) {
            case "REQUIRED":
                if (fieldValue == null || (fieldValue instanceof String && ((String) fieldValue).trim().isEmpty())) {
                    throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                            "فیلد " + fieldName + " برای این الگو اجباری است: " + rule.getExplanation());
                }
                break;

            case "MIN":
                if (fieldValue != null && fieldValue instanceof Number) {
                    double minValue = Double.parseDouble(ruleParams);
                    double currentValue = ((Number) fieldValue).doubleValue();
                    if (currentValue < minValue) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "مقدار فیلد " + fieldName + " باید حداقل " + minValue + " باشد: " + rule.getExplanation());
                    }
                }
                break;

            case "MAX":
                if (fieldValue != null && fieldValue instanceof Number) {
                    double maxValue = Double.parseDouble(ruleParams);
                    double currentValue = ((Number) fieldValue).doubleValue();
                    if (currentValue > maxValue) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "مقدار فیلد " + fieldName + " باید حداکثر " + maxValue + " باشد: " + rule.getExplanation());
                    }
                }
                break;

            case "LENGTH_MIN":
                if (fieldValue != null && fieldValue instanceof String) {
                    int minLength = Integer.parseInt(ruleParams);
                    if (((String) fieldValue).length() < minLength) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "طول فیلد " + fieldName + " باید حداقل " + minLength + " کاراکتر باشد: " + rule.getExplanation());
                    }
                }
                break;

            case "LENGTH_MAX":
                if (fieldValue != null && fieldValue instanceof String) {
                    int maxLength = Integer.parseInt(ruleParams);
                    if (((String) fieldValue).length() > maxLength) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "طول فیلد " + fieldName + " باید حداکثر " + maxLength + " کاراکتر باشد: " + rule.getExplanation());
                    }
                }
                break;

            case "ENUM":
                if (fieldValue != null) {
                    String[] allowedValues = ruleParams.split(",");
                    String currentValue = fieldValue.toString();
                    boolean isValid = Arrays.stream(allowedValues)
                            .anyMatch(v -> v.trim().equalsIgnoreCase(currentValue));
                    if (!isValid) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "مقدار فیلد " + fieldName + " باید یکی از مقادیر زیر باشد: " + ruleParams + " - " + rule.getExplanation());
                    }
                }
                break;

            case "PATTERN":
                if (fieldValue != null && fieldValue instanceof String) {
                    if (!((String) fieldValue).matches(ruleParams)) {
                        throw new BusinessException(ErrorCode.VALIDATION_FAILED, 
                                "فرمت فیلد " + fieldName + " معتبر نیست: " + rule.getExplanation());
                    }
                }
                break;

            default:
                // Unknown rule type, skip
                break;
        }
    }

    /**
     * بررسی وجود فیلد در Entity
     */
    private boolean isFieldPresent(ElectronicInvoiceHeaderEntity entity, String fieldName) {
        Object value = getFieldValue(entity, fieldName);
        if (value == null) {
            return false;
        }
        if (value instanceof String) {
            return !((String) value).trim().isEmpty();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).compareTo(BigDecimal.ZERO) != 0;
        }
        return true;
    }

    /**
     * دریافت مقدار فیلد از Entity با استفاده از Reflection
     */
    private Object getFieldValue(ElectronicInvoiceHeaderEntity entity, String fieldName) {
        try {
            Field field = ElectronicInvoiceHeaderEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // اگر فیلد یافت نشد، null برمی‌گرداند
            return null;
        }
    }
}


