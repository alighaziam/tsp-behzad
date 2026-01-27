package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateTemplateFieldRepository extends JpaRepository<TemplateTemplateFieldEntity, TemplateTemplateFieldId> {
    List<TemplateTemplateFieldEntity> findByTemplateId(UUID templateId);
    List<TemplateTemplateFieldEntity> findByTemplateFieldId(UUID templateFieldId);
    void deleteByTemplateId(UUID templateId);
    void deleteByTemplateFieldId(UUID templateFieldId);
}





