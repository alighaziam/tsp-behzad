package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceItemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ElectronicInvoiceItemService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoiceItemEntity, UUID> {
    private final ElectronicInvoiceItemRepository itemRepository;

    public ElectronicInvoiceItemService(ElectronicInvoiceItemRepository itemRepository) {
        super(itemRepository);
        this.itemRepository = itemRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceItemEntity> findByHeaderId(UUID headerId) {
        return itemRepository.findByHeaderIdOrderByLineNo(headerId);
    }

    @Cacheable(keyPrefix = "sstid")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceItemEntity> findBySstid(String sstid) {
        return itemRepository.findBySstid(sstid);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteByHeaderId(UUID headerId) {
        List<ElectronicInvoiceItemEntity> items = itemRepository.findByHeaderId(headerId);
        itemRepository.deleteAll(items);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceItemEntity save(ElectronicInvoiceItemEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceItemEntity update(UUID id, ElectronicInvoiceItemEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



