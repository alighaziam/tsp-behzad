package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateTypeRepository extends BaseRepository<TemplateTypeEntity, UUID> {
    
    @Override
    @NonNull
    List<TemplateTypeEntity> findAll();
    
    Optional<TemplateTypeEntity> findByCode(String code);
}



