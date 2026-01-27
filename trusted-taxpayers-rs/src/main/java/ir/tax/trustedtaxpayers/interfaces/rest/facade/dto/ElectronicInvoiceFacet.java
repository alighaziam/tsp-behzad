package ir.tax.trustedtaxpayers.interfaces.rest.facade.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * مجموعه بخش‌های پشتیبانی‌شده توسط فاساد صورتحساب الکترونیکی.
 * <p>
 * Enumeration describing the individual data segments that can be assembled by the
 * electronic invoice facade. Clients pick one or more facets to drive both the data payload
 * and the optional dynamic form schema generation.
 */
@Schema(name = "ElectronicInvoiceFacet", description = "بخش یا سرویس قابل فراخوانی توسط فاساد صورتحساب الکترونیکی")
public enum ElectronicInvoiceFacet {

    /**
     * داده‌های مربوط به {@code TemplateEntity} و DTO متناظر.
     */
    @Schema(description = "دریافت اطلاعات الگو (TemplateEntity)", example = "TEMPLATE")
    TEMPLATE,

    /**
     * قواعد اعتبارسنجی وابسته به قالب انتخاب‌شده.
     */
    @Schema(description = "دریافت قواعد الگو (TemplateRuleEntity)", example = "TEMPLATE_RULES")
    TEMPLATE_RULES,

    /**
     * داده‌های نوع قالب صورتحساب.
     */
    @Schema(description = "دریافت نوع قالب (TemplateTypeEntity)", example = "TEMPLATE_TYPE")
    TEMPLATE_TYPE,

    /**
     * جزئیات سربرگ صورتحساب الکترونیکی شامل اقلام، پرداخت‌ها و وضعیت سیستمی.
     */
    @Schema(description = "دریافت جزئیات سربرگ صورتحساب", example = "HEADER")
    HEADER,

    /**
     * اقلام صورتحساب مرتبط با سربرگ انتخاب‌شده.
     */
    @Schema(description = "دریافت لیست اقلام صورتحساب", example = "ITEMS")
    ITEMS,

    /**
     * وضعیت سیستمی و متادیتای ارسال صورتحساب.
     */
    @Schema(description = "دریافت وضعیت‌های سیستمی صورتحساب", example = "SYSTEM")
    SYSTEM
}



