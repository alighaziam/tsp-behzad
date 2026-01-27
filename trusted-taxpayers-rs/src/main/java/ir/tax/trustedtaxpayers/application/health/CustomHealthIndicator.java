package ir.tax.trustedtaxpayers.application.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Health Indicator برای بررسی سلامت Database و سایر سرویس‌ها
 */
@Component
public class CustomHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CustomHealthIndicator.class);
    
    private final DataSource dataSource;

    public CustomHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        Health.Builder builder = Health.up();

        try {
            // Check Database connection
            DatabaseHealthStatus dbStatus = checkDatabase();
            details.put("database", dbStatus.toMap());
            
            if (!dbStatus.isHealthy()) {
                builder.down();
            }

            // Add application info
            details.put("application", Map.of(
                "name", "Trusted Taxpayers",
                "version", getClass().getPackage().getImplementationVersion() != null 
                    ? getClass().getPackage().getImplementationVersion() 
                    : "unknown"
            ));

            // Add JVM info
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            details.put("jvm", Map.of(
                "maxMemory", formatBytes(maxMemory),
                "totalMemory", formatBytes(totalMemory),
                "usedMemory", formatBytes(usedMemory),
                "freeMemory", formatBytes(freeMemory),
                "memoryUsagePercent", String.format("%.2f%%", (usedMemory * 100.0 / maxMemory))
            ));

            return builder.withDetails(details).build();

        } catch (Exception e) {
            logger.error("Error checking health", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetails(details)
                    .build();
        }
    }

    private DatabaseHealthStatus checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            String databaseProductName = metaData.getDatabaseProductName();
            String databaseProductVersion = metaData.getDatabaseProductVersion();
            String driverName = metaData.getDriverName();
            String driverVersion = metaData.getDriverVersion();
            
            // Test query
            boolean isValid = connection.isValid(2); // 2 seconds timeout
            
            return new DatabaseHealthStatus(
                true,
                databaseProductName,
                databaseProductVersion,
                driverName,
                driverVersion,
                isValid
            );
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return new DatabaseHealthStatus(
                false,
                null,
                null,
                null,
                null,
                false
            );
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static class DatabaseHealthStatus {
        private final boolean healthy;
        private final String productName;
        private final String productVersion;
        private final String driverName;
        private final String driverVersion;
        private final boolean connectionValid;

        public DatabaseHealthStatus(boolean healthy, String productName, String productVersion,
                                   String driverName, String driverVersion, boolean connectionValid) {
            this.healthy = healthy;
            this.productName = productName;
            this.productVersion = productVersion;
            this.driverName = driverName;
            this.driverVersion = driverVersion;
            this.connectionValid = connectionValid;
        }

        public boolean isHealthy() {
            return healthy && connectionValid;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("status", healthy && connectionValid ? "UP" : "DOWN");
            if (productName != null) map.put("productName", productName);
            if (productVersion != null) map.put("productVersion", productVersion);
            if (driverName != null) map.put("driverName", driverName);
            if (driverVersion != null) map.put("driverVersion", driverVersion);
            map.put("connectionValid", connectionValid);
            return map;
        }
    }
}

