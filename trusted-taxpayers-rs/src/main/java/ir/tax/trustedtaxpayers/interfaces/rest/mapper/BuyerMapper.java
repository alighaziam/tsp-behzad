package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.BuyerDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateBuyerRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface BuyerMapper {
    @Mapping(target = "id", expression = "java(ir.tax.trustedtaxpayers.interfaces.rest.util.EntityDtoMapper.uuidToString(entity.getId()))")
    /*@Mapping(target = "taxpayerNo", source = "taxpayerNo")*/
    @Mapping(target = "active", source = "isActive")
    BuyerDto toDto(TaxpayerEntity entity);
    
    List<BuyerDto> toDtos(List<TaxpayerEntity> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taxId", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "province", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "vatRegisterNo", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    /*@Mapping(target = "buyer", expression = "java(true)")*/
    /*@Mapping(target = "buyerType", source = "buyerType")*/
    @Mapping(target = "nationalId", source = "nationalId")
    @Mapping(target = "name", source = "name")
    TaxpayerEntity toEntity(CreateBuyerRequest request);

    /*@Mapping(target = "buyerType", source = "request.buyerType")*/
    @Mapping(target = "nationalId", source = "request.nationalId")
    @Mapping(target = "name", source = "request.name")
    void update(@MappingTarget TaxpayerEntity target, CreateBuyerRequest request);
}


