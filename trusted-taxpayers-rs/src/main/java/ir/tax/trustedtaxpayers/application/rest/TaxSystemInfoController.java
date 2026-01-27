package ir.tax.trustedtaxpayers.application.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import ir.tax.trustedtaxpayers.biz.TaxSystemInfoManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * کنترلر REST برای اطلاعات سامانه مالیاتی
 * REST Controller for Tax System Information
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tax/system")
@RequiredArgsConstructor
@Tag(name = "Tax System Info", description = "اطلاعات سامانه مالیاتی - Tax System Information APIs")
public class TaxSystemInfoController {

    private final TaxSystemInfoManager taxSystemInfoManager;

    @GetMapping
    public ResponseEntity<java.util.Map<String, String>> health() {
        return ResponseEntity.ok(java.util.Map.of("status", "ok"));
    }

    /**
     * دریافت اطلاعات سرور مالیاتی
     * Get tax server information
     */
    @GetMapping("/server-info")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "دریافت اطلاعات سرور", description = "Get tax server information including public keys")
    public ResponseEntity<ServerInformationModel> getServerInformation() {
        log.info("Received request to get server information");

        try {
            ServerInformationModel serverInfo = taxSystemInfoManager.getServerInformation();
            log.debug("Server information retrieved: {}", serverInfo);
            return ResponseEntity.ok(serverInfo);
        } catch (Exception e) {
            log.error("Error getting server information", e);
            throw e;
        }
    }

    /**
     * دریافت توکن فعلی
     * Get current token
     */
    @GetMapping("/token")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(summary = "دریافت توکن فعلی", description = "Get current authentication token")
    public ResponseEntity<TokenModel> getToken() {
        log.info("Received request to get current token");

        try {
            TokenModel token = taxSystemInfoManager.getToken();
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Error getting token", e);
            throw e;
        }
    }

    /**
     * درخواست توکن جدید
     * Request new token
     */
    @PostMapping("/token/refresh")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(summary = "درخواست توکن جدید", description = "Request a new authentication token")
    public ResponseEntity<TokenModel> requestNewToken() {
        log.info("Received request to refresh token");

        try {
            TokenModel token = taxSystemInfoManager.requestNewToken();
            log.info("New token received successfully");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            log.error("Error requesting new token", e);
            throw e;
        }
    }

    /**
     * دریافت اطلاعات حافظه مالیاتی
     * Get fiscal memory information
     */
    @GetMapping("/fiscal/{fiscalId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
    @Operation(summary = "دریافت اطلاعات حافظه مالیاتی", description = "Get fiscal memory information by fiscal ID")
    public ResponseEntity<FiscalInformationModel> getFiscalInformation(
            @Parameter(description = "شناسه حافظه مالیاتی (Fiscal/Memory ID)", example = "A111H3")
            @PathVariable String fiscalId) {
        log.info("Received request to get fiscal information for: {}", fiscalId);

        try {
            FiscalInformationModel fiscalInfo = taxSystemInfoManager.getFiscalInformation(fiscalId);
            return ResponseEntity.ok(fiscalInfo);
        } catch (Exception e) {
            log.error("Error getting fiscal information for: {}", fiscalId, e);
            throw e;
        }
    }

    /**
     * دریافت اطلاعات کد اقتصادی
     * Get economic code information
     */
    @GetMapping("/economic-code/{code}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "دریافت اطلاعات کد اقتصادی", description = "Get economic code information")
    public ResponseEntity<EconomicCodeModel> getEconomicCodeInformation(
            @Parameter(description = "کد اقتصادی (Economic Code)", example = "5555555555")
            @PathVariable String code) {
        log.info("Received request to get economic code information for: {}", code);

        try {
            EconomicCodeModel economicInfo = taxSystemInfoManager.getEconomicCodeInformation(code);
            return ResponseEntity.ok(economicInfo);
        } catch (Exception e) {
            log.error("Error getting economic code information for: {}", code, e);
            throw e;
        }
    }

    /**
     * جستجوی لیست کالا و خدمات
     * Search service stuff list
     */
    @PostMapping("/service-stuff/search")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'TSP', 'TAXPAYER')")
    @Operation(summary = "جستجوی کالا و خدمات", description = "Search service and stuff list with pagination")
    public ResponseEntity<SearchResultModel<ServiceStuffModel>> searchServiceStuff(
            @Valid @RequestBody SearchDto searchDto) {
        log.info("Received request to search service stuff: page={}, size={}", 
                searchDto.getPage(), searchDto.getSize());

        try {
            SearchResultModel<ServiceStuffModel> results = taxSystemInfoManager.searchServiceStuff(searchDto);
            log.info("Service stuff search completed, results count: {}", 
                    results != null && results.getResult() != null ? results.getResult().size() : 0);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching service stuff", e);
            throw e;
        }
    }
}

