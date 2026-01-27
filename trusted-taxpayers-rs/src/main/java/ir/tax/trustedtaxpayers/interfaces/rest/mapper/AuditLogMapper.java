package ir.tax.trustedtaxpayers.interfaces.rest.mapper;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.AuditLogDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreateAuditLogRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface AuditLogMapper {
    @Mapping(target = "eventTime", source = "createdOn")
    AuditLogDto toDto(AuditLogEntity entity);
    
    List<AuditLogDto> toDtos(List<AuditLogEntity> entities);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "idString", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AuditLogEntity toEntity(CreateAuditLogRequest request);
}


