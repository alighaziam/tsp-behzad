package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.beans.model.UserSummary;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.UserDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @org.mapstruct.Mapping(target = "id", expression = "java(ir.tax.trustedtaxpayers.interfaces.rest.util.EntityDtoMapper.uuidToString(entity.getId()))")
    UserDto toDto(UserEntity entity);
    
    UserDto toDto(UserSummary summary);
    
    List<UserDto> toDtos(List<UserEntity> entities);
    
    List<UserDto> toDtosFromSummary(List<UserSummary> summaries);
}


