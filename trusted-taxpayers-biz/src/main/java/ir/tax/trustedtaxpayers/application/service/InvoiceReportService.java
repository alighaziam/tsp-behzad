package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.dto.InvoiceReportItemDto;
import ir.tax.trustedtaxpayers.application.dto.InvoiceReportResponseDto;
import ir.tax.trustedtaxpayers.application.dto.InvoiceStatusSummaryDto;
import ir.tax.trustedtaxpayers.application.dto.ReportTaxpayerDto;
import ir.tax.trustedtaxpayers.core.spec.BaseSpecification;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceReportService {

    private static final List<String> KNOWN_STATUSES = List.of(
            "DRAFT",
            "SUBMITTED",
            "PENDING",
            "ACCEPTED",
            "REJECTED",
            "CANCELLED",
            "FAILED"
    );

    private final ElectronicInvoiceSystemRepository systemRepository;
    private final EntityManager entityManager;
    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final InvoiceExportService invoiceExportService;
    private final TaxpayerRepository taxpayerRepository;

    @Transactional(readOnly = true)
    public Optional<ElectronicInvoiceHeaderEntity> findInvoiceById(UUID headerId) {
        return headerRepository.findById(headerId);
    }

    @Transactional(readOnly = true)
    public byte[] exportInvoiceToPdf(ElectronicInvoiceHeaderEntity invoice) {
        String title = String.format("صورتحساب %s", invoice.getTaxId());
        // تلاش برای افزودن ReferenceNumber به عنوان بخشی از عنوان در صورت وجود
        try {
            var systemOpt = systemRepository.findByHeaderId(invoice.getId());
            if (systemOpt.isPresent() && systemOpt.get().getReferenceNumber() != null) {
                title = String.format("صورتحساب %s | Ref: %s", invoice.getTaxId(), systemOpt.get().getReferenceNumber());
            }
        } catch (Exception ignored) { }
        return invoiceExportService.exportToPdf(List.of(invoice), title);
    }

    @Transactional(readOnly = true)
    public InvoiceReportResponseDto fetchReport(InvoiceReportFilter filter, Pageable pageable, List<ReportTaxpayerDto> taxpayers) {
        Specification<ElectronicInvoiceSystemEntity> specification = buildSpecification(filter);
        Page<ElectronicInvoiceSystemEntity> page = systemRepository.findAll(specification, pageable);
        List<ElectronicInvoiceSystemEntity> content = page.getContent();

        Map<UUID, ElectronicInvoiceHeaderEntity> headersById = headerRepository.findAllById(
                        content.stream().map(ElectronicInvoiceSystemEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(ElectronicInvoiceHeaderEntity::getId, header -> header));
        content.forEach(system -> system.setHeader(headersById.get(system.getId())));

        Map<String, TaxpayerEntity> taxpayersByTaxId = loadTaxpayersByTaxId(headersById.values());
        List<InvoiceReportItemDto> items = content.stream()
                .map(system -> mapToItemDto(system, taxpayersByTaxId))
                .toList();

        List<InvoiceStatusSummaryDto> summary = summarize(filter);
        return new InvoiceReportResponseDto(
                items, page.getTotalElements(), page.getNumber(), page.getSize(), summary, taxpayers);
    }

    @Transactional(readOnly = true)
    public String exportCsv(InvoiceReportFilter filter, int maxRows) {
        Specification<ElectronicInvoiceSystemEntity> specification = buildSpecification(filter);
        List<ElectronicInvoiceSystemEntity> results = new ArrayList<>(
                systemRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdOn")));
        if (maxRows > 0 && results.size() > maxRows) {
            results = results.subList(0, maxRows);
        }

        Map<UUID, ElectronicInvoiceHeaderEntity> headersById = headerRepository.findAllById(
                        results.stream().map(ElectronicInvoiceSystemEntity::getId).toList())
                .stream()
                .collect(Collectors.toMap(ElectronicInvoiceHeaderEntity::getId, header -> header));
        results.forEach(system -> system.setHeader(headersById.get(system.getId())));
        Map<String, TaxpayerEntity> taxpayersByTaxId = loadTaxpayersByTaxId(headersById.values());

        StringBuilder csv = new StringBuilder();
        csv.append("TaxId,InvoiceNumber,TemplateCode,PatternNumber,InvoiceSubject,SellerTaxpayerNo,SellerName,BuyerTaxpayerNo,BuyerName,TotalAmount,CreatedOn,StatusTimestamp,Status,RejectionCode,RejectionDescription,ReferenceNumber\n");
        for (ElectronicInvoiceSystemEntity systemEntity : results) {
            InvoiceReportItemDto dto = mapToItemDto(systemEntity, taxpayersByTaxId);
            csv.append(quote(dto.taxId())).append(',')
               .append(quote(dto.invoiceNumber())).append(',')
               .append(quote(dto.templateCode())).append(',')
               .append(quote(safeNumber(dto.patternNumber()))).append(',')
               .append(quote(dto.invoiceSubjectLabel())).append(',')
               .append(quote(dto.sellerTaxpayerNo())).append(',')
               .append(quote(dto.sellerName())).append(',')
               .append(quote(dto.buyerTaxpayerNo())).append(',')
               .append(quote(dto.buyerName())).append(',')
               .append(quote(safeBigDecimal(dto.totalAmount()))).append(',')
               .append(quote(safeDate(dto.createdOn()))).append(',')
               .append(quote(safeDate(dto.statusTimestamp()))).append(',')
               .append(quote(dto.status())).append(',')
               .append(quote(dto.rejectionCode())).append(',')
               .append(quote(dto.rejectionDescription())).append(',')
               .append(quote(dto.referenceNumber())).append('\n');
        }
        return csv.toString();
    }

    /*private Specification<ElectronicInvoiceSystemEntity> buildSpecification(InvoiceReportFilter filter) {
        return (root, query, cb) -> {
            // ۱. تعریف Join ها برای استفاده در Predicate ها (همیشه لازم است)
            Join<ElectronicInvoiceSystemEntity, ElectronicInvoiceHeaderEntity> headerJoin = root.join("header", JoinType.INNER);
            Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> sellerJoin = headerJoin.join("sellerTaxpayer", JoinType.LEFT);
            Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> buyerJoin = headerJoin.join("buyerTaxpayer", JoinType.LEFT);

            // ۲. انجام Fetch فقط اگر کوئری برای دریافت دیتا است (نه برای Count)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("header", JoinType.INNER)
                        .fetch("sellerTaxpayer", JoinType.LEFT);
                // نکته: Fetch دوم را روی همان مسیر Header انجام دهید
            }

            List<Predicate> predicates = buildCommonPredicates(filter, cb, root, headerJoin, sellerJoin, buyerJoin);

            // ۳. حذف distinct در صورت امکان (Distinct باعث سنگین شدن کوئری اوراکل و تداخل با RAW می‌شود)
            // query.distinct(true);

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }*/

    private Specification<ElectronicInvoiceSystemEntity> buildSpecification(InvoiceReportFilter filter) {
        return (root, query, cb) -> {
            // ۱. تعریف Join ها (این بخش باعث شناسایی سمبل های header, seller, buyer می شود)
            Join<ElectronicInvoiceSystemEntity, ElectronicInvoiceHeaderEntity> header = root.join("header", JoinType.INNER);
            /*Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> seller = header.join("sellerTaxpayer", JoinType.LEFT);
            Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> buyer = header.join("buyerTaxpayer", JoinType.LEFT);*/

            // ۲. حذف Fetch ها برای جلوگیری از خطای "8 byte" در Pagination
            // این بخش را دیگر اضافه نکنید یا اگر هست پاک کنید:
            // root.fetch("header", ...); // عامل خطا

            // ۳. فراخوانی متد Predicate ها با متغیرهایی که در بالا تعریف کردیم
            /*List<Predicate> predicates = buildCommonPredicates(filter, cb, root, header, seller, buyer);*/
            List<Predicate> predicates = buildCommonPredicates(filter, cb, root, header);

            // ۴. مدیریت کوئری های Count و Distinct
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<Predicate> buildCommonPredicates(InvoiceReportFilter filter,
                                                  CriteriaBuilder cb,
                                                  Root<ElectronicInvoiceSystemEntity> root,
                                                  Join<ElectronicInvoiceSystemEntity, ElectronicInvoiceHeaderEntity> header
                                                  ) {
        List<Predicate> predicates = new ArrayList<>();

        if (Objects.nonNull(filter) && filter.taxpayerIds() != null && !filter.taxpayerIds().isEmpty()) {
            List<String> assignedTaxIds = taxpayerRepository.findAllById(filter.taxpayerIds()).stream()
                    .map(TaxpayerEntity::getTaxId)
                    .filter(Objects::nonNull)
                    .filter(value -> !value.isBlank())
                    .map(value -> value.toUpperCase(Locale.ROOT))
                    .toList();
            if (assignedTaxIds.isEmpty()) {
                predicates.add(cb.disjunction());
            } else {
                Predicate sellerPredicate = cb.upper(header.get("tins")).in(assignedTaxIds);
                Predicate buyerPredicate = cb.upper(header.get("tinb")).in(assignedTaxIds);
                predicates.add(cb.or(sellerPredicate, buyerPredicate));
            }
        }

        if (Objects.nonNull(filter) && filter.taxpayerNo() != null && !filter.taxpayerNo().isBlank()) {
            String pattern = filter.taxpayerNo().trim().toUpperCase(Locale.ROOT);
            Predicate sellerPredicate = cb.equal(cb.upper(header.get("tins")), pattern);
            Predicate buyerPredicate = cb.equal(cb.upper(header.get("tinb")), pattern);
            predicates.add(cb.or(sellerPredicate, buyerPredicate));
        }

        if (Objects.nonNull(filter) && filter.statuses() != null && !filter.statuses().isEmpty()) {
            predicates.add(root.get("sts").in(filter.statuses()));
        }

        if (Objects.nonNull(filter) && filter.fromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(header.get("createdOn"), filter.fromDate()));
        }

        if (Objects.nonNull(filter) && filter.toDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(header.get("createdOn"), filter.toDate()));
        }

        return predicates;
    }

    private Map<String, TaxpayerEntity> loadTaxpayersByTaxId(
            java.util.Collection<ElectronicInvoiceHeaderEntity> headers) {
        Set<String> taxIds = headers.stream()
                .flatMap(header -> java.util.stream.Stream.of(header.getTins(), header.getTinb()))
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        if (taxIds.isEmpty()) {
            return Map.of();
        }
        return taxpayerRepository.findByTaxIdIn(taxIds).stream()
                .collect(Collectors.toMap(
                        taxpayer -> taxpayer.getTaxId().toUpperCase(Locale.ROOT),
                        taxpayer -> taxpayer,
                        (first, second) -> first
                ));
    }

    private InvoiceReportItemDto mapToItemDto(
            ElectronicInvoiceSystemEntity systemEntity,
            Map<String, TaxpayerEntity> taxpayersByTaxId) {
        ElectronicInvoiceHeaderEntity header = systemEntity.getHeader();
        if (header == null) {
            header = headerRepository.findById(systemEntity.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "InvoiceHeader not found for systemEntity id=" + systemEntity.getId()));
        }

        String sellerNo = header.getTins();
        String buyerNo = header.getTinb();
        TaxpayerEntity seller = sellerNo == null ? null : taxpayersByTaxId.get(sellerNo.toUpperCase(Locale.ROOT));
        TaxpayerEntity buyer = buyerNo == null ? null : taxpayersByTaxId.get(buyerNo.toUpperCase(Locale.ROOT));
        Integer subject = header.getIns() != null ? header.getIns().getValue() : null;
        String subjectLabel = header.getIns() != null ? header.getIns().getPersianName() : null;

        return new InvoiceReportItemDto(
                header.getId(), header.getTaxId(), header.getInno(), header.getTemplateCode(),
                header.getInp(), subject, subjectLabel, sellerNo, seller != null ? seller.getName() : null,
                buyerNo, buyer != null ? buyer.getName() : null, header.getTbill(), header.getCreatedOn(),
                systemEntity.getSrvdt(), systemEntity.getSts(), systemEntity.getRejcode(),
                systemEntity.getRejdesc(), systemEntity.getReferenceNumber());
    }


    private List<InvoiceStatusSummaryDto> summarize(InvoiceReportFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ElectronicInvoiceSystemEntity> root = cq.from(ElectronicInvoiceSystemEntity.class);
        Join<ElectronicInvoiceSystemEntity, ElectronicInvoiceHeaderEntity> header = root.join("header", JoinType.INNER);

        List<Predicate> predicates = buildCommonPredicates(filter, cb, root, header);
        cq.multiselect(
                root.get("sts").alias("status"),
                cb.count(root).alias("invoiceCount"),
                cb.sum(header.get("tbill")).alias("totalAmount")
        );
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(Predicate[]::new));
        }
        cq.groupBy(root.get("sts"));

        Map<String, InvoiceStatusSummaryDto> map = entityManager.createQuery(cq).getResultList().stream()
                .collect(Collectors.toMap(
                        tuple -> {
                            String status = tuple.get("status", String.class);
                            return status != null ? status : "UNKNOWN";
                        },
                        tuple -> {
                            String status = tuple.get("status", String.class);
                            Long count = tuple.get("invoiceCount", Long.class);
                            BigDecimal total = tuple.get("totalAmount", BigDecimal.class);
                            return new InvoiceStatusSummaryDto(
                                    status != null ? status : "UNKNOWN",
                                    count != null ? count : 0L,
                                    total != null ? total : BigDecimal.ZERO
                            );
                        },
                        (first, second) -> first
                ));

        List<InvoiceStatusSummaryDto> result = new ArrayList<>();
        for (String status : KNOWN_STATUSES) {
            result.add(map.getOrDefault(status,
                    new InvoiceStatusSummaryDto(status, 0L, BigDecimal.ZERO)));
        }
        map.forEach((status, summary) -> {
            if (!KNOWN_STATUSES.contains(status)) {
                result.add(summary);
            }
        });
        return result;
    }


    private static String quote(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        if (text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return '"' + text + '"';
        }
        return text;
    }

    private static String safeNumber(Number number) {
        return number != null ? number.toString() : "";
    }

    private static BigDecimal safeBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private static String safeDate(OffsetDateTime value) {
        return value != null ? value.toString() : "";
    }

    @Builder
    public record InvoiceReportFilter(
            Set<UUID> taxpayerIds,
            UUID sellerTaxpayerId,
            String taxpayerNo,
            List<String> statuses,
            OffsetDateTime fromDate,
            OffsetDateTime toDate
    ) {
        public InvoiceReportFilter {
            taxpayerIds = taxpayerIds == null ? Set.of() : taxpayerIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            statuses = statuses == null ? List.of() : statuses.stream()
                    .filter(Objects::nonNull)
                    .map(value -> value.trim().toUpperCase(Locale.ROOT))
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }
    }
}
