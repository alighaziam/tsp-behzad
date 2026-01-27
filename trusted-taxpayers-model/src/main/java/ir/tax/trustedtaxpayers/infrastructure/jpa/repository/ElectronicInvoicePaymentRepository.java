package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoicePaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ElectronicInvoicePaymentRepository extends BaseRepository<ElectronicInvoicePaymentEntity, UUID> {
    List<ElectronicInvoicePaymentEntity> findByHeaderId(UUID headerId);
    List<ElectronicInvoicePaymentEntity> findByHeaderIdOrderByPdtDesc(UUID headerId);
    Page<ElectronicInvoicePaymentEntity> findByPdtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}



