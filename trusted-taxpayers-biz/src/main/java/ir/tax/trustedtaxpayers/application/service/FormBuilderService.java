package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * سرویس برای تولید JSON Schema برای Angular Form Builder از Entity classes
 */
@Slf4j
@Service
public class FormBuilderService {

    /**
     * تولید Form Schema از یک کلاس Entity (با Caching)
     */
    @Cacheable(cacheName = "formSchemas", keyPrefix = "entityClass")
    public FormSchemaDto generateFormSchema(Class<?> entityClass) {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = 0;

        // دریافت تمام فیلدهای کلاس و کلاس‌های والد
        Set<Field> allFields = getAllFields(entityClass);

        for (Field field : allFields) {
            // رد کردن فیلدهای audit و id در حالت ایجاد
            if (shouldSkipField(field)) {
                continue;
            }

            FormFieldDto fieldDto = createFormField(field, order++);
            if (fieldDto != null) {
                fields.add(fieldDto);
            }
        }

        // مرتب‌سازی بر اساس order
        fields.sort(Comparator.comparingInt(f -> f.getOrder() != null ? f.getOrder() : Integer.MAX_VALUE));

        return FormSchemaDto.builder()
                .entityName(entityClass.getSimpleName())
                .fields(fields)
                .title("فرم " + entityClass.getSimpleName())
                .description("فرم ایجاد و ویرایش " + entityClass.getSimpleName())
                .build();
    }

