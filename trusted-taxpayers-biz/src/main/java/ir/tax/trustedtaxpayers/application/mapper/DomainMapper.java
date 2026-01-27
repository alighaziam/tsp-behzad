package ir.tax.trustedtaxpayers.application.mapper;

import ir.tax.trustedtaxpayers.beans.model.Buyer;
import ir.tax.trustedtaxpayers.beans.model.Taxpayer;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DomainMapper {
    Taxpayer toTaxpayer(TaxpayerEntity entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    /*@Mapping(target = "taxId", source = "taxpayerNo")*/
    @Mapping(target = "name", source = "name")
    @Mapping(target = "type", ignore = true)
    /*@Mapping(target = "buyerType", ignore = true)*/
    @Mapping(target = "nationalId", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "province", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "vatRegisterNo", ignore = true)
    /*@Mapping(target = "buyer", ignore = true)*/
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TaxpayerEntity toTaxpayerEntity(Taxpayer domain);

    default Buyer toBuyer(TaxpayerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Buyer(
                entity.getId(),
                "Buyer",
                entity.getNationalId(),
                entity.getName(),
                entity.getTaxId(),
                entity.getIsActive()
        );
    }

    default TaxpayerEntity toBuyerEntity(Buyer domain) {
        if (domain == null) {
            return null;
        }
        String buyerType = domain.getBuyerType();
        String taxpayerNo = domain.getNationalId() != null
                ? "BUYER-" + domain.getNationalId()
                : "BUYER-" + domain.getId();

        return TaxpayerEntity.builder()
                .id(domain.getId())
                .nationalId(domain.getNationalId())
                .name(domain.getName())
                .type(null) // Will be set by BuyerService.prepareBuyerEntity based on buyerType
                .taxId(taxpayerNo)
                .isActive(domain.isActive())
                .build();
    }
}


