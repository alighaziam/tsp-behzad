package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TemplateTypeService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<TemplateTypeEntity, UUID> {
    private final TemplateTypeRepository templateTypeRepository;

    public TemplateTypeService(TemplateTypeRepository templateTypeRepository) {
        super(templateTypeRepository);
        this.templateTypeRepository = templateTypeRepository;
    }

    @Cacheable(cacheName = "templateTypes", keyPrefix = "code")
    @Transactional(readOnly = true)
    public Optional<TemplateTypeEntity> findByCode(String code) {
        return templateTypeRepository.findByCode(code);
    }

    @CacheEvict(cacheName = "templateTypes", allEntries = true)
    @Override
    @Transactional
    public TemplateTypeEntity save(TemplateTypeEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(cacheName = "templateTypes", allEntries = true)
    @Override
    @Transactional
    public TemplateTypeEntity update(UUID id, TemplateTypeEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(cacheName = "templateTypes", allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



