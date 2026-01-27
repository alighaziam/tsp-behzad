package ir.tax.trustedtaxpayers.beans.dto.tax;

import java.util.List;


public class InquiryByReferenceNumberDto {

    private List<String> referenceNumber;

    public List<String> getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(List<String> referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}
