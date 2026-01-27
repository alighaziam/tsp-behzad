package ir.tax.trustedtaxpayers.application.rest;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.tax.trustedtaxpayers.application.service.InvoiceHtmlGeneratorService;
import ir.tax.trustedtaxpayers.application.service.TaxInvoiceBusinessService;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import ir.tax.trustedtaxpayers.biz.TaxInvoiceManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * کنترلر REST برای مدیریت صورت‌حساب‌های مالیاتی
 * REST Controller for Tax Invoice Management
 */
@Slf4j
@RestController
@RequestMapping({"/api/v1/tax/invoices", "/api/tax/invoices"})
@RequiredArgsConstructor
@Tag(name = "Tax Invoices", description = "مدیریت صورت‌حساب‌های مالیاتی - Tax Invoice Management APIs")
public class TaxInvoiceController {

    private final TaxInvoiceManager taxInvoiceManager;
    private final InvoiceHtmlGeneratorService htmlGeneratorService;
    private final TaxInvoiceBusinessService taxInvoiceBusinessService;

    @GetMapping
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    /**
     * ارسال صورت‌حساب‌های مالیاتی
     * Send tax invoices
     * این متد ابتدا invoice را در دیتابیس ذخیره می‌کند و سپس آن را به سامانه مودیان ارسال می‌کند
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "ارسال صورت‌حساب‌های مالیاتی", description = "Save invoices to database and send to tax system")
    @Transactional
    public ResponseEntity<TaxInvoiceResponse> sendInvoices(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TaxInvoiceRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"invoices\": [\n    {\n      \"header\": {\n        \"taxId\": \"AB12CD34EF56GH78IJ90KL\",\n        \"fiscalId\": \"12345678901\"\n      },\n      \"items\": [\n        {\n          \"sstid\": \"100200\",\n          \"sstt\": \"Service A\",\n          \"am\": 1,\n          \"fee\": 100000\n        }\n      ]\n    }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody TaxInvoiceRequest request) {
        log.info("Received request to send {} invoices", request.getInvoices().size());

        try {
            // Save invoices to database with READY_TO_SEND status BEFORE sending
            log.info("Saving {} invoices to database before sending", request.getInvoices().size());
            List<UUID> savedHeaderIds = taxInvoiceBusinessService.saveInvoicesToDatabase(request.getInvoices());
            
            if (savedHeaderIds.isEmpty()) {
                log.warn("No invoices were saved to database");
                throw new RuntimeException("Failed to save invoices to database");
            }
            
            log.info("Successfully saved {} invoices to database with status READY_TO_SEND", savedHeaderIds.size());
            
            // Send invoices to tax system
            AsyncResponseModel response = taxInvoiceManager.sendInvoices(request.getInvoices());
            
            // Update status to SENT for successfully sent invoices
            if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                log.info("Updating {} invoices status to SENT", savedHeaderIds.size());
                taxInvoiceBusinessService.updateInvoiceStatusToSent(savedHeaderIds);
            }
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Invoices saved and sent successfully")
                    .timestamp(LocalDateTime.now())
                    .successCount(response.getResult() != null ? response.getResult().size() : 0)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error processing invoices (save or send)", e);
            throw e;
        }
    }


    /**
     * ارسال صورت‌حساب‌های مالیاتی با wrapper (برای TSP)
     * Send tax invoices with wrapper (for TSP)
     */
    @PostMapping("/send-tsp")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(summary = "ارسال صورت‌حساب با کلید شرکت معتمد", description = "Send tax invoices with TSP key")
    public ResponseEntity<TaxInvoiceResponse> sendInvoicesWithWrapper(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TspInvoiceRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"invoiceWrappers\": [\n    {\n      \"payload\": \"BASE64_PAYLOAD\",\n      \"signature\": \"BASE64_SIGNATURE\"\n    }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody TspInvoiceRequest request) {
        log.info("Received request to send {} invoice wrappers with TSP key", request.getInvoiceWrappers().size());

        try {
            AsyncResponseModel response = taxInvoiceManager.sendInvoicesWithWrapper(request.getInvoiceWrappers());
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Invoice wrappers sent successfully with TSP key")
                    .timestamp(LocalDateTime.now())
                    .successCount(response.getResult() != null ? response.getResult().size() : 0)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending invoice wrappers", e);
            throw e;
        }
    }

