package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.RoleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDto toDto(RoleEntity role);
}


