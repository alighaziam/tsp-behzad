package ir.tax.trustedtaxpayers.beans.dto.tax;

import java.math.BigDecimal;

public class PaymentDto {

    /**
     * IinNumber
     */
    private String iinn;

    /**
     * acceptorNumber
     */
    private String acn;

    /**
     * terminalNumber
     */
    private String trmn;

    /**
     * trackingNumber
     */
    private String trn;

    /**
     * payerCardNumber
     */
    private String pcn;

    /**
     * payerId
     */
    private String pid;

    /**
     * vat payment id
     */
    private String vatpid;

    /**
     * vat bill id
     */
    private String vatbid;

    /**
     * payDateTime
     */
    private Long pdt;

    /**
     * paymentValue
     */
    private BigDecimal pv;

    /**
     * paymentMethod
     */
    private Integer pmt;

    public String getIinn() {
        return iinn;
    }

    public void setIinn(String iinn) {
        this.iinn = iinn;
    }

    public String getAcn() {
        return acn;
    }

    public void setAcn(String acn) {
        this.acn = acn;
    }

    public String getTrmn() {
        return trmn;
    }

    public void setTrmn(String trmn) {
        this.trmn = trmn;
    }

    public String getTrn() {
        return trn;
    }

    public void setTrn(String trn) {
        this.trn = trn;
    }

    public String getPcn() {
        return pcn;
    }

    public void setPcn(String pcn) {
        this.pcn = pcn;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public Long getPdt() {
        return pdt;
    }

    public void setPdt(Long pdt) {
        this.pdt = pdt;
    }

    public BigDecimal getPv() {
        return pv;
    }

    public void setPv(BigDecimal pv) {
        this.pv = pv;
    }

    public Integer getPmt() {
        return pmt;
    }

    public void setPmt(Integer pmt) {
        this.pmt = pmt;
    }

    public String getVatbid() {
        return vatbid;
    }

    public void setVatbid(String vatbid) {
        this.vatbid = vatbid;
    }

    public String getVatpid() {
        return vatpid;
    }

    public void setVatpid(String vatpid) {
        this.vatpid = vatpid;
    }
}
