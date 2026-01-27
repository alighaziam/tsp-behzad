package ir.tax.trustedtaxpayers.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "DTO های صورتحساب الکترونیکی")
public class ElectronicInvoiceDtos {
    
    // ========== DTO های نوع الگو ==========
    @Schema(description = "DTO نوع الگو")
    public record TemplateTypeDto(
            @Schema(description = "کد نوع الگو") String code, 
            @Schema(description = "عنوان نوع الگو") String title, 
            @Schema(description = "توضیحات") String description) {}
    
    @Schema(description = "درخواست ایجاد نوع الگو")
    public record CreateTemplateTypeRequest(
            @Schema(description = "کد نوع الگو", requiredMode = Schema.RequiredMode.REQUIRED) String code, 
            @Schema(description = "عنوان نوع الگو", requiredMode = Schema.RequiredMode.REQUIRED) String title, 
            @Schema(description = "توضیحات") String description) {}
    
    @Schema(description = "درخواست به‌روزرسانی نوع الگو")
    public record UpdateTemplateTypeRequest(
            @Schema(description = "عنوان نوع الگو") String title, 
            @Schema(description = "توضیحات") String description) {}
    
    // ========== DTO های الگو ==========
    @Schema(description = "DTO الگو")
    public record TemplateDto(
            @Schema(description = "کد الگو") String code, 
            @Schema(description = "عنوان الگو") String title) {}
    
    @Schema(description = "درخواست ایجاد الگو")
    public record CreateTemplateRequest(
            @Schema(description = "کد الگو", requiredMode = Schema.RequiredMode.REQUIRED) String code, 
            @Schema(description = "عنوان الگو", requiredMode = Schema.RequiredMode.REQUIRED) String title) {}
    
    // ========== DTO های قاعده الگو ==========
    @Schema(description = "DTO قاعده الگو")
    public record TemplateRuleDto(
            @Schema(description = "شناسه یکتا") UUID id, 
            @Schema(description = "کد الگو") String templateCode, 
            @Schema(description = "نام فیلد") String fieldName, 
            @Schema(description = "نوع قاعده") String ruleType, 
            @Schema(description = "پارامترهای قاعده") String ruleParams, 
            @Schema(description = "توضیحات") String explanation, 
            @Schema(description = "وضعیت فعال بودن") String active) {}
    
    @Schema(description = "درخواست ایجاد قاعده الگو")
    public record CreateTemplateRuleRequest(
            @Schema(description = "کد الگو", requiredMode = Schema.RequiredMode.REQUIRED) String templateCode, 
            @Schema(description = "نام فیلد", requiredMode = Schema.RequiredMode.REQUIRED) String fieldName, 
            @Schema(description = "نوع قاعده", requiredMode = Schema.RequiredMode.REQUIRED) String ruleType, 
            @Schema(description = "پارامترهای قاعده") String ruleParams, 
            @Schema(description = "توضیحات") String explanation) {}
    
