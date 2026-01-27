package ir.tax.trustedtaxpayers.application.service;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceBodyDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.InvoiceHeaderDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating HTML representation of electronic invoices
 * سرویس برای تولید HTML صورتحساب الکترونیکی
 * 
 * This service generates HTML that matches the official electronic invoice PDF structure
 */
@Service
public class InvoiceHtmlGeneratorService {

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    
    // Days in each Jalali month
    private static final int[] JALALI_DAYS_IN_MONTH = {31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29};

    /**
     * Generate HTML for an invoice
     * تولید HTML برای یک صورتحساب
     */
    public String generateHtml(InvoiceDto invoice) {
        return generateHtml(invoice, null, null);
    }

    /**
     * Generate HTML for an invoice with seller and buyer names
     * تولید HTML برای یک صورتحساب با نام فروشنده و خریدار
     */
    public String generateHtml(InvoiceDto invoice, String sellerName, String buyerName) {
        if (invoice == null) {
            return "<html><body><p>صورتحساب یافت نشد</p></body></html>";
        }

        InvoiceHeaderDto header = invoice.getHeader();
        List<InvoiceBodyDto> body = invoice.getBody();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html dir=\"rtl\" lang=\"fa\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>صورتحساب الکترونیکی</title>\n");
        html.append("    <style>\n");
        html.append(getCssStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"invoice-container\">\n");
        
        // Government / Invoice Header Section
        html.append(generateInvoiceHeaderSection(header));
        
        // Seller Section
        html.append(generateSellerSection(header, sellerName));
        
        // Buyer Section
        html.append(generateBuyerSection(header, buyerName));
        
        // Items Table
        if (body != null && !body.isEmpty()) {
            html.append(generateItemsTable(body));
        }
        
