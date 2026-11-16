package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.service.UserTaxpayerManagementService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Administrative management of trusted user-to-taxpayer assignments. */
@RestController
@RequestMapping("/api/users/{userId}/taxpayers")
@PreAuthorize("hasAuthority('ADMIN')")
public class UserTaxpayerController {
    private final UserTaxpayerManagementService service;

    public UserTaxpayerController(UserTaxpayerManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AssociationResponse>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.findByUserId(userId).stream()
                .map(item -> new AssociationResponse(item.getTaxpayerId(), item.getRelationRole()))
                .toList());
    }

    @PutMapping("/{taxpayerId}")
    public ResponseEntity<Void> assign(@PathVariable UUID userId,
                                        @PathVariable UUID taxpayerId,
                                        @Valid @RequestBody(required = false) AssignRequest request) {
        service.assign(userId, taxpayerId, request == null ? "OWNER" : request.relationRole());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taxpayerId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID userId, @PathVariable UUID taxpayerId) {
        service.revoke(userId, taxpayerId);
        return ResponseEntity.noContent().build();
    }

    public record AssignRequest(
            @Pattern(regexp = "(?i)OWNER|ACCOUNTANT|VIEWER") String relationRole) {}

    public record AssociationResponse(UUID taxpayerId, String relationRole) {}
}
