package ir.tax.trustedtaxpayers.interfaces.rest;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.rate.RateLimited;
import ir.tax.trustedtaxpayers.application.service.FormSchemaService;
import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.core.service.BaseService;
import ir.tax.trustedtaxpayers.core.spec.BaseSpecification;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.util.PaginationHeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * کنترلر پایه برای عملیات CRUD مشترک
 */
@RateLimited(perMinute = 100)
@Validated
public abstract class BaseController<T, ID> {

    protected final BaseService<T, ID> service;
    protected final BaseRepository<T, ID> repository;
    @Autowired(required = false)
    protected ir.tax.trustedtaxpayers.interfaces.rest.util.ReportExporter reportExporter;
    @Autowired(required = false)
    protected org.springframework.context.ApplicationEventPublisher eventPublisher;
    
    @Autowired(required = false)
    protected FormSchemaService formSchemaService;

    protected BaseController(BaseService<T, ID> service, BaseRepository<T, ID> repository) {
        this.service = service;
        this.repository = repository;
    }

    @Operation(
            summary = "ایجاد رکورد جدید",
            description = "ایجاد یک رکورد جدید در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رکورد با موفقیت ایجاد شد",
                            content = @Content(schema = @Schema())
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "رکورد تکراری است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> create(@Valid @RequestBody T entity) {
        preCreate(entity);
        T toSave = transformBeforeCreate(entity);
        T saved = service.save(toSave);
        postCreate(saved);
        return ResponseEntity.ok(saved);
    }

    @Operation(
            summary = "به‌روزرسانی رکورد",
            description = "به‌روزرسانی یک رکورد موجود بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رکورد با موفقیت به‌روزرسانی شد",
                            content = @Content(schema = @Schema())
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رکورد یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @Hidden
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<T> update(
            @PathVariable
            @Schema(description = "شناسه یکتای رکورد", required = true)
            ID id,
            @Valid @RequestBody T entity) {
        preUpdate(id, entity);
        T toUpdate = transformBeforeUpdate(id, entity);
        T updated = service.update(id, toUpdate);
        postUpdate(id, updated);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "دریافت رکورد بر اساس شناسه",
            description = "دریافت اطلاعات کامل یک رکورد بر اساس شناسه یکتای آن",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رکورد با موفقیت یافت شد",
                            content = @Content(schema = @Schema())
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رکورد یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<T> findById(
            @PathVariable
            @Schema(description = "شناسه یکتای رکورد", required = true)
            ID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ir.tax.trustedtaxpayers.application.exception.ResourceNotFoundException("رکورد با شناسه " + id + " یافت نشد"));
    }

    @Operation(
            summary = "دریافت لیست تمام رکوردها",
            description = "دریافت لیست تمام رکوردهای موجود در سیستم",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست رکوردها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping
    public ResponseEntity<List<T>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @Operation(
            summary = "حذف رکورد",
            description = "حذف یک رکورد بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "رکورد با موفقیت حذف شد"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رکورد یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable
            @Schema(description = "شناسه یکتای رکورد", required = true)
            ID id) {
        preDelete(id);
        service.deleteById(id);
        postDelete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "بررسی وجود رکورد",
            description = "بررسی وجود یک رکورد بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتیجه بررسی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(type = "boolean"))
                    )
            }
    )
    @GetMapping("/exists/{id}")
    public ResponseEntity<Boolean> exists(
            @PathVariable
            @Schema(description = "شناسه یکتای رکورد", required = true)
            ID id) {
        return ResponseEntity.ok(service.exists(id));
    }

    @Operation(
            summary = "جستجوی پیشرفته با فیلتر",
            description = "جستجوی رکوردها با استفاده از فیلترهای پیشرفته و صفحه‌بندی",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FilterRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "یک فیلتر (Object)",
                                            value = "{\n  \"key\": \"tins\",\n  \"operand\": \"=\",\n  \"value\": \"12345678901\"\n}"
                                    ),
                                    @ExampleObject(
                                            name = "چند فیلتر (Array)",
                                            value = "[\n  {\n    \"key\": \"templateCode\",\n    \"operand\": \"=\",\n    \"value\": \"TEMPLATE_01\"\n  },\n  {\n    \"key\": \"inty\",\n    \"operand\": \">=\",\n    \"value\": 1\n  }\n]"
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتایج جستجو با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema())
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "فیلترهای ورودی نامعتبر",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<T>> search(
            @RequestBody List<FilterRequest> filters,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (شروع از 0)", example = "0")
            @Min(0) int page,
            @RequestParam(defaultValue = "10")
            @Schema(description = "تعداد رکوردها در هر صفحه", example = "10")
            @Min(1) @Max(200) int size,
            HttpServletRequest request) {
        // TODO: Move all pagination and specification logic to the service layer.
        int safeSize = Math.max(1, Math.min(size, 200));
        int safePage = Math.max(0, page);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Specification<T> spec = new BaseSpecification<>(filters);
        Page<T> result = (repository).findAll(spec, pageable);
        org.springframework.http.HttpHeaders headers = PaginationHeaderUtil
                .buildLinkHeaders(result, safePage, safeSize, request.getRequestURI());
        return ResponseEntity.ok().headers(headers).body(result);
    }

