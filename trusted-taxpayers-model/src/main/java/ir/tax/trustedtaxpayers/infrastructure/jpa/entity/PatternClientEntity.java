package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * موجودیت JPA برای نگهداری اطلاعات شرکت‌های طرف قرارداد (third-party).
 * <p>
 * JPA entity storing metadata and credentials for pattern clients.
 */
@Entity
@Table(name = "PATTERN_CLIENT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PatternClientEntity extends BaseEntity {

    /**
     * کلید یکتای قابل‌استفاده در API ها.
     * <p>
     * Unique API key issued to the client.
     */
    @Column(name = "CLIENT_KEY", nullable = false, unique = true, length = 64)
    private String clientKey;

    /**
     * نام نمایشی شرکت.
     * <p>
     * Display name of the client organization.
     */
    @Column(name = "NAME", nullable = false, length = 150)
    private String name;

    /**
     * توضیحات اختیاری درباره شرکت.
     * <p>
     * Optional description for the client.
     */
    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

    /**
     * ایمیل تماس جهت هماهنگی و پشتیبانی.
     * <p>
     * Contact email used for coordination and support.
     */
    @Column(name = "CONTACT_EMAIL", length = 150)
    private String contactEmail;

    /**
     * آدرس callback ثبت‌شده برای دریافت نتیجه ارسال.
     * <p>
     * Callback URL registered for webhook notifications.
     */
    @Column(name = "CALLBACK_URL", length = 300)
    private String callbackUrl;

    /**
     * کلید مخفی اختصاصی برای امضا و احراز هویت.
     * <p>
     * Secret key used for signing and authentication.
     */
    @Column(name = "CLIENT_SECRET", nullable = false, length = 128)
    private String clientSecret;
}
