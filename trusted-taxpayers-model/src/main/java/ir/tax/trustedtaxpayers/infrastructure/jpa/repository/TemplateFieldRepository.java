package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemplateFieldRepository extends BaseRepository<TemplateFieldEntity, UUID> {
}





