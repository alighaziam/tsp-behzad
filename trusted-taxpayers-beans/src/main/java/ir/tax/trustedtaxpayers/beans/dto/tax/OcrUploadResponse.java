package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class OcrUploadResponse {
    private Map<String, Object> meta;
    private OcrData ocr;
    private ParsedData parsed;

    @Data
    @Builder
    public static class OcrData {
        private String textPreview;
        private String fullText;
    }

    @Data
    @Builder
    public static class ParsedData {
        private String sellerName;
        private String sellerTaxId;
        private String buyerName;
        private String buyerTaxId;
        private String pattern;
        private List<Map<String, Object>> items;
        private String invoiceNumber;
        private String issueDate;
        private Number totalAmount;
        private String taxpayerId;
    }
}


