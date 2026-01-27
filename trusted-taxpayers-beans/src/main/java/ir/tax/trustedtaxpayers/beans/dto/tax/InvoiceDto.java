package ir.tax.trustedtaxpayers.beans.dto.tax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class InvoiceDto {

    private InvoiceHeaderDto header;

    private List<InvoiceBodyDto> body;

    private List<PaymentDto> payments;

    private List<InvoiceExtension> extension;

}
