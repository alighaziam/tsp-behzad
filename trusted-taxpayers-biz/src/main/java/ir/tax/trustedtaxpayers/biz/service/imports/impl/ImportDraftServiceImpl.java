package ir.tax.trustedtaxpayers.biz.service.imports.impl;

import ir.tax.trustedtaxpayers.application.service.InvoiceFormSubmissionService;
import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceDto;
import ir.tax.trustedtaxpayers.biz.service.imports.ImportDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportDraftServiceImpl implements ImportDraftService {

    private final InvoiceFormSubmissionService invoiceFormSubmissionService;

    @Override
    public List<Result> createDrafts(List<NormalizedInvoiceDto> invoices, Integer patternNumber) {
        List<Result> results = new ArrayList<>();
        if (invoices == null || invoices.isEmpty()) {
            return results;
        }
        int defaultPattern = patternNumber != null ? patternNumber : 13; // الگوی پیش‌فرض
        for (int i = 0; i < invoices.size(); i++) {
            var inv = invoices.get(i);
            try {
                Map<String, Object> header = new HashMap<>();
                // inty: نوع صورت‌حساب (۱ فروش عادی) - اختیاری
                header.put("inty", 1);
                // اگر sellerTaxId وجود دارد، در هدر بگذاریم (سیستم ممکن است نگاشت دهد)
                if (inv.getSellerTaxId() != null) {
                    header.put("tins", inv.getSellerTaxId());
                }
                if (inv.getBuyerTaxId() != null) {
                    header.put("bins", inv.getBuyerTaxId());
                }
                if (inv.getInvoiceNumber() != null) {
                    header.put("inno", inv.getInvoiceNumber());
                }
                if (inv.getIssueDate() != null) {
                    // تاریخ را به صورت رشته می‌گذاریم؛ سرویس پیش‌نویس ولیدیشن سبک دارد
                    header.put("indati2m_str", inv.getIssueDate());
                }
                if (inv.getCurrency() != null) {
                    header.put("cur", inv.getCurrency());
                }
                if (inv.getTotalAmount() != null) {
                    header.put("tbill", inv.getTotalAmount());
                }

                List<Map<String, Object>> items = new ArrayList<>();
                if (inv.getItems() != null) {
                    for (var it : inv.getItems()) {
                        Map<String, Object> row = new HashMap<>();
                        // sstt: شرح قلم
                        row.put("sstt", it.getItemName());
                        // am: مقدار
                        row.put("am", it.getItemQty());
                        // fee: فی
                        row.put("fee", it.getItemUnitPrice());
                        items.add(row);
                    }
                }

                var res = invoiceFormSubmissionService.saveDraft(defaultPattern, header, items);
                results.add(new Result(res.headerId, res.taxId, i));
            } catch (Exception e) {
                log.error("Failed to create draft for imported invoice index {}", i, e);
            }
        }
        return results;
    }
}


