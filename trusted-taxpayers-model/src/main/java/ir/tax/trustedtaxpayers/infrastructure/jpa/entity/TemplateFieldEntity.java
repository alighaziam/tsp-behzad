package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.ComponentType;
import ir.tax.trustedtaxpayers.enums.InvoiceStep;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TEMPLATE_FIELD")
@Getter
@Setter
public class TemplateFieldEntity extends BaseEntity {

    @Column(name = "TSP_CODE", length = 100, nullable = false, unique = true)
    private String tspCode;

    @Column(name = "TAX_CODE", length = 100, nullable = false, unique = true)
    private String taxCode;

    @Column(name = "TITLE", length = 250, nullable = false)
    private String title;

    @Column(name = "REGEX_PATTERN", length = 500)
    private String regexPattern;

    @Column(name = "ERROR_MESSAGE_FA", length = 500)
    private String errorMessageFa;

    @Column(name = "SECOND_ERROR_MESSAGE", length = 100)
    private String secondErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "COMPONENT_TYPE", length = 50)
    private ComponentType componentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "INVOICE_STEP")
    private InvoiceStep invoiceStep;
}


