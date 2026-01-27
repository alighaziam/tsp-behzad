package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.externalService.integration.TaxpayerSystemClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "Draft Invoices", description = "مدیریت پیش‌نویس‌ها و ارسال به سامانه مودیان")
@RequestMapping("/api/drafts")
public class DraftsController {

	private final ElectronicInvoiceHeaderRepository invoiceHeaderRepository;
	private final TaxpayerSystemClient taxpayerSystemClient;

	@Operation(
			summary = "ارسال پیش‌نویس به سامانه مودیان",
			description = "پیش‌نویس با شناسه سربرگ (headerId) را به سامانه ارسال می‌کند.",
			responses = {
					@ApiResponse(responseCode = "200", description = "ارسال انجام شد", content = @Content(schema = @Schema())),
					@ApiResponse(responseCode = "404", description = "یافت نشد", content = @Content),
			}
	)
	@PostMapping("/{headerId}/submit")
	public ResponseEntity<?> submitDraft(@PathVariable("headerId") UUID headerId) {
		ElectronicInvoiceHeaderEntity header = invoiceHeaderRepository.findById(headerId)
				.orElse(null);
		if (header == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "Invoice header not found", "headerId", String.valueOf(headerId)));
		}
		var response = taxpayerSystemClient.submitInvoice(header);
		return ResponseEntity.ok(Map.of(
				"status", response.getStatus(),
				"message", response.getMessage(),
				"headerId", String.valueOf(headerId)
		));
	}
}

