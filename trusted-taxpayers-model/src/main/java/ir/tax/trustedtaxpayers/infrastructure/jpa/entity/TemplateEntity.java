package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity الگوی صورتحساب
 * 
 * <p>این Entity شامل تعریف الگوهای صورتحساب است.</p>
 * 
 * @author Ghazi-a
 * @since 1.0
 */
@Entity
@Table(name = "TEMPLATE")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TemplateEntity extends BaseEntity {
    
    @Column(name = "CODE", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;
}



