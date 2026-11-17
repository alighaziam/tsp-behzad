package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Arbitrary outbound webhook dispatch is disabled until a controlled egress
 * proxy with destination pinning and redirect protection is available.
 */
@RestController
@RequestMapping("/mobile-compat/api/webhooks")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Mobile-Compat Webhooks", description = "Webhook dispatch is disabled pending secure egress controls")
public class WebhookDispatchController {

    public record DispatchRequest(@NotBlank String url, Map<String, Object> payload, String secret) {}

    @PostMapping("/dispatch")
    @Operation(summary = "Disabled: arbitrary webhook destinations are not accepted")
    public ResponseEntity<?> dispatch(@RequestBody DispatchRequest request) {
        return ResponseEntity.status(HttpStatus.GONE).body(Map.of(
                "error", "WEBHOOK_DISPATCH_DISABLED",
                "message", "Outbound dispatch requires an approved egress proxy and destination allow-list"
        ));
    }
}
