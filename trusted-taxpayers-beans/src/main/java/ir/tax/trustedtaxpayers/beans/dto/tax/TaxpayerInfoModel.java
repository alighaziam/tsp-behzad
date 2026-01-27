package ir.tax.trustedtaxpayers.beans.dto.tax;


public class TaxpayerInfoModel {

    private String taxpayerName;
    private Integer taxpayerProfileStatus;
    private Boolean taxpayerCallStatus;
    private Boolean taxpayerArticle2Status;

    @Override
    public String toString() {
        return "TaxpayerInfoModel{" +
               "taxpayerName='" + taxpayerName + '\'' +
               ", taxpayerProfileStatus=" + taxpayerProfileStatus +
               ", taxpayerCallStatus=" + taxpayerCallStatus +
               ", taxpayerArticle2Status=" + taxpayerArticle2Status +
               '}';
    }

    public String getTaxpayerName() {
        return taxpayerName;
    }

    public void setTaxpayerName(final String taxpayerName) {
        this.taxpayerName = taxpayerName;
    }

    public Integer getTaxpayerProfileStatus() {
        return taxpayerProfileStatus;
    }

    public void setTaxpayerProfileStatus(final Integer taxpayerProfileStatus) {
        this.taxpayerProfileStatus = taxpayerProfileStatus;
    }

    public Boolean getTaxpayerCallStatus() {
        return taxpayerCallStatus;
    }

    public void setTaxpayerCallStatus(final Boolean taxpayerCallStatus) {
        this.taxpayerCallStatus = taxpayerCallStatus;
    }

    public Boolean getTaxpayerArticle2Status() {
        return taxpayerArticle2Status;
    }

    public void setTaxpayerArticle2Status(final Boolean taxpayerArticle2Status) {
        this.taxpayerArticle2Status = taxpayerArticle2Status;
    }
}
