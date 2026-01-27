package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends BaseRepository<RoleEntity, UUID> {
    Optional<RoleEntity> findByName(String name);
    Optional<RoleEntity> findByNameIgnoreCase(String name);
    List<RoleEntity> findByNameContainingIgnoreCase(String name);
    
    @Query(value = "SELECT * FROM ROLES WHERE " +
           "UPPER(NAME) LIKE UPPER(CONCAT('%', :name, '%')) OR " +
           "(DESCRIPTION IS NOT NULL AND UPPER(CAST(DESCRIPTION AS VARCHAR(4000))) LIKE UPPER(CONCAT('%', :description, '%')))",
           nativeQuery = true)
    List<RoleEntity> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            @Param("name") String name, 
            @Param("description") String description);
}



