package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.OutboxDispatcher;
import ir.tax.trustedtaxpayers.biz.service.OutboxService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.OutboxMessageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.OutboxMessageRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mobile-compat/api/outbox")
@RequiredArgsConstructor
@Tag(name = "Outbox & Webhooks", description = "Outbox message logging and dispatch helpers for mobile")
public class OutboxController {

	private final OutboxService outboxService;
	private final java.util.Optional<OutboxDispatcher> outboxDispatcher;
	private final OutboxMessageRepository outboxRepository;

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	public record EnqueueRequest(
			@NotBlank String aggregate_type,
			@NotBlank String aggregate_id,
			@NotBlank String event_type,
			Map<String, Object> payload,
			Map<String, Object> headers
	) {}

	@PostMapping("/messages")
	@Operation(summary = "Enqueue outbox message")
	public ResponseEntity<?> enqueue(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestBody EnqueueRequest req) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		UUID id = outboxService.enqueue(req.aggregate_type(), req.aggregate_id(), req.event_type(), req.payload(), req.headers());
		return ResponseEntity.ok(Map.of("id", id));
	}

	@GetMapping("/messages")
	@Operation(summary = "List outbox messages")
	public ResponseEntity<?> list(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String status) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		Pageable pageable = PageRequest.of(page, size);
        Page<OutboxMessageEntity> result = outboxRepository.findAll(pageable);
		return ResponseEntity.ok(Map.of(
				"items", result.getContent(),
				"page", result.getNumber(),
				"size", result.getSize(),
				"total", result.getTotalElements()
		));
	}

	@PostMapping("/dispatch")
	@Operation(summary = "Trigger dispatcher once (immediate)")
	public ResponseEntity<?> dispatchNow(@RequestHeader(value = "Authorization", required = false) String auth) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		if (outboxDispatcher.isEmpty()) {
			return ResponseEntity.status(501).body(Map.of("error", "dispatcher_unavailable"));
		}
		outboxDispatcher.get().dispatch();
		return ResponseEntity.ok(Map.of("ok", true));
	}
}


