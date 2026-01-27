package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.DraftInvoiceService;
import ir.tax.trustedtaxpayers.biz.service.StructuredImportService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping({"/api/import", "/api/v1/import"})
@Tag(name = "Structured Import", description = "آپلود و پارس فایل‌های CSV/XLSX/JSON برای ایجاد پیش‌نویس صورتحساب")
@Validated
@RequiredArgsConstructor
public class StructuredImportController {

    private final StructuredImportService importService;
    private final DraftInvoiceService draftService;

    @Operation(summary = "آپلود و پارس فایل ساخت‌یافته")
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@NotNull @RequestPart("file") MultipartFile file) throws Exception {
        Map<String, Object> normalized = importService.parse(file);
        var draft = draftService.create(null, null, normalized);
        return ResponseEntity.ok(Map.of(
                "parsed", normalized,
                "draftId", draft.getId()
        ));
    }
}


