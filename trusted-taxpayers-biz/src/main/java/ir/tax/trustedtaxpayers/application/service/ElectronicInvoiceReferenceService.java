package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceReferenceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ElectronicInvoiceReferenceService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoiceReferenceEntity, UUID> {
    private final ElectronicInvoiceReferenceRepository referenceRepository;

    public ElectronicInvoiceReferenceService(ElectronicInvoiceReferenceRepository referenceRepository) {
        super(referenceRepository);
        this.referenceRepository = referenceRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceReferenceEntity> findByHeaderId(UUID headerId) {
        return referenceRepository.findByHeaderId(headerId);
    }

    @Cacheable(keyPrefix = "irtaxid")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceReferenceEntity> findByIrtaxid(String irtaxid) {
        return referenceRepository.findByIrtaxid(irtaxid);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceReferenceEntity save(ElectronicInvoiceReferenceEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceReferenceEntity update(UUID id, ElectronicInvoiceReferenceEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



