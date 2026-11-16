package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.InvoiceTemplateService;
import ir.tax.trustedtaxpayers.application.service.CurrentUserTaxpayerResolverService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.InvoiceTemplateEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateInvoiceTemplateRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.InvoiceTemplateDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
@RequestMapping({"/api/invoice-templates", "/api/v1/invoice-templates"})
@Tag(
        name = "Invoice Templates",
        description = "مدیریت الگوهای آماده صورتحساب\n\n"
                + "راهنمای سریع:\n"
                + "- لیست الگوها: GET /api/invoice-templates/me\n"
                + "- ایجاد الگو: POST /api/invoice-templates/me\n"
                + "- استفاده از الگو: GET /api/invoice-templates/me/{templateId}\n"
                + "- حذف الگو: DELETE /api/invoice-templates/me/{templateId}"
)
@RequiredArgsConstructor
public class InvoiceTemplateController {

    private final InvoiceTemplateService templateService;
    private final CurrentUserTaxpayerResolverService taxpayerAccess;

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    @Operation(summary = "دریافت لیست الگوهای کاربر جاری")
    public ResponseEntity<List<InvoiceTemplateDto>> getMyTemplates(
            HttpServletRequest request,
            @RequestParam(required = false) UUID taxpayerId,
            @RequestParam(required = false) Integer patternNumber) {

        UUID userId = taxpayerAccess.resolveCurrentUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID effectiveTaxpayerId = taxpayerId != null ? taxpayerId : taxpayerAccess.findCurrentUserPrimaryTaxpayerId().orElse(null);
        if (effectiveTaxpayerId != null && !taxpayerAccess.hasAccessToTaxpayer(effectiveTaxpayerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (effectiveTaxpayerId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<InvoiceTemplateEntity> templates;
        if (patternNumber != null) {
            templates = templateService.getUserTemplatesByPattern(
                    userId, effectiveTaxpayerId, patternNumber);
        } else {
            templates = templateService.getUserTemplates(userId, effectiveTaxpayerId);
        }

        return ResponseEntity.ok(templates.stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
    }

    @GetMapping("/me/{templateId}")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    @Operation(summary = "دریافت یک الگوی خاص")
    public ResponseEntity<InvoiceTemplateDto> getTemplate(
            HttpServletRequest request,
            @PathVariable UUID templateId,
            @RequestParam(required = false) UUID taxpayerId) {

        UUID userId = taxpayerAccess.resolveCurrentUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID effectiveTaxpayerId = taxpayerId != null ? taxpayerId : taxpayerAccess.findCurrentUserPrimaryTaxpayerId().orElse(null);
        if (effectiveTaxpayerId != null && !taxpayerAccess.hasAccessToTaxpayer(effectiveTaxpayerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (effectiveTaxpayerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            InvoiceTemplateEntity template = templateService.getTemplate(
                    templateId, userId, effectiveTaxpayerId);
            return ResponseEntity.ok(toDto(template));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/me")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    @Operation(summary = "ایجاد الگوی جدید")
    public ResponseEntity<InvoiceTemplateDto> createTemplate(
            HttpServletRequest request,
            @Valid @RequestBody CreateInvoiceTemplateRequest createRequest,
            @RequestParam(required = false) UUID taxpayerId) {

        UUID userId = taxpayerAccess.resolveCurrentUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID effectiveTaxpayerId = taxpayerId != null ? taxpayerId : taxpayerAccess.findCurrentUserPrimaryTaxpayerId().orElse(null);
        if (effectiveTaxpayerId != null && !taxpayerAccess.hasAccessToTaxpayer(effectiveTaxpayerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (effectiveTaxpayerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        InvoiceTemplateEntity template = templateService.createTemplate(
                userId,
                effectiveTaxpayerId,
                createRequest.templateName(),
                createRequest.patternNumber(),
                createRequest.headerData(),
                createRequest.itemsData(),
                createRequest.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(template));
    }

    @DeleteMapping("/me/{templateId}")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    @Operation(summary = "حذف الگو")
    public ResponseEntity<Void> deleteTemplate(
            HttpServletRequest request,
            @PathVariable UUID templateId,
            @RequestParam(required = false) UUID taxpayerId) {

        UUID userId = taxpayerAccess.resolveCurrentUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UUID effectiveTaxpayerId = taxpayerId != null ? taxpayerId : taxpayerAccess.findCurrentUserPrimaryTaxpayerId().orElse(null);
        if (effectiveTaxpayerId != null && !taxpayerAccess.hasAccessToTaxpayer(effectiveTaxpayerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (effectiveTaxpayerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            templateService.deleteTemplate(templateId, userId, effectiveTaxpayerId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private InvoiceTemplateDto toDto(InvoiceTemplateEntity entity) {
        return new InvoiceTemplateDto(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTaxpayer().getId(),
                entity.getTemplateName(),
                entity.getPatternNumber(),
                entity.getHeaderData(),
                entity.getItemsData(),
                entity.getDescription(),
                entity.getCreatedOn(),
                entity.getUpdatedOn()
        );
    }
}

