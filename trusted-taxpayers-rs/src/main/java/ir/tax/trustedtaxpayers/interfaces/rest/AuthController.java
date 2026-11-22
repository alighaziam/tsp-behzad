package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.security.JwtService;
import ir.tax.trustedtaxpayers.application.service.UserAuthenticationService;
import ir.tax.trustedtaxpayers.application.service.PasswordResetEmailService;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.AuthLoginRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ChangePasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ForgotPasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ForgotPasswordResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ResetPasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.util.CurrentUserResolver;
import ir.tax.trustedtaxpayers.application.rate.RateLimited;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;
import java.util.Arrays;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping({"/api/auth", "/api/v1/auth"})
@Tag(name = "Authentication",
     description = """
         احراز هویت و مدیریت رمز عبور
         
         - دریافت توکن ورود: POST /api/auth/login → {username,password}
         - فراموشی رمز: POST /api/auth/forgot-password → {identifier}
         - تنظیم رمز جدید با توکن: POST /api/auth/reset-password → {token,newPassword}
         - تغییر رمز با رمز فعلی: POST /api/auth/change-password
         """)
@Slf4j
public class AuthController {

    private static final long ACCESS_TOKEN_TTL_MS = 3_600_000L; // 1 hour

    private final JwtService jwtService;
    private final UserAuthenticationService authenticationService;
    private final Environment environment;
    private final ObjectProvider<PasswordResetEmailService> passwordResetEmailServiceProvider;

    public AuthController(JwtService jwtService, UserAuthenticationService authenticationService,
                         Environment environment,
                         ObjectProvider<PasswordResetEmailService> passwordResetEmailServiceProvider) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
        this.environment = environment;
        this.passwordResetEmailServiceProvider = passwordResetEmailServiceProvider;
    }

    @Operation(summary = "بررسی سلامت سرویس احراز هویت")
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "ورود کاربر و دریافت توکن",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ورود موفق",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            value = "{ \"access_token\": \"<jwt>\", \"token_type\": \"Bearer\" }"
                                    )
                            )),
                    @ApiResponse(responseCode = "401", description = "اعتبارسنجی ناموفق"),
                    @ApiResponse(responseCode = "403", description = "حساب کاربری غیرفعال")
            }
    )
    @RateLimited(perMinute = 5)
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthLoginRequest request) {
        var result = authenticationService.authenticate(request.username(), request.password());
        var claims = Map.<String, Object>of("roles", result.getRoles());
        String token = jwtService.generate(result.getUsername(), ACCESS_TOKEN_TTL_MS, claims);
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }

    @Operation(summary = "درخواست توکن بازیابی رمز عبور")
    @RateLimited(perMinute = 3)
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        Optional<String> token = production && !isEmailAddress(request.identifier())
                ? Optional.empty()
                : authenticationService.initiatePasswordReset(request.identifier());
        // Keep the public response identical for known and unknown accounts.
        if (production && token.isPresent() && isEmailAddress(request.identifier())) {
            try {
                passwordResetEmailServiceProvider.getObject().send(request.identifier(), token.get());
            } catch (Exception deliveryFailure) {
                authenticationService.cancelPasswordReset(token.get());
                log.warn("Password reset email delivery failed; the public response remains unchanged.");
            }
        }
        return ResponseEntity.ok(new ForgotPasswordResponse(true, production ? null : token.orElse(null)));
    }

    @Operation(summary = "تنظیم رمز جدید با استفاده از توکن بازیابی")
    @RateLimited(perMinute = 5)
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "تغییر رمز عبور با رمز فعلی")
    @RateLimited(perMinute = 5)
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        String username = resolveUsername(httpRequest, request);
        authenticationService.changePassword(username, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * باقی‌مانده برای سازگاری عقب‌رو به زودی حذف خواهد شد.
     */
    /**
     * Disabled because this unauthenticated compatibility endpoint previously allowed
     * callers to mint tokens with arbitrary usernames and roles.
     */
    @Deprecated
    @PostMapping("/token")
    public ResponseEntity<Void> legacyToken() {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }

    private String resolveUsername(HttpServletRequest request, ChangePasswordRequest body) {
        String authenticatedUsername = extractUsernameFromAuthorizationHeader(request);
        if (!hasText(authenticatedUsername)) {
            authenticatedUsername = CurrentUserResolver.resolveUsername(request);
        }
        if (!hasText(authenticatedUsername)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication is required to change a password.");
        }
        if (hasText(body.username()) && !authenticatedUsername.equalsIgnoreCase(body.username().trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The requested username does not match the authenticated account.");
        }
        return authenticatedUsername;
    }

    private String extractUsernameFromAuthorizationHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!hasText(header) || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring(7);
        try {
            return jwtService.parse(token).getSubject();
        } catch (Exception ex) {
            log.debug("Unable to parse JWT token for change-password request: {}", ex.getMessage());
            return null;
        }
    }

    private boolean isEmailAddress(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
