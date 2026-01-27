package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceShippingEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceShippingRepository extends BaseRepository<ElectronicInvoiceShippingEntity, UUID> {
    Optional<ElectronicInvoiceShippingEntity> findByHeaderId(UUID headerId);
    List<ElectronicInvoiceShippingEntity> findByLno(String lno);
}



