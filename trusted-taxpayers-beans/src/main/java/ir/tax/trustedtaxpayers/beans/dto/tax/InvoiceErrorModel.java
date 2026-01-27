package ir.tax.trustedtaxpayers.beans.dto.tax;

import java.util.List;

public class InvoiceErrorModel {

    private String code;
    private String message;
    private List<Object> detail;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Object> getDetail() {
        return detail;
    }

    public void setDetail(List<Object> detail) {
        this.detail = detail;
    }
}
