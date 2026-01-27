package ir.tax.trustedtaxpayers.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityRelationDto {
    private String fieldName;
    private RelationType relationType;
    private String targetEntitySimpleName;
    private String targetEntityClassName;
    private Boolean required;
}


