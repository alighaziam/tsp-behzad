package ir.tax.trustedtaxpayers.infrastructure.jpa.converter;

import ir.tax.trustedtaxpayers.beans.model.InvoiceSubject;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * مبدل JPA برای InvoiceSubject enum
 * تبدیل بین InvoiceSubject و Integer در دیتابیس
 */
@Converter(autoApply = false)
public class InvoiceSubjectConverter implements AttributeConverter<InvoiceSubject, Integer> {
    private static final Logger log = LoggerFactory.getLogger(InvoiceSubjectConverter.class);

    @Override
    public Integer convertToDatabaseColumn(InvoiceSubject attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public InvoiceSubject convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return InvoiceSubject.fromValue(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("Encountered unknown invoice subject value '{}'; defaulting to null", dbData, e);
            return null;
        }
    }
}


