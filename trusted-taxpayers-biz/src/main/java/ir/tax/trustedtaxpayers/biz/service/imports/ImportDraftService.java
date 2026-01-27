package ir.tax.trustedtaxpayers.biz.service.imports;


import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceDto;

import java.util.List;

public interface ImportDraftService {
    List<Result> createDrafts(List<NormalizedInvoiceDto> invoices, Integer patternNumber);

    class Result {
        public final java.util.UUID headerId;
        public final String taxId;
        public final int index;

        public Result(java.util.UUID headerId, String taxId, int index) {
            this.headerId = headerId;
            this.taxId = taxId;
            this.index = index;
        }
    }
}


