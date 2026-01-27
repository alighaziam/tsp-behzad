package ir.tax.trustedtaxpayers.application.dto;

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
public class FormFieldDto {
    private String key;
    private String type;
    private String name;
    private String label;
    private String placeholder;
    private Boolean required;
    private Boolean mandatory;
    private Boolean disabled;
    private Boolean readonly;
    private Object defaultValue;
    private List<Map<String, Object>> options;
    private Integer maxLength;
    private Integer minLength;
    private String pattern;
    private String validationMessage;
    private Integer order;
    private String className;
    private Map<String, Object> additionalProperties;
}


