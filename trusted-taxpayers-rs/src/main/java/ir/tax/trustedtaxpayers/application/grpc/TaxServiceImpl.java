package ir.tax.trustedtaxpayers.application.grpc;

import io.grpc.stub.StreamObserver;
import ir.tax.trustedtaxpayers.biz.service.InvoiceTransmissionService;
import ir.tax.trustedtaxpayers.biz.service.OutboxService;
import ir.tax.trustedtaxpayers.grpc.*;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceSystemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceSystemRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.OutboxMessageRepository;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class TaxServiceImpl extends TaxServiceGrpc.TaxServiceImplBase {

    private final OutboxService outboxService;
    private final InvoiceTransmissionService transmissionService;
    private final ElectronicInvoiceSystemRepository systemRepository;
    private final ElectronicInvoiceHeaderRepository headerRepository;

    public TaxServiceImpl(OutboxService outboxService, OutboxMessageRepository outboxRepository, InvoiceTransmissionService transmissionService, ElectronicInvoiceSystemRepository systemRepository, ElectronicInvoiceHeaderRepository headerRepository) {
        this.outboxService = outboxService;
        this.transmissionService = transmissionService;
        this.systemRepository = systemRepository;
        this.headerRepository = headerRepository;
    }

    @Override
    @Transactional
    public void submitInvoice(SubmitInvoiceRequest request, StreamObserver<SubmitInvoiceResponse> responseObserver) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoiceId", request.getInvoiceId());
        payload.put("taxpayerId", request.getTaxpayerId());
        payload.put("metadata", request.getMetadataMap());

        Map<String, Object> headers = new HashMap<>();
        headers.put("taxpayerId", request.getTaxpayerId());
        headers.put("idempotencyKey", request.getInvoiceId());

        UUID id = outboxService.enqueue("INVOICE", request.getInvoiceId(), "SUBMIT_INVOICE", payload, headers);

        SubmitInvoiceResponse response = SubmitInvoiceResponse.newBuilder()
                .setSuccess(true)
                .setMessage("ENQUEUED")
                .setSubmissionId(id.toString())
                .setTimestamp(System.currentTimeMillis())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getInvoiceStatus(GetInvoiceStatusRequest request, StreamObserver<GetInvoiceStatusResponse> responseObserver) {
        java.util.UUID invoiceId = java.util.UUID.fromString(request.getInvoiceId());
        InvoiceTransmissionService.TransmissionStatus ts = transmissionService.getTransmissionStatus(invoiceId);
        String status = ts.getStatus();
        String message = ts.getRejectionDescription() != null ? ts.getRejectionDescription() : ts.getStatus();
        GetInvoiceStatusResponse response = GetInvoiceStatusResponse.newBuilder()
                .setInvoiceId(request.getInvoiceId())
                .setStatus(status)
                .setMessage(message)
                .setTimestamp(System.currentTimeMillis())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void listInvoices(ListInvoicesRequest request, StreamObserver<ListInvoicesResponse> responseObserver) {
        int page = Math.max(0, request.getPage());
        int size = Math.max(1, request.getPageSize() == 0 ? 20 : request.getPageSize());
        java.util.List<String> statuses = java.util.Arrays.asList("DRAFT","SUBMITTED","ACCEPTED","REJECTED","CANCELLED","FAILED","PENDING");
        var pageResult = systemRepository.findByStsInWithHeader(statuses, org.springframework.data.domain.PageRequest.of(page, size));

        ListInvoicesResponse.Builder builder = ListInvoicesResponse.newBuilder()
                .setPage(page)
                .setTotalCount((int) pageResult.getTotalElements());
        for (ElectronicInvoiceSystemEntity s : pageResult.getContent()) {
            long createdAt = 0;
            if (s.getSrvdt() != null) {
                createdAt = s.getSrvdt().toInstant().toEpochMilli();
            } else if (s.getCreatedOn() != null) {
                createdAt = s.getCreatedOn().toInstant().toEpochMilli();
            }
            String taxpayerId = s.getHeader() != null ? s.getHeader().getTaxId() : "";
            builder.addInvoices(InvoiceSummary.newBuilder()
                    .setInvoiceId(s.getId().toString())
                    .setStatus(s.getSts() != null ? s.getSts() : "")
                    .setCreatedAt(createdAt)
                    .setTaxpayerId(taxpayerId)
                    .build());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private ElectronicInvoiceHeaderEntity findHeader(UUID id) {
        Optional<ElectronicInvoiceHeaderEntity> headerOpt = headerRepository.findById(id);
        if (headerOpt.isEmpty()) {
            return null;
        }
        return headerOpt.get();
    }
}

