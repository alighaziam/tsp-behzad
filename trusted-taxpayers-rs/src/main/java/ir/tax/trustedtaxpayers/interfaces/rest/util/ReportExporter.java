package ir.tax.trustedtaxpayers.interfaces.rest.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class ReportExporter {

    private final ir.tax.trustedtaxpayers.interfaces.rest.util.export.CsvExportStrategy csvExportStrategy;
    private final ir.tax.trustedtaxpayers.interfaces.rest.util.export.PdfExportStrategy pdfExportStrategy;

    public ReportExporter(
            ir.tax.trustedtaxpayers.interfaces.rest.util.export.CsvExportStrategy csvExportStrategy,
            ir.tax.trustedtaxpayers.interfaces.rest.util.export.PdfExportStrategy pdfExportStrategy
    ) {
        this.csvExportStrategy = csvExportStrategy;
        this.pdfExportStrategy = pdfExportStrategy;
    }

    public byte[] exportUsersToCsv(List<ir.tax.trustedtaxpayers.interfaces.rest.dto.UserDto> users) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Write UTF-8 BOM for better Excel compatibility
            baos.write(0xEF);
            baos.write(0xBB);
            baos.write(0xBF);
            try (OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.builder()
                         .setHeader("ID", "Username", "Email")
                         .build())) {
                for (var u : users) {
                    printer.printRecord(
                            u.id() == null ? "" : u.id(),
                            u.username() == null ? "" : u.username(),
                            u.email() == null ? "" : u.email()
                    );
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    public byte[] exportUsersToPdf(List<ir.tax.trustedtaxpayers.interfaces.rest.dto.UserDto> users, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());

            String docTitle = (title == null || title.isBlank()) ? "Users Report" : title;
            document.add(new Paragraph(docTitle + " - " + nowStr()).setBold());

            float[] columnWidths = { 250f, 250f, 350f };
            Table table = new Table(columnWidths);

            table.addHeaderCell(new Cell().add(new Paragraph("ID")));
            table.addHeaderCell(new Cell().add(new Paragraph("Username")));
            table.addHeaderCell(new Cell().add(new Paragraph("Email")));

            for (var u : users) {
                table.addCell(new Cell().add(new Paragraph(u.id() == null ? "" : u.id().toString())));
                table.addCell(new Cell().add(new Paragraph(u.username() == null ? "" : u.username())));
                table.addCell(new Cell().add(new Paragraph(u.email() == null ? "" : u.email())));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }

    public byte[] exportTaxpayersToCsv(List<ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerDto> taxpayers) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0xEF); baos.write(0xBB); baos.write(0xBF);
            try (OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.builder()
                         .setHeader("ID", "TaxpayerNo", "Name")
                         .build())) {
                for (var t : taxpayers) {
                    printer.printRecord(
                            t.id() == null ? "" : t.id(),
                            t.taxId() == null ? "" : t.taxId(),
                            t.name() == null ? "" : t.name()
                    );
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    public byte[] exportTaxpayersToPdf(List<ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerDto> taxpayers, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());
            String docTitle = (title == null || title.isBlank()) ? "Taxpayers Report" : title;
            document.add(new Paragraph(docTitle + " - " + nowStr()).setBold());
            float[] columnWidths = { 200f, 200f, 300f };
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("ID")));
            table.addHeaderCell(new Cell().add(new Paragraph("TaxpayerNo")));
            table.addHeaderCell(new Cell().add(new Paragraph("Name")));
            for (var t : taxpayers) {
                table.addCell(new Cell().add(new Paragraph(t.id() == null ? "" : t.id().toString())));
                table.addCell(new Cell().add(new Paragraph(t.taxId() == null ? "" : t.taxId())));
                table.addCell(new Cell().add(new Paragraph(t.name() == null ? "" : t.name())));
            }
            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF", e);
        }
    }

    public byte[] exportSettingsToCsv(List<ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity> settings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0xEF); baos.write(0xBB); baos.write(0xBF);
            try (OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(osw, CSVFormat.DEFAULT.builder()
                         .setHeader("Key", "Value", "Path", "Url")
                         .build())) {
                for (var s : settings) {
                    printer.printRecord(
                            s.getKey() == null ? "" : s.getKey(),
                            s.getValue() == null ? "" : s.getValue(),
                            s.getPath() == null ? "" : s.getPath(),
                            s.getUrl() == null ? "" : s.getUrl()
                    );
                }
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    public byte[] exportSettingsToPdf(List<ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity> settings, String title) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());
            String docTitle = (title == null || title.isBlank()) ? "Settings Report" : title;
            document.add(new Paragraph(docTitle + " - " + nowStr()).setBold());
            float[] columnWidths = { 200f, 250f, 200f, 300f };
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("Key")));
            table.addHeaderCell(new Cell().add(new Paragraph("Value")));
            table.addHeaderCell(new Cell().add(new Paragraph("Path")));
            table.addHeaderCell(new Cell().add(new Paragraph("Url")));
            for (var s : settings) {
                table.addCell(new Cell().add(new Paragraph(s.getKey() == null ? "" : s.getKey())));
                table.addCell(new Cell().add(new Paragraph(s.getValue() == null ? "" : s.getValue())));
                table.addCell(new Cell().add(new Paragraph(s.getPath() == null ? "" : s.getPath())));
                table.addCell(new Cell().add(new Paragraph(s.getUrl() == null ? "" : s.getUrl())));
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

    // ===== Generic exports (for BaseController) =====
    public byte[] exportToCsv(List<Map<String, Object>> rows, List<String> headers) {
        return csvExportStrategy.export(rows, headers, null);
    }

    public byte[] exportToPdf(List<Map<String, Object>> rows, List<String> headers, String title) {
        return pdfExportStrategy.export(rows, headers, title);
    }
}


