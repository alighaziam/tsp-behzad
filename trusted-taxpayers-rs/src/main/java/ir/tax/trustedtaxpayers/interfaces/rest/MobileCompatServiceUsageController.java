package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.AuditLogRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mobile-compat/api/service-usage")
@org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Mobile-Compat Service Usage Reports", description = "Summary and simple trend for service access/usage")
public class MobileCompatServiceUsageController {

	private final ServiceUsageRepository serviceUsageRepository;
	private final AuditLogRepository auditLogRepository;

	private static boolean unauthorized(String auth) {
        return !MobileCompatSecurity.isAuthenticated();
    }

	@GetMapping("/summary")
	@Operation(summary = "Summary counts for access/usage")
	public ResponseEntity<?> summary(
			@RequestHeader(value = "Authorization", required = false) String auth,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
		if (unauthorized(auth)) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
		if (startDate == null) startDate = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
		if (endDate == null) endDate = LocalDate.now();
		OffsetDateTime start = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
		OffsetDateTime endExclusive = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
		List<AuditLogEntity> accessLogs = auditLogRepository.findByEntityNameAndCreatedOnRange(
				"SERVICE_ACCESS", start, endExclusive);
		long totalAccess = accessLogs.size();
		long totalSuccessfulAccess = accessLogs.stream()
				.filter(l -> l.getAction() != null && l.getAction().contains("GRANTED")).count();
		long totalFailedAccess = accessLogs.stream()
				.filter(l -> l.getAction() != null && l.getAction().contains("DENIED")).count();
		Long usageCount = serviceUsageRepository.sumUsageBetween(startDate, endDate);
		long totalServiceUsage = usageCount != null ? usageCount : 0L;
		return ResponseEntity.ok(Map.of(
				"start_date", startDate,
				"end_date", endDate,
				"total_access", totalAccess,
				"total_successful_access", totalSuccessfulAccess,
				"total_failed_access", totalFailedAccess,
				"total_service_usage", totalServiceUsage
		));
	}
}


