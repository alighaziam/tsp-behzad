package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO برای اطلاعات یک Entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityInfoDto {
    /**
     * نام کامل کلاس Entity
     */
    private String className;
    
    /**
     * نام ساده کلاس Entity
     */
    private String simpleName;
    
    /**
     * نام جدول در دیتابیس
     */
    private String tableName;
    
    /**
     * Package کلاس
     */
    private String packageName;
    
    /**
     * تعداد فیلدها
     */
    private Integer fieldCount;
    
    /**
     * آیا Form Schema موجود است؟
     */
    private Boolean hasSchema;
    
    /**
     * لیست رابطه‌های Entity با Entity های دیگر
     */
    private List<EntityRelationDto> relations;
    
    /**
     * لیست Entity های مرتبط (مستقیم)
     */
    private List<String> relatedEntities;
}

