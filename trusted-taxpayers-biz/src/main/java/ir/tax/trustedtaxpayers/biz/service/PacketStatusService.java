package ir.tax.trustedtaxpayers.biz.service;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketResponse;
import ir.gov.tax.tpis.sdk.transfer.dto.SyncResponseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing packet transmission status
 * سرویس مدیریت وضعیت ارسال Packetها
 * 
 * این سرویس وضعیت ارسال Packetها را مدیریت می‌کند
 * و می‌تواند برای استعلام وضعیت و retry استفاده شود.
 */
@Service
public class PacketStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(PacketStatusService.class);
    
    // این می‌تواند به یک Repository یا Cache متصل شود
    // در حال حاضر از Memory استفاده می‌کنیم
    private final Map<String, PacketStatus> packetStatusCache = new HashMap<>();
    
    /**
     * ثبت وضعیت Packet پس از ارسال
     */
    @Transactional
    public void recordPacketStatus(String uid, SyncResponseModel response) {
        PacketStatus status = new PacketStatus();
        status.setUid(uid);
        status.setSentAt(OffsetDateTime.now());
        status.setStatus(response != null && response.getResult() != null ? "SUCCESS" : "FAILED");
        
        if (response != null) {
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                status.setErrors(response.getErrors().stream()
                    .map(e -> e.getCode() + ": " + e.getMessage())
                    .toList());
            }
            if (response.getResult() != null) {
                status.setReferenceNumber(response.getResult().getUid());
            }
        }
        
        packetStatusCache.put(uid, status);
        logger.info("Recorded packet status for UID: {}", uid);
    }
    
    /**
     * ثبت وضعیت Packetهای Async
     */
    @Transactional
    public void recordPacketStatuses(List<PacketResponse> responses) {
        for (PacketResponse response : responses) {
            if (response.getUid() != null) {
                PacketStatus status = new PacketStatus();
                status.setUid(response.getUid());
                status.setSentAt(OffsetDateTime.now());
                status.setReferenceNumber(response.getReferenceNumber());
                
                if (response.getErrorCode() != null && !response.getErrorCode().isBlank()) {
                    status.setStatus("FAILED");
                    status.setErrors(List.of(response.getErrorCode() + ": " + response.getErrorDetail()));
                } else {
                    status.setStatus("SUCCESS");
                }
                
                packetStatusCache.put(response.getUid(), status);
                logger.info("Recorded packet status for UID: {}", response.getUid());
            }
        }
    }
    
    /**
     * استعلام وضعیت Packet با UID
     */
    @Transactional(readOnly = true)
    public PacketStatus getPacketStatus(String uid) {
        PacketStatus status = packetStatusCache.get(uid);
        if (status == null) {
            logger.warn("Packet status not found for UID: {}", uid);
            return PacketStatus.notFound(uid);
        }
        return status;
    }
    
    /**
     * دریافت تمام Packetهای FAILED
     */
    @Transactional(readOnly = true)
    public List<PacketStatus> getFailedPackets() {
        return packetStatusCache.values().stream()
            .filter(s -> "FAILED".equals(s.getStatus()))
            .toList();
    }
    
    /**
     * دریافت تمام Packetهای SUCCESS
     */
    @Transactional(readOnly = true)
    public List<PacketStatus> getSuccessfulPackets() {
        return packetStatusCache.values().stream()
            .filter(s -> "SUCCESS".equals(s.getStatus()))
            .toList();
    }
    
    /**
     * کلاس داخلی برای نگهداری وضعیت Packet
     */
    public static class PacketStatus {
        private String uid;
        private String referenceNumber;
        private String status;
        private OffsetDateTime sentAt;
        private List<String> errors;
        
        public String getUid() {
            return uid;
        }
        
        public void setUid(String uid) {
            this.uid = uid;
        }
        
        public String getReferenceNumber() {
            return referenceNumber;
        }
        
        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public OffsetDateTime getSentAt() {
            return sentAt;
        }
        
        public void setSentAt(OffsetDateTime sentAt) {
            this.sentAt = sentAt;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
        
        public boolean isSuccessful() {
            return "SUCCESS".equals(status);
        }
        
        public static PacketStatus notFound(String uid) {
            PacketStatus status = new PacketStatus();
            status.setUid(uid);
            status.setStatus("NOT_FOUND");
            return status;
        }
    }
}

