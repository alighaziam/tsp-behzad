package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TAXPAYER_TEMPLATE_USAGE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TaxpayerTemplateUsageId.class)
public class TaxpayerTemplateUsageEntity {
    
    @Id
    @Convert(converter = RawUuidConverter.class)
    @Column(name = "TAXPAYER_ID", columnDefinition = "RAW(16)")
    private UUID taxpayerId;
    
    @Id
    @Convert(converter = RawUuidConverter.class)
    @Column(name = "TEMPLATE_ID", columnDefinition = "RAW(16)")
    private UUID templateId;
    
    @Column(name = "LAST_USED_AT", nullable = false)
    private LocalDateTime lastUsedAt;
}





