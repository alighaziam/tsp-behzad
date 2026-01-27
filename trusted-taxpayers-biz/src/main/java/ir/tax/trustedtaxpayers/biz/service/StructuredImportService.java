package ir.tax.trustedtaxpayers.biz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StructuredImportService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> parse(MultipartFile file) throws Exception {
        String ct = file.getContentType() != null ? file.getContentType() : "";
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (ct.equals(MediaType.APPLICATION_JSON_VALUE) || name.endsWith(".json")) {
            return parseJson(file.getInputStream());
        }
        if (ct.equals("text/csv") || name.endsWith(".csv")) {
            return parseCsv(file.getInputStream());
        }
        if (ct.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || name.endsWith(".xlsx")) {
            return parseXlsx(file.getInputStream());
        }
        throw new IllegalArgumentException("Unsupported file type: " + ct);
    }

    private Map<String, Object> parseJson(InputStream in) throws Exception {
        // Expecting a normalized JSON matching the invoice schema or array of items
        Map<String, Object> obj = objectMapper.readValue(in, Map.class);
        return normalize(obj);
    }

    private Map<String, Object> parseCsv(InputStream in) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .parse(reader);
            Map<String, Integer> header = parser.getHeaderMap();
            List<Map<String, Object>> items = new ArrayList<>();
            String sellerName = null, sellerTaxId = null, buyerName = null, buyerTaxId = null, invoiceNumber = null, issueDate = null, totalAmount = null, pattern = null;
            for (CSVRecord r : parser) {
                // Header names: sellerName,sellerTaxId,buyerName,buyerTaxId,invoiceNumber,issueDate,totalAmount,pattern,itemName,qty,price
                sellerName = get(r, header, "sellerName", sellerName);
                sellerTaxId = get(r, header, "sellerTaxId", sellerTaxId);
                buyerName = get(r, header, "buyerName", buyerName);
                buyerTaxId = get(r, header, "buyerTaxId", buyerTaxId);
                invoiceNumber = get(r, header, "invoiceNumber", invoiceNumber);
                issueDate = get(r, header, "issueDate", issueDate);
                totalAmount = get(r, header, "totalAmount", totalAmount);
                pattern = get(r, header, "pattern", pattern);
                if (header.containsKey("itemName")) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", r.get("itemName"));
                    item.put("quantity", parseNumber(r, "qty"));
                    item.put("unitPrice", parseNumber(r, "price"));
                    items.add(item);
                }
            }
            Map<String, Object> obj = new HashMap<>();
            obj.put("sellerName", sellerName);
            obj.put("sellerTaxId", sellerTaxId);
            obj.put("buyerName", buyerName);
            obj.put("buyerTaxId", buyerTaxId);
            obj.put("invoiceNumber", invoiceNumber);
            obj.put("issueDate", issueDate);
            obj.put("totalAmount", totalAmount);
            obj.put("pattern", pattern);
            obj.put("items", items);
            return normalize(obj);
        }
    }

    private Map<String, Object> parseXlsx(InputStream in) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            // Expect header in row 0 similar to CSV
            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (int i = 0; header != null && i < header.getLastCellNum(); i++) {
                var cell = header.getCell(i);
                if (cell != null && cell.getStringCellValue() != null) {
                    col.put(cell.getStringCellValue().trim(), i);
                }
            }
            List<Map<String, Object>> items = new ArrayList<>();
            String sellerName = null, sellerTaxId = null, buyerName = null, buyerTaxId = null, invoiceNumber = null, issueDate = null, totalAmount = null, pattern = null;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                sellerName = get(row, col, "sellerName", sellerName);
                sellerTaxId = get(row, col, "sellerTaxId", sellerTaxId);
                buyerName = get(row, col, "buyerName", buyerName);
                buyerTaxId = get(row, col, "buyerTaxId", buyerTaxId);
                invoiceNumber = get(row, col, "invoiceNumber", invoiceNumber);
                issueDate = get(row, col, "issueDate", issueDate);
                totalAmount = get(row, col, "totalAmount", totalAmount);
                pattern = get(row, col, "pattern", pattern);
                if (col.containsKey("itemName")) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", get(row, col, "itemName", null));
                    item.put("quantity", parseNumber(row, col, "qty"));
                    item.put("unitPrice", parseNumber(row, col, "price"));
                    items.add(item);
                }
            }
            Map<String, Object> obj = new HashMap<>();
            obj.put("sellerName", sellerName);
            obj.put("sellerTaxId", sellerTaxId);
            obj.put("buyerName", buyerName);
            obj.put("buyerTaxId", buyerTaxId);
            obj.put("invoiceNumber", invoiceNumber);
            obj.put("issueDate", issueDate);
            obj.put("totalAmount", totalAmount);
            obj.put("pattern", pattern);
            obj.put("items", items);
            return normalize(obj);
        }
    }

    private static String get(CSVRecord r, Map<String, Integer> header, String key, String fallback) {
        if (header.containsKey(key)) {
            String v = r.get(key);
            return (v != null && !v.isBlank()) ? v.trim() : fallback;
        }
        return fallback;
    }

    private static String get(Row row, Map<String, Integer> col, String key, String fallback) {
        Integer i = col.get(key);
        if (i == null) return fallback;
        var cell = row.getCell(i);
        if (cell == null) return fallback;
        cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        String v = cell.getStringCellValue();
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    private static Number parseNumber(CSVRecord r, String key) {
        try {
            String s = r.isMapped(key) ? r.get(key) : null;
            if (s == null || s.isBlank()) return null;
            return Double.parseDouble(s.replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Number parseNumber(Row row, Map<String, Integer> col, String key) {
        try {
            Integer i = col.get(key);
            if (i == null) return null;
            var cell = row.getCell(i);
            if (cell == null) return null;
            return Double.parseDouble(cell.toString().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Object> normalize(Map<String, Object> obj) {
        // Ensure items exists
        obj.computeIfAbsent("items", k -> new ArrayList<>());
        return obj;
    }
}


