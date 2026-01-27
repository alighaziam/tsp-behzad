package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftsQueryController {

    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final ElectronicInvoiceSystemRepository systemRepository;

    @Operation(summary = "لیست پیش‌نویس‌ها", description = "لیست صورتحساب‌های با وضعیت DRAFT را برمی‌گرداند")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<Page<ElectronicInvoiceSystemEntity>> listDrafts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        Page<ElectronicInvoiceSystemEntity> drafts = systemRepository.findBySts("DRAFT", pageable);
        return ResponseEntity.ok(drafts);
    }

    @Operation(summary = "دریافت پیش‌نویس", description = "جزئیات یک پیش‌نویس را بر اساس شناسه هدر برمی‌گرداند")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{headerId}")
    public ResponseEntity<?> getDraft(@PathVariable UUID headerId) {
        ElectronicInvoiceHeaderEntity header = headerRepository.findById(headerId)
                .orElse(null);
        if (header == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        ElectronicInvoiceSystemEntity sys = systemRepository.findByHeaderId(headerId).orElse(null);
        return ResponseEntity.ok(Map.of(
                "header", header,
                "system", sys
        ));
    }

    @Operation(summary = "به‌روزرسانی ساده پیش‌نویس", description = "بخشی از فیلدهای سربرگ پیش‌نویس را به‌روزرسانی می‌کند (inno, tins, tinb, tbill)")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{headerId}")
    public ResponseEntity<?> updateDraft(@PathVariable UUID headerId, @RequestBody Map<String, Object> body) {
        ElectronicInvoiceHeaderEntity header = headerRepository.findById(headerId)
                .orElse(null);
        if (header == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (body.containsKey("inno")) {
            header.setInno(String.valueOf(body.get("inno")));
        }
        if (body.containsKey("tins")) {
            header.setTins(String.valueOf(body.get("tins")));
        }
        if (body.containsKey("tinb")) {
            header.setTinb(String.valueOf(body.get("tinb")));
        }
        if (body.containsKey("tbill")) {
            try {
                header.setTbill(new java.math.BigDecimal(String.valueOf(body.get("tbill"))));
            } catch (Exception ignored) {}
        }
        headerRepository.save(header);
        ElectronicInvoiceSystemEntity sys = systemRepository.findByHeaderId(headerId).orElse(null);
        return ResponseEntity.ok(Map.of("header", header, "system", sys));
    }
}



