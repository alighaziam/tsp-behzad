package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateTaxpayerRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface TaxpayerMapper {

    @Mapping(target = "id", expression = "java(entity.getId() != null ? entity.getId().toString() : null)")
    @Mapping(target = "type", expression = "java(entity.getType() != null ? entity.getType().getTitle() : null)")
    @Mapping(target = "typeId", expression = "java(entity.getType() != null ? entity.getType().getId().toString() : null)")
    TaxpayerDto toDto(TaxpayerEntity entity);

    List<TaxpayerDto> toDtos(List<TaxpayerEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TaxpayerEntity toEntity(TaxpayerDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    TaxpayerEntity toEntity(CreateTaxpayerRequest request);
}