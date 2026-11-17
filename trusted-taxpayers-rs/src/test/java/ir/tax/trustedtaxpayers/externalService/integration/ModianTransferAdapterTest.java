package ir.tax.trustedtaxpayers.externalService.integration;

import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.dto.ResponsePacketModel;
import ir.gov.tax.tpis.sdk.transfer.dto.SyncResponseModel;
import ir.tax.trustedtaxpayers.application.util.PacketConverter;
import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import ir.tax.trustedtaxpayers.externalService.service.ModianTransferService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModianTransferAdapterTest {

    private ModianTransferService modianTransferService;
    private PacketConverter packetConverter;
    private ModianTransferAdapter adapter;
    private ElectronicInvoiceHeaderEntity invoice;

    @BeforeEach
    void setUp() {
        modianTransferService = mock(ModianTransferService.class);
        packetConverter = mock(PacketConverter.class);
        adapter = new ModianTransferAdapter(modianTransferService, packetConverter);
        invoice = mock(ElectronicInvoiceHeaderEntity.class);
        when(invoice.getTaxId()).thenReturn("invoice-tax-id");
        when(invoice.getTins()).thenReturn("seller-fiscal-id");
    }

    @Test
    void mapsConfirmedInquiryToAcceptedWithoutResubmission() {
        InquiryResultModel existing = inquiry("CONFIRM");
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id")).thenReturn(existing);

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("ACCEPTED", response.getStatus());
        verify(modianTransferService, never()).sendPacket(any(PacketDto.class), anyBoolean(), anyBoolean());
        verifyNoInteractions(packetConverter);
    }

    @Test
    void mapsSystemConfirmedInquiryToAcceptedWithoutResubmission() {
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id"))
                .thenReturn(inquiry("SYSTEM_CONFIRM"));

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("ACCEPTED", response.getStatus());
        verify(modianTransferService, never()).sendPacket(any(PacketDto.class), anyBoolean(), anyBoolean());
    }

    @Test
    void mapsRejectedInquiryToRejectedWithoutResubmission() {
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id"))
                .thenReturn(inquiry("REJECT"));

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("REJECTED", response.getStatus());
        verify(modianTransferService, never()).sendPacket(any(PacketDto.class), anyBoolean(), anyBoolean());
    }

    @Test
    void keepsPendingInquirySubmittedWithoutResubmission() {
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id"))
                .thenReturn(inquiry("PENDING"));

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("SUBMITTED", response.getStatus());
        verify(modianTransferService, never()).sendPacket(any(PacketDto.class), anyBoolean(), anyBoolean());
    }

    @Test
    void failsClosedWhenInquiryIsUnavailable() {
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id"))
                .thenThrow(new IllegalStateException("inquiry unavailable"));

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("FAILED", response.getStatus());
        verify(modianTransferService, never()).sendPacket(any(PacketDto.class), anyBoolean(), anyBoolean());
        verifyNoInteractions(packetConverter);
    }

    @Test
    void sendsPacketWithPersistedUidOnlyWhenInquiryFindsNoExistingPacket() {
        when(modianTransferService.inquiryInvoiceByUid("stable-uid", "seller-fiscal-id")).thenReturn(null);
        PacketDto packet = new PacketDto();
        when(packetConverter.convertInvoiceToPacket(invoice, "INVOICE", "stable-uid")).thenReturn(packet);
        SyncResponseModel gatewayResponse = new SyncResponseModel();
        gatewayResponse.setResult(new ResponsePacketModel());
        when(modianTransferService.sendPacket(packet, true, true)).thenReturn(gatewayResponse);

        InvoiceTransmissionService.TransmissionResponse response = adapter.submitInvoice(invoice, "stable-uid");

        assertEquals("SUBMITTED", response.getStatus());
        verify(packetConverter).convertInvoiceToPacket(invoice, "INVOICE", "stable-uid");
        verify(modianTransferService).sendPacket(packet, true, true);
    }

    private InquiryResultModel inquiry(String status) {
        InquiryResultModel result = new InquiryResultModel();
        result.setUid("stable-uid");
        result.setStatus(status);
        return result;
    }
}
