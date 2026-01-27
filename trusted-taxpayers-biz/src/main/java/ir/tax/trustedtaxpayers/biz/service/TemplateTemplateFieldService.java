package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldId;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateFieldRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTemplateFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateTemplateFieldService {
    
    private final TemplateTemplateFieldRepository repository;
    private final TemplateFieldRepository templateFieldRepository;
    
    public TemplateTemplateFieldEntity save(TemplateTemplateFieldEntity entity) {
        return repository.save(entity);
    }
    
    public Optional<TemplateTemplateFieldEntity> findById(UUID templateId, UUID templateFieldId) {
        return repository.findById(new TemplateTemplateFieldId(templateId, templateFieldId));
    }
    
    public List<TemplateTemplateFieldEntity> findByTemplateId(UUID templateId) {
        return repository.findByTemplateId(templateId);
    }
    
    public List<TemplateTemplateFieldEntity> findByTemplateFieldId(UUID templateFieldId) {
        return repository.findByTemplateFieldId(templateFieldId);
    }
    
    public List<TemplateTemplateFieldEntity> findAll() {
        return repository.findAll();
    }
    
    @Transactional
    public void delete(UUID templateId, UUID templateFieldId) {
        repository.deleteById(new TemplateTemplateFieldId(templateId, templateFieldId));
    }
    
    @Transactional
    public void deleteByTemplateId(UUID templateId) {
        repository.deleteByTemplateId(templateId);
    }
    
    @Transactional
    public void deleteByTemplateFieldId(UUID templateFieldId) {
        repository.deleteByTemplateFieldId(templateFieldId);
    }
    
    public List<TemplateFieldEntity> getFieldsByTemplateId(UUID templateId) {
        List<TemplateTemplateFieldEntity> templateFields = repository.findByTemplateId(templateId);
        List<UUID> fieldIds = templateFields.stream()
                .map(TemplateTemplateFieldEntity::getTemplateFieldId)
                .collect(Collectors.toList());
        return templateFieldRepository.findAllById(fieldIds);
    }
    
    public List<TemplateTemplateFieldEntity> getTemplateFieldsByTemplateId(UUID templateId) {
        return repository.findByTemplateId(templateId);
    }
}

