package ir.tax.trustedtaxpayers.interfaces.rest.facade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.facade.ElectronicInvoiceFacadeService;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.facade.dto.ElectronicInvoiceFacadeRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.facade.dto.ElectronicInvoiceFacadeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * کنترلر فاساد جهت ارائه یک نقطه دسترسی یکپارچه برای سرویس‌های صورتحساب الکترونیکی.
 * <p>
 * Exposes a single facade endpoint so that UI clients can fetch multiple invoice-related datasets
 * and their corresponding form schemas with one round-trip.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/electronic-invoice-facade")
@Tag(name = "Electronic Invoice Facade", description = "تجمیع داده‌های الگو و صورتحساب در یک فراخوانی")
public class ElectronicInvoiceFacadeController {

    private final ElectronicInvoiceFacadeService facadeService;

    /**
     * تجمیع داده‌های درخواستی و شِماهای فرم داینامیک.
     *
     * @param request مشخص‌کننده سرویس‌های موردنیاز
     * @return پاسخ فاساد شامل داده‌های ترکیب‌شده
     */
    @PostMapping("/context")
    @Operation(
            summary = "دریافت داده‌های ترکیبی صورتحساب",
            description = "با استفاده از این سرویس می‌توان چندین بخش (Template, Template Rules, Header, Items, System) را به صورت یکجا فراخوانی کرد. در صورت نیاز، شِمای فرم داینامیک هر بخش نیز برگردانده می‌شود.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "اطلاعات با موفقیت تجمیع شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceFacadeResponse.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "{\n" +
                                                    "  \"template\": {\"code\": \"TEMPLATE_01\", \"name\": \"قالب اصلی\"},\n" +
                                                    "  \"templateRules\": [{\"fieldName\": \"taxId\", \"ruleType\": \"REQUIRED\"}],\n" +
                                                    "  \"header\": {\"taxId\": \"1234567890123456789012\", \"items\": []},\n" +
                                                    "  \"formSchemas\": {\"TemplateEntity\": {\"title\": \"فرم قالب\"}}\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "ورودی نامعتبر است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "منبع درخواستی یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public ResponseEntity<ElectronicInvoiceFacadeResponse> aggregate(
            @Valid @RequestBody ElectronicInvoiceFacadeRequest request) {
        return ResponseEntity.ok(facadeService.aggregate(request));
    }
}



