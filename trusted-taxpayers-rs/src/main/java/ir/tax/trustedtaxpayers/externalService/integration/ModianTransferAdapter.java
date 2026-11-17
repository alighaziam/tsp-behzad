package ir.tax.trustedtaxpayers.externalService.integration;

import ir.gov.tax.tpis.sdk.content.enumeration.InvoiceStatus;
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
        return submitInvoice(invoice, java.util.UUID.randomUUID().toString());
    }

    @Override
    public InvoiceTransmissionService.TransmissionResponse submitInvoice(
            ElectronicInvoiceHeaderEntity invoice, String uid) {
        logger.info("Submitting invoice {} via Modian Transfer Service with stable UID", invoice.getTaxId());

        try {
            ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel existing =
                    modianTransferService.inquiryInvoiceByUid(uid, invoice.getTins());
            if (existing != null) {
                String status = existing.getStatus();
                if (InvoiceStatus.REJECT.name().equalsIgnoreCase(status)) {
                    return InvoiceTransmissionService.TransmissionResponse.failure(
                            "MODIAN_REJECTED", "The tax system already recorded this invoice as rejected");
                }
                if (InvoiceStatus.CONFIRM.name().equalsIgnoreCase(status)
                        || InvoiceStatus.SYSTEM_CONFIRM.name().equalsIgnoreCase(status)) {
                    return InvoiceTransmissionService.TransmissionResponse.success(
                            "ACCEPTED", "Invoice was already accepted by the tax system");
                }
                // PENDING and unknown statuses are treated as already submitted.
                // Never send a second packet while the first UID exists remotely.
                return InvoiceTransmissionService.TransmissionResponse.success(
                        "SUBMITTED", "Invoice UID already exists; duplicate submission was prevented");
            }

            PacketDto packet = packetConverter.convertInvoiceToPacket(invoice, "INVOICE", uid);
            SyncResponseModel response = modianTransferService.sendPacket(packet, true, true);

            if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                String errorCode = response.getErrors().get(0).getCode();
                String errorMessage = response.getErrors().get(0).getMessage();
                return InvoiceTransmissionService.TransmissionResponse.failure(
                        errorCode != null ? errorCode : "MODIAN_ERROR",
                        errorMessage != null ? errorMessage : "Unknown error from Modian system");
            }
            if (response != null && response.getResult() != null) {
                return InvoiceTransmissionService.TransmissionResponse.success(
                        "SUBMITTED", "Invoice submission was accepted by the transfer gateway");
            }
            return InvoiceTransmissionService.TransmissionResponse.retryableFailure(
                    "INVALID_RESPONSE", "No definitive response received from Modian system");

        } catch (Exception e) {
            logger.error("Failed to submit or inquire invoice {} via Modian Transfer Service", invoice.getTaxId(), e);
            return InvoiceTransmissionService.TransmissionResponse.retryableFailure(
                    "TRANSMISSION_ERROR",
                    "Submission outcome is unknown; retry will inquire using the same UID");
        }
    }    
}

