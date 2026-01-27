package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/import")
public class ImportSampleController {

    private static final String[] HEADERS = new String[] {
            "invoiceNumber", "issueDate", "sellerTaxId", "buyerTaxId",
            "currency", "totalAmount", "itemName", "itemQty", "itemUnitPrice"
    };

    @Operation(
            summary = "دانلود فایل نمونه ایمپورت",
            description = "فایل نمونه برای CSV/XLSX/JSON برمی‌گرداند تا کاربر ساختار صحیح را ببیند.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "نمونه برگردانده شد", content = @Content),
                    @ApiResponse(responseCode = "400", description = "نوع فایل نامعتبر", content = @Content)
            }
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/sample")
    public ResponseEntity<byte[]> getSample(@RequestParam("type") String type) {
        String t = type == null ? "" : type.trim().toLowerCase();
        return switch (t) {
            case "csv" -> serveClasspath("/samples/invoice_sample.csv", "text/csv; charset=UTF-8", "invoice_sample.csv");
            case "json" -> serveClasspath("/samples/invoice_sample.json", MediaType.APPLICATION_JSON_VALUE, "invoice_sample.json");
            case "xlsx" -> buildXlsxFallbackCsv(); // فعلاً CSV به‌عنوان جایگزین
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        };
    }

    private ResponseEntity<byte[]> serveClasspath(String path, String contentType, String filename) {
        try {
            ClassPathResource resource = new ClassPathResource(path.startsWith("/") ? path.substring(1) : path);
            byte[] bytes = resource.getContentAsByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(contentType));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<byte[]> buildXlsxFallbackCsv() {
        // فعلاً به‌عنوان جایگزین XLSX، همان CSV نمونه را بازمی‌گردانیم
        byte[] bytes;
        try {
            ClassPathResource resource = new ClassPathResource("samples/invoice_sample.csv");
            bytes = resource.getContentAsByteArray();
        } catch (Exception e) {
            // در صورت خطا، CSV inline ساده
            String header = String.join(",", HEADERS);
            String sampleRow = String.join(",",
                    "INV-001","2025-11-16","57490845111","12345678901","IRR","12255","نمونه کالا","2","5000");
            String csv = header + "\n" + sampleRow + "\n";
            bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_sample.xlsx\"");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}


