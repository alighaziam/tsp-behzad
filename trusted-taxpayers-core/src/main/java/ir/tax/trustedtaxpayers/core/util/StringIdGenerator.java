package ir.tax.trustedtaxpayers.core.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * تولید کننده ID String امن
 * استفاده از Base64 URL-safe encoding برای امنیت بیشتر
 */
public class StringIdGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int ID_LENGTH = 24; // 24 character = 18 bytes = 144 bits

    /**
     * تولید یک ID String امن و یکتا
     * @return String ID با طول 24 کاراکتر
     */
    public static String generateId() {
        byte[] randomBytes = new byte[18];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * بررسی معتبر بودن ID
     * @param id ID برای بررسی
     * @return true اگر ID معتبر باشد
     */
    public static boolean isValidId(String id) {
        if (id == null || id.length() != ID_LENGTH) {
            return false;
        }
        try {
            Base64.getUrlDecoder().decode(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

