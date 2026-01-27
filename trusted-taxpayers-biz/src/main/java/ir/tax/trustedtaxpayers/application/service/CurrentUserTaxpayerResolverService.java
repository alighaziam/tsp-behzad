package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserTaxpayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for resolving current user's associated taxpayers
 * این سرویس برای پیدا کردن مودیان مرتبط با کاربر جاری استفاده می‌شود
 * 
 * @author System
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentUserTaxpayerResolverService {

    private final UserTaxpayerRepository userTaxpayerRepository;
    private final UserRoleService userRoleService;

    /**
     * Resolves the current authenticated user's ID from security context
     * 
     * @return Optional containing user ID if authenticated, empty otherwise
     */
    public Optional<UUID> resolveCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authenticated user found in security context");
                return Optional.empty();
            }

            String username = authentication.getName();
            if (username == null || username.isBlank()) {
                log.debug("Username is null or blank");
                return Optional.empty();
            }

            return userRoleService.findUserIdByUsername(username);
        } catch (Exception e) {
            log.error("Error resolving current user ID", e);
            return Optional.empty();
        }
    }

    /**
     * Finds all taxpayer IDs associated with the current user
     * 
     * @return Set of taxpayer IDs, empty if user not found or has no taxpayers
     */
    @Transactional(readOnly = true)
    public Set<UUID> findCurrentUserTaxpayerIds() {
        return resolveCurrentUserId()
                .map(this::findTaxpayerIdsByUserId)
                .orElse(Set.of());
    }

    /**
     * Finds the primary (first) taxpayer ID for the current user
     * برای کاربران TAXPAYER، اولین مودی را برمی‌گرداند
     * 
     * @return Optional containing primary taxpayer ID if found
     */
    @Transactional(readOnly = true)
    public Optional<UUID> findCurrentUserPrimaryTaxpayerId() {
        Set<UUID> taxpayerIds = findCurrentUserTaxpayerIds();
        if (taxpayerIds.isEmpty()) {
            log.debug("No taxpayers found for current user");
            return Optional.empty();
        }
        
        // Return the first taxpayer ID
        UUID primaryTaxpayerId = taxpayerIds.iterator().next();
        log.debug("Primary taxpayer ID for current user: {}", primaryTaxpayerId);
        return Optional.of(primaryTaxpayerId);
    }

    /**
     * Finds all taxpayer IDs associated with a specific user ID
     * 
     * @param userId User ID to find taxpayers for
     * @return Set of taxpayer IDs
     */
    @Transactional(readOnly = true)
    public Set<UUID> findTaxpayerIdsByUserId(UUID userId) {
        if (userId == null) {
            log.warn("User ID is null");
            return Set.of();
        }

        return userTaxpayerRepository.findByUserId(userId).stream()
                .map(UserTaxpayerEntity::getTaxpayerId)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks if the current user has access to a specific taxpayer
     * 
     * @param taxpayerId Taxpayer ID to check
     * @return true if user has access, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToTaxpayer(UUID taxpayerId) {
        if (taxpayerId == null) {
            return false;
        }
        
        Set<UUID> accessibleTaxpayers = findCurrentUserTaxpayerIds();
        boolean hasAccess = accessibleTaxpayers.contains(taxpayerId);
        log.debug("User access to taxpayer {}: {}", taxpayerId, hasAccess);
        return hasAccess;
    }
}












