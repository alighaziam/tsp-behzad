package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TemplateService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<TemplateEntity, UUID> {
    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        super(templateRepository);
        this.templateRepository = templateRepository;
    }

    @Cacheable(cacheName = "templates", keyPrefix = "code")
    @Transactional(readOnly = true)
    public Optional<TemplateEntity> findByCode(String code) {
        return templateRepository.findByCode(code);
    }

    @CacheEvict(cacheName = "templates", allEntries = true)
    @Override
    @Transactional
    public TemplateEntity save(TemplateEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(cacheName = "templates", allEntries = true)
    @Override
    @Transactional
    public TemplateEntity update(UUID id, TemplateEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(cacheName = "templates", allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
    
    /**
     * جستجوی templates برای autocomplete
     * 
     * @param query متن جستجو (می‌تواند بخشی از code یا title باشد)
     * @param limit حداکثر تعداد نتایج
     * @param page شماره صفحه
     * @return لیست templates
     */
    @Cacheable(cacheName = "templates", keyPrefix = "autocomplete")
    @Transactional(readOnly = true)
    public List<TemplateEntity> search(String query, int limit, int page) {
        int safeLimit = Math.min(Math.max(limit, 1), 30);
        int safePage = Math.max(page, 0);
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(safePage, safeLimit, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "title"));
        
        if (query == null || query.isBlank()) {
            return templateRepository.findAll(pageable).getContent();
        }
        
        return templateRepository.findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
                query, query, pageable);
    }
}



