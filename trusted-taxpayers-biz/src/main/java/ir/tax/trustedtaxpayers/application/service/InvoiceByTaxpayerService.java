package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Loads invoices for a taxpayer using the fiscal identifiers stored on invoice headers.
 * The header taxpayer relations are intentionally not mapped in this schema, so
 * seller TINS and buyer TINB are the authoritative lookup keys.
 */
@Service
public class InvoiceByTaxpayerService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceByTaxpayerService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdOn", "updatedOn", "taxId", "indatim", "tbill", "tins", "tinb"
    );

    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final CacheService cacheService;

    public InvoiceByTaxpayerService(
            ElectronicInvoiceHeaderRepository invoiceRepository,
            TaxpayerRepository taxpayerRepository,
            CacheService cacheService) {
        this.invoiceRepository = invoiceRepository;
        this.taxpayerRepository = taxpayerRepository;
        this.cacheService = cacheService;
    }

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTaxpayer(
            UUID taxpayerId, Integer page, Integer size, String sortBy, String direction) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        if (taxpayer == null) {
            return Page.empty();
        }
        return invoiceRepository.findByTinsOrTinb(
                taxpayer.getTaxId(), taxpayer.getTaxId(), buildPageable(page, size, sortBy, direction));
    }

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTaxpayerNo(
            String taxpayerNo, Integer page, Integer size, String sortBy, String direction) {
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            return Page.empty();
        }
        if (taxpayerRepository.findByTaxId(taxpayerNo.trim())
                .filter(taxpayer -> Boolean.TRUE.equals(taxpayer.getIsActive())).isEmpty()) {
            return Page.empty();
        }
        return invoiceRepository.findByTinsOrTinb(
                taxpayerNo.trim(), taxpayerNo.trim(), buildPageable(page, size, sortBy, direction));
    }

    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadAllInvoicesByTaxpayer(UUID taxpayerId) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        if (taxpayer == null) {
            return Collections.emptyList();
        }
        return invoiceRepository.findByTinsOrTinb(taxpayer.getTaxId(), taxpayer.getTaxId());
    }

    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadAllInvoicesByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()
                || taxpayerRepository.findByTaxId(taxpayerNo.trim())
                        .filter(taxpayer -> Boolean.TRUE.equals(taxpayer.getIsActive())).isEmpty()) {
            return Collections.emptyList();
        }
        return invoiceRepository.findByTinsOrTinb(taxpayerNo.trim(), taxpayerNo.trim());
    }

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesAsSeller(
            UUID taxpayerId, Integer page, Integer size, String sortBy, String direction) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        if (taxpayer == null) {
            return Page.empty();
        }
        return invoiceRepository.findByTins(
                taxpayer.getTaxId(), buildPageable(page, size, sortBy, direction));
    }

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesAsBuyer(
            UUID taxpayerId, Integer page, Integer size, String sortBy, String direction) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        if (taxpayer == null) {
            return Page.empty();
        }
        return invoiceRepository.findByTinb(
                taxpayer.getTaxId(), buildPageable(page, size, sortBy, direction));
    }

    @Transactional(readOnly = true)
    public long countInvoicesByTaxpayer(UUID taxpayerId) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        return taxpayer == null ? 0 : invoiceRepository.countByTinsOrTinb(
                taxpayer.getTaxId(), taxpayer.getTaxId());
    }

    @Transactional(readOnly = true)
    public long countInvoicesByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()
                || taxpayerRepository.findByTaxId(taxpayerNo.trim()).isEmpty()) {
            return 0;
        }
        return invoiceRepository.countByTinsOrTinb(taxpayerNo.trim(), taxpayerNo.trim());
    }

    @Transactional(readOnly = true)
    public long countInvoicesAsSeller(UUID taxpayerId) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        return taxpayer == null ? 0 : invoiceRepository.countByTins(taxpayer.getTaxId());
    }

    @Transactional(readOnly = true)
    public long countInvoicesAsBuyer(UUID taxpayerId) {
        TaxpayerEntity taxpayer = findTaxpayer(taxpayerId);
        return taxpayer == null ? 0 : invoiceRepository.countByTinb(taxpayer.getTaxId());
    }

    private TaxpayerEntity findTaxpayer(UUID taxpayerId) {
        if (taxpayerId == null) {
            return null;
        }
        return taxpayerRepository.findById(taxpayerId)
                .filter(taxpayer -> Boolean.TRUE.equals(taxpayer.getIsActive()))
                .orElse(null);
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy, String direction) {
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return PageRequest.of(safePage, safeSize, buildSort(sortBy, direction));
    }

    private Sort buildSort(String sortBy, String direction) {
        String field = sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdOn";
        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(sortDirection, field);
    }

    public void invalidateCache(UUID taxpayerId) {
        cacheService.evictAll("invoices-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-list");
        cacheService.evictAll("invoice-count-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-as-seller");
        cacheService.evictAll("invoices-by-taxpayer-as-buyer");
        cacheService.evictAll("invoice-count-by-taxpayer-as-seller");
        cacheService.evictAll("invoice-count-by-taxpayer-as-buyer");
        logger.debug("Cache invalidated for taxpayerId: {}", taxpayerId);
    }

    public void invalidateCacheByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            return;
        }
        taxpayerRepository.findByTaxId(taxpayerNo.trim())
                .ifPresent(taxpayer -> invalidateCache(taxpayer.getId()));
    }

    public void invalidateAllCache() {
        cacheService.evictAll("invoices-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-list");
        cacheService.evictAll("invoice-count-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-as-seller");
        cacheService.evictAll("invoices-by-taxpayer-as-buyer");
        cacheService.evictAll("invoice-count-by-taxpayer-as-seller");
        cacheService.evictAll("invoice-count-by-taxpayer-as-buyer");
        logger.info("All invoice by taxpayer caches invalidated");
    }
}
