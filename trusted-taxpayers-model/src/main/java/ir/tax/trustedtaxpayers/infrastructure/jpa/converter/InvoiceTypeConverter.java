package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.InvoiceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * مبدل JPA برای InvoiceType enum
 * تبدیل بین InvoiceType و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class InvoiceTypeConverter implements AttributeConverter<InvoiceType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(InvoiceType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public InvoiceType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return InvoiceType.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid InvoiceType value in database: " + dbData, e);
        }
    }
}


