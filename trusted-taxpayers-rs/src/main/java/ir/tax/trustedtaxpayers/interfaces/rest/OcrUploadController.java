package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.DraftInvoiceService;
import ir.tax.trustedtaxpayers.biz.service.OcrService;
import ir.tax.trustedtaxpayers.biz.service.ocr.OcrMetricsService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/ocr", "/api/v1/ocr"})
@Tag(name = "OCR Upload", description = "آپلود تصویر (PNG/JPG/PDF) برای OCR و استخراج داده‌های صورتحساب")
@Validated
@Slf4j
public class OcrUploadController {

    private static final List<String> ALLOWED_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.APPLICATION_PDF_VALUE
    );

    // e.g. 10MB
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    private final OcrService ocrService;
    private final OcrMetricsService metricsService;
    private final DraftInvoiceService draftService;
    
    public OcrUploadController(OcrService ocrService, 
                               OcrMetricsService metricsService,
                               DraftInvoiceService draftService) {
        this.ocrService = ocrService;
        this.metricsService = metricsService;
        this.draftService = draftService;
    }

    @Operation(
            summary = "آپلود تصویر صورتحساب برای OCR",
            description = """
                    ورودی: Multipart file با کلید "file"
                    فرمت‌های مجاز: image/jpeg, image/png, application/pdf
                    سقف حجم: 10MB
                    خروجی: نتیجه استاب OCR + اسکلت داده نرمال‌شده
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
                    @ApiResponse(responseCode = "400", description = "نوع/حجم نامعتبر", content = @Content),
                    @ApiResponse(responseCode = "413", description = "فایل بیش از حد بزرگ", content = @Content),
            }
    )
    @PostMapping(
            path = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> upload(
            @NotNull @RequestPart("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "فایل خالی است"));
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "حجم فایل بیشتر از حد مجاز است"));
        }
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        // اگر Content-Type null است یا نامعتبر است، از پسوند فایل استفاده کن
        if (contentType == null || contentType.isBlank()) {
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".png")) {
                    contentType = MediaType.IMAGE_PNG_VALUE;
                } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                    contentType = MediaType.IMAGE_JPEG_VALUE;
                } else if (lowerFilename.endsWith(".pdf")) {
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                }
                log.debug("Content-Type was null, inferred from filename: {} -> {}", filename, contentType);
            }
        }
        
        // بررسی نهایی
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            // بررسی مجدد بر اساس پسوند فایل
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".png") || lowerFilename.endsWith(".jpg") 
                        || lowerFilename.endsWith(".jpeg") || lowerFilename.endsWith(".pdf")) {
                    log.warn("Content-Type mismatch: contentType={}, filename={}, but allowing based on extension", 
                            contentType, filename);
                    // اجازه می‌دهیم ادامه دهد - providerها خودشان بررسی می‌کنند
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "نوع فایل نامعتبر است. فقط image/png، image/jpeg و application/pdf مجاز است",
                            "receivedContentType", contentType != null ? contentType : "null",
                            "filename", filename != null ? filename : "unknown"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "نوع فایل نامعتبر است. فقط image/png، image/jpeg و application/pdf مجاز است",
                        "receivedContentType", contentType != null ? contentType : "null"
                ));
            }
        }

        // TODO: optional antivirus/malware scan hook

        try {
            Map<String, Object> result = ocrService.parseInvoiceImage(file);
            if (result == null) {
                log.error("OCR service returned null result");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "خطا در پردازش OCR"));
            }

            // Persist as draft and return draftId alongside result
            Map<String, Object> parsed = (Map<String, Object>) result.getOrDefault("parsed", Map.of());
            if (parsed == null) {
                parsed = Map.of();
            }
            
            Object ocrObj = result.getOrDefault("ocr", Map.of());
            String textPreview = "";
            if (ocrObj instanceof Map) {
                textPreview = (String) ((Map<String, Object>) ocrObj).getOrDefault("textPreview", "");
            }
            if (textPreview == null) {
                textPreview = "";
            }
            
            var draft = draftService.create(null, "OCR", parsed);
            if (draft != null && draft.getId() != null) {
                result.put("draftId", draft.getId());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing OCR upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "خطا در پردازش OCR: " + (e.getMessage() != null ? e.getMessage() : "خطای نامشخص")));
        }
    }

    @Operation(
            summary = "استخراج متن خام از فایل نمونه (برای تحلیل ساختار)",
            description = "استخراج متن خام از فایل نمونه بدون پردازش برای تحلیل ساختار واقعی فاکتور",
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
                    @ApiResponse(responseCode = "404", description = "فایل یافت نشد", content = @Content),
            }
    )
    @GetMapping("/extract-text/{filename}")
    public ResponseEntity<Map<String, Object>> extractRawText(@PathVariable String filename) {
        try {
            // Sanitize filename
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body(Map.of("error", "نام فایل نامعتبر است"));
            }
            
            String resourcePath = "document/ocr-sample/" + filename;
            Resource resource = new ClassPathResource(resourcePath);
            
            if (!resource.exists()) {
                log.warn("Sample file not found: {}", resourcePath);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Extracting raw text from sample file: {}", filename);
            MultipartFile multipartFile = new ResourceMultipartFile(resource, filename);
            
            // Extract text using OCR service but return raw text
            Map<String, Object> ocrResult = ocrService.parseInvoiceImage(multipartFile);
            
            Map<String, Object> result = new HashMap<>();
            result.put("filename", filename);
            
            if (ocrResult != null) {
                Object ocrObj = ocrResult.getOrDefault("ocr", Map.of());
                if (ocrObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ocrMap = (Map<String, Object>) ocrObj;
                    // Try to get full text first, fallback to preview
                    String fullText = (String) ocrMap.getOrDefault("fullText", "");
                    if (fullText == null || fullText.isBlank()) {
                        fullText = (String) ocrMap.getOrDefault("textPreview", "");
                    }
                    result.put("rawText", fullText);
                    result.put("textLength", fullText != null ? fullText.length() : 0);
                    result.put("textPreview", fullText != null && fullText.length() > 1000 ? fullText.substring(0, 1000) + "..." : fullText);
                }
                
                Map<String, Object> parsed = (Map<String, Object>) ocrResult.getOrDefault("parsed", Map.of());
                result.put("extractedFields", parsed);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error extracting text from sample file: " + filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "خطا در استخراج متن: " + (e.getMessage() != null ? e.getMessage() : "خطای نامشخص")));
        }
    }

    @Operation(
            summary = "تحلیل و بررسی ساختار فایل‌های نمونه",
            description = "تحلیل ساختار واقعی فایل‌های نمونه و مقایسه با الگوهای استخراج",
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
            }
    )
    @GetMapping("/analyze-samples")
    public ResponseEntity<Map<String, Object>> analyzeSamples() {
        Map<String, Object> result = new HashMap<>();
        List<String> sampleFiles = List.of(
            "invoice (87).pdf",
            "invoice (88).pdf",
            "invoice (89).pdf",
            "invoice (90).pdf"
        );
        
        List<Map<String, Object>> analyses = new java.util.ArrayList<>();
        int totalProcessed = 0;
        int totalSuccess = 0;
        
        for (String filename : sampleFiles) {
            try {
                String resourcePath = "document/ocr-sample/" + filename;
                Resource resource = new ClassPathResource(resourcePath);
                
                if (!resource.exists()) {
                    log.warn("Sample file not found: {}", resourcePath);
                    continue;
                }
                
                totalProcessed++;
                log.info("Analyzing sample file: {}", filename);
                MultipartFile multipartFile = new ResourceMultipartFile(resource, filename);
                
                // Extract and analyze
                Map<String, Object> ocrResult = ocrService.parseInvoiceImage(multipartFile);
                
                if (ocrResult != null) {
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("filename", filename);
                    analysis.put("status", "success");
                    
                    // Get raw text (prefer full text over preview)
                    Object ocrObj = ocrResult.getOrDefault("ocr", Map.of());
                    String rawText = "";
                    if (ocrObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> ocrMap = (Map<String, Object>) ocrObj;
                        rawText = (String) ocrMap.getOrDefault("fullText", "");
                        if (rawText == null || rawText.isBlank()) {
                            rawText = (String) ocrMap.getOrDefault("textPreview", "");
                        }
                    }
                    analysis.put("textLength", rawText != null ? rawText.length() : 0);
                    analysis.put("textPreview", rawText != null && rawText.length() > 500 ? rawText.substring(0, 500) + "..." : rawText);
                    
                    // Get extracted fields
                    Map<String, Object> parsed = (Map<String, Object>) ocrResult.getOrDefault("parsed", Map.of());
                    analysis.put("extractedFields", parsed);
                    
                    // Analyze structure
                    Map<String, Object> structure = analyzeTextStructure(rawText);
                    analysis.put("structure", structure);
                    
                    // Check extraction quality
                    Map<String, Object> quality = checkExtractionQuality(parsed, rawText);
                    analysis.put("quality", quality);
                    
                    analyses.add(analysis);
                    totalSuccess++;
                } else {
                    Map<String, Object> analysis = new HashMap<>();
                    analysis.put("filename", filename);
                    analysis.put("status", "error");
                    analysis.put("error", "OCR service returned null");
                    analyses.add(analysis);
                }
            } catch (Exception e) {
                log.error("Error analyzing sample file: " + filename, e);
                Map<String, Object> analysis = new HashMap<>();
                analysis.put("filename", filename);
                analysis.put("status", "error");
                analysis.put("error", e.getMessage() != null ? e.getMessage() : "خطای نامشخص");
                analyses.add(analysis);
            }
        }
        
        result.put("analyses", analyses);
        result.put("totalFiles", sampleFiles.size());
        result.put("processed", totalProcessed);
        result.put("successful", totalSuccess);
        result.put("failed", totalProcessed - totalSuccess);
        
        return ResponseEntity.ok(result);
    }
    
    private Map<String, Object> analyzeTextStructure(String text) {
        Map<String, Object> structure = new HashMap<>();
        if (text == null || text.isBlank()) {
            return structure;
        }
        
        // Basic structure analysis
        structure.put("hasNewlines", text.contains("\n") || text.contains("\r"));
        structure.put("hasTabs", text.contains("\t"));
        structure.put("hasColons", text.contains(":"));
        structure.put("hasDashes", text.contains("-") || text.contains("—") || text.contains("–"));
        
        // Section detection
        structure.put("hasSellerSection", text.contains("فروشنده") || text.toLowerCase().contains("seller"));
        structure.put("hasBuyerSection", text.contains("خریدار") || text.toLowerCase().contains("buyer"));
        structure.put("hasItemsSection", text.contains("اقلام") || text.contains("کالا") || text.contains("خدمت") || 
                      text.toLowerCase().contains("items"));
        structure.put("hasTotalSection", text.contains("جمع") || text.contains("کل") || text.contains("مبلغ") || 
                      text.toLowerCase().contains("total"));
        
        // Pattern detection
        structure.put("hasPatternNumber", text.matches(".*(?:الگو|INP|inp|pattern).*\\d+.*"));
        structure.put("hasInvoiceNumber", text.matches(".*(?:شماره|number|no).*(?:فاکتور|صورتحساب|invoice).*"));
        structure.put("hasDate", text.matches(".*\\d{4}[\\/\\-]\\d{1,2}[\\/\\-]\\d{1,2}.*"));
        
        // Count lines
        String[] lines = text.split("[\r\n]+");
        structure.put("lineCount", lines.length);
        
        // Count numbers (potential items/amounts)
        long numberCount = java.util.regex.Pattern.compile("\\d+").matcher(text).results().count();
        structure.put("numberCount", numberCount);
        
        return structure;
    }
    
    private Map<String, Object> checkExtractionQuality(Map<String, Object> extracted, String rawText) {
        Map<String, Object> quality = new HashMap<>();
        
        // Check which fields were extracted
        boolean hasSellerName = extracted.containsKey("sellerName") && extracted.get("sellerName") != null;
        boolean hasSellerTaxId = extracted.containsKey("sellerTaxId") && extracted.get("sellerTaxId") != null;
        boolean hasBuyerName = extracted.containsKey("buyerName") && extracted.get("buyerName") != null;
        boolean hasBuyerTaxId = extracted.containsKey("buyerTaxId") && extracted.get("buyerTaxId") != null;
        boolean hasInvoiceNumber = extracted.containsKey("invoiceNumber") && extracted.get("invoiceNumber") != null;
        boolean hasIssueDate = extracted.containsKey("issueDate") && extracted.get("issueDate") != null;
        boolean hasTotalAmount = extracted.containsKey("totalAmount") && extracted.get("totalAmount") != null;
        boolean hasPattern = extracted.containsKey("pattern") && extracted.get("pattern") != null;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) extracted.get("items");
        boolean hasItems = items != null && !items.isEmpty();
        
        quality.put("hasSellerName", hasSellerName);
        quality.put("hasSellerTaxId", hasSellerTaxId);
        quality.put("hasBuyerName", hasBuyerName);
        quality.put("hasBuyerTaxId", hasBuyerTaxId);
        quality.put("hasInvoiceNumber", hasInvoiceNumber);
        quality.put("hasIssueDate", hasIssueDate);
        quality.put("hasTotalAmount", hasTotalAmount);
        quality.put("hasPattern", hasPattern);
        quality.put("hasItems", hasItems);
        quality.put("itemCount", hasItems ? items.size() : 0);
        
        // Calculate extraction score
        int score = 0;
        int maxScore = 9; // Total number of fields to check
        if (hasSellerName) score++;
        if (hasSellerTaxId) score++;
        if (hasBuyerName) score++;
        if (hasBuyerTaxId) score++;
        if (hasInvoiceNumber) score++;
        if (hasIssueDate) score++;
        if (hasTotalAmount) score++;
        if (hasPattern) score++;
        if (hasItems) score++;
        
        quality.put("extractionScore", score);
        quality.put("maxScore", maxScore);
        quality.put("extractionRate", (double) score / maxScore * 100);
        
        // Check if text contains expected fields but weren't extracted
        List<String> missingFields = new java.util.ArrayList<>();
        if (!hasSellerName && (rawText.contains("فروشنده") || rawText.toLowerCase().contains("seller"))) {
            missingFields.add("sellerName");
        }
        if (!hasBuyerName && (rawText.contains("خریدار") || rawText.toLowerCase().contains("buyer"))) {
            missingFields.add("buyerName");
        }
        if (!hasInvoiceNumber && (rawText.contains("شماره") || rawText.contains("فاکتور") || rawText.toLowerCase().contains("invoice"))) {
            missingFields.add("invoiceNumber");
        }
        if (!hasIssueDate && rawText.matches(".*\\d{4}[\\/\\-]\\d{1,2}[\\/\\-]\\d{1,2}.*")) {
            missingFields.add("issueDate");
        }
        if (!hasItems && (rawText.contains("اقلام") || rawText.contains("کالا") || rawText.contains("خدمت"))) {
            missingFields.add("items");
        }
        
        quality.put("missingFields", missingFields);
        
        return quality;
    }

    @Operation(
            summary = "لیست فایل‌های نمونه OCR",
            description = "دریافت لیست فایل‌های نمونه موجود در resources/document/ocr-sample",
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
            }
    )
    @GetMapping("/test-samples")
    public ResponseEntity<Map<String, Object>> listSampleFiles() {
        Map<String, Object> result = new HashMap<>();
        List<String> sampleFiles = List.of(
            "invoice (87).pdf",
            "invoice (88).pdf",
            "invoice (89).pdf",
            "invoice (90).pdf"
        );
        
        // Verify which files actually exist
        List<String> existingFiles = new java.util.ArrayList<>();
        for (String filename : sampleFiles) {
            String resourcePath = "document/ocr-sample/" + filename;
            Resource resource = new ClassPathResource(resourcePath);
            if (resource.exists()) {
                existingFiles.add(filename);
            }
        }
        
        result.put("availableSamples", existingFiles);
        result.put("totalCount", existingFiles.size());
        result.put("endpoint", "/api/ocr/test-sample/{filename}");
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "تست پردازش فایل‌های نمونه OCR",
            description = """
                    پردازش فایل‌های نمونه موجود در resources/document/ocr-sample
                    فایل‌های موجود: invoice (87).pdf, invoice (88).pdf, invoice (89).pdf, invoice (90).pdf
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
                    @ApiResponse(responseCode = "404", description = "فایل یافت نشد", content = @Content),
            }
    )
    @GetMapping("/test-sample/{filename}")
    public ResponseEntity<Map<String, Object>> testSampleFile(@PathVariable String filename) {
        try {
            // Sanitize filename to prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest().body(Map.of("error", "نام فایل نامعتبر است"));
            }
            
            String resourcePath = "document/ocr-sample/" + filename;
            Resource resource = new ClassPathResource(resourcePath);
            
            if (!resource.exists()) {
                log.warn("Sample file not found: {}", resourcePath);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Processing sample file: {}", filename);
            
            // Convert Resource to MultipartFile-like object
            // We'll create a simple implementation
            MultipartFile multipartFile = new ResourceMultipartFile(resource, filename);
            
            Map<String, Object> result = ocrService.parseInvoiceImage(multipartFile);
            if (result == null) {
                log.error("OCR service returned null result for sample file: {}", filename);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "خطا در پردازش OCR"));
            }
            
            // Persist as draft
            Map<String, Object> parsed = (Map<String, Object>) result.getOrDefault("parsed", Map.of());
            if (parsed == null) {
                parsed = Map.of();
            }
            
            Object ocrObj = result.getOrDefault("ocr", Map.of());
            String textPreview = "";
            if (ocrObj instanceof Map) {
                textPreview = (String) ((Map<String, Object>) ocrObj).getOrDefault("textPreview", "");
            }
            if (textPreview == null) {
                textPreview = "";
            }
            
            var draft = draftService.create(null, "OCR-Sample: " + filename, parsed);
            if (draft != null && draft.getId() != null) {
                result.put("draftId", draft.getId());
            }
            
            result.put("sampleFile", filename);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing sample file: " + filename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "خطا در پردازش فایل نمونه: " + (e.getMessage() != null ? e.getMessage() : "خطای نامشخص")));
        }
    }

    @Operation(
            summary = "پردازش همه فایل‌های نمونه OCR",
            description = "پردازش همه فایل‌های نمونه موجود به صورت batch",
            responses = {
                    @ApiResponse(responseCode = "200", description = "موفق", content = @Content),
            }
    )
    @GetMapping("/test-all-samples")
    public ResponseEntity<Map<String, Object>> testAllSampleFiles() {
        Map<String, Object> result = new HashMap<>();
        List<String> sampleFiles = List.of(
            "invoice (87).pdf",
            "invoice (88).pdf",
            "invoice (89).pdf",
            "invoice (90).pdf"
        );
        
        List<Map<String, Object>> results = new java.util.ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (String filename : sampleFiles) {
            try {
                String resourcePath = "document/ocr-sample/" + filename;
                Resource resource = new ClassPathResource(resourcePath);
                
                if (!resource.exists()) {
                    log.warn("Sample file not found: {}", resourcePath);
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", filename);
                    fileResult.put("status", "not_found");
                    fileResult.put("error", "فایل یافت نشد");
                    results.add(fileResult);
                    failureCount++;
                    continue;
                }
                
                log.info("Processing sample file: {}", filename);
                MultipartFile multipartFile = new ResourceMultipartFile(resource, filename);
                Map<String, Object> ocrResult = ocrService.parseInvoiceImage(multipartFile);
                
                if (ocrResult != null) {
                    Map<String, Object> parsed = (Map<String, Object>) ocrResult.getOrDefault("parsed", Map.of());
                    Object ocrObj = ocrResult.getOrDefault("ocr", Map.of());
                    String textPreview = "";
                    if (ocrObj instanceof Map) {
                        textPreview = (String) ((Map<String, Object>) ocrObj).getOrDefault("textPreview", "");
                    }
                    
                    var draft = draftService.create(null, "OCR-Sample: " + filename, parsed);
                    if (draft != null && draft.getId() != null) {
                        ocrResult.put("draftId", draft.getId());
                    }
                    
                    ocrResult.put("filename", filename);
                    ocrResult.put("status", "success");
                    results.add(ocrResult);
                    successCount++;
                } else {
                    Map<String, Object> fileResult = new HashMap<>();
                    fileResult.put("filename", filename);
                    fileResult.put("status", "error");
                    fileResult.put("error", "OCR service returned null");
                    results.add(fileResult);
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("Error processing sample file: " + filename, e);
                Map<String, Object> fileResult = new HashMap<>();
                fileResult.put("filename", filename);
                fileResult.put("status", "error");
                fileResult.put("error", e.getMessage() != null ? e.getMessage() : "خطای نامشخص");
                results.add(fileResult);
                failureCount++;
            }
        }
        
        result.put("results", results);
        result.put("totalFiles", sampleFiles.size());
        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Simple MultipartFile implementation for Resource
     */
    private static class ResourceMultipartFile implements MultipartFile {
        private final Resource resource;
        private final String filename;
        
        public ResourceMultipartFile(Resource resource, String filename) {
            this.resource = resource;
            this.filename = filename;
        }
        
        @Override
        public String getName() {
            return "file";
        }
        
        @Override
        public String getOriginalFilename() {
            return filename;
        }
        
        @Override
        public String getContentType() {
            if (filename.toLowerCase().endsWith(".pdf")) {
                return MediaType.APPLICATION_PDF_VALUE;
            } else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                return MediaType.IMAGE_JPEG_VALUE;
            } else if (filename.toLowerCase().endsWith(".png")) {
                return MediaType.IMAGE_PNG_VALUE;
            }
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        
        @Override
        public boolean isEmpty() {
            try {
                return resource.contentLength() == 0;
            } catch (IOException e) {
                return true;
            }
        }
        
        @Override
        public long getSize() {
            try {
                return resource.contentLength();
            } catch (IOException e) {
                return 0;
            }
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return resource.getInputStream().readAllBytes();
        }
        
        @Override
        public java.io.InputStream getInputStream() throws IOException {
            return resource.getInputStream();
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (var input = resource.getInputStream();
                 var output = new java.io.FileOutputStream(dest)) {
                input.transferTo(output);
            }
        }
    }
    
    /**
     * دریافت آمار OCR
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get OCR metrics", description = "Returns statistics about OCR performance")
    public ResponseEntity<OcrMetricsService.OcrMetrics> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }
}


