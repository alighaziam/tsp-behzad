package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * کنترلر سازگار با موبایل برای مدیریت قالب‌ها
 * 
 * @author Ghazi-a
 */
@RestController
@RequestMapping("/mobile-compat/api/templates")
@RequiredArgsConstructor
@Tag(name = "Mobile-Compat Templates", description = "CRUD سازگار با موبایل برای قالب‌ها")
public class MobileCompatTemplatesController {

	private final TemplateService templateService;

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	@GetMapping
	@Operation(summary = "لیست تمام قالب‌ها")
	public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String auth) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		List<TemplateEntity> all = templateService.findAll();
		return ResponseEntity.ok(all.stream().map(this::toDto).toList());
	}

	@GetMapping("/active")
	@Operation(summary = "لیست قالب‌های فعال")
	public ResponseEntity<?> listActive(@RequestHeader(value = "Authorization", required = false) String auth) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		List<TemplateEntity> actives = templateService.findAll().stream()
				.filter(TemplateEntity::getIsActive)
				.toList();
		return ResponseEntity.ok(actives.stream().map(this::toDto).toList());
	}

	public record CreateTemplateRequest(@NotBlank String code, @NotBlank String name, String active) {}

	@PostMapping
	@Operation(summary = "ایجاد قالب")
	public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String auth,
	                                @RequestBody CreateTemplateRequest req) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		TemplateEntity e = TemplateEntity.builder()
				.code(req.code())
				.title(req.name())
				.build();
		if (req.active() != null) {
			e.setIsActive("Y".equals(req.active()));
		}
		TemplateEntity saved = templateService.save(e);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.LOCATION, "/mobile-compat/api/templates/" + saved.getCode());
		return new ResponseEntity<>(toDto(saved), headers, HttpStatus.CREATED);
	}

	public record UpdateTemplateRequest(String name, String active) {}

	@PutMapping("/{code}")
	@Operation(summary = "به‌روزرسانی قالب بر اساس کد")
	public ResponseEntity<?> update(@RequestHeader(value = "Authorization", required = false) String auth,
	                                @PathVariable String code,
	                                @RequestBody UpdateTemplateRequest req) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		var entityOpt = templateService.findByCode(code);
		if (entityOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
		TemplateEntity e = entityOpt.get();
		if (req.name() != null) e.setTitle(req.name());
		if (req.active() != null) {
			e.setIsActive("Y".equals(req.active()));
		}
		TemplateEntity updated = templateService.save(e);
		return ResponseEntity.ok(toDto(updated));
	}

	@DeleteMapping("/{code}")
	@Operation(summary = "حذف قالب بر اساس کد")
	public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String auth,
	                                @PathVariable String code) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		var entityOpt = templateService.findByCode(code);
		if (entityOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
		templateService.deleteById(entityOpt.get().getId());
		return ResponseEntity.noContent().build();
	}

	private Map<String, Object> toDto(TemplateEntity e) {
		return Map.of(
				"code", e.getCode(),
				"name", e.getTitle(),
				"active", e.getIsActive() ? "Y" : "N"
		);
	}
}


