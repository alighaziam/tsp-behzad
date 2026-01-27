package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorDto {
    private String fieldKey;      // مثال: "inno" یا "body.fee[2]"
    private String message;       // پیام خطا به فارسی
    private String code;          // کد خطا مانند REQUIRED, TYPE_MISMATCH, PATTERN, LENGTH
}