    // ========== DTO های سربرگ صورتحساب ==========
    @Schema(description = "DTO خلاصه سربرگ صورتحساب الکترونیکی")
    public record ElectronicInvoiceHeaderSummaryDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شماره منحصر به فرد مالیاتی (22 کاراکتر)") String taxId,
        @Schema(description = "کد الگو") String templateCode,
        @Schema(description = "نوع صورتحساب (1, 2, 3)") Integer inty,
        @Schema(description = "سریال صورتحساب داخلی") String inno,
        @Schema(description = "تاریخ و زمان صدور صورتحساب (Unix Time)") Long indatim,
        @Schema(description = "شماره اقتصادی فروشنده") String tins,
        @Schema(description = "شماره اقتصادی خریدار") String tinb,
        @Schema(description = "مجموع صورتحساب") BigDecimal tbill,
        @Schema(description = "وضعیت سیستمی") String sts,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn
    ) {}
    
    @Schema(description = "DTO جزئیات سربرگ صورتحساب الکترونیکی")
    public record ElectronicInvoiceHeaderDetailDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شماره منحصر به فرد مالیاتی (taxid) - 22 کاراکتر، اجباری") String taxId,
        @Schema(description = "کد الگو") String templateCode,
        @Schema(description = "زیرنوع الگو") String templateSubtype,
        @Schema(description = "نوع صورتحساب (inty) - 1, 2, 3") Integer inty,
        @Schema(description = "سریال صورتحساب داخلی (inno) - 10 کاراکتر هگزادسیمال") String inno,
        @Schema(description = "تاریخ و زمان صدور صورتحساب (indatim) - Unix Time") Long indatim,
        @Schema(description = "تاریخ و زمان ایجاد صورتحساب (m2Indati) - در شرایط خاص") Long m2Indati,
        @Schema(description = "الگوی صورتحساب (inp) - 1 تا 13") Integer inp,
        @Schema(description = "موضوع صورتحساب (ins) - 1=اصلی, 2=اصلاحی, 3=ابطالی, 4=برگشت از فروش") Integer ins,
        @Schema(description = "شماره مالیاتی صورتحساب مرجع (irtaxid) - 22 کاراکتر") String irtaxid,
        @Schema(description = "شماره اقتصادی فروشنده (tins) - 11-14 رقم") String tins,
        @Schema(description = "کد شعبه فروشنده (sbc) - 4 رقم") String sbc,
        @Schema(description = "نوع شخص خریدار (tob) - 1=حقیقی, 2=حقوقی, 3=مشارکت مدنی, 4=اتباع غیر ایرانی") Integer tob,
        @Schema(description = "شناسه ملی/شماره ملی/شناسه مشارکت مدنی/کد فراگیر اتباع غیر ایرانی (bid)") String bid,
        @Schema(description = "شماره اقتصادی خریدار (tinb)") String tinb,
        @Schema(description = "کد شعبه خریدار (bbc) - 4 رقم") String bbc,
        @Schema(description = "کد پستی خریدار (bpc) - 10 رقم") String bpc,
        @Schema(description = "شماره گذرنامه خریدار (bpn) - 9 کاراکتر") String bpn,
        @Schema(description = "نوع پرواز (ft) - 1=داخلی, 2=خارجی") Integer ft,
        @Schema(description = "شماره پروانه گمرکی (scln) - حداکثر 14 رقم") String scln,
        @Schema(description = "کد گمرک محل اظهار فروشنده (scc) - 5 رقم") String scc,
        @Schema(description = "شماره کوتاژ اظهارنامه گمرکی (cdcn) - حداکثر 14 رقم") String cdcn,
        @Schema(description = "تاریخ کوتاژ اظهارنامه گمرکی (cdcd) - Unix Time - 5 رقم") Integer cdcd,
        @Schema(description = "شماره قرارداد پیمانکاری (crn) - حداکثر 12 رقم") String crn,
        @Schema(description = "شماره اشتراک/شناسه قبض بهره بردار (billid) - حداکثر 19 رقم") String billid,
        @Schema(description = "شناسه یکتای پرداخت (billingId) - حداکثر 50 کاراکتر") String billingId,
        @Schema(description = "مجموع مبلغ قبل از کسر تخفیف (tprdis)") BigDecimal tprdis,
        @Schema(description = "مجموع تخفیفات (tdis)") BigDecimal tdis,
        @Schema(description = "مجموع مبلغ پس از کسر تخفیف (tadis)") BigDecimal tadis,
        @Schema(description = "مجموع مالیات بر ارزش افزوده (tvam)") BigDecimal tvam,
        @Schema(description = "مجموع سایر مالیات، عوارض و وجوه قانونی (todam)") BigDecimal todam,
        @Schema(description = "مجموع صورتحساب (tbill)") BigDecimal tbill,
        @Schema(description = "مجموع سهم مالیات بر ارزش افزوده از پرداخت (tvop)") BigDecimal tvop,
        @Schema(description = "مالیات موضوع ماده 17 (tax17)") BigDecimal tax17,
        @Schema(description = "مجموع وزن خالص (tonw) - 15 رقم صحیح، 8 رقم اعشار") BigDecimal tonw,
        @Schema(description = "مجموع ارزش ریالی (torv)") BigDecimal torv,
        @Schema(description = "مجموع ارزش ارزی (tocv) - 14 رقم صحیح، 4 رقم اعشار") BigDecimal tocv,
        @Schema(description = "روش تسویه (setm) - 1=نقدی, 2=نسیه, 3=نقدی/نسیه") Integer setm,
        @Schema(description = "مبلغ پرداختی نقدی (cap)") BigDecimal cap,
        @Schema(description = "مبلغ نسیه (insp)") BigDecimal insp,
        @Schema(description = "شماره اقتصادی آژانس (tinc)") String tinc,
        @Schema(description = "شناسه یکتای بیمه نامه (inInsuranceId)") String inInsuranceId,
        @Schema(description = "شناسه یکتای الحاقیه (an)") String an,
        @Schema(description = "شماره اعلامیه فروش بورس (asn)") String asn,
        @Schema(description = "تاریخ اعلامیه فروش بورس (asd) - Unix Time") Long asd,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn,
        @Schema(description = "تاریخ به‌روزرسانی") OffsetDateTime updatedOn,
        @Schema(description = "کاربر ایجاد کننده") String createdBy,
        @Schema(description = "کاربر به‌روزرسانی کننده") String updatedBy,
        @Schema(description = "لیست اقلام") List<ElectronicInvoiceItemDto> items,
        @Schema(description = "لیست پرداخت‌ها") List<ElectronicInvoicePaymentDto> payments,
        @Schema(description = "اطلاعات سیستمی") ElectronicInvoiceSystemDto system
    ) {}
    
    @Schema(description = "درخواست ایجاد سربرگ صورتحساب الکترونیکی")
    public record CreateElectronicInvoiceHeaderRequest(
        @Schema(description = "شماره منحصر به فرد مالیاتی (taxid) - 22 کاراکتر، اجباری", requiredMode = Schema.RequiredMode.REQUIRED) String taxId,
        @Schema(description = "کد الگو", requiredMode = Schema.RequiredMode.REQUIRED) String templateCode,
        @Schema(description = "زیرنوع الگو") String templateSubtype,
        @Schema(description = "نوع صورتحساب (inty) - 1, 2, 3", requiredMode = Schema.RequiredMode.REQUIRED) Integer inty,
        @Schema(description = "سریال صورتحساب داخلی (inno)", requiredMode = Schema.RequiredMode.REQUIRED) String inno,
        @Schema(description = "تاریخ و زمان صدور صورتحساب (indatim) - Unix Time", requiredMode = Schema.RequiredMode.REQUIRED) Long indatim,
        @Schema(description = "شماره اقتصادی فروشنده (tins) - 11-14 رقم", requiredMode = Schema.RequiredMode.REQUIRED) String tins,
        @Schema(description = "کد شعبه فروشنده (sbc) - 4 رقم") String sbc,
        @Schema(description = "نوع شخص خریدار (tob) - 1=حقیقی, 2=حقوقی, 3=مشارکت مدنی, 4=اتباع غیر ایرانی", requiredMode = Schema.RequiredMode.REQUIRED) Integer tob,
        @Schema(description = "شناسه ملی/شماره ملی خریدار (bid)") String bid,
        @Schema(description = "شماره اقتصادی خریدار (tinb)") String tinb,
        @Schema(description = "کد شعبه خریدار (bbc) - 4 رقم") String bbc,
        @Schema(description = "کد پستی خریدار (bpc) - 10 رقم") String bpc,
        @Schema(description = "شماره گذرنامه خریدار (bpn) - 9 کاراکتر") String bpn
    ) {}
    
    // ========== DTO های ردیف صورتحساب ==========
    @Schema(description = "DTO ردیف صورتحساب الکترونیکی")
    public record ElectronicInvoiceItemDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "شماره ردیف") Integer lineNo,
        @Schema(description = "شناسه کالا/خدمت (sstid) - 13 رقم، اجباری") String sstid,
        @Schema(description = "شرح کالا/خدمت (sstt) - حداکثر 400 کاراکتر") String sstt,
        @Schema(description = "تعداد/مقدار (am) - 18 رقم صحیح، 8 رقم اعشار") BigDecimal am,
        @Schema(description = "واحد اندازه گیری (mu) - حداکثر 8 کاراکتر") String mu,
        @Schema(description = "وزن خالص (nw) - 15 رقم صحیح، 8 رقم اعشار") BigDecimal nw,
        @Schema(description = "مبلغ واحد (fee) - 18 رقم صحیح، 8 رقم اعشار") BigDecimal fee,
        @Schema(description = "میزان ارز (cfee) - 14 رقم صحیح، 4 رقم اعشار") BigDecimal cfee,
        @Schema(description = "نوع ارز (cut) - 3 کاراکتر ISO 4217") String cut,
        @Schema(description = "نرخ برابری ارز با ریال/نرخ فروش ارز (exr) - 18 رقم صحیح، 8 رقم اعشار") BigDecimal exr,
        @Schema(description = "نرخ خرید ارز (cpr)") BigDecimal cpr,
        @Schema(description = "ارزش ریالی کالا (ssrv) - 18 رقم صحیح، 0 رقم اعشار") BigDecimal ssrv,
        @Schema(description = "ارزش ارزی کالا (sscv) - 14 رقم صحیح، 4 رقم اعشار") BigDecimal sscv,
        @Schema(description = "مبلغ قبل از تخفیف (prdis)") BigDecimal prdis,
        @Schema(description = "مبلغ تخفیف (dis)") BigDecimal dis,
        @Schema(description = "مبلغ بعد از تخفیف (adis)") BigDecimal adis,
        @Schema(description = "نرخ مالیات بر ارزش افزوده (vra)") BigDecimal vra,
        @Schema(description = "مبلغ مالیات بر ارزش افزوده (vam)") BigDecimal vam,
        @Schema(description = "مبلغ پایه مالیات بر ارزش افزوده (vba)") BigDecimal vba,
        @Schema(description = "سهم مالیات بر ارزش افزوده از پرداخت (vop)") BigDecimal vop,
        @Schema(description = "موضوع سایر مالیات و عوارض (odt)") String odt,
        @Schema(description = "نرخ سایر مالیات و عوارض (odr)") BigDecimal odr,
        @Schema(description = "مبلغ سایر مالیات و عوارض (odam)") BigDecimal odam,
        @Schema(description = "موضوع سایر وجوه قانونی (olt)") String olt,
        @Schema(description = "نرخ سایر وجوه قانونی (olr)") BigDecimal olr,
        @Schema(description = "مبلغ سایر وجوه قانونی (olam)") BigDecimal olam,
        @Schema(description = "اجرت ساخت (consfee)") BigDecimal consfee,
        @Schema(description = "سود فروشنده (spro)") BigDecimal spro,
        @Schema(description = "حق العمل (bros)") BigDecimal bros,
        @Schema(description = "جمع کل اجرت، حق العمل و سود (tcpbs)") BigDecimal tcpbs,
        @Schema(description = "شماره قرارداد حق العملکاری (bsrn)") String bsrn,
        @Schema(description = "سهم نقدی از پرداخت (cop)") BigDecimal cop,
        @Schema(description = "عیار (cui)") BigDecimal cui,
        @Schema(description = "مبلغ کل کالا/خدمت (tsstam)") BigDecimal tsstam,
        @Schema(description = "شناسه محصول پزشکی (medid)") String medid,
        @Schema(description = "قیمت مصوب (aspr)") BigDecimal aspr
    ) {}
    
    @Schema(description = "درخواست ایجاد ردیف صورتحساب الکترونیکی")
    public record CreateElectronicInvoiceItemRequest(
        @Schema(description = "شناسه سربرگ", requiredMode = Schema.RequiredMode.REQUIRED) UUID headerId,
        @Schema(description = "شماره ردیف", requiredMode = Schema.RequiredMode.REQUIRED) Integer lineNo,
        @Schema(description = "شناسه کالا/خدمت (sstid) - 13 رقم، اجباری", requiredMode = Schema.RequiredMode.REQUIRED) String sstid,
        @Schema(description = "شرح کالا/خدمت (sstt) - حداکثر 400 کاراکتر", requiredMode = Schema.RequiredMode.REQUIRED) String sstt,
        @Schema(description = "تعداد/مقدار (am) - 18 رقم صحیح، 8 رقم اعشار", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal am,
        @Schema(description = "واحد اندازه گیری (mu) - حداکثر 8 کاراکتر") String mu,
        @Schema(description = "مبلغ واحد (fee) - 18 رقم صحیح، 8 رقم اعشار", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal fee
    ) {}
    
    // ========== DTO های پرداخت صورتحساب ==========
    @Schema(description = "DTO پرداخت صورتحساب الکترونیکی")
    public record ElectronicInvoicePaymentDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "روش پرداخت (pmt)") String pmt,
        @Schema(description = "مبلغ پرداختی (pv)") BigDecimal pv,
        @Schema(description = "تاریخ و زمان پرداخت (pdt)") OffsetDateTime pdt,
        @Schema(description = "شماره سوییچ پرداخت (iinn)") String iinn,
        @Schema(description = "شماره پذیرنده فروشگاهی (acn)") String acn,
        @Schema(description = "شماره پایانه (trmn)") String trmn,
        @Schema(description = "شماره پیگیری/شماره مرجع (trn)") String trn,
        @Schema(description = "شماره کارت پرداخت کننده (pcn)") String pcn,
        @Schema(description = "شماره/شناسه ملی/کد فراگیر پرداخت کننده (pid)") String pid
    ) {}
    
    @Schema(description = "درخواست ایجاد پرداخت صورتحساب الکترونیکی")
    public record CreateElectronicInvoicePaymentRequest(
        @Schema(description = "شناسه سربرگ", requiredMode = Schema.RequiredMode.REQUIRED) UUID headerId,
        @Schema(description = "روش پرداخت (pmt)", requiredMode = Schema.RequiredMode.REQUIRED) String pmt,
        @Schema(description = "مبلغ پرداختی (pv)", requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal pv,
        @Schema(description = "تاریخ و زمان پرداخت (pdt)", requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime pdt,
        @Schema(description = "شماره سوییچ پرداخت (iinn)") String iinn,
        @Schema(description = "شماره پذیرنده فروشگاهی (acn)") String acn,
        @Schema(description = "شماره پایانه (trmn)") String trmn,
        @Schema(description = "شماره پیگیری/شماره مرجع (trn)") String trn,
        @Schema(description = "شماره کارت پرداخت کننده (pcn)") String pcn,
        @Schema(description = "شماره/شناسه ملی/کد فراگیر پرداخت کننده (pid)") String pid
    ) {}
    
    // ========== DTO های مرجع صورتحساب ==========
    @Schema(description = "DTO مرجع صورتحساب الکترونیکی")
    public record ElectronicInvoiceReferenceDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "شماره مالیاتی صورتحساب مرجع (irtaxid)") String irtaxid,
        @Schema(description = "شماره داخلی صورتحساب مرجع (inref)") String inref,
        @Schema(description = "نوع مرجع (reftyp)") String reftyp,
        @Schema(description = "شماره اعلامیه (asno)") String asno,
        @Schema(description = "تاریخ اعلامیه (asdt)") java.time.LocalDate asdt,
        @Schema(description = "شماره قرارداد (bsrn)") String bsrn,
        @Schema(description = "شماره موضوع (sovat)") String sovat,
        @Schema(description = "شماره گذرنامه (bpn)") String bpn,
        @Schema(description = "شناسه الحاقیه (an)") String an
    ) {}
    
    // ========== DTO های بارنامه و حمل ==========
    @Schema(description = "DTO اطلاعات بارنامه و حمل")
    public record ElectronicInvoiceShippingDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "شماره بارنامه (lno)") String lno,
        @Schema(description = "شماره بارنامه مرجع (lrno)") String lrno,
        @Schema(description = "کشور مبدا (ocu)") String ocu,
        @Schema(description = "شهر مبدا (oci)") String oci,
        @Schema(description = "کشور مقصد (dco)") String dco,
        @Schema(description = "شهر مقصد (dci)") String dci,
        @Schema(description = "شناسه ملی فرستنده (tid)") String tid,
        @Schema(description = "شناسه ملی گیرنده (rid)") String rid,
        @Schema(description = "نوع بارنامه/نوع حمل (lt)") Integer lt,
        @Schema(description = "شماره ناوگان (cno)") String cno,
        @Schema(description = "شماره ملی/کد فراگیر راننده (did)") String did,
        @Schema(description = "کالاهای حمل شده (sg)") String sg,
        @Schema(description = "شناسه کالای حمل شده (sgid)") String sgid,
        @Schema(description = "شرح کالای حمل شده (sgt)") String sgt,
        @Schema(description = "شماره کوتاژ (cdcn)") String cdcn,
        @Schema(description = "تاریخ کوتاژ (cdcd)") String cdcd,
        @Schema(description = "شماره کانتینر (cnid)") String cnid,
        @Schema(description = "مجموع وزن پردازش شده (tpwd)") BigDecimal tpwd,
        @Schema(description = "مجموع وزن خالص (tonw)") BigDecimal tonw,
        @Schema(description = "مجموع ارزش ریالی (torv)") BigDecimal torv,
        @Schema(description = "مجموع ارزش ارزی (tocv)") BigDecimal tocv
    ) {}
    
    // ========== DTO های اطلاعات سیستمی صورتحساب ==========
    @Schema(description = "DTO اطلاعات سیستمی صورتحساب الکترونیکی")
    public record ElectronicInvoiceSystemDto(
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "وضعیت (sts) - DRAFT, SUBMITTED, ACCEPTED, REJECTED, CANCELLED") String sts,
        @Schema(description = "تاریخ و زمان ارسال (srvdt)") OffsetDateTime srvdt,
        @Schema(description = "شماره مرجع سامانه مودیان (referenceNumber)") String referenceNumber,
        @Schema(description = "کد رد (rejcode)") String rejcode,
        @Schema(description = "توضیحات رد (rejdesc)") String rejdesc,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn,
        @Schema(description = "تاریخ به‌روزرسانی") OffsetDateTime updatedOn
    ) {}
    
    @Schema(description = "درخواست به‌روزرسانی اطلاعات سیستمی")
    public record UpdateElectronicInvoiceSystemRequest(
        @Schema(description = "وضعیت (sts)") String sts,
        @Schema(description = "تاریخ و زمان ارسال (srvdt)") OffsetDateTime srvdt,
        @Schema(description = "کد رد (rejcode)") String rejcode,
        @Schema(description = "توضیحات رد (rejdesc)") String rejdesc
    ) {}
    
    // ========== DTO های اطلاعات سیستمی و سربرگ ==========
    @Schema(description = "DTO اطلاعات سیستمی و سربرگ صورتحساب")
    public record ElectronicInvoiceSystemWithHeaderDto(
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "وضعیت ارسال") String sts,
        @Schema(description = "تاریخ و زمان ارسال") OffsetDateTime srvdt,
        @Schema(description = "کد رد") String rejcode,
        @Schema(description = "توضیحات رد") String rejdesc,
        @Schema(description = "شماره مالیاتی") String taxId,
        @Schema(description = "کد الگو") String templateCode,
        @Schema(description = "نوع صورتحساب") Integer inty,
        @Schema(description = "شماره صورتحساب") String inno,
        @Schema(description = "تاریخ و زمان صدور") Long indatim,
        @Schema(description = "شماره اقتصادی فروشنده") String tins,
        @Schema(description = "شماره اقتصادی خریدار") String tinb,
        @Schema(description = "مبلغ کل") java.math.BigDecimal tbill,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn
    ) {}
    
    // ========== DTO های فیلدهای تکمیلی ==========
    @Schema(description = "DTO فیلدهای تکمیلی صورتحساب الکترونیکی")
    public record ElectronicInvoiceExtensionDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه سربرگ") UUID headerId,
        @Schema(description = "کلید فیلد تکمیلی") String extensionKey,
        @Schema(description = "مقدار فیلد تکمیلی") String extensionValue,
        @Schema(description = "نوع داده (STRING, NUMBER, DATE, JSON, XML, BOOLEAN)") String extensionType,
        @Schema(description = "تاریخ ایجاد") OffsetDateTime createdOn
    ) {}
    
    @Schema(description = "درخواست ایجاد فیلد تکمیلی")
    public record CreateElectronicInvoiceExtensionRequest(
        @Schema(description = "شناسه سربرگ", requiredMode = Schema.RequiredMode.REQUIRED) UUID headerId,
        @Schema(description = "کلید فیلد تکمیلی", requiredMode = Schema.RequiredMode.REQUIRED) String extensionKey,
        @Schema(description = "مقدار فیلد تکمیلی", requiredMode = Schema.RequiredMode.REQUIRED) String extensionValue,
        @Schema(description = "نوع داده (STRING, NUMBER, DATE, JSON, XML, BOOLEAN)", requiredMode = Schema.RequiredMode.REQUIRED) String extensionType
    ) {}
}

