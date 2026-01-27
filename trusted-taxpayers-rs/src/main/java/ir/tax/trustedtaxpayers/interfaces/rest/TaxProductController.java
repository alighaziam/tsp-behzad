package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TaxProductCsvImportService;
import ir.tax.trustedtaxpayers.application.service.TaxProductService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxProductRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت محصولات مالیاتی
 * 
 * <p>REST controller handling tax product operations.</p>
 */
@RestController
@RequestMapping("/api/tax-products")
@Tag(name = "Tax Product Management", description = "مدیریت محصولات مالیاتی")
public class TaxProductController extends BaseController<TaxProductEntity, UUID> {
    
    private final TaxProductCsvImportService csvImportService;
    private final TaxProductService taxProductService;

    public TaxProductController(TaxProductService taxProductService, 
                                TaxProductRepository taxProductRepository,
                                TaxProductCsvImportService csvImportService) {
        super(taxProductService, taxProductRepository);
        this.taxProductService = taxProductService;
        this.csvImportService = csvImportService;
    }

    @Operation(
            summary = "آپلود و import محصولات مالیاتی از فایل CSV",
            description = "دریافت فایل CSV و import محصولات مالیاتی. فایل باید شامل ستون‌های زیر باشد:\n" +
                    "- id: شناسه محصول (TAX_PRODUCT_ID)\n" +
                    "- DescriptionOfID: عنوان محصول (TITLE)\n" +
                    "- Type: نوع محصول به فارسی (مثلاً 'شناسه اختصاصی تولید داخل')\n" +
                    "- Taxable: نوع مشمولیت مالیاتی به فارسی (مثلاً 'مشمول', 'معاف', 'غیر مشمول')\n" +
                    "- RunDate: تاریخ اجرا به صورت شمسی (مثلاً 1403/01/01)\n" +
                    "- Expiration: تاریخ انقضا به صورت شمسی (اختیاری)\n" +
                    "- Vat: نرخ مالیات بر ارزش افزوده (اختیاری)\n" +
                    "- TaxCreateDate: تاریخ ایجاد مالیاتی به صورت شمسی (اختیاری)\n" +
                    "- TaxLastEditDate: تاریخ آخرین ویرایش مالیاتی به صورت شمسی (اختیاری)\n" +
                    "- PricingDescription: توضیحات قیمت‌گذاری (اختیاری)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Import با موفقیت انجام شد",
                            content = @Content(schema = @Schema(implementation = TaxProductCsvImportService.TaxProductImportResult.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "فایل نامعتبر یا خطا در پردازش"
                    )
            }
    )
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaxProductCsvImportService.TaxProductImportResult> importCsv(
            @NotNull @RequestPart("file") MultipartFile file) {
        
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(TaxProductCsvImportService.TaxProductImportResult.failure("فایل خالی است"));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                    .body(TaxProductCsvImportService.TaxProductImportResult.failure("فایل باید از نوع CSV باشد"));
        }

        TaxProductCsvImportService.TaxProductImportResult result = csvImportService.importFromCsv(file);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @Operation(
            summary = "اتوکامپلیت محصولات مالیاتی بر اساس شناسه یا عنوان",
            description = "برای استفاده در جستجوی سریع؛ بیش از ۳۰ نتیجه بازگردانده نمی‌شود.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست کوتاه محصولات مالیاتی برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxProductEntity.class))
                    )
            }
    )
    @GetMapping("/autocomplete")
    public ResponseEntity<List<TaxProductEntity>> autocomplete(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        int max = Math.min(Math.max(limit == null ? 10 : limit, 1), 30);
        int safePage = Math.max(page == null ? 0 : page, 0);
        var results = taxProductService.search(query, max, safePage);
        return ResponseEntity.ok(results);
    }
}

