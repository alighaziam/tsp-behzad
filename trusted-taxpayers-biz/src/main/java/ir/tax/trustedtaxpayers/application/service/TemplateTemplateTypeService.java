package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTemplateTypeRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * سرویس مدیریت رابطه بین Template و TemplateType
 * 
 * @author Ghazi-a
 */
@Service
public class TemplateTemplateTypeService {
    
    private final TemplateTemplateTypeRepository repository;
    private final TemplateRepository templateRepository;
    private final TemplateTypeRepository templateTypeRepository;
    
    public TemplateTemplateTypeService(
            TemplateTemplateTypeRepository repository,
            TemplateRepository templateRepository,
            TemplateTypeRepository templateTypeRepository) {
        this.repository = repository;
        this.templateRepository = templateRepository;
        this.templateTypeRepository = templateTypeRepository;
    }
    
    @Transactional(readOnly = true)
    public List<TemplateTemplateTypeEntity> findAll() {
        return repository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateTemplateTypeEntity> findById(UUID id) {
        return repository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateTemplateTypeEntity> findByTemplateId(UUID templateId) {
        return repository.findByTemplateId(templateId);
    }
    
    @Transactional(readOnly = true)
    public List<TemplateTemplateTypeEntity> findByTemplateTypeId(UUID templateTypeId) {
        return repository.findByTemplateTypeId(templateTypeId);
    }
    
    @Transactional(readOnly = true)
    public Optional<TemplateTemplateTypeEntity> findByTemplateIdAndTemplateTypeId(UUID templateId, UUID templateTypeId) {
        return repository.findByTemplateIdAndTemplateTypeId(templateId, templateTypeId);
    }
    
    @Transactional
    public TemplateTemplateTypeEntity create(UUID templateId, UUID templateTypeId) {
        TemplateEntity template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        TemplateTypeEntity templateType = templateTypeRepository.findById(templateTypeId)
                .orElseThrow(() -> new IllegalArgumentException("TemplateType not found: " + templateTypeId));
        
        // بررسی وجود رابطه تکراری
        if (repository.findByTemplateIdAndTemplateTypeId(templateId, templateTypeId).isPresent()) {
            throw new IllegalArgumentException("Relationship already exists between template and templateType");
        }
        
        TemplateTemplateTypeEntity entity = TemplateTemplateTypeEntity.builder()
                .template(template)
                .templateType(templateType)
                .build();
        // ID به صورت خودکار در @PrePersist تولید می‌شود
        
        return repository.save(entity);
    }
    
    @Transactional
    public TemplateTemplateTypeEntity save(TemplateTemplateTypeEntity entity) {
        return repository.save(entity);
    }
    
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
    
    @Transactional
    public void deleteByTemplateId(UUID templateId) {
        repository.deleteByTemplateId(templateId);
    }
    
    @Transactional
    public void deleteByTemplateTypeId(UUID templateTypeId) {
        repository.deleteByTemplateTypeId(templateTypeId);
    }
    
    @Transactional
    public void deleteByTemplateIdAndTemplateTypeId(UUID templateId, UUID templateTypeId) {
        repository.deleteByTemplateIdAndTemplateTypeId(templateId, templateTypeId);
    }
}

