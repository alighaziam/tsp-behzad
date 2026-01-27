package ir.tax.trustedtaxpayers.core.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

@Converter(autoApply = false)
public class RawUuidConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID attribute) {
        if (attribute == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.allocate(16); // safer than wrap(new byte[16])
        bb.putLong(attribute.getMostSignificantBits());
        bb.putLong(attribute.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public UUID convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null; // safe null handling
        }
        if (dbData.length != 16) {
            byte[] padded = new byte[16];
            int copyLen = Math.min(dbData.length, 16);
            // right-align the bytes so missing leading zeros are restored
            System.arraycopy(dbData, dbData.length - copyLen, padded, 16 - copyLen, copyLen);
            dbData = padded;
        }
        ByteBuffer bb = ByteBuffer.wrap(dbData);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}




