package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.service.CurrentUserTaxpayerResolverService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Self-service endpoint exposing only taxpayers explicitly assigned to the authenticated account. */
@RestController
@RequestMapping("/api/me/taxpayers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyTaxpayersController {
    private final CurrentUserTaxpayerResolverService accessResolver;
    private final TaxpayerRepository taxpayerRepository;

    @GetMapping
    public ResponseEntity<List<MyTaxpayerResponse>> listMyTaxpayers() {
        Set<UUID> ids = accessResolver.findCurrentUserTaxpayerIds();
        if (ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<MyTaxpayerResponse> response = taxpayerRepository.findAllById(ids).stream()
                .map(t -> new MyTaxpayerResponse(t.getId(), t.getTaxpayerNo(), t.getName()))
                .toList();
        return ResponseEntity.ok(response);
    }

    public record MyTaxpayerResponse(UUID id, String taxpayerNo, String name) {}
}
