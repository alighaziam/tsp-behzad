package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoicePaymentEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoicePaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ElectronicInvoicePaymentService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoicePaymentEntity, UUID> {
    private final ElectronicInvoicePaymentRepository paymentRepository;

    public ElectronicInvoicePaymentService(ElectronicInvoicePaymentRepository paymentRepository) {
        super(paymentRepository);
        this.paymentRepository = paymentRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public List<ElectronicInvoicePaymentEntity> findByHeaderId(UUID headerId) {
        return paymentRepository.findByHeaderIdOrderByPdtDesc(headerId);
    }

    @Cacheable(keyPrefix = "date-range")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoicePaymentEntity> findByDateRange(OffsetDateTime start, OffsetDateTime end, Pageable pageable) {
        return paymentRepository.findByPdtBetween(start, end, pageable);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoicePaymentEntity save(ElectronicInvoicePaymentEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoicePaymentEntity update(UUID id, ElectronicInvoicePaymentEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



