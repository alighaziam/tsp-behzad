package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "ROLE")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleEntity extends BaseEntity {

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "TITLE", length = 100)
    private String title;

    @Lob
    @Column(name = "DESCRIPTION")
    private String description;

}



