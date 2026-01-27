package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TrustedServiceProviderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TrustedServiceProviderRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TrustedServiceProviderDto;
import ir.tax.trustedtaxpayers.interfaces.rest.mapper.TrustedServiceProviderMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/tsp/providers", "/api/v1/tsp/providers"})
@Tag(name = "Trusted Service Providers", description = "اطلاعات شرکت‌های معتمد ارائه‌دهنده خدمات سامانه مودیان")
@Validated
public class TrustedServiceProviderController {

    private final TrustedServiceProviderRepository providerRepository;
    private final TrustedServiceProviderMapper mapper;

    public TrustedServiceProviderController(TrustedServiceProviderRepository providerRepository,
                                            TrustedServiceProviderMapper mapper) {
        this.providerRepository = providerRepository;
        this.mapper = mapper;
    }

    @Operation(summary = "دریافت لیست شرکت‌های معتمد فعال")
    @GetMapping
    public ResponseEntity<List<TrustedServiceProviderDto>> list() {
        List<TrustedServiceProviderEntity> providers = providerRepository.findByIsActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(
                providers.stream()
                        .map(mapper::toDto)
                        .toList()
        );
    }
}


