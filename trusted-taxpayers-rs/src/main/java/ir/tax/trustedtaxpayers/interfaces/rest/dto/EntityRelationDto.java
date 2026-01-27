package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO برای رابطه بین Entity ها
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRelationDto {
    /**
     * نام فیلد رابطه
     */
    private String fieldName;
    
    /**
     * نوع رابطه: ManyToOne, OneToMany, OneToOne, ManyToMany
     */
    private RelationType relationType;
    
    /**
     * نام کلاس Entity مرتبط
     */
    private String relatedEntityClassName;
    
    /**
     * نام ساده کلاس Entity مرتبط
     */
    private String relatedEntitySimpleName;
    
    /**
     * آیا رابطه اجباری است؟
     */
    private Boolean required;
    
    /**
     * آیا رابطه nullable است؟
     */
    private Boolean nullable;
}

