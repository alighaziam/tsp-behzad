package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDto {
    private boolean valid;
    @Builder.Default
    private List<ValidationErrorDto> errors = new ArrayList<>();

    public static ValidationResultDto ok() {
        return ValidationResultDto.builder().valid(true).build();
    }

    public void addError(String fieldKey, String code, String message) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(ValidationErrorDto.builder()
                .fieldKey(fieldKey)
                .code(code)
                .message(message)
                .build());
        valid = false;
    }
}


