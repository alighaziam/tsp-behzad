package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceReferenceEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceReferenceRepository extends BaseRepository<ElectronicInvoiceReferenceEntity, UUID> {
    List<ElectronicInvoiceReferenceEntity> findByHeaderId(UUID headerId);
    List<ElectronicInvoiceReferenceEntity> findByIrtaxid(String irtaxid);
}



