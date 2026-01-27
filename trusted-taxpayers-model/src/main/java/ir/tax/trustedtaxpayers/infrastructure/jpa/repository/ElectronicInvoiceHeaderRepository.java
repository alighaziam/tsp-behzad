package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceHeaderRepository extends BaseRepository<ElectronicInvoiceHeaderEntity, UUID> {
    
   /* @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Optional<ElectronicInvoiceHeaderEntity> findByTaxId(String taxId);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByTemplateCode(String templateCode, Pageable pageable);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByTins(String tins, Pageable pageable);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByTinb(String tinb, Pageable pageable);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByInty(Integer inty, Pageable pageable);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByIrtaxid(String irtaxid, Pageable pageable);
    
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByTemplateCodeAndInty(String templateCode, Integer inty, Pageable pageable);
    
    *//**
     * Count invoices by template code
     *//*
    long countByTemplateCode(String templateCode);
    
    *//**
     * Find invoices by multiple template codes (IN clause) for better performance
     * استفاده از این متد برای InvoiceByTemplateTypeService
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByTemplateCodeIn(List<String> templateCodes, Pageable pageable);
    
    *//**
     * Find all invoices by multiple template codes (IN clause) without pagination
     * استفاده از این متد برای InvoiceByTemplateTypeService - لیست بدون pagination
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByTemplateCodeIn(List<String> templateCodes);
    
    *//**
     * Count invoices by multiple template codes
     *//*
    long countByTemplateCodeIn(List<String> templateCodes);
    
    *//**
     * Find invoices by seller taxpayer ID with pagination
     * جستجوی صورتحساب‌ها بر اساس شناسه مودی فروشنده
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findBySellerTaxpayerId(UUID sellerTaxpayerId, Pageable pageable);
    
    *//**
     * Find invoices by buyer taxpayer ID with pagination
     * جستجوی صورتحساب‌ها بر اساس شناسه مودی خریدار
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findByBuyerTaxpayerId(UUID buyerTaxpayerId, Pageable pageable);
    
    *//**
     * Find invoices where taxpayer is either seller or buyer
     * جستجوی صورتحساب‌ها بر اساس شناسه مودی (فروشنده یا خریدار)
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findBySellerTaxpayerIdOrBuyerTaxpayerId(
            UUID sellerTaxpayerId, UUID buyerTaxpayerId, Pageable pageable);
    
    *//**
     * Find invoices by both seller and buyer taxpayer IDs with pagination
     * جستجوی صورتحساب‌ها بر اساس شناسه مودی فروشنده و خریدار
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    Page<ElectronicInvoiceHeaderEntity> findBySellerTaxpayerIdAndBuyerTaxpayerId(
            UUID sellerTaxpayerId, UUID buyerTaxpayerId, Pageable pageable);*/
    
    /**
     * Count invoices by seller taxpayer ID
     */
    /*long countBySellerTaxpayerId(UUID sellerTaxpayerId);*/
    
    /**
     * Count invoices by buyer taxpayer ID
     */
   /* long countByBuyerTaxpayerId(UUID buyerTaxpayerId);
    
    *//**
     * Count invoices by both seller and buyer taxpayer IDs
     *//*
    long countBySellerTaxpayerIdAndBuyerTaxpayerId(UUID sellerTaxpayerId, UUID buyerTaxpayerId);*/
    
    // Legacy methods without pagination (deprecated - use paginated versions instead)
    /*@Deprecated
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByTemplateCode(String templateCode);
    
    @Deprecated
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByTins(String tins);
    
    @Deprecated
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByTinb(String tinb);
    
    @Deprecated
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByInty(Integer inty);
    
    @Deprecated
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findByIrtaxid(String irtaxid);
    
    *//**
     * Legacy helper without pagination برای استفاده از سرویس‌های داخلی
     *//*
    @EntityGraph(attributePaths = {"sellerTaxpayer", "buyerTaxpayer", "template"})
    List<ElectronicInvoiceHeaderEntity> findBySellerTaxpayerIdOrBuyerTaxpayerId(
            UUID sellerTaxpayerId, UUID buyerTaxpayerId);*/

    @Query("SELECT h FROM ElectronicInvoiceHeaderEntity h WHERE h.id = :id")
    Optional<ElectronicInvoiceHeaderEntity> findHeaderManual(@Param("id") UUID id);
}



