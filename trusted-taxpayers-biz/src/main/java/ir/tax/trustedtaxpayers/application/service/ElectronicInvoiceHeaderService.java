package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.application.validation.TemplateValidator;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ElectronicInvoiceHeaderService extends ir.tax.trustedtaxpayers.core.service.BaseCacheServiceImpl<ElectronicInvoiceHeaderEntity, UUID> {
    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final TemplateValidator templateValidator;

    public ElectronicInvoiceHeaderService(
            ElectronicInvoiceHeaderRepository headerRepository,
            TemplateValidator templateValidator) {
        super(headerRepository);
        this.headerRepository = headerRepository;
        this.templateValidator = templateValidator;
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceHeaderEntity save(ElectronicInvoiceHeaderEntity entity) {
        // اعتبارسنجی Template قبل از ذخیره
      /*  templateValidator.validate(entity);*/
        return super.save(entity);
    }

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public ElectronicInvoiceHeaderEntity update(UUID id, ElectronicInvoiceHeaderEntity entity) {
        // اعتبارسنجی Template قبل از به‌روزرسانی
        templateValidator.validate(entity);
        return super.update(id, entity);
    }

   /* @Cacheable(keyPrefix = "tax-id", includeParameters = false)
    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceHeaderEntity> findByTaxId(String taxId) {
        return headerRepository.findByTaxId(taxId);
    }

    @Cacheable(keyPrefix = "template-code", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findByTemplateCode(String templateCode, Pageable pageable) {
        return headerRepository.findByTemplateCode(templateCode, pageable);
    }

    @Cacheable(keyPrefix = "seller-tin", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findBySellerTin(String tins, Pageable pageable) {
        return headerRepository.findByTins(tins, pageable);
    }

    @Cacheable(keyPrefix = "buyer-tin", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findByBuyerTin(String tinb, Pageable pageable) {
        return headerRepository.findByTinb(tinb, pageable);
    }

    @Cacheable(keyPrefix = "inty", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findByInty(Integer inty, Pageable pageable) {
        return headerRepository.findByInty(inty, pageable);
    }

    @Cacheable(keyPrefix = "reference-tax-id", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findByReferenceTaxId(String irtaxid, Pageable pageable) {
        return headerRepository.findByIrtaxid(irtaxid, pageable);
    }

    @Cacheable(keyPrefix = "template-inty", includeParameters = false)
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> findByTemplateAndInty(String templateCode, Integer inty, Pageable pageable) {
        return headerRepository.findByTemplateCodeAndInty(templateCode, inty, pageable);
    }

    // Deprecated methods - kept for backward compatibility
    @Deprecated
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> findByTemplateCode(String templateCode) {
        return headerRepository.findByTemplateCode(templateCode);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> findBySellerTin(String tins) {
        return headerRepository.findByTins(tins);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> findByBuyerTin(String tinb) {
        return headerRepository.findByTinb(tinb);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> findByInty(Integer inty) {
        return headerRepository.findByInty(inty);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> findByReferenceTaxId(String irtaxid) {
        return headerRepository.findByIrtaxid(irtaxid);
    }*/

    @CacheEvict(allEntries = true)
    @Override
    @Transactional
    public void deleteById(UUID id) {
        super.deleteById(id);
    }
}



