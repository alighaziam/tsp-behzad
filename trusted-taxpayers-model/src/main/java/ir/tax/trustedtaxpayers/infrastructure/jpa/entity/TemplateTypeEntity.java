package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity نوع الگو
 * 
 * <p>این Entity شامل انواع مختلف الگوهای صورتحساب است.</p>
 * 
 * @author Ghazi-a
 * @since 1.0
 */
@Entity
@Table(name = "TEMPLATE_TYPE")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTypeEntity extends BaseEntity {
    
    @Column(name = "CODE", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;
}



