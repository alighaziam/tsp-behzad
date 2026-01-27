package ir.tax.trustedtaxpayers.biz;

import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDtoWrapper;
import ir.tax.trustedtaxpayers.beans.dto.tax.UidAndFiscalId;

import java.util.List;

/**
 * مدیریت صورت‌حساب‌های مالیاتی
 * Tax Invoice Management
 * 
 * مشابه Manager ها در samt-git
 */
public interface TaxInvoiceManager extends GenericBiz {

    /**
     * ارسال صورت‌حساب‌های مالیاتی
     * Send tax invoices
     * 
     * @param invoices لیست صورت‌حساب‌ها
     * @return پاسخ غیرهمزمان شامل UID و شماره مرجع
     */
    AsyncResponseModel sendInvoices(List<InvoiceDto> invoices);

    /**
     * ارسال صورت‌حساب‌های مالیاتی با wrapper (برای TSP)
     * Send tax invoices with wrapper (for TSP)
     * 
     * @param invoiceWrappers لیست wrapper های صورت‌حساب
     * @return پاسخ غیرهمزمان شامل UID و شماره مرجع
     */
    AsyncResponseModel sendInvoicesWithWrapper(List<InvoiceDtoWrapper> invoiceWrappers);

    /**
     * استعلام وضعیت صورت‌حساب با UID
     * Inquiry invoice status by UID
     * 
     * @param uidAndFiscalIds لیست UID و شناسه مالیاتی
     * @return نتایج استعلام
     */
    List<InquiryResultModel> inquiryByUidAndFiscalId(List<UidAndFiscalId> uidAndFiscalIds);

    /**
     * استعلام وضعیت صورت‌حساب با شماره مرجع
     * Inquiry invoice status by reference number
     * 
     * @param referenceNumbers لیست شماره‌های مرجع
     * @return نتایج استعلام
     */
    List<InquiryResultModel> inquiryByReferenceNumber(List<String> referenceNumbers);

    /**
     * استعلام صورت‌حساب‌های یک تاریخ خاص
     * Inquiry invoices by specific date
     * 
     * @param persianDate تاریخ شمسی (مثال: 14030101)
     * @return نتایج استعلام
     */
    List<InquiryResultModel> inquiryByDate(String persianDate);

    /**
     * استعلام صورت‌حساب‌های یک بازه زمانی
     * Inquiry invoices by date range
     * 
     * @param startDate تاریخ شروع (شمسی)
     * @param endDate تاریخ پایان (شمسی)
     * @return نتایج استعلام
     */
    List<InquiryResultModel> inquiryByDateRange(String startDate, String endDate);

    /**
     * ساخت و ارسال یک صورت‌حساب نمونه
     * Create and send a sample invoice
     * 
     * @return پاسخ غیرهمزمان
     */
    AsyncResponseModel sendSampleInvoice();
}


