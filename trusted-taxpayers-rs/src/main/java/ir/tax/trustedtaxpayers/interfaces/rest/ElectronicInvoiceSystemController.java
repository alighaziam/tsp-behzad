package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت داده‌های سیستمی صورتحساب برای ردیابی وضعیت ارسال و متادیتای وابسته.
 * <p>
 * REST controller providing access to external system statuses, summaries, and pagination views.
 */
@RestController
@RequestMapping("/api/electronic-invoice-systems")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Electronic Invoice System", description = "مدیریت اطلاعات سیستمی صورتحساب الکترونیکی\n\nراهنمای سریع:\n- Base: /api/electronic-invoice-systems\n- بر اساس Header: /by-header/{headerId}\n- بر اساس وضعیت: /by-status/{sts}?page=0&size=10\n- accepted/pending/failed: /status/accepted|pending|failed?page=0&size=20\n- همه: /all | جزئیات: /details/{id}\n")
public class ElectronicInvoiceSystemController extends BaseController<ElectronicInvoiceSystemEntity, UUID> {
    private final ElectronicInvoiceSystemService systemService;
    private final ElectronicInvoiceSystemRepository systemRepository;
    private final ElectronicInvoiceMapper mapper;

    public ElectronicInvoiceSystemController(
            ElectronicInvoiceSystemService systemService,
            ElectronicInvoiceSystemRepository systemRepository,
            ElectronicInvoiceMapper mapper) {
        super(systemService, systemRepository);
        this.systemService = systemService;
        this.systemRepository = systemRepository;
        this.mapper = mapper;
    }

    @GetMapping("/by-header/{headerId}")
    public ResponseEntity<ElectronicInvoiceDtos.ElectronicInvoiceSystemDto> findByHeaderId(@PathVariable UUID headerId) {
        var system = systemService.findByHeaderId(headerId)
                .orElseThrow(() -> new ir.tax.trustedtaxpayers.application.exception.ResourceNotFoundException("System info for header " + headerId + " not found"));
        return ResponseEntity.ok(mapper.toSystemDto(system));
    }

    @GetMapping("/by-status/{sts}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceSystemDto>> findByStatus(
            @PathVariable String sts,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ElectronicInvoiceSystemEntity> result = systemService.findByStatus(sts, pageable);
        return ResponseEntity.ok(result.getContent().stream().map(mapper::toSystemDto).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceSystemDto>> getAll() {
        List<ElectronicInvoiceSystemEntity> entities = systemService.findAll();
        return ResponseEntity.ok(entities.stream().map(mapper::toSystemDto).toList());
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<ElectronicInvoiceDtos.ElectronicInvoiceSystemDto> getById(@PathVariable UUID id) {
        ElectronicInvoiceSystemEntity entity = systemService.findById(id)
                .orElseThrow(() -> new ir.tax.trustedtaxpayers.application.exception.ResourceNotFoundException("رکورد با شناسه " + id + " یافت نشد"));
        return ResponseEntity.ok(mapper.toSystemDto(entity));
    }
    
    /**
     * دریافت صورتحساب‌های ارسال‌شده موفق (ACCEPTED)
     */
    @GetMapping("/status/accepted")
    @Operation(summary = "دریافت صورتحساب‌های ارسال شده موفق", 
               description = "لیست تمام صورتحساب‌هایی که با موفقیت به سامانه مودیان ارسال شده‌اند")
    @Parameter(name = "page", description = "شماره صفحه (از 0)", example = "0")
    @Parameter(name = "size", description = "تعداد ردیف در هر صفحه", example = "20")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto>> getAcceptedInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ElectronicInvoiceSystemEntity> result = systemService.findByStatusWithHeader("ACCEPTED", pageable);
        Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto> dtos = result.map(mapper::toSystemWithHeaderDto);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        int totalPages = dtos.getTotalPages();
        java.util.List<String> links = new java.util.ArrayList<>();
        String base = request.getRequestURI();
        java.util.function.BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + base + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";
        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (page > 0) links.add(String.format(linkOf.apply(page - 1, size), "prev"));
            if (page + 1 < totalPages) links.add(String.format(linkOf.apply(page + 1, size), "next"));
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }
        if (!links.isEmpty()) headers.add(org.springframework.http.HttpHeaders.LINK, String.join(", ", links));
        headers.add("X-Total-Count", String.valueOf(dtos.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(dtos);
    }
    
    /**
     * دریافت صورتحساب‌های در انتظار ارسال (DRAFT, SUBMITTED, PENDING)
     */
    @GetMapping("/status/pending")
    @Operation(summary = "دریافت صورتحساب‌های در انتظار ارسال", 
               description = "لیست تمام صورتحساب‌هایی که هنوز ارسال نشده یا در حال پردازش هستند")
    @Parameter(name = "page", description = "شماره صفحه (از 0)", example = "0")
    @Parameter(name = "size", description = "تعداد ردیف در هر صفحه", example = "20")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto>> getPendingInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        
        // دریافت همه وضعیت‌های در انتظار با یک query
        List<String> pendingStatuses = Arrays.asList("DRAFT", "SUBMITTED", "PENDING");
        Page<ElectronicInvoiceSystemEntity> result = systemService.findByStatusesWithHeader(pendingStatuses, pageable);
        Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto> dtos = result.map(mapper::toSystemWithHeaderDto);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        int totalPages = dtos.getTotalPages();
        java.util.List<String> links = new java.util.ArrayList<>();
        String base = request.getRequestURI();
        java.util.function.BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + base + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";
        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (page > 0) links.add(String.format(linkOf.apply(page - 1, size), "prev"));
            if (page + 1 < totalPages) links.add(String.format(linkOf.apply(page + 1, size), "next"));
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }
        if (!links.isEmpty()) headers.add(org.springframework.http.HttpHeaders.LINK, String.join(", ", links));
        headers.add("X-Total-Count", String.valueOf(dtos.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(dtos);
    }
    
    /**
     * دریافت صورتحساب‌های ناموفق (REJECTED, FAILED)
     */
    @GetMapping("/status/failed")
    @Operation(summary = "دریافت صورتحساب‌های ناموفق", 
               description = "لیست تمام صورتحساب‌هایی که ارسال آنها ناموفق بوده است")
    @Parameter(name = "page", description = "شماره صفحه (از 0)", example = "0")
    @Parameter(name = "size", description = "تعداد ردیف در هر صفحه", example = "20")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto>> getFailedInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        Pageable pageable = PageRequest.of(page, size);
        
        // دریافت REJECTED و FAILED با یک query
        List<String> failedStatuses = Arrays.asList("REJECTED", "FAILED");
        Page<ElectronicInvoiceSystemEntity> result = systemService.findByStatusesWithHeader(failedStatuses, pageable);
        Page<ElectronicInvoiceDtos.ElectronicInvoiceSystemWithHeaderDto> dtos = result.map(mapper::toSystemWithHeaderDto);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        int totalPages = dtos.getTotalPages();
        java.util.List<String> links = new java.util.ArrayList<>();
        String base = request.getRequestURI();
        java.util.function.BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + base + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";
        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (page > 0) links.add(String.format(linkOf.apply(page - 1, size), "prev"));
            if (page + 1 < totalPages) links.add(String.format(linkOf.apply(page + 1, size), "next"));
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }
        if (!links.isEmpty()) headers.add(org.springframework.http.HttpHeaders.LINK, String.join(", ", links));
        headers.add("X-Total-Count", String.valueOf(dtos.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(dtos);
    }
}



