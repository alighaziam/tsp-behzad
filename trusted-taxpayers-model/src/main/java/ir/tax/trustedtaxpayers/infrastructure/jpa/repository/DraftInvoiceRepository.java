package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.DraftInvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DraftInvoiceRepository extends JpaRepository<DraftInvoiceEntity, String> {
}


