package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSubmissionRequest {

    @NotNull
    @Schema(description = "شماره الگو (1-16)", example = "2")
    private Integer patternNumber;

    @Schema(description = "اطلاعات Header به‌صورت key/value. کلیدها مطابق schema")
    private Map<String, Object> header;

    @Schema(description = "لیست اقلام Body. هر آیتم شامل کلیدهای body.* بدون پیشوند body.")
    private List<Map<String, Object>> items;

    @Schema(description = "نوع عملیات (اختیاری) مثلا VALIDATE_ONLY یا SUBMIT")
    private String operation;
}


