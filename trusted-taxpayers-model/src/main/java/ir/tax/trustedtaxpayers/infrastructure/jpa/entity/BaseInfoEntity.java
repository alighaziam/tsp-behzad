package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.Convert;
import java.util.UUID;


@Entity
@Table(name = "BASE_INFO")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 50)
public class BaseInfoEntity extends BaseEntity {

    @Id
    @Convert(converter = RawUuidConverter.class)
    @Column(name = "ID", nullable = false, updatable = false, columnDefinition = "RAW(16)")
    private UUID id;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    @Column(name = "CATEGORY", nullable = false, length = 100)
    private String category;
}

