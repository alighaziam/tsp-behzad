package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
@Entity
@Table(name = "PATTERN_REQUEST_CLIENT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PatternRequestClientEntity extends BaseEntity {
    @Column(name = "THIRD_PARTY_ID", nullable = false, length = 100)
    private String thirdPartyId;
    @Column(name = "EXTERNAL_REFERENCE", length = 150)
    private String externalReference;
    @Column(name = "EXTERNAL_REQUEST_ID", length = 128)
    private String externalRequestId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REQUEST_ID", insertable = false, updatable = false)
    private PatternRequestEntity request;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", insertable = false, updatable = false)
    private PatternClientEntity client;
}
