package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateTypeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateTemplateTypeRepository extends JpaRepository<TemplateTemplateTypeEntity, UUID> {
    
    /**
     * پیدا کردن تمام روابط یک Template
     */
    @EntityGraph(attributePaths = {"templateType"})
    List<TemplateTemplateTypeEntity> findByTemplateId(UUID templateId);
    
    /**
     * پیدا کردن تمام روابط یک TemplateType
     */
    List<TemplateTemplateTypeEntity> findByTemplateTypeId(UUID templateTypeId);
    
    /**
     * پیدا کردن رابطه خاص بین Template و TemplateType
     */
    Optional<TemplateTemplateTypeEntity> findByTemplateIdAndTemplateTypeId(UUID templateId, UUID templateTypeId);
    
    /**
     * حذف تمام روابط یک Template
     */
    void deleteByTemplateId(UUID templateId);
    
    /**
     * حذف تمام روابط یک TemplateType
     */
    void deleteByTemplateTypeId(UUID templateTypeId);
    
    /**
     * حذف رابطه خاص بین Template و TemplateType
     */
    void deleteByTemplateIdAndTemplateTypeId(UUID templateId, UUID templateTypeId);
}







