package ir.tax.trustedtaxpayers.beans.dto.tax;


import ir.gov.tax.tpis.sdk.content.enumeration.FiscalStatus;

import java.math.BigDecimal;


public class FiscalFullInformationModel {

    private String nameTrade;
    private FiscalStatus fiscalStatus;
    private BigDecimal saleThreshold;
    private String codeEconomic;
    private String nationalId;

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(final String nationalId) {
        this.nationalId = nationalId;
    }

    public String getNameTrade() {
        return nameTrade;
    }

    public void setNameTrade(String nameTrade) {
        this.nameTrade = nameTrade;
    }

    public FiscalStatus getFiscalStatus() {
        return fiscalStatus;
    }

    public void setFiscalStatus(FiscalStatus fiscalStatus) {
        this.fiscalStatus = fiscalStatus;
    }

    public BigDecimal getSaleThreshold() {
        return saleThreshold;
    }

    public void setSaleThreshold(BigDecimal saleThreshold) {
        this.saleThreshold = saleThreshold;
    }

    public String getCodeEconomic() {
        return codeEconomic;
    }

    public void setCodeEconomic(String codeEconomic) {
        this.codeEconomic = codeEconomic;
    }
}
