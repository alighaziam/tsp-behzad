package ir.tax.trustedtaxpayers.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityInfoDto {
    private String className;
    private String simpleName;
    private String tableName;
    private String packageName;
    private Integer fieldCount;
    private Boolean hasSchema;
    private List<EntityRelationDto> relations;
    private List<String> relatedEntities;
}