    /**
     * ارسال صورت‌حساب نمونه
     * Send sample invoice
     */
    @PostMapping("/send-sample")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "ارسال صورت‌حساب نمونه", description = "Send a sample invoice for testing")
    public ResponseEntity<TaxInvoiceResponse> sendSampleInvoice() {
        log.info("Received request to send sample invoice");

        try {
            AsyncResponseModel response = taxInvoiceManager.sendSampleInvoice();
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Sample invoice sent successfully")
                    .timestamp(LocalDateTime.now())
                    .successCount(1)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending sample invoice", e);
            throw e;
        }
    }

    /**
     * استعلام وضعیت صورت‌حساب با UID
     * Inquiry invoice status by UID
     */
    @PostMapping("/inquiry/uid")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "استعلام با UID", description = "Inquiry invoice status by UID and Fiscal ID")
    public ResponseEntity<InquiryResponse> inquiryByUid(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InquiryByUidRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"uidAndFiscalIds\": [\n    { \"uid\": \"550e8400-e29b-41d4-a716-446655440000\", \"fiscalId\": \"AB12CD34EF56GH78IJ90KL\" }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody InquiryByUidRequest request) {
        log.info("Received inquiry request by UID for {} items", request.getUidAndFiscalIds().size());

        try {
            List<InquiryResultModel> results = taxInvoiceManager.inquiryByUidAndFiscalId(request.getUidAndFiscalIds());
            
            InquiryResponse response = InquiryResponse.builder()
                    .results(results)
                    .message("Inquiry completed successfully")
                    .timestamp(LocalDateTime.now())
                    .count(results != null ? results.size() : 0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inquiring by UID", e);
            throw e;
        }
    }

    /**
     * استعلام وضعیت صورت‌حساب با شماره مرجع
     * Inquiry invoice status by reference number
     */
    @PostMapping("/inquiry/reference")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "استعلام با شماره مرجع", description = "Inquiry invoice status by reference number")
    public ResponseEntity<InquiryResponse> inquiryByReference(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InquiryByReferenceRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"referenceNumbers\": [\n    \"REF-2025-0001\", \"REF-2025-0002\"\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody InquiryByReferenceRequest request) {
        log.info("Received inquiry request by reference number for {} items", request.getReferenceNumbers().size());

        try {
            List<InquiryResultModel> results = taxInvoiceManager.inquiryByReferenceNumber(request.getReferenceNumbers());
            
            InquiryResponse response = InquiryResponse.builder()
                    .results(results)
                    .message("Inquiry completed successfully")
                    .timestamp(LocalDateTime.now())
                    .count(results != null ? results.size() : 0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inquiring by reference number", e);
            throw e;
        }
    }

    /**
     * استعلام صورت‌حساب‌های یک تاریخ خاص
     * Inquiry invoices by specific date
     */
    @PostMapping("/inquiry/date")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "استعلام بر اساس تاریخ", description = "Inquiry invoices by specific Persian date")
    public ResponseEntity<InquiryResponse> inquiryByDate(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InquiryByDateRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"date\": \"14040401\"\n}"
                            )
                    )
            )
            @Valid @RequestBody InquiryByDateRequest request) {
        log.info("Received inquiry request by date: {}", request.getDate());

        try {
            List<InquiryResultModel> results = taxInvoiceManager.inquiryByDate(request.getDate());
            
            InquiryResponse response = InquiryResponse.builder()
                    .results(results)
                    .message("Inquiry by date completed successfully")
                    .timestamp(LocalDateTime.now())
                    .count(results != null ? results.size() : 0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inquiring by date", e);
            throw e;
        }
    }

    /**
     * استعلام صورت‌حساب‌های یک بازه زمانی
     * Inquiry invoices by date range
     */
    @PostMapping("/inquiry/date-range")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "استعلام بر اساس بازه زمانی", description = "Inquiry invoices by Persian date range")
    public ResponseEntity<InquiryResponse> inquiryByDateRange(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = InquiryByDateRangeRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"startDate\": \"14040401\",\n  \"endDate\": \"14040431\"\n}"
                            )
                    )
            )
            @Valid @RequestBody InquiryByDateRangeRequest request) {
        log.info("Received inquiry request by date range: {} to {}", request.getStartDate(), request.getEndDate());

        try {
            List<InquiryResultModel> results = taxInvoiceManager.inquiryByDateRange(
                    request.getStartDate(), 
                    request.getEndDate()
            );
            
            InquiryResponse response = InquiryResponse.builder()
                    .results(results)
                    .message("Inquiry by date range completed successfully")
                    .timestamp(LocalDateTime.now())
                    .count(results != null ? results.size() : 0)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error inquiring by date range", e);
            throw e;
        }
    }

    /**
     * تولید HTML صورتحساب
     * Generate invoice HTML
     */
    @PostMapping("/generate-html")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "تولید HTML صورتحساب", description = "Generate HTML representation of invoice")
    public ResponseEntity<String> generateInvoiceHtml(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GenerateInvoiceHtmlRequest.class)
                    )
            )
            @Valid @RequestBody GenerateInvoiceHtmlRequest request) {
        log.info("Received request to generate HTML for {} invoices", request.getInvoices().size());

        try {
            if (request.getInvoices() == null || request.getInvoices().isEmpty()) {
                return ResponseEntity.badRequest().body("<html><body><p>هیچ صورتحسابی ارسال نشده است</p></body></html>");
            }

            // Generate HTML for the first invoice
            InvoiceDto invoice = request.getInvoices().get(0);
            String html = htmlGeneratorService.generateHtml(invoice, request.getSellerName(), request.getBuyerName());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.setContentDispositionFormData("inline", "invoice.html");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(html);
        } catch (Exception e) {
            log.error("Error generating invoice HTML", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><p>خطا در تولید HTML: " + e.getMessage() + "</p></body></html>");
        }
    }
}

