package ir.tax.trustedtaxpayers.biz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptive field extraction service that learns from actual invoice structure.
 * This service analyzes the text structure and adapts extraction patterns dynamically.
 */
@Service
@Slf4j
public class AdaptiveFieldExtractionService {

    private final FieldExtractionService baseExtractionService;

    public AdaptiveFieldExtractionService(FieldExtractionService baseExtractionService) {
        this.baseExtractionService = baseExtractionService;
    }

    /**
     * Extract fields using adaptive patterns based on actual invoice structure
     */
    public Map<String, Object> extractAdaptive(String text) {
        if (text == null || text.isBlank()) {
            return baseExtractionService.extract(text);
        }

        // Analyze text structure
        InvoiceStructure structure = analyzeStructure(text);
        
        // Use base extraction as fallback
        Map<String, Object> result = baseExtractionService.extract(text);
        
        // Enhance with adaptive extraction
        enhanceWithAdaptiveExtraction(text, structure, result);
        
        return result;
    }

    /**
     * Analyze the structure of the invoice text to identify patterns
     */
    private InvoiceStructure analyzeStructure(String text) {
        InvoiceStructure structure = new InvoiceStructure();
        
        // Detect line separators
        structure.hasNewlines = text.contains("\n") || text.contains("\r");
        structure.hasTabs = text.contains("\t");
        structure.hasColons = text.contains(":");
        structure.hasDashes = text.contains("-") || text.contains("—") || text.contains("–");
        
        // Detect common invoice sections
        structure.hasSellerSection = detectSection(text, "فروشنده", "seller");
        structure.hasBuyerSection = detectSection(text, "خریدار", "buyer");
        structure.hasItemsSection = detectSection(text, "اقلام", "items", "کالا", "خدمت");
        structure.hasTotalSection = detectSection(text, "جمع", "کل", "total", "مبلغ");
        
        // Detect common field labels
        structure.fieldLabels = detectFieldLabels(text);
        
        // Detect table-like structure (items in rows)
        structure.hasTableStructure = detectTableStructure(text);
        
        log.debug("Invoice structure analysis: {}", structure);
        return structure;
    }

