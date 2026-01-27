package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTemplateUsageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTemplateUsageId;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerTemplateUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxpayerTemplateUsageService {
    
    private final TaxpayerTemplateUsageRepository repository;

    public TaxpayerTemplateUsageEntity save(TaxpayerTemplateUsageEntity entity) {
        return repository.save(entity);
    }
    
    public Optional<TaxpayerTemplateUsageEntity> findById(UUID taxpayerId, UUID templateId) {
        return repository.findById(new TaxpayerTemplateUsageId(taxpayerId, templateId));
    }
    
    public List<TaxpayerTemplateUsageEntity> findTop10ByTaxpayerIdOrderByLastUsedAtDesc(UUID taxpayerId) {
        Pageable pageable = PageRequest.of(0, 10);
        return repository.findByTaxpayerIdOrderByLastUsedAtDesc(taxpayerId, pageable);
    }
    
    @Transactional
    public TaxpayerTemplateUsageEntity updateOrCreate(UUID taxpayerId, UUID templateId) {
        Optional<TaxpayerTemplateUsageEntity> existing = findById(taxpayerId, templateId);
        if (existing.isPresent()) {
            TaxpayerTemplateUsageEntity entity = existing.get();
            entity.setLastUsedAt(LocalDateTime.now());
            return repository.save(entity);
        } else {
            TaxpayerTemplateUsageEntity entity = TaxpayerTemplateUsageEntity.builder()
                    .taxpayerId(taxpayerId)
                    .templateId(templateId)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            return repository.save(entity);
        }
    }
    
    public List<TaxpayerTemplateUsageEntity> findAll() {
        return repository.findAll();
    }
    
    @Transactional
    public void delete(UUID taxpayerId, UUID templateId) {
        repository.deleteById(new TaxpayerTemplateUsageId(taxpayerId, templateId));
    }
}

