package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity واسط بین Template و TemplateType
 * 
 * <p>این Entity رابطه Many-to-Many بین Template و TemplateType را برقرار می‌کند.</p>
 * 
 * @author Ghazi-a
 * @since 1.0
 */
@Entity
@Table(name = "TEMPLATE_TEMPLATE_TYPE", 
       uniqueConstraints = {
           @UniqueConstraint(name = "UK_TEMPLATE_TEMPLATE_TYPE", columnNames = {"TEMPLATE_ID", "TEMPLATE_TYPE_ID"})
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTemplateTypeEntity {
    
    @Id
    @Convert(converter = RawUuidConverter.class)
    @Column(name = "ID", nullable = false, updatable = false, columnDefinition = "RAW(16)")
    private UUID id;
    @PrePersist
    protected void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_ID", nullable = false)
    private TemplateEntity template;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_TYPE_ID", nullable = false)
    private TemplateTypeEntity templateType;
}
