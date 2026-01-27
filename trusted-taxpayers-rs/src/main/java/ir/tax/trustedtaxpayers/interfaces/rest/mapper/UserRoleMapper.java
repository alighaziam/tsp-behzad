package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserRoleEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UserRoleDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {
    UserRoleDto toDto(UserRoleEntity entity);
    
    List<UserRoleDto> toDtos(List<UserRoleEntity> entities);
}


