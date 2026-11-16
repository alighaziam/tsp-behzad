package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * کنترلر ارسال صورتحساب به سامانه مودیان برای مدیریت عملیات همزمان و غیرهمزمان.
 * <p>
 * REST controller orchestrating synchronous/async invoice transmission, status retrieval, and retries.
 */
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoice Transmission", description = "ارسال صورتحساب به سامانه مودیان\n\nراهنمای سریع:\n- Base: /api/invoices\n- ارسال: POST /{invoiceId}/transmit | وضعیت: GET /{invoiceId}/transmission-status\n- Async: POST /{invoiceId}/transmit-async\n- Batch: POST /batch-transmit → {invoiceIds:[UUID...]}\n- Retry: POST /{invoiceId}/retry | Retry همه: POST /retry-all-failed\n")
@PreAuthorize("hasAuthority('ADMIN')")
public class InvoiceTransmissionController {
    
    private final InvoiceTransmissionService transmissionService;
    
    public InvoiceTransmissionController(InvoiceTransmissionService transmissionService) {
        this.transmissionService = transmissionService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
    
    /**
     * ارسال صورتحساب به سامانه مودیان
     * 
     * @param invoiceId شناسه صورتحساب
     * @return نتیجه ارسال
     */
    @PostMapping("/{invoiceId}/transmit")
    @Operation(
        summary = "ارسال صورتحساب به سامانه مودیان",
        description = "صورتحساب را به سامانه مودیان خارجی ارسال کرده و وضعیت ارسال را ثبت می‌کند"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "ارسال با موفقیت انجام شد",
            content = @Content(schema = @Schema(implementation = TransmissionResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "صورتحساب یافت نشد"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "صورتحساب در وضعیت نهایی است و قابل ارسال مجدد نیست"
        )
    })
    public ResponseEntity<TransmissionResponseDto> transmitInvoice(
            @Parameter(description = "شناسه صورتحساب") @PathVariable UUID invoiceId) {
        
        InvoiceTransmissionService.TransmissionResult result = transmissionService.transmitInvoice(invoiceId);
        
        TransmissionResponseDto response = new TransmissionResponseDto(
            result.getInvoiceId(),
            result.getTaxId(),
            result.getStatus(),
            result.getMessage(),
            result.getRejectionCode(),
            result.getRejectionDescription(),
            result.getSentAt()
        );
        
        HttpStatus status = result.isSuccessful() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * دریافت وضعیت ارسال صورتحساب
     * 
     * @param invoiceId شناسه صورتحساب
     * @return وضعیت فعلی ارسال
     */
    @GetMapping("/{invoiceId}/transmission-status")
    @Operation(
        summary = "دریافت وضعیت ارسال صورتحساب",
        description = "وضعیت فعلی ارسال صورتحساب به سامانه مودیان را برمی‌گرداند"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "وضعیت با موفقیت دریافت شد",
            content = @Content(schema = @Schema(implementation = TransmissionStatusDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "صورتحساب یافت نشد"
        )
    })
    public ResponseEntity<TransmissionStatusDto> getTransmissionStatus(
            @Parameter(description = "شناسه صورتحساب") @PathVariable UUID invoiceId) {
        
        InvoiceTransmissionService.TransmissionStatus status = transmissionService.getTransmissionStatus(invoiceId);
        
        TransmissionStatusDto dto = new TransmissionStatusDto(
            status.getInvoiceId(),
            status.getStatus(),
            status.getSentAt(),
            status.getRejectionCode(),
            status.getRejectionDescription()
        );
        
        return ResponseEntity.ok(dto);
    }
    
    /**
     * ارسال Async صورتحساب به سامانه مودیان
     * 
     * @param invoiceId شناسه صورتحساب
     * @return Future که نتیجه ارسال را برمی‌گرداند
     */
    @PostMapping("/{invoiceId}/transmit-async")
    @Operation(
        summary = "ارسال Async صورتحساب",
        description = "صورتحساب را به صورت غیرهمزمان به سامانه مودیان ارسال می‌کند"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "درخواست پذیرفته شد و در حال پردازش است"),
        @ApiResponse(responseCode = "404", description = "صورتحساب یافت نشد")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> transmitInvoiceAsync(
            @Parameter(description = "شناسه صورتحساب") @PathVariable UUID invoiceId) {
        
        return transmissionService.transmitInvoiceAsync(invoiceId)
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("invoiceId", result.getInvoiceId());
                response.put("taxId", result.getTaxId());
                response.put("status", result.getStatus());
                response.put("message", result.getMessage());
                response.put("sentAt", result.getSentAt());
                
                HttpStatus status = result.isSuccessful() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
                return ResponseEntity.status(status).body(response);
            });
    }
    
