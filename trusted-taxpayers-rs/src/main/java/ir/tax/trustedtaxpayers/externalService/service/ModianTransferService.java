package ir.tax.trustedtaxpayers.externalService.service;

import ir.gov.tax.tpis.sdk.content.api.TaxApi;
import ir.gov.tax.tpis.sdk.content.dto.InquiryResultModel;
import ir.gov.tax.tpis.sdk.content.dto.UidAndFiscalId;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.dto.*;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for transferring data to Modian System (سامانه مودیان)
 * سرویس برای ارسال داده به سامانه مودیان
 * 
 * این سرویس از TransferApi SDK استفاده می‌کند
 * و مسئولیت ارسال Packetها به سامانه مودیان را دارد.
 */
@Service
public class ModianTransferService {

    private static final Logger logger = LoggerFactory.getLogger(ModianTransferService.class);

    private final TransferApi transferApi;
    private final TaxApi taxApi;

    @Value("${tax.sdk.memory-id:}")
    private String memoryId;

    public ModianTransferService(TransferApi transferApi, TaxApi taxApi) {
        this.transferApi = transferApi;
        this.taxApi = taxApi;
    }

    /**
     * ارسال یک Packet به صورت Synchronous
     * 
     * @param packetDto Packet برای ارسال
     * @param encrypt آیا باید رمزنگاری شود
     * @param sign آیا باید امضا شود
     * @return پاسخ از سامانه
     */
    @Transactional
    public SyncResponseModel sendPacket(PacketDto packetDto, boolean encrypt, boolean sign) {
        logger.info("Sending packet with UID: {}, type: {}", packetDto.getUid(), packetDto.getPacketType());
        
        try {
            Map<String, String> headers = new HashMap<>();
            SyncResponseModel response = transferApi.sendPacket(packetDto, headers, encrypt, sign);
            
            if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
                logger.warn("Packet {} sent but has errors: {}", packetDto.getUid(), response.getErrors());
            } else if (response != null && response.getResult() != null) {
                logger.info("Packet {} sent successfully", packetDto.getUid());
            }
            
            return response;
        } catch (TaxApiException e) {
            logger.error("Failed to send packet {}: {}", packetDto.getUid(), e.getMessage(), e);
            throw new RuntimeException("Failed to send packet to Modian system: " + e.getMessage(), e);
        }
    }

    /**
     * ارسال چندین Packet به صورت Async
     * 
     * @param packets لیست Packetها برای ارسال
     * @param encrypt آیا باید رمزنگاری شود
     * @param sign آیا باید امضا شود
     * @return پاسخ از سامانه
     */
    @Transactional
    public AsyncResponseModel sendPackets(List<PacketDto> packets, boolean encrypt, boolean sign) {
        logger.info("Sending {} packets asynchronously", packets.size());
        
        try {
            Map<String, String> headers = new HashMap<>();
            AsyncResponseModel response = transferApi.sendPackets(packets, headers, encrypt, sign);
            
            if (response != null) {
                int successCount = 0;
                int errorCount = 0;
                
                if (response.getResult() != null) {
                    for (PacketResponse packetResponse : response.getResult()) {
                        if (packetResponse.getErrorCode() == null || packetResponse.getErrorCode().isBlank()) {
                            successCount++;
                        } else {
                            errorCount++;
                        }
                    }
                }
                
                if (response.getErrors() != null) {
                    errorCount += response.getErrors().size();
                }
                
                logger.info("Packets sent: {} successful, {} with errors", successCount, errorCount);
            }
            
            return response;
        } catch (TaxApiException e) {
            logger.error("Failed to send packets: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send packets to Modian system: " + e.getMessage(), e);
        }
    }

    /**
     * ارسال Packet با تنظیمات پیش‌فرض (رمزنگاری و امضا فعال)
     */
    public SyncResponseModel sendPacket(PacketDto packetDto) {
        return sendPacket(packetDto, true, true);
    }

    /**
     * ارسال Packetها با تنظیمات پیش‌فرض (رمزنگاری و امضا فعال)
     */
    public AsyncResponseModel sendPackets(List<PacketDto> packets) {
        return sendPackets(packets, true, true);
    }

    /**
     * ساخت PacketDto از Invoice
     */
    public PacketDto createInvoicePacket(String uid, String fiscalId, Object invoiceData, String packetType) {
        PacketDto packet = new PacketDto();
        packet.setUid(uid);
        packet.setFiscalId(fiscalId);
        packet.setPacketType(packetType);
        packet.setData(invoiceData);
        packet.setRetry(false);
        return packet;
    }

    /**
     * استعلام وضعیت Packet با UID
     * این متد نیاز به پیاده‌سازی در SDK دارد
     */
    /*public ResponsePacketModel inquiryByUid(String uid) {
        logger.info("Inquiring packet status for UID: {}", uid);
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("UID cannot be null or empty");
        }
        if (taxApi == null) {
            throw new IllegalStateException("TaxApi bean is not available to perform inquiry");
        }
        if (memoryId == null || memoryId.isBlank()) {
            throw new IllegalStateException("tax.sdk.memory-id is not configured; cannot perform inquiry by UID");
        }
        try {
            UidAndFiscalId pair = new UidAndFiscalId();
            pair.setUid(uid);
            pair.setFiscalId(memoryId);
            List<UidAndFiscalId> list = singletonList(pair);
            List<InquiryResultModel> results = taxApi.inquiryByUidAndFiscalId(list);
            if (results == null || results.isEmpty()) {
                return null;
            }
            InquiryResultModel first = results.get(0);
            ResponsePacketModel model = new ResponsePacketModel();
            model.setUid(first.getUid());
            model.setPacketType(first.getPacketType());
            model.setData(first.getData());
            return model;
        } catch (TaxApiException e) {
            logger.error("Inquiry by UID failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to inquire packet status: " + e.getMessage(), e);
        }
    }*/

    /**
     * Query the tax authority by the exact persisted packet UID and fiscal ID.
     * A failed inquiry is propagated so callers can fail closed instead of
     * risking a duplicate submission with a new UID.
     */
    public InquiryResultModel inquiryInvoiceByUid(String uid, String fiscalId) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("UID cannot be null or empty");
        }
        if (fiscalId == null || fiscalId.isBlank()) {
            throw new IllegalArgumentException("Fiscal ID cannot be null or empty");
        }
        if (taxApi == null) {
            throw new IllegalStateException("TaxApi bean is not available to perform inquiry");
        }

        try {
            if (taxApi.getToken() == null || taxApi.getToken().getToken() == null
                    || taxApi.getToken().getToken().isBlank()) {
                taxApi.requestToken();
            }
            UidAndFiscalId pair = new UidAndFiscalId();
            pair.setUid(uid);
            pair.setFiscalId(fiscalId);
            List<InquiryResultModel> results =
                    taxApi.inquiryByUidAndFiscalId(java.util.Collections.singletonList(pair));
            if (results == null) {
                return null;
            }
            return results.stream()
                    .filter(result -> result != null && uid.equals(result.getUid()))
                    .findFirst()
                    .orElse(null);
        } catch (TaxApiException e) {
            logger.error("Invoice inquiry failed for UID {}: {}", uid, e.getMessage(), e);
            throw new RuntimeException("Failed to inquire invoice packet: " + e.getMessage(), e);
        }
    }

    /**
     * بررسی پاسخ و استخراج خطاها
     */
    public List<String> extractErrors(SyncResponseModel response) {
        List<String> errors = new ArrayList<>();
        
        if (response == null) {
            errors.add("Response is null");
            return errors;
        }
        
        if (response.getErrors() != null) {
            for (ErrorModel error : response.getErrors()) {
                errors.add(String.format("[%s] %s", error.getCode(), error.getMessage()));
            }
        }
        
        return errors;
    }

    /**
     * بررسی پاسخ Async و استخراج خطاها
     */
    public List<String> extractErrors(AsyncResponseModel response) {
        List<String> errors = new ArrayList<>();
        
        if (response == null) {
            errors.add("Response is null");
            return errors;
        }
        
        if (response.getErrors() != null) {
            for (ErrorModel error : response.getErrors()) {
                errors.add(String.format("[%s] %s", error.getCode(), error.getMessage()));
            }
        }
        
        if (response.getResult() != null) {
            for (PacketResponse packetResponse : response.getResult()) {
                if (packetResponse.getErrorCode() != null && !packetResponse.getErrorCode().isBlank()) {
                    errors.add(String.format("Packet [%s]: [%s] %s", 
                        packetResponse.getUid(), 
                        packetResponse.getErrorCode(),
                        packetResponse.getErrorDetail()));
                }
            }
        }
        
        return errors;
    }
}

