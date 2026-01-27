package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.rate.RateLimitToggleService;
import ir.tax.trustedtaxpayers.application.service.FormSchemaService;
import ir.tax.trustedtaxpayers.application.service.SettingService;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CircuitBreakerConfigDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.RateLimitConfigDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceInfoDto;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * کنترلر مدیریت تنظیمات سیستم برای قرائت، ویرایش و به‌روزرسانی ویژگی‌های پیکربندی.
 * <p>
 * Settings controller exposing CRUD, rate-limit, circuit-breaker, and service toggle endpoints.
 */
@RestController
@RequestMapping("/api/settings")
@Tag(name = "Settings Management", description = "مدیریت تنظیمات سیستم\n\nراهنمای سریع:\n- Base: /api/settings\n- مقدار کلید: GET /key/{key}/value | PUT /key/{key}/value → {value}\n- RateLimit: GET /rate/{path} | PUT /rate\n- CircuitBreaker: GET /cb/{path} | PUT /cb\n- لیست سرویس‌ها: GET /services\n- Snapshot سرویس: GET /service/{classFqn} | PUT /service\n- Form Schema: GET /form-schema\n")
public class SettingController {
    
    private final SettingService settingService;
    private final RateLimitToggleService rateToggleService;
    private final FormSchemaService formSchemaService;
    private final ApplicationContext applicationContext;
    
    public SettingController(SettingService settingService, RateLimitToggleService rateToggleService, FormSchemaService formSchemaService, ApplicationContext applicationContext) {
        this.settingService = settingService;
        this.rateToggleService = rateToggleService;
        this.formSchemaService = formSchemaService;
        this.applicationContext = applicationContext;
    }

    @Operation(summary = "دریافت تنظیمات بر اساس key")
    @GetMapping("/key/{key}")
    public ResponseEntity<SettingEntity> getByKey(@PathVariable String key) {
        Optional<SettingEntity> setting = settingService.findByKey(key);
        if (setting.isPresent()) {
            return ResponseEntity.ok(setting.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "دریافت value تنظیمات")
    @GetMapping("/key/{key}/value")
    public ResponseEntity<Map<String, String>> getValue(@PathVariable String key) {
        return settingService.getValue(key)
                .map(value -> ResponseEntity.ok(Map.of("key", key, "value", value)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "به‌روزرسانی value تنظیمات")
    @PutMapping("/key/{key}/value")
    public ResponseEntity<SettingEntity> updateValue(
            @PathVariable String key,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = java.util.Map.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"value\": \"ON\"\n}"
                            )
                    )
            )
            @RequestBody Map<String, String> request) {
        String value = request.get("value");
        SettingGroupType groupType = toGroupType(request.get("groupType"));
        SettingEntity updated = groupType != null
                ? settingService.saveOrUpdate(key, value, groupType)
                : settingService.updateValue(key, value);
        rateToggleService.invalidateCache();
        return ResponseEntity.ok(updated);
    }

    // ===== Rate-limit specific endpoints =====

    @Operation(summary = "دریافت تنظیمات RateLimit برای یک مسیر")
    @GetMapping("/rate/{path}")
    public ResponseEntity<Map<String, Object>> getRateForPath(@PathVariable String path) {
        String base = "RATE_LIMIT_" + normalizePath(path) + "/";
        Map<String, Object> body = Map.of(
                "perSecond", settingService.getValue(base + "perSecond").orElse("0"),
                "perMinute", settingService.getValue(base + "perMinute").orElse("0"),
                "perHour", settingService.getValue(base + "perHour").orElse("0"),
                "byIp", settingService.getValue(base + "byIp").orElse("true"),
                "whitelist", settingService.getValue(base + "whitelist").orElse(""),
                "blacklist", settingService.getValue(base + "blacklist").orElse("")
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ثبت/به‌روزرسانی تنظیمات RateLimit برای یک مسیر")
    @PutMapping("/rate")
    public ResponseEntity<Map<String, Object>> upsertRateForPath(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = java.util.Map.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"path\": \"/api/invoices\",\n  \"perSecond\": 5,\n  \"perMinute\": 120,\n  \"perHour\": 1000,\n  \"byIp\": true,\n  \"whitelist\": \"127.0.0.1\",\n  \"blacklist\": \"\"\n}"
                            )
                    )
            )
            @RequestBody Map<String, Object> req) {
        String rawPath = String.valueOf(req.getOrDefault("path", ""));
        String path = normalizePath(rawPath);
        if (path.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }
        String base = "RATE_LIMIT_" + path + "/";
        upsertIfPresent(base + "perSecond", req.get("perSecond"));
        upsertIfPresent(base + "perMinute", req.get("perMinute"));
        upsertIfPresent(base + "perHour", req.get("perHour"));
        upsertIfPresent(base + "byIp", req.get("byIp"));
        upsertIfPresent(base + "whitelist", req.get("whitelist"));
        upsertIfPresent(base + "blacklist", req.get("blacklist"));

        rateToggleService.invalidateCache();
        return getRateForPath(path);
    }

