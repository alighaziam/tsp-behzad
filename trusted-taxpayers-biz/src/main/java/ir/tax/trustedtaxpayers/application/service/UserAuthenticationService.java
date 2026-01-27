package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PasswordResetTokenEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserRoleEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PasswordResetTokenRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.RoleRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final String PURPOSE_FORGOT_PASSWORD = "FORGOT_PASSWORD";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResult authenticate(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> unauthorized("نام کاربری یا رمز عبور نادرست است."));

        if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "حساب کاربری غیرفعال است.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw unauthorized("نام کاربری یا رمز عبور نادرست است.");
        }

        List<String> roles = loadActiveRoleNames(user.getId());
        return new LoginResult(user.getId(), user.getUsername(), roles);
    }

    @Transactional
    public Optional<String> initiatePasswordReset(String usernameOrEmail) {
        Optional<UserEntity> optionalUser = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail));

        if (optionalUser.isEmpty()) {
            log.debug("Password reset requested for non-existing identifier {}", usernameOrEmail);
            return Optional.empty();
        }

        UserEntity user = optionalUser.get();
        deactivateActiveTokens(user.getId());

        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime now = OffsetDateTime.now();
        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .userId(user.getId())
                .token(token)
                .purpose(PURPOSE_FORGOT_PASSWORD)
                .expiresAt(now.plus(RESET_TOKEN_TTL))
                .build();

        passwordResetTokenRepository.save(entity);
        return Optional.of(token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "توکن بازیابی معتبر نیست."));

        if (!resetToken.getIsActive() || resetToken.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "توکن بازیابی قبلاً استفاده شده یا غیرفعال است.");
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            resetToken.setIsActive(false);
            passwordResetTokenRepository.save(resetToken);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "توکن بازیابی منقضی شده است.");
        }

        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "کاربر مرتبط با توکن یافت نشد."));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(OffsetDateTime.now());
        resetToken.setIsActive(false);
        passwordResetTokenRepository.save(resetToken);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "کاربر یافت نشد."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw unauthorized("رمز عبور فعلی صحیح نیست.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private void deactivateActiveTokens(UUID userId) {
        List<PasswordResetTokenEntity> tokens = passwordResetTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            return;
        }
        tokens.forEach(token -> token.setIsActive(false));
        passwordResetTokenRepository.saveAll(tokens);
    }

    private List<String> loadActiveRoleNames(UUID userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .filter(UserRoleEntity::isActive)
                .map(UserRoleEntity::getRoleId)
                .map(roleId -> roleRepository.findById(roleId))
                .flatMap(Optional::stream)
                .map(RoleEntity::getName)
                .collect(Collectors.toList());
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }

    @Value
    public static class LoginResult {
        UUID userId;
        String username;
        List<String> roles;
    }
}


