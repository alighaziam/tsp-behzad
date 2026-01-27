package ir.tax.trustedtaxpayers.interfaces.rest.util.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvExportStrategy implements ReportExportStrategy {
    @Override
    public byte[] export(List<Map<String, Object>> rows, List<String> headers, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0xEF); baos.write(0xBB); baos.write(0xBF);
            try (OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.builder()
                         .setHeader(headers.toArray(new String[0]))
                         .build())) {
                for (Map<String, Object> row : rows) {
                    Object[] values = headers.stream()
                            .map(h -> {
                                Object v = row.get(h);
                                String s = v == null ? "" : String.valueOf(v);
                                // Prevent CSV formula injection in Excel
                                if (!s.isEmpty() && (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@"))) {
                                    return "'" + s;
                                }
                                return s;
                            })
                            .toArray();
                    printer.printRecord(values);
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }
}


