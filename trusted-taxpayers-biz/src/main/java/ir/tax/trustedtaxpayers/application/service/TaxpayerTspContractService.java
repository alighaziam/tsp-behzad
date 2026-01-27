package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTspContractEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TrustedServiceProviderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerTspContractRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TrustedServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TaxpayerTspContractService {

    private final TaxpayerRepository taxpayerRepository;
    private final TrustedServiceProviderRepository providerRepository;
    private final TaxpayerTspContractRepository contractRepository;

    @Transactional(readOnly = true)
    public Optional<TaxpayerTspContractEntity> findActiveContract(UUID taxpayerId) {
        return contractRepository.findActiveByTaxpayer(taxpayerId);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerTspContractEntity> findContracts(UUID taxpayerId) {
        return contractRepository.findByTaxpayerId(taxpayerId);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerTspContractEntity> findAllContracts() {
        return contractRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TaxpayerTspContractEntity> findById(UUID contractId) {
        return contractRepository.findById(contractId);
    }

    @Transactional
    public TaxpayerTspContractEntity createContract(UUID taxpayerId,
                                                    UUID providerId,
                                                    String contractNumber,
                                                    String contractTitle,
                                                    LocalDate signedAt,
                                                    LocalDate expiresAt,
                                                    String status,
                                                    String notes) {
        TaxpayerEntity taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "مودی یافت نشد."));
        TrustedServiceProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "شرکت معتمد یافت نشد."));

        if (signedAt == null) {
            throw new ResponseStatusException(BAD_REQUEST, "تاریخ عقد قرارداد الزامی است.");
        }

        if (expiresAt != null && expiresAt.isBefore(signedAt)) {
            throw new ResponseStatusException(BAD_REQUEST, "تاریخ پایان نمی‌تواند قبل از تاریخ شروع باشد.");
        }

        findActiveContract(taxpayerId).ifPresent(existing -> {
            existing.setIsActive(false);
            existing.setStatus("ARCHIVED");
            contractRepository.save(existing);
        });

        TaxpayerTspContractEntity contract = TaxpayerTspContractEntity.builder()
                .taxpayer(taxpayer)
                .provider(provider)
                .contractNumber(contractNumber)
                .contractTitle(contractTitle)
                .signedAt(signedAt)
                .expiresAt(expiresAt)
                .status(status != null ? status : "ACTIVE")
                .notes(notes)
                .build();

        return contractRepository.save(contract);
    }

    @Transactional
    public void deactivateContract(UUID contractId) {
        TaxpayerTspContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "قرارداد یافت نشد."));
        contract.setIsActive(false);
        contract.setStatus("INACTIVE");
        contractRepository.save(contract);
    }

    @Transactional
    public TaxpayerTspContractEntity updateContract(UUID contractId,
                                                    String contractNumber,
                                                    String contractTitle,
                                                    LocalDate signedAt,
                                                    LocalDate expiresAt,
                                                    String status,
                                                    String notes,
                                                    Boolean active) {
        TaxpayerTspContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "قرارداد یافت نشد."));

        if (contractNumber != null) {
            contract.setContractNumber(contractNumber);
        }
        if (contractTitle != null) {
            contract.setContractTitle(contractTitle);
        }
        if (signedAt != null) {
            if (contract.getExpiresAt() != null && contract.getExpiresAt().isBefore(signedAt)) {
                throw new ResponseStatusException(BAD_REQUEST, "تاریخ پایان نمی‌تواند قبل از تاریخ شروع باشد.");
            }
            contract.setSignedAt(signedAt);
        }
        if (expiresAt != null) {
            LocalDate effectiveSignedAt = signedAt != null ? signedAt : contract.getSignedAt();
            if (effectiveSignedAt != null && expiresAt.isBefore(effectiveSignedAt)) {
                throw new ResponseStatusException(BAD_REQUEST, "تاریخ پایان نمی‌تواند قبل از تاریخ شروع باشد.");
            }
            contract.setExpiresAt(expiresAt);
        }
        if (status != null) {
            contract.setStatus(status);
        }
        if (notes != null) {
            contract.setNotes(notes);
        }
        if (active != null) {
            contract.setIsActive(active);
        }
        return contractRepository.save(contract);
    }

    @Transactional
    public TaxpayerTspContractEntity approveContract(UUID contractId, String status, String approvalNote) {
        TaxpayerTspContractEntity contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "قرارداد یافت نشد."));
        contract.setIsActive(true);
        contract.setStatus(status != null && !status.isBlank() ? status : "APPROVED");
        if (approvalNote != null && !approvalNote.isBlank()) {
            contract.setNotes(approvalNote);
        }
        return contractRepository.save(contract);
    }

    @Transactional(readOnly = true)
    public List<TaxpayerTspContractEntity> findExpiringContracts(int withinDays, boolean includeExpired) {
        if (withinDays < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "بازه زمانی منفی مجاز نیست.");
        }

        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(withinDays);

        List<TaxpayerTspContractEntity> upcoming = contractRepository.findByIsActiveTrueAndExpiresAtBetween(today, threshold);
        if (!includeExpired) {
            return upcoming;
        }
        List<TaxpayerTspContractEntity> expired = contractRepository.findByIsActiveTrueAndExpiresAtBefore(today);
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        List<TaxpayerTspContractEntity> combined = new java.util.ArrayList<>();
        upcoming.forEach(c -> {
            combined.add(c);
            seen.add(c.getId());
        });
        expired.stream()
                .filter(c -> c.getExpiresAt() != null && !seen.contains(c.getId()))
                .forEach(combined::add);
        combined.sort(java.util.Comparator.comparing(
                TaxpayerTspContractEntity::getExpiresAt,
                java.util.Comparator.nullsLast(java.time.LocalDate::compareTo)));
        return combined;
    }
}


