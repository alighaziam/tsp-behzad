package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.application.usecase.AssignRoleUseCase;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mobile-compat/api/user-roles")
@RequiredArgsConstructor
@Tag(name = "Mobile-Compat Roles", description = "Mobile compatible roles listing and assignment")
public class MobileCompatRolesController {

	private final UserRoleService userRoleService;
	private final AssignRoleUseCase assignRoleUseCase;

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	@GetMapping("/roles")
	@Operation(summary = "List all roles")
	public ResponseEntity<?> listRoles(@RequestHeader(value = "Authorization", required = false) String auth) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		List<RoleEntity> roles = userRoleService.getAllRoles();
		return ResponseEntity.ok(roles.stream().map(r -> Map.of(
				"id", r.getId(),
				"name", r.getName(),
				"description", r.getDescription()
		)).toList());
	}

	public record AssignRequest(@NotNull UUID user_id, @NotBlank String role) {}

	@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
	@PostMapping("/assign")
	@Operation(summary = "Assign role to user")
	public ResponseEntity<?> assignRole(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestBody AssignRequest req) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		assignRoleUseCase.execute(req.user_id(), req.role());
		return ResponseEntity.ok(Map.of("ok", true));
	}
}


