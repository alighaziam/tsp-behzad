package ir.tax.trustedtaxpayers.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO برای Schema فرم Angular Form Builder
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSchemaDto {
    /**
     * نام entity یا controller
     */
    private String entityName;
    
    /**
     * لیست فیلدهای فرم
     */
    private List<FormFieldDto> fields;
    
    /**
     * عنوان فرم
     */
    private String title;
    
    /**
     * توضیحات فرم
     */
    private String description;
}

