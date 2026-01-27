package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.spec.BaseSpecification;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.infrastructure.jpa.spec.SearchFilter;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenericSearchService<T> {
    
    public Page<T> search(JpaSpecificationExecutor<T> repository, 
                         List<ir.tax.trustedtaxpayers.application.dto.SearchRequest.Filter> requestFilters,
                         Integer page, Integer size, String sortBy, String direction) {
        
        // تبدیل Filterهای DTO به FilterRequest
        List<FilterRequest> filterRequests = new ArrayList<>();
        if (requestFilters != null) {
            filterRequests = requestFilters.stream()
                    .map(f -> {
                        FilterRequest filterRequest = new FilterRequest();
                        filterRequest.setKey(f.getKey());
                        filterRequest.setOperand(f.getOperand());
                        filterRequest.setValue(f.getValue());
                        return filterRequest;
                    })
                    .collect(Collectors.toList());
        }
        
        BaseSpecification<T> spec = new BaseSpecification<>(filterRequests);

        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (direction != null && direction.trim().equalsIgnoreCase("desc")) {
            sortDirection = Sort.Direction.DESC;
        }
        
        String validSortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "id";
        Sort sort = Sort.by(sortDirection, validSortBy);

        int validPage = page != null && page >= 0 ? page : 0;
        int validSize = size != null && size > 0 ? Math.min(size, 100) : 10;
        
        Pageable pageable = PageRequest.of(validPage, validSize, sort);

        return repository.findAll(spec, pageable);
    }
}