    /**
     * دریافت تمام فیلدهای کلاس و کلاس‌های والد
     */
    private Set<Field> getAllFields(Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * بررسی اینکه آیا باید فیلد را رد کنیم
     */
    private boolean shouldSkipField(Field field) {
        String fieldName = field.getName();
        
        // رد کردن فیلد id (در حالت ایجاد)
        if (fieldName.equals("id") && field.isAnnotationPresent(Id.class)) {
            return true;
        }
        
        // رد کردن فیلدهای audit
        if (fieldName.equals("createdOn") || fieldName.equals("updatedOn") ||
            fieldName.equals("createdBy") || fieldName.equals("updatedBy")) {
            return true;
        }

        // رد کردن روابط JPA (ManyToOne, OneToMany, etc.)
        if (field.isAnnotationPresent(ManyToOne.class) || 
            field.isAnnotationPresent(OneToMany.class) ||
            field.isAnnotationPresent(OneToOne.class) ||
            field.isAnnotationPresent(ManyToMany.class)) {
            return true;
        }

        // رد کردن فیلدهای @Transient
        if (field.isAnnotationPresent(Transient.class)) {
            return true;
        }

        return false;
    }

    /**
     * ایجاد FormFieldDto از یک Field
     */
    private FormFieldDto createFormField(Field field, int order) {
        try {
            field.setAccessible(true);
            
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            String fieldLabel = getFieldLabel(field);
            boolean isRequiredField = isRequired(field);
            
            FormFieldDto.FormFieldDtoBuilder builder = createBaseBuilder(fieldName, fieldLabel, order, isRequiredField);
            
            applyValidationAnnotations(field, builder);
            applyTypeAndReadOnly(field, fieldType, builder);
            applyDefaultValues(field, fieldType, builder);
            applyEnumOptions(field, fieldType, builder);

            return builder.build();
        } catch (Exception e) {
            log.warn("خطا در پردازش فیلد {}: {}", field.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * ایجاد builder پایه برای FormFieldDto
     */
    private FormFieldDto.FormFieldDtoBuilder createBaseBuilder(String fieldName, String fieldLabel, int order, boolean isRequired) {
        return FormFieldDto.builder()
                .key(fieldName)
                .name(fieldLabel)
                .label(fieldLabel)
                .order(order)
                .required(isRequired)
                .mandatory(isRequired)
                .disabled(false)
                .readonly(false)
                .placeholder("لطفا " + fieldLabel + " را وارد کنید");
    }

    /**
     * اعمال validation annotations به builder
     */
    private void applyValidationAnnotations(Field field, FormFieldDto.FormFieldDtoBuilder builder) {
        Integer minLength = ValidationAnnotationExtractor.extractMinLength(field);
        if (minLength != null) {
            builder.minLength(minLength);
        }

        Integer maxLength = ValidationAnnotationExtractor.extractMaxLength(field);
        if (maxLength != null) {
            builder.maxLength(maxLength);
        }

        String pattern = ValidationAnnotationExtractor.extractPattern(field);
        if (pattern != null) {
            builder.pattern(pattern);
        }

        String validationMessage = ValidationAnnotationExtractor.extractValidationMessage(field);
        if (validationMessage != null) {
            builder.validationMessage(validationMessage);
        }
    }

    /**
     * اعمال type detection و read-only settings
     */
    private void applyTypeAndReadOnly(Field field, Class<?> fieldType, FormFieldDto.FormFieldDtoBuilder builder) {
        String fieldTypeStr = getFieldType(field);
        builder.type(fieldTypeStr);

        Column column = field.getAnnotation(Column.class);
        boolean isReadOnly = false;
        if (column != null) {
            if (!column.updatable() || !column.insertable()) {
                isReadOnly = true;
            }
            Integer maxLengthFromSize = ValidationAnnotationExtractor.extractMaxLength(field);
            if (maxLengthFromSize == null && column.length() > 0 && 
                (fieldType == String.class || fieldType == char[].class)) {
                builder.maxLength(column.length());
            }
        }
        builder.readonly(isReadOnly);
    }

    /**
     * اعمال default values
     */
    private void applyDefaultValues(Field field, Class<?> fieldType, FormFieldDto.FormFieldDtoBuilder builder) {
        Column column = field.getAnnotation(Column.class);
        Object defaultValue = extractDefaultValue(field, column);
        
        if (defaultValue != null) {
            builder.defaultValue(defaultValue);
        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
            builder.defaultValue(false);
        }
    }

    /**
     * اعمال enum options اگر فیلد enum باشد
     */
    private void applyEnumOptions(Field field, Class<?> fieldType, FormFieldDto.FormFieldDtoBuilder builder) {
        if (fieldType.isEnum() || field.isAnnotationPresent(Enumerated.class)) {
            if (fieldType.isEnum()) {
                List<Map<String, Object>> enumOptions = extractEnumOptions(fieldType);
                builder.options(enumOptions);
            }
        }
    }

    /**
     * استخراج مقدار پیش‌فرض از annotation ها
     */
    private Object extractDefaultValue(Field field, Column column) {
        // بررسی @Column(columnDefinition) که ممکن است DEFAULT داشته باشد
        if (column != null && column.columnDefinition() != null && !column.columnDefinition().isEmpty()) {
            String columnDef = column.columnDefinition().toUpperCase();
            // بررسی DEFAULT در columnDefinition
            if (columnDef.contains("DEFAULT")) {
                try {
                    // استخراج مقدار از DEFAULT 'value'
                    int defaultIndex = columnDef.indexOf("DEFAULT");
                    String defaultPart = columnDef.substring(defaultIndex + 7).trim();
                    if (defaultPart.startsWith("'") && defaultPart.contains("'")) {
                        String value = defaultPart.substring(1, defaultPart.indexOf("'", 1));
                        return value;
                    } else if (defaultPart.matches("\\d+")) {
                        return Integer.parseInt(defaultPart);
                    } else if (defaultPart.equalsIgnoreCase("TRUE") || defaultPart.equalsIgnoreCase("FALSE")) {
                        return Boolean.parseBoolean(defaultPart);
                    }
                } catch (Exception e) {
                    log.debug("خطا در استخراج default value از columnDefinition: {}", e.getMessage());
                }
            }
        }

        // بررسی @Value annotation
        try {
            Class<?> valueClass = Class.forName("org.springframework.beans.factory.annotation.Value");
            if (field.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) valueClass)) {
                java.lang.annotation.Annotation valueAnnotation = field.getAnnotation((Class<? extends java.lang.annotation.Annotation>) valueClass);
                String value = (String) valueClass.getMethod("value").invoke(valueAnnotation);
                if (value != null && !value.isEmpty()) {
                    // استخراج مقدار از ${...} یا #{...}
                    if (value.startsWith("${") && value.endsWith("}")) {
                        return value.substring(2, value.length() - 1);
                    } else if (value.startsWith("#{") && value.endsWith("}")) {
                        return value.substring(2, value.length() - 1);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        return null;
    }

    /**
     * استخراج گزینه‌های Enum برای combobox
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractEnumOptions(Class<?> enumClass) {
        List<Map<String, Object>> options = new ArrayList<>();
        try {
            if (enumClass.isEnum()) {
                Object[] enumConstants = enumClass.getEnumConstants();
                for (Object enumConstant : enumConstants) {
                    Map<String, Object> option = new HashMap<>();
                    option.put("value", enumConstant.toString());
                    option.put("label", formatEnumLabel(enumConstant.toString()));
                    options.add(option);
                }
            }
        } catch (Exception e) {
            log.warn("خطا در استخراج گزینه‌های Enum {}: {}", enumClass.getName(), e.getMessage());
        }
        return options;
    }

    /**
     * فرمت برچسب Enum (تبدیل SNAKE_CASE به خوانا)
     */
    private String formatEnumLabel(String enumValue) {
        if (enumValue == null || enumValue.isEmpty()) {
            return "";
        }
        // تبدیل SNAKE_CASE به "Snake Case"
        String lower = enumValue.replace('_', ' ').toLowerCase();
        Pattern pattern = Pattern.compile("\\b\\w");
        Matcher matcher = pattern.matcher(lower);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * تعیین نوع فیلد برای Angular Form Builder (بهبود یافته)
     */
    private String getFieldType(Field field) {
        Class<?> fieldType = field.getType();
        String fieldName = field.getName().toLowerCase();
        
        // اول بررسی Enum
        if (fieldType.isEnum() || field.isAnnotationPresent(Enumerated.class)) {
            return "combobox";
        }

        // بررسی @Lob برای multiline
        if (field.isAnnotationPresent(Lob.class) && fieldType == String.class) {
            return "multiline";
        }

        // ===== TYPE DETECTION بهتر =====
        // بررسی @Email annotation
        if (ValidationAnnotationExtractor.hasEmailAnnotation(field)) {
            return "email";
        }

        // تشخیص email از نام فیلد
        if (fieldName.contains("email") && fieldType == String.class) {
            return "email";
        }

        // تشخیص phone از نام فیلد
        if ((fieldName.contains("phone") || fieldName.contains("mobile") || fieldName.contains("tel")) 
            && fieldType == String.class) {
            return "text"; // می‌تواند "phone" هم باشد اگر Angular از آن پشتیبانی کند
        }

        // تشخیص nationalId / taxpayerNo
        if ((fieldName.contains("nationalid") || fieldName.contains("national_id") || 
             fieldName.contains("taxpayerno") || fieldName.contains("taxpayer_no")) 
            && fieldType == String.class) {
            return "text"; // می‌تواند "nationalId" هم باشد
        }

        // تشخیص url
        if (fieldName.contains("url") || fieldName.contains("link") || fieldName.contains("website")) {
            return "text"; // می‌تواند "url" هم باشد
        }

        // بررسی نوع داده
        if (fieldType == String.class) {
            // بررسی طول برای تشخیص multiline
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.length() > 255) {
                return "multiline";
            }
            return "text";
        } else if (fieldType == Integer.class || fieldType == int.class ||
                   fieldType == Long.class || fieldType == long.class ||
                   fieldType == Double.class || fieldType == double.class ||
                   fieldType == Float.class || fieldType == float.class ||
                   fieldType == Short.class || fieldType == short.class ||
                   fieldType == Byte.class || fieldType == byte.class) {
            return "number";
        } else if (fieldType == BigDecimal.class) {
            return "number"; // برای money fields
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "checkbox";
        } else if (fieldType == LocalDate.class || fieldType == java.sql.Date.class) {
            return "date";
        } else if (fieldType == OffsetDateTime.class || 
                   fieldType == LocalDateTime.class ||
                   fieldType == java.util.Date.class) {
            return "datetime";
        } else if (fieldType == UUID.class) {
            return "text";
        } else {
            return "text";
        }
    }

    /**
     * بررسی اینکه آیا فیلد اجباری است (mandatory) - بهبود یافته با Validation
     */
    private boolean isRequired(Field field) {
        // اول بررسی @Column(nullable)
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.nullable()) {
            return true;
        }
        
        // بررسی Jakarta Validation @NotNull
        if (ValidationAnnotationExtractor.hasNotNullAnnotation(field)) {
            return true;
        }
        
        // بررسی @NotBlank (برای String)
        if (ValidationAnnotationExtractor.hasNotBlankAnnotation(field)) {
            return true;
        }
        
        return false;
    }

    /**
     * دریافت برچسب فیلد از نام یا annotation
     */
    private String getFieldLabel(Field field) {
        // تبدیل camelCase به فضای خالی جدا شده
        String fieldName = field.getName();
        return camelCaseToLabel(fieldName);
    }

    /**
     * تبدیل camelCase به برچسب خوانا
     */
    private String camelCaseToLabel(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toUpperCase(camelCase.charAt(0)));
        
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append(' ');
            }
            result.append(ch);
        }
        
        return result.toString();
    }
}
