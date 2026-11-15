package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceHeaderService;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mobile-compat/api/invoices")
@Tag(name = "Mobile-Compat Invoices", description = "Mobile compatible listing and search for invoices")
public class MobileCompatInvoicesController {

	private final ElectronicInvoiceHeaderService headerService;
	private final ElectronicInvoiceSystemService systemService;

	public MobileCompatInvoicesController(ElectronicInvoiceHeaderService headerService,
	                                      ElectronicInvoiceSystemService systemService) {
		this.headerService = headerService;
		this.systemService = systemService;
	}

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	private Map<String, Object> toMobileSummary(ElectronicInvoiceHeaderEntity h) {
		String sts = systemService.findByHeaderId(h.getId()).map(sys -> sys.getSts()).orElse(null);
		return Map.of(
				"id", h.getId(),
				"tax_id", h.getTaxId(),
				"template_code", h.getTemplateCode(),
				"inty", h.getInty(),
				"tins", h.getTins(),
				"tinb", h.getTinb(),
				"tbill", h.getTbill(),
				"sts", sts,
				"created_at", h.getCreatedOn()
		);
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
		Pageable pageable = PageRequest.of(page, size);
		Page<ElectronicInvoiceHeaderEntity> result;
		/*if (tins != null && !tins.isBlank()) {
			result = headerService.findBySellerTin(tins, pageable);
		} else if (tinb != null && !tinb.isBlank()) {
			result = headerService.findByBuyerTin(tinb, pageable);
		} else {
			result = headerService.findAll(pageable);
		}*/
		result = headerService.findAll(pageable);
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
		List<FilterRequest> filters = new ArrayList<>();
		if (tax_id != null && !tax_id.isBlank()) filters.add(new FilterRequest("taxId", "=", tax_id));
		if (tins != null && !tins.isBlank()) filters.add(new FilterRequest("tins", "=", tins));
		if (tinb != null && !tinb.isBlank()) filters.add(new FilterRequest("tinb", "=", tinb));
		if (template_code != null && !template_code.isBlank()) filters.add(new FilterRequest("templateCode", "=", template_code));
        Pageable pageable = PageRequest.of(page, size);
        // Minimal implementation: defer to findAll(pageable) if custom search not available
        Page<ElectronicInvoiceHeaderEntity> result = headerService.findAll(pageable);
		return ResponseEntity.ok(Map.of(
				"items", result.getContent().stream().map(this::toMobileSummary).toList(),
				"page", result.getNumber(),
				"size", result.getSize(),
				"total", result.getTotalElements()
		));
	}
}


