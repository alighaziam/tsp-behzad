package ir.tax.trustedtaxpayers.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class SearchRequest {
    @JsonProperty("filters")
    private List<Filter> filters = new ArrayList<>();
    
    @JsonProperty("page")
    private Integer page = 0;
    
    @JsonProperty("size")
    private Integer size = 10;
    
    @JsonProperty("sortBy")
    private String sortBy = "id";
    
    @JsonProperty("direction")
    private String direction = "asc";

    public SearchRequest() {
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page != null ? page : 0;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size != null ? size : 10;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null ? sortBy : "id";
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction != null ? direction : "asc";
    }

    public static class Filter {
        @JsonProperty("key")
        private String key;
        
        @JsonProperty("operand")
        private String operand; // مثل >, <, =, !=, ~
        
        @JsonProperty("value")
        private Object value;

        public Filter() {
        }

        public Filter(String key, String operand, Object value) {
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
}


