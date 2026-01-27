package ir.tax.trustedtaxpayers.application;

import java.util.List;

public record SearchResult<T>(List<T> items, long total, int page, int size) {
    public static <T> SearchResult<T> empty() {
        return new SearchResult<>(List.of(), 0, 0, 0);
    }
}



