package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity تنظیمات سیستم
 * 
 * <p>این Entity برای ذخیره تنظیمات سیستم مانند فعال/غیرفعال کردن Redis، RateLimit و سایر تنظیمات استفاده می‌شود.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "SETTINGS")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SettingEntity extends BaseEntity {
    @Column(name = "PATH", length = 500)
    private String path;

    @Column(name = "URL", length = 1000)
    private String url;

    @Column(name = "KEY_NAME", nullable = false, unique = true, length = 100)
    private String key;

    @Column(name = "\"VALUE\"", length = 4000)
    private String value;

    @Column(name = "PASS", length = 500)
    private String pass;

    @Enumerated(EnumType.STRING)
    @Column(name = "GROUP_TYPE", length = 50)
    private SettingGroupType groupType;
}


