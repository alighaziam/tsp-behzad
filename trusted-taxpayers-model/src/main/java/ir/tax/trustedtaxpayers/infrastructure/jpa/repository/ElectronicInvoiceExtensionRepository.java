package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceExtensionEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ElectronicInvoiceExtensionRepository extends BaseRepository<ElectronicInvoiceExtensionEntity, UUID> {
    List<ElectronicInvoiceExtensionEntity> findByHeaderId(UUID headerId);
    List<ElectronicInvoiceExtensionEntity> findByHeaderIdAndExtensionKey(UUID headerId, String extensionKey);
    List<ElectronicInvoiceExtensionEntity> findByExtensionKey(String extensionKey);
}



