package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.ExtensionType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.converter.ExtensionTypeConverter;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Entity فیلدهای تکمیلی صورتحساب الکترونیکی
 * 
 * <p>این Entity برای فیلدهای آینده و اطلاعات تکمیلی در نظر گرفته شده است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_EXTENSION")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicInvoiceExtensionEntity extends BaseEntity {

    @Column(name = "HEADER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID headerId;

    @Column(name = "EXTENSION_KEY", nullable = false, length = 100)
    private String extensionKey;

    @Lob
    @Column(name = "EXTENSION_VALUE")
    private String extensionValue;

    @Column(name = "EXTENSION_TYPE", length = 20)
    @Convert(converter = ExtensionTypeConverter.class)
    private ExtensionType extensionType; // STRING, NUMBER, DATE, JSON, XML, BOOLEAN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    private ElectronicInvoiceHeaderEntity header;
}



