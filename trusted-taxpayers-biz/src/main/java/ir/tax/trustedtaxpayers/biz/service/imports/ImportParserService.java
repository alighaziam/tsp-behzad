package ir.tax.trustedtaxpayers.biz.service.imports;

import ir.tax.trustedtaxpayers.beans.dto.tax.NormalizedInvoiceDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImportParserService {
    List<NormalizedInvoiceDto> parseCsv(MultipartFile file);
    List<NormalizedInvoiceDto> parseJson(MultipartFile file);
    List<NormalizedInvoiceDto> parseXlsx(MultipartFile file);
}