    @Operation(
            summary = "خروجی CSV عمومی",
            description = "بر اساس فیلترها تمام رکوردها (بدون pagination) استخراج و به CSV خروجی داده می‌شود."
    )
    @PostMapping(value = "/export/csv", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestBody List<FilterRequest> filters,
            @RequestParam(name = "fields", required = false) String fields
    ) {
        if (reportExporter == null) {
            return ResponseEntity.status(501).body("ReportExporter not available".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        Specification<T> spec = new BaseSpecification<>(filters == null ? java.util.List.of() : filters);
        java.util.List<T> all = ((org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) repository).findAll(spec);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (T item : all) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> row = mapper.convertValue(item, java.util.Map.class);
            rows.add(row);
        }

        java.util.List<String> headers;
        if (fields != null && !fields.isBlank()) {
            headers = java.util.Arrays.stream(fields.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            if (!rows.isEmpty()) {
                keys.addAll(rows.get(0).keySet());
            }
            headers = new java.util.ArrayList<>(keys);
        }

        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        HttpHeaders httpHeaders = new HttpHeaders();
        byte[] bytes = reportExporter.exportToCsv(rows, headers);
        httpHeaders.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=UTF-8"));
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + getEntityClass().getSimpleName().toLowerCase() + "-" + ts + ".csv");
        return ResponseEntity.ok().headers(httpHeaders).body(bytes);
    }

    @Operation(
            summary = "خروجی PDF عمومی",
            description = "بر اساس فیلترها تمام رکوردها (بدون pagination) استخراج و به PDF خروجی داده می‌شود."
    )
    @PostMapping(value = "/export/pdf", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(
            @RequestBody List<FilterRequest> filters,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "fields", required = false) String fields
    ) {
        if (reportExporter == null) {
            return ResponseEntity.status(501).body("ReportExporter not available".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        Specification<T> spec = new BaseSpecification<>(filters == null ? java.util.List.of() : filters);
        java.util.List<T> all = ((org.springframework.data.jpa.repository.JpaSpecificationExecutor<T>) repository).findAll(spec);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        for (T item : all) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> row = mapper.convertValue(item, java.util.Map.class);
            rows.add(row);
        }

        java.util.List<String> headers;
        if (fields != null && !fields.isBlank()) {
            headers = java.util.Arrays.stream(fields.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            if (!rows.isEmpty()) {
                keys.addAll(rows.get(0).keySet());
            }
            headers = new java.util.ArrayList<>(keys);
        }

        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        HttpHeaders httpHeaders = new HttpHeaders();
        byte[] bytes = reportExporter.exportToPdf(rows, headers, title);
        httpHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + getEntityClass().getSimpleName().toLowerCase() + "-" + ts + ".pdf");
        return ResponseEntity.ok().headers(httpHeaders).body(bytes);
    }

    /**
     * هندلر خطاهای Validation برای بدنه درخواست (@Valid)
     */
    // moved to GlobalExceptionHandler

    /**
     * هندلر خطاهای اعتبارسنجی سطح پارامتر (@Validated)
     */
    // moved to GlobalExceptionHandler

    /**
     * بدنه JSON نامعتبر/قابل پارس نیست
     */
    // moved to GlobalExceptionHandler

    /**
     * منابعی که یافت نمی‌شوند
     */
    // moved to GlobalExceptionHandler

    /**
     * خطاهای عمومی ورودی نامعتبر
     */
    // moved to GlobalExceptionHandler

    /**
     * تبدیل request body به لیست FilterRequest
     * پشتیبانی از هر دو فرمت: object واحد یا array
     */
    protected List<FilterRequest> parseFilters(JsonNode requestBody) {
        if (requestBody == null || requestBody.isNull()) {
            return List.of();
        }

        ObjectMapper mapper = new ObjectMapper();
        List<FilterRequest> filters = new ArrayList<>();

        if (requestBody.isArray()) {
            // اگر array است
            for (JsonNode node : requestBody) {
                try {
                    FilterRequest filter = mapper.treeToValue(node, FilterRequest.class);
                    if (filter != null && filter.getKey() != null) {
                        filters.add(filter);
                    }
                } catch (Exception e) {
                    // ignore invalid entries
                }
            }
        } else if (requestBody.isObject()) {
            // اگر object است
            try {
                FilterRequest filter = mapper.treeToValue(requestBody, FilterRequest.class);
                if (filter != null && filter.getKey() != null) {
                    filters.add(filter);
                }
            } catch (Exception e) {
                // ignore invalid entries
            }
        }

        return filters;
    }

    /**
     * دریافت Entity Class از generic type parameter
     */
    @SuppressWarnings("unchecked")
    protected Class<T> getEntityClass() {
        Type superclass = this.getClass().getGenericSuperclass();
        if (superclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) superclass;
            Type[] types = parameterizedType.getActualTypeArguments();
            if (types.length > 0 && types[0] instanceof Class) {
                return (Class<T>) types[0];
            }
        }
        // Fallback: try to get from first entity in repository
        try {
            List<T> all = service.findAll();
            if (all != null && !all.isEmpty()) {
                T first = all.get(0);
                if (first != null) {
                    return (Class<T>) first.getClass();
                }
            }
        } catch (Exception e) {
            // Log and continue to throw exception
        }
        throw new IllegalStateException("نمی‌توان نوع Entity را تشخیص داد. لطفا متد getEntityClass() را override کنید.");
    }

    // ===== Template Method hooks (SOLID-friendly extension points) =====
    protected void preCreate(T entity) {}
    protected T transformBeforeCreate(T entity) { return entity; }
    protected void postCreate(T createdEntity) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ir.tax.trustedtaxpayers.interfaces.rest.events.EntityCreatedEvent<>(getEntityClass(), createdEntity));
        }
    }

    protected void preUpdate(ID id, T entity) {}
    protected T transformBeforeUpdate(ID id, T entity) { return entity; }
    protected void postUpdate(ID id, T updatedEntity) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ir.tax.trustedtaxpayers.interfaces.rest.events.EntityUpdatedEvent<>(getEntityClass(), id, updatedEntity));
        }
    }

    protected void preDelete(ID id) {}
    protected void postDelete(ID id) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new ir.tax.trustedtaxpayers.interfaces.rest.events.EntityDeletedEvent<>(getEntityClass(), id));
        }
    }

    // ===== Default Observer behavior applied in hooks above =====

    @Operation(
            summary = "دریافت Schema فرم برای Angular Form Builder",
            description = "دریافت JSON Schema برای استفاده در Angular Form Builder. این endpoint لیستی از فیلدهای فرم را با مشخصات کامل (نوع، برچسب، اجباری بودن و ...) برمی‌گرداند.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = FormSchemaDto.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"entityName\": \"TemplateEntity\",\n" +
                                                    "  \"title\": \"فرم TemplateEntity\",\n" +
                                                    "  \"description\": \"فرم ایجاد و ویرایش TemplateEntity\",\n" +
                                                    "  \"fields\": [\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"code\",\n" +
                                                    "      \"type\": \"text\",\n" +
                                                    "      \"name\": \"Code\",\n" +
                                                    "      \"label\": \"Code\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"maxLength\": 10,\n" +
                                                    "      \"minLength\": 3,\n" +
                                                    "      \"pattern\": \"^[A-Z0-9]+$\",\n" +
                                                    "      \"validationMessage\": \"کد باید شامل حروف بزرگ و اعداد باشد\",\n" +
                                                    "      \"placeholder\": \"لطفا Code را وارد کنید\",\n" +
                                                    "      \"order\": 0,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"name\",\n" +
                                                    "      \"type\": \"text\",\n" +
                                                    "      \"name\": \"Name\",\n" +
                                                    "      \"label\": \"Name\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"maxLength\": 200,\n" +
                                                    "      \"minLength\": 2,\n" +
                                                    "      \"placeholder\": \"لطفا Name را وارد کنید\",\n" +
                                                    "      \"order\": 1,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"requiredFields\",\n" +
                                                    "      \"type\": \"multiline\",\n" +
                                                    "      \"name\": \"Required Fields\",\n" +
                                                    "      \"label\": \"Required Fields\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"mandatory\": false,\n" +
                                                    "      \"placeholder\": \"لطفا Required Fields را وارد کنید\",\n" +
                                                    "      \"order\": 2,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"createdOn\",\n" +
                                                    "      \"type\": \"datetime\",\n" +
                                                    "      \"name\": \"Created On\",\n" +
                                                    "      \"label\": \"Created On\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"mandatory\": false,\n" +
                                                    "      \"readonly\": true,\n" +
                                                    "      \"defaultValue\": null,\n" +
                                                    "      \"placeholder\": \"لطفا Created On را وارد کنید\",\n" +
                                                    "      \"order\": 3,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"isActive\",\n" +
                                                    "      \"type\": \"checkbox\",\n" +
                                                    "      \"name\": \"Is Active\",\n" +
                                                    "      \"label\": \"Is Active\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"defaultValue\": false,\n" +
                                                    "      \"placeholder\": \"لطفا Is Active را وارد کنید\",\n" +
                                                    "      \"order\": 4,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"status\",\n" +
                                                    "      \"type\": \"combobox\",\n" +
                                                    "      \"name\": \"Status\",\n" +
                                                    "      \"label\": \"Status\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"mandatory\": false,\n" +
                                                    "      \"options\": [\n" +
                                                    "        {\"value\": \"ACTIVE\", \"label\": \"Active\"},\n" +
                                                    "        {\"value\": \"INACTIVE\", \"label\": \"Inactive\"},\n" +
                                                    "        {\"value\": \"PENDING\", \"label\": \"Pending\"}\n" +
                                                    "      ],\n" +
                                                    "      \"defaultValue\": \"ACTIVE\",\n" +
                                                    "      \"placeholder\": \"لطفا Status را انتخاب کنید\",\n" +
                                                    "      \"order\": 5,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "خطا در تولید Schema",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/form-schema")
    public ResponseEntity<FormSchemaDto> getFormSchema() {
        if (formSchemaService == null) {
            throw new IllegalStateException("FormSchemaService در دسترس نیست");
        }
        Class<T> entityClass = getEntityClass();
        FormSchemaDto schema = formSchemaService.getFormSchema(entityClass);
        return ResponseEntity.ok(schema);
    }

    @Operation(
            summary = "اتوکامپلیت بر اساس جستجو",
            description = "برای استفاده در جستجوی سریع؛ بیش از ۳۰ نتیجه بازگردانده نمی‌شود. در فیلدهای قابل جستجو (code, name, title و ...) جستجو می‌کند.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست کوتاه نتایج برگردانده شد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/autocomplete")
    public ResponseEntity<List<T>> autocomplete(
            @RequestParam(name = "query", required = false, defaultValue = "") String query,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page) {
        int max = Math.min(Math.max(limit == null ? 10 : limit, 1), 30);
        int safePage = Math.max(page == null ? 0 : page, 0);
        
        Pageable pageable = PageRequest.of(safePage, max);
        
        if (query == null || query.isBlank()) {
            // اگر query خالی است، لیست اول را برگردان
            Page<T> result = repository.findAll(pageable);
            return ResponseEntity.ok(result.getContent());
        }
        
        // جستجو در فیلدهای قابل جستجو با OR
        List<String> searchFields = getAutocompleteSearchFields();
        if (searchFields.isEmpty()) {
            // اگر فیلدی مشخص نشده، لیست اول را برگردان
            Page<T> result = repository.findAll(pageable);
            return ResponseEntity.ok(result.getContent());
        }
        
        String searchPattern = "%" + query + "%";
        Specification<T> spec = null;
        
        // ساخت OR specification برای جستجو در چند فیلد
        for (String field : searchFields) {
            FilterRequest filter = new FilterRequest(field, "LIKE", searchPattern);
            Specification<T> fieldSpec = new BaseSpecification<>(List.of(filter));
            if (spec == null) {
                spec = fieldSpec;
            } else {
                spec = spec.or(fieldSpec);
            }
        }
        
        Page<T> result = repository.findAll(spec, pageable);
        return ResponseEntity.ok(result.getContent());
    }
    
    /**
     * دریافت لیست فیلدهای قابل جستجو برای autocomplete
     * کلاس‌های فرزند می‌توانند این متد را override کنند تا فیلدهای خاص خود را اضافه کنند
     * 
     * @return لیست نام فیلدها
     */
    protected List<String> getAutocompleteSearchFields() {
        // فیلدهای پیش‌فرض برای جستجو
        return List.of("code", "name", "title");
    }
}

