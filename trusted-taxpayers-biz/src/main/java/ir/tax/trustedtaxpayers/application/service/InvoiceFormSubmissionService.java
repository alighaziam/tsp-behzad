package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.beans.model.InvoiceType;
import ir.tax.trustedtaxpayers.enums.InvoiceStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceItemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceFormSubmissionService {

    private final ElectronicInvoiceHeaderService headerService;
    private final ElectronicInvoiceItemService itemService;
    private final TaxpayerService taxpayerService;
    private final InvoiceTemplateLoaderService templateLoaderService;
    private final TaxpayerTspContractService contractService;

    public static class SubmitResult {
        public final UUID headerId;
        public final String taxId;
        public final String templateCode;
        public final String templateTypeCode;
        public final Integer patternNumber;
        public final Integer invoiceSubject;
        public final UUID sellerTaxpayerId;

        public SubmitResult(UUID headerId,
                            String taxId,
                            String templateCode,
                            String templateTypeCode,
                            Integer patternNumber,
                            Integer invoiceSubject,
                            UUID sellerTaxpayerId) {
            this.headerId = headerId;
            this.taxId = taxId;
            this.templateCode = templateCode;
            this.templateTypeCode = templateTypeCode;
            this.patternNumber = patternNumber;
            this.invoiceSubject = invoiceSubject;
            this.sellerTaxpayerId = sellerTaxpayerId;
        }
    }

    /**
     * ذخیره صورتحساب به عنوان پیش‌نویس (بدون validation کامل)
     * Save invoice as draft (without full validation)
     */
    @Transactional
    public SubmitResult saveDraft(Integer patternNumber, Map<String, Object> header, List<Map<String, Object>> items) {
        if (header == null) header = new HashMap<>();
        if (items == null) items = new ArrayList<>();

        // 1) map header
        ElectronicInvoiceHeaderEntity headerEntity = new ElectronicInvoiceHeaderEntity();

        // برای پیش‌نویس، validation سبک‌تر است - فقط فیلدهای ضروری
        // taxId (22 chars). اگر ورودی ندارد، تولید کن
        String taxId = getString(header.get("irtaxid"));
        if (taxId == null || taxId.isBlank()) {
            taxId = generateTaxId();
        }
        headerEntity.setTaxId(taxId);

        // templateCode: اولین template فعال بر اساس inty
        Integer intyVal = getInteger(header.get("inty"));
        if (intyVal != null) {
            try {
                InvoiceType invType = InvoiceType.fromValue(intyVal);
                headerEntity.setInty(invType);
            } catch (Exception e) {
                /*log.warn("Invalid inty value: {}", intyVal);*/
            }
        }

        String templateCode = null;
        String templateTypeCode = null;
        if (intyVal != null) {
            var templateOpt = templateLoaderService.loadFirstTemplateByInty(intyVal);
            if (templateOpt.isPresent()) {
                templateCode = templateOpt.get().getCode();
            }
            templateTypeCode = templateLoaderService.getTemplateTypeCode(intyVal);
        }
        if (templateCode == null) {
            templateCode = getString(header.get("templateCode"));
        }
        headerEntity.setTemplateCode(templateCode);
        if (templateTypeCode == null && intyVal != null) {
            templateTypeCode = templateLoaderService.getTemplateTypeCode(intyVal);
        }

        // pattern (inp)
        headerEntity.setInp(patternNumber);

        // basic fields (optional for draft)
        headerEntity.setInno(getString(header.get("inno")));
        Integer insVal = getInteger(header.get("ins"));
        if (insVal != null) {
            headerEntity.setIns(ir.tax.trustedtaxpayers.beans.model.InvoiceSubject.fromValue(insVal));
        }
        headerEntity.setIrtaxid(getString(header.get("irtaxid")));
        headerEntity.setTins(getString(header.get("tins")));
        headerEntity.setSbc(getString(header.get("sbc")));
        Integer tobVal = getInteger(header.get("tob"));
        if (tobVal != null) {
            headerEntity.setTob(ir.tax.trustedtaxpayers.beans.model.PersonType.fromValue(tobVal));
        }
        headerEntity.setBid(getString(header.get("bid")));
        headerEntity.setTinb(getString(header.get("tinb")));
        headerEntity.setBbc(getString(header.get("bbc")));
        headerEntity.setBpc(getString(header.get("bpc")));
        headerEntity.setBpn(getString(header.get("bpn")));

        // datetime to epoch millis
        Long indatim = getEpochMillis(header.get("indatim"));
        if (indatim == null) {
            indatim = System.currentTimeMillis(); // default to now
        }
        headerEntity.setIndatim(indatim);

        // totals (optional for draft)
        headerEntity.setTprdis(getBigDecimal(header.get("tprdis")));
        headerEntity.setTdis(getBigDecimal(header.get("tdis")));
        headerEntity.setTadis(getBigDecimal(header.get("tadis")));
        headerEntity.setTvam(getBigDecimal(header.get("tvam")));
        headerEntity.setTbill(getBigDecimal(header.get("tbill")));
        Integer setmVal = getInteger(header.get("setm"));
        if (setmVal != null) {
            headerEntity.setSetm(ir.tax.trustedtaxpayers.beans.model.SettlementMethod.fromValue(setmVal));
        }
        headerEntity.setCap(getBigDecimal(header.get("cap")));
        headerEntity.setInsp(getBigDecimal(header.get("insp")));
        headerEntity.setTvop(getBigDecimal(header.get("tvop")));

        // seller/buyer taxpayers (optional for draft)
        UUID sellerTaxpayerId = resolveSellerTaxpayerId(headerEntity, false);

        // برای پیش‌نویس، قرارداد را چک نمی‌کنیم
        // 2) save header (بدون validation کامل)
        headerEntity = headerService.save(headerEntity);

        // 3) map & save items
        UUID headerId = headerEntity.getId();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> it = items.get(i);
            if (it == null) continue;
            ElectronicInvoiceItemEntity item = new ElectronicInvoiceItemEntity();
            item.setHeaderId(headerId);
            item.setLineNo(i + 1);
            item.setSstid(getString(it.get("sstid")));
            item.setSstt(getString(it.get("sstt")));
            item.setMu(getString(it.get("mu")));
            item.setAm(getBigDecimal(it.get("am")));
            item.setFee(getBigDecimal(it.get("fee")));
            item.setPrdis(getBigDecimal(it.get("prdis")));
            item.setDis(getBigDecimal(it.get("dis")));
            item.setAdis(getBigDecimal(it.get("adis")));
            item.setVra(getBigDecimal(it.get("vra")));
            item.setVam(getBigDecimal(it.get("vam")));

            // pattern-specific
            item.setCut(getString(it.get("cut")));
            item.setCfee(getBigDecimal(it.get("cfee")));
            item.setExr(getBigDecimal(it.get("exr")));
            item.setCpr(getBigDecimal(it.get("cpr")));
            item.setVba(getBigDecimal(it.get("vba")));

            itemService.save(item);
        }

        Integer insValue = headerEntity.getIns() != null ? headerEntity.getIns().getValue() : null;

        return new SubmitResult(
                headerId,
                taxId,
                templateCode,
                templateTypeCode,
                patternNumber,
                insValue,
                sellerTaxpayerId
        );
    }

    @Transactional
    public SubmitResult submit(Integer patternNumber, Map<String, Object> header, List<Map<String, Object>> items) {
        if (header == null) header = new HashMap<>();
        if (items == null) items = new ArrayList<>();

        // 1) map header
        ElectronicInvoiceHeaderEntity headerEntity = new ElectronicInvoiceHeaderEntity();

        // taxId (22 chars). اگر ورودی ندارد، تولید کن
        String taxId = getString(header.get("irtaxid"));
        if (taxId == null || taxId.isBlank()) {
            taxId = generateTaxId();
        }
        headerEntity.setTaxId(taxId);

        // templateCode: اولین template فعال بر اساس inty
        Integer intyVal = getInteger(header.get("inty"));
        if (intyVal != null) {
            try {
                InvoiceType invType = InvoiceType.fromValue(intyVal);
                headerEntity.setInty(invType);
            } catch (Exception e) {
                /*log.warn("Invalid inty value: {}", intyVal);*/
            }
        }

        String templateCode = null;
        String templateTypeCode = null;
        if (intyVal != null) {
            var templateOpt = templateLoaderService.loadFirstTemplateByInty(intyVal);
            if (templateOpt.isPresent()) {
                templateCode = templateOpt.get().getCode();
                /*templateTypeCode = templateOpt.get().getTemplateTypeCode();*/
            }
        }
        if (templateCode == null) {
            // fallback ایمن برای عبور از TemplateValidator فقط در صورت وجود چنین رکوردی
            // در غیراینصورت TemplateValidator ذخیره را رد می‌کند
            templateCode = getString(header.get("templateCode"));
        }
        headerEntity.setTemplateCode(templateCode);
        if (templateTypeCode == null && intyVal != null) {
            templateTypeCode = templateLoaderService.getTemplateTypeCode(intyVal);
        }

        // pattern (inp)
        headerEntity.setInp(patternNumber);

        // basic fields
        headerEntity.setInno(getString(header.get("inno")));
        headerEntity.setIns(ir.tax.trustedtaxpayers.beans.model.InvoiceSubject.fromValue(getInteger(header.get("ins"))));
        headerEntity.setIrtaxid(getString(header.get("irtaxid"))); // ممکن است null باشد
        headerEntity.setTins(getString(header.get("tins")));
        headerEntity.setSbc(getString(header.get("sbc")));
        headerEntity.setTob(ir.tax.trustedtaxpayers.beans.model.PersonType.fromValue(getInteger(header.get("tob"))));
        headerEntity.setBid(getString(header.get("bid")));
        headerEntity.setTinb(getString(header.get("tinb")));
        headerEntity.setBbc(getString(header.get("bbc")));
        headerEntity.setBpc(getString(header.get("bpc")));
        headerEntity.setBpn(getString(header.get("bpn")));

        // datetime to epoch millis
        Long indatim = getEpochMillis(header.get("indatim"));
        headerEntity.setIndatim(indatim);

        // totals
        headerEntity.setTprdis(getBigDecimal(header.get("tprdis")));
        headerEntity.setTdis(getBigDecimal(header.get("tdis")));
        headerEntity.setTadis(getBigDecimal(header.get("tadis")));
        headerEntity.setTvam(getBigDecimal(header.get("tvam")));
        headerEntity.setTbill(getBigDecimal(header.get("tbill")));
        headerEntity.setSetm(ir.tax.trustedtaxpayers.beans.model.SettlementMethod.fromValue(getInteger(header.get("setm"))));
        headerEntity.setCap(getBigDecimal(header.get("cap")));
        headerEntity.setInsp(getBigDecimal(header.get("insp")));
        headerEntity.setTvop(getBigDecimal(header.get("tvop")));

        // seller/buyer taxpayers
        UUID sellerTaxpayerId = resolveSellerTaxpayerId(headerEntity, true);
        if (sellerTaxpayerId != null) {
            boolean hasActiveContract = contractService.findActiveContract(sellerTaxpayerId).isPresent();
            if (!hasActiveContract) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "برای مودی فروشنده قرارداد همکاری با شرکت معتمد ثبت نشده است.");
            }
        }

        // Set status to READY_TO_SEND before saving
        headerEntity.setStatus(InvoiceStatus.READY_TO_SEND);

        // 2) save header (TemplateValidator اجرا می‌شود)
        headerEntity = headerService.save(headerEntity);

        // 3) map & save items
        UUID headerId = headerEntity.getId();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> it = items.get(i);
            if (it == null) continue;
            ElectronicInvoiceItemEntity item = new ElectronicInvoiceItemEntity();
            item.setHeaderId(headerId);
            item.setLineNo(i + 1);
            item.setSstid(getString(it.get("sstid")));
            item.setSstt(getString(it.get("sstt")));
            item.setMu(getString(it.get("mu")));
            item.setAm(getBigDecimal(it.get("am")));
            item.setFee(getBigDecimal(it.get("fee")));
            item.setPrdis(getBigDecimal(it.get("prdis")));
            item.setDis(getBigDecimal(it.get("dis")));
            item.setAdis(getBigDecimal(it.get("adis")));
            item.setVra(getBigDecimal(it.get("vra")));
            item.setVam(getBigDecimal(it.get("vam")));

            // pattern-specific (e.g., pattern 2 FX)
            item.setCut(getString(it.get("cut")));
            item.setCfee(getBigDecimal(it.get("cfee")));
            item.setExr(getBigDecimal(it.get("exr")));
            item.setCpr(getBigDecimal(it.get("cpr")));
            item.setVba(getBigDecimal(it.get("vba")));

            itemService.save(item);
        }

        sellerTaxpayerId = /*headerEntity.getSellerTaxpayer() != null ? headerEntity.getSellerTaxpayer().getId() :*/ null;
        Integer insValue = headerEntity.getIns() != null ? headerEntity.getIns().getValue() : null;

        return new SubmitResult(
                headerId,
                taxId,
                templateCode,
                templateTypeCode,
                patternNumber,
                insValue,
                sellerTaxpayerId
        );
    }

    private UUID resolveSellerTaxpayerId(ElectronicInvoiceHeaderEntity headerEntity, boolean required) {
        String tins = headerEntity.getTins();
        if (tins == null || tins.isBlank()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "شناسه اقتصادی فروشنده الزامی است.");
            }
            return null;
        }

        TaxpayerEntity seller = taxpayerService.findByTaxId(tins.trim())
                .filter(taxpayer -> Boolean.TRUE.equals(taxpayer.getIsActive()))
                .orElse(null);
        if (seller == null && required) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "مودی فروشنده ثبت‌شده و فعال یافت نشد.");
        }
        return seller != null ? seller.getId() : null;
    }


    private static final int TAX_ID_LENGTH = 22;
    private static final String TAX_ID_PADDING = "XXXXXXXXXXXXXXXXXXXXXX";

    private String generateTaxId() {
        // 22 کاراکتر حروف/عدد: زمان + رندوم
        String base = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        String rand = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase(Locale.ROOT);
        String s = (base + rand).replaceAll("[^A-Z0-9]", "");
        int length = Math.min(TAX_ID_LENGTH, s.length());
        String padded = s.substring(0, length) + TAX_ID_PADDING;
        return padded.substring(0, TAX_ID_LENGTH);
    }

    private String getString(Object o) {
        return o == null ? null : o.toString();
    }

    private Integer getInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal getBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(((Number) o).toString());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return null; }
    }

    private Long getEpochMillis(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof Date) return ((Date) o).getTime();
        try {
            OffsetDateTime odt = OffsetDateTime.parse(o.toString());
            return odt.toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}


