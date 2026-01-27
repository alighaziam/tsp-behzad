package ir.tax.trustedtaxpayers.interfaces.rest;

import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceItemService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceItemEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceItemRepository;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت اقلام صورتحساب برای دسترسی به خطوط کالا/خدمت و جستجوهای سفارشی.
 * <p>
 * REST controller providing endpoints to list invoice items and filter them by header or codes.
 */
@RestController
@RequestMapping("/api/electronic-invoice-items")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Electronic Invoice Items", description = "مدیریت اقلام صورتحساب الکترونیکی\n\nراهنمای سریع:\n- Base: /api/electronic-invoice-items\n- همه: /all | بر اساس Header: /by-header/{headerId} | sstid: /by-sstid/{sstid}\n- جستجوی سفارشی (page): POST /search-custom?page=0&size=10 → [{key,operand,value}]\n")
public class ElectronicInvoiceItemController extends BaseController<ElectronicInvoiceItemEntity, UUID> {
    private final ElectronicInvoiceItemService itemService;
    private final ElectronicInvoiceItemRepository itemRepository;
    private final ElectronicInvoiceMapper mapper;

    public ElectronicInvoiceItemController(
            ElectronicInvoiceItemService itemService,
            ElectronicInvoiceItemRepository itemRepository,
            ElectronicInvoiceMapper mapper) {
        super(itemService, itemRepository);
        this.itemService = itemService;
        this.itemRepository = itemRepository;
        this.mapper = mapper;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ElectronicInvoiceItemEntity>> getAll() {
        return ResponseEntity.ok(itemService.findAll());
    }

    @GetMapping("/by-header/{headerId}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto>> findByHeaderId(@PathVariable UUID headerId) {
        var items = itemService.findByHeaderId(headerId);
        return ResponseEntity.ok(items.stream().map(mapper::toItemDto).toList());
    }

    @GetMapping("/by-sstid/{sstid}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto>> findBySstid(@PathVariable String sstid) {
        var items = itemService.findBySstid(sstid);
        return ResponseEntity.ok(items.stream().map(mapper::toItemDto).toList());
    }

    @io.swagger.v3.oas.annotations.Operation(
            summary = "جستجوی پیشرفته اقلام (سفارشی)",
            description = "Body می‌تواند یک فیلتر یا آرایه‌ای از فیلترها باشد.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FilterRequest.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "فیلتر بر اساس HEADER_ID",
                                            value = "{\n  \"key\": \"headerId\",\n  \"operand\": \"=\",\n  \"value\": \"8f1b3c58-4d3d-4b96-a3b7-6a2b9f8cb6a3\"\n}"
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "چند فیلتر (SSTID و LINE_NO)",
                                            value = "[\n  {\n    \"key\": \"sstid\",\n    \"operand\": \"=\",\n    \"value\": \"100200\"\n  },\n  {\n    \"key\": \"lineNo\",\n    \"operand\": \">=\",\n    \"value\": 1\n  }\n]"
                                    )
                            }
                    )
            )
    )
    @PostMapping(value = "/search-custom", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<org.springframework.data.domain.Page<ElectronicInvoiceItemEntity>> searchCustom(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<FilterRequest> filters,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        return (org.springframework.http.ResponseEntity) super.search(filters, page, size, request);
    }
}



