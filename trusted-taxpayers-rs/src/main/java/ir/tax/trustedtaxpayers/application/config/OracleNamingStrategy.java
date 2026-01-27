package ir.tax.trustedtaxpayers.application.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Custom naming strategy for Oracle database.
 * Preserves the exact table and column names as specified in @Table and @Column annotations.
 * This ensures that uppercase table names like "SUBSCRIPTION_PLANS" are used exactly as specified,
 * without being converted to lowercase by Spring Boot's default naming strategy.
 */
public class OracleNamingStrategy extends PhysicalNamingStrategyStandardImpl {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // Preserve the exact name from @Table annotation without modification
        // This ensures that @Table(name = "SUBSCRIPTION_PLANS") is used as-is
        if (name == null) {
            return null;
        }
        // Return the identifier exactly as specified in @Table annotation
        // Oracle will handle case conversion for unquoted identifiers automatically
        return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        // Preserve the exact name from @Column annotation without modification
        // This ensures that @Column(name = "IS_ACTIVE") is used as-is
        if (name == null) {
            return null;
        }
        // Return the identifier exactly as specified in @Column annotation
        // Oracle will handle case conversion for unquoted identifiers automatically
        return name;
    }
}