    private void upsertIfPresent(String key, Object val) {
        if (val == null) return;
        String value = String.valueOf(val);
        settingService.saveOrUpdate(key, value);
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim();
        if (p.isEmpty()) {
            return "";
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p;
    }

    private SettingGroupType toGroupType(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return SettingGroupType.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ===== Circuit Breaker endpoints =====
    @Operation(summary = "دریافت تنظیمات Circuit Breaker برای یک مسیر")
    @GetMapping("/cb/{path}")
    public ResponseEntity<Map<String, Object>> getCbForPath(@PathVariable String path) {
        String base = "CB_" + normalizePath(path) + "/";
        Map<String, Object> body = Map.of(
                "failureThreshold", settingService.getValue(base + "failureThreshold").orElse("5"),
                "failureRate", settingService.getValue(base + "failureRate").orElse("0.5"),
                "windowSeconds", settingService.getValue(base + "windowSeconds").orElse("60"),
                "openMillis", settingService.getValue(base + "openMillis").orElse("60000"),
                "halfOpenMaxCalls", settingService.getValue(base + "halfOpenMaxCalls").orElse("1"),
                "enabled", settingService.getValue(base + "enabled").orElse("true")
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ثبت/به‌روزرسانی تنظیمات Circuit Breaker برای یک مسیر")
    @PutMapping("/cb")
    public ResponseEntity<Map<String, Object>> upsertCbForPath(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = java.util.Map.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"path\": \"/api/invoices\",\n  \"failureThreshold\": 5,\n  \"failureRate\": 0.5,\n  \"windowSeconds\": 60,\n  \"openMillis\": 60000,\n  \"halfOpenMaxCalls\": 1,\n  \"enabled\": true\n}"
                            )
                    )
            )
            @RequestBody Map<String, Object> req) {
        String rawPath = String.valueOf(req.getOrDefault("path", ""));
        String path = normalizePath(rawPath);
        if (path.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "path is required"));
        }
        String base = "CB_" + path + "/";
        upsertIfPresent(base + "failureThreshold", req.get("failureThreshold"));
        upsertIfPresent(base + "failureRate", req.get("failureRate"));
        upsertIfPresent(base + "windowSeconds", req.get("windowSeconds"));
        upsertIfPresent(base + "openMillis", req.get("openMillis"));
        upsertIfPresent(base + "halfOpenMaxCalls", req.get("halfOpenMaxCalls"));
        upsertIfPresent(base + "enabled", req.get("enabled"));
        rateToggleService.invalidateCache();
        return getCbForPath(path);
    }

    // ===== Service discovery and listing =====
    @Operation(
            summary = "لیست تمام سرویس‌های REST موجود",
            description = "این endpoint لیست کامل تمام Controller های REST را برمی‌گرداند که شامل:\n" +
                    "- اطلاعات پایه سرویس (نام کلاس، مسیر)\n" +
                    "- وضعیت فعال/غیرفعال\n" +
                    "- تنظیمات RateLimit (اگر وجود دارد)\n" +
                    "- تنظیمات CircuitBreaker (اگر وجود دارد)\n" +
                    "- تنظیمات Cache (اگر وجود دارد)"
    )
    @GetMapping("/services")
    public ResponseEntity<List<ServiceInfoDto>> getAllServices() {
        // یافتن تمام Controller های REST از ApplicationContext
        Map<String, Object> restControllers = applicationContext.getBeansWithAnnotation(RestController.class);
        Set<String> serviceClassNames = new HashSet<>();
        
        for (Object controller : restControllers.values()) {
            String classFqn = controller.getClass().getName();
            // فقط Controller های داخل package interfaces.rest را در نظر بگیریم
            if (classFqn.startsWith("ir.tax.trustedtaxpayers.interfaces.rest")) {
                serviceClassNames.add(classFqn);
            }
        }
        
        // همچنین سرویس‌هایی که در دیتابیس ثبت شده‌اند اما ممکن است Controller نباشند
        List<SettingEntity> serviceSettings = settingService.findByGroupType(SettingGroupType.SERVICE);
        for (SettingEntity setting : serviceSettings) {
            String key = setting.getKey();
            if (key.startsWith("SERVICE_") && key.contains("/")) {
                String classFqn = key.substring(8, key.indexOf("/"));
                if (classFqn.contains(".")) { // اطمینان از اینکه FQN معتبر است
                    serviceClassNames.add(classFqn);
                }
            }
        }
        
        // تبدیل به ServiceInfoDto
        List<ServiceInfoDto> services = serviceClassNames.stream()
                .map(this::buildServiceInfo)
                .sorted((a, b) -> a.getClassFqn().compareTo(b.getClassFqn()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(services);
    }
    
    private ServiceInfoDto buildServiceInfo(String classFqn) {
        String svcPrefix = "SERVICE_" + classFqn + "/";
        String rateSvcPrefix = "RATE_LIMIT_SERVICE_" + classFqn + "/";
        String cbSvcPrefix = "CB_SERVICE_" + classFqn + "/";
        String cacheSvcPrefix = "CACHE_SERVICE_" + classFqn + "/";
        
        // دریافت اطلاعات پایه سرویس
        String servicePath = settingService.getValue(svcPrefix + "path").orElse("");
        Boolean serviceEnabled = parseBoolean(settingService.getValue(svcPrefix + "enabled").orElse("true"));
        Boolean rateLimitEnabled = parseBoolean(settingService.getValue(rateSvcPrefix + "enabled").orElse("true"));
        Boolean circuitBreakerEnabled = parseBoolean(settingService.getValue(cbSvcPrefix + "enabled").orElse("true"));
        Boolean cacheEnabled = parseBoolean(settingService.getValue(cacheSvcPrefix + "enabled").orElse("true"));
        
        // استخراج نام ساده کلاس
        String simpleClassName = classFqn.contains(".") 
                ? classFqn.substring(classFqn.lastIndexOf(".") + 1)
                : classFqn;
        
        // ساخت RateLimitConfig (اگر تنظیمات path وجود دارد)
        RateLimitConfigDto rateLimitConfig = null;
        if (!servicePath.isEmpty()) {
            String ratePathPrefix = "RATE_LIMIT_" + normalizePath(servicePath) + "/";
            Optional<String> perSecond = settingService.getValue(ratePathPrefix + "perSecond");
            Optional<String> perMinute = settingService.getValue(ratePathPrefix + "perMinute");
            Optional<String> perHour = settingService.getValue(ratePathPrefix + "perHour");
            
            if (perSecond.isPresent() || perMinute.isPresent() || perHour.isPresent()) {
                rateLimitConfig = RateLimitConfigDto.builder()
                        .perSecond(parseInt(perSecond.orElse("0")))
                        .perMinute(parseInt(perMinute.orElse("0")))
                        .perHour(parseInt(perHour.orElse("0")))
                        .byIp(parseBoolean(settingService.getValue(ratePathPrefix + "byIp").orElse("true")))
                        .whitelist(settingService.getValue(ratePathPrefix + "whitelist").orElse(""))
                        .blacklist(settingService.getValue(ratePathPrefix + "blacklist").orElse(""))
                        .build();
            }
        }
        
        // ساخت CircuitBreakerConfig (اگر تنظیمات path وجود دارد)
        CircuitBreakerConfigDto circuitBreakerConfig = null;
        if (!servicePath.isEmpty()) {
            String cbPathPrefix = "CB_" + normalizePath(servicePath) + "/";
            Optional<String> failureThreshold = settingService.getValue(cbPathPrefix + "failureThreshold");
            Optional<String> failureRate = settingService.getValue(cbPathPrefix + "failureRate");
            
            if (failureThreshold.isPresent() || failureRate.isPresent()) {
                circuitBreakerConfig = CircuitBreakerConfigDto.builder()
                        .failureThreshold(parseInt(settingService.getValue(cbPathPrefix + "failureThreshold").orElse("5")))
                        .failureRate(parseDouble(settingService.getValue(cbPathPrefix + "failureRate").orElse("0.5")))
                        .windowSeconds(parseInt(settingService.getValue(cbPathPrefix + "windowSeconds").orElse("60")))
                        .openMillis(parseLong(settingService.getValue(cbPathPrefix + "openMillis").orElse("60000")))
                        .halfOpenMaxCalls(parseInt(settingService.getValue(cbPathPrefix + "halfOpenMaxCalls").orElse("1")))
                        .enabled(parseBoolean(settingService.getValue(cbPathPrefix + "enabled").orElse("true")))
                        .build();
            }
        }
        
        return ServiceInfoDto.builder()
                .classFqn(classFqn)
                .simpleClassName(simpleClassName)
                .servicePath(servicePath)
                .serviceEnabled(serviceEnabled)
                .rateLimitEnabled(rateLimitEnabled)
                .circuitBreakerEnabled(circuitBreakerEnabled)
                .cacheEnabled(cacheEnabled)
                .rateLimitConfig(rateLimitConfig)
                .circuitBreakerConfig(circuitBreakerConfig)
                .build();
    }
    
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return !("false".equals(normalized) || "0".equals(normalized) || "off".equals(normalized) || "disabled".equals(normalized));
    }
    
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // ===== Service snapshot and toggles =====
    @Operation(summary = "نمایش وضعیت و تنظیمات سرویس/کلاس")
    @GetMapping("/service/{classFqn}")
    public ResponseEntity<Map<String, Object>> getServiceSnapshot(@PathVariable String classFqn) {
        String svc = "SERVICE_" + classFqn + "/";
        String rateSvc = "RATE_LIMIT_SERVICE_" + classFqn + "/";
        String cbSvc = "CB_SERVICE_" + classFqn + "/";
        String cacheSvc = "CACHE_SERVICE_" + classFqn + "/";
        Map<String, Object> body = Map.of(
                "serviceEnabled", settingService.getValue(svc + "enabled").orElse("true"),
                "servicePath", settingService.getValue(svc + "path").orElse(""),
                "rateEnabled", settingService.getValue(rateSvc + "enabled").orElse("true"),
                "cbEnabled", settingService.getValue(cbSvc + "enabled").orElse("true"),
                "cacheEnabled", settingService.getValue(cacheSvc + "enabled").orElse("true")
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "به‌روزرسانی تنظیمات سرویس/کلاس")
    @PutMapping("/service")
    public ResponseEntity<Map<String, Object>> upsertService(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = java.util.Map.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"classFqn\": \"ir.tax.trustedtaxpayers.application.service.InvoiceByTemplateService\",\n  \"serviceEnabled\": true,\n  \"servicePath\": \"/api/invoices/by-template\",\n  \"rateEnabled\": true,\n  \"cbEnabled\": true,\n  \"cacheEnabled\": true\n}"
                            )
                    )
            )
            @RequestBody Map<String, Object> req) {
        String classFqn = String.valueOf(req.getOrDefault("classFqn", ""));
        if (classFqn.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "classFqn is required"));
        }
        String svc = "SERVICE_" + classFqn + "/";
        String rateSvc = "RATE_LIMIT_SERVICE_" + classFqn + "/";
        String cbSvc = "CB_SERVICE_" + classFqn + "/";
        String cacheSvc = "CACHE_SERVICE_" + classFqn + "/";
        upsertIfPresent(svc + "enabled", req.get("serviceEnabled"));
        upsertIfPresent(svc + "path", req.get("servicePath"));
        upsertIfPresent(rateSvc + "enabled", req.get("rateEnabled"));
        upsertIfPresent(cbSvc + "enabled", req.get("cbEnabled"));
        upsertIfPresent(cacheSvc + "enabled", req.get("cacheEnabled"));
        rateToggleService.invalidateCache();
        return getServiceSnapshot(classFqn);
    }

    @Operation(
            summary = "دریافت Schema فرم برای Angular Form Builder",
            description = "دریافت JSON Schema برای استفاده در Angular Form Builder. این endpoint لیستی از فیلدهای فرم SettingEntity را با مشخصات کامل برمی‌گرداند.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FormSchemaDto.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"entityName\": \"SettingEntity\",\n" +
                                                    "  \"title\": \"فرم SettingEntity\",\n" +
                                                    "  \"description\": \"فرم ایجاد و ویرایش SettingEntity\",\n" +
                                                    "  \"fields\": [\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"path\",\n" +
                                                    "      \"type\": \"input\",\n" +
                                                    "      \"label\": \"Path\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"maxLength\": 500,\n" +
                                                    "      \"placeholder\": \"لطفا Path را وارد کنید\",\n" +
                                                    "      \"order\": 0,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"url\",\n" +
                                                    "      \"type\": \"input\",\n" +
                                                    "      \"label\": \"Url\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"maxLength\": 1000,\n" +
                                                    "      \"placeholder\": \"لطفا Url را وارد کنید\",\n" +
                                                    "      \"order\": 1,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"key\",\n" +
                                                    "      \"type\": \"input\",\n" +
                                                    "      \"label\": \"Key\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"maxLength\": 100,\n" +
                                                    "      \"placeholder\": \"لطفا Key را وارد کنید\",\n" +
                                                    "      \"order\": 2,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"value\",\n" +
                                                    "      \"type\": \"input\",\n" +
                                                    "      \"label\": \"Value\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"maxLength\": 4000,\n" +
                                                    "      \"placeholder\": \"لطفا Value را وارد کنید\",\n" +
                                                    "      \"order\": 3,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"pass\",\n" +
                                                    "      \"type\": \"input\",\n" +
                                                    "      \"label\": \"Pass\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"maxLength\": 500,\n" +
                                                    "      \"placeholder\": \"لطفا Pass را وارد کنید\",\n" +
                                                    "      \"order\": 4,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"isActive\",\n" +
                                                    "      \"type\": \"checkbox\",\n" +
                                                    "      \"label\": \"Is Active\",\n" +
                                                    "      \"required\": false,\n" +
                                                    "      \"defaultValue\": false,\n" +
                                                    "      \"placeholder\": \"لطفا Is Active را وارد کنید\",\n" +
                                                    "      \"order\": 5,\n" +
                                                    "      \"disabled\": false\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "خطا در تولید Schema",
                            content = @io.swagger.v3.oas.annotations.media.Content(
                                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse.class)
                            )
                    )
            }
    )
    @GetMapping("/form-schema")
    public ResponseEntity<FormSchemaDto> getFormSchema() {
        FormSchemaDto schema = formSchemaService.getFormSchema(SettingEntity.class);
        return ResponseEntity.ok(schema);
    }

    @Operation(summary = "دریافت همه تنظیمات")
    @GetMapping
    public ResponseEntity<List<SettingEntity>> findAll() {
        return ResponseEntity.ok(settingService.findAll());
    }

    @Operation(summary = "دریافت تنظیمات بر اساس گروه")
    @GetMapping("/group/{groupType}")
    public ResponseEntity<List<SettingEntity>> findByGroup(@PathVariable String groupType) {
        SettingGroupType type = toGroupType(groupType);
        if (type == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(settingService.findByGroupType(type));
    }

    @Operation(summary = "به‌روزرسانی گروهی تنظیمات و گرم‌کردن کش")
    @PutMapping
    public ResponseEntity<List<SettingEntity>> updateAll(@RequestBody List<SettingEntity> settings) {
        List<SettingEntity> saved = settingService.updateAll(settings);
        rateToggleService.invalidateCache();
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "به‌روزرسانی گروهی به صورت key/value")
    @PutMapping("/kv")
    public ResponseEntity<List<SettingEntity>> updateAllKeyValues(@RequestBody List<java.util.Map<String, String>> items) {
        List<SettingEntity> settings = items.stream()
                .filter(m -> m.get("key") != null)
                .map(m -> SettingEntity.builder()
                        .key(m.get("key"))
                        .value(m.get("value"))
                        .groupType(toGroupType(m.get("groupType")))
                        .build())
                .collect(Collectors.toList());

        List<SettingEntity> saved = settingService.updateAll(settings);
        rateToggleService.invalidateCache();
        return ResponseEntity.ok(saved);
    }

}


