package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateRuleEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TemplateRuleService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<TemplateRuleEntity, UUID> {
    private final TemplateRuleRepository templateRuleRepository;

    public TemplateRuleService(TemplateRuleRepository templateRuleRepository) {
        super(templateRuleRepository);
        this.templateRuleRepository = templateRuleRepository;
    }

    @Cacheable(keyPrefix = "template-code")
    @Transactional(readOnly = true)
    public List<TemplateRuleEntity> findByTemplateCode(String templateCode) {
        return templateRuleRepository.findByTemplateCode(templateCode);
    }

    @Cacheable(keyPrefix = "template-code-active")
    @Transactional(readOnly = true)
    public List<TemplateRuleEntity> findByTemplateCodeAndActive(String templateCode, String active) {
        return templateRuleRepository.findByTemplateCodeAndActive(templateCode, active);
    }

    @Cacheable(keyPrefix = "field-name")
    @Transactional(readOnly = true)
    public List<TemplateRuleEntity> findByFieldName(String fieldName) {
        return templateRuleRepository.findByFieldName(fieldName);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public TemplateRuleEntity save(TemplateRuleEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public TemplateRuleEntity update(UUID id, TemplateRuleEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



