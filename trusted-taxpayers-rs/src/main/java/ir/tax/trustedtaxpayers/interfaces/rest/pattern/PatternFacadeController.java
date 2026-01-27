package ir.tax.trustedtaxpayers.interfaces.rest.pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import ir.tax.trustedtaxpayers.biz.service.PatternFacadeService;
import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * کنترلر REST برای مدیریت درخواست‌های الگو و متادیتای مرتبط.
 * <p>
 * REST controller exposing endpoints for managing pattern requests and metadata.
 */
@RestController
@RequestMapping("/api/pattern-facade")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pattern Facade", description = "سرویس‌های مستقل برای دریافت فیلدهای الگو، ثبت و استعلام درخواست‌های صورتحساب")
public class PatternFacadeController {

    /**
     * فاساد هماهنگ‌کننده منطق کسب‌وکار درخواست‌های الگو.
     * <p>
     * Facade coordinating the business logic layer for pattern requests.
     */
    private final PatternFacadeService patternFacadeService;

    /**
     * درخواست الگوی جدید ایجاد کرده و تعریف فیلدهای مرتبط را بازمی‌گرداند.
     * <p>
     * Creates a new pattern request and returns the associated fields definition.
     *
     * @param request بدنه تاییدشده درخواست / validated payload describing the desired pattern
     * @return پاسخ حاوی شناسه‌ها و ساختار فرم / response containing identifiers and form structure
     */
    @PostMapping("/requests")
    @Operation(summary = "ایجاد درخواست جدید و دریافت فیلدهای موردنیاز",
            description = "یک شناسه یکتا صادر می‌شود و بر اساس شماره الگو فیلدهای موردنیاز بازگردانده می‌شود.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "درخواست با موفقیت ثبت شد",
                            content = @Content(schema = @Schema(implementation = PatternRequestResponseDto.class),
                                    examples = @ExampleObject(value = "{\n  \"requestId\": \"d290f1ee-6c54-4b01-90e6-d701748f0851\",\n  \"patternNumber\": 2,\n  \"status\": \"PENDING\",\n  \"title\": \"الگوی فروش داخلی\",\n  \"description\": \"فیلدهای الگوی فروش\",\n  \"headerFields\": [ { \"section\": \"HEADER\", \"key\": \"inno\", \"label\": \"شماره صورتحساب\" } ],\n  \"itemFields\": [ { \"section\": \"ITEM\", \"key\": \"sstid\", \"label\": \"کد کالا/خدمت\" } ]\n}")))
            })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = PatternRequestCreateDto.class),
                    examples = @ExampleObject(name = "درخواست الگوی 2",
                            summary = "نمونه ایجاد درخواست برای third-party",
                            value = "{\n  \"patternNumber\": 2,\n  \"clientKey\": \"c3ebf6c0-a4ff-4d07-9a3b-1a0f2aa7d7f4\",\n  \"metadata\": {\n    \"clientRef\": \"REQ-7788\",\n    \"callbackUrl\": \"https://callback.example.com/result\"\n  }\n}")))
    public ResponseEntity<PatternRequestResponseDto> createRequest(@Valid @RequestBody PatternRequestCreateDto request) {
        return ResponseEntity.ok(patternFacadeService.createRequest(request));
    }

    /**
     * درخواست ساخته‌شده را اعتبارسنجی و ارسال می‌کند.
     * <p>
     * Validates and submits a previously created pattern request.
     *
     * @param requestId شناسه درخواست برای ارسال / identifier of the request to submit
     * @param request   داده‌های سربرگ و اقلام / payload containing header and item data
     * @return پاسخ ارسال / submission response
     */
    @PostMapping("/requests/{requestId}/submit")
    @Operation(summary = "ارسال داده‌های تکمیل‌شده",
            description = "پس از اعتبارسنجی، صورتحساب ذخیره و برای سامانه امور مالیاتی ارسال می‌شود.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ارسال انجام شد",
                            content = @Content(schema = @Schema(implementation = PatternSubmissionResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "اعتبارسنجی ناموفق",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(schema = @Schema(implementation = PatternSubmissionRequestDto.class),
                    examples = @ExampleObject(name = "الگوی فروش داخلی",
                            summary = "نمونه ارسال بر اساس الگوی 2",
                            value = "{\n  \"header\": {\n    \"inno\": \"INV-14031234\",\n    \"indatim\": \"2025-11-08T10:15:30+03:30\",\n    \"inty\": 1,\n    \"ins\": 1,\n    \"inp\": 2,\n    \"tins\": \"12345678901\",\n    \"sbc\": \"0001\",\n    \"tinb\": \"10987654321\",\n    \"tbill\": 3500000,\n    \"tprdis\": 3500000,\n    \"tadis\": 3500000\n  },\n  \"items\": [\n    {\n      \"sstid\": \"241012\",\n      \"sstt\": \"خدمت مشاوره\",\n      \"mu\": \"SERVICE\",\n      \"am\": 1,\n      \"fee\": 3500000,\n      \"adis\": 3500000\n    }\n  ],\n  \"metadata\": {\n    \"clientRef\": \"REQ-7788\"\n  }\n}")))
    public ResponseEntity<PatternSubmissionResponseDto> submit(@PathVariable UUID requestId,
                                                                @Valid @RequestBody PatternSubmissionRequestDto request) {
        return ResponseEntity.ok(patternFacadeService.submit(requestId, request));
    }

    /**
     * جدیدترین وضعیت درخواست را بر اساس شناسه یکتا بازمی‌گرداند.
     * <p>
     * Retrieves the latest status of a pattern request by its identifier.
     *
     * @param requestId شناسه درخواست / request identifier
     * @return خروجی استعلام / inquiry response
     */
    @GetMapping("/requests/{requestId}")
    @Operation(summary = "استعلام وضعیت با شناسه درخواست",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(implementation = PatternInquiryResponseDto.class))))
    public ResponseEntity<PatternInquiryResponseDto> getStatus(@PathVariable UUID requestId) {
        return ResponseEntity.ok(patternFacadeService.getStatus(requestId));
    }

    /**
     * جدیدترین وضعیت درخواست را بر اساس شماره مرجع سامانه بیرونی بازمی‌گرداند.
     * <p>
     * Retrieves the latest status of a pattern request by its reference number.
     *
     * @param referenceNumber شماره مرجع سامانه امور مالیاتی / reference number assigned by the external system
     * @return خروجی استعلام / inquiry response
     */
    @GetMapping("/requests/by-reference/{referenceNumber}")
    @Operation(summary = "استعلام وضعیت با referenceNumber سامانه امور مالیاتی",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(schema = @Schema(implementation = PatternInquiryResponseDto.class))))
    public ResponseEntity<PatternInquiryResponseDto> getStatusByReference(@PathVariable String referenceNumber) {
        return ResponseEntity.ok(patternFacadeService.getStatusByReference(referenceNumber));
    }

    /**
     * درخواست‌ها را با فیلترهای اختیاری بازیابی می‌کند.
     * <p>
     * Searches for requests matching optional filter criteria.
     *
     * @param status        فیلتر وضعیت / desired status filter
     * @param from          بازه شروع تاریخ / lower bound for creation date
     * @param to            بازه پایان تاریخ / upper bound for creation date
     * @param patternNumber شماره الگو / pattern number filter
     * @param requestedBy   شناسه درخواست‌کننده / requester filter
     * @return فهرست درخواست‌های منطبق / list of matching requests
     */
    @GetMapping("/requests")
    @Operation(summary = "جستجوی درخواست‌ها بر اساس فیلترهای مختلف",
            description = "امکان فیلتر بر اساس وضعیت، بازه زمانی ایجاد و شماره الگو فراهم است.")
    public ResponseEntity<List<PatternInquiryResponseDto>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer patternNumber,
            @RequestParam(required = false, name = "requestedBy") String requestedBy) {

        PatternRequestSearchCriteria.PatternRequestSearchCriteriaBuilder builder = PatternRequestSearchCriteria.builder();

        if (status != null && !status.isBlank()) {
            builder.status(parseStatus(status));
        }
        if (from != null && !from.isBlank()) {
            builder.from(parseDate(from, "from"));
        }
        if (to != null && !to.isBlank()) {
            builder.to(parseDate(to, "to"));
        }
        if (patternNumber != null) {
            builder.patternNumber(patternNumber);
        }
        if (requestedBy != null && !requestedBy.isBlank()) {
            builder.requestedBy(requestedBy);
        }

        return ResponseEntity.ok(patternFacadeService.search(builder.build()));
    }

    /**
     * درخواست معوق را تایید می‌کند تا به‌صورت غیرهمزمان پردازش شود.
     * <p>
     * Approves a deferred request so it can be processed asynchronously.
     *
     * @param requestId شناسه درخواست / request identifier
     * @return پاسخ خالی / empty response
     */
    @PatchMapping("/requests/{requestId}/approve")
    @Operation(summary = "تایید ارسال درخواست معوق",
            description = "درخواست‌هایی که هنگام ثبت معوق شده‌اند با این سرویس تایید می‌شوند تا جاب پس‌زمینه آن‌ها را ارسال کند.")
    public ResponseEntity<Void> approveDeferred(@PathVariable UUID requestId) {
        patternFacadeService.approveDeferredRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * فهرست تمام الگوهای پشتیبانی‌شده و مشخصات فیلدهای آن‌ها را بازمی‌گرداند.
     * <p>
     * Returns the list of all supported patterns along with their field descriptors.
     *
     * @return مجموعه‌ای از تعاریف الگو / collection of pattern definitions
     */
    @GetMapping("/patterns")
    @Operation(summary = "دریافت فهرست تمام الگوها و فیلدهای موردنیاز آن‌ها",
            description = "برای نمایش کامل ساختار سربرگ و اقلام تمام الگوهای موجود." )
    public ResponseEntity<List<PatternDefinitionDto>> getAllPatterns() {
        return ResponseEntity.ok(patternFacadeService.listAllPatterns());
    }

    /**
     * مقدار وضعیت ارسالی را به enum متناظر تبدیل می‌کند.
     * <p>
     * Parses a request status string into the corresponding enum value.
     *
     * @param value مقدار وضعیت ورودی / status text supplied via query parameter
     * @return وضعیت تفسیرشده / parsed status
     */
    private PatternRequestStatus parseStatus(String value) {
        try {
            return PatternRequestStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status value: " + value, ex);
        }
    }

    /**
     * رشته تاریخ-زمان با آفست را به {@link OffsetDateTime} تبدیل می‌کند.
     * <p>
     * Parses an ISO-8601 offset date-time string into {@link OffsetDateTime}.
     *
     * @param value      مقدار ورودی / input value
     * @param fieldName  نام پارامتر برای گزارش خطا / parameter name for error reporting
     * @return تاریخ-زمان تفسیرشده / parsed date-time
     */
    private OffsetDateTime parseDate(String value, String fieldName) {
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format for " + fieldName + ": " + value, ex);
        }
    }
}
