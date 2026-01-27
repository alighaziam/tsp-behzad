package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.SubscriptionPlanRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final TaxpayerSubscriptionRepository subscriptionRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ServiceAccessService serviceAccessService;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanEntity> getActivePlans() {
        return planRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionPlanEntity> getPlanById(UUID planId) {
        return planRepository.findById(planId);
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionPlanEntity> getPlanByCode(String code) {
        return planRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerSubscriptionEntity> getTaxpayerSubscriptions(UUID taxpayerId) {
        return subscriptionRepository.findByTaxpayerId(taxpayerId);
    }

    @Transactional(readOnly = true)
    public Optional<TaxpayerSubscriptionEntity> getActiveSubscription(UUID taxpayerId) {
        return subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
    }

    @Transactional
    public TaxpayerSubscriptionEntity purchaseSubscription(
            UUID taxpayerId,
            UUID planId,
            String paymentReference,
            String notes) {
        
        TaxpayerEntity taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new ResourceNotFoundException("مودی یافت نشد."));
        
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("پلن اشتراک یافت نشد."));

        // Deactivate existing active subscriptions
        Optional<TaxpayerSubscriptionEntity> existingActive = 
                subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
        if (existingActive.isPresent()) {
            TaxpayerSubscriptionEntity existing = existingActive.get();
            existing.setStatus("EXPIRED");
            subscriptionRepository.save(existing);
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(plan.getDurationDays());

        TaxpayerSubscriptionEntity subscription = TaxpayerSubscriptionEntity.builder()
                .taxpayer(taxpayer)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status("ACTIVE")
                .remainingInvoices(plan.getMaxInvoices())
                .paymentReference(paymentReference)
                .notes(notes)
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public void decrementRemainingInvoices(UUID subscriptionId) {
        TaxpayerSubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("اشتراک یافت نشد."));
        
        if (subscription.getRemainingInvoices() != null && subscription.getRemainingInvoices() > 0) {
            subscription.setRemainingInvoices(subscription.getRemainingInvoices() - 1);
            subscriptionRepository.save(subscription);
        }
    }

    @Transactional
    public void updateSubscriptionStatus(UUID subscriptionId, String status) {
        TaxpayerSubscriptionEntity subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("اشتراک یافت نشد."));
        
        subscription.setStatus(status);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerSubscriptionEntity> getExpiringSubscriptions(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(withinDays);
        return subscriptionRepository.findExpiringSubscriptions(today, endDate);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerSubscriptionEntity> getExpiredSubscriptions() {
        return subscriptionRepository.findExpiredSubscriptions(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getPlanServices(UUID planId) {
        return serviceAccessService.getServicesForPlan(planId);
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getTaxpayerServices(UUID taxpayerId) {
        return serviceAccessService.getServicesForTaxpayer(taxpayerId);
    }

    @Transactional(readOnly = true)
    public boolean hasTaxpayerServiceAccess(UUID taxpayerId, String serviceCode) {
        return serviceAccessService.hasServiceAccess(taxpayerId, serviceCode);
    }
}

