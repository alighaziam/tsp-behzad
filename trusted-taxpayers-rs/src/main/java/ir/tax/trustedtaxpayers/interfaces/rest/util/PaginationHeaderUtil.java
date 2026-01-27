package ir.tax.trustedtaxpayers.interfaces.rest.util;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

/**
 * Utility for adding RFC5988 pagination Link headers and X-Total-Count.
 */
public final class PaginationHeaderUtil {

    private PaginationHeaderUtil() {}

    public static HttpHeaders buildLinkHeaders(Page<?> page, int pageNumber, int size, String requestUri) {
        return PageHeaderBuilder.create()
                .requestUri(requestUri)
                .pageNumber(pageNumber)
                .size(size)
                .build(page);
    }
}


