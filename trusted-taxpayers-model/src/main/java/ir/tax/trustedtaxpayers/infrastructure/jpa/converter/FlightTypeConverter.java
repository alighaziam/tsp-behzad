package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.enums.FlightType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * مبدل JPA برای FlightType enum
 * تبدیل بین FlightType و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class FlightTypeConverter implements AttributeConverter<FlightType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(FlightType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public FlightType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return FlightType.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid FlightType value in database: " + dbData, e);
        }
    }
}


