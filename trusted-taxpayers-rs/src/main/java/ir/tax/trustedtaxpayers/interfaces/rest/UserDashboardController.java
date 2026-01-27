package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.application.service.InvoiceByTaxpayerService;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserInvoicePreferenceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserInvoicePreferenceRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.InvoiceSummaryDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UserDashboardResponseDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UserInvoicePreferenceDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ir.tax.trustedtaxpayers.interfaces.rest.util.CurrentUserResolver.resolveUsername;

@RestController
@RequestMapping({"/api/user/dashboard", "/api/v1/user/dashboard"})
@Tag(
        name = "User Dashboard",
        description = "نمای کلی تنظیمات و تاریخچه صورتحساب کاربر\n\n"
                + "راهنمای سریع:\n"
                + "- اطلاعات کاربر جاری: GET /api/user/dashboard\n"
                + "- با تعیین مودی خاص: GET /api/user/dashboard?taxpayerId={uuid}\n"
                + "- پارامتر limit برای تعداد نتایج تاریخچه (پیش‌فرض 10)"
)
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserInvoicePreferenceRepository preferenceRepository;
    private final UserRoleService userRoleService;
    private final InvoiceByTaxpayerService invoiceByTaxpayerService;
    private final ElectronicInvoiceSystemService systemService;

    @GetMapping
    @Operation(summary = "دریافت ترجیحات و تاریخچه صورتحساب کاربر جاری")
    public ResponseEntity<UserDashboardResponseDto> getDashboard(
            HttpServletRequest request,
            @RequestParam(required = false) UUID taxpayerId,
            @RequestParam(defaultValue = "10") Integer limit) {

        UUID userId = resolveCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UserInvoicePreferenceEntity> preferences = preferenceRepository.findByUserId(userId);

        UUID effectiveTaxpayerId = taxpayerId;
        if (effectiveTaxpayerId == null) {
            effectiveTaxpayerId = preferences.stream()
                    .map(UserInvoicePreferenceEntity::getTaxpayerId)
                    .findFirst()
                    .orElse(null);
        }

        List<InvoiceSummaryDto> recentInvoices = fetchRecentInvoices(effectiveTaxpayerId, limit);

        UserDashboardResponseDto response = new UserDashboardResponseDto(
                preferences.stream().map(this::toDto).collect(Collectors.toList()),
                recentInvoices
        );

        return ResponseEntity.ok(response);
    }

    private UUID resolveCurrentUserId(HttpServletRequest request) {
        String username = resolveUsername(request);
        if (username == null) {
            return null;
        }
        return userRoleService.findUserIdByUsername(username).orElse(null);
    }

    private List<InvoiceSummaryDto> fetchRecentInvoices(UUID taxpayerId, Integer limit) {
        if (taxpayerId == null) {
            return List.of();
        }

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
       /* Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService.loadInvoicesByTaxpayer(
                taxpayerId,
                0,
                pageSize,
                "createdOn",
                "DESC"
        );*/

        Page<ElectronicInvoiceHeaderEntity> invoices = null;
        List<UUID> headerIds = invoices.getContent().stream()
                .map(ElectronicInvoiceHeaderEntity::getId)
                .collect(Collectors.toList());

        var statusMap = systemService.findStsByHeaderIds(headerIds);

        return invoices.getContent().stream()
                .map(header -> toSummaryDto(header, statusMap.get(header.getId())))
                .collect(Collectors.toList());
    }

    private InvoiceSummaryDto toSummaryDto(ElectronicInvoiceHeaderEntity header, String status) {
        // templateTypeCode دیگر در TemplateEntity وجود ندارد
        String templateTypeCode = null;

        Integer patternNumber = header.getInp();
        Integer invoiceSubject = header.getIns() != null ? header.getIns().getValue() : null;
        String invoiceSubjectLabel = header.getIns() != null ? header.getIns().getPersianName() : null;

        return new InvoiceSummaryDto(
                ir.tax.trustedtaxpayers.interfaces.rest.util.EntityDtoMapper.uuidToString(header.getId()),
                header.getTaxId(),
                header.getTemplateCode(),
                templateTypeCode,
                patternNumber,
                invoiceSubject,
                invoiceSubjectLabel,
                header.getTbill(),
                header.getIndatim(),
                status
        );
    }

    private UserInvoicePreferenceDto toDto(UserInvoicePreferenceEntity entity) {
        return new UserInvoicePreferenceDto(
                entity.getId(),
                entity.getUserId(),
                entity.getTaxpayerId(),
                entity.getDefaultTemplateCode(),
                entity.getDefaultTemplateTypeCode(),
                entity.getDefaultPatternNumber(),
                entity.getDefaultIns(),
                entity.getLastUsedTemplateCode(),
                entity.isFirstLoginCompleted()
        );
    }
}

