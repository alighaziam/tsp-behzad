package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserInvoicePreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInvoicePreferenceRepository extends JpaRepository<UserInvoicePreferenceEntity, UUID> {

    List<UserInvoicePreferenceEntity> findByUserId(UUID userId);

    Optional<UserInvoicePreferenceEntity> findByUserIdAndTaxpayerId(UUID userId, UUID taxpayerId);
}

