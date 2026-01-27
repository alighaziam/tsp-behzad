package ir.tax.trustedtaxpayers.biz.impl;

import ir.gov.tax.tpis.sdk.content.api.TaxApi;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import ir.gov.tax.tpis.sdk.utils.TaxUtils;
import ir.tax.trustedtaxpayers.application.exception.BusinessException;
import ir.tax.trustedtaxpayers.application.exception.ErrorCode;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDtoWrapper;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceHeaderDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.PaymentDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.UidAndFiscalId;
import ir.tax.trustedtaxpayers.biz.TaxInvoiceManager;
import ir.tax.trustedtaxpayers.config.TaxSdkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * پیاده‌سازی مدیریت صورت‌حساب‌های مالیاتی
 * Tax Invoice Management Implementation
 */
@Slf4j
@Service("taxInvoiceManager")
@RequiredArgsConstructor
public class TaxInvoiceManagerImpl extends GenericImpl implements TaxInvoiceManager {

    private final TaxApi taxApi;
    private final TaxSdkProperties taxSdkProperties;

    @Override
    public AsyncResponseModel sendInvoices(List<ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto> invoices) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            // TODO: API call to tax system
            // Ensure SDK encrypter and token are initialized
            // taxApi.getServerInformation();
            // taxApi.requestToken();

            log.info("Sending {} invoices to tax system", invoices.size());

            // TODO: Send invoices to tax API
            // AsyncResponseModel response = taxApi.sendMyInvoices(invoices);

            // if (response == null) {
            //     throw new BusinessException(
            //             ErrorCode.EXTERNAL_SERVICE_ERROR,
            //             "Tax API returned null response while sending invoices"
            //     );
            // }

            // log.info("Invoices sent successfully. Response: {}", response);
            // return response;