        // Totals Section
        html.append(generateTotalsSection(header));
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    /**
     * Generate Government / Invoice Header Section
     * This section contains official invoice identification fields
     */
    private String generateInvoiceHeaderSection(InvoiceHeaderDto header) {
        if (header == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <table class=\"invoice-header-table\">\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th colspan=\"4\" class=\"header-title\">صورتحساب الکترونیکی فروش کالا/خدمات</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        // Unique tax invoice ID
        html.append("                <tr>\n");
        html.append("                    <td class=\"label-cell\">شماره منحصر به فرد مالیاتی:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(header.getTaxid() != null ? escapeHtml(header.getTaxid()) : "").append("</td>\n");
        html.append("                    <td class=\"label-cell\">شماره سریال حافظه مالیاتی:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(header.getBillid() != null ? escapeHtml(header.getBillid()) : "").append("</td>\n");
        html.append("                </tr>\n");
        
        // Internal invoice number and Pattern
        html.append("                <tr>\n");
        html.append("                    <td class=\"label-cell\">شماره صورتحساب داخلی:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(header.getInno() != null ? escapeHtml(header.getInno()) : "").append("</td>\n");
        html.append("                    <td class=\"label-cell\">شماره الگو:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(header.getInp() != null ? formatNumber(BigDecimal.valueOf(header.getInp())) : "").append("</td>\n");
        html.append("                </tr>\n");
        
        // Invoice type and Subject
        html.append("                <tr>\n");
        html.append("                    <td class=\"label-cell\">نوع صورتحساب:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(getInvoiceTypeText(header.getInty())).append("</td>\n");
        html.append("                    <td class=\"label-cell\">موضوع صورتحساب:</td>\n");
        html.append("                    <td class=\"value-cell\">").append(getInvoiceSubjectText(header.getIns())).append("</td>\n");
        html.append("                </tr>\n");
        
        // Issue date and time
        html.append("                <tr>\n");
        html.append("                    <td class=\"label-cell\">تاریخ و زمان صدور:</td>\n");
        html.append("                    <td class=\"value-cell\" colspan=\"3\">").append(formatDateTime(header.getIndatim())).append("</td>\n");
        html.append("                </tr>\n");
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        return html.toString();
    }

    /**
     * Generate Seller Section in table format
     */
    private String generateSellerSection(InvoiceHeaderDto header, String sellerName) {
        if (header == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <table class=\"info-table\">\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th colspan=\"4\" class=\"section-title\">مشخصات فروشنده</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        if (sellerName != null && !sellerName.trim().isEmpty()) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">نام شخص حقیقی/حقوقی:</td>\n");
            html.append("                    <td class=\"value-cell\" colspan=\"3\">").append(escapeHtml(sellerName)).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTins() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">شماره اقتصادی:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(escapeHtml(header.getTins())).append("</td>\n");
            html.append("                    <td class=\"label-cell\">کد شعبه:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(header.getSbc() != null ? escapeHtml(header.getSbc()) : "").append("</td>\n");
            html.append("                </tr>\n");
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        return html.toString();
    }

    /**
     * Generate Buyer Section in table format
     */
    private String generateBuyerSection(InvoiceHeaderDto header, String buyerName) {
        if (header == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <table class=\"info-table\">\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th colspan=\"4\" class=\"section-title\">مشخصات خریدار</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        if (buyerName != null && !buyerName.trim().isEmpty()) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">نام شخص حقیقی/حقوقی:</td>\n");
            html.append("                    <td class=\"value-cell\" colspan=\"3\">").append(escapeHtml(buyerName)).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getBid() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">شماره/شناسه ملی:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(escapeHtml(header.getBid())).append("</td>\n");
            html.append("                    <td class=\"label-cell\">کد شعبه:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(header.getBbc() != null ? escapeHtml(header.getBbc()) : "").append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTinb() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">شماره اقتصادی:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(escapeHtml(header.getTinb())).append("</td>\n");
            html.append("                    <td class=\"label-cell\">نوع:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(getBuyerTypeText(header.getTob())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getBpc() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">کد پستی:</td>\n");
            html.append("                    <td class=\"value-cell\" colspan=\"3\">").append(escapeHtml(header.getBpc())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        return html.toString();
    }

    /**
     * Generate Items Table with all required columns
     */
    private String generateItemsTable(List<InvoiceBodyDto> body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <table class=\"items-table\">\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th>ردیف</th>\n");
        html.append("                    <th>شناسه کالا/خدمت</th>\n");
        html.append("                    <th>شرح کالا/خدمت</th>\n");
        html.append("                    <th>واحد اندازه‌گیری</th>\n");
        html.append("                    <th>تعداد/مقدار</th>\n");
        html.append("                    <th>مبلغ واحد (ریال)</th>\n");
        html.append("                    <th>مبلغ تخفیف (ریال)</th>\n");
        html.append("                    <th>نرخ مالیات بر ارزش افزوده (%)</th>\n");
        html.append("                    <th>مبلغ مالیات بر ارزش افزوده (ریال)</th>\n");
        html.append("                    <th>نوع ارز</th>\n");
        html.append("                    <th>مبلغ کل (ریال)</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        for (int i = 0; i < body.size(); i++) {
            InvoiceBodyDto item = body.get(i);
            html.append("                <tr>\n");
            html.append("                    <td>").append(i + 1).append("</td>\n");
            html.append("                    <td>").append(item.getSstid() != null ? escapeHtml(item.getSstid()) : "").append("</td>\n");
            html.append("                    <td>").append(item.getSstt() != null ? escapeHtml(item.getSstt()) : "").append("</td>\n");
            html.append("                    <td>").append(item.getMu() != null ? escapeHtml(item.getMu()) : "").append("</td>\n");
            html.append("                    <td>").append(formatNumber(item.getAm())).append("</td>\n");
            html.append("                    <td>").append(formatCurrency(item.getFee())).append("</td>\n");
            html.append("                    <td>").append(formatCurrency(item.getTdis())).append("</td>\n");
            html.append("                    <td>").append(formatNumber(item.getVra())).append("</td>\n");
            html.append("                    <td>").append(formatCurrency(item.getVam())).append("</td>\n");
            html.append("                    <td>").append(item.getCut() != null ? escapeHtml(item.getCut()) : "").append("</td>\n");
            html.append("                    <td>").append(formatCurrency(item.getTsstam())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        return html.toString();
    }

    /**
     * Generate Totals Section with final amount in numbers and Persian words
     */
    private String generateTotalsSection(InvoiceHeaderDto header) {
        if (header == null) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("        <table class=\"totals-table\">\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th colspan=\"2\" class=\"section-title\">جمع کل</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        if (header.getTprdis() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">مبلغ کل کالا/خدمت:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(formatCurrency(header.getTprdis())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTdis() != null && header.getTdis().compareTo(BigDecimal.ZERO) > 0) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">مبلغ تخفیف:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(formatCurrency(header.getTdis())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTadis() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">جمع بعد از تخفیف:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(formatCurrency(header.getTadis())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTvam() != null) {
            html.append("                <tr>\n");
            html.append("                    <td class=\"label-cell\">مبلغ مالیات بر ارزش افزوده:</td>\n");
            html.append("                    <td class=\"value-cell\">").append(formatCurrency(header.getTvam())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        if (header.getTbill() != null) {
            html.append("                <tr class=\"total-final-row\">\n");
            html.append("                    <td class=\"label-cell final-label\">مبلغ نهایی قابل پرداخت:</td>\n");
            html.append("                    <td class=\"value-cell final-value\">").append(formatCurrency(header.getTbill())).append("</td>\n");
            html.append("                </tr>\n");
            html.append("                <tr class=\"total-final-row\">\n");
            html.append("                    <td class=\"label-cell final-label\">مبلغ نهایی به حروف:</td>\n");
            html.append("                    <td class=\"value-cell final-value\">").append(convertNumberToPersianWords(header.getTbill())).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        return html.toString();
    }

    /**
     * Get CSS styles for print-friendly, PDF-oriented layout
     */
    private String getCssStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            body {
                font-family: 'Tahoma', 'Arial', sans-serif;
                direction: rtl;
                background-color: #ffffff;
                padding: 10px;
                font-size: 9px;
                line-height: 1.4;
            }
            .invoice-container {
                max-width: 100%;
                margin: 0 auto;
                background: white;
                padding: 10px;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                margin-bottom: 15px;
                border: 1px solid #000000;
            }
            .invoice-header-table,
            .info-table,
            .items-table,
            .totals-table {
                border: 2px solid #000000;
            }
            th, td {
                border: 1px solid #000000;
                padding: 5px 8px;
                text-align: right;
                vertical-align: middle;
            }
            th {
                background-color: #e0e0e0;
                font-weight: bold;
                color: #000000;
                font-size: 9px;
            }
            td {
                color: #000000;
                font-size: 8px;
            }
            .header-title {
                font-size: 12px;
                text-align: center;
                padding: 10px;
                background-color: #d0d0d0;
            }
            .section-title {
                font-size: 10px;
                text-align: center;
                padding: 8px;
                background-color: #d0d0d0;
            }
            .label-cell {
                background-color: #f5f5f5;
                font-weight: bold;
                width: 30%;
                text-align: right;
            }
            .value-cell {
                background-color: #ffffff;
                text-align: right;
            }
            .items-table th,
            .items-table td {
                text-align: center;
                font-size: 7px;
                padding: 4px 5px;
            }
            .items-table th {
                background-color: #e0e0e0;
                font-weight: bold;
            }
            .total-final-row {
                border-top: 2px solid #000000;
            }
            .final-label {
                background-color: #d0d0d0;
                font-weight: bold;
                font-size: 10px;
            }
            .final-value {
                background-color: #d0d0d0;
                font-weight: bold;
                font-size: 10px;
            }
            @media print {
                body {
                    background-color: white;
                    padding: 0;
                }
                .invoice-container {
                    padding: 5px;
                }
                @page {
                    margin: 1cm;
                    size: A4;
                }
            }
            """;
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NUMBER_FORMAT.format(value);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NUMBER_FORMAT.format(value) + " ریال";
    }

    private String formatDateTime(Long timestamp) {
        if (timestamp == null) {
            return "";
        }
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            
            // Convert Gregorian to Jalali
            int[] jalali = gregorianToJalali(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth());
            
            // Format as yyyy/MM/dd HH:mm:ss
            String dateStr = String.format("%04d/%02d/%02d", jalali[0], jalali[1], jalali[2]);
            String timeStr = String.format("%02d:%02d:%02d", dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
            return dateStr + " " + timeStr;
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
    
    /**
     * Convert Gregorian date to Jalali (Persian) date
     * Algorithm based on common JDN (Julian Day Number) conversion
     */
    private int[] gregorianToJalali(int gy, int gm, int gd) {
        int gDayNo = 365 * (gy - 1600) + 
                     (gy - 1600 + 3) / 4 - 
                     (gy - 1600 + 99) / 100 + 
                     (gy - 1600 + 399) / 400;
        
        // Add days for months before current month
        int[] gregorianDaysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isGregorianLeapYear(gy)) {
            gregorianDaysInMonth[1] = 29;
        }
        
        for (int i = 0; i < gm - 1; i++) {
            gDayNo += gregorianDaysInMonth[i];
        }
        gDayNo += gd - 1;
        
        int jDayNo = gDayNo - 79;
        int jNp = jDayNo / 12053;
        jDayNo %= 12053;
        
        int jy = 979 + 33 * jNp + 4 * (jDayNo / 1461);
        jDayNo %= 1461;
        
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365;
            jDayNo = (jDayNo - 1) % 365;
        }
        
        int jm = 0;
        for (; jm < 11 && jDayNo >= JALALI_DAYS_IN_MONTH[jm]; jm++) {
            jDayNo -= JALALI_DAYS_IN_MONTH[jm];
        }
        
        int jd = jDayNo + 1;
        return new int[]{jy, jm + 1, jd};
    }
    
    private boolean isGregorianLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private String getInvoiceTypeText(Integer inty) {
        if (inty == null) return "";
        return switch (inty) {
            case 1 -> "نوع اول";
            case 2 -> "نوع دوم";
            case 3 -> "نوع سوم";
            default -> "";
        };
    }

    private String getInvoiceSubjectText(Integer ins) {
        if (ins == null) return "";
        return switch (ins) {
            case 1 -> "اصلی";
            case 2 -> "اصلاحی";
            case 3 -> "ابطالی";
            case 4 -> "برگشت";
            default -> "";
        };
    }

    private String getBuyerTypeText(Integer tob) {
        if (tob == null) return "";
        return switch (tob) {
            case 1 -> "حقیقی";
            case 2 -> "حقوقی";
            case 3 -> "مشارکت مدنی";
            case 4 -> "اتباع غیر ایرانی";
            default -> "";
        };
    }

    /**
     * Convert number to Persian words
     * This is a placeholder method - actual implementation should be added
     * 
     * @param amount The amount to convert
     * @return Persian words representation of the number
     */
    private String convertNumberToPersianWords(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        // TODO: Implement number to Persian words conversion
        // This is a hook for future implementation
        // For now, return empty string or placeholder
        return ""; // Placeholder - implement actual conversion logic
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
