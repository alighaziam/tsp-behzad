package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * کلاس پایه برای Pattern Field Providers
 * متدهای کمکی مشترک را فراهم می‌کند
 */
public abstract class AbstractPatternFieldProvider implements InvoicePatternFieldProvider {
    
    /**
     * Helper method برای ایجاد FormFieldDto
     */
    protected FormFieldDto createField(String key, String type, String name, String label, 
                                       boolean required, int order, Integer maxLength, 
                                       List<Map<String, Object>> options, String placeholder) {
        FormFieldDto.FormFieldDtoBuilder builder = FormFieldDto.builder()
                .key(key)
                .type(type)
                .name(getEnglishNameForKey(key, name))
                .label(label)
                .required(required)
                .mandatory(required)
                .order(order)
                .disabled(false)
                .readonly(false)
                .placeholder(placeholder);

        if (maxLength != null) {
            builder.maxLength(maxLength);
        }

        if (options != null) {
            builder.options(options);
        }

        return builder.build();
    }

    private String getEnglishNameForKey(String key, String fallbackFa) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("inno", "Invoice Number");
        map.put("indatim", "Invoice DateTime");
        map.put("inty", "Invoice Type");
        map.put("ins", "Invoice Subject");
        map.put("inp", "Invoice Pattern");
        map.put("tins", "Seller Tax ID");
        map.put("sbc", "Seller Branch Code");
        map.put("tob", "Buyer Type");
        map.put("bid", "Buyer National/Foreign ID");
        map.put("tinb", "Buyer Tax ID");
        map.put("bbc", "Buyer Branch Code");
        map.put("bpc", "Buyer Postal Code");
        map.put("bpn", "Buyer Passport Number");
        map.put("tprdis", "Total Price Before Discount");
        map.put("tdis", "Total Discount");
        map.put("tadis", "Total After Discount");
        map.put("tvam", "Total VAT Amount");
        map.put("tbill", "Invoice Total");
        map.put("setm", "Settlement Type");
        map.put("cap", "Cash Amount");
        map.put("insp", "Installment Amount");
        map.put("tvop", "Total Tax Paid");
        map.put("body.sstid", "Item Code");
        map.put("body.sstt", "Item Title");
        map.put("body.mu", "Measure Unit");
        map.put("body.am", "Amount");
        map.put("body.fee", "Unit Price");
        map.put("body.prdis", "Price Before Discount");
        map.put("body.dis", "Discount");
        map.put("body.adis", "Price After Discount");
        map.put("body.vra", "Tax Rate");
        map.put("body.vam", "Tax Amount");
        // pattern 2 specific
        map.put("scc", "Seller Customs Code");
        map.put("scln", "Seller Customs License No");
        map.put("body.cut", "Currency Code");
        map.put("body.cfee", "Unit Price (Currency)");
        map.put("body.exr", "Exchange Rate");
        map.put("body.cpr", "Currency Purchase Rate");
        map.put("body.sovat", "VAT Base");
        map.put("body.vba", "Tax Base");

        String v = map.get(key);
        if (v != null) return v;
        // default fallback: transform key to English-like title
        String k = key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : key;
        return toTitle(k);
    }

    private String toTitle(String s) {
        if (s == null || s.isBlank()) return "Field";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0) out.append(Character.toUpperCase(c));
            else if (c == '_' || c == '-') out.append(' ');
            else if (Character.isUpperCase(c)) { out.append(' ').append(c); }
            else out.append(c);
        }
        return out.toString();
    }
    
    /**
     * ایجاد فیلد text
     */
    protected FormFieldDto textField(String key, String name, boolean required, int order, Integer maxLength) {
        return createField(key, "text", name, name, required, order, maxLength, null, 
                "لطفا " + name + " را وارد کنید");
    }
    
    /**
     * ایجاد فیلد number
     */
    protected FormFieldDto numberField(String key, String name, boolean required, int order) {
        return createField(key, "number", name, name, required, order, null, null, 
                "لطفا " + name + " را وارد کنید");
    }
    
    /**
     * ایجاد فیلد combobox
     */
    protected FormFieldDto comboboxField(String key, String name, boolean required, int order, 
                                        List<Map<String, Object>> options) {
        return createField(key, "combobox", name, name, required, order, null, options, 
                "لطفا " + name + " را انتخاب کنید");
    }
    
    /**
     * ایجاد فیلد datetime
     */
    protected FormFieldDto datetimeField(String key, String name, boolean required, int order) {
        return createField(key, "datetime", name, name, required, order, null, null, 
                "لطفا " + name + " را وارد کنید");
    }
    
    /**
     * گزینه‌های نوع پرواز
     */
    protected List<Map<String, Object>> getFlightTypeOptions() {
        return Arrays.asList(
                Map.of("value", 1, "label", "داخلی"),
                Map.of("value", 2, "label", "خارجی")
        );
    }
}

