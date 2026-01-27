package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TaxpayerTemplateUsageId implements Serializable {
    private UUID taxpayerId;
    private UUID templateId;
}





