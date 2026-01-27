package ir.tax.trustedtaxpayers.externalService.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.externalService.config.TaxpayerSystemConfigProperties;
import ir.tax.trustedtaxpayers.externalService.integration.TaxpayerSystemClient;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/external")
@Tag(name = "External SDK Integration", description = "سرویس‌های خارجی مطابق راهنمای SDK")
public class ExternalIntegrationController {
    private static final Logger logger = LoggerFactory.getLogger(ExternalIntegrationController.class);

    private final TaxpayerSystemConfigProperties config;
    private final TaxpayerSystemClient taxpayerSystemClient;
    private final ElectronicInvoiceHeaderRepository invoiceHeaderRepository;
    private final ElectronicInvoiceSystemRepository systemRepository;
    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final TaxpayerRepository taxpayerRepository;

    public ExternalIntegrationController(TaxpayerSystemConfigProperties config,
                                         TaxpayerSystemClient taxpayerSystemClient,
                                         ElectronicInvoiceHeaderRepository invoiceHeaderRepository,
                                         ElectronicInvoiceSystemRepository systemRepository, ElectronicInvoiceHeaderRepository headerRepository,
                                         TaxpayerRepository taxpayerRepository) {
        this.config = config;
        this.taxpayerSystemClient = taxpayerSystemClient;
        this.invoiceHeaderRepository = invoiceHeaderRepository;
        this.systemRepository = systemRepository;
        this.headerRepository = headerRepository;
        this.taxpayerRepository = taxpayerRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/server-info")
    @Operation(summary = "دریافت اطلاعات سرور",
            description = "اطلاعات پایه جهت راه‌اندازی کلید عمومی سازمان و اتصال به سامانه")
    public ResponseEntity<Map<String, Object>> getServerInformation() {
        Map<String, Object> body = new HashMap<>();
        body.put("baseUrl", config.getBaseUrl());
        body.put("apiPath", config.getApiPath());
        Map<String, String> orgKey = new HashMap<>();
        orgKey.put("key", config.getOrgPublicKey());
        orgKey.put("id", config.getOrgKeyId());
        body.put("orgPublicKey", orgKey);
        body.put("clientId", config.getClientId());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/invoices/{headerId}/submit")
    @Operation(summary = "ارسال صورتحساب",
            description = "ارسال یک صورتحساب به سامانه مودیان از طریق کلاینت خارجی")
    public ResponseEntity<?> submitInvoice(@PathVariable UUID headerId) {
        ElectronicInvoiceHeaderEntity header = invoiceHeaderRepository.findById(headerId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice header not found: " + headerId));

        TaxpayerSystemClient.InvoiceSubmissionResponse response = taxpayerSystemClient.submitInvoice(header);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/token")
    @Operation(summary = "دریافت توکن دسترسی",
            description = "تولید توکن موقت برای یکپارچه‌سازی. در محیط واقعی با سامانه احراز هویت جایگزین می‌شود.")
    public ResponseEntity<Map<String, Object>> requestToken(@RequestParam(required = false) String clientId) {
        String effectiveClientId = clientId != null ? clientId : config.getClientId();
        Map<String, Object> token = new HashMap<>();
        token.put("clientId", effectiveClientId);
        token.put("token", java.util.UUID.randomUUID().toString());
        token.put("expiresIn", 3600);
        return ResponseEntity.ok(token);
    }

    public static class UidInquiryRequest {
        public String uid;
        public String fiscalId;
    }

    /*@PostMapping("/inquiry/uid")
    @Operation(summary = "استعلام نتیجه درخواست با UID",
            description = "وضعیت پردازش بر اساس شناسه یکتای سمت کلاینت")
    public ResponseEntity<Map<String, Object>> inquiryByUid(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = UidInquiryRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "نمونه درخواست",
                                    value = "{\n  \"uid\": \"550e8400-e29b-41d4-a716-446655440000\",\n  \"fiscalId\": \"AB12CD34EF56GH78IJ90KL\"\n}"
                            )
                    )
            )
            @RequestBody UidInquiryRequest request) {
        var sys = systemRepository.findByUniqueId(java.util.UUID.fromString(request.uid))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "UID not found: " + request.uid));
        Map<String, Object> body = new HashMap<>();
        body.put("uid", request.uid);
        body.put("status", sys.getSts());
        body.put("rejectionCode", sys.getRejcode());
        body.put("rejectionDescription", sys.getRejdesc());
        body.put("taxId", sys.getHeader() != null ? sys.getHeader().getTaxId() : null);
        return ResponseEntity.ok(body);
    }*/

    @GetMapping("/inquiry/time-range")
    @Operation(summary = "استعلام نتایج بر اساس بازه زمانی",
            description = "لیست تغییر وضعیت‌ها در بازه زمانی مشخص (UTC)")
    public ResponseEntity<java.util.List<Map<String, Object>>> inquiryByTimeRange(
            @RequestParam String from,
            @RequestParam String to) {
        var fromTs = java.time.OffsetDateTime.parse(from);
        var toTs = java.time.OffsetDateTime.parse(to);
        // انتخاب بر اساس createdOn برای سادگی
        var result = systemRepository.findAll().stream()
                .filter(s -> s.getCreatedOn() != null &&
                        (s.getCreatedOn().isEqual(fromTs) || s.getCreatedOn().isAfter(fromTs)) &&
                        (s.getCreatedOn().isEqual(toTs) || s.getCreatedOn().isBefore(toTs)))
                .limit(200)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                   /* m.put("uid", s.getUniqueId() != null ? s.getUniqueId().toString() : null);*/
                    m.put("status", s.getSts());
                    m.put("taxId", s.getHeader() != null ? s.getHeader() : null);
                    m.put("rejectionCode", s.getRejcode());
                    m.put("rejectionDescription", s.getRejdesc());
                    m.put("timestamp", s.getCreatedOn());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/inquiry/reference")
    @Operation(summary = "استعلام نتیجه درخواست با referenceNumber",
            description = "وضعیت پردازش بر اساس referenceNumber تولیدشده سمت سرور")
    public ResponseEntity<Map<String, Object>> inquiryByReferenceNumber(@RequestParam("referenceNumber") String referenceNumber) {
        var sys = systemRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "referenceNumber not found: " + referenceNumber));
        Map<String, Object> body = new HashMap<>();
        body.put("referenceNumber", referenceNumber);
        body.put("status", sys.getSts());
        body.put("rejectionCode", sys.getRejcode());
        body.put("rejectionDescription", sys.getRejdesc());
        body.put("taxId", sys.getHeader() != null ? sys.getHeader().getTaxId() : null);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/inquiry/time-shamsi")
    @Operation(summary = "استعلام نتیجه درخواست بر اساس تاریخ شمسی",
            description = "ورودی به صورت yyyymmdd (شمسی). در حال حاضر به صورت تاریخ میلادی ساده تفسیر می‌شود.")
    public ResponseEntity<java.util.List<Map<String, Object>>> inquiryByShamsiDate(
            @RequestParam("date") String yyyymmdd) {
        // تبدیل واقعی تاریخ شمسی به میلادی
        var date = ir.tax.trustedtaxpayers.application.util.JalaliDateUtils.parseJalaliYyyyMmDd(yyyymmdd);
        var fromTs = date.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        var toTs = date.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        var result = systemRepository.findAll().stream()
                .filter(s -> s.getCreatedOn() != null &&
                        (s.getCreatedOn().isEqual(fromTs) || s.getCreatedOn().isAfter(fromTs)) &&
                        s.getCreatedOn().isBefore(toTs))
                .limit(200)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("uid", s.getId() != null ? s.getId().toString() : null);
                    m.put("status", s.getSts());
                    m.put("taxId", s.getHeader() != null ? s.getHeader().getTaxId() : null);
                    m.put("rejectionCode", s.getRejcode());
                    m.put("rejectionDescription", s.getRejdesc());
                    m.put("timestamp", s.getCreatedOn());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fiscal-info/{clientId}")
    @Operation(summary = "گرفتن اطلاعات حافظه",
            description = "بر اساس شناسه حافظه (clientId) اطلاعات پایه برگردانده می‌شود")
    public ResponseEntity<Map<String, Object>> getFiscalInformation(@PathVariable String clientId) {
        Map<String, Object> body = new HashMap<>();
        body.put("clientId", clientId);
        body.put("baseUrl", config.getBaseUrl());
        body.put("apiPath", config.getApiPath());
        body.put("orgKeyId", config.getOrgKeyId());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/service-stuff")
    @Operation(summary = "لیست کالا/خدمات",
            description = "بر اساس اقلام ثبت‌شده در سیستم، شناسه و شرح کالا/خدمات را باز می‌گرداند")
    public ResponseEntity<java.util.List<Map<String, Object>>> getServiceStuff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // استخراج یکتا از اقلام ثبت‌شده (نمایش ساده)
        var items = new java.util.LinkedHashMap<String, String>();
        // برای سادگی از کل اقلام استفاده می‌شود؛ در صورت نیاز بهینه‌سازی شود
        ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceItemRepository itemRepo = null;
        try {
            itemRepo = (ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceItemRepository)
                    org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext()
                            .getBean(ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceItemRepository.class);
        } catch (Exception e) {
            logger.debug("Could not retrieve ElectronicInvoiceItemRepository from context: {}", e.getMessage());
        }
        if (itemRepo != null) {
            itemRepo.findAll().forEach(it -> {
                if (it.getSstid() != null && !items.containsKey(it.getSstid())) {
                    items.put(it.getSstid(), it.getSstt());
                }
            });
        }
        var list = items.entrySet().stream().skip((long) page * size).limit(size)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sstid", e.getKey());
                    m.put("sstt", e.getValue());
                    return m;
                }).toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/economic-code/{code}")
    @Operation(summary = "استعلام کد اقتصادی",
            description = "دریافت اطلاعات مودی بر اساس کد اقتصادی")
    public ResponseEntity<Map<String, Object>> getEconomicCode(@PathVariable String code) {
        var tp = taxpayerRepository.findByTaxId(code)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Taxpayer not found: " + code));
        Map<String, Object> body = new HashMap<>();
        body.put("taxId", tp.getTaxId());
        body.put("name", tp.getName());
        body.put("id", tp.getId());
        return ResponseEntity.ok(body);
    }

    private ElectronicInvoiceHeaderEntity findHeader(UUID id) {
        Optional<ElectronicInvoiceHeaderEntity> headerOpt = headerRepository.findById(id);
        if (headerOpt.isEmpty()) {
            return headerOpt.get();
        }
        return null;
    }
}

