package ir.tax.trustedtaxpayers.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.DraftInvoiceService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.DraftInvoiceEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping({"/api/drafts", "/api/v1/drafts"})
@Tag(name = "Draft Invoices", description = "مدیریت پیش‌نویس‌های صورتحساب")
@Validated
@RequiredArgsConstructor
public class DraftInvoiceController {
    private final DraftInvoiceService service;
    private final ObjectMapper objectMapper;

    @Data
    public static class DraftCreateRequest {
        @NotBlank
        private String source;
        private String rawText;
        @Valid
        private Map<String, Object> parsed;
    }

    @Operation(summary = "ایجاد پیش‌نویس")
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody DraftCreateRequest req, Principal principal) {
        String owner = principal != null ? principal.getName() : null;
        DraftInvoiceEntity e = service.create(owner, req.getSource(),  req.getParsed());
        return ResponseEntity.ok(Map.of("id", e.getId(), "status", e.getStatus()));
    }

    @Operation(summary = "دریافت پیش‌نویس")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id) throws Exception {
        DraftInvoiceEntity e = service.get(id).orElseThrow();
        Map parsed = e.getParsedJson() != null ? objectMapper.readValue(e.getParsedJson(), Map.class) : Map.of();
        return ResponseEntity.ok(Map.of(
                "id", e.getId(),
                "ownerUserId", e.getOwnerUserId(),
                "source", e.getSource(),
                "status", e.getStatus(),
                "parsed", parsed
        ));
    }

    @Operation(summary = "به‌روزرسانی پیش‌نویس")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        DraftInvoiceEntity e = service.update(id, body);
        return ResponseEntity.ok(Map.of("id", e.getId(), "status", e.getStatus()));
    }
}


