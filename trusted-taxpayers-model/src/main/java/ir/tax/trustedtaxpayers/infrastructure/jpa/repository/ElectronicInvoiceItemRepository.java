package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceItemEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceItemRepository extends BaseRepository<ElectronicInvoiceItemEntity, UUID> {
    
    @EntityGraph(attributePaths = {"header"})
    List<ElectronicInvoiceItemEntity> findByHeaderId(UUID headerId);
    
    @EntityGraph(attributePaths = {"header"})
    List<ElectronicInvoiceItemEntity> findByHeaderIdOrderByLineNo(UUID headerId);
    
    @EntityGraph(attributePaths = {"header"})
    List<ElectronicInvoiceItemEntity> findBySstid(String sstid);
}



