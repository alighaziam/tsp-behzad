package ir.tax.trustedtaxpayers.interfaces.rest.util.export;

import java.util.List;
import java.util.Map;

public interface ReportExportStrategy {
    byte[] export(List<Map<String, Object>> rows, List<String> headers, String title);
}


