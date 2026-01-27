package ir.tax.trustedtaxpayers.application.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting invoices to PDF and CSV formats
 * این سرویس برای export کردن صورتحساب‌ها به فرمت‌های PDF و CSV استفاده می‌شود.
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Service
public class InvoiceExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    
    /**
     * Export invoices to PDF format
     * 
     * @param invoices لیست صورتحساب‌ها
     * @param title عنوان گزارش
     * @return byte array of PDF content
     */
    public byte[] exportToPdf(List<ElectronicInvoiceHeaderEntity> invoices, String title) {
        if (invoices == null || invoices.isEmpty()) {
            logger.warn("Empty invoice list provided for PDF export");
            return new byte[0];
        }
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            // Setting up fonts
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            
            // Title
            Paragraph titlePara = new Paragraph(title)
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(titlePara);
            
            // Summary information
            Paragraph summary = new Paragraph(
                String.format("تعداد کل: %d | تاریخ گزارش: %s", 
                    invoices.size(), 
                    LocalDateTime.now().format(DATE_FORMATTER))
            )
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(summary);
            
            // Create table
            float[] columnWidths = {2f, 2f, 1.5f, 1.5f, 1.5f, 2f, 2f, 2f};
            Table table = new Table(columnWidths).useAllAvailableWidth();
            
            // Header row
            table.addHeaderCell(new Cell().add(new Paragraph("شماره صورتحساب").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("Tax ID").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("الگو").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("نوع").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("شماره اقتصادی فروشنده").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("شماره اقتصادی خریدار").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("مبلغ کل").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("تاریخ ایجاد").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            
            // Data rows
            for (ElectronicInvoiceHeaderEntity invoice : invoices) {
                table.addCell(new Cell().add(new Paragraph(invoice.getInno() != null ? invoice.getInno() : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(invoice.getTaxId() != null ? invoice.getTaxId() : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(invoice.getTemplateCode() != null ? invoice.getTemplateCode() : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(invoice.getInty() != null ? String.valueOf(invoice.getInty().getValue()) : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(invoice.getTins() != null ? invoice.getTins() : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(invoice.getTinb() != null ? invoice.getTinb() : "-").setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(formatCurrency(invoice.getTbill())).setFont(font)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(formatDate(invoice.getCreatedOn())).setFont(font)).setTextAlignment(TextAlignment.CENTER));
            }
            
            document.add(table);
            
            // Footer - Total amount
            if (!invoices.isEmpty()) {
                BigDecimal totalAmount = invoices.stream()
                    .map(inv -> inv.getTbill() != null ? inv.getTbill() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                Paragraph footer = new Paragraph(
                    String.format("مجموع مبالغ: %s", formatCurrency(totalAmount))
                )
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
                document.add(footer);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error generating PDF export", e);
            throw new RuntimeException("خطا در ایجاد فایل PDF", e);
        }
    }
    
    /**
     * Export invoices to CSV format
     * 
     * @param invoices لیست صورتحساب‌ها
     * @return CSV content as string
     */
    public String exportToCsv(List<ElectronicInvoiceHeaderEntity> invoices) {
        if (invoices == null || invoices.isEmpty()) {
            logger.warn("Empty invoice list provided for CSV export");
            return "";
        }
        
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            
            // Header row
            csvPrinter.printRecord(
                "شماره صورتحساب",
                "Tax ID",
                "الگو",
                "نوع",
                "شماره اقتصادی فروشنده",
                "شماره اقتصادی خریدار",
                "مبلغ کل",
                "تاریخ ایجاد"
            );
            
            // Data rows
            for (ElectronicInvoiceHeaderEntity invoice : invoices) {
                csvPrinter.printRecord(
                    invoice.getInno() != null ? invoice.getInno() : "",
                    invoice.getTaxId() != null ? invoice.getTaxId() : "",
                    invoice.getTemplateCode() != null ? invoice.getTemplateCode() : "",
                    invoice.getInty() != null ? String.valueOf(invoice.getInty().getValue()) : "",
                    invoice.getTins() != null ? invoice.getTins() : "",
                    invoice.getTinb() != null ? invoice.getTinb() : "",
                    invoice.getTbill() != null ? invoice.getTbill().toString() : "0",
                    formatDate(invoice.getCreatedOn())
                );
            }
            
            csvPrinter.flush();
            return writer.toString();
            
        } catch (IOException e) {
            logger.error("Error generating CSV export", e);
            throw new RuntimeException("خطا در ایجاد فایل CSV", e);
        }
    }
    
    /**
     * Format currency amount
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.toString();
    }
    
    /**
     * Generate sample invoice PDF based on Iranian invoice format
     * ایجاد فایل PDF نمونه فاکتور بر اساس فرمت فاکتور ایرانی
     */
    public byte[] generateSampleInvoicePdf() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            
            // Header - Invoice Title
            Paragraph title = new Paragraph("صورتحساب الکترونیکی")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(title);
            
            // Invoice Info Section
            Paragraph invoiceInfo = new Paragraph("شماره صورتحساب: INV-1403-0001 | تاریخ: 1403/09/01 | الگوی صورتحساب: 1")
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(invoiceInfo);
            
            // Pattern Number - Clear label for OCR
            Paragraph patternInfo = new Paragraph("شماره الگو (INP): 1 | نوع الگو: فروش کالا و خدمات")
                .setFont(boldFont)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(patternInfo);
            
            // Seller and Buyer Info Table
            float[] infoWidths = {1f, 1f};
            Table infoTable = new Table(infoWidths).useAllAvailableWidth();
            
            // Seller Info
            infoTable.addCell(new Cell().add(new Paragraph("اطلاعات فروشنده").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            infoTable.addCell(new Cell().add(new Paragraph("اطلاعات خریدار").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            infoTable.addCell(new Cell().add(new Paragraph("نام: شرکت نمونه\nشناسه اقتصادی: 57490845111\nکد پستی: 1234567890")).setFont(font));
            infoTable.addCell(new Cell().add(new Paragraph("نام: مشتری نمونه\nشناسه اقتصادی: 10100302746\nکد پستی: 0987654321")).setFont(font));
            
            document.add(infoTable);
            document.add(new Paragraph(" ").setMarginBottom(15));
            
            // Items Table
            float[] itemWidths = {0.5f, 1.5f, 1f, 1f, 1f, 1f};
            Table itemsTable = new Table(itemWidths).useAllAvailableWidth();
            
            // Header row
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("ردیف").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("شرح کالا/خدمت").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("کد کالا").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("مقدار").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("فی واحد").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("مبلغ").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            
            // Sample items
            itemsTable.addCell(new Cell().add(new Paragraph("1").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("شیر کم چرب پاستوریزه").setFont(font)));
            itemsTable.addCell(new Cell().add(new Paragraph("2902230386252").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("1").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("10,000").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("10,000").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            
            document.add(itemsTable);
            document.add(new Paragraph(" ").setMarginBottom(10));
            
            // Pattern-specific fields section (for OCR detection)
            Paragraph patternFieldsTitle = new Paragraph("فیلدهای اختصاصی الگو (در صورت نیاز):")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginTop(15)
                .setMarginBottom(10);
            document.add(patternFieldsTitle);
            
            float[] patternWidths = {2f, 3f};
            Table patternFieldsTable = new Table(patternWidths).useAllAvailableWidth();
            
            patternFieldsTable.addCell(new Cell().add(new Paragraph("الگوی 2 (فروش ارز):").setFont(boldFont)));
            patternFieldsTable.addCell(new Cell().add(new Paragraph("نوع ارز (CUT): USD | فی واحد ارزی (CFEE): 100 | نرخ تبدیل (EXR): 42000").setFont(font)));
            
            patternFieldsTable.addCell(new Cell().add(new Paragraph("الگوی 3 (طلا و جواهر):").setFont(boldFont)));
            patternFieldsTable.addCell(new Cell().add(new Paragraph("عیار طلا (CUI): 18.5").setFont(font)));
            
            patternFieldsTable.addCell(new Cell().add(new Paragraph("الگوی 4 (قرارداد):").setFont(boldFont)));
            patternFieldsTable.addCell(new Cell().add(new Paragraph("شماره قرارداد (CRN): CR-2024-001").setFont(font)));
            
            document.add(patternFieldsTable);
            
            Paragraph patternNote = new Paragraph("توجه: لطفاً شماره الگو (INP) و فیلدهای مربوط به آن را در فاکتور خود مشخص کنید.")
                .setFont(font)
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
            document.add(patternNote);
            
            document.add(new Paragraph(" ").setMarginBottom(10));
            document.add(new Paragraph(" ").setMarginBottom(10));
            
            // Totals Section
            float[] totalWidths = {2f, 1f};
            Table totalTable = new Table(totalWidths).useAllAvailableWidth();
            
            totalTable.addCell(new Cell().add(new Paragraph("جمع قبل از تخفیف:").setFont(font)));
            totalTable.addCell(new Cell().add(new Paragraph("10,000").setFont(font)).setTextAlignment(TextAlignment.LEFT));
            
            totalTable.addCell(new Cell().add(new Paragraph("تخفیف:").setFont(font)));
            totalTable.addCell(new Cell().add(new Paragraph("500").setFont(font)).setTextAlignment(TextAlignment.LEFT));
            
            totalTable.addCell(new Cell().add(new Paragraph("جمع بعد از تخفیف:").setFont(font)));
            totalTable.addCell(new Cell().add(new Paragraph("9,500").setFont(font)).setTextAlignment(TextAlignment.LEFT));
            
            totalTable.addCell(new Cell().add(new Paragraph("مالیات بر ارزش افزوده (9%):").setFont(font)));
            totalTable.addCell(new Cell().add(new Paragraph("855").setFont(font)).setTextAlignment(TextAlignment.LEFT));
            
            totalTable.addCell(new Cell().add(new Paragraph("مبلغ کل:").setFont(boldFont)).setTextAlignment(TextAlignment.RIGHT));
            totalTable.addCell(new Cell().add(new Paragraph("12,255").setFont(boldFont)).setTextAlignment(TextAlignment.LEFT));
            
            document.add(totalTable);
            
            // Footer
            Paragraph footer = new Paragraph("این فایل نمونه است و برای تست استفاده می‌شود")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
            document.add(footer);
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error generating sample invoice PDF", e);
            throw new RuntimeException("خطا در ایجاد فایل PDF نمونه", e);
        }
    }
    
    /**
     * Generate sample invoice PDF for Pattern 2 (Currency Sales)
     * ایجاد فایل PDF نمونه برای الگوی 2 (فروش ارز)
     */
    public byte[] generateSampleInvoicePdfPattern2() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {
            
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            
            // Header
            Paragraph title = new Paragraph("صورتحساب الکترونیکی - فروش ارز")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(title);
            
            // Invoice Info with Pattern
            Paragraph invoiceInfo = new Paragraph("شماره صورتحساب: INV-1403-0002 | تاریخ: 1403/09/01 | الگوی صورتحساب: 2")
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
            document.add(invoiceInfo);
            
            Paragraph patternInfo = new Paragraph("شماره الگو (INP): 2 | نوع الگو: فروش ارز")
                .setFont(boldFont)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(patternInfo);
            
            // Seller and Buyer Info
            float[] infoWidths = {1f, 1f};
            Table infoTable = new Table(infoWidths).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("اطلاعات فروشنده").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            infoTable.addCell(new Cell().add(new Paragraph("اطلاعات خریدار").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            infoTable.addCell(new Cell().add(new Paragraph("نام: شرکت نمونه\nشناسه اقتصادی: 57490845111")).setFont(font));
            infoTable.addCell(new Cell().add(new Paragraph("نام: مشتری نمونه\nشناسه اقتصادی: 10100302746")).setFont(font));
            document.add(infoTable);
            document.add(new Paragraph(" ").setMarginBottom(15));
            
            // Items Table with Currency fields
            float[] itemWidths = {0.5f, 1.5f, 1f, 1f, 1f, 1f, 1f, 1f};
            Table itemsTable = new Table(itemWidths).useAllAvailableWidth();
            
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("ردیف").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("شرح").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("نوع ارز (CUT)").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("فی واحد ارزی (CFEE)").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("نرخ تبدیل (EXR)").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("مقدار").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("مبلغ ارزی").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addHeaderCell(new Cell().add(new Paragraph("مبلغ ریالی").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            
            // Sample currency item
            itemsTable.addCell(new Cell().add(new Paragraph("1").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("فروش ارز").setFont(font)));
            itemsTable.addCell(new Cell().add(new Paragraph("USD").setFont(boldFont)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("100").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("42,000").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("1").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("100 USD").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            itemsTable.addCell(new Cell().add(new Paragraph("4,200,000").setFont(font)).setTextAlignment(TextAlignment.CENTER));
            
            document.add(itemsTable);
            document.add(new Paragraph(" ").setMarginBottom(10));
            
            // Totals
            float[] totalWidths = {2f, 1f};
            Table totalTable = new Table(totalWidths).useAllAvailableWidth();
            totalTable.addCell(new Cell().add(new Paragraph("ارزش ارزی (TOCV):").setFont(font)));
            totalTable.addCell(new Cell().add(new Paragraph("100 USD").setFont(font)).setTextAlignment(TextAlignment.LEFT));
            totalTable.addCell(new Cell().add(new Paragraph("مبلغ کل (TBILL):").setFont(boldFont)));
            totalTable.addCell(new Cell().add(new Paragraph("4,200,000").setFont(boldFont)).setTextAlignment(TextAlignment.LEFT));
            document.add(totalTable);
            
            // Footer
            Paragraph footer = new Paragraph("این فایل نمونه برای الگوی 2 (فروش ارز) است")
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
            document.add(footer);
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            logger.error("Error generating sample invoice PDF for pattern 2", e);
            throw new RuntimeException("خطا در ایجاد فایل PDF نمونه الگوی 2", e);
        }
    }
    
    /**
     * Generate sample invoice Word document (HTML format that can be opened in Word)
     * ایجاد فایل Word نمونه فاکتور (فرمت HTML که در Word قابل باز کردن است)
     */
    public byte[] generateSampleInvoiceWord() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html dir=\"rtl\" lang=\"fa\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"ProgId\" content=\"Word.Document\">\n");
        html.append("    <meta name=\"Generator\" content=\"Microsoft Word\">\n");
        html.append("    <meta name=\"Originator\" content=\"Microsoft Word\">\n");
        html.append("    <title>صورتحساب نمونه</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'B Nazanin', 'Tahoma', Arial; direction: rtl; text-align: right; padding: 20px; }\n");
        html.append("        h1 { text-align: center; color: #333; }\n");
        html.append("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: right; }\n");
        html.append("        th { background-color: #f2f2f2; font-weight: bold; }\n");
        html.append("        .info-section { margin: 20px 0; }\n");
        html.append("        .total-row { font-weight: bold; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>صورتحساب الکترونیکی</h1>\n");
        html.append("    \n");
        html.append("    <div class=\"info-section\">\n");
        html.append("        <p><strong>شماره صورتحساب:</strong> INV-1403-0001</p>\n");
        html.append("        <p><strong>تاریخ:</strong> 1403/09/01</p>\n");
        html.append("        <p><strong>شماره الگو (INP):</strong> 1</p>\n");
        html.append("        <p><strong>نوع الگو:</strong> فروش کالا و خدمات</p>\n");
        html.append("    </div>\n");
        html.append("    \n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th colspan=\"2\">اطلاعات فروشنده</th>\n");
        html.append("            <th colspan=\"2\">اطلاعات خریدار</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>نام:</td>\n");
        html.append("            <td>شرکت نمونه</td>\n");
        html.append("            <td>نام:</td>\n");
        html.append("            <td>مشتری نمونه</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>شناسه اقتصادی:</td>\n");
        html.append("            <td>57490845111</td>\n");
        html.append("            <td>شناسه اقتصادی:</td>\n");
        html.append("            <td>10100302746</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>کد پستی:</td>\n");
        html.append("            <td>1234567890</td>\n");
        html.append("            <td>کد پستی:</td>\n");
        html.append("            <td>0987654321</td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <th>ردیف</th>\n");
        html.append("            <th>شرح کالا/خدمت</th>\n");
        html.append("            <th>کد کالا</th>\n");
        html.append("            <th>مقدار</th>\n");
        html.append("            <th>فی واحد</th>\n");
        html.append("            <th>مبلغ</th>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>1</td>\n");
        html.append("            <td>شیر کم چرب پاستوریزه</td>\n");
        html.append("            <td>2902230386252</td>\n");
        html.append("            <td>1</td>\n");
        html.append("            <td>10,000</td>\n");
        html.append("            <td>10,000</td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <!-- Pattern-specific fields example -->\n");
        html.append("    <div style=\"margin: 20px 0; padding: 15px; background-color: #f0f8ff; border-right: 4px solid #0066cc;\">\n");
        html.append("        <h3 style=\"color: #0066cc; margin-top: 0;\">فیلدهای اختصاصی الگو (در صورت نیاز):</h3>\n");
        html.append("        <table style=\"width: 100%; margin-top: 10px;\">\n");
        html.append("            <tr>\n");
        html.append("                <td><strong>الگوی 2 (فروش ارز):</strong></td>\n");
        html.append("                <td>نوع ارز (CUT): USD | فی واحد ارزی (CFEE): 100 | نرخ تبدیل (EXR): 42000</td>\n");
        html.append("            </tr>\n");
        html.append("            <tr>\n");
        html.append("                <td><strong>الگوی 3 (طلا و جواهر):</strong></td>\n");
        html.append("                <td>عیار طلا (CUI): 18.5</td>\n");
        html.append("            </tr>\n");
        html.append("            <tr>\n");
        html.append("                <td><strong>الگوی 4 (قرارداد):</strong></td>\n");
        html.append("                <td>شماره قرارداد (CRN): CR-2024-001</td>\n");
        html.append("            </tr>\n");
        html.append("        </table>\n");
        html.append("        <p style=\"color: #666; font-size: 12px; margin-top: 15px; margin-bottom: 0;\">\n");
        html.append("            <strong>توجه:</strong> لطفاً شماره الگو (INP) و فیلدهای مربوط به آن را در فاکتور خود مشخص کنید تا OCR بتواند آن‌ها را به درستی تشخیص دهد.\n");
        html.append("        </p>\n");
        html.append("    </div>\n");
        html.append("    \n");
        html.append("    <table>\n");
        html.append("        <tr>\n");
        html.append("            <td>جمع قبل از تخفیف:</td>\n");
        html.append("            <td>10,000</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>تخفیف:</td>\n");
        html.append("            <td>500</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>جمع بعد از تخفیف:</td>\n");
        html.append("            <td>9,500</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr>\n");
        html.append("            <td>مالیات بر ارزش افزوده (9%):</td>\n");
        html.append("            <td>855</td>\n");
        html.append("        </tr>\n");
        html.append("        <tr class=\"total-row\">\n");
        html.append("            <td>مبلغ کل:</td>\n");
        html.append("            <td>12,255</td>\n");
        html.append("        </tr>\n");
        html.append("    </table>\n");
        html.append("    \n");
        html.append("    <p style=\"text-align: center; margin-top: 30px; color: #666;\">\n");
        html.append("        این فایل نمونه است و برای تست استفاده می‌شود. لطفاً اطلاعات را پر کرده و سپس به تصویر تبدیل کنید.\n");
        html.append("    </p>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * Format date
     */
    private String formatDate(java.time.OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_FORMATTER);
    }
}


