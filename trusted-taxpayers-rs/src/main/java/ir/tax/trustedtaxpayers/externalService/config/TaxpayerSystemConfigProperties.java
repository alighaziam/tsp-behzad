package ir.tax.trustedtaxpayers.externalService.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "taxpayer-system")
public class TaxpayerSystemConfigProperties {
    private String baseUrl;
    private String apiPath;
    private String orgPublicKey;
    private String orgKeyId;
    private String clientId;
    private TimeoutConfig timeout = new TimeoutConfig();
    private RetryConfig retry = new RetryConfig();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public String getOrgPublicKey() {
        return orgPublicKey;
    }

    public void setOrgPublicKey(String orgPublicKey) {
        this.orgPublicKey = orgPublicKey;
    }

    public String getOrgKeyId() {
        return orgKeyId;
    }

    public void setOrgKeyId(String orgKeyId) {
        this.orgKeyId = orgKeyId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public void setTimeout(TimeoutConfig timeout) {
        this.timeout = timeout;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public static class TimeoutConfig {
        private int connect = 10000;
        private int read = 30000;

        public int getConnect() {
            return connect;
        }

        public void setConnect(int connect) {
            this.connect = connect;
        }

        public int getRead() {
            return read;
        }

        public void setRead(int read) {
            this.read = read;
        }
    }

    public static class RetryConfig {
        private int maxAttempts = 3;
        private int backoffDelay = 1000;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getBackoffDelay() {
            return backoffDelay;
        }

        public void setBackoffDelay(int backoffDelay) {
            this.backoffDelay = backoffDelay;
        }
    }
}