    /**
     * ارسال Batch چندین صورتحساب
     * 
     * @param request درخواست شامل لیست شناسه‌های صورتحساب
     * @return نتیجه ارسال انبوه
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/batch-transmit")
    @Operation(
        summary = "ارسال Batch صورتحساب‌ها",
        description = "چندین صورتحساب را به صورت همزمان و async ارسال می‌کند"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "درخواست پذیرفته شد"),
        @ApiResponse(responseCode = "400", description = "لیست شناسه‌ها خالی است")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> transmitInvoicesBatch(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = BatchTransmissionRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"invoiceIds\": [\n    \"550e8400-e29b-41d4-a716-446655440000\",\n    \"660e8400-e29b-41d4-a716-446655440000\"\n  ]\n}"
                            )
                    )
            )
            @RequestBody BatchTransmissionRequest request) {
        
        if (request.invoiceIds() == null || request.invoiceIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invoice IDs list cannot be empty");
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(error)
            );
        }
        
        return transmissionService.transmitInvoicesBatchAsync(request.invoiceIds())
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("total", result.getTotal());
                response.put("successful", result.getSuccessful());
                response.put("failed", result.getFailed());
                response.put("results", result.getResults());
                
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            });
    }
    
    /**
     * Retry ارسال برای صورتحساب‌های FAILED یا REJECTED
     * 
     * @param invoiceId شناسه صورتحساب
     * @return نتیجه ارسال مجدد
     */
    @PostMapping("/{invoiceId}/retry")
    @Operation(
        summary = "Retry ارسال صورتحساب ناموفق",
        description = "صورتحساب‌های FAILED یا REJECTED را دوباره ارسال می‌کند"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retry با موفقیت انجام شد"),
        @ApiResponse(responseCode = "404", description = "صورتحساب یافت نشد"),
        @ApiResponse(responseCode = "400", description = "صورتحساب قابل Retry نیست")
    })
    public ResponseEntity<TransmissionResponseDto> retryFailedInvoice(
            @Parameter(description = "شناسه صورتحساب") @PathVariable UUID invoiceId) {
        
        InvoiceTransmissionService.TransmissionResult result = transmissionService.retryFailedInvoice(invoiceId);
        
        TransmissionResponseDto response = new TransmissionResponseDto(
            result.getInvoiceId(),
            result.getTaxId(),
            result.getStatus(),
            result.getMessage(),
            result.getRejectionCode(),
            result.getRejectionDescription(),
            result.getSentAt()
        );
        
        HttpStatus status = result.isSuccessful() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Retry انبوه تمام صورتحساب‌های FAILED یا REJECTED
     * 
     * @return نتیجه Retry انبوه
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/retry-all-failed")
    @Operation(
        summary = "Retry انبوه صورتحساب‌های ناموفق",
        description = "تمام صورتحساب‌های FAILED یا REJECTED را به صورت async دوباره ارسال می‌کند"
    )
    @ApiResponse(responseCode = "202", description = "Retry انبوه شروع شد")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> retryAllFailedInvoices() {
        
        return transmissionService.retryAllFailedInvoicesAsync()
            .thenApply(result -> {
                Map<String, Object> response = new HashMap<>();
                response.put("total", result.getTotal());
                response.put("successful", result.getSuccessful());
                response.put("failed", result.getFailed());
                response.put("results", result.getResults());
                
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            });
    }
    
    /**
     * DTO برای درخواست Batch
     */
    @Schema(description = "درخواست ارسال Batch")
    public record BatchTransmissionRequest(
        @Schema(description = "لیست شناسه‌های صورتحساب") List<UUID> invoiceIds
    ) {}
    
    /**
     * DTO برای پاسخ ارسال صورتحساب
     */
    @Schema(description = "پاسخ ارسال صورتحساب")
    public record TransmissionResponseDto(
        @Schema(description = "شناسه صورتحساب") UUID invoiceId,
        @Schema(description = "شماره مالیاتی صورتحساب") String taxId,
        @Schema(description = "وضعیت ارسال") String status,
        @Schema(description = "پیام ارسال") String message,
        @Schema(description = "کد رد در صورت خطا") String rejectionCode,
        @Schema(description = "توضیحات رد در صورت خطا") String rejectionDescription,
        @Schema(description = "زمان ارسال") java.time.OffsetDateTime sentAt
    ) {}
    
    /**
     * DTO برای وضعیت ارسال صورتحساب
     */
    @Schema(description = "وضعیت ارسال صورتحساب")
    public record TransmissionStatusDto(
        @Schema(description = "شناسه صورتحساب") UUID invoiceId,
        @Schema(description = "وضعیت ارسال") String status,
        @Schema(description = "زمان ارسال") java.time.OffsetDateTime sentAt,
        @Schema(description = "کد رد در صورت خطا") String rejectionCode,
        @Schema(description = "توضیحات رد در صورت خطا") String rejectionDescription
    ) {}
}


