package ir.tax.trustedtaxpayers.config;

import ir.gov.tax.tpis.sdk.content.api.DefaultTaxApiClient;
import ir.gov.tax.tpis.sdk.content.api.TaxApi;
import ir.gov.tax.tpis.sdk.transfer.api.ObjectTransferApiImpl;
import ir.gov.tax.tpis.sdk.transfer.api.TransferApi;
import ir.gov.tax.tpis.sdk.transfer.config.ApiConfig;
import ir.gov.tax.tpis.sdk.transfer.impl.signatory.SignatoryFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;

/**
 * پیکربندی SDK سامانه مودیان (ایندوتا)
 * Tax System SDK Configuration (Indota)
 * 
 * این پیکربندی فقط در profile های غیر تست فعال می‌شود
 * This configuration is only active in non-test profiles
 */
@Slf4j
@Configuration
@Profile("!test")
@EnableConfigurationProperties(TaxSdkProperties.class)
public class TaxSdkConfig {

    private final TaxSdkProperties properties;
    private final ResourceLoader resourceLoader;

    public TaxSdkConfig(TaxSdkProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * ایجاد TransferApi برای ارتباط با سرور مالیات
     * Create TransferApi for communication with tax server
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "tax.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TransferApi transferApi() throws IOException {
        log.info("Initializing Tax Transfer API with base URL: {}", properties.getBaseUrl());
        
        File privateKeyFile = resolvePrivateKeyFile(properties.getPrivateKeyPath());
        if (privateKeyFile == null || !privateKeyFile.exists()) {
            String errorMessage = String.format(
                "Private key file not found: %s%n" +
                "Please ensure the file exists or set TAX_SDK_PRIVATE_KEY_PATH environment variable.%n" +
                "For classpath resources, place the file at: src/main/resources/keys/private.pem%n" +
                "For absolute paths, use: TAX_SDK_PRIVATE_KEY_PATH=/path/to/private.pem%n" +
                "To disable SDK temporarily, set: tax.sdk.enabled=false in application.yml",
                properties.getPrivateKeyPath()
            );
            throw new IllegalStateException(errorMessage);
        }

        ApiConfig apiConfig = new ApiConfig()
                .baseUrl(properties.getBaseUrl())
                .transferSignatory(
                        SignatoryFactory.getInstance()
                                .createPKCS8Signatory(privateKeyFile, properties.getPrivateKeyPassword())
                );

        return new ObjectTransferApiImpl(apiConfig);
    }
    
    /**
     * Resolve private key file path (supports both absolute paths and classpath:)
     * Handles both file system resources and JAR-packaged resources
     */
    private File resolvePrivateKeyFile(String path) throws IOException {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        if (path.startsWith("classpath:")) {
            // Handle classpath resources
            String classpathPath = path.substring("classpath:".length());
            Resource resource = resourceLoader.getResource("classpath:" + classpathPath);
            
            if (!resource.exists()) {
                log.warn("Classpath resource not found: {}", path);
                return null;
            }
            
            try {
                // Try to get as File (works for file system resources)
                if (resource.isFile()) {
                    File file = resource.getFile();
                    if (file.exists()) {
                        return file;
                    }
                }
                
                // If resource is in JAR, try to copy to temp file
                // This is a fallback for JAR-packaged resources
                log.warn("Resource exists but is not directly accessible as File (might be in JAR): {}", path);
                log.warn("Consider using an absolute file path instead of classpath for JAR deployments");
                
            } catch (Exception e) {
                log.warn("Failed to resolve resource as File: {} - {}", path, e.getMessage());
            }
            
            return null;
        } else {
            // Handle absolute or relative file paths
            File file = new File(path);
            if (!file.exists()) {
                log.warn("File not found at path: {}", path);
            }
            return file;
        }
    }

    /**
     * ایجاد TaxApi برای عملیات مالیاتی
     * Create TaxApi for tax operations
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "tax.sdk", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TaxApi taxApi(TransferApi transferApi) {
        log.info("Initializing Tax API with memory ID: {}", properties.getMemoryId());
        return new DefaultTaxApiClient(transferApi, properties.getMemoryId());
    }
}


