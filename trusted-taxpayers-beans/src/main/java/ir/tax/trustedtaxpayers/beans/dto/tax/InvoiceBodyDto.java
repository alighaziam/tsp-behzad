package ir.tax.trustedtaxpayers.beans.dto.tax;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvoiceBodyDto {
    private String sstid;

    /**
     * serviceStuffTitle
     */
    private String sstt;

    /**
     * measurementUnit
     */
    private String mu;

    /**
     * amount
     */
    private BigDecimal am;

    /**
     * fee
     */
    private BigDecimal fee;

    /**
     * currencyFee
     */
    private BigDecimal cfee;

    /**
     * currencyType
     */
    private String cut;

    /**
     * exchangeRate
     */
    private BigDecimal exr;

    /**
     * preDiscount
     */
    private BigDecimal prdis;

    /**
     * discount
     */
    private BigDecimal tdis;

    /**
     * afterDiscount
     */
    private BigDecimal adis;

    /**
     * vatRate
     */
    private BigDecimal vra;

    /**
     * vatAmount
     */
    private BigDecimal vam;

    /**
     * overDutyTitle
     */
    private String odt;

    /**
     * overDutyRate
     */
    private BigDecimal odr;

    /**
     * overDutyAmount
     */
    private BigDecimal odam;

    /**
     * otherLegalTitle
     */
    private String olt;

    /**
     * otherLegalRate
     */
    private BigDecimal olr;

    /**
     * otherLegalAmount
     */
    private BigDecimal olam;

    /**
     * constructionFee
     */
    private BigDecimal consfee;

    /**
     * sellerProfit
     */
    private BigDecimal spro;

    /**
     * brokerSalary
     */
    private BigDecimal bros;

    /**
     * totalConstructionProfitBrokerSalary
     */
    private BigDecimal tcpbs;

    /**
     * cashOfPayment
     */
    private BigDecimal cop;

    /**
     * buyerSRegisterNumber
     */
    private String bsrn;

    /**
     * vatOfPayment
     */
    private BigDecimal vop;

    /**
     * totalServiceStuffAmount
     */
    private BigDecimal tsstam;

    /**
     * وزن خالص
     */
    private BigDecimal nw;

    /**
     * ارزش ریالی کالا
     */
    private BigDecimal ssrv;

    /**
     * ارزش ارزی کالا
     */
    private BigDecimal sscv;

    /**
     * عیار طلا
     */
    private BigDecimal cui;

    /**
     * کد HS قلم کالا
     */
    private String hs;

    /**
     *نرخ خرید ارز
     */
    private BigDecimal cpr;

    /**
     * ماخذ مالیات بر ارزش افزوده در الگوی فروش ارز
     */
    private Long sovat ;

    /**
     * vat base amount
     */
    private BigDecimal vba;

}
