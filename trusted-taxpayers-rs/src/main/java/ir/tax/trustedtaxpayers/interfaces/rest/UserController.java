package ir.tax.trustedtaxpayers.interfaces.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.SearchResult;
import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.application.service.AuditLogService;
import ir.tax.trustedtaxpayers.application.service.UserRoleService;
import ir.tax.trustedtaxpayers.application.usecase.AssignRoleUseCase;
import ir.tax.trustedtaxpayers.application.usecase.CreateUserUseCase;
import ir.tax.trustedtaxpayers.application.usecase.SearchUsersUseCase;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.*;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.UserMapper;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.UserRoleMapper;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * کنترلر مدیریت کاربران و نقش‌ها برای عملیات ایجاد، جستجو، تخصیص نقش و خروجی گزارش.
 * <p>
 * REST controller handling user CRUD, role assignments, advanced filters, and export endpoints.
 */
@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
@Tag(name = "User Management", description = "مدیریت کاربران و نقش‌ها\n\nراهنمای سریع:\n- Base: /api/users\n- هدرها: Content-Type: application/json و Authorization: Bearer <JWT>\n- صفحه‌بندی: page از 0، size پیش‌فرض 20، sortBy و asc\n- نمونه‌ها:\n  * ایجاد کاربر: POST /api/users → {username,password,email} → {id}\n  * نقش‌های کاربر: GET /api/users/{id}/roles\n  * جستجو: POST /api/users/search?page=0&size=20 → [{key:\"username\",operand:\"LIKE\",value:\"test\"}]\n")
@Validated
@PreAuthorize("hasAuthority('ADMIN')")
public class UserController {
    private final UserRoleService userRoleService;
    private final SearchUsersUseCase searchUsersUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final AssignRoleUseCase assignRoleUseCase;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final ir.tax.trustedtaxpayers.interfaces.rest.mapper.RoleMapper roleMapper;
    private final ir.tax.trustedtaxpayers.interfaces.rest.util.ReportExporter reportExporter;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public UserController(UserRoleService userRoleService, SearchUsersUseCase searchUsersUseCase,
                          CreateUserUseCase createUserUseCase, AssignRoleUseCase assignRoleUseCase,
                          UserMapper userMapper, UserRoleMapper userRoleMapper,
                          ir.tax.trustedtaxpayers.interfaces.rest.mapper.RoleMapper roleMapper,
                          ir.tax.trustedtaxpayers.interfaces.rest.util.ReportExporter reportExporter,
                          AuditLogService auditLogService,
                          ObjectMapper objectMapper) {
        this.userRoleService = userRoleService;
        this.searchUsersUseCase = searchUsersUseCase;
        this.createUserUseCase = createUserUseCase;
        this.assignRoleUseCase = assignRoleUseCase;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.reportExporter = reportExporter;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "ایجاد کاربر جدید",
            description = "ایجاد یک کاربر جدید در سیستم با نام کاربری، رمز عبور و ایمیل",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "کاربر با موفقیت ایجاد شد",
                            content = @Content(
                                    schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.ApiResponse.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n  \"success\": true,\n  \"message\": \"Success\",\n  \"data\": { \n    \"id\": \"123e4567-e89b-12d3-a456-426614174000\" \n  }\n}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "اطلاعات ورودی نامعتبر"),
                    @ApiResponse(responseCode = "409", description = "نام کاربری یا ایمیل تکراری است"),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping
    @PreAuthorize("permitAll()")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.CreatedIdResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "اطلاعات کاربر",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateUserRequest.class),
                            examples = @ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n" +
                                            "  \"username\": \"testuser\",\n" +
                                            "  \"password\": \"$2a$10$hashedpassword...\",\n" +
                                            "  \"email\": \"test@example.com\"\n" +
                                            "}"
                            )
                    )
            )
            @jakarta.validation.Valid @RequestBody ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateUserRequest req) {
        UUID id = createUserUseCase.execute(req.username(), req.password(), req.email());
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.created(id);
    }

    @Operation(
            summary = "اختصاص نقش به کاربر",
            description = "اختصاص یک نقش به کاربر بر اساس شناسه کاربر و نام نقش",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نقش با موفقیت به کاربر اختصاص داده شد"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کاربر یا نقش یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping("/{id}/roles/{role}")
    public ResponseEntity<Void> assign(@PathVariable("id") UUID id, @PathVariable("role") String role) {
        assignRoleUseCase.execute(id, role);
        return ResponseEntity.ok().build();
    }

   /* @Operation(
            summary = "دریافت نقش‌های کاربر",
            description = "دریافت لیست تمام نقش‌های اختصاص داده شده به یک کاربر",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست نقش‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = UserRoleDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کاربر یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<UserRoleDto>> roles(@PathVariable("id") UUID id) {
        var items = userRoleMapper.toDtos(userRoleService.rolesOfUser(id));
        return ResponseEntity.ok(items);
    }*/

    @Operation(
            summary = "جستجوی کاربران با صفحه‌بندی",
            description = "جستجوی کاربران با استفاده از فیلترهای پیشرفته و صفحه‌بندی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = SearchResult.class))
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
    public ResponseEntity<SearchResult<UserDto>> search(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequest.class),
                            examples = {
                                    @ExampleObject(name = "نام کاربری LIKE", value = "[{\n  \"key\": \"username\",\n  \"operand\": \"LIKE\",\n  \"value\": \"test\"\n}]"),
                                    @ExampleObject(name = "ایمیل برابر", value = "[{\n  \"key\": \"email\",\n  \"operand\": \"=\",\n  \"value\": \"test@example.com\"\n}]")
                            }
                    )
            )
            @RequestBody List<FilterRequest> filters,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "asc", required = false) Boolean asc
    ) {
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);
        var result = searchUsersUseCase.execute(filterCriteria, page, size, sortBy, asc);
        var dtoItems = userMapper.toDtosFromSummary(result.items());
        return ResponseEntity.ok(new SearchResult<>(dtoItems, result.total(), result.page(), result.size()));
    }

    // متدهای تکراری تبدیل/پارس فیلتر حذف شد؛ از FilterCriteriaConverter استفاده می‌شود.

    @Operation(
            summary = "دریافت لیست تمام کاربران",
            description = "دریافت لیست تمام کاربران ثبت شده در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست کاربران با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = UserDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "    \"username\": \"testuser\",\n" +
                                                    "    \"email\": \"test@example.com\"\n" +
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
    public ResponseEntity<List<UserDto>> getAll() {
        var users = userRoleService.getAllUsers();
        return ResponseEntity.ok(userMapper.toDtos(users));
    }

    @Operation(
            summary = "دریافت اطلاعات کاربر بر اساس شناسه",
            description = "دریافت اطلاعات کامل کاربر بر اساس شناسه یکتای آن",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "اطلاعات کاربر با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = UserDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"username\": \"testuser\",\n" +
                                                    "  \"email\": \"test@example.com\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(responseCode = "404", description = "کاربر یافت نشد"),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> findById(
            @PathVariable("id")
            @Schema(description = "شناسه یکتای کاربر", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID id) {
        var user = userRoleService.findUserById(id).orElseThrow();
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @Operation(
            summary = "جستجوی کاربر بر اساس نام کاربری",
            description = "دریافت اطلاعات کاربر بر اساس نام کاربری",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "کاربر با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = UserDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"username\": \"testuser\",\n" +
                                                    "  \"email\": \"test@example.com\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کاربر یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserDto> findByUsername(@PathVariable("username") @Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$", message = "error.validation") String username) {
        var user = userRoleService.findUserByUsername(username).orElseThrow();
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @Operation(
            summary = "جستجوی کاربران بر اساس وضعیت",
            description = "دریافت لیست کاربران بر اساس وضعیت آن‌ها",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست کاربران با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = UserDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<UserDto>> findByStatus(@PathVariable("status") @Pattern(regexp = "^(ACTIVE|INACTIVE|PENDING)$", message = "error.validation") String status) {
        var users = userRoleService.findUsersByStatus(status);
        return ResponseEntity.ok(userMapper.toDtos(users));
    }

    @Operation(
            summary = "بررسی وجود کاربر بر اساس شناسه",
            description = "بررسی وجود کاربر در سیستم بر اساس شناسه",
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
    @GetMapping("/{id}/exists")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.ExistsResponse> exists(@PathVariable("id") UUID id) {
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.exists(userRoleService.userExistsById(id));
    }

    @Operation(
            summary = "دریافت تاریخچه فعالیت کاربر",
            description = "آخرین فعالیت‌های ثبت شده برای این کاربر شامل ایجاد، ویرایش، فعال‌سازی و مدیریت نقش‌ها را برمی‌گرداند.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست فعالیت‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = UserActivityDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کاربر یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/{id}/activity")
    public ResponseEntity<List<UserActivityDto>> activity(@PathVariable("id") UUID id) {
        userRoleService.findUserById(id).orElseThrow();
        var logs = auditLogService.findByEntityNameAndEntityId("USER", id.toString());
        Comparator<AuditLogEntity> byEventTimeDesc = (left, right) -> {
            java.time.OffsetDateTime leftTime = left.getCreatedOn();
            java.time.OffsetDateTime rightTime = right.getCreatedOn();
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        };
        var dtos = logs.stream()
                .sorted(byEventTimeDesc)
                .map(this::toUserActivityDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "بررسی وجود کاربر بر اساس نام کاربری",
            description = "بررسی وجود کاربر در سیستم بر اساس نام کاربری",
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
    @GetMapping("/username/{username}/exists")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.ExistsResponse> existsByUsername(@PathVariable("username") String username) {
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.exists(userRoleService.userExistsByUsername(username));
    }

    @Operation(
            summary = "جستجوی ساده کاربران",
            description = "جستجوی کاربران با استفاده از فیلترهای ساده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = UserDto.class))
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
    @PostMapping("/search-simple")
    public ResponseEntity<List<UserDto>> searchSimple(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequest.class),
                            examples = {
                                    @ExampleObject(name = "همه کاربران (بدون فیلتر)", value = "[]"),
                                    @ExampleObject(name = "نام کاربری شامل 'test'", value = "[{\n  \"key\": \"username\",\n  \"operand\": \"LIKE\",\n  \"value\": \"test\"\n}]")
                            }
                    )
            )
            @RequestBody List<FilterRequest> filters) {
        if (filters == null || filters.isEmpty()) {
            var users = userRoleService.getAllUsers();
            return ResponseEntity.ok(userMapper.toDtos(users));
        }
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);
        var result = searchUsersUseCase.execute(filterCriteria, null, null, null, null);
        var dtoItems = userMapper.toDtosFromSummary(result.items());
        return ResponseEntity.ok(dtoItems);
    }

    @Operation(
            summary = "به‌روزرسانی کاربر",
            description = "به‌روزرسانی اطلاعات کاربر موجود بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "کاربر با موفقیت به‌روزرسانی شد",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کاربر یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    @PutMapping("/{id}")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.CreatedIdResponse> update(@PathVariable("id") UUID id,
                                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                 required = true,
                                                                 content = @Content(
                                                                         schema = @Schema(implementation = UpdateUserRequest.class),
                                                                         examples = @ExampleObject(
                                                                                 name = "نمونه درخواست",
                                                                                 value = "{\n  \"username\": \"testuser\",\n  \"email\": \"new@example.com\",\n  \"status\": \"ACTIVE\",\n  \"active\": true\n}"
                                                                         )
                                                                 )
                                                         )
                                                         @jakarta.validation.Valid @RequestBody UpdateUserRequest req) {
        var updated = userRoleService.updateUser(id, req.username(), req.email(), req.status(), req.active());
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.created(updated.getId());
    }

    @Operation(
            summary = "حذف کاربر",
            description = "کاربر انتخاب شده را به طور کامل از سیستم حذف می‌کند.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "کاربر با موفقیت حذف شد"),
                    @ApiResponse(responseCode = "404", description = "کاربر یافت نشد", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        userRoleService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "تنظیم رمز عبور کاربر",
            description = "رمز عبور کاربر را با استفاده از هش ارسال شده به‌روزرسانی می‌کند."
    )
    @PreAuthorize("hasAnyAuthority('ADMIN','ROLE_ADMIN')")
    @PostMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable("id") UUID id,
                                               @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                       required = true,
                                                       content = @Content(
                                                               schema = @Schema(implementation = SetUserPasswordRequest.class),
                                                               examples = @ExampleObject(
                                                                       name = "نمونه درخواست",
                                                                       value = "{\n  \"password\": \"$2a$10$hashedpassword...\" \n}"
                                                               )
                                                       )
                                               )
                                               @jakarta.validation.Valid @RequestBody SetUserPasswordRequest request) {
        userRoleService.setPassword(id, request.password());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "فعال‌سازی کاربر",
            description = "کاربر را فعال می‌کند تا بتواند وارد سیستم شود."
    )
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable("id") UUID id) {
        userRoleService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "غیرفعال‌سازی کاربر",
            description = "دسترسی کاربر را مسدود می‌کند."
    )
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable("id") UUID id) {
        userRoleService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    // Role endpoints
    @Operation(
            summary = "دریافت لیست تمام نقش‌ها",
            description = "دریافت لیست تمام نقش‌های تعریف شده در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست نقش‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/roles/all")
    public ResponseEntity<List<ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto>> getAllRoles() {
        var roles = userRoleService.getAllRoles();
        return ResponseEntity.ok(roles.stream().map(roleMapper::toDto).toList());
    }

    @Operation(
            summary = "ایجاد نقش جدید",
            description = "ایجاد یک نقش جدید در سیستم با نام و توضیحات",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نقش با موفقیت ایجاد شد",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\"\n" +
                                                    "}"
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
                            description = "نام نقش تکراری است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "422", description = "خطای اعتبارسنجی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @PostMapping("/roles")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.CreatedIdResponse> createRole(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"name\": \"ADMIN\",\n  \"description\": \"نقش مدیر سیستم\"\n}"
                            )
                    )
            )
            @RequestBody Map<String, String> req) {
        UUID id = userRoleService.createRole(req.get("name"), req.get("description"));
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.created(id);
    }

    @Operation(
            summary = "دریافت اطلاعات نقش بر اساس شناسه",
            description = "دریافت اطلاعات کامل نقش بر اساس شناسه یکتای آن",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نقش با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"name\": \"ADMIN\",\n" +
                                                    "  \"description\": \"مدیر سیستم\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "نقش یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/roles/{id}")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto> findRoleById(@PathVariable("id") UUID id) {
        var role = userRoleService.findRoleById(id).orElseThrow();
        return ResponseEntity.ok(roleMapper.toDto(role));
    }

    @Operation(
            summary = "جستجوی نقش بر اساس نام",
            description = "دریافت اطلاعات نقش بر اساس نام نقش",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نقش با موفقیت یافت شد",
                            content = @Content(schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "نقش یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "عدم احراز هویت"),
                    @ApiResponse(responseCode = "403", description = "عدم دسترسی"),
                    @ApiResponse(responseCode = "500", description = "خطای داخلی سرور")
            }
    )
    @GetMapping("/roles/by-name/{name}")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto> findRoleByName(@PathVariable("name") String name) {
        var role = userRoleService.findRoleByName(name).orElseThrow();
        return ResponseEntity.ok(roleMapper.toDto(role));
    }

    @Operation(
            summary = "بررسی وجود نقش بر اساس شناسه",
            description = "بررسی وجود نقش در سیستم بر اساس شناسه",
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
    @GetMapping("/roles/{id}/exists")
    public ResponseEntity<Map<String, Boolean>> roleExists(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(Map.of("exists", userRoleService.roleExistsById(id)));
    }

    @Operation(
            summary = "جستجوی نقش‌ها",
            description = "جستجوی نقش‌ها با استفاده از فیلترهای پیشرفته",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto.class))
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
    @PostMapping("/roles/search")
    public ResponseEntity<List<ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto>> searchRoles(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = false,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequest.class),
                            examples = {
                                    @ExampleObject(name = "نام نقش LIKE", value = "[{\n  \"key\": \"name\",\n  \"operand\": \"LIKE\",\n  \"value\": \"ADM\"\n}]")
                            }
                    )
            )
            @RequestBody List<FilterRequest> filters) {
        // برای roles، از فیلترها استفاده می‌کنیم
        // اگر فیلتری نباشد، همه roles را برمی‌گردانیم
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);
        if (filterCriteria == null || filterCriteria.isEmpty()) {
            var allRoles = userRoleService.getAllRoles();
            return ResponseEntity.ok(allRoles.stream()
                    .map(r -> new ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto(r.getId(), r.getName(), r.getTitle(), r.getDescription()))
                    .toList());
        }
        // با فیلترها جستجو می‌کنیم
        // برای سادگی، از searchRoles با query string استفاده می‌کنیم
        // اما می‌توانیم از findBy استفاده کنیم
        var roles = userRoleService.getAllRoles();
        var filteredRoles = ir.tax.trustedtaxpayers.interfaces.rest.util.RoleFilter.apply(roles, filterCriteria)
                .stream()
                .map(roleMapper::toDto)
                .toList();
        return ResponseEntity.ok(filteredRoles);
    }

    @Operation(
            summary = "به‌روزرسانی نقش",
            description = "به‌روزرسانی اطلاعات نقش موجود بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نقش با موفقیت به‌روزرسانی شد",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\"\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "نقش یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
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
    @PutMapping("/roles/{id}")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.CreatedIdResponse> updateRole(@PathVariable("id") UUID id,
                                                           @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                   required = true,
                                                                   content = @Content(
                                                                           schema = @Schema(implementation = ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity.class),
                                                                           examples = @ExampleObject(
                                                                                   name = "نمونه درخواست",
                                                                                   value = "{\n  \"name\": \"ADMIN\",\n  \"description\": \"نقش مدیر سیستم\"\n}"
                                                                           )
                                                                   )
                                                           )
                                                           @RequestBody ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity req) {
        var updated = userRoleService.updateRole(id, req);
        return ir.tax.trustedtaxpayers.interfaces.rest.util.ResponseFactory.created(updated.getId());
    }

    @Operation(
            summary = "خروجی گزارش کاربران (CSV/PDF)",
            description = "بر اساس فیلترهای جستجو، تمام نتایج صفحه‌به‌صفحه جمع‌آوری و به صورت CSV یا PDF خروجی داده می‌شود."
    )
    @PostMapping(value = "/export")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(name = "format", required = false, defaultValue = "csv") String format,
            @RequestParam(name = "title", required = false) String title,
            @RequestBody(required = false) List<FilterRequest> filters
    ) {
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);

        int page = 0;
        int size = 100;
        long total;
        java.util.List<UserDto> all = new java.util.ArrayList<>();
        do {
            var result = searchUsersUseCase.execute(filterCriteria, page, size, "username", true);
            var dtos = userMapper.toDtosFromSummary(result.items());
            all.addAll(dtos);
            total = result.total();
            page++;
        } while (all.size() < total);

        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        HttpHeaders headers = new HttpHeaders();

        if ("pdf".equalsIgnoreCase(format)) {
            byte[] bytes = reportExporter.exportUsersToPdf(all, title);
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-" + ts + ".pdf");
            return ResponseEntity.ok().headers(headers).body(bytes);
        } else {
            byte[] bytes = reportExporter.exportUsersToCsv(all);
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-" + ts + ".csv");
            return ResponseEntity.ok().headers(headers).body(bytes);
        }
    }

    @Operation(
            summary = "خروجی CSV کاربران",
            description = "بر اساس فیلترها، تمام نتایج به صورت CSV خروجی داده می‌شود."
    )
    @PostMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportUsersCsv(
            @RequestBody(required = false) List<FilterRequest> filters
    ) {
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);

        int page = 0;
        int size = 100;
        long total;
        java.util.List<UserDto> all = new java.util.ArrayList<>();
        do {
            var result = searchUsersUseCase.execute(filterCriteria, page, size, "username", true);
            var dtos = userMapper.toDtosFromSummary(result.items());
            all.addAll(dtos);
            total = result.total();
            page++;
        } while (all.size() < total);

        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        HttpHeaders headers = new HttpHeaders();
        byte[] bytes = reportExporter.exportUsersToCsv(all);
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-" + ts + ".csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @Operation(
            summary = "خروجی PDF کاربران",
            description = "بر اساس فیلترها، تمام نتایج به صورت PDF خروجی داده می‌شود."
    )
    @PostMapping(value = "/export/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportUsersPdf(
            @RequestParam(name = "title", required = false) String title,
            @RequestBody(required = false) List<FilterRequest> filters
    ) {
        List<FilterCriterionDto> filterCriteria = ir.tax.trustedtaxpayers.interfaces.rest.util.FilterCriteriaConverter.toCriteria(filters);

        int page = 0;
        int size = 100;
        long total;
        java.util.List<UserDto> all = new java.util.ArrayList<>();
        do {
            var result = searchUsersUseCase.execute(filterCriteria, page, size, "username", true);
            var dtos = userMapper.toDtosFromSummary(result.items());
            all.addAll(dtos);
            total = result.total();
            page++;
        } while (all.size() < total);

        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        HttpHeaders headers = new HttpHeaders();
        byte[] bytes = reportExporter.exportUsersToPdf(all, title);
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users-" + ts + ".pdf");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private UserActivityDto toUserActivityDto(AuditLogEntity entity) {
        Map<String, Object> payload = parseActivityDetails(entity.getDetails());
        Map<String, Object> metadata = extractMetadata(payload);
        String ipAddress = extractIp(payload);
        return new UserActivityDto(
                entity.getId(),
                entity.getCreatedOn(),
                entity.getAction(),
                entity.getCreatedBy(),
                ipAddress,
                metadata
        );
    }

    private Map<String, Object> parseActivityDetails(String details) {
        if (details == null || details.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(details, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("metadata", Map.of("rawDetails", details));
        }
    }

    private Map<String, Object> extractMetadata(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyMap();
        }
        Object meta = payload.get("metadata");
        if (meta instanceof Map<?, ?> metaMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : metaMap.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    private String extractIp(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        Object ip = payload.get("ipAddress");
        if (ip instanceof String) {
            String value = ((String) ip).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}



