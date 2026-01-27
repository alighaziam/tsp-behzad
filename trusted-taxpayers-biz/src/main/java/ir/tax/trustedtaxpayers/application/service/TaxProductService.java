package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.service.BaseServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * سرویس مدیریت محصولات مالیاتی
 */
@Service
public class TaxProductService extends BaseServiceImpl<TaxProductEntity, UUID> {
    
    private final TaxProductRepository taxProductRepository;

    public TaxProductService(TaxProductRepository taxProductRepository) {
        super(taxProductRepository);
        this.taxProductRepository = taxProductRepository;
    }

    /**
     * جستجوی محصولات مالیاتی برای autocomplete
     *
     * @param query متن جستجو (می‌تواند بخشی از taxProductId یا title باشد)
     * @param limit حداکثر تعداد نتایج
     * @param page شماره صفحه
     * @return لیست محصولات مالیاتی
     */
    @Transactional(readOnly = true)
    public List<TaxProductEntity> search(String query, int limit, int page) {
        int safeLimit = Math.min(Math.max(limit, 1), 30);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.ASC, "productTitle"));

        if (query == null || query.isBlank()) {
            return taxProductRepository.findAll(pageable).getContent();
        }
        
        return taxProductRepository.findByTaxProductIdContainingIgnoreCaseOrTitleContainingIgnoreCase(
                query, pageable);
    }
}

