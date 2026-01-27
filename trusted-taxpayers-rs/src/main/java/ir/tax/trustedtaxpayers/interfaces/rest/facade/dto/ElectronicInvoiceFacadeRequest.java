package ir.tax.trustedtaxpayers.interfaces.rest.facade.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;
import java.util.UUID;

/**
 * ورودی اصلی فاساد صورتحساب الکترونیکی برای تجمیع داده‌ها و تولید فرم‌های داینامیک.
 * <p>
 * Request payload describing which data facets should be assembled and which identifiers must be
 * used to retrieve them. The facade interprets the selected facets and loads the relevant domain
 * objects, optionally enriching the response with dynamic {@code FormSchemaDto} definitions.
 */
@Schema(name = "ElectronicInvoiceFacadeRequest", description = "درخواست تجمیع داده‌های صورتحساب با استفاده از فاساد")
public record ElectronicInvoiceFacadeRequest(

        @NotEmpty(message = "حداقل یک سرویس باید انتخاب شود")
        @Schema(description = "لیست بخش‌های موردنیاز. تعیین می‌کند کدام داده‌ها و فرم‌ها بارگذاری شوند.",
                example = "[\"TEMPLATE\",\"HEADER\",\"ITEMS\"]")
        Set<ElectronicInvoiceFacet> facets,

        @Schema(description = "کد الگو برای بارگذاری Template و Template Rules", example = "TEMPLATE_01")
        String templateCode,

        @Schema(description = "کد نوع الگو برای دریافت TemplateType", example = "TYPE_01")
        String templateTypeCode,

        @Schema(description = "شناسه سربرگ صورتحساب جهت فراخوانی HEADER/ITEMS/SYSTEM", example = "8f1b3c58-4d3d-4b96-a3b7-6a2b9f8cb6a3")
        UUID headerId,

        @Schema(description = "شماره مالیاتی صورتحساب جهت یافتن سربرگ در صورت نبود headerId", example = "1234567890123456789012")
        String taxId,

        @Schema(description = "اگر true باشد تنها قواعد فعال الگو بازگردانده می‌شود")
        boolean onlyActiveRules,

        @Schema(description = "در صورت true بودن، شِمای فرم موجودیت‌های انتخابی نیز تولید می‌شود")
        boolean includeFormSchemas
) {
}



