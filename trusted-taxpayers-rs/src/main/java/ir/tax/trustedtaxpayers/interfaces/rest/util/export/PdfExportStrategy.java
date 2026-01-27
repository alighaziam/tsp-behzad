package ir.tax.trustedtaxpayers.interfaces.rest.util.export;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PdfExportStrategy implements ReportExportStrategy {
    @Override
    public byte[] export(List<Map<String, Object>> rows, List<String> headers, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());

            String docTitle = (title == null || title.isBlank()) ? "Report" : title;
            document.add(new Paragraph(docTitle + " - " + nowStr()).setBold());

            float[] columnWidths = new float[Math.max(headers.size(), 1)];
            java.util.Arrays.fill(columnWidths, Math.max(800f / Math.max(headers.size(), 1), 120f));
            Table table = new Table(columnWidths);

            for (String h : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(h)));
            }

            for (Map<String, Object> row : rows) {
                for (String h : headers) {
                    Object v = row.get(h);
                    table.addCell(new Cell().add(new Paragraph(v == null ? "" : String.valueOf(v))));
                }
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }

    private String nowStr() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }
}


