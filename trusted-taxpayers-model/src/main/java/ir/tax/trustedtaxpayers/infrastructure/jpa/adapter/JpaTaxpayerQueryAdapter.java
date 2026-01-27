package ir.tax.trustedtaxpayers.infrastructure.jpa.adapter;

import ir.tax.trustedtaxpayers.application.SearchResult;
import ir.tax.trustedtaxpayers.beans.model.Taxpayer;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.model.ports.TaxpayerQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JpaTaxpayerQueryAdapter implements TaxpayerQueryPort {
    private final TaxpayerRepository taxpayerRepository;

    public JpaTaxpayerQueryAdapter(TaxpayerRepository taxpayerRepository) {
        this.taxpayerRepository = taxpayerRepository;
    }

    @Override
    public SearchResult<Taxpayer> searchByQuery(String queryText, int page, int size, String sortBy, boolean ascending) {
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, validSortField(sortBy));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        // Prefer Spring Data derived query for portability across DBs (handles case-insensitive contains)
        Page<TaxpayerEntity> p = taxpayerRepository
                .findByTaxIdContainingIgnoreCaseOrNameContainingIgnoreCase(queryText, queryText, pageable);
        List<Taxpayer> items = p.getContent().stream()
                .map(e -> new Taxpayer(e.getId(), e.getTaxId(), e.getName()))
                .toList();
        return new SearchResult<>(items, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    private String validSortField(String sortBy) {
        if ("taxpayerNo".equalsIgnoreCase(sortBy)) return "taxpayerNo";
        if ("name".equalsIgnoreCase(sortBy)) return "name";
        return "name";
    }
}



