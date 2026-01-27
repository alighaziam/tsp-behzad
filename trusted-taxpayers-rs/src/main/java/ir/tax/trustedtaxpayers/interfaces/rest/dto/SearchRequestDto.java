package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "درخواست جستجو")
public class SearchRequestDto {
    @Schema(description = "نام فیلد برای جستجو")
    @JsonProperty("key")
    private String key;
    
    @Schema(description = "عملگر جستجو (equal, like, gt, lt, gte, lte, in)")
    @JsonProperty("operand")
    private String operand;
    
    @Schema(description = "مقدار جستجو")
    @JsonProperty("value")
    private Object value;

    public SearchRequestDto() {
    }

    public SearchRequestDto(String key, String operand, Object value) {
        this.key = key;
        this.operand = operand;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperand() {
        return operand;
    }

    public void setOperand(String operand) {
        this.operand = operand;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}


