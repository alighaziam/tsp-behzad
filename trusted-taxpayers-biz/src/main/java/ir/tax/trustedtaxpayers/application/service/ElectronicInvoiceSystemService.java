package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ElectronicInvoiceSystemService extends BaseCacheServiceImpl<ElectronicInvoiceSystemEntity, UUID> {
    private final ElectronicInvoiceSystemRepository systemRepository;

    public ElectronicInvoiceSystemService(ElectronicInvoiceSystemRepository systemRepository) {
        super(systemRepository);
        this.systemRepository = systemRepository;
    }

    @Cacheable(keyPrefix = "header-id")
    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceSystemEntity> findByHeaderId(UUID headerId) {
        return systemRepository.findByHeaderId(headerId);
    }

    @Transactional(readOnly = true)
    public java.util.Map<java.util.UUID, String> findStsByHeaderIds(java.util.List<java.util.UUID> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return systemRepository.findByHeaderIdIn(headerIds).stream()
                .filter(e -> e.getHeader() != null && e.getHeader().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getHeader().getId(),
                        ElectronicInvoiceSystemEntity::getSts,
                        (a, b) -> a
                ));
    }

    @Cacheable(keyPrefix = "status")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceSystemEntity> findByStatus(String sts) {
        return systemRepository.findBySts(sts);
    }

    @Cacheable(keyPrefix = "status-page")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceSystemEntity> findByStatus(String sts, Pageable pageable) {
        return systemRepository.findBySts(sts, pageable);
    }
    
    /**
     * دریافت صورتحساب‌ها با وضعیت مشخص همراه با اطلاعات header
     */
    @Cacheable(keyPrefix = "status-with-header")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceSystemEntity> findByStatusWithHeader(String sts, Pageable pageable) {
        return systemRepository.findBySts(sts, pageable);
    }
    
    /**
     * دریافت صورتحساب‌ها با چند وضعیت مختلف همراه با اطلاعات header
     */
    @Cacheable(keyPrefix = "statuses-with-header")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceSystemEntity> findByStatusesWithHeader(List<String> statuses, Pageable pageable) {
        return systemRepository.findByStsInWithHeader(statuses, pageable);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceSystemEntity save(ElectronicInvoiceSystemEntity entity) {
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceSystemEntity update(UUID id, ElectronicInvoiceSystemEntity entity) {
        return super.update(id, entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }


    /*public Page<ElectronicInvoiceSystemEntity> fetchReport(
            InvoiceReportService.InvoiceReportFilter filter,
            Pageable pageable
    ) {
        return systemRepository.findReports(
                filter != null ? filter.sellerTaxpayerId() : null,
                filter != null ? filter.taxpayerIds() : List.of(),
                filter == null || filter.taxpayerIds().isEmpty(),
                filter != null && filter.taxpayerNo() != null
                        ? filter.taxpayerNo().trim().toUpperCase()
                        : null,
                filter != null ? filter.statuses() : List.of(),
                filter == null || filter.statuses().isEmpty(),
                filter != null ? filter.fromDate() : null,
                filter != null ? filter.toDate() : null,
                pageable
        );
    }*/
}



