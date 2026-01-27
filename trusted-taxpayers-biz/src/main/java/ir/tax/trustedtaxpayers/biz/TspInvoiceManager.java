package ir.tax.trustedtaxpayers.biz;

import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto;

import java.util.List;

/**
 * مدیریت ارسال صورت‌حساب برای شرکت‌های معتمد (TSP)
 * TSP Invoice Management - Trusted Service Provider
 * 
 * این Manager برای ارسال صورت‌حساب با دو روش:
 * 1. ارسال با کلید شرکت معتمد
 * 2. ارسال با کلید مودی (امضای مودی + ارسال توسط TSP)
 */
public interface TspInvoiceManager extends GenericBiz {

    /**
     * ارسال صورت‌حساب با کلید شرکت معتمد
     * Send invoice with TSP key (both transfer and content signed by TSP)
     * 
     * در این حالت بستهها، چه در الیه انتقال و چه در الیه محتوا توسط کلید شرکت معتمد امضاء میشوند.
     * 
     * @param invoices لیست صورت‌حساب‌ها
     * @param fiscalId شناسه حافظه مالیاتی مودی
     * @return پاسخ غیرهمزمان
     */
    AsyncResponseModel sendWithTspKey(List<InvoiceDto> invoices, String fiscalId);

    /**
     * ارسال صورت‌حساب با کلید مودی
     * Send invoice with taxpayer key (content signed by taxpayer, transfer by TSP)
     * 
     * در این حالت محتوای بسته توسط کلید مودی امضاء و رمز میشود
     * و توسط کلید شرکت معتمد ارسال میشود.
     * 
     * @param invoices لیست صورت‌حساب‌ها
     * @param fiscalId شناسه حافظه مالیاتی مودی
     * @param taxpayerPrivateKeyPath مسیر کلید خصوصی مودی
     * @param taxpayerPrivateKeyPassword رمز کلید خصوصی مودی (اختیاری)
     * @return پاسخ غیرهمزمان
     */
    AsyncResponseModel sendWithTaxpayerKey(
            List<InvoiceDto> invoices, 
            String fiscalId,
            String taxpayerPrivateKeyPath,
            String taxpayerPrivateKeyPassword
    );

    /**
     * ارسال بسته‌های آماده شده توسط مودی
     * Send pre-signed packets from taxpayer
     * 
     * در این حالت بسته‌ها قبلاً توسط مودی آماده و امضا شده‌اند
     * و فقط توسط TSP ارسال می‌شوند.
     * 
     * @param packets لیست بسته‌های آماده شده
     * @param token توکن احراز هویت
     * @return پاسخ غیرهمزمان
     */
    AsyncResponseModel sendPreSignedPackets(List<PacketDto> packets, String token);
}

