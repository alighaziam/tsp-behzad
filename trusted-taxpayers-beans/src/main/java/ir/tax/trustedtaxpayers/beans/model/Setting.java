package ir.tax.trustedtaxpayers.beans.model;

import java.util.UUID;

/**
 * مدل دامنه برای تنظیمات سیستم شامل مسیر، کلید و مقادیر حساس/غیرحساس.
 * <p>
 * Domain model representing system settings and their associated metadata.
 */
public class Setting {
    /**
     * شناسه یکتا برای تنظیم.
     * <p>
     * Unique identifier of the setting.
     */
    private UUID id;
    /**
     * مسیر منطقی یا نام گروه تنظیم.
     * <p>
     * Logical path or grouping identifier.
     */
    private String path;
    /**
     * آدرس URL مرتبط در صورت وجود.
     * <p>
     * Optional URL associated with the setting.
     */
    private String url;
    /**
     * کلید تنظیم.
     * <p>
     * Setting key.
     */
    private String key;
    /**
     * مقدار ذخیره‌شده (معمولاً غیرحساس).
     * <p>
     * Stored value (often non-sensitive).
     */
    private String value;
    /**
     * مقدار محرمانه یا رمز عبور مرتبط.
     * <p>
     * Secret value or password tied to the setting.
     */
    private String pass;
    /**
     * گروه‌بندی نوع تنظیم برای دسته‌بندی بهتر.
     * <p>
     * Group type category for organizing settings.
     */
    private String groupType;

    public Setting() {
    }

    public Setting(UUID id, String path, String url, String key, String value, String pass) {
        this(id, path, url, key, value, pass, null);
    }

    public Setting(UUID id, String path, String url, String key, String value, String pass, String groupType) {
        this.id = id;
        this.path = path;
        this.url = url;
        this.key = key;
        this.value = value;
        this.pass = pass;
        this.groupType = groupType;
    }

    public UUID getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getPass() {
        return pass;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(String groupType) {
        this.groupType = groupType;
    }
}


