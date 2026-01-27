package ir.tax.trustedtaxpayers.core.util;

import java.util.Base64;
import java.util.UUID;

/**
 * تبدیل UUID به String و برعکس برای امنیت بیشتر
 * UUID در دیتابیس ذخیره می‌شود اما در API به صورت String (encoded) نمایش داده می‌شود
 */
public class UuidStringConverter {
    
    /**
     * تبدیل UUID به String (Base64 URL-safe encoding)
     * @param uuid UUID برای تبدیل
     * @return String encoded
     */
    public static String encode(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    /**
     * تبدیل String (encoded) به UUID
     * @param idString String encoded
     * @return UUID decoded
     */
    public static UUID decode(String idString) {
        if (idString == null || idString.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(idString);
            if (bytes.length != 16) {
                throw new IllegalArgumentException("Invalid ID string length");
            }
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (bytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (bytes[i] & 0xff);
            }
            return new UUID(msb, lsb);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID string format: " + idString, e);
        }
    }
    
    /**
     * بررسی معتبر بودن String ID
     * @param idString String ID برای بررسی
     * @return true اگر معتبر باشد
     */
    public static boolean isValid(String idString) {
        if (idString == null || idString.isBlank()) {
            return false;
        }
        try {
            decode(idString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

