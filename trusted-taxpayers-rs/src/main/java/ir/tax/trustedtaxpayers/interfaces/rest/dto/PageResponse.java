package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "ساختار صفحه‌بندی داده‌ها")
public class PageResponse<T> {

    @Schema(description = "لیست داده‌ها در صفحه فعلی")
    private List<T> content;

    @Schema(description = "شماره صفحه فعلی (صفرمبنا)")
    private int page;

    @Schema(description = "تعداد رکورد در هر صفحه")
    private int size;

    @Schema(description = "تعداد کل رکوردها")
    private long totalElements;

    @Schema(description = "تعداد کل صفحات")
    private int totalPages;

    public PageResponse() {}

    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}


