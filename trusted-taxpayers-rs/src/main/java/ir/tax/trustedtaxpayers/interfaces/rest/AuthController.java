package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.security.JwtService;
import ir.tax.trustedtaxpayers.application.service.UserAuthenticationService;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.AuthLoginRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ChangePasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ForgotPasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ForgotPasswordResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ResetPasswordRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.util.CurrentUserResolver;
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

    public AuthController(JwtService jwtService, UserAuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
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
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthLoginRequest request) {
        var result = authenticationService.authenticate(request.username(), request.password());
        var claims = Map.<String, Object>of("roles", result.getRoles());
        String token = jwtService.generate(result.getUsername(), ACCESS_TOKEN_TTL_MS, claims);
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "Bearer"));
    }

    @Operation(summary = "درخواست توکن بازیابی رمز عبور")
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        Optional<String> token = authenticationService.initiatePasswordReset(request.identifier());
        return ResponseEntity.ok(new ForgotPasswordResponse(token.isPresent(), token.orElse(null)));
    }

    @Operation(summary = "تنظیم رمز جدید با استفاده از توکن بازیابی")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "تغییر رمز عبور با رمز فعلی")
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
        String fromJwt = extractUsernameFromAuthorizationHeader(request);
        if (hasText(fromJwt)) {
            return fromJwt;
        }

        String resolved = CurrentUserResolver.resolveUsername(request);
        if (hasText(resolved)) {
            return resolved;
        }

        if (hasText(body.username())) {
            return body.username();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "امکان تشخیص کاربر برای تغییر رمز وجود ندارد.");
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
