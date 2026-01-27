package ir.tax.trustedtaxpayers.interfaces.rest.util;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.UuidStringConverter;

import java.util.UUID;

/**
 * Mapper برای تبدیل Entity به DTO و برعکس
 * UUID در Entity به String (encoded) در DTO تبدیل می‌شود
 */
public class EntityDtoMapper {
    
    /**
     * تبدیل UUID به String برای استفاده در DTO
     * @param uuid UUID برای تبدیل
     * @return String encoded
     */
    public static String uuidToString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return UuidStringConverter.encode(uuid);
    }
    
    /**
     * تبدیل String (encoded) به UUID برای استفاده در Entity
     * اگر id شامل '-' باشد (UUID استاندارد)، از UUID.fromString استفاده می‌شود
     * در غیر این صورت (Base62 encoded)، از UuidStringConverter.decode استفاده می‌شود
     * @param idString String encoded یا UUID استاندارد
     * @return UUID decoded
     */
    public static UUID stringToUuid(String idString) {
        if (idString == null || idString.isBlank()) {
            return null;
        }
        // اگر id شامل '-' باشد، UUID استاندارد است
        if (idString.contains("-")) {
            try {
                return UUID.fromString(idString);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("فرمت UUID نامعتبر: " + idString, e);
            }
        }
        // در غیر این صورت، Base62 encoded است
        return UuidStringConverter.decode(idString);
    }
    
    /**
     * دریافت ID String از Entity
     * @param entity Entity با BaseEntity
     * @return String ID
     */
    public static String getIdString(BaseEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return entity.getIdString();
    }
    
    /**
     * تنظیم ID در Entity از String
     * @param entity Entity برای تنظیم ID
     * @param idString String ID
     */
    public static void setIdFromString(BaseEntity entity, String idString) {
        if (entity != null && idString != null && !idString.isBlank()) {
            entity.setIdString(idString);
        }
    }
}

