package ir.tax.trustedtaxpayers.interfaces.rest;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.application.service.BaseInfoService;
import ir.tax.trustedtaxpayers.application.service.TaxpayerService;
import ir.tax.trustedtaxpayers.application.usecase.SearchTaxpayersUseCase;
import ir.tax.trustedtaxpayers.core.spec.BaseSpecification;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.enums.BaseInfoCategory;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.BaseInfoEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.*;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.TaxpayerMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت مودیان مالیاتی برای ایجاد، جستجو و گزارش‌گیری بر اساس شماره اقتصادی.
 * <p>
 * REST controller handling taxpayer creation, lookup, and advanced search use cases.
 */
@RestController
@RequestMapping("/api/taxpayers")
@Tag(name = "Taxpayer Management", description = "مدیریت مودیان مالیاتی\n\nراهنمای سریع:\n- Base: /api/taxpayers\n- ایجاد: POST /api/taxpayers/create → {taxId,name} → {id}\n- دریافت/لیست: GET /api/taxpayers/get/{id} | /all\n- جستجو: /by-taxpayer-no/{taxId} | /by-name/{name} | /taxpayer-no/{taxId}/exists\n- فیلتر پیشرفته (page): POST /api/taxpayers/search-custom?page=0&size=10 → [{key,operand,value}]\n")
@PreAuthorize("hasAuthority('ADMIN')")
public class TaxpayerController extends BaseController<TaxpayerEntity, UUID> {
    private final TaxpayerService taxpayerService;
    private final BaseInfoService baseInfoService;
    private final SearchTaxpayersUseCase searchTaxpayersUseCase;
    private final TaxpayerMapper taxpayerMapper;

    public TaxpayerController(TaxpayerService taxpayerService, TaxpayerRepository taxpayerRepository,
                              BaseInfoService baseInfoService,
                              SearchTaxpayersUseCase searchTaxpayersUseCase,
                              TaxpayerMapper taxpayerMapper) {
        super(taxpayerService, taxpayerRepository);
        this.taxpayerService = taxpayerService;
        this.baseInfoService = baseInfoService;
        this.searchTaxpayersUseCase = searchTaxpayersUseCase;
        this.taxpayerMapper = taxpayerMapper;
    }

