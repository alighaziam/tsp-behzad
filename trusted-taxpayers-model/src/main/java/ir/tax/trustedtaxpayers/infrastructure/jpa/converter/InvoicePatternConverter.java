package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.InvoicePattern;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * مبدل JPA برای InvoicePattern enum
 * تبدیل بین InvoicePattern و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class InvoicePatternConverter implements AttributeConverter<InvoicePattern, Integer> {
    @Override
    public Integer convertToDatabaseColumn(InvoicePattern attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public InvoicePattern convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return InvoicePattern.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid InvoicePattern value in database: " + dbData, e);
        }
    }
}


