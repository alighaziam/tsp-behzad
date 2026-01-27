package ir.tax.trustedtaxpayers.biz.impl;

import ir.gov.tax.tpis.sdk.content.api.TaxApi;
import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import ir.tax.trustedtaxpayers.application.exception.BusinessException;
import ir.tax.trustedtaxpayers.application.exception.ErrorCode;
import ir.tax.trustedtaxpayers.beans.dto.tax.*;
import ir.tax.trustedtaxpayers.biz.TaxSystemInfoManager;
import ir.tax.trustedtaxpayers.config.TaxSdkProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * پیاده‌سازی مدیریت اطلاعات سامانه مالیاتی
 * Tax System Information Manager Implementation
 */
@Slf4j
@Service("taxSystemInfoManager")
@RequiredArgsConstructor
public class TaxSystemInfoManagerImpl extends GenericImpl implements TaxSystemInfoManager {

    private final TaxApi taxApi;
    private final TaxSdkProperties taxSdkProperties;

    @Override
    public TokenModel getToken() {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.debug("Getting current token from tax API");
            TokenModel token = taxApi.getToken();
            log.debug("Token retrieved successfully");
            return token;
        } catch (Exception e) {
            log.error("Error getting token", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to get token: " + e.getMessage(), e);
        }
    }

    @Override
    public TokenModel requestNewToken() {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Requesting new token from tax system");
            TokenModel token = taxApi.requestToken();
            log.info("New token received successfully");
            return token;
        } catch (TaxApiException e) {
            log.error("Error requesting new token", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to request token: " + e.getMessage(), e);
        }
    }

    @Override
    public ServerInformationModel getServerInformation() {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Getting server information from tax system");
            ServerInformationModel serverInfo = taxApi.getServerInformation();
            log.info("Server information retrieved successfully: {}", serverInfo);
            return serverInfo;
        } catch (TaxApiException e) {
            log.error("Error getting server information", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to get server information: " + e.getMessage(), e);
        }
    }

    @Override
    public FiscalInformationModel getFiscalInformation(String fiscalId) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Getting fiscal information for fiscalId: {}", fiscalId);
            FiscalInformationModel fiscalInfo = taxApi.getFiscalInformation(fiscalId);
            log.info("Fiscal information retrieved successfully");
            return fiscalInfo;
        } catch (TaxApiException e) {
            log.error("Error getting fiscal information for fiscalId: {}", fiscalId, e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to get fiscal information: " + e.getMessage(), e);
        }
    }

    @Override
    public EconomicCodeModel getEconomicCodeInformation(String economicCode) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Getting economic code information for code: {}", economicCode);
            EconomicCodeModel economicInfo = taxApi.getEconomicCodeInformation(economicCode);
            log.info("Economic code information retrieved successfully");
            return economicInfo;
        } catch (TaxApiException e) {
            log.error("Error getting economic code information for code: {}", economicCode, e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to get economic code information: " + e.getMessage(), e);
        }
    }

    @Override
    public SearchResultModel<ServiceStuffModel> searchServiceStuff(SearchDto searchDto) {
        if (!taxSdkProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Tax SDK is disabled");
        }

        try {
            log.info("Searching service stuff with criteria: {}", searchDto);
            SearchResultModel<ServiceStuffModel> results = taxApi.getServiceStuffList(searchDto);
            log.info("Service stuff search completed, results count: {}", 
                    results != null && results.getResult() != null ? results.getResult().size() : 0);
            return results;
        } catch (TaxApiException e) {
            log.error("Error searching service stuff", e);
            throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, 
                    "Failed to search service stuff: " + e.getMessage(), e);
        }
    }
}


