package ir.tax.trustedtaxpayers.biz.service.imports.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceItemDto;
import ir.tax.trustedtaxpayers.biz.service.imports.ImportParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportParserServiceImpl implements ImportParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<NormalizedInvoiceDto> parseCsv(MultipartFile file) {
        List<NormalizedInvoiceDto> invoices = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return invoices;
            String[] headers = headerLine.split(",");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", -1);
                String invoiceNumber = get(cols, headers, "invoiceNumber");
                String issueDate = get(cols, headers, "issueDate");
                String sellerTaxId = get(cols, headers, "sellerTaxId");
                String buyerTaxId = get(cols, headers, "buyerTaxId");
                String currency = get(cols, headers, "currency");
                String totalAmount = get(cols, headers, "totalAmount");
                String itemName = get(cols, headers, "itemName");
                String itemQty = get(cols, headers, "itemQty");
                String itemUnitPrice = get(cols, headers, "itemUnitPrice");

                NormalizedInvoiceItemDto item = NormalizedInvoiceItemDto.builder()
                        .itemName(nullIfBlank(itemName))
                        .itemQty(parseBigDecimal(itemQty))
                        .itemUnitPrice(parseBigDecimal(itemUnitPrice))
                        .build();

                NormalizedInvoiceDto dto = NormalizedInvoiceDto.builder()
                        .invoiceNumber(nullIfBlank(invoiceNumber))
                        .issueDate(nullIfBlank(issueDate))
                        .sellerTaxId(nullIfBlank(sellerTaxId))
                        .buyerTaxId(nullIfBlank(buyerTaxId))
                        .currency(nullIfBlank(currency))
                        .totalAmount(parseBigDecimal(totalAmount))
                        .items(List.of(item))
                        .build();
                invoices.add(dto);
            }
        } catch (Exception e) {
            log.error("Failed to parse CSV import", e);
        }
        return invoices;
    }

    @Override
    public List<NormalizedInvoiceDto> parseJson(MultipartFile file) {
        List<NormalizedInvoiceDto> invoices = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            JsonNode arr = root.path("invoices");
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    List<NormalizedInvoiceItemDto> items = new ArrayList<>();
                    JsonNode itemsNode = node.path("items");
                    if (itemsNode.isArray()) {
                        for (JsonNode it : itemsNode) {
                            items.add(NormalizedInvoiceItemDto.builder()
                                    .itemName(textOrNull(it, "itemName"))
                                    .itemQty(numberOrNull(it, "itemQty"))
                                    .itemUnitPrice(numberOrNull(it, "itemUnitPrice"))
                                    .build());
                        }
                    }
                    invoices.add(NormalizedInvoiceDto.builder()
                            .invoiceNumber(textOrNull(node, "invoiceNumber"))
                            .issueDate(textOrNull(node, "issueDate"))
                            .sellerTaxId(textOrNull(node, "sellerTaxId"))
                            .buyerTaxId(textOrNull(node, "buyerTaxId"))
                            .currency(textOrNull(node, "currency"))
                            .totalAmount(numberOrNull(node, "totalAmount"))
                            .items(items)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON import", e);
        }
        return invoices;
    }

    @Override
    public List<NormalizedInvoiceDto> parseXlsx(MultipartFile file) {
        // در این مرحله، بدون وابستگی POI، پشتیبانی نمی‌کنیم
        log.warn("XLSX parsing not implemented yet");
        return List.of();
    }

    private String get(String[] cols, String[] headers, String key) {
        for (int i = 0; i < headers.length; i++) {
            if (key.equalsIgnoreCase(headers[i].trim())) {
                return i < cols.length ? cols[i].trim() : "";
            }
        }
        return "";
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private BigDecimal parseBigDecimal(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            return new BigDecimal(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private BigDecimal numberOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        try {
            if (v.isNumber()) return new BigDecimal(v.asText());
            if (v.isTextual()) return new BigDecimal(v.asText());
        } catch (Exception ignored) {
        }
        return null;
    }
}