    private boolean detectSection(String text, String... keywords) {
        String lowerText = text.toLowerCase();
        for (String keyword : keywords) {
            if (text.contains(keyword) || lowerText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> detectFieldLabels(String text) {
        Set<String> labels = new HashSet<>();
        
        // Common Persian labels
        String[] persianLabels = {
            "فروشنده", "خریدار", "شماره", "تاریخ", "شناسه", "ملی",
            "نام", "کد", "اقلام", "کالا", "خدمت", "تعداد", "قیمت",
            "فی", "واحد", "جمع", "کل", "مبلغ", "الگو", "صورتحساب"
        };
        
        for (String label : persianLabels) {
            if (text.contains(label)) {
                labels.add(label);
            }
        }
        
        return labels;
    }

    private boolean detectTableStructure(String text) {
        // Check for multiple lines with similar structure (potential table)
        String[] lines = text.split("[\r\n]+");
        if (lines.length < 3) return false;
        
        // Check if multiple lines contain numbers (likely items)
        int linesWithNumbers = 0;
        Pattern numberPattern = Pattern.compile("\\d+");
        for (String line : lines) {
            if (numberPattern.matcher(line).find()) {
                linesWithNumbers++;
            }
        }
        
        return linesWithNumbers >= 3;
    }

    /**
     * Enhance extraction results with adaptive patterns
     */
    private void enhanceWithAdaptiveExtraction(String text, InvoiceStructure structure, Map<String, Object> result) {
        // If seller section detected, try to extract more accurately
        if (structure.hasSellerSection) {
            enhanceSellerExtraction(text, structure, result);
        }
        
        // If buyer section detected, try to extract more accurately
        if (structure.hasBuyerSection) {
            enhanceBuyerExtraction(text, structure, result);
        }
        
        // If items section detected, try to extract items more accurately
        if (structure.hasItemsSection || structure.hasTableStructure) {
            enhanceItemsExtraction(text, structure, result);
        }
        
        // If total section detected, try to extract total more accurately
        if (structure.hasTotalSection) {
            enhanceTotalExtraction(text, structure, result);
        }
    }

    private void enhanceSellerExtraction(String text, InvoiceStructure structure, Map<String, Object> result) {
        // Try to find seller section boundaries
        Pattern sellerSectionPattern = Pattern.compile(
            "(?:فروشنده|seller)[\\s\\S]{0,500}?(?:خریدار|buyer|اقلام|items|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = sellerSectionPattern.matcher(text);
        if (matcher.find()) {
            String sellerSection = matcher.group(0);
            
            // Extract seller name with better context
            if (result.get("sellerName") == null || ((String) result.get("sellerName")).isBlank()) {
                String sellerName = extractFromSection(sellerSection, "نام", "name");
                if (sellerName != null && !sellerName.isBlank()) {
                    result.put("sellerName", sellerName);
                }
            }
            
            // Extract seller tax ID with better context
            if (result.get("sellerTaxId") == null || ((String) result.get("sellerTaxId")).isBlank()) {
                String sellerTaxId = extractFromSection(sellerSection, "شناسه\\s*ملی", "tax\\s*id", "کد\\s*اقتصادی");
                if (sellerTaxId != null && !sellerTaxId.isBlank()) {
                    result.put("sellerTaxId", sellerTaxId);
                }
            }
        }
    }

    private void enhanceBuyerExtraction(String text, InvoiceStructure structure, Map<String, Object> result) {
        // Try to find buyer section boundaries
        Pattern buyerSectionPattern = Pattern.compile(
            "(?:خریدار|buyer)[\\s\\S]{0,500}?(?:اقلام|items|جمع|total|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = buyerSectionPattern.matcher(text);
        if (matcher.find()) {
            String buyerSection = matcher.group(0);
            
            // Extract buyer name with better context
            if (result.get("buyerName") == null || ((String) result.get("buyerName")).isBlank()) {
                String buyerName = extractFromSection(buyerSection, "نام", "name");
                if (buyerName != null && !buyerName.isBlank()) {
                    result.put("buyerName", buyerName);
                }
            }
            
            // Extract buyer tax ID with better context
            if (result.get("buyerTaxId") == null || ((String) result.get("buyerTaxId")).isBlank()) {
                String buyerTaxId = extractFromSection(buyerSection, "شناسه\\s*ملی", "tax\\s*id", "کد\\s*اقتصادی");
                if (buyerTaxId != null && !buyerTaxId.isBlank()) {
                    result.put("buyerTaxId", buyerTaxId);
                }
            }
        }
    }

    private void enhanceItemsExtraction(String text, InvoiceStructure structure, Map<String, Object> result) {
        // If table structure detected, try table-based extraction
        if (structure.hasTableStructure) {
            List<Map<String, Object>> tableItems = extractItemsFromTable(text);
            if (!tableItems.isEmpty()) {
                result.put("items", tableItems);
                return;
            }
        }
        
        // Otherwise, try section-based extraction
        Pattern itemsSectionPattern = Pattern.compile(
            "(?:اقلام|items|کالا|خدمت)[\\s\\S]{0,2000}?(?:جمع|total|مبلغ|کل|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = itemsSectionPattern.matcher(text);
        if (matcher.find()) {
            String itemsSection = matcher.group(0);
            List<Map<String, Object>> sectionItems = extractItemsFromSection(itemsSection);
            if (!sectionItems.isEmpty()) {
                result.put("items", sectionItems);
            }
        }
    }

    private void enhanceTotalExtraction(String text, InvoiceStructure structure, Map<String, Object> result) {
        // Try to find total section
        Pattern totalSectionPattern = Pattern.compile(
            "(?:جمع\\s*کل|مبلغ\\s*کل|total)[\\s\\S]{0,200}?",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = totalSectionPattern.matcher(text);
        if (matcher.find()) {
            String totalSection = matcher.group(0);
            String total = extractAmountFromSection(totalSection);
            if (total != null && !total.isBlank()) {
                result.put("totalAmount", total);
            }
        }
    }

    private String extractFromSection(String section, String... keywords) {
        for (String keyword : keywords) {
            Pattern pattern = Pattern.compile(
                keyword + "\\s*[:\\-]?\\s*([^\\r\\n]{2,100})",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
            Matcher matcher = pattern.matcher(section);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                // Clean up value
                value = value.replaceAll("[\\s:;\\-]+$", "");
                if (!value.isBlank() && value.length() <= 200) {
                    return value;
                }
            }
        }
        return null;
    }

    private String extractAmountFromSection(String section) {
        // Look for numbers in the section
        Pattern amountPattern = Pattern.compile("([\\d,\\.]+)");
        Matcher matcher = amountPattern.matcher(section);
        if (matcher.find()) {
            String amount = matcher.group(1).replaceAll("[^\\d\\.]", "");
            return amount.isBlank() ? null : amount;
        }
        return null;
    }

    private List<Map<String, Object>> extractItemsFromTable(String text) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        // Split into lines
        String[] lines = text.split("[\r\n]+");
        
        // Try to identify item rows (lines with numbers that might be quantities/prices)
        Pattern itemRowPattern = Pattern.compile(
            "([^\\r\\n]{3,100})\\s+([\\d,\\.]+)\\s+([\\d,\\.]+)",
            Pattern.MULTILINE
        );
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            Matcher matcher = itemRowPattern.matcher(line);
            if (matcher.find()) {
                Map<String, Object> item = new HashMap<>();
                String itemName = matcher.group(1).trim();
                String quantity = matcher.group(2).replaceAll("[^\\d\\.]", "");
                String price = matcher.group(3).replaceAll("[^\\d\\.]", "");
                
                if (!itemName.isBlank() && !quantity.isBlank() && !price.isBlank()) {
                    item.put("sstt", itemName);
                    item.put("am", quantity);
                    item.put("fee", price);
                    item.put("mu", "عدد");
                    items.add(item);
                }
            }
        }
        
        return items;
    }

    private List<Map<String, Object>> extractItemsFromSection(String section) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        // Try multiple patterns based on common invoice formats
        // Pattern 1: Line-based items with separators
        String[] lines = section.split("[\r\n]+");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // Try to extract item info from line
            Map<String, Object> item = extractItemFromLine(line);
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }
        
        // If no items found from lines, try regex patterns
        if (items.isEmpty()) {
            items = extractItemsWithRegex(section);
        }
        
        return items;
    }

    private Map<String, Object> extractItemFromLine(String line) {
        Map<String, Object> item = new HashMap<>();
        
        // Pattern: "کالای 1 - تعداد: 10 - قیمت: 100000"
        Pattern pattern1 = Pattern.compile(
            "([^\\-]{2,50})\\s*[-–—]\\s*(?:تعداد|مقدار)\\s*[:\\-]?\\s*([\\d,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی)\\s*[:\\-]?\\s*([\\d,\\.]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = pattern1.matcher(line);
        if (matcher.find()) {
            String itemName = matcher.group(1).trim();
            String quantity = matcher.group(2).replaceAll("[^\\d\\.]", "");
            String price = matcher.group(3).replaceAll("[^\\d\\.]", "");
            
            if (!itemName.isBlank() && !quantity.isBlank() && !price.isBlank()) {
                item.put("sstt", itemName);
                item.put("am", quantity);
                item.put("fee", price);
                item.put("mu", "عدد");
                return item;
            }
        }
        
        // Pattern: "کالا: نام - تعداد: 10 - قیمت: 100000"
        Pattern pattern2 = Pattern.compile(
            "(?:کالا|خدمت|item)\\s*[:\\-]?\\s*([^\\-]{2,50})\\s*[-–—]\\s*(?:تعداد|مقدار)\\s*[:\\-]?\\s*([\\d,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی)\\s*[:\\-]?\\s*([\\d,\\.]+)",
            Pattern.CASE_INSENSITIVE
        );
        
        matcher = pattern2.matcher(line);
        if (matcher.find()) {
            String itemName = matcher.group(1).trim();
            String quantity = matcher.group(2).replaceAll("[^\\d\\.]", "");
            String price = matcher.group(3).replaceAll("[^\\d\\.]", "");
            
            if (!itemName.isBlank() && !quantity.isBlank() && !price.isBlank()) {
                item.put("sstt", itemName);
                item.put("am", quantity);
                item.put("fee", price);
                item.put("mu", "عدد");
                return item;
            }
        }
        
        return null;
    }

    private List<Map<String, Object>> extractItemsWithRegex(String section) {
        List<Map<String, Object>> items = new ArrayList<>();
        
        // Use base extraction service's patterns as fallback
        // This delegates to the base service for complex patterns
        try {
            Map<String, Object> baseResult = baseExtractionService.extract(section);
            if (baseResult != null && baseResult.containsKey("items")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> baseItems = (List<Map<String, Object>>) baseResult.get("items");
                if (baseItems != null) {
                    items.addAll(baseItems);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract items using base service: {}", e.getMessage());
        }
        
        return items;
    }

    /**
     * Structure analysis result
     */
    private static class InvoiceStructure {
        boolean hasNewlines;
        boolean hasTabs;
        boolean hasColons;
        boolean hasDashes;
        boolean hasSellerSection;
        boolean hasBuyerSection;
        boolean hasItemsSection;
        boolean hasTotalSection;
        boolean hasTableStructure;
        Set<String> fieldLabels = new HashSet<>();

        @Override
        public String toString() {
            return String.format(
                "InvoiceStructure{newlines=%s, seller=%s, buyer=%s, items=%s, table=%s, labels=%s}",
                hasNewlines, hasSellerSection, hasBuyerSection, hasItemsSection, hasTableStructure, fieldLabels
            );
        }
    }
}

