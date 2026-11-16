package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserTaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTaxpayerManagementService {
    private static final List<String> ALLOWED_ROLES = List.of("OWNER", "ACCOUNTANT", "VIEWER");

    private final UserRepository userRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final UserTaxpayerRepository userTaxpayerRepository;

    @Transactional
    public void assign(UUID userId, UUID taxpayerId, String relationRole) {
        if (userId == null || taxpayerId == null) {
            throw new IllegalArgumentException("User ID and taxpayer ID are required");
        }
        String normalizedRole = relationRole == null || relationRole.isBlank()
                ? "OWNER" : relationRole.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Unsupported taxpayer relation role");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر مورد نظر یافت نشد."));
        var taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new ResourceNotFoundException("مودی مورد نظر یافت نشد."));
        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Cannot assign a taxpayer to an inactive user");
        }
        if (!Boolean.TRUE.equals(taxpayer.getIsActive())) {
            throw new IllegalStateException("Cannot assign an inactive taxpayer");
        }

        UserTaxpayerEntity association = userTaxpayerRepository
                .findByUserIdAndTaxpayerId(userId, taxpayerId)
                .orElseGet(() -> {
                    UserTaxpayerEntity created = new UserTaxpayerEntity();
                    created.setUserId(userId);
                    created.setTaxpayerId(taxpayerId);
                    return created;
                });
        association.setRelationRole(normalizedRole);
        userTaxpayerRepository.save(association);
    }

    @Transactional
    public void revoke(UUID userId, UUID taxpayerId) {
        userTaxpayerRepository.findByUserIdAndTaxpayerId(userId, taxpayerId)
                .ifPresent(userTaxpayerRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<UserTaxpayerEntity> findByUserId(UUID userId) {
        return userTaxpayerRepository.findAllByUserId(userId);
    }
}
