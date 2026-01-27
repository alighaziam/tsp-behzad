package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserInvoicePreferenceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserInvoicePreferenceRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UpsertUserInvoicePreferenceRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UserInvoicePreferenceDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static ir.tax.trustedtaxpayers.interfaces.rest.util.CurrentUserResolver.resolveUsername;

@RestController
@RequestMapping({"/api/user/preferences", "/api/v1/user/preferences"})
@Tag(
        name = "User Invoice Preferences",
        description = "مدیریت تنظیمات پیش‌فرض صورتحساب کاربران\n\n"
                + "راهنمای سریع:\n"
                + "- دریافت تنظیمات کاربر جاری: GET /api/user/preferences/me\n"
                + "- بروزرسانی/ایجاد برای کاربر جاری: PUT /api/user/preferences/me\n"
                + "- دریافت بر اساس شناسه کاربر: GET /api/user/preferences/{userId}\n"
                + "- بروزرسانی بر اساس شناسه کاربر: PUT /api/user/preferences/{userId}"
)
@RequiredArgsConstructor
public class UserInvoicePreferenceController {

    private final UserInvoicePreferenceRepository preferenceRepository;
    private final UserRoleService userRoleService;

    @GetMapping("/{userId}")
    @Operation(summary = "دریافت تنظیمات پیش‌فرض صورتحساب براساس شناسه کاربر")
    public ResponseEntity<?> getPreferences(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID taxpayerId) {

        if (taxpayerId != null) {
            return preferenceRepository
                    .findByUserIdAndTaxpayerId(userId, taxpayerId)
                    .map(this::toDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        List<UserInvoicePreferenceEntity> entities = preferenceRepository.findByUserId(userId);
        return ResponseEntity.ok(
                entities.stream()
                        .map(this::toDto)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/{userId}")
    @Operation(summary = "ثبت یا بروزرسانی تنظیمات پیش‌فرض کاربر (برای مدیریت)")
    public ResponseEntity<UserInvoicePreferenceDto> upsertPreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpsertUserInvoicePreferenceRequest request) {

        UserInvoicePreferenceEntity saved = upsertPreference(userId, request);
        return ResponseEntity.ok(toDto(saved));
    }

    @GetMapping("/me")
    @Operation(summary = "دریافت تنظیمات پیش‌فرض صورتحساب کاربر جاری")
    public ResponseEntity<?> getMyPreferences(
            HttpServletRequest request,
            @RequestParam(required = false) UUID taxpayerId) {

        UUID userId = resolveCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (taxpayerId != null) {
            return preferenceRepository
                    .findByUserIdAndTaxpayerId(userId, taxpayerId)
                    .map(this::toDto)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        List<UserInvoicePreferenceEntity> entities = preferenceRepository.findByUserId(userId);
        return ResponseEntity.ok(
                entities.stream()
                        .map(this::toDto)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/me")
    @Operation(summary = "ثبت یا بروزرسانی تنظیمات پیش‌فرض صورتحساب کاربر جاری")
    public ResponseEntity<UserInvoicePreferenceDto> upsertMyPreferences(
            HttpServletRequest request,
            @Valid @RequestBody UpsertUserInvoicePreferenceRequest preferenceRequest) {

        UUID userId = resolveCurrentUserId(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserInvoicePreferenceEntity saved = upsertPreference(userId, preferenceRequest);
        return ResponseEntity.ok(toDto(saved));
    }

    private UserInvoicePreferenceEntity upsertPreference(UUID userId, UpsertUserInvoicePreferenceRequest request) {
        UserInvoicePreferenceEntity entity = preferenceRepository
                .findByUserIdAndTaxpayerId(userId, request.taxpayerId())
                .orElseGet(() -> UserInvoicePreferenceEntity.builder()
                        .userId(userId)
                        .taxpayerId(request.taxpayerId())
                        .firstLoginCompleted(false)
                        .build());

        entity.setDefaultTemplateCode(request.defaultTemplateCode());
        entity.setDefaultTemplateTypeCode(request.defaultTemplateTypeCode());
        entity.setDefaultPatternNumber(request.defaultPatternNumber());
        entity.setDefaultIns(request.defaultIns());
        entity.setLastUsedTemplateCode(request.lastUsedTemplateCode());
        if (request.firstLoginCompleted() != null) {
            entity.setFirstLoginCompleted(request.firstLoginCompleted());
        }

        return preferenceRepository.save(entity);
    }

    private UUID resolveCurrentUserId(HttpServletRequest request) {
        String username = resolveUsername(request);
        if (username == null) {
            return null;
        }
        return userRoleService.findUserIdByUsername(username).orElse(null);
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