            // Temporary mock response for development
            AsyncResponseModel mockResponse = new AsyncResponseModel();
            mockResponse.setTimestamp(System.currentTimeMillis());
            mockResponse.setResult(java.util.Collections.emptyList());
            mockResponse.setErrors(java.util.Collections.emptyList());
            log.info("Invoices processed (API call is TODO). Count: {}", invoices.size());
            return mockResponse;

        } catch (Exception e) {
            log.error("Error processing invoices", e);
            throw new BusinessException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Failed to process invoices: " + e.getMessage(),
                    e
            );
        }
    }


    @Override
    public AsyncResponseModel sendInvoicesWithWrapper(List<InvoiceDtoWrapper> invoiceWrappers) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            // Ensure SDK encrypter and token are initialized
            taxApi.getServerInformation();
            taxApi.requestToken();
            log.info("Sending {} invoice wrappers to tax system", invoiceWrappers.size());
            AsyncResponseModel response = taxApi.sendInvoices(invoiceWrappers);
            log.info("Invoice wrappers sent successfully. Response: {}", response);
            return response;
        } catch (TaxApiException e) {
            log.error("Error sending invoice wrappers to tax system", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to send invoice wrappers: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> inquiryByUidAndFiscalId(List<UidAndFiscalId> uidAndFiscalIds) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Inquiring invoices by UID and Fiscal ID, count: {}", uidAndFiscalIds.size());
            List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> results = taxApi.inquiryByUidAndFiscalId(uidAndFiscalIds);
            log.info("Inquiry by UID completed, results count: {}", results.size());
            return results;
        } catch (TaxApiException e) {
            log.error("Error inquiring invoices by UID", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to inquiry invoices: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> inquiryByReferenceNumber(List<String> referenceNumbers) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Inquiring invoices by reference numbers, count: {}", referenceNumbers.size());
            List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> results = taxApi.inquiryByReferenceId(referenceNumbers);
            log.info("Inquiry by reference number completed, results count: {}", results.size());
            return results;
        } catch (TaxApiException e) {
            log.error("Error inquiring invoices by reference number", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to inquiry invoices: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> inquiryByDate(String persianDate) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Inquiring invoices by date: {}", persianDate);
            List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> results = taxApi.inquiryByTime(persianDate);
            log.info("Inquiry by date completed, results count: {}", results.size());
            return results;
        } catch (TaxApiException e) {
            log.error("Error inquiring invoices by date", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to inquiry invoices: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> inquiryByDateRange(String startDate, String endDate) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Inquiring invoices by date range: {} to {}", startDate, endDate);
            List<ir.tax.trustedtaxpayers.beans.dto.tax.InquiryResultModel> results = taxApi.inquiryByTimeRange(startDate, endDate);
            log.info("Inquiry by date range completed, results count: {}", results.size());
            return results;
        } catch (TaxApiException e) {
            log.error("Error inquiring invoices by date range", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to inquiry invoices: " + e.getMessage(), e);
        }
    }

    @Override
    public AsyncResponseModel sendSampleInvoice() {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            // Ensure SDK encrypter and token are initialized
            taxApi.getServerInformation();
            taxApi.requestToken();
            log.info("Creating and sending sample invoice");
            
            // ساخت صورت‌حساب نمونه
            Random random = new Random();
            long randomSerialDecimal = random.nextInt(999999999);
            Instant invoiceCreatedDate = Instant.now();
            String taxId = TaxUtils.generateTaxId(
                    taxSdkProperties.getMemoryId(), 
                    randomSerialDecimal, 
                    invoiceCreatedDate
            );

            // هدر صورت‌حساب
            InvoiceHeaderDto header = new ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceHeaderDto();
            header.setIndatim(invoiceCreatedDate.toEpochMilli());
            header.setIndati2m(invoiceCreatedDate.toEpochMilli());
            header.setTaxid(taxId);
            header.setInty(1); // نوع صورت‌حساب: فروش
            header.setInp(13); // الگوی صورت‌حساب
            header.setIns(1); // وضعیت: عادی
            header.setTins("57490845111"); // شناسه اقتصادی فروشنده (نمونه)
            header.setTprdis(BigDecimal.valueOf(10_000)); // مجموع قبل از تخفیف
            header.setTdis(BigDecimal.valueOf(500)); // تخفیف
            header.setTvam(BigDecimal.valueOf(855)); // مالیات بر ارزش افزوده
            header.setTodam(BigDecimal.valueOf(1900)); // سایر مالیات‌ها
            header.setTbill(BigDecimal.valueOf(12255)); // جمع کل
            header.setCap(BigDecimal.valueOf(9500)); // قابل پرداخت
            header.setSetm(1); // نوع تسویه
            header.setInsp(BigDecimal.valueOf(0));
            header.setTax17(BigDecimal.valueOf(500));
            header.setTob(2); // نوع خریدار
            header.setTinb("10100302746"); // شناسه اقتصادی خریدار (نمونه)
            header.setTadis(BigDecimal.valueOf(9500));

            // بدنه صورت‌حساب (اقلام)
            ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceBodyDto body = new ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceBodyDto();
            body.setSstid("2902230386252"); // شناسه کالا/خدمت
            body.setSstt("شیر کم چرب پاستوریزه"); // شرح کالا/خدمت
            body.setMu("1689"); // واحد اندازه‌گیری
            body.setAm(BigDecimal.valueOf(1)); // تعداد
            body.setFee(BigDecimal.valueOf(10_000)); // مبلغ واحد
            body.setPrdis(BigDecimal.valueOf(10_000));
            body.setTdis(BigDecimal.valueOf(500)); // تخفیف
            body.setAdis(BigDecimal.valueOf(9_500)); // مبلغ بعد از تخفیف
            body.setVra(BigDecimal.valueOf(9)); // نرخ مالیات
            body.setVam(BigDecimal.valueOf(855)); // مبلغ مالیات
            body.setTsstam(BigDecimal.valueOf(12_255));
            body.setOdam(BigDecimal.valueOf(950));
            body.setOlam(BigDecimal.valueOf(950));

            // اطلاعات پرداخت
            ir.tax.trustedtaxpayers.beans.dto.tax.PaymentDto payment = new PaymentDto();
            payment.setIinn("113124421");
            payment.setAcn("12345678952145");
            payment.setTrmn("31312442");
            payment.setTrn("4131244214");
            payment.setPdt(System.currentTimeMillis());
            // Set payment value equal to total bill for sample (pv)
            payment.setPv(header.getTbill());

            // ترکیب صورت‌حساب
            ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto invoiceDto = new ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto();
            invoiceDto.setBody(Collections.singletonList(body));
            invoiceDto.setHeader(header);
            invoiceDto.setPayments(Collections.singletonList(payment));

            // ارسال
            AsyncResponseModel response = taxApi.sendMyInvoices(Collections.singletonList(invoiceDto));
            log.info("Sample invoice sent successfully. TaxID: {}, Response: {}", taxId, response);
            
            return response;
        } catch (TaxApiException e) {
            log.error("Error sending sample invoice", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to send sample invoice: " + e.getMessage(), e);
        }
    }
}


