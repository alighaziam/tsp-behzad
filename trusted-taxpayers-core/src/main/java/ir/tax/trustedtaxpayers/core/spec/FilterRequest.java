package ir.tax.trustedtaxpayers.core.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FilterRequest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private String key;
    private String operand;

    @JsonProperty("value")
    private JsonNode valueNode;

    @JsonIgnore
    private Object value;

    public FilterRequest() {
    }

    public FilterRequest(String key, String operand, Object value) {
        this.key = key;
        this.operand = operand;
        this.value = value;
        // تبدیل Object به JsonNode اگر value تنظیم شده باشد
        if (value != null) {
            this.valueNode = OBJECT_MAPPER.valueToTree(value);
        }
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

    /**
     * دریافت value به صورت JsonNode (برای serialization به JSON)
     */
    @JsonProperty("value")
    public JsonNode getValueNode() {
        return valueNode;
    }

    /**
     * تنظیم value به صورت JsonNode (برای deserialization از JSON)
     */
    @JsonProperty("value")
    public void setValueNode(JsonNode valueNode) {
        this.valueNode = valueNode;
        this.value = null; // reset cached value
    }

    /**
     * دریافت value به صورت Object (برای استفاده در BaseSpecification)
     * این متد JsonNode را به Object مناسب تبدیل می‌کند
     */
    public Object getValue() {
        // اگر value قبلاً cache شده باشد، آن را برمی‌گرداند
        if (value != null) {
            return value;
        }
        
        // اگر valueNode وجود داشته باشد، آن را به Object تبدیل می‌کند
        if (valueNode != null) {
            value = convertJsonNodeToObject(valueNode);
            return value;
        }
        
        return null;
    }

    /**
     * تنظیم value به صورت Object (برای سازگاری با کدهای قدیمی)
     */
    public void setValue(Object value) {
        this.value = value;
        // تبدیل Object به JsonNode
        if (value != null) {
            this.valueNode = OBJECT_MAPPER.valueToTree(value);
        } else {
            this.valueNode = null;
        }
    }

    /**
     * تبدیل JsonNode به Object مناسب
     */
    private Object convertJsonNodeToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        
        if (node.isInt()) {
            return node.intValue();
        }
        
        if (node.isLong()) {
            return node.longValue();
        }
        
        if (node.isDouble()) {
            return node.doubleValue();
        }
        
        if (node.isFloat()) {
            return node.floatValue();
        }
        
        if (node.isTextual()) {
            return node.textValue();
        }
        
        if (node.isArray()) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (JsonNode item : node) {
                list.add(convertJsonNodeToObject(item));
            }
            return list;
        }
        
        if (node.isObject()) {
            // برای object، آن را به Map تبدیل می‌کنیم
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            node.fields().forEachRemaining(entry -> {
                map.put(entry.getKey(), convertJsonNodeToObject(entry.getValue()));
            });
            return map;
        }
        
        // در غیر این صورت، به صورت string برمی‌گرداند
        return node.asText();
    }
}

