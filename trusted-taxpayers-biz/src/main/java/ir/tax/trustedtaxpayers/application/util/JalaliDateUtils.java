package ir.tax.trustedtaxpayers.application.util;

import java.time.LocalDate;

public final class JalaliDateUtils {
    private JalaliDateUtils() {}

    public static LocalDate jalaliToGregorian(int jy, int jm, int jd) {
        int[] g = toGregorian(jy, jm, jd);
        return LocalDate.of(g[0], g[1], g[2]);
    }

    public static LocalDate parseJalaliYyyyMmDd(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            throw new IllegalArgumentException("Invalid Jalali date format. Expected yyyyMMdd");
        }
        int jy = Integer.parseInt(yyyymmdd.substring(0, 4));
        int jm = Integer.parseInt(yyyymmdd.substring(4, 6));
        int jd = Integer.parseInt(yyyymmdd.substring(6, 8));
        return jalaliToGregorian(jy, jm, jd);
    }

    /**
     * Parse Jalali date from various formats: yyyy/MM/dd, yyyy-MM-dd, yyyyMMdd
     */
    public static LocalDate parseJalaliDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String normalized = dateStr.trim();
        
        // Format: yyyyMMdd (no separators)
        if (normalized.length() == 8 && normalized.matches("\\d{8}")) {
            return parseJalaliYyyyMmDd(normalized);
        }
        
        // Format: yyyy/MM/dd or yyyy-MM-dd
        String[] parts;
        if (normalized.contains("/")) {
            parts = normalized.split("/");
        } else if (normalized.contains("-")) {
            parts = normalized.split("-");
        } else {
            throw new IllegalArgumentException("Invalid Jalali date format: " + dateStr);
        }
        
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid Jalali date format: " + dateStr);
        }
        
        int jy = Integer.parseInt(parts[0].trim());
        int jm = Integer.parseInt(parts[1].trim());
        int jd = Integer.parseInt(parts[2].trim());
        
        return jalaliToGregorian(jy, jm, jd);
    }

    // Algorithm adapted from common JDN conversion approach
    private static int[] toGregorian(int jy, int jm, int jd) {
        int gy;
        int gm;
        int gd;

        jy += 1595;
        long days = -355668L + (365L * jy) + ((jy / 33) * 8) + (((jy % 33) + 3) / 4)
                + jd + ((jm < 7) ? (jm - 1) * 31 : ((jm - 7) * 30 + 186));

        gy = (int) (400 * (days / 146097));
        days %= 146097;

        if (days > 36524) {
            gy += 100 * (int) (--days / 36524);
            days %= 36524;
            if (days >= 365) days++;
        }

        gy += 4 * (int) (days / 1461);
        days %= 1461;

        if (days > 365) {
            gy += (int) ((days - 1) / 365);
            days = (days - 1) % 365;
        }

        int[] sal_a = {0,31, (isLeapGregorian(gy) ? 29 : 28),31,30,31,30,31,31,30,31,30,31};
        for (gm = 1; gm <= 12 && days >= sal_a[gm]; gm++) {
            days -= sal_a[gm];
        }
        gd = (int) days + 1;
        return new int[]{gy, gm, gd};
    }

    private static boolean isLeapGregorian(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}






