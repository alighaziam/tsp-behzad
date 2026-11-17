package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceHeaderService;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mobile-compat/api/invoices")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Mobile-Compat Invoices", description = "Mobile compatible listing and search for invoices")
public class MobileCompatInvoicesController {

	private final ElectronicInvoiceHeaderService headerService;
	private final ElectronicInvoiceSystemService systemService;
	private final ElectronicInvoiceHeaderRepository headerRepository;

	public MobileCompatInvoicesController(ElectronicInvoiceHeaderService headerService,
	                                      ElectronicInvoiceSystemService systemService,
	                                      ElectronicInvoiceHeaderRepository headerRepository) {
		this.headerService = headerService;
		this.systemService = systemService;
		this.headerRepository = headerRepository;
	}

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	private Map<String, Object> toMobileSummary(ElectronicInvoiceHeaderEntity h) {
		String sts = systemService.findByHeaderId(h.getId()).map(sys -> sys.getSts()).orElse(null);
		Map<String, Object> summary = new HashMap<>();
		summary.put("id", h.getId());
		summary.put("tax_id", h.getTaxId());
		summary.put("template_code", h.getTemplateCode());
		summary.put("inty", h.getInty());
		summary.put("tins", h.getTins());
		summary.put("tinb", h.getTinb());
		summary.put("tbill", h.getTbill());
		summary.put("sts", sts);
		summary.put("created_at", h.getCreatedOn());
		return summary;
	}

	@GetMapping
	@Operation(summary = "List invoices by seller or buyer with paging")
	public ResponseEntity<?> list(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestParam(required = false) String tins,
			@RequestParam(required = false) String tinb,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		int safePage = Math.max(0, page);
		int safeSize = Math.min(100, Math.max(1, size));
		Pageable pageable = PageRequest.of(safePage, safeSize);
		boolean hasTins = tins != null && !tins.isBlank();
		boolean hasTinb = tinb != null && !tinb.isBlank();
		Page<ElectronicInvoiceHeaderEntity> result;
		if (hasTins && hasTinb) {
			result = headerRepository.findByTinsAndTinb(tins.trim(), tinb.trim(), pageable);
		} else if (hasTins) {
			result = headerRepository.findByTins(tins.trim(), pageable);
		} else if (hasTinb) {
			result = headerRepository.findByTinb(tinb.trim(), pageable);
		} else {
			result = headerService.findAll(pageable);
		}
		return ResponseEntity.ok(Map.of(
				"items", result.getContent().stream().map(this::toMobileSummary).toList(),
				"page", result.getNumber(),
				"size", result.getSize(),
				"total", result.getTotalElements()
		));
	}

	@GetMapping("/search")
	@Operation(summary = "Search invoices by filters (tax_id, tins, tinb, template_code)")
	public ResponseEntity<?> search(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestParam(required = false) String tax_id,
			@RequestParam(required = false) String tins,
			@RequestParam(required = false) String tinb,
			@RequestParam(required = false) String template_code,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
		Page<ElectronicInvoiceHeaderEntity> result = headerRepository.findForMobileSearch(
				normalize(tax_id), normalize(tins), normalize(tinb), normalize(template_code), pageable);
		return ResponseEntity.ok(Map.of(
				"items", result.getContent().stream().map(this::toMobileSummary).toList(),
				"page", result.getNumber(),
				"size", result.getSize(),
				"total", result.getTotalElements()
		));
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}


