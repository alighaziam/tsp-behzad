package ir.tax.trustedtaxpayers.application.usecase;

import ir.tax.trustedtaxpayers.beans.model.Taxpayer;
import ir.tax.trustedtaxpayers.model.ports.TaxpayerQueryPort;
import ir.tax.trustedtaxpayers.application.SearchResult;

public class SearchTaxpayersUseCase {
    private final TaxpayerQueryPort taxpayerQueryPort;

    public SearchTaxpayersUseCase(TaxpayerQueryPort taxpayerQueryPort) {
        this.taxpayerQueryPort = taxpayerQueryPort;
    }

    public SearchResult<Taxpayer> execute(String queryText, Integer page, Integer size, String sortBy, Boolean ascending) {
        if (queryText == null) {
            return SearchResult.empty();
        }
        String normalized = normalizeQuery(queryText);
        if (normalized.isBlank() || normalized.length() < 2) {
            return SearchResult.empty();
        }
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        String sort = (sortBy == null || sortBy.isBlank()) ? "name" : sortBy;
        boolean asc = ascending == null || Boolean.TRUE.equals(ascending);
        return taxpayerQueryPort.searchByQuery(normalized, p, s, sort, asc);
    }

    private String normalizeQuery(String input) {
        String trimmed = input.trim().replaceAll("\\s+", " ");
        // Convert Persian/Arabic digits to Latin 0-9 to match DB content
        StringBuilder sb = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            // Arabic-Indic digits 0x0660-0x0669
            if (c >= '\u0660' && c <= '\u0669') {
                sb.append((char) ('0' + (c - '\u0660')));
            // Extended Arabic-Indic digits 0x06F0-0x06F9 (Persian)
            } else if (c >= '\u06F0' && c <= '\u06F9') {
                sb.append((char) ('0' + (c - '\u06F0')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}



