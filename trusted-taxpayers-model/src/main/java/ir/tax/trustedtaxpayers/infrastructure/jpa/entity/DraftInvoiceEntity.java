package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "DRAFT_INVOICES")
@Data
public class DraftInvoiceEntity extends BaseEntity {

    @Convert(converter = RawUuidConverter.class)
    @Column(name = "OWNER_USER_ID", columnDefinition = "RAW(16)")
    private UUID ownerUserId;

    @Column(name = "SOURCE", length = 32, nullable = false)
    private String source; // OCR | IMPORT

    @Column(name = "RAW_TEXT", columnDefinition = "CLOB")
    private String rawText;

    @Column(name = "PARSED_JSON", columnDefinition = "CLOB")
    private String parsedJson;

    @Column(name = "STATUS", length = 24, nullable = false)
    private String status; // DRAFT | CONFIRMED | SUBMITTED

}

