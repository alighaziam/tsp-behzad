package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.AuditLogService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.AuditLogRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.AuditLogDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.AuditLogMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت لاگ‌های حسابرسی برای جستجو، فیلتر و ارائه خروجی‌های ردیابی.
 * <p>
 * REST controller exposing audit log listings, filters, and detail retrieval for compliance needs.
 */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Audit Log Management", description = "مدیریت لاگ‌های حسابرسی\n\nراهنمای سریع:\n- Base: /api/audit-logs\n- همه: GET /all\n- جزئیات: GET /by-id/{id}\n- فیلترها: /by-actor/{actorUserId} | /by-entity/{entityName} | /by-action/{action} | /by-date-range?start=...&end=...\n")
public class AuditLogController extends BaseController<AuditLogEntity, UUID> {
    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogService auditLogService, AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        super(auditLogService, auditLogRepository);
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Operation(
            summary = "دریافت لیست تمام لاگ‌های حسابرسی",
            description = "دریافت لیست تمام لاگ‌های حسابرسی ثبت شده در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست لاگ‌های حسابرسی با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = AuditLogDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "    \"actorUserId\": \"223e4567-e89b-12d3-a456-426614174001\",\n" +
                                                    "    \"entityName\": \"Taxpayer\",\n" +
                                                    "    \"action\": \"CREATE\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/all")
    public ResponseEntity<List<AuditLogDto>> getAll() {
        var logs = auditLogService.getAll();
        return ResponseEntity.ok(auditLogMapper.toDtos(logs));
    }

    @Operation(
            summary = "دریافت لاگ حسابرسی بر اساس شناسه",
            description = "دریافت اطلاعات کامل لاگ حسابرسی بر اساس شناسه یکتای آن",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لاگ حسابرسی با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = AuditLogDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"actorUserId\": \"223e4567-e89b-12d3-a456-426614174001\",\n" +
                                                    "  \"entityName\": \"Taxpayer\",\n" +
                                                    "  \"action\": \"CREATE\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "لاگ حسابرسی یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-id/{id}")
    public ResponseEntity<AuditLogDto> findAuditLogById(@PathVariable("id") UUID id) {
        var log = auditLogService.findById(id).orElseThrow();
        return ResponseEntity.ok(auditLogMapper.toDto(log));
    }

    @Operation(
            summary = "جستجوی لاگ‌های حسابرسی بر اساس شناسه کاربر عامل",
            description = "دریافت لیست لاگ‌های حسابرسی بر اساس شناسه کاربری که عمل را انجام داده است",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست لاگ‌های حسابرسی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = AuditLogDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-actor/{actorUserId}")
    public ResponseEntity<List<AuditLogDto>> findByActorUserId(@PathVariable("actorUserId") UUID actorUserId) {
        var logs = auditLogService.findByActorUserId(actorUserId);
        return ResponseEntity.ok(auditLogMapper.toDtos(logs));
    }

    @Operation(
            summary = "جستجوی لاگ‌های حسابرسی بر اساس نام موجودیت",
            description = "دریافت لیست لاگ‌های حسابرسی بر اساس نام موجودیت مورد نظر",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست لاگ‌های حسابرسی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = AuditLogDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-entity/{entityName}")
    public ResponseEntity<List<AuditLogDto>> findByEntityName(@PathVariable("entityName") String entityName) {
        var logs = auditLogService.findByEntityName(entityName);
        return ResponseEntity.ok(auditLogMapper.toDtos(logs));
    }

    @Operation(
            summary = "جستجوی لاگ‌های حسابرسی بر اساس نوع عمل",
            description = "دریافت لیست لاگ‌های حسابرسی بر اساس نوع عمل انجام شده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست لاگ‌های حسابرسی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = AuditLogDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-action/{action}")
    public ResponseEntity<List<AuditLogDto>> findByAction(@PathVariable("action") String action) {
        var logs = auditLogService.findByAction(action);
        return ResponseEntity.ok(auditLogMapper.toDtos(logs));
    }

    @Operation(
            summary = "جستجوی لاگ‌های حسابرسی بر اساس بازه زمانی",
            description = "دریافت لیست لاگ‌های حسابرسی در بازه زمانی مشخص شده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست لاگ‌های حسابرسی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = AuditLogDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "بازه زمانی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-date-range")
    public ResponseEntity<List<AuditLogDto>> findByEventTimeBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        var logs = auditLogService.findByEventTimeBetween(start, end);
        return ResponseEntity.ok(auditLogMapper.toDtos(logs));
    }

}


