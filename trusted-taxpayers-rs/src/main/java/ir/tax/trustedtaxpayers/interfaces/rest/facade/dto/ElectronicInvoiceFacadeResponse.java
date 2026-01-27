package ir.tax.trustedtaxpayers.interfaces.rest.facade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * خروجی فاساد صورتحساب الکترونیکی که داده‌های موردنیاز چند سرویس را تجمیع می‌کند.
 * <p>
 * Response wrapper containing both aggregated data and optional dynamic form schemas for the
 * requested facets. Fields remain {@code null} when a facet was not requested, allowing clients
 * to reason about the payload presence without additional flags.
 */
@Builder
@Schema(name = "ElectronicInvoiceFacadeResponse", description = "خروجی تجمیعی داده‌ها و شِماهای فرم صورتحساب")
public record ElectronicInvoiceFacadeResponse(

        @Schema(description = "اطلاعات الگو (TemplateEntity → TemplateDto)")
        ElectronicInvoiceDtos.TemplateDto template,

        @Schema(description = "اطلاعات نوع الگو (TemplateTypeEntity → TemplateTypeDto)")
        ElectronicInvoiceDtos.TemplateTypeDto templateType,

        @Schema(description = "قواعد الگو در صورت درخواست")
        List<ElectronicInvoiceDtos.TemplateRuleDto> templateRules,

        @Schema(description = "جزئیات کامل سربرگ صورتحساب به همراه اقلام، پرداخت‌ها و وضعیت")
        ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto header,

        @Schema(description = "لیست اقلام صورتحساب")
        List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto> items,

        @Schema(description = "وضعیت و متادیتای سیستمی صورتحساب")
        ElectronicInvoiceDtos.ElectronicInvoiceSystemDto system,

        @Schema(description = "شِمای فرم داینامیک برای موجودیت‌های درخواستی (کلید برابر با نام کلاس)")
        Map<String, FormSchemaDto> formSchemas
) {
}



