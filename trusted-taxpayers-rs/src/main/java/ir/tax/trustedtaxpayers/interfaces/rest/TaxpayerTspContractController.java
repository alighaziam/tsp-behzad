package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TaxpayerService;
import ir.tax.trustedtaxpayers.application.service.TaxpayerTspContractService;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ApproveContractRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateTaxpayerContractRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerTspContractDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UpdateTaxpayerContractRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.TaxpayerMapper;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.TaxpayerTspContractMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/tsp/contracts", "/api/v1/tsp/contracts"})
@Tag(name = "Taxpayer TSP Contracts", description = "مدیریت قراردادهای مودی با شرکت‌های معتمد")
public class TaxpayerTspContractController {

    private final TaxpayerTspContractService contractService;
    private final TaxpayerTspContractMapper contractMapper;
    private final TaxpayerService taxpayerService;
    private final TaxpayerMapper taxpayerMapper;

    public TaxpayerTspContractController(TaxpayerTspContractService contractService,
                                         TaxpayerTspContractMapper contractMapper,
                                         TaxpayerService taxpayerService,
                                         TaxpayerMapper taxpayerMapper) {
        this.contractService = contractService;
        this.contractMapper = contractMapper;
        this.taxpayerService = taxpayerService;
        this.taxpayerMapper = taxpayerMapper;
    }

    @Operation(summary = "لیست قراردادهای یک مودی")
    @GetMapping("/taxpayer/{taxpayerId}")
    public ResponseEntity<List<TaxpayerTspContractDto>> getContracts(@PathVariable UUID taxpayerId) {
        return ResponseEntity.ok(
                contractService.findContracts(taxpayerId).stream().map(contractMapper::toDto).toList()
        );
    }

    @Operation(summary = "قرارداد فعال مودی")
    @GetMapping("/taxpayer/{taxpayerId}/active")
    public ResponseEntity<TaxpayerTspContractDto> getActiveContract(@PathVariable UUID taxpayerId) {
        return contractService.findActiveContract(taxpayerId)
                .map(contractMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "دریافت قرارداد بر اساس شناسه")
    @GetMapping("/{contractId}")
    public ResponseEntity<TaxpayerTspContractDto> getById(@PathVariable UUID contractId) {
        return contractService.findById(contractId)
                .map(contractMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "لیست قراردادها (بدون فیلتر یا با فیلتر ساده)")
    @GetMapping
    public ResponseEntity<List<TaxpayerTspContractDto>> listContracts(
            @RequestParam(name = "taxpayerId", required = false) UUID taxpayerId) {
        var contracts = taxpayerId != null
                ? contractService.findContracts(taxpayerId)
                : contractService.findAllContracts();
        return ResponseEntity.ok(contracts.stream().map(contractMapper::toDto).toList());
    }

    @Operation(summary = "ایجاد قرارداد جدید بین مودی و شرکت معتمد")
    @PostMapping
    public ResponseEntity<TaxpayerTspContractDto> create(@Valid @RequestBody CreateTaxpayerContractRequest request) {
        var contract = contractService.createContract(
                request.taxpayerId(),
                request.providerId(),
                request.contractNumber(),
                request.contractTitle(),
                request.signedAt(),
                request.expiresAt(),
                request.status(),
                request.notes()
        );
        return ResponseEntity.ok(contractMapper.toDto(contract));
    }

    @Operation(summary = "غیرفعال‌سازی قرارداد")
    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID contractId) {
        contractService.deactivateContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "به‌روزرسانی قرارداد")
    @PutMapping("/{contractId}")
    public ResponseEntity<TaxpayerTspContractDto> update(@PathVariable UUID contractId,
                                                         @Valid @RequestBody UpdateTaxpayerContractRequest request) {
        var updated = contractService.updateContract(
                contractId,
                request.contractNumber(),
                request.contractTitle(),
                request.signedAt(),
                request.expiresAt(),
                request.status(),
                request.notes(),
                request.active()
        );
        return ResponseEntity.ok(contractMapper.toDto(updated));
    }

    @Operation(summary = "تایید قرارداد")
    @PostMapping("/{contractId}/approve")
    public ResponseEntity<TaxpayerTspContractDto> approve(@PathVariable UUID contractId,
                                                          @Valid @RequestBody(required = false) ApproveContractRequest request) {
        var approved = contractService.approveContract(
                contractId,
                request != null ? request.status() : null,
                request != null ? request.approvalNote() : null
        );
        return ResponseEntity.ok(contractMapper.toDto(approved));
    }

    @Operation(summary = "مشاهده قراردادهای رو به پایان")
    @GetMapping("/expiring")
    public ResponseEntity<List<TaxpayerTspContractDto>> expiring(
            @RequestParam(name = "withinDays", defaultValue = "30") int withinDays,
            @RequestParam(name = "includeExpired", defaultValue = "false") boolean includeExpired) {
        var contracts = contractService.findExpiringContracts(withinDays, includeExpired);
        return ResponseEntity.ok(contracts.stream().map(contractMapper::toDto).toList());
    }
}


