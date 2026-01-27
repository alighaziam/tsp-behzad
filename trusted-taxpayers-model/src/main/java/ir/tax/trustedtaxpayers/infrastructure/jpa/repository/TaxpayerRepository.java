package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxpayerRepository extends BaseRepository<TaxpayerEntity, UUID> {
    
    /**
     * Case-insensitive search using a prebuilt pattern (e.g. "%QUERY%").
     * Avoids CONCAT for better H2 compatibility.
     */
    @Query("SELECT t FROM TaxpayerEntity t WHERE " +
           "UPPER(t.taxId) LIKE :pattern OR " +
           "UPPER(t.name) LIKE :pattern")
    Page<TaxpayerEntity> searchByPattern(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Spring Data derived query for case-insensitive contains on number or name with paging
     */
    Page<TaxpayerEntity> findByTaxIdContainingIgnoreCaseOrNameContainingIgnoreCase(
            String taxId,
            String name,
            Pageable pageable);
    
    Optional<TaxpayerEntity> findByTaxId(String taxId);
    
    Optional<TaxpayerEntity> findByNationalId(String nationalId);
    
    @Query("SELECT t FROM TaxpayerEntity t WHERE UPPER(t.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<TaxpayerEntity> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /*@Query("SELECT t FROM TaxpayerEntity t WHERE " +
           "UPPER(t.taxpayerNo) LIKE :pattern OR " +
           "UPPER(t.name) LIKE :pattern")
    List<TaxpayerEntity> searchListByPattern(@Param("pattern") String pattern);*/

    /*@Query("SELECT t FROM TaxpayerEntity t WHERE " +
           "UPPER(t.taxpayerNo) LIKE UPPER(CONCAT('%', :taxpayerNo, '%')) OR " +
           "UPPER(t.name) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<TaxpayerEntity> findByTaxpayerNoContainingIgnoreCaseOrNameContainingIgnoreCase(
            @Param("taxpayerNo") String taxpayerNo,
            @Param("name") String name);*/

   /* List<TaxpayerEntity> findByBuyerTrue();

    Optional<TaxpayerEntity> findByBuyerTrueAndId(UUID id);*/


    /*@Query("SELECT t FROM TaxpayerEntity t WHERE t.buyer = true AND UPPER(t.buyerType) = UPPER(:buyerType)")
    List<TaxpayerEntity> findByBuyerTypeIgnoreCase(@Param("buyerType") String buyerType);*/

   /* @Query("SELECT t FROM TaxpayerEntity t WHERE t.buyer = true AND ( " +
           "(:query IS NULL OR :query = '') OR " +
           "UPPER(t.nationalId) LIKE UPPER(CONCAT('%', :query, '%')) OR " +
           "UPPER(t.name) LIKE UPPER(CONCAT('%', :query, '%')) )")
    Page<TaxpayerEntity> findBuyerPage(@Param("query") String query, Pageable pageable);*/
}



