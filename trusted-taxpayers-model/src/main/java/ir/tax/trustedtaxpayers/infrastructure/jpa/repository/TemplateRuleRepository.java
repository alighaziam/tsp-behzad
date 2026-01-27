package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateRuleEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateRuleRepository extends BaseRepository<TemplateRuleEntity, UUID> {
    List<TemplateRuleEntity> findByTemplateCode(String templateCode);
    List<TemplateRuleEntity> findByTemplateCodeAndActive(String templateCode, String active);
    List<TemplateRuleEntity> findByFieldName(String fieldName);
}



