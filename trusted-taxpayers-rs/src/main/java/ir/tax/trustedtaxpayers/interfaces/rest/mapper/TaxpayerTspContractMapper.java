package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTspContractEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerTspContractDto;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TaxpayerTspContractMapper {

    public TaxpayerTspContractDto toDto(TaxpayerTspContractEntity entity) {
        return new TaxpayerTspContractDto(
                Objects.nonNull(entity.getId()) ? entity.getId().toString() : "",
                Objects.nonNull(entity.getTaxpayerId()) ? entity.getTaxpayerId().toString() : "",
                Objects.nonNull(entity.getProviderId()) ? entity.getProviderId().toString() : "",
                Objects.nonNull(entity.getProvider()) && Objects.nonNull(entity.getProvider().getName())
                        ? entity.getProvider().getName()
                        : "",
                Objects.nonNull(entity.getContractNumber()) ? entity.getContractNumber() : "",
                Objects.nonNull(entity.getContractTitle()) ? entity.getContractTitle() : "",
                Objects.nonNull(entity.getSignedAt()) ? entity.getSignedAt() : null,
                Objects.nonNull(entity.getExpiresAt()) ? entity.getExpiresAt() : null,
                Objects.nonNull(entity.getStatus()) ? entity.getStatus() : null,
                Objects.nonNull(entity.getNotes()) ? entity.getNotes() : "",
                Objects.nonNull(entity.getIsActive()) ? entity.getIsActive() : false
        );
    }

}


