package ir.tax.trustedtaxpayers.application.config;

import ir.gov.tax.tpis.sdk.transfer.api.SimpleTransferApiImpl;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.ApiConfig;
import ir.gov.tax.tpis.sdk.transfer.dto.PriorityLevel;
import ir.gov.tax.tpis.sdk.transfer.impl.encrypter.DefaultEncrypter;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.InMemorySignatory;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.SignatoryFactory;
import ir.gov.tax.tpis.sdk.transfer.interfaces.Encrypter;
import ir.gov.tax.tpis.sdk.transfer.interfaces.Signatory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration for TPIS SDK Transfer Module
 * پیکربندی برای ماژول انتقال SDK
 * 
 * این کلاس مسئولیت ایجاد و پیکربندی TransferApi را دارد
 * و از کلیدهای خصوصی و عمومی برای امضا و رمزنگاری استفاده می‌کند.
 */
@Configuration
public class TpisSdkConfig {

    private static final Logger logger = LoggerFactory.getLogger(TpisSdkConfig.class);

    private final ir.tax.trustedtaxpayers.externalService.config.TaxpayerSystemConfigProperties props;
    private final Environment env;

    public TpisSdkConfig(ir.tax.trustedtaxpayers.externalService.config.TaxpayerSystemConfigProperties props, Environment env) {
        this.props = props;
        this.env = env;
    }

    @Bean
    @ConditionalOnMissingBean(name = "tpisApiConfig")
    public ApiConfig tpisApiConfig() {
        logger.info("Initializing TPIS ApiConfig with baseUrl: {}", props.getBaseUrl());
        
        ApiConfig apiConfig = new ApiConfig();
        
        // تنظیم Base URL
        if (props.getBaseUrl() != null && !props.getBaseUrl().isBlank()) {
            apiConfig.baseUrl(props.getBaseUrl());
        }
        
        // تنظیم API Version (اگر لازم باشد)
        // apiConfig.apiVersion("v1");
        
        // تنظیم Priority Level
        apiConfig.priorityLevel(PriorityLevel.NORMAL);
        
        // تنظیم Encrypter در صورت وجود کلید عمومی سازمان
        if (props.getOrgPublicKey() != null && !props.getOrgPublicKey().isBlank() 
            && props.getOrgKeyId() != null && !props.getOrgKeyId().isBlank()) {
            try {
                Encrypter encrypter = new DefaultEncrypter(props.getOrgPublicKey(), props.getOrgKeyId());
                apiConfig.encrypter(encrypter);
                logger.info("Encrypter configured with encryption key ID: {}", props.getOrgKeyId());
            } catch (Exception e) {
                logger.warn("Failed to configure encrypter: {}", e.getMessage());
            }
        }
        
        return apiConfig;
    }

    @Bean
    @ConditionalOnMissingBean(name = "tpisTransferSignatory")
    public Signatory tpisTransferSignatory() {
        try {
            java.io.File keyFile = resolvePrivateKeyFile();
            if (keyFile == null) {
                logger.warn("Transfer Signatory not configured (no private key). Set tax.sdk.private-key-path or TAX_SDK_PRIVATE_KEY_PATH.");
                return null;
            }
            String password = getProperty("tax.sdk.private-key-password", System.getenv("TAX_SDK_PRIVATE_KEY_PASSWORD"));
            String keyId = props.getClientId() != null ? props.getClientId() : getProperty("tax.sdk.client-id", System.getenv("TAX_SDK_CLIENT_ID"));
            if (keyId == null || keyId.isBlank()) {
                logger.warn("Transfer Signatory keyId (clientId) not configured. Set taxpayer-system.client-id or tax.sdk.client-id");
                return null;
            }
            return SignatoryFactory.getInstance().createPKCS8Signatory(keyFile, password != null ? password : "");
        } catch (Exception e) {
            logger.warn("Failed to configure Transfer Signatory: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = "tpisPacketSignatory")
    public Signatory tpisPacketSignatory() {
        try {
            java.io.File keyFile = resolvePrivateKeyFile();
            if (keyFile == null) {
                logger.warn("Packet Signatory not configured (no private key). Set tax.sdk.private-key-path or TAX_SDK_PRIVATE_KEY_PATH.");
                return null;
            }
            String password = getProperty("tax.sdk.private-key-password", System.getenv("TAX_SDK_PRIVATE_KEY_PASSWORD"));
            String keyId = props.getClientId() != null ? props.getClientId() : getProperty("tax.sdk.client-id", System.getenv("TAX_SDK_CLIENT_ID"));
            if (keyId == null || keyId.isBlank()) {
                logger.warn("Packet Signatory keyId (clientId) not configured. Set taxpayer-system.client-id or tax.sdk.client-id");
                return null;
            }
            return SignatoryFactory.getInstance().createPKCS8Signatory(keyFile, password != null ? password : "");
        } catch (Exception e) {
            logger.warn("Failed to configure Packet Signatory: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = "tpisTransferApi")
    public TransferApi tpisTransferApi(ApiConfig tpisApiConfig, 
                                      @org.springframework.beans.factory.annotation.Qualifier("tpisTransferSignatory") 
                                      ObjectProvider<Signatory> tpisTransferSignatoryProvider) {
        logger.info("Initializing TPIS TransferApi");
        
        // تنظیم Signatory در صورت وجود
        Signatory signatory = tpisTransferSignatoryProvider.getIfAvailable();
        if (signatory != null) {
            tpisApiConfig.transferSignatory(signatory);
            logger.info("Transfer Signatory configured");
        } else {
            logger.info("Transfer Signatory not available - continuing without it");
        }
        
        return new SimpleTransferApiImpl(tpisApiConfig);
    }

    /**
     * Helper method برای ایجاد Signatory از Base64 private key
     */
    public static Signatory createSignatory(String base64PrivateKey, String keyId) {
        if (base64PrivateKey == null || base64PrivateKey.isBlank()) {
            throw new IllegalArgumentException("Private key cannot be null or empty");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("Key ID cannot be null or empty");
        }
        
        return new InMemorySignatory(base64PrivateKey, keyId);
    }

    /**
     * Helper method برای ایجاد Signatory از فایل PKCS8
     */
    public static Signatory createSignatoryFromFile(java.io.File pkcs8File, String keyId) throws java.io.IOException {
        return SignatoryFactory.getInstance().createPKCS8Signatory(pkcs8File, keyId);
    }

    private java.io.File resolvePrivateKeyFile() {
        String path = getProperty("tax.sdk.private-key-path", System.getenv("TAX_SDK_PRIVATE_KEY_PATH"));
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("classpath:")) {
            String cp = path.substring("classpath:".length());
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(cp);
            if (url == null) {
                return null;
            }
            try {
                return new java.io.File(url.toURI());
            } catch (Exception ignored) {
                return null;
            }
        }
        java.io.File f = new java.io.File(path);
        return f.exists() ? f : null;
    }

    private String getProperty(String key, String fallback) {
        try {
            String val = env != null ? env.getProperty(key) : null;
            return (val != null && !val.isBlank()) ? val : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
