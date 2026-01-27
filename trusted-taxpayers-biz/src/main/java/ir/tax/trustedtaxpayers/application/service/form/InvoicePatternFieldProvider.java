package ir.tax.trustedtaxpayers.application.service.form;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;

import java.util.List;

/**
 * Interface برای تولید فیلدهای اختصاصی هر الگوی صورتحساب
 * این interface باعث می‌شود که اگر الگو جدید اضافه شود، فقط یک implementation جدید اضافه شود
 */
public interface InvoicePatternFieldProvider {
    
    /**
     * شماره الگویی که این provider پشتیبانی می‌کند
     */
    Integer getSupportedPattern();
    
    /**
     * فیلدهای اختصاصی Header برای این الگو
     */
    List<FormFieldDto> getHeaderSpecificFields(int baseOrder);
    
    /**
     * فیلدهای اختصاصی Body برای این الگو
     */
    List<FormFieldDto> getBodySpecificFields(int baseOrder);
    
    /**
     * نام فارسی الگو
     */
    String getPatternName();
}

