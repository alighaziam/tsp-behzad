package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/mobile-compat/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Mobile-Compat Webhooks", description = "Dispatch a webhook with simple payload (for testing)")
public class WebhookDispatchController {

	private final RestTemplate restTemplate = new RestTemplate();

	private static boolean unauthorized(String auth) {
		return auth == null || !auth.equals("Bearer demo-token");
	}

	public record DispatchRequest(@NotBlank String url, Map<String, Object> payload, String secret) {}

	@PostMapping("/dispatch")
	@Operation(summary = "Dispatch webhook once with payload")
	public ResponseEntity<?> dispatch(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestBody DispatchRequest req) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (req.secret() != null && !req.secret().isBlank()) {
			headers.add("X-Webhook-Secret", req.secret());
		}
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(req.payload(), headers);
		var response = restTemplate.postForEntity(req.url(), entity, String.class);
		return ResponseEntity.status(response.getStatusCode()).body(Map.of(
				"status", response.getStatusCodeValue(),
				"body", response.getBody()
		));
	}
}


