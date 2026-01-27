package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.ShippingType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter برای تبدیل ShippingType به Integer و بالعکس
 */
@Converter(autoApply = true)
public class ShippingTypeConverter implements AttributeConverter<ShippingType, Integer> {
    
    @Override
    public Integer convertToDatabaseColumn(ShippingType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }
    
    @Override
    public ShippingType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return ShippingType.fromValue(dbData);
    }
}


