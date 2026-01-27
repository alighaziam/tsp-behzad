package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceShippingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceShippingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ElectronicInvoiceShippingService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoiceShippingEntity, UUID> {
    private final ElectronicInvoiceShippingRepository shippingRepository;

    public ElectronicInvoiceShippingService(ElectronicInvoiceShippingRepository shippingRepository) {
        super(shippingRepository);
        this.shippingRepository = shippingRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceShippingEntity> findByHeaderId(UUID headerId) {
        return shippingRepository.findByHeaderId(headerId);
    }

    @Cacheable(keyPrefix = "waybill-no")
    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceShippingEntity> findByWaybillNo(String lno) {
        return shippingRepository.findByLno(lno).stream().findFirst();
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceShippingEntity save(ElectronicInvoiceShippingEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceShippingEntity update(UUID id, ElectronicInvoiceShippingEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



