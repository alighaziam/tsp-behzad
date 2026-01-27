package ir.gov.tax.tpis.sdk.transfer.config;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;

public final class DtoValidator {

    private DtoValidator() {}

    public static void validatePacket(PacketDto packetDto) {
        if (packetDto == null) {
            throw new TaxApiException("packet is null");
        }
        if (packetDto.getUid() == null || packetDto.getUid().isEmpty()) {
            throw new TaxApiException("packet uid is empty");
        }
        if (packetDto.getPacketType() == null || packetDto.getPacketType().isEmpty()) {
            throw new TaxApiException("packet type is empty");
        }
        if (packetDto.getFiscalId() == null || packetDto.getFiscalId().isEmpty()) {
            throw new TaxApiException("packet fiscalId is empty");
        }
    }
}

