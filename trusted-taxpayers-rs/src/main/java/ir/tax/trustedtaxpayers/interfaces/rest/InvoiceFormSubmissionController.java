package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.InvoiceFormSubmissionService;
import ir.tax.trustedtaxpayers.application.service.InvoiceFormValidationService;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.beans.dto.tax.ValidationResultDto;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserInvoicePreferenceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserInvoicePreferenceRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.InvoiceSubmissionRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

import static ir.tax.trustedtaxpayers.interfaces.rest.util.CurrentUserResolver.resolveUsername;

/**
 * کنترلر اعتبارسنجی و ارسال داده‌های صورت‌حساب بر اساس الگوهای رسمی.
 * <p>
 * REST controller that validates invoice payloads against pattern schemas and triggers submission flows.
 */
@RestController
@RequestMapping("/api/invoice-forms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Form Submission", description = "اعتبارسنجی و ارسال صورت‌حساب بر اساس الگو\n\nراهنمای سریع:\n- Base: /api/invoice-forms\n- اعتبارسنجی: POST /pattern/{patternNumber}/validate\n- ارسال: POST /pattern/{patternNumber}/submit\n- هدرها: Content-Type: application/json و Authorization: Bearer <JWT>\n")
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
public class InvoiceFormSubmissionController {

    private final InvoiceFormValidationService invoiceFormValidationService;
    private final InvoiceFormSubmissionService invoiceFormSubmissionService;
    private final UserInvoicePreferenceRepository userInvoicePreferenceRepository;
    private final UserRoleService userRoleService;

    @Operation(
            summary = "اعتبارسنجی داده‌های صورت‌حساب بر اساس الگو",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InvoiceSubmissionRequest.class),
                            examples = @ExampleObject(
                                    name = "نمونه بدنه درخواست",
                                    value = "{\n  \"patternNumber\": 2,\n  \"operation\": \"VALIDATE_ONLY\",\n  \"header\": {\n    \"inno\": \"A-12345\",\n    \"indatim\": \"2025-11-05T10:20:30+03:30\",\n    \"inty\": 2,\n    \"ins\": 1,\n    \"inp\": 2,\n    \"tins\": \"12345678901\",\n    \"sbc\": \"0001\",\n    \"tob\": 2,\n    \"bid\": \"1234567890\",\n    \"tinb\": \"98765432109\",\n    \"bbc\": \"0002\",\n    \"bpc\": \"1234567890\",\n    \"bpn\": \"K123456\",\n    \"tprdis\": 300000,\n    \"tdis\": 0,\n    \"tadis\": 300000,\n    \"tvam\": 0,\n    \"tbill\": 300000,\n    \"setm\": 1,\n    \"cap\": 300000,\n    \"insp\": 0,\n    \"tvop\": 0\n  },\n  \"items\": [\n    {\n      \"sstid\": \"100200\",\n      \"sstt\": \"Service A\",\n      \"mu\": \"EA\",\n      \"am\": 2,\n      \"fee\": 150000,\n      \"prdis\": 300000,\n      \"dis\": 0,\n      \"adis\": 300000,\n      \"vra\": 0,\n      \"vam\": 0,\n      \"cut\": \"USD\",\n      \"cfee\": 20,\n      \"exr\": 500000,\n      \"cpr\": 480000,\n      \"vba\": 0\n    }\n  ]\n}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتیجه اعتبارسنجی",
                            content = @Content(schema = @Schema(implementation = ValidationResultDto.class),
                                    examples = @ExampleObject(
                                            value = "{\n  \"valid\": false,\n  \"errors\": [ { \"fieldKey\": \"inno\", \"code\": \"REQUIRED\", \"message\": \"پر کردن این فیلد الزامی است\" } ]\n}"))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "بدنه درخواست نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/pattern/{patternNumber}/validate")
    public ResponseEntity<ValidationResultDto> validate(@PathVariable("patternNumber") Integer patternNumber,
                                                        @RequestBody InvoiceSubmissionRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getPatternNumber() == null) {
            request.setPatternNumber(patternNumber);
        }
        if (!patternNumber.equals(request.getPatternNumber())) {
            return ResponseEntity.badRequest().build();
        }
        ValidationResultDto result = invoiceFormValidationService.validate(
                request.getPatternNumber(), request.getHeader(), request.getItems());
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "ارسال صورت‌حساب پس از اعتبارسنجی",
            description = "ابتدا اعتبارسنجی انجام می‌شود؛ در صورت صحت، پاسخ OK برمی‌گردد. (اتصال ذخیره‌سازی/انتقال می‌تواند اینجا فراخوانی شود)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InvoiceSubmissionRequest.class),
                            examples = @ExampleObject(
                                    name = "نمونه بدنه درخواست",
                                    value = "{\n  \"patternNumber\": 2,\n  \"operation\": \"SUBMIT\",\n  \"header\": {\n    \"inno\": \"A-12345\",\n    \"indatim\": \"2025-11-05T10:20:30+03:30\",\n    \"inty\": 2,\n    \"ins\": 1,\n    \"inp\": 2,\n    \"tins\": \"12345678901\",\n    \"sbc\": \"0001\",\n    \"tob\": 2,\n    \"bid\": \"1234567890\",\n    \"tinb\": \"98765432109\",\n    \"bbc\": \"0002\",\n    \"bpc\": \"1234567890\",\n    \"bpn\": \"K123456\",\n    \"tprdis\": 300000,\n    \"tdis\": 0,\n    \"tadis\": 300000,\n    \"tvam\": 0,\n    \"tbill\": 300000,\n    \"setm\": 1,\n    \"cap\": 300000,\n    \"insp\": 0,\n    \"tvop\": 0,\n    \"irtaxid\": \"AB12CD34EF56GH78IJ90KL\"\n  },\n  \"items\": [\n    {\n      \"sstid\": \"100200\",\n      \"sstt\": \"Service A\",\n      \"mu\": \"EA\",\n      \"am\": 2,\n      \"fee\": 150000,\n      \"prdis\": 300000,\n      \"dis\": 0,\n      \"adis\": 300000,\n      \"vra\": 0,\n      \"vam\": 0,\n      \"cut\": \"USD\",\n      \"cfee\": 20,\n      \"exr\": 500000,\n      \"cpr\": 480000,\n      \"vba\": 0\n    }\n  ]\n}"
                            )
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "ثبت/ارسال موفق",
                            content = @Content(schema = @Schema(implementation = java.util.Map.class),
                                    examples = @ExampleObject(
                                            value = "{\n  \"headerId\": \"8f1b3c58-4d3d-4b96-a3b7-6a2b9f8cb6a3\",\n  \"taxId\": \"AB12CD34EF56GH78IJ90KL\"\n}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "اعتبارسنجی ناموفق", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping("/pattern/{patternNumber}/submit")
    public ResponseEntity<?> submit(@PathVariable("patternNumber") Integer patternNumber,
                                    @RequestBody InvoiceSubmissionRequest request,
                                    HttpServletRequest httpRequest) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getPatternNumber() == null) {
            request.setPatternNumber(patternNumber);
        }
        if (!patternNumber.equals(request.getPatternNumber())) {
            return ResponseEntity.badRequest().build();
        }

        ValidationResultDto result = invoiceFormValidationService.validate(
                request.getPatternNumber(), request.getHeader(), request.getItems());
        if (!result.isValid()) {
            return ResponseEntity.badRequest().body(result);
        }

        var submitResult = invoiceFormSubmissionService.submit(
                request.getPatternNumber(), request.getHeader(), request.getItems());

        updateUserInvoicePreferences(httpRequest, submitResult);

        return ResponseEntity.ok(java.util.Map.of(
                "headerId", submitResult.headerId,
                "taxId", submitResult.taxId
        ));
    }

