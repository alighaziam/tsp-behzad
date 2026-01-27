package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TrustedServiceProviderEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TrustedServiceProviderDto;
import org.springframework.stereotype.Component;

@Component
public class TrustedServiceProviderMapper {

    public TrustedServiceProviderDto toDto(TrustedServiceProviderEntity entity) {
        return new TrustedServiceProviderDto(
                entity.getId().toString(),
                entity.getCode(),
                entity.getName(),
                entity.getContactName(),
                entity.getContactPhone(),
                entity.getContactEmail(),
                entity.getWebsite(),
                entity.getIsActive()
        );
    }
}


