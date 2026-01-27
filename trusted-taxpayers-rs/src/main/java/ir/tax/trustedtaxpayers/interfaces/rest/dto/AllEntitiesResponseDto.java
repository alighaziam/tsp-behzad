package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO برای پاسخ شامل تمام Entity ها و Schema های آنها
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllEntitiesResponseDto {
    /**
     * لیست اطلاعات Entity ها
     */
    private List<EntityInfoDto> entities;
    
    /**
     * تعداد کل Entity ها
     */
    private Integer totalCount;
    
    /**
     * Map از Entity name به FormSchemaDto
     */
    private Map<String, FormSchemaDto> schemas;
}

