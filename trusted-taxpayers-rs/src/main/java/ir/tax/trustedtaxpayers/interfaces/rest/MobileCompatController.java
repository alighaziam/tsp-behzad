package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.security.JwtService;
import ir.tax.trustedtaxpayers.application.rate.RateLimited;
import org.springframework.security.access.prepost.PreAuthorize;
import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.application.service.UserAuthenticationService;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/mobile-compat")
@Validated
public class MobileCompatController {

    private static final long ACCESS_TOKEN_TTL_MS = 3_600_000L;

    private final TaxpayerRepository taxpayerRepository;
    private final SettingService settingService;
    private final UserAuthenticationService authenticationService;
    private final JwtService jwtService;

    public MobileCompatController(TaxpayerRepository taxpayerRepository,
                                  SettingService settingService,
                                  UserAuthenticationService authenticationService,
                                  JwtService jwtService) {
        this.taxpayerRepository = taxpayerRepository;
        this.settingService = settingService;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    /**
     * Compatibility login delegates to the same credential verification and
     * token issuer used by the canonical authentication API.
     */
    @RateLimited(perMinute = 5)
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        Object usernameValue = body.get("username");
        Object passwordValue = body.get("password");
        if (!(usernameValue instanceof String username) || username.isBlank()
                || !(passwordValue instanceof String password) || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
        }

        var result = authenticationService.authenticate(username, password);
        String token = jwtService.generate(result.getUsername(), ACCESS_TOKEN_TTL_MS,
                Map.of("roles", result.getRoles()));
        return ResponseEntity.ok(Map.of(
                "token", token,
                "access_token", token,
                "token_type", "Bearer",
                "user", Map.of("username", result.getUsername())
        ));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!MobileCompatSecurity.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority()).toList()
        ));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/api/settings")
    public ResponseEntity<List<Map<String, Object>>> listSettings(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        if (!MobileCompatSecurity.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        List<SettingEntity> settings = settingService.findAll();
        return ResponseEntity.ok(settings.stream().map(this::toSettingDto).toList());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/api/settings")
    public ResponseEntity<Map<String, Object>> upsertSetting(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody Map<String, Object> body) {
        if (!MobileCompatSecurity.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        Object keyObj = body.get("key");
        if (!(keyObj instanceof String key) || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "key is required"));
        }
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        String groupTypeStr = body.get("group_type") == null ? "GENERAL" : String.valueOf(body.get("group_type"));
        Optional<SettingEntity> existing = settingService.findByKey(key);
        SettingEntity entity = existing.orElseGet(SettingEntity::new);
        entity.setKey(key);
        entity.setValue(value);
        try {
            entity.setGroupType(SettingGroupType.valueOf(groupTypeStr.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            entity.setGroupType(SettingGroupType.GENERAL);
        }
        entity.setUpdatedOn(OffsetDateTime.now());
        entity.setUpdatedBy("MOBILE_COMPAT");
        if (entity.getCreatedOn() == null) {
            entity.setCreatedOn(OffsetDateTime.now());
            entity.setCreatedBy("MOBILE_COMPAT");
        }
        settingService.save(entity);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/api/taxpayers")
    public ResponseEntity<Map<String, Object>> listTaxpayers(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String q) {
        if (!MobileCompatSecurity.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        int safePage = Math.max(1, page) - 1;
        int safeSize = Math.min(100, Math.max(1, pageSize));
        Sort.Direction direction = "ASC".equalsIgnoreCase(order)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = switch (sort) {
            case "national_id" -> "nationalId";
            case "name" -> "name";
            case "is_active" -> "isActive";
            case "created_at" -> "createdOn";
            default -> "id";
        };
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortProperty));
        String query = q == null || q.isBlank() ? null : q.trim();
        Page<TaxpayerEntity> result = taxpayerRepository.findForMobileSearch(query, pageable);

        List<Map<String, Object>> items = result.getContent().stream().map(t -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", t.getId());
            item.put("national_id", t.getNationalId());
            item.put("taxpayer_no", t.getTaxId());
            item.put("name", t.getName());
            item.put("is_active", Boolean.TRUE.equals(t.getIsActive()));
            return item;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("page", result.getNumber() + 1);
        response.put("pageSize", result.getSize());
        response.put("total", result.getTotalElements());
        return ResponseEntity.ok(response);
    }


    private Map<String, Object> toSettingDto(SettingEntity s) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", s.getKey());
        m.put("value", s.getValue());
        m.put("group_type", s.getGroupType() != null ? s.getGroupType().name() : null);
        return m;
    }
}
