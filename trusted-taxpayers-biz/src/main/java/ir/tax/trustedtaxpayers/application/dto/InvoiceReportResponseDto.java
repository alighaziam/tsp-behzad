package ir.tax.trustedtaxpayers.application.dto;

import java.util.List;

public record InvoiceReportResponseDto(
        List<InvoiceReportItemDto> items,
        long total,
        int page,
        int size,
        List<InvoiceStatusSummaryDto> summary,
        List<ReportTaxpayerDto> taxpayers
) {
}