    @Operation(
            summary = "ایجاد مودی مالیاتی جدید",
            description = "ایجاد یک مودی مالیاتی جدید در سیستم با شماره مالیاتی و نام مشخص",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "مودی مالیاتی با موفقیت ایجاد شد",
                            content = @Content(
                                    schema = @Schema(implementation = ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n  \"success\": true,\n  \"message\": \"Success\",\n  \"data\": { \n    \"id\": \"123e4567-e89b-12d3-a456-426614174000\" \n  }\n}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "شماره مالیاتی تکراری است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping("/create")
    public ResponseEntity<CreatedIdResponse> createTaxpayer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "اطلاعات مودی مالیاتی",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateTaxpayerRequest.class),
                            examples = @ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n" +
                                            "  \"taxId\": \"12345678901\",\n" +
                                            "  \"name\": \"شرکت نمونه\"\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody CreateTaxpayerRequest req) {
        var entity = taxpayerMapper.toEntity(req);
        
        // Convert type title to BaseInfoEntity if provided
        if (req.type() != null && !req.type().trim().isEmpty()) {
            UUID typeId = UUID.fromString(req.type());
            BaseInfoEntity type = baseInfoService.findById(typeId).get();
            if (BaseInfoCategory.PERSON_TYPE.name().equals(type.getCategory())) {
                entity.setType(type);
            }
        }
        
        UUID id = taxpayerService.create(entity);
        String idString = id.toString();
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/taxpayers/get/" + idString)
                .body(new CreatedIdResponse(idString));
    }

    

    @Operation(
            summary = "دریافت اطلاعات مودی بر اساس شناسه",
            description = "دریافت اطلاعات کامل مودی مالیاتی بر اساس شناسه یکتای آن",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "اطلاعات مودی با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = TaxpayerDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"taxId\": \"12345678901\",\n" +
                                                    "  \"name\": \"شرکت نمونه\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "مودی یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/get/{id}")
    public ResponseEntity<TaxpayerDto> getTaxpayerById(
            @PathVariable("id")
            @Schema(description = "شناسه یکتای مودی (encoded)", example = "-XigTP49ST2oD1ogRA65eA")
            String id) {
        UUID uuid = ir.tax.trustedtaxpayers.interfaces.rest.util.EntityDtoMapper.stringToUuid(id);
        if (uuid == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "شناسه نامعتبر است");
        }
        var taxpayer = taxpayerService.findById(uuid).orElseThrow();
        return ResponseEntity.ok(taxpayerMapper.toDto(taxpayer));
    }

    @Operation(
            summary = "دریافت لیست تمام مودیان",
            description = "دریافت لیست تمام مودیان مالیاتی ثبت شده در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست مودیان با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = TaxpayerDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "    \"taxpayerNo\": \"12345678901\",\n" +
                                                    "    \"name\": \"شرکت نمونه 1\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"223e4567-e89b-12d3-a456-426614174001\",\n" +
                                                    "    \"taxpayerNo\": \"12345678902\",\n" +
                                                    "    \"name\": \"شرکت نمونه 2\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/all")
    public ResponseEntity<List<TaxpayerDto>> getAll() {
        var taxpayers = taxpayerService.findAll();
        return ResponseEntity.ok(taxpayerMapper.toDtos(taxpayers));
    }

   /* @Operation(
            summary = "اتوکامپلیت مودیان بر اساس نام یا شماره اقتصادی",
            description = "برای استفاده در جستجوی سریع؛ بیش از ۳۰ نتیجه بازگردانده نمی‌شود.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست کوتاه مودیان برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerDto.class))
                    )
            }
    )
    @GetMapping("/autocomplete")
    public ResponseEntity<List<TaxpayerDto>> autocomplete(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        int max = Math.min(Math.max(limit == null ? 10 : limit, 1), 30);
        int safePage = Math.max(page == null ? 0 : page, 0);
        var results = taxpayerService.search(query, max, safePage);
        return ResponseEntity.ok(taxpayerMapper.toDtos(results));
    }*/

    @Operation(
            summary = "جستجوی مودی بر اساس شماره مالیاتی",
            description = "جستجو و دریافت اطلاعات مودی مالیاتی بر اساس شماره مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "مودی با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = TaxpayerDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"taxId\": \"12345678901\",\n" +
                                                    "  \"name\": \"شرکت نمونه\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "مودی یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-taxpayer-no/{taxpayerNo}")
    public ResponseEntity<TaxpayerDto> findByTaxpayerNo(
            @PathVariable("taxpayerNo")
            @Schema(description = "شماره مالیاتی مودی", example = "12345678901")
            String taxpayerNo) {
        var taxpayer = taxpayerService.findByTaxId(taxpayerNo).orElseThrow();
        return ResponseEntity.ok(taxpayerMapper.toDto(taxpayer));
    }

    @Operation(
            summary = "جستجوی مودیان بر اساس نام",
            description = "جستجو و دریافت لیست مودیان مالیاتی بر اساس نام",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست مودیان با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = TaxpayerDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "    \"taxpayerNo\": \"12345678901\",\n" +
                                                    "    \"name\": \"شرکت نمونه\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-name/{name}")
    public ResponseEntity<List<TaxpayerDto>> findByName(@PathVariable("name") String name) {
        var taxpayers = taxpayerService.findByName(name);
        return ResponseEntity.ok(taxpayerMapper.toDtos(taxpayers));
    }

    @Operation(
            summary = "بررسی وجود مودی بر اساس شماره مالیاتی",
            description = "بررسی وجود مودی در سیستم بر اساس شماره مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتیجه بررسی با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"exists\": true\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/taxpayer-no/{taxpayerNo}/exists")
    public ResponseEntity<ExistsResponse> existsByTaxpayerNo(@PathVariable("taxpayerNo") String taxpayerNo) {
        return ResponseEntity.ok(new ExistsResponse(taxpayerService.existsByTaxId(taxpayerNo)));
    }



    @Operation(
            summary = "جستجوی مودیان با معیار ساده",
            description = "جستجوی مودیان بر اساس یک معیار ساده (کلید، عملگر و مقدار)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping("/findBy")
    public ResponseEntity<List<TaxpayerDto>> findBy(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SearchRequestDto.class),
                            examples = {
                                    @ExampleObject(name = "شماره اقتصادی برابر", value = "{\n  \"key\": \"taxId\",\n  \"operand\": \"=\",\n  \"value\": \"12345678901\"\n}"),
                                    @ExampleObject(name = "نام شامل کلمه شرکت", value = "{\n  \"key\": \"name\",\n  \"operand\": \"like\",\n  \"value\": \"شرکت\"\n}")
                            }
                    )
            )
            @RequestBody SearchRequestDto searchRequest) {
        var taxpayers = taxpayerService.findBy(searchRequest.getKey(), searchRequest.getOperand(), searchRequest.getValue());
        return ResponseEntity.ok(taxpayerMapper.toDtos(taxpayers));
    }

    @Operation(
            summary = "جستجوی مودیان",
            description = "جستجوی مودیان با استفاده از فیلترهای پیشرفته",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "فیلترهای ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping("/search")
    public ResponseEntity<List<TaxpayerDto>> searchTaxpayers(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequest.class),
                            examples = {
                                    @ExampleObject(name = "نام مودی LIKE", value = "[{\n  \"key\": \"name\",\n  \"operand\": \"LIKE\",\n  \"value\": \"شرکت\"\n}]"),
                                    @ExampleObject(name = "شماره اقتصادی برابر", value = "[{\n  \"key\": \"taxId\",\n  \"operand\": \"=\",\n  \"value\": \"12345678901\"\n}]")
                            }
                    )
            )
            @RequestBody List<FilterRequest> filters) {
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            var allTaxpayers = taxpayerService.findAll();
            return ResponseEntity.ok(taxpayerMapper.toDtos(allTaxpayers));
        }
        
        List<FilterRequest> filterRequests = filterCriteria.stream()
                .map(c -> {
                    FilterRequest fr = new FilterRequest();
                    fr.setKey(c.field());
                    fr.setOperand(c.operand());
                    fr.setValue(c.value());
                    return fr;
                })
                .toList();
        
        BaseSpecification<TaxpayerEntity> spec = new BaseSpecification<>(filterRequests);
        var filteredTaxpayers = repository.findAll(spec);
        
        return ResponseEntity.ok(taxpayerMapper.toDtos(filteredTaxpayers));
    }


}


