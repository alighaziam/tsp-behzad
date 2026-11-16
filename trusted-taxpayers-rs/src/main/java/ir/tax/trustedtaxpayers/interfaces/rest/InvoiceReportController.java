package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import ir.tax.trustedtaxpayers.application.dto.InvoiceReportItemDto;
import ir.tax.trustedtaxpayers.application.dto.InvoiceReportResponseDto;
import ir.tax.trustedtaxpayers.application.dto.InvoiceStatusSummaryDto;
import ir.tax.trustedtaxpayers.application.dto.ReportTaxpayerDto;
import ir.tax.trustedtaxpayers.application.service.*;
import ir.tax.trustedtaxpayers.application.service.InvoiceReportService.InvoiceReportFilter;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping({"/api/reports", "/api/v1/reports"})
@RequiredArgsConstructor
public class InvoiceReportController {

    private final InvoiceReportService reportService;
    private final UserRoleService userRoleService;
    private final CurrentUserTaxpayerResolverService taxpayerResolverService;
    private final TaxpayerRepository taxpayerRepository;
    private final InvoiceExportService exportService;
    private final ElectronicInvoiceSystemService systemService;
    private final ElectronicInvoiceHeaderService headerService;

    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_LIMIT = 10_000;

    @GetMapping("/admin/invoices")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<InvoiceReportResponseDto> adminInvoices(
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId,
            @RequestParam(name = "taxpayerNo", required = false) String taxpayerNo,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = buildPageable(page, size);
        InvoiceReportFilter filter = InvoiceReportFilter.builder()
                .taxpayerIds(taxpayerId != null ? Set.of(taxpayerId) : Collections.emptySet())
                .taxpayerNo(normalizeTaxpayerNo(taxpayerNo))
                .statuses(statuses)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        InvoiceReportResponseDto response = reportService.fetchReport(filter, pageable, List.of());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> adminExport(
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId,
            @RequestParam(name = "taxpayerNo", required = false) String taxpayerNo,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {

        InvoiceReportFilter filter = InvoiceReportFilter.builder()
                .taxpayerIds(taxpayerId != null ? Set.of(taxpayerId) : Collections.emptySet())
                .taxpayerNo(normalizeTaxpayerNo(taxpayerNo))
                .statuses(statuses)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        String csv = reportService.exportCsv(filter, EXPORT_LIMIT);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-invoices.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    @GetMapping("/me/invoices")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    public ResponseEntity<InvoiceReportResponseDto> myInvoices(
            HttpServletRequest request,
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Set<UUID> accessibleTaxpayers = taxpayerResolverService.findCurrentUserTaxpayerIds();
        if (accessibleTaxpayers.isEmpty()) {
            return ResponseEntity.ok(emptyResponse(page, size, List.of()));
        }

        Set<UUID> appliedTaxpayers = accessibleTaxpayers;
        if (taxpayerId != null) {
            if (!accessibleTaxpayers.contains(taxpayerId)) {
                throw new ResponseStatusException(FORBIDDEN, "کاربر به این مودی دسترسی ندارد.");
            }
            appliedTaxpayers = Set.of(taxpayerId);
        }

        List<ReportTaxpayerDto> taxpayerDtos = buildTaxpayerDtos(accessibleTaxpayers);
        Pageable pageable = buildPageable(page, size);
        
        // Filter both seller and buyer sides against the exact taxpayer assignments of this account.
        InvoiceReportFilter.InvoiceReportFilterBuilder filterBuilder = InvoiceReportFilter.builder()
                .taxpayerIds(appliedTaxpayers)
                .statuses(statuses)
                .fromDate(fromDate)
                .toDate(toDate);

        InvoiceReportResponseDto response = reportService.fetchReport(filterBuilder.build(), pageable, taxpayerDtos);
        return ResponseEntity.ok(response);
    }

    /*@GetMapping("/me/invoices")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    public ResponseEntity<InvoiceReportResponseDto> myInvoices(
            HttpServletRequest request,
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(name = "page", defaultValue = "0") int pageNumber,
            @RequestParam(name = "size", defaultValue = "20") int pageSize
    ) {

        Set<UUID> accessibleTaxpayers =
                taxpayerResolverService.findCurrentUserTaxpayerIds();

        if (accessibleTaxpayers.isEmpty()) {
            return ResponseEntity.ok(emptyResponse(pageNumber, pageSize, List.of()));
        }

        Set<UUID> appliedTaxpayers = accessibleTaxpayers;
        if (taxpayerId != null) {
            if (!accessibleTaxpayers.contains(taxpayerId)) {
                throw new ResponseStatusException(
                        FORBIDDEN,
                        "کاربر به این مودی دسترسی ندارد."
                );
            }
            appliedTaxpayers = Set.of(taxpayerId);
        }

        List<ReportTaxpayerDto> taxpayerDtos =
                buildTaxpayerDtos(accessibleTaxpayers);

        Pageable pageable = buildPageable(pageNumber, pageSize);

        InvoiceReportFilter.InvoiceReportFilterBuilder filterBuilder =
                InvoiceReportFilter.builder()
                        .statuses(statuses)
                        .fromDate(fromDate)
                        .toDate(toDate);

        if (isTaxpayerUser()) {
            taxpayerResolverService
                    .findCurrentUserPrimaryTaxpayerId()
                    .ifPresent(filterBuilder::sellerTaxpayerId);
        } else {
            filterBuilder.taxpayerIds(appliedTaxpayers);
        }

        Page<ElectronicInvoiceSystemEntity> resultPage =
                systemService.fetchReport(filterBuilder.build(), pageable);

        InvoiceReportResponseDto response =
                mapToResponse(resultPage, taxpayerDtos);

        return ResponseEntity.ok(response);
    }*/



    @GetMapping("/me/invoices/{headerId}/pdf")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    @Operation(summary = "دانلود PDF یک صورتحساب", description = "دانلود PDF یک صورتحساب خاص")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            HttpServletRequest request,
            @PathVariable UUID headerId) {
        
        Set<UUID> accessibleTaxpayers = taxpayerResolverService.findCurrentUserTaxpayerIds();
        
        // Find invoice and check access
        var invoiceOpt = reportService.findInvoiceById(headerId);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var invoice = invoiceOpt.get();
        // Behzad stores taxpayer references as TINS/TINB strings on the invoice header.
        Set<String> assignedTaxIds = taxpayerRepository.findAllById(accessibleTaxpayers).stream()
                .map(TaxpayerEntity::getTaxId)
                .filter(Objects::nonNull)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        String sellerTaxId = invoice.getTins() == null ? "" : invoice.getTins().trim().toUpperCase(Locale.ROOT);
        String buyerTaxId = invoice.getTinb() == null ? "" : invoice.getTinb().trim().toUpperCase(Locale.ROOT);
        if (!assignedTaxIds.contains(sellerTaxId) && !assignedTaxIds.contains(buyerTaxId)) {
            return ResponseEntity.status(FORBIDDEN).build();
        }
        
        // Generate PDF
        byte[] pdfContent = reportService.exportInvoiceToPdf(invoice);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoice_%s.pdf", invoice.getTaxId()));
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @GetMapping("/samples/invoice.pdf")
    @Operation(summary = "دانلود فایل PDF نمونه فاکتور", description = "دانلود فایل PDF نمونه برای تست")
    public ResponseEntity<byte[]> downloadSampleInvoicePdf() {
        byte[] pdfContent = exportService.generateSampleInvoicePdf();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "sample-invoice.pdf");
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @GetMapping("/samples/invoice-pattern2.pdf")
    @Operation(summary = "دانلود فایل PDF نمونه الگوی 2 (فروش ارز)", description = "دانلود فایل PDF نمونه برای الگوی 2 با فیلدهای اختصاصی")
    public ResponseEntity<byte[]> downloadSampleInvoicePdfPattern2() {
        byte[] pdfContent = exportService.generateSampleInvoicePdfPattern2();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "sample-invoice-pattern2.pdf");
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @GetMapping("/samples/invoice.doc")
    @Operation(summary = "دانلود فایل Word نمونه فاکتور", description = "دانلود فایل Word نمونه برای پر کردن و تبدیل به تصویر")
    public ResponseEntity<byte[]> downloadSampleInvoiceWord() {
        byte[] wordContent = exportService.generateSampleInvoiceWord();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/msword"));
        headers.setContentDispositionFormData("attachment", "sample-invoice.doc");
        
        return ResponseEntity.ok().headers(headers).body(wordContent);
    }

    @GetMapping("/me/export")
    @PreAuthorize("hasAnyAuthority('TAXPAYER','ADMIN')")
    public ResponseEntity<String> myExport(
            HttpServletRequest request,
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId,
            @RequestParam(name = "statuses", required = false) List<String> statuses,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate) {

        Set<UUID> accessibleTaxpayers = taxpayerResolverService.findCurrentUserTaxpayerIds();
        if (accessibleTaxpayers.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "هیچ مودی فعالی برای کاربر ثبت نشده است.");
        }
        Set<UUID> appliedTaxpayers = accessibleTaxpayers;
        if (taxpayerId != null) {
            if (!accessibleTaxpayers.contains(taxpayerId)) {
                throw new ResponseStatusException(FORBIDDEN, "کاربر به این مودی دسترسی ندارد.");
            }
            appliedTaxpayers = Set.of(taxpayerId);
        }

        InvoiceReportFilter.InvoiceReportFilterBuilder filterBuilder = InvoiceReportFilter.builder()
                .taxpayerIds(appliedTaxpayers)
                .statuses(statuses)
                .fromDate(fromDate)
                .toDate(toDate);
        
        InvoiceReportFilter filter = filterBuilder.build();

        String csv = reportService.exportCsv(filter, EXPORT_LIMIT);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-invoices.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }

    private Pageable buildPageable(int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // نکته: Sort روی createdOn (از BaseEntity) در سناریوی join+pagination برای Oracle/UUID مشکل‌زا و حتی مبهم است.
        // این endpoint باید روی تاریخ سربرگ (header.createdOn) مرتب شود تا هم پایدار باشد و هم ambiguity نداشته باشد.
        Sort safeSort = Sort.by(Sort.Direction.DESC, "header.createdOn");

        return PageRequest.of(0, 10);
    
    }

    private String normalizeTaxpayerNo(String taxpayerNo) {
        return taxpayerNo != null && !taxpayerNo.isBlank()
                ? taxpayerNo.trim().toUpperCase(Locale.ROOT)
                : null;
    }

    private List<ReportTaxpayerDto> buildTaxpayerDtos(Set<UUID> taxpayerIds) {
        if (taxpayerIds.isEmpty()) {
            return List.of();
        }
        List<TaxpayerEntity> taxpayers = taxpayerRepository.findAllById(taxpayerIds);
        Map<UUID, TaxpayerEntity> taxpayersById = taxpayers.stream()
                .collect(Collectors.toMap(TaxpayerEntity::getId, t -> t));
        return taxpayerIds.stream()
                .map(id -> {
                    TaxpayerEntity taxpayer = taxpayersById.get(id);
                    if (taxpayer == null) {
                        return new ReportTaxpayerDto(id, null, null);
                    }
                    return new ReportTaxpayerDto(id, taxpayer.getTaxId(), taxpayer.getName());
                })
                .sorted((a, b) -> {
                  String nameA = a.name() != null ? a.name() : "";
                  String nameB = b.name() != null ? b.name() : "";
                  return nameA.compareToIgnoreCase(nameB);
                })
                .toList();
    }

    private InvoiceReportResponseDto emptyResponse(int page, int size, List<ReportTaxpayerDto> taxpayers) {
        return new InvoiceReportResponseDto(List.of(), 0, Math.max(page, 0), Math.min(size, MAX_PAGE_SIZE), List.of(), taxpayers);
    }

    private InvoiceReportResponseDto mapToResponse(
            Page<ElectronicInvoiceSystemEntity> resultPage,
            List<ReportTaxpayerDto> taxpayers
    ) {

        List<ElectronicInvoiceSystemEntity> content = resultPage.getContent();

        List<InvoiceReportItemDto> items = content.stream()
                .map(this::mapToItem)
                .toList();

        List<InvoiceStatusSummaryDto> summary = content.stream()
                .collect(Collectors.groupingBy(ElectronicInvoiceSystemEntity::getSts))
                .entrySet()
                .stream()
                .map(entry -> {
                    String status = entry.getKey();
                    List<ElectronicInvoiceSystemEntity> entities = entry.getValue();

                    long count = entities.size();

                    BigDecimal totalAmount = entities.stream()
                            .map(e -> {
                                ElectronicInvoiceHeaderEntity header = e.getHeader();
                                return header != null && header.getTbill() != null
                                        ? header.getTbill()
                                        : BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new InvoiceStatusSummaryDto(status, count, totalAmount);
                })
                .toList();

        return new InvoiceReportResponseDto(
                items,
                resultPage.getTotalElements(),
                resultPage.getNumber(),
                resultPage.getSize(),
                summary,
                taxpayers
        );
    }


    private InvoiceReportItemDto mapToItem(ElectronicInvoiceSystemEntity system) {

        ElectronicInvoiceHeaderEntity header = system.getHeader();

        return InvoiceReportItemDto.builder()
                // شناسه سربرگ
                .headerId(header.getId())

                // اطلاعات اصلی صورتحساب
                .taxId(Objects.nonNull(header) && Objects.nonNull(header.getTaxId()) ? header.getTaxId(): null)
                .invoiceNumber(header.getInno())
                .templateCode(header.getTemplateCode())

                // الگو و موضوع صورتحساب
                .patternNumber(header.getInp())
                .invoiceSubject(
                        header.getIns() != null ? header.getIns().getValue() : null
                )
                .invoiceSubjectLabel(
                        header.getIns() != null ? header.getIns().getPersianName() : null
                )

                // فروشنده
                .sellerTaxpayerNo(
                        /*header.getSellerTaxpayer() != null
                                ? header.getSellerTaxpayer().getTaxId()
                                :*/ null
                )
                .sellerName(
                        /*header.getSellerTaxpayer() != null
                                ? header.getSellerTaxpayer().getName()
                                :*/ null
                )

                // خریدار
                .buyerTaxpayerNo(
                        /*header.getBuyerTaxpayer() != null
                                ? header.getBuyerTaxpayer().getTaxId()
                                :*/ null
                )
                .buyerName(
                        /*header.getBuyerTaxpayer() != null
                                ? header.getBuyerTaxpayer().getName()
                                :*/ null
                )

                // مبلغ کل
                .totalAmount(header.getTbill())

                // زمان‌ها
                .createdOn(header.getCreatedOn())
                .statusTimestamp(system.getSrvdt())

                // وضعیت
                .status(system.getSts())

                // اطلاعات رد
                .rejectionCode(system.getRejcode())
                .rejectionDescription(system.getRejdesc())

                // شماره مرجع سامانه
                .referenceNumber(system.getReferenceNumber())

                .build();
    }

    private List<InvoiceStatusSummaryDto> buildStatusSummary(
            List<ElectronicInvoiceSystemEntity> allResults
    ) {
        return allResults.stream()
                .collect(Collectors.groupingBy(
                        ElectronicInvoiceSystemEntity::getSts
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    String status = entry.getKey();
                    List<ElectronicInvoiceSystemEntity> items = entry.getValue();

                    long count = items.size();

                    BigDecimal totalAmount = items.stream()
                            .map(e -> {
                                ElectronicInvoiceHeaderEntity header = e.getHeader();
                                return header != null && header.getTbill() != null
                                        ? header.getTbill()
                                        : BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new InvoiceStatusSummaryDto(status, count, totalAmount);
                })
                .toList();
    }

    private ElectronicInvoiceHeaderEntity findHeader(UUID id) {
        Optional<ElectronicInvoiceHeaderEntity> headerOpt = headerService.findById(id);
        if (headerOpt.isEmpty()) {
            return null;
        }
        return headerOpt.get();
    }



}
