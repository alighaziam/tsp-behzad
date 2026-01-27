package ir.tax.trustedtaxpayers.externalService.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.gov.tax.tpis.sdk.transfer.dto.AsyncResponseModel;
import ir.gov.tax.tpis.sdk.transfer.dto.ErrorModel;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import ir.gov.tax.tpis.sdk.transfer.dto.PacketResponse;
import ir.gov.tax.tpis.sdk.transfer.dto.ResponsePacketModel;
import ir.gov.tax.tpis.sdk.transfer.dto.SyncResponseModel;
import ir.tax.trustedtaxpayers.externalService.service.ModianTransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller برای ارسال داده به سامانه مودیان
 * REST Controller for Modian System Transfer
 * 
 * این Controller API endpoints برای ارسال Packetها به سامانه مودیان را ارائه می‌دهد.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/modian")
@Tag(name = "Modian Transfer", description = "API برای ارسال داده به سامانه مودیان - Modian System Transfer APIs")
public class ModianTransferController {

    private final ModianTransferService modianTransferService;

    public ModianTransferController(ModianTransferService modianTransferService) {
        this.modianTransferService = modianTransferService;
    }

    @GetMapping
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    /**
     * ارسال یک Packet به صورت Synchronous
     */
    @PostMapping("/packets/sync")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(
        summary = "ارسال Packet به صورت Synchronous",
        description = "Send a single packet synchronously to Modian system"
    )
    public ResponseEntity<SyncTransferResponse> sendPacketSync(
            @Valid @RequestBody PacketRequest request) {
        log.info("Received sync packet request with UID: {}, type: {}", 
                request.getUid(), request.getPacketType());
        
        try {
            PacketDto packetDto = createPacketDto(request);
            SyncResponseModel response = modianTransferService.sendPacket(
                    packetDto, 
                    request.isEncrypt(), 
                    request.isSign()
            );
            
            SyncTransferResponse responseDto = SyncTransferResponse.builder()
                    .success(response != null && response.getResult() != null)
                    .timestamp(response != null ? response.getTimestamp() : null)
                    .result(response != null ? response.getResult() : null)
                    .errors(response != null ? response.getErrors() : null)
                    .message(response != null && response.getResult() != null 
                            ? "Packet sent successfully" 
                            : "Failed to send packet")
                    .build();
            
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending sync packet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SyncTransferResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * ارسال چندین Packet به صورت Async
     */
    @PostMapping("/packets/async")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(
        summary = "ارسال Packetها به صورت Async",
        description = "Send multiple packets asynchronously to Modian system"
    )
    public ResponseEntity<AsyncTransferResponse> sendPacketsAsync(
            @Valid @RequestBody PacketsRequest request) {
        log.info("Received async packets request with {} packets", request.getPackets().size());
        
        try {
            List<PacketDto> packetDtos = request.getPackets().stream()
                    .map(this::createPacketDto)
                    .toList();
            
            AsyncResponseModel response = modianTransferService.sendPackets(
                    packetDtos,
                    request.isEncrypt(),
                    request.isSign()
            );
            
            AsyncTransferResponse responseDto = AsyncTransferResponse.builder()
                    .success(response != null)
                    .timestamp(response != null ? response.getTimestamp() : null)
                    .results(response != null ? response.getResult() : null)
                    .errors(response != null ? response.getErrors() : null)
                    .message(response != null ? "Packets sent successfully" : "Failed to send packets")
                    .build();
            
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error sending async packets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AsyncTransferResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build());
        }
    }

    /**
     * ساخت PacketDto از Request
     */
    private PacketDto createPacketDto(PacketRequest request) {
        PacketDto packet = new PacketDto();
        packet.setUid(request.getUid() != null ? request.getUid() : UUID.randomUUID().toString());
        packet.setPacketType(request.getPacketType());
        packet.setFiscalId(request.getFiscalId());
        packet.setData(request.getData());
        packet.setRetry(request.getRetry());
        return packet;
    }

    // DTO Classes

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacketRequest {
        @NotBlank(message = "Packet type is required")
        private String packetType;
        
        private String uid;
        private String fiscalId;
        private Object data;
        private Boolean retry = false;
        private boolean encrypt = true;
        private boolean sign = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PacketsRequest {
        @NotEmpty(message = "Packets list cannot be empty")
        private List<PacketRequest> packets;
        
        private boolean encrypt = true;
        private boolean sign = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncTransferResponse {
        private boolean success;
        private Long timestamp;
        private ResponsePacketModel result;
        private List<ErrorModel> errors;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsyncTransferResponse {
        private boolean success;
        private Long timestamp;
        private List<PacketResponse> results;
        private List<ErrorModel> errors;
        private String message;
    }
}

