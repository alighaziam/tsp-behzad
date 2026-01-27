package ir.tax.trustedtaxpayers.interfaces.rest.util;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class PageHeaderBuilder {
    private String requestUri;
    private int pageNumber;
    private int size;

    public static PageHeaderBuilder create() { return new PageHeaderBuilder(); }

    public PageHeaderBuilder requestUri(String requestUri) { this.requestUri = requestUri; return this; }
    public PageHeaderBuilder pageNumber(int pageNumber) { this.pageNumber = pageNumber; return this; }
    public PageHeaderBuilder size(int size) { this.size = size; return this; }

    public HttpHeaders build(Page<?> page) {
        HttpHeaders headers = new HttpHeaders();
        int totalPages = page.getTotalPages();
        List<String> links = new ArrayList<>();
        BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + requestUri + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";

        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (pageNumber > 0) {
                links.add(String.format(linkOf.apply(pageNumber - 1, size), "prev"));
            }
            if (pageNumber + 1 < totalPages) {
                links.add(String.format(linkOf.apply(pageNumber + 1, size), "next"));
            }
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }

        if (!links.isEmpty()) {
            headers.add(HttpHeaders.LINK, String.join(", ", links));
        }
        headers.add("X-Total-Count", String.valueOf(page.getTotalElements()));
        return headers;
    }
}


