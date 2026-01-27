package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.enums.ExtensionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter برای تبدیل ExtensionType به String و بالعکس
 */
@Converter(autoApply = true)
public class ExtensionTypeConverter implements AttributeConverter<ExtensionType, String> {
    
    @Override
    public String convertToDatabaseColumn(ExtensionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }
    
    @Override
    public ExtensionType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return ExtensionType.fromValue(dbData);
    }
}


