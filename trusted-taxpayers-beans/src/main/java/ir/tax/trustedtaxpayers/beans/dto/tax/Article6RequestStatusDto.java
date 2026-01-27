package ir.tax.trustedtaxpayers.beans.dto.tax;


public class Article6RequestStatusDto {

    private String economicCode;
    private Long vatValue;
    private Integer period;

    public Article6RequestStatusDto(final String economicCode, final Long vatValue, final Integer period) {
        this.economicCode = economicCode;
        this.vatValue = vatValue;
        this.period = period;
    }

    @Override
    public String toString() {
        return "Article6StatusModel{" +
               "economicCode='" + economicCode + '\'' +
               ", vatValue=" + vatValue +
               ", period=" + period +
               '}';
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(final Integer period) {
        this.period = period;
    }

    public Long getVatValue() {
        return vatValue;
    }

    public void setVatValue(final Long vatValue) {
        this.vatValue = vatValue;
    }

    public String getEconomicCode() {
        return economicCode;
    }

    public void setEconomicCode(final String economicCode) {
        this.economicCode = economicCode;
    }
}
