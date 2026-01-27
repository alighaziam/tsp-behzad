package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.SettlementMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * مبدل JPA برای SettlementMethod enum
 * تبدیل بین SettlementMethod و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class SettlementMethodConverter implements AttributeConverter<SettlementMethod, Integer> {
    @Override
    public Integer convertToDatabaseColumn(SettlementMethod attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public SettlementMethod convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return SettlementMethod.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid SettlementMethod value in database: " + dbData, e);
        }
    }
}


