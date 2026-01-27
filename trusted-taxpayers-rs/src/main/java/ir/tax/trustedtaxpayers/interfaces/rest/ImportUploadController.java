package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceDto;
import ir.tax.trustedtaxpayers.biz.service.imports.ImportDraftService;
import ir.tax.trustedtaxpayers.biz.service.imports.ImportParserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/import")
public class ImportUploadController {

    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB
    private final ImportParserService importParserService;
    private final ImportDraftService importDraftService;

    @Operation(
            summary = "آپلود و پارس فایل ایمپورت",
            description = "فایل CSV/JSON/XLSX را دریافت و به مدل نرمال‌شده تبدیل می‌کند.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "لیست فاکتورهای نرمال‌شده",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = NormalizedInvoiceDto.class)))),
                    @ApiResponse(responseCode = "400", description = "فایل/نوع نامعتبر", content = @Content)
            }
    )
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "createDraft", required = false, defaultValue = "false") boolean createDraft,
            @RequestParam(name = "patternNumber", required = false) Integer patternNumber
    ) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_UPLOAD_SIZE_BYTES) {
            return ResponseEntity.badRequest().build();
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "";
        String t = (type != null ? type : "").trim().toLowerCase();
        List<NormalizedInvoiceDto> result;

        if ("application/json".equalsIgnoreCase(contentType) || "json".equals(t) || file.getOriginalFilename().toLowerCase().endsWith(".json")) {
            result = importParserService.parseJson(file);
        } else if ("text/csv".equalsIgnoreCase(contentType) || "csv".equals(t) || file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            result = importParserService.parseCsv(file);
        } else if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equalsIgnoreCase(contentType)
                || "xlsx".equals(t) || file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            result = importParserService.parseXlsx(file);
        } else {
            return ResponseEntity.badRequest().build();
        }
        if (createDraft) {
            var drafts = importDraftService.createDrafts(result, patternNumber);
            return ResponseEntity.ok(drafts);
        } else {
            return ResponseEntity.ok(result);
        }
    }
}


