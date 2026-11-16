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
            "CANCELLED"
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
        // فرض می‌کنیم page خروجی متد findAll شماست
        List<ElectronicInvoiceSystemEntity> content = page.getContent();

        for (ElectronicInvoiceSystemEntity systemEntity : content) {
            UUID currentId = systemEntity.getId();
            System.out.println("Processing System Entity ID: " + currentId);

            // پیدا کردن هدر بر اساس آی‌دی
            Optional<ElectronicInvoiceHeaderEntity> headerOptional = headerRepository.findHeaderManual(currentId);

            if (headerOptional.isPresent()) {
                ElectronicInvoiceHeaderEntity header = headerOptional.get();
                System.out.println("Header found for ID: " + currentId);

                // ست کردن هدر در موجودیت سیستم
                systemEntity.setHeader(header);
            } else {
                System.out.println("WARNING: No header found for ID: " + currentId);
                // اینجا می‌توانید متوجه شوید که آیا دیتای یتیم (Orphan) در جدول SYSTEM دارید یا خیر
            }
        }


        List<InvoiceReportItemDto> items = page.getContent().stream()
                .map(this::mapToItemDto)
                .toList();

        List<InvoiceStatusSummaryDto> summary = summarize(filter);

        return new InvoiceReportResponseDto(
                items,
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                summary,
                taxpayers
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(InvoiceReportFilter filter, int maxRows) {
        Specification<ElectronicInvoiceSystemEntity> specification = buildSpecification(filter);
        List<ElectronicInvoiceSystemEntity> results = new ArrayList<>(systemRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdOn")));
        if (maxRows > 0 && results.size() > maxRows) {
            results = results.subList(0, maxRows);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("TaxId,InvoiceNumber,TemplateCode,PatternNumber,InvoiceSubject,SellerTaxpayerNo,SellerName,BuyerTaxpayerNo,BuyerName,TotalAmount,CreatedOn,StatusTimestamp,Status,RejectionCode,RejectionDescription,ReferenceNumber\n");
        for (ElectronicInvoiceSystemEntity systemEntity : results) {
            InvoiceReportItemDto dto = mapToItemDto(systemEntity);
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

       /* if (Objects.nonNull(filter) && filter.sellerTaxpayerId() != null) {
            predicates.add(cb.equal(seller.get("id"), filter.sellerTaxpayerId()));
        }

        if (Objects.nonNull(filter) && !filter.taxpayerIds().isEmpty()) {
            List<String> assignedTaxIds = taxpayerRepository.findAllById(filter.taxpayerIds()).stream()
                    .map(TaxpayerEntity::getTaxId)
                    .filter(Objects::nonNull)
                    .filter(value -> !value.isBlank())
                    .toList();
            if (assignedTaxIds.isEmpty()) {
                predicates.add(cb.disjunction());
            } else {
                Predicate sellerPredicate = cb.upper(header.get("tins")).in(
                        assignedTaxIds.stream().map(value -> value.toUpperCase(Locale.ROOT)).toList());
                Predicate buyerPredicate = cb.upper(header.get("tinb")).in(
                        assignedTaxIds.stream().map(value -> value.toUpperCase(Locale.ROOT)).toList());
                predicates.add(cb.or(sellerPredicate, buyerPredicate));
            }
        }*/

        if (Objects.nonNull(filter) && (filter.taxpayerNo() != null && !filter.taxpayerNo().isBlank())) {
            String pattern = filter.taxpayerNo().trim().toUpperCase(Locale.ROOT);
           /* Predicate sellerPredicate = cb.equal(cb.upper(seller.get("taxpayerNo")), pattern);
            Predicate buyerPredicate = cb.equal(cb.upper(buyer.get("taxpayerNo")), pattern);*/
            /*predicates.add(cb.or(sellerPredicate, buyerPredicate));*/
        }

        if (Objects.nonNull(filter) && !filter.statuses().isEmpty()) {
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

    private InvoiceReportItemDto mapToItemDto(ElectronicInvoiceSystemEntity systemEntity) {
        return headerRepository.findById(systemEntity.getId())
                .map(header -> {
                    String sellerNo = /*header.getSellerTaxpayer() != null ? header.getSellerTaxpayer().getTaxId() : */null;
                    String sellerName = /*header.getSellerTaxpayer() != null ? header.getSellerTaxpayer().getName() :*/ null;
                    String buyerNo = /*header.getBuyerTaxpayer() != null ? header.getBuyerTaxpayer().getTaxId() :*/ null;
                    String buyerName = /*header.getBuyerTaxpayer() != null ? header.getBuyerTaxpayer().getName() :*/ null;
                    Integer subject = header.getIns() != null ? header.getIns().getValue() : null;
                    String subjectLabel = header.getIns() != null ? header.getIns().getPersianName() : null;

                    return new InvoiceReportItemDto(
                            header.getId(),
                            header.getTaxId(),
                            header.getInno(),
                            header.getTemplateCode(),
                            header.getInp(),
                            subject,
                            subjectLabel,
                            sellerNo,
                            sellerName,
                            buyerNo,
                            buyerName,
                            header.getTbill(),
                            header.getCreatedOn(),
                            systemEntity.getSrvdt(),
                            systemEntity.getSts(),
                            systemEntity.getRejcode(),
                            systemEntity.getRejdesc(),
                            systemEntity.getReferenceNumber()
                    );
                })
                .orElseThrow(() -> new EntityNotFoundException(
                        "InvoiceHeader not found for systemEntity id=" + systemEntity.getId()
                ));
    }


    private List<InvoiceStatusSummaryDto> summarize(InvoiceReportFilter filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
        Root<ElectronicInvoiceSystemEntity> root = cq.from(ElectronicInvoiceSystemEntity.class);
        Join<ElectronicInvoiceSystemEntity, ElectronicInvoiceHeaderEntity> header = root.join("header", JoinType.INNER);
        Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> seller = header.join("sellerTaxpayer", JoinType.LEFT);
        Join<ElectronicInvoiceHeaderEntity, TaxpayerEntity> buyer = header.join("buyerTaxpayer", JoinType.LEFT);

        /*List<Predicate> predicates = buildCommonPredicates(filter, cb, root, header, seller, buyer);*/
        List<Predicate> predicates = buildCommonPredicates(filter, cb, root, header);
        /*cq.multiselect(root.get("sts"), cb.count(root), cb.sum(header.get("tbill")));*/
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(Predicate[]::new));
        }
        /*cq.groupBy(root.get("sts"));*/

        List<Tuple> tuples = entityManager.createQuery(cq).getResultList();
        Map<String, InvoiceStatusSummaryDto> map = tuples.stream().collect(Collectors.toMap(
                tuple -> {
                    String status = tuple.get(0, String.class);
                    return status != null ? status : "UNKNOWN";
                },
                tuple -> new InvoiceStatusSummaryDto(
                        tuple.get(0, String.class) != null ? tuple.get(0, String.class) : "UNKNOWN",
                        tuple.get(1, Long.class),
                        toBigDecimal(tuple.get(2))
                )
        ));

        List<InvoiceStatusSummaryDto> result = new ArrayList<>();
        for (String status : KNOWN_STATUSES) {
            InvoiceStatusSummaryDto existing = map.get(status);
            if (existing != null) {
                result.add(existing);
            } else {
                result.add(new InvoiceStatusSummaryDto(status, 0, BigDecimal.ZERO));
            }
        }
        // add any unknown statuses that may exist
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
