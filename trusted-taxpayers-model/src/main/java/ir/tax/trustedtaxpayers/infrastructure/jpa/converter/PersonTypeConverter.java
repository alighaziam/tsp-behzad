package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.PersonType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * مبدل JPA برای PersonType enum
 * تبدیل بین PersonType و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class PersonTypeConverter implements AttributeConverter<PersonType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PersonType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public PersonType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return PersonType.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid PersonType value in database: " + dbData, e);
        }
    }
}


