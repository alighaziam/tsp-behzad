package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * سرویس برای دریافت مشخصات فیلدهای فرم از Entity classes
 * این سرویس می‌تواند توسط سایر سرویس‌ها برای دریافت schema فرم استفاده شود
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormSchemaService {

    private final FormBuilderService formBuilderService;

    /**
     * دریافت Form Schema از نام کلاس Entity
     * 
     * @param entityClassName نام کامل کلاس Entity (مثلاً: ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity)
     * @return Optional از FormSchemaDto
     */
    public Optional<FormSchemaDto> getFormSchemaByClassName(String entityClassName) {
        try {
            Class<?> entityClass = Class.forName(entityClassName);
            FormSchemaDto schema = formBuilderService.generateFormSchema(entityClass);
            return Optional.of(schema);
        } catch (ClassNotFoundException e) {
            log.warn("کلاس Entity یافت نشد: {}", entityClassName);
            return Optional.empty();
        } catch (Exception e) {
            log.error("خطا در تولید Form Schema برای {}: {}", entityClassName, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * دریافت Form Schema از نام ساده کلاس Entity
     * این متد سعی می‌کند کلاس را در package های رایج پیدا کند
     * 
     * @param simpleClassName نام ساده کلاس Entity (مثلاً: TemplateEntity)
     * @return Optional از FormSchemaDto
     */
    public Optional<FormSchemaDto> getFormSchemaBySimpleName(String simpleClassName) {
        // لیست package های رایج برای Entity ها
        String[] commonPackages = {
            "ir.tax.trustedtaxpayers.infrastructure.jpa.entity",
            "ir.tax.trustedtaxpayers.infrastructure.jpa.entity.base",
            "ir.tax.trustedtaxpayers.model.entity",
            "ir.tax.trustedtaxpayers.domain.entity"
        };

        for (String packageName : commonPackages) {
            String fullClassName = packageName + "." + simpleClassName;
            Optional<FormSchemaDto> schema = getFormSchemaByClassName(fullClassName);
            if (schema.isPresent()) {
                return schema;
            }
        }

        log.warn("کلاس Entity {} در هیچ یک از package های رایج یافت نشد", simpleClassName);
        return Optional.empty();
    }

    /**
     * دریافت Form Schema از کلاس Entity
     * 
     * @param entityClass کلاس Entity
     * @return FormSchemaDto
     */
    public FormSchemaDto getFormSchema(Class<?> entityClass) {
        return formBuilderService.generateFormSchema(entityClass);
    }

    /**
     * بررسی وجود کلاس Entity
     * 
     * @param entityClassName نام کامل کلاس Entity
     * @return true اگر کلاس وجود داشته باشد
     */
    public boolean entityClassExists(String entityClassName) {
        try {
            Class.forName(entityClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * دریافت لیست تمام فیلدهای یک Entity
     * 
     * @param entityClassName نام کامل کلاس Entity
     * @return Optional از FormSchemaDto (فقط شامل فیلدها)
     */
    public Optional<FormSchemaDto> getFieldsByClassName(String entityClassName) {
        return getFormSchemaByClassName(entityClassName);
    }
}

