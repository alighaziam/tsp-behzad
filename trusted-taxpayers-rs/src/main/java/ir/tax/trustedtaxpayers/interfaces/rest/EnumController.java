package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.beans.model.*;
import ir.tax.trustedtaxpayers.enums.FlightType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enums")
@Tag(name = "Enum Management", description = "مدیریت Enum ها\n\nراهنمای سریع:\n- Base: /api/enums\n- همه یکجا: GET /api/enums\n- جداگانه: /invoice-types | /invoice-patterns | /invoice-subjects | /person-types | /settlement-methods | /flight-types\n")
/**
 * کنترلر سرویسی برای ارائه مقادیر ثابت Enum به کاربر و سرویس‌های مشتری.
 * <p>
 * REST controller exposing enumeration values consumed by clients and front-end applications.
 */
public class EnumController {

    @Operation(
            summary = "لیست تمام Enum ها",
            description = "دریافت لیست تمام Enum های موجود در سیستم شامل انواع صورتحساب، الگوها، موضوعات، انواع شخص، روش‌های تسویه و انواع پرواز",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست Enum ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"invoiceTypes\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"SALES\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"فروش\"\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"PURCHASE\",\n" +
                                                    "      \"value\": 2,\n" +
                                                    "      \"persianName\": \"خرید\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"invoicePatterns\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"PATTERN_1\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"الگوی 1\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"invoiceSubjects\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"ORIGINAL\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"اصلی\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"personTypes\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"NATURAL\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"حقیقی\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"settlementMethods\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"CASH\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"نقدی\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"flightTypes\": [\n" +
                                                    "    {\n" +
                                                    "      \"name\": \"DOMESTIC\",\n" +
                                                    "      \"value\": 1,\n" +
                                                    "      \"persianName\": \"داخلی\"\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    )
            }
    )
    @GetMapping
    public ResponseEntity<Map<String, List<EnumDto>>> getAllEnums() {
        return ResponseEntity.ok(Map.of(
                "invoiceTypes", getInvoiceTypesList(),
                "invoicePatterns", getInvoicePatternsList(),
                "invoiceSubjects", getInvoiceSubjectsList(),
                "personTypes", getPersonTypesList(),
                "settlementMethods", getSettlementMethodsList(),
                "flightTypes", getFlightTypesList()
        ));
    }

    @Operation(
            summary = "نوع صورتحساب",
            description = "دریافت لیست انواع صورتحساب (inty) - مقادیر 1=فروش, 2=خرید, 3=برگشت از فروش",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست انواع صورتحساب با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"SALES\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"فروش\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"PURCHASE\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"خرید\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"RETURN\",\n" +
                                                    "    \"value\": 3,\n" +
                                                    "    \"persianName\": \"برگشت از فروش\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/invoice-types")
    public ResponseEntity<List<EnumDto>> getInvoiceTypes() {
        return ResponseEntity.ok(getInvoiceTypesList());
    }

    private List<EnumDto> getInvoiceTypesList() {
        return Arrays.stream(InvoiceType.values())
                .map(type -> new EnumDto(type.name(), type.getValue(), type.getPersianName()))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "الگوی صورتحساب",
            description = "دریافت لیست الگوهای صورتحساب (inp) - مقادیر 1 تا 13",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست الگوهای صورتحساب با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"PATTERN_1\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"الگوی 1 - صورتحساب کالا و خدمات\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"PATTERN_2\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"الگوی 2 - صورتحساب خدمات\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/invoice-patterns")
    public ResponseEntity<List<EnumDto>> getInvoicePatterns() {
        return ResponseEntity.ok(getInvoicePatternsList());
    }

    private List<EnumDto> getInvoicePatternsList() {
        return Arrays.stream(InvoicePattern.values())
                .map(pattern -> new EnumDto(pattern.name(), pattern.getValue(), pattern.getPersianName()))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "موضوع صورتحساب",
            description = "دریافت لیست موضوعات صورتحساب (ins) - 1=اصلی, 2=اصلاحی, 3=ابطالی, 4=برگشت از فروش",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست موضوعات صورتحساب با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"ORIGINAL\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"اصلی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"CORRECTION\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"اصلاحی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"CANCELLATION\",\n" +
                                                    "    \"value\": 3,\n" +
                                                    "    \"persianName\": \"ابطالی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"RETURN\",\n" +
                                                    "    \"value\": 4,\n" +
                                                    "    \"persianName\": \"برگشت از فروش\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/invoice-subjects")
    public ResponseEntity<List<EnumDto>> getInvoiceSubjects() {
        return ResponseEntity.ok(getInvoiceSubjectsList());
    }

    private List<EnumDto> getInvoiceSubjectsList() {
        return Arrays.stream(InvoiceSubject.values())
                .map(subject -> new EnumDto(subject.name(), subject.getValue(), subject.getPersianName()))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "نوع شخص",
            description = "دریافت لیست انواع شخص (tob) - 1=حقیقی, 2=حقوقی, 3=مشارکت مدنی, 4=اتباع غیر ایرانی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست انواع شخص با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"NATURAL\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"حقیقی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"LEGAL\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"حقوقی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"PARTNERSHIP\",\n" +
                                                    "    \"value\": 3,\n" +
                                                    "    \"persianName\": \"مشارکت مدنی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"FOREIGN\",\n" +
                                                    "    \"value\": 4,\n" +
                                                    "    \"persianName\": \"اتباع غیر ایرانی\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/person-types")
    public ResponseEntity<List<EnumDto>> getPersonTypes() {
        return ResponseEntity.ok(getPersonTypesList());
    }

    private List<EnumDto> getPersonTypesList() {
        return Arrays.stream(PersonType.values())
                .map(type -> new EnumDto(type.name(), type.getValue(), type.getPersianName()))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "روش تسویه",
            description = "دریافت لیست روش‌های تسویه (setm) - 1=نقدی, 2=نسیه, 3=نقدی/نسیه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روش‌های تسویه با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"CASH\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"نقدی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"CREDIT\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"نسیه\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"MIXED\",\n" +
                                                    "    \"value\": 3,\n" +
                                                    "    \"persianName\": \"نقدی/نسیه\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/settlement-methods")
    public ResponseEntity<List<EnumDto>> getSettlementMethods() {
        return ResponseEntity.ok(getSettlementMethodsList());
    }

    private List<EnumDto> getSettlementMethodsList() {
        return Arrays.stream(SettlementMethod.values())
                .map(method -> new EnumDto(method.name(), method.getValue(), method.getPersianName()))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "نوع پرواز",
            description = "دریافت لیست انواع پرواز (ft) - 1=داخلی, 2=خارجی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست انواع پرواز با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = EnumDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"DOMESTIC\",\n" +
                                                    "    \"value\": 1,\n" +
                                                    "    \"persianName\": \"داخلی\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"name\": \"INTERNATIONAL\",\n" +
                                                    "    \"value\": 2,\n" +
                                                    "    \"persianName\": \"خارجی\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/flight-types")
    public ResponseEntity<List<EnumDto>> getFlightTypes() {
        return ResponseEntity.ok(getFlightTypesList());
    }

    private List<EnumDto> getFlightTypesList() {
        return Arrays.stream(FlightType.values())
                .map(type -> new EnumDto(type.name(), type.getValue(), type.getPersianName()))
                .collect(Collectors.toList());
    }

    @Schema(description = "DTO مقادیر Enum")
    public record EnumDto(
            @Schema(description = "نام Enum به انگلیسی") String name,
            @Schema(description = "مقدار عددی") Integer value,
            @Schema(description = "نام فارسی") String persianName
    ) {}
}


