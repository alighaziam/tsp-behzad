package ir.tax.trustedtaxpayers.biz.service.ocr;

import ir.tax.trustedtaxpayers.beans.dto.tax.OcrUploadResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrFieldExtractionService {

    // الگوهای ساده برای شماره مالیاتی، تاریخ، مبلغ و شماره فاکتور (فارسی/لاتین)
    private static final Pattern TAX_ID = Pattern.compile("(?:شناسه\\s*اقتصادی|شناسه\\s*ملی|tax\\s*id|tin)[:\\s]*([۰-۹0-9]{5,14})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern INVOICE_NO = Pattern.compile("(?:شماره\\s*فاکتور|invoice\\s*no\\.?|شماره\\s*صورتحساب)[:\\s]*([A-Za-z0-9\\-/]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DATE = Pattern.compile("(\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2})");
    private static final Pattern TOTAL = Pattern.compile("(?:جمع\\s*کل|قابل\\s*پرداخت|total)[:\\s]*([۰-۹0-9][۰-۹0-9,\\.\\s]*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public OcrUploadResponse.ParsedData extract(String text) {
        if (text == null || text.isBlank()) {
            return OcrUploadResponse.ParsedData.builder().items(List.of()).build();
        }
        String latin = convertPersianDigits(text);

        String invoiceNo = groupOrNull(INVOICE_NO.matcher(latin), 1);
        String date = normalizeDate(groupOrNull(DATE.matcher(latin), 1));

        List<String> taxIds = findAll(TAX_ID, latin);
        String sellerTaxId = taxIds.size() > 0 ? digitsOnly(taxIds.get(0)) : null;
        String buyerTaxId = taxIds.size() > 1 ? digitsOnly(taxIds.get(1)) : null;

        String totalStr = groupOrNull(TOTAL.matcher(latin), 1);
        BigDecimal total = parseAmount(totalStr);

        return OcrUploadResponse.ParsedData.builder()
                .invoiceNumber(emptyToNull(invoiceNo))
                .issueDate(emptyToNull(date))
                .sellerTaxId(emptyToNull(sellerTaxId))
                .buyerTaxId(emptyToNull(buyerTaxId))
                .totalAmount(total)
                .items(List.of())
                .build();
    }

    private String convertPersianDigits(String input) {
        if (input == null) return null;
        StringBuilder out = new StringBuilder(input.length());
        for (char ch : input.toCharArray()) {
            out.append(switch (ch) {
                case '۰' -> '0';
                case '۱' -> '1';
                case '۲' -> '2';
                case '۳' -> '3';
                case '۴' -> '4';
                case '۵' -> '5';
                case '۶' -> '6';
                case '۷' -> '7';
                case '۸' -> '8';
                case '۹' -> '9';
                default -> ch;
            });
        }
        return out.toString();
    }

    private String normalizeDate(String d) {
        if (d == null) return null;
        return d.replace('.', '-').replace('/', '-');
    }

    private String groupOrNull(Matcher m, int idx) {
        return m.find() ? m.group(idx) : null;
    }

    private List<String> findAll(Pattern p, String s) {
        List<String> res = new ArrayList<>();
        Matcher m = p.matcher(s);
        while (m.find()) {
            res.add(m.group(1));
        }
        return res;
    }

    private String digitsOnly(String s) {
        if (s == null) return null;
        return s.replaceAll("[^0-9]", "");
    }

    private BigDecimal parseAmount(String s) {
        try {
            if (s == null) return null;
            String n = s.replace(",", "").replace(" ", "");
            return new BigDecimal(n);
        } catch (Exception e) {
            return null;
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}


