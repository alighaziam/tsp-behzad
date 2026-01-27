package ir.tax.trustedtaxpayers.application.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.tax.trustedtaxpayers.beans.dto.tax.TaxInvoiceResponse;
import ir.tax.trustedtaxpayers.beans.dto.tax.TspSendWithKeyRequest;
import ir.tax.trustedtaxpayers.beans.dto.tax.TaxpayerKeySendRequest;
import ir.tax.trustedtaxpayers.beans.dto.tax.PreSignedPacketsRequest;
import ir.tax.trustedtaxpayers.biz.TspInvoiceManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * کنترلر REST برای مدیریت ارسال صورت‌حساب توسط شرکت معتمد (TSP)
 * REST Controller for TSP Invoice Management
 * 
 * این Controller شامل API های مختص شرکت‌های معتمد برای ارسال
 * صورت‌حساب با کلید TSP یا کلید مودی است.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tsp/invoices")
@RequiredArgsConstructor
@Tag(name = "TSP Invoices", description = "مدیریت صورت‌حساب توسط شرکت معتمد - TSP Invoice Management APIs")
public class TspInvoiceController {

    private final TspInvoiceManager tspInvoiceManager;

    @GetMapping
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    /**
     * ارسال صورت‌حساب با کلید شرکت معتمد
     * Send invoices with TSP key
     * 
     * در این حالت بستهها، چه در الیه انتقال و چه در الیه محتوا توسط کلید شرکت معتمد امضاء میشوند.
     */
    @PostMapping("/send-with-tsp-key")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(
        summary = "ارسال با کلید شرکت معتمد", 
        description = "Send invoices signed by TSP key (both transfer and content layers signed by TSP)"
    )
    public ResponseEntity<TaxInvoiceResponse> sendWithTspKey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TspSendWithKeyRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"fiscalId\": \"12345678901\",\n  \"invoices\": [\n    {\n      \"header\": {\n        \"taxId\": \"AB12CD34EF56GH78IJ90KL\"\n      },\n      \"items\": [\n        { \"sstid\": \"100200\", \"sstt\": \"Service A\", \"am\": 1, \"fee\": 100000 }\n      ]\n    }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody TspSendWithKeyRequest request) {
        log.info("Received request to send {} invoices with TSP key for fiscal ID: {}", 
                request.getInvoices().size(), request.getFiscalId());

        try {
            AsyncResponseModel response = tspInvoiceManager.sendWithTspKey(
                    request.getInvoices(), 
                    request.getFiscalId()
            );
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Invoices sent successfully with TSP key")
                    .timestamp(LocalDateTime.now())
                    .successCount(response.getResult() != null ? response.getResult().size() : 0)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending invoices with TSP key", e);
            throw e;
        }
    }

    /**
     * ارسال صورت‌حساب با کلید مودی
     * Send invoices with taxpayer key
     * 
     * در این حالت محتوای بسته توسط کلید مودی امضاء و رمز میشود
     * و توسط کلید شرکت معتمد ارسال میشود.
     */
    @PostMapping("/send-with-taxpayer-key")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(
        summary = "ارسال با کلید مودی", 
        description = "Send invoices signed by taxpayer key (content signed by taxpayer, transfer by TSP)"
    )
    public ResponseEntity<TaxInvoiceResponse> sendWithTaxpayerKey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = TaxpayerKeySendRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"fiscalId\": \"12345678901\",\n  \"privateKeyPath\": \"/path/to/private.key\",\n  \"privateKeyPassword\": \"secret\",\n  \"invoices\": [\n    {\n      \"header\": { \"taxId\": \"AB12CD34EF56GH78IJ90KL\" },\n      \"items\": [ { \"sstid\": \"100200\", \"am\": 1, \"fee\": 100000 } ]\n    }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody TaxpayerKeySendRequest request) {
        log.info("Received request to send {} invoices with taxpayer key for fiscal ID: {}", 
                request.getInvoices().size(), request.getFiscalId());

        try {
            AsyncResponseModel response = tspInvoiceManager.sendWithTaxpayerKey(
                    request.getInvoices(),
                    request.getFiscalId(),
                    request.getPrivateKeyPath(),
                    request.getPrivateKeyPassword()
            );
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Invoices sent successfully with taxpayer key")
                    .timestamp(LocalDateTime.now())
                    .successCount(response.getResult() != null ? response.getResult().size() : 0)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending invoices with taxpayer key", e);
            throw e;
        }
    }

    /**
     * ارسال بسته‌های از پیش امضا شده
     * Send pre-signed packets
     * 
     * در این حالت بسته‌ها قبلاً توسط مودی آماده و امضا شده‌اند
     * و فقط توسط TSP ارسال می‌شوند.
     */
    @PostMapping("/send-pre-signed")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(
        summary = "ارسال بسته‌های از پیش امضا شده", 
        description = "Send packets that are already signed by taxpayer (TSP only transfers them)"
    )
    public ResponseEntity<TaxInvoiceResponse> sendPreSignedPackets(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = PreSignedPacketsRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"token\": \"BEARER_ORG_TOKEN\",\n  \"packets\": [\n    { \"content\": \"BASE64_CONTENT\", \"signature\": \"BASE64_SIGNATURE\" }\n  ]\n}"
                            )
                    )
            )
            @Valid @RequestBody PreSignedPacketsRequest request) {
        log.info("Received request to send {} pre-signed packets", request.getPackets().size());

        try {
            AsyncResponseModel response = tspInvoiceManager.sendPreSignedPackets(
                    request.getPackets(),
                    request.getToken()
            );
            
            TaxInvoiceResponse responseDto = TaxInvoiceResponse.builder()
                    .results(response.getResult())
                    .message("Pre-signed packets sent successfully")
                    .timestamp(LocalDateTime.now())
                    .successCount(response.getResult() != null ? response.getResult().size() : 0)
                    .failureCount(0)
                    .build();

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending pre-signed packets", e);
            throw e;
        }
    }
}

