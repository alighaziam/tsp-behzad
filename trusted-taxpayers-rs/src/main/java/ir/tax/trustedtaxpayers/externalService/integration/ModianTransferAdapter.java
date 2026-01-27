package ir.tax.trustedtaxpayers.externalService.integration;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.dto.SyncResponseModel;
import ir.tax.trustedtaxpayers.application.util.PacketConverter;
import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import ir.tax.trustedtaxpayers.externalService.service.ModianTransferService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Adapter for connecting ModianTransferService to InvoiceTransmissionService
 * آداپتور برای اتصال سرویس انتقال مودیان به سرویس انتقال صورتحساب
 * 
 * این کلاس InvoiceTransmissionService.InvoiceSubmissionDelegate را پیاده‌سازی می‌کند
 * و از ModianTransferService برای ارسال صورتحساب‌ها به سامانه مودیان استفاده می‌کند.
 */
@Component
@Primary
public class ModianTransferAdapter implements InvoiceTransmissionService.InvoiceSubmissionDelegate {
    
    private static final Logger logger = LoggerFactory.getLogger(ModianTransferAdapter.class);
    
    private final ModianTransferService modianTransferService;
    private final PacketConverter packetConverter;
    
    public ModianTransferAdapter(ModianTransferService modianTransferService, PacketConverter packetConverter) {
        this.modianTransferService = modianTransferService;
        this.packetConverter = packetConverter;
    }
    
    @Override
    public InvoiceTransmissionService.TransmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity invoice) {
        logger.info("Submitting invoice {} via Modian Transfer Service", invoice.getTaxId());
        
        try {
            // تبدیل Invoice به PacketDto با استفاده از PacketConverter
            PacketDto packet = packetConverter.convertInvoiceToPacket(invoice, "INVOICE");
            
            // ارسال Packet به سامانه مودیان
            SyncResponseModel response = modianTransferService.sendPacket(packet, true, true);
            
            // تبدیل پاسخ SDK به TransmissionResponse
            if (response != null && response.getResult() != null) {
                // موفقیت
                return InvoiceTransmissionService.TransmissionResponse.success(
                    "ACCEPTED",
                    "Invoice sent successfully to Modian system"
                );
            } else if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                // خطا
                String errorCode = response.getErrors().get(0).getCode();
                String errorMessage = response.getErrors().get(0).getMessage();
                return InvoiceTransmissionService.TransmissionResponse.failure(
                    errorCode != null ? errorCode : "MODIAN_ERROR",
                    errorMessage != null ? errorMessage : "Unknown error from Modian system"
                );
            } else {
                // پاسخ نامعتبر
                return InvoiceTransmissionService.TransmissionResponse.failure(
                    "INVALID_RESPONSE",
                    "Received invalid response from Modian system"
                );
            }
            
        } catch (Exception e) {
            logger.error("Failed to submit invoice {} via Modian Transfer Service", invoice.getTaxId(), e);
            return InvoiceTransmissionService.TransmissionResponse.failure(
                "TRANSMISSION_ERROR",
                "Failed to send invoice to Modian system: " + e.getMessage()
            );
        }
    }
    
}

