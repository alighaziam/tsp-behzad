package ir.gov.tax.tpis.sdk.transfer.http;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utility class for managing SSL trust-all configurations.
 * <p>
 * <b>Warning:</b> This implementation trusts all certificates and should only be used
 * in development or testing environments. Using this in production is a security risk.
 * <p>
 * توضیح فارسی: این کلاس ابزار برای پیکربندی حالت «اعتماد به تمامی گواهی‌های SSL» است.
 * استفاده از این حالت صرفاً برای محیط‌های توسعه و آزمایشی مناسب بوده و استفاده در
 * محیط تولید خطر امنیتی به‌همراه دارد.
 */
public final class SslTrustManager {

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // Trust all client certificates
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // Trust all server certificates
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
    };

    private static final SSLContext TRUST_ALL_SSL_CONTEXT;

    static {
        try {
            TRUST_ALL_SSL_CONTEXT = SSLContext.getInstance("SSL");
            TRUST_ALL_SSL_CONTEXT.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize SSL context", e);
        }
    }

    private static final SSLSocketFactory TRUST_ALL_SSL_SOCKET_FACTORY = TRUST_ALL_SSL_CONTEXT.getSocketFactory();

    private SslTrustManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Configures an OkHttpClient to trust all SSL certificates.
     * <p>
     * <b>Warning:</b> This disables SSL certificate verification and should only be used
     * in development or testing environments.
     *
     * @param client the OkHttpClient to configure
     * @return a new OkHttpClient configured to trust all certificates
     */
    // توضیح فارسی: یک نمونه OkHttpClient را طوری پیکربندی می‌کند که همه گواهی‌های SSL را بپذیرد
    // (صرفاً مناسب محیط توسعه/تست).
    public static okhttp3.OkHttpClient configureTrustAllSsl(okhttp3.OkHttpClient client) {
        okhttp3.OkHttpClient.Builder builder = client.newBuilder();
        builder.sslSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY, (X509TrustManager) TRUST_ALL_CERTS[0]);
        builder.hostnameVerifier((hostname, session) -> true);
        return builder.build();
    }
}

