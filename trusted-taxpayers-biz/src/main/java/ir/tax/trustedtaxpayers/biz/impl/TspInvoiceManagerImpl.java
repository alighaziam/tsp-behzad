package ir.tax.trustedtaxpayers.biz.impl;

import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.TransferConstants;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import ir.gov.tax.tpis.sdk.transfer.impl.normalize.ObjectNormalizer;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.SignatoryFactory;
import ir.gov.tax.tpis.sdk.transfer.interfaces.Signatory;
import ir.tax.trustedtaxpayers.application.exception.BusinessException;
import ir.tax.trustedtaxpayers.application.exception.ErrorCode;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDtoWrapper;
import ir.tax.trustedtaxpayers.biz.TspInvoiceManager;
import ir.tax.trustedtaxpayers.config.TaxSdkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * پیاده‌سازی مدیریت ارسال صورت‌حساب برای شرکت‌های معتمد
 * TSP Invoice Management Implementation
 */
@Slf4j
@Service("tspInvoiceManager")
@RequiredArgsConstructor
public class TspInvoiceManagerImpl extends GenericImpl implements TspInvoiceManager {

    private final TransferApi transferApi;
    private final TaxSdkProperties taxSdkProperties;

    @Override
    public AsyncResponseModel sendWithTspKey(List<InvoiceDto> invoices, String fiscalId) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Sending {} invoices with TSP key for fiscal ID: {}", invoices.size(), fiscalId);

            // تبدیل به InvoiceDtoWrapper
            List<InvoiceDtoWrapper> wrappers = invoices.stream()
                    .map(invoice -> {
                        InvoiceDtoWrapper wrapper = new InvoiceDtoWrapper();
                        wrapper.setInvoice(invoice);
                        wrapper.setFiscalId(fiscalId);
                        return wrapper;
                    })
                    .collect(Collectors.toList());

            // ایجاد بسته‌ها
            List<PacketDto> packets = new ArrayList<>();
            for (InvoiceDtoWrapper wrapper : wrappers) {
                PacketDto packet = new PacketDto();
                packet.setUid(UUID.randomUUID().toString());
                packet.setPacketType("INVOICE.V01");
                packet.setFiscalId(wrapper.getFiscalId());
                packet.setData(wrapper.getInvoice());
                packet.setRetry(false);
                packets.add(packet);
            }

            // ارسال با امضا و رمزنگاری
            AsyncResponseModel response = transferApi.sendPackets(packets, null, true, true);
            log.info("Invoices sent successfully with TSP key. Response: {}", response);
            
            return response;
        } catch (TaxApiException e) {
            log.error("Error sending invoices with TSP key", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to send invoices with TSP key: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending invoices with TSP key", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, 
                    "Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public AsyncResponseModel sendWithTaxpayerKey(
            List<InvoiceDto> invoices, 
            String fiscalId,
            String taxpayerPrivateKeyPath,
            String taxpayerPrivateKeyPassword) {
        
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Sending {} invoices with taxpayer key for fiscal ID: {}", invoices.size(), fiscalId);

            // بارگذاری کلید خصوصی مودی
            File privateKeyFile = new File(taxpayerPrivateKeyPath);
            if (!privateKeyFile.exists()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, 
                        "Taxpayer private key file not found: " + taxpayerPrivateKeyPath);
            }

            Signatory taxpayerSignatory = SignatoryFactory.getInstance()
                    .createPKCS8Signatory(privateKeyFile, taxpayerPrivateKeyPassword);

            ObjectNormalizer normalizer = new ObjectNormalizer();

            // ایجاد بسته‌ها با امضای مودی
            List<PacketDto> packets = new ArrayList<>();
            for (InvoiceDto invoice : invoices) {
                // نرمال‌سازی و امضای محتوا توسط مودی
                String normalized = normalizer.normalize(invoice, null);
                String signature = taxpayerSignatory.sign(normalized);

                // ایجاد بسته
                PacketDto packet = new PacketDto();
                packet.setUid(UUID.randomUUID().toString());
                packet.setPacketType("INVOICE.V01");
                packet.setFiscalId(fiscalId);
                packet.setData(invoice);
                packet.setDataSignature(signature);
                packet.setRetry(false);
                
                packets.add(packet);
            }

            // ارسال بسته‌ها توسط TSP (با رمزنگاری اما بدون امضای مجدد)
            AsyncResponseModel response = transferApi.sendPackets(packets, null, true, false);
            log.info("Invoices sent successfully with taxpayer key. Response: {}", response);
            
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending invoices with taxpayer key", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to send invoices with taxpayer key: " + e.getMessage(), e);
        }
    }

    @Override
    public AsyncResponseModel sendPreSignedPackets(List<PacketDto> packets, String token) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Sending {} pre-signed packets", packets.size());

            // تنظیم هدرهای HTTP
            Map<String, String> headers = new HashMap<>();
            if (token != null && !token.isEmpty()) {
                headers.put(TransferConstants.AUTHORIZATION_HEADER, "Bearer " + token);
            }

            // ارسال بسته‌های آماده شده (با رمزنگاری اما بدون امضای مجدد)
            AsyncResponseModel response = transferApi.sendPackets(packets, headers, true, false);
            log.info("Pre-signed packets sent successfully. Response: {}", response);
            
            return response;
        } catch (Exception e) {
            log.error("Error sending pre-signed packets", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to send pre-signed packets: " + e.getMessage(), e);
        }
    }
}

