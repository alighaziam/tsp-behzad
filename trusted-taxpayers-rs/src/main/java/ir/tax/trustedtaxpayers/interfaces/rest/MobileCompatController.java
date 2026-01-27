package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.application.service.TaxpayerService;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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

    private final TaxpayerService taxpayerService;
    private final SettingService settingService;

    public MobileCompatController(TaxpayerService taxpayerService, SettingService settingService) {
        this.taxpayerService = taxpayerService;
        this.settingService = settingService;
    }

    // Auth (demo parity)
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        if (username == null || password == null) {
            return Map.of("error", "username and password are required");
        }
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        return Map.of("token", "demo-token", "user", user);
    }

    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.equals("Bearer demo-token")) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        return ResponseEntity.ok(Map.of("username", "demo", "roles", List.of("ROLE_ADMIN")));
    }

    // Settings list (GET) similar to mobile `/api/settings`
    @GetMapping("/api/settings")
    public ResponseEntity<List<Map<String, Object>>> listSettings(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.equals("Bearer demo-token")) {
            return ResponseEntity.status(401).build();
        }
        List<SettingEntity> settings = settingService.findAll();
        return ResponseEntity.ok(
                settings.stream().map(this::toSettingDto).toList()
        );
    }

    // Create/Upsert setting (POST)
    @PostMapping("/api/settings")
    public ResponseEntity<Map<String, Object>> upsertSetting(@RequestHeader(value = "Authorization", required = false) String auth,
                                                             @RequestBody Map<String, Object> body) {
        if (auth == null || !auth.equals("Bearer demo-token")) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        Object keyObj = body.get("key");
        if (!(keyObj instanceof String) || ((String) keyObj).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "key is required"));
        }
        String key = (String) keyObj;
        String value = body.get("value") == null ? null : String.valueOf(body.get("value"));
        String groupTypeStr = body.get("group_type") == null ? "GENERAL" : String.valueOf(body.get("group_type"));
        Optional<SettingEntity> existing = settingService.findByKey(key);
        SettingEntity entity = existing.orElseGet(SettingEntity::new);
        entity.setKey(key);
        entity.setValue(value);
        try {
            SettingGroupType gt = SettingGroupType.valueOf(groupTypeStr.toUpperCase());
            entity.setGroupType(gt);
        } catch (Exception ignored) {
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

    // Taxpayers list with pagination + q search
    @GetMapping("/api/taxpayers")
    public ResponseEntity<Map<String, Object>> listTaxpayers(
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "DESC") String order,
            @RequestParam(required = false) String q
    ) {
        if (auth == null || !auth.equals("Bearer demo-token")) {
            return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        }
        int p = Math.max(1, page) - 1;
        int size = Math.min(200, Math.max(1, pageSize));
        Sort.Direction dir = "ASC".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProp = switch (sort) {
            case "national_id", "name", "is_active", "created_at" -> sort;
            default -> "id";
        };
        Pageable pageable = PageRequest.of(p, size, Sort.by(dir, sortProp));
        Page<TaxpayerEntity> result;
        if (q != null && !q.isBlank()) {
            var list = taxpayerService.search(q, size, p);
            result = new org.springframework.data.domain.PageImpl<>(list, pageable, list.size());
        } else {
            result = taxpayerService.findAll(pageable);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", result.getContent().stream().map(t -> Map.of(
                "id", t.getId(),
                "national_id", t.getTaxId(),
                "name", t.getName(),
                "is_active", true
        )).toList());
        resp.put("page", page);
        resp.put("pageSize", size);
        resp.put("total", result.getTotalElements());
        return ResponseEntity.ok(resp);
    }

    private Map<String, Object> toSettingDto(SettingEntity s) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", s.getKey());
        m.put("value", s.getValue());
        m.put("group_type", s.getGroupType() != null ? s.getGroupType().name() : null);
        return m;
    }
}