    @Operation(
            summary = "ذخیره صورتحساب به عنوان پیش‌نویس",
            description = "صورتحساب را بدون validation کامل به عنوان پیش‌نویس ذخیره می‌کند",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InvoiceSubmissionRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "پیش‌نویس با موفقیت ذخیره شد",
                            content = @Content(schema = @Schema(implementation = java.util.Map.class),
                                    examples = @ExampleObject(
                                            value = "{\n  \"headerId\": \"8f1b3c58-4d3d-4b96-a3b7-6a2b9f8cb6a3\",\n  \"taxId\": \"AB12CD34EF56GH78IJ90KL\"\n}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "درخواست نامعتبر", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PostMapping("/pattern/{patternNumber}/save-draft")
    public ResponseEntity<?> saveDraft(@PathVariable("patternNumber") Integer patternNumber,
                                      @RequestBody InvoiceSubmissionRequest request,
                                      HttpServletRequest httpRequest) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        if (request.getPatternNumber() == null) {
            request.setPatternNumber(patternNumber);
        }
        if (!patternNumber.equals(request.getPatternNumber())) {
            return ResponseEntity.badRequest().build();
        }

        // برای پیش‌نویس، validation کامل انجام نمی‌دهیم
        var submitResult = invoiceFormSubmissionService.saveDraft(
                request.getPatternNumber(), request.getHeader(), request.getItems());

        updateUserInvoicePreferences(httpRequest, submitResult);

        return ResponseEntity.ok(java.util.Map.of(
                "headerId", submitResult.headerId,
                "taxId", submitResult.taxId,
                "message", "پیش‌نویس با موفقیت ذخیره شد"
        ));
    }

    private void updateUserInvoicePreferences(HttpServletRequest httpRequest,
                                              InvoiceFormSubmissionService.SubmitResult submitResult) {
        if (submitResult == null || submitResult.sellerTaxpayerId == null) {
            return;
        }

        String username = resolveUsername(httpRequest);
        if (username == null || username.isBlank()) {
            return;
        }

        Optional<UUID> userIdOpt = userRoleService.findUserIdByUsername(username);
        if (userIdOpt.isEmpty()) {
            return;
        }

        UUID userId = userIdOpt.get();
        UUID taxpayerId = submitResult.sellerTaxpayerId;

        var preference = userInvoicePreferenceRepository
                .findByUserIdAndTaxpayerId(userId, taxpayerId)
                .orElseGet(() -> UserInvoicePreferenceEntity.builder()
                        .userId(userId)
                        .taxpayerId(taxpayerId)
                        .firstLoginCompleted(false)
                        .build());

        if (submitResult.templateCode != null) {
            preference.setDefaultTemplateCode(submitResult.templateCode);
            preference.setLastUsedTemplateCode(submitResult.templateCode);
        }
        if (submitResult.templateTypeCode != null) {
            preference.setDefaultTemplateTypeCode(submitResult.templateTypeCode);
        }
        if (submitResult.patternNumber != null) {
            preference.setDefaultPatternNumber(submitResult.patternNumber);
        }
        if (submitResult.invoiceSubject != null) {
            preference.setDefaultIns(submitResult.invoiceSubject);
        }
        preference.setFirstLoginCompleted(true);

        userInvoicePreferenceRepository.save(preference);
    }
}


