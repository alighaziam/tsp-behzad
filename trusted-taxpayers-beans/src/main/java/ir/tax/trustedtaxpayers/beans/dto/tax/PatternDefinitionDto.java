package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternDefinitionDto {

    @Schema(description = "شماره الگو", example = "2")
    private Integer patternNumber;

    @Schema(description = "عنوان الگو")
    private String title;

    @Schema(description = "شرح الگو")
    private String description;

    @Schema(description = "فیلدهای سربرگ")
    private List<PatternFieldDto> headerFields;

    @Schema(description = "فیلدهای اقلام")
    private List<PatternFieldDto> itemFields;
}
