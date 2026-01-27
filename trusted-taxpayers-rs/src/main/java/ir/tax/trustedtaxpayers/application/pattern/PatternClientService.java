package ir.tax.trustedtaxpayers.application.pattern;

import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternClientEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.PatternClientRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientCreateDto;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientResponseDto;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * سرویس مسئول عملیات CRUD روی کلاینت‌های الگو و اعتبارنامه‌های آن‌ها.
 * <p>
 * Service responsible for CRUD operations on pattern clients and their credentials.
 */
@Service
@RequiredArgsConstructor
public class PatternClientService {

    /**
     * ریپازیتوری ذخیره و بازیابی {@link PatternClientEntity}.
     * <p>
     * Repository for persisting and retrieving {@link PatternClientEntity} instances.
     */
    private final PatternClientRepository patternClientRepository;
    /**
     * تولیدکننده تصادفی امن برای ساخت کلید مخفی کلاینت‌ها.
     * <p>
     * Secure random generator used to produce API secrets for pattern clients.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * یک کلاینت جدید با جفت کلید/رمز تولیدی ایجاد می‌کند.
     * <p>
     * Creates a new client with a generated key/secret pair.
     *
     * @param dto ویژگی‌های ورودی کلاینت / inbound client attributes
     * @return نمای کلاینت ایجادشده / created client view
     */
    @Transactional
    public PatternClientResponseDto createClient(PatternClientCreateDto dto) {
        String clientKey = UUID.randomUUID().toString();
        String clientSecret = generateSecret();
        PatternClientEntity entity = PatternClientEntity.builder()
                .clientKey(clientKey)
                .clientSecret(clientSecret)
                .name(dto.getName())
                .description(dto.getDescription())
                .contactEmail(dto.getContactEmail())
                .callbackUrl(dto.getCallbackUrl())
                .build();
        PatternClientEntity saved = patternClientRepository.save(entity);
        return toResponse(saved);
    }

    /**
     * تمامی کلاینت‌ها را برمی‌گرداند و در صورت نیاز شامل موارد غیرفعال می‌شود.
     * <p>
     * Retrieves all clients, optionally including inactive entries.
     *
     * @param includeInactive آیا کلاینت‌های غیرفعال نیز بازگردانده شوند؟ / whether inactive clients should be returned
     * @return فهرست کلاینت‌ها / list of client projections
     */
    @Transactional(readOnly = true)
    public List<PatternClientResponseDto> listClients(boolean includeInactive) {
        return patternClientRepository.findAll().stream()
                .filter(entity -> includeInactive || entity.getIsActive())
                .map(this::toResponse)
                .toList();
    }

    /**
     * یک کلاینت را بر اساس شناسه‌اش جستجو می‌کند.
     * <p>
     * Looks up a single client by identifier.
     *
     * @param id شناسه کلاینت / client identifier
     * @return نمای کلاینت / client projection
     */
    @Transactional(readOnly = true)
    public PatternClientResponseDto getClient(UUID id) {
        return toResponse(getEntity(id));
    }

    /**
     * به‌روزرسانی‌های لازم روی فیلدهای قابل تغییر کلاینت را اعمال می‌کند.
     * <p>
     * Applies updates to mutable client fields.
     *
     * @param id  شناسه کلاینت / client identifier
     * @param dto داده‌های به‌روزرسانی / update payload
     * @return نمای کلاینت به‌روزشده / updated client projection
     */
    @Transactional
    public PatternClientResponseDto updateClient(UUID id, PatternClientUpdateDto dto) {
        PatternClientEntity entity = getEntity(id);
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getContactEmail() != null) {
            entity.setContactEmail(dto.getContactEmail());
        }
        if (dto.getCallbackUrl() != null) {
            entity.setCallbackUrl(dto.getCallbackUrl());
        }
        return toResponse(patternClientRepository.save(entity));
    }

    /**
     * وضعیت فعال بودن کلاینت را تغییر می‌دهد.
     * <p>
     * Toggles the active state of a client.
     *
     * @param id     شناسه کلاینت / client identifier
     * @param active مقدار وضعیت جدید / new active flag value
     */
    @Transactional
    public void setActive(UUID id, boolean active) {
        PatternClientEntity entity = getEntity(id);
        entity.setIsActive(active);
        patternClientRepository.save(entity);
    }

    /**
     * موجودیت را به DTO پاسخ تبدیل می‌کند.
     * <p>
     * Converts an entity to a response DTO.
     *
     * @param entity موجودیت ذخیره‌شده / persisted entity
     * @return DTO پاسخ / response DTO
     */
    private PatternClientResponseDto toResponse(PatternClientEntity entity) {
        return PatternClientResponseDto.builder()
                .id(entity.getId())
                .clientKey(entity.getClientKey())
                .clientSecret(entity.getClientSecret())
                .name(entity.getName())
                .contactEmail(entity.getContactEmail())
                .callbackUrl(entity.getCallbackUrl())
                .description(entity.getDescription())
                .createdOn(entity.getCreatedOn())
                .active(entity.getIsActive())
                .build();
    }

    /**
     * موجودیت را بارگذاری می‌کند یا در صورت نبود، خطا می‌دهد.
     * <p>
     * Loads an entity or throws when it does not exist.
     *
     * @param id شناسه کلاینت / client identifier
     * @return موجودیت یافت‌شده / resolved entity
     */
    private PatternClientEntity getEntity(UUID id) {
        return patternClientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pattern client not found: " + id));
    }

    /**
     * یک رشته مخفی تصادفی و سازگار با URL تولید می‌کند.
     * <p>
     * Generates a URL-safe random secret string.
     *
     * @return راز تولیدشده / generated secret
     */
    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
