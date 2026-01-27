package ir.tax.trustedtaxpayers.infrastructure.jpa.adapter;

import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.application.SearchResult;
import ir.tax.trustedtaxpayers.beans.model.UserSummary;
import ir.tax.trustedtaxpayers.model.ports.UserQueryPort;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserRepository;
import ir.tax.trustedtaxpayers.core.spec.BaseSpecification;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaUserQueryAdapter implements UserQueryPort {
    private final UserRepository userRepository;

    public JpaUserQueryAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public SearchResult<UserSummary> search(List<FilterCriterionDto> filters, int page, int size, String sortBy, boolean ascending) {
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, validSortField(sortBy));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        java.util.List<FilterRequest> infraFilters;
        if (filters == null) {
            infraFilters = java.util.Collections.emptyList();
        } else {
            infraFilters = filters.stream().map(f -> {
                FilterRequest fr = new FilterRequest();
                fr.setKey(f.field());
                fr.setOperand(f.operand());
                fr.setValue(f.value());
                return fr;
            }).toList();
        }
        BaseSpecification<UserEntity> spec = new BaseSpecification<>(infraFilters);
        Page<UserEntity> p = userRepository.findAll(spec, pageable);
        List<UserSummary> items = p.getContent().stream()
                .map(u -> new UserSummary(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getStatus(),
                        u.getIsActive()))
                .toList();
        return new SearchResult<>(items, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    private String validSortField(String sortBy) {
        if ("username".equalsIgnoreCase(sortBy)) return "username";
        if ("email".equalsIgnoreCase(sortBy)) return "email";
        return "username";
    }
}



