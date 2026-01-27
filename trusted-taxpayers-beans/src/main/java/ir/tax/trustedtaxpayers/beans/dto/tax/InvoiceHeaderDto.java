package ir.tax.trustedtaxpayers.beans.dto.tax;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceHeaderDto {

    private Integer cdcd;
    private BigDecimal tonw;
    private BigDecimal torv;
    private BigDecimal tocv;
    private String tinc;

    private Long indati2m;
    private Long indatim;
    private Integer inty;
    private Integer ft;
    private String inno;
    private String irtaxid;
    private String scln;
    private Integer setm;
    private String tins;
    private BigDecimal cap;
    private String bid;
    private BigDecimal insp;
    private BigDecimal tvop;
    private String bpc;
    private BigDecimal tax17;
    private String taxid;
    private Integer inp;
    private String scc;
    private Integer ins;
    private String billid;
    private BigDecimal tprdis;
    private BigDecimal tdis;
    private BigDecimal tadis;
    private BigDecimal tvam;
    private BigDecimal todam;
    private BigDecimal tbill;
    private Integer tob;

    private String tinb;
    private String sbc;
    private String bbc;
    private String bpn;
    private String crn;
    private String cdcn;
    private String lno;
    private String lrno;
    private String ocu;
    private String oci;
    private String dco;
    private String dci;
    private String tid;
    private String rid;
    private Byte lt;
    private String cno;
    private String did;
    private String asn;
    private Long asd;
    private String in;
    private String an;

}
