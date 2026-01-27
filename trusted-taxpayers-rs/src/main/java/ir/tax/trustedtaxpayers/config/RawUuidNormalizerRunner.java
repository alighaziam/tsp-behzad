package ir.tax.trustedtaxpayers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(name = "db.normalize-raw-uuid-on-startup", havingValue = "true", matchIfMissing = true)
public class RawUuidNormalizerRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public RawUuidNormalizerRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        List<ColumnRef> shortRawColumns = jdbcTemplate.query(
            "SELECT table_name, column_name FROM user_tab_columns WHERE data_type = 'RAW' AND data_length < 16",
            (rs, rowNum) -> new ColumnRef(rs.getString("table_name"), rs.getString("column_name"))
        );

        for (ColumnRef column : shortRawColumns) {
            String alterSql = "ALTER TABLE " + column.tableName() + " MODIFY (" + column.columnName() + " RAW(16))";
            try {
                jdbcTemplate.execute(alterSql);
                log.info("Expanded RAW column to 16 bytes: {}.{}", column.tableName(), column.columnName());
            } catch (Exception ex) {
                log.warn("Failed to expand RAW column {}.{}: {}", column.tableName(), column.columnName(), ex.getMessage());
            }
        }

        List<ColumnRef> rawColumns = jdbcTemplate.query(
            "SELECT table_name, column_name FROM user_tab_columns WHERE data_type = 'RAW' AND data_length = 16",
            (rs, rowNum) -> new ColumnRef(rs.getString("table_name"), rs.getString("column_name"))
        );

        for (ColumnRef column : rawColumns) {
            String updateSql = "UPDATE " + column.tableName()
                + " SET " + column.columnName() + " = HEXTORAW(LPAD(RAWTOHEX(" + column.columnName() + "), 32, '0'))"
                + " WHERE " + column.columnName() + " IS NOT NULL"
                + " AND UTL_RAW.LENGTH(" + column.columnName() + ") < 16";
            try {
                int updated = jdbcTemplate.update(updateSql);
                if (updated > 0) {
                    log.info("Normalized {} short RAW values in {}.{}", updated, column.tableName(), column.columnName());
                }
            } catch (Exception ex) {
                log.warn("Failed to normalize RAW values in {}.{}: {}", column.tableName(), column.columnName(), ex.getMessage());
            }
        }
    }

    private record ColumnRef(String tableName, String columnName) {}
}
