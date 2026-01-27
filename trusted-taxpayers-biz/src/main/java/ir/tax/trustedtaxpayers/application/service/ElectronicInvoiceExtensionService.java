package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceExtensionEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceExtensionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ElectronicInvoiceExtensionService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoiceExtensionEntity, UUID> {
    private final ElectronicInvoiceExtensionRepository extensionRepository;

    public ElectronicInvoiceExtensionService(ElectronicInvoiceExtensionRepository extensionRepository) {
        super(extensionRepository);
        this.extensionRepository = extensionRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceExtensionEntity> findByHeaderId(UUID headerId) {
        return extensionRepository.findByHeaderId(headerId);
    }

    @Cacheable(keyPrefix = "header-key")
    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceExtensionEntity> findByHeaderIdAndKey(UUID headerId, String extensionKey) {
        return extensionRepository.findByHeaderIdAndExtensionKey(headerId, extensionKey).stream().findFirst();
    }

    @Cacheable(keyPrefix = "extension-key")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceExtensionEntity> findByExtensionKey(String extensionKey) {
        return extensionRepository.findByExtensionKey(extensionKey);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceExtensionEntity save(ElectronicInvoiceExtensionEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceExtensionEntity update(UUID id, ElectronicInvoiceExtensionEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



