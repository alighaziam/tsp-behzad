package ir.tax.trustedtaxpayers.interfaces.rest.pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.pattern.PatternClientService;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientCreateDto;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientResponseDto;
import ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto.PatternClientUpdateDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر REST برای مدیریت شرکت‌های طرف قرارداد الگو و وضعیت فعال بودن آن‌ها.
 * <p>
 * REST controller for administering pattern clients and their activation state.
 */
@RestController
@RequestMapping("/api/pattern-clients")
@RequiredArgsConstructor
@Tag(name = "Pattern Clients", description = "مدیریت شرکت‌های طرف قرارداد برای الگوها")
public class PatternClientController {

    /**
     * سرویس مسئول منطق کسب‌وکار مرتبط با کلاینت‌های الگو.
     * <p>
     * Service delegating business logic related to pattern clients.
     */
    private final PatternClientService patternClientService;

    /**
     * یک کلاینت الگو را ثبت کرده و اعتبارنامه لازم را تولید می‌کند.
     * <p>
     * Registers a new pattern client and generates credentials.
     *
     * @param dto مشخصات کلاینت / payload describing the client
     * @return نمای خروجی ایجادشده / created client representation
     */
    @PostMapping
    @Operation(summary = "ثبت شرکت طرف قرارداد",
            description = "برای هر شرکت یک کلید یونیک تولید می‌شود که باید در درخواست‌های الگو استفاده شود.")
    public ResponseEntity<PatternClientResponseDto> createClient(@Valid @RequestBody PatternClientCreateDto dto) {
        return ResponseEntity.ok(patternClientService.createClient(dto));
    }

    /**
     * فهرست کلاینت‌های ثبت‌شده را بازمی‌گرداند.
     * <p>
     * Returns the list of registered clients.
     *
     * @param includeInactive آیا کلاینت‌های غیرفعال نیز بازگردانده شوند؟ / whether inactive clients should be included
     * @return فهرست کلاینت‌ها / list of clients
     */
    @GetMapping
    @Operation(summary = "فهرست شرکت‌های ثبت‌شده")
    public ResponseEntity<List<PatternClientResponseDto>> listClients(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(patternClientService.listClients(includeInactive));
    }

    /**
     * اطلاعات یک کلاینت را بر اساس شناسه دریافت می‌کند.
     * <p>
     * Retrieves a single client by identifier.
     *
     * @param id شناسه کلاینت / client identifier
     * @return نمای کلاینت / client projection
     */
    @GetMapping("/{id}")
    @Operation(summary = "دریافت اطلاعات یک شرکت")
    public ResponseEntity<PatternClientResponseDto> getClient(@PathVariable UUID id) {
        return ResponseEntity.ok(patternClientService.getClient(id));
    }

    /**
     * کلاینت موجود را با ویژگی‌های جدید به‌روزرسانی می‌کند.
     * <p>
     * Updates an existing client with new attributes.
     *
     * @param id  شناسه کلاینت / client identifier
     * @param dto بدنه به‌روزرسانی / update payload
     * @return نمای کلاینت به‌روزشده / updated client projection
     */
    @PutMapping("/{id}")
    @Operation(summary = "به‌روزرسانی اطلاعات شرکت")
    public ResponseEntity<PatternClientResponseDto> updateClient(@PathVariable UUID id,
                                                                 @Valid @RequestBody PatternClientUpdateDto dto) {
        return ResponseEntity.ok(patternClientService.updateClient(id, dto));
    }

    /**
     * یک کلاینت را فعال می‌کند.
     * <p>
     * Activates a client.
     *
     * @param id شناسه کلاینت / client identifier
     * @return پاسخ خالی / empty response
     */
    @PatchMapping("/{id}/activate")
    @Operation(summary = "فعال‌سازی شرکت")
    public ResponseEntity<Void> activateClient(@PathVariable UUID id) {
        patternClientService.setActive(id, true);
        return ResponseEntity.noContent().build();
    }

    /**
     * یک کلاینت را غیرفعال می‌کند.
     * <p>
     * Deactivates a client.
     *
     * @param id شناسه کلاینت / client identifier
     * @return پاسخ خالی / empty response
     */
    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "غیرفعال‌سازی شرکت")
    public ResponseEntity<Void> deactivateClient(@PathVariable UUID id) {
        patternClientService.setActive(id, false);
        return ResponseEntity.noContent().build();
    }
}
