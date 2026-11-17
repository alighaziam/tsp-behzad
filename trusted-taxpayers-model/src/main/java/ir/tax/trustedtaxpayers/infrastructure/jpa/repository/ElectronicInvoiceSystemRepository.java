package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceSystemRepository extends BaseRepository<ElectronicInvoiceSystemEntity, UUID> {
    Optional<ElectronicInvoiceSystemEntity> findByHeaderId(UUID headerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ElectronicInvoiceSystemEntity s WHERE s.id = :headerId")
    Optional<ElectronicInvoiceSystemEntity> findByHeaderIdForUpdate(@Param("headerId") UUID headerId);
    /*Optional<ElectronicInvoiceSystemEntity> findByUniqueId(UUID uniqueId);*/
    Optional<ElectronicInvoiceSystemEntity> findByReferenceNumber(String referenceNumber);
    List<ElectronicInvoiceSystemEntity> findBySts(String sts);
    @EntityGraph(attributePaths = {"header", "header.sellerTaxpayer", "header.buyerTaxpayer", "header.template"})
    Page<ElectronicInvoiceSystemEntity> findBySts(String sts, Pageable pageable);
    
    // Removed invalid derived method name findByStsWithHeader; use @EntityGraph on findBySts instead
    
    /**
     * Find invoices with multiple statuses joined with header
     * استفاده از OR برای جستجوی چند وضعیت همزمان
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"header", "header.sellerTaxpayer", "header.buyerTaxpayer", "header.template"})
    @Query("SELECT s FROM ElectronicInvoiceSystemEntity s WHERE s.sts IN :statuses")
    Page<ElectronicInvoiceSystemEntity> findByStsInWithHeader(@Param("statuses") List<String> statuses, Pageable pageable);

    /**
     * Batch fetch by header ids to avoid N+1 when mapping summaries that need sts.
     */
    List<ElectronicInvoiceSystemEntity> findByHeaderIdIn(List<UUID> headerIds);

    /*@Query("""
        select distinct eis
        from ElectronicInvoiceSystemEntity eis
        join eis.header h
        left join h.sellerTaxpayer s
        left join h.buyerTaxpayer b
        where
            (:sellerId is null or s.id = :sellerId)
        and (
            :taxpayerIdsEmpty = true
            or s.id in :taxpayerIds
            or b.id in :taxpayerIds
        )
        and (
            :taxpayerNo is null
            or upper(s.taxId) = :taxpayerNo
            or upper(b.taxId) = :taxpayerNo
        )
        and (
            :statusesEmpty = true
            or eis.sts in :statuses
        )
        and (
            :fromDate is null
            or h.createdOn >= :fromDate
        )
        and (
            :toDate is null
            or h.createdOn <= :toDate
        )
        """)
    Page<ElectronicInvoiceSystemEntity> findReports(
            @Param("sellerId") UUID sellerId,
            @Param("taxpayerIds") Collection<UUID> taxpayerIds,
            @Param("taxpayerIdsEmpty") boolean taxpayerIdsEmpty,
            @Param("taxpayerNo") String taxpayerNo,
            @Param("statuses") Collection<String> statuses,
            @Param("statusesEmpty") boolean statusesEmpty,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate,
            Pageable pageable
    );*/

    @Transactional
    @Modifying
    @Query(
            value = """
        UPDATE ELECTRONIC_INVOICE_SYSTEM
        SET STS = :sts
        WHERE HEADER_ID = HEXTORAW(REPLACE(:id, '-', ''))
        """,
            nativeQuery = true
    )
    int updateStsNative(@Param("id") String id,
                        @Param("sts") String sts);


    @Transactional
    @Modifying
    @Query(
            value = """
    INSERT INTO ELECTRONIC_INVOICE_SYSTEM (HEADER_ID, STS)
    VALUES (HEXTORAW(REPLACE(:id, '-', '')),:sts)
    """,
            nativeQuery = true
    )
    void insertNative(@Param("id") String id,
                      @Param("sts") String sts);



}



