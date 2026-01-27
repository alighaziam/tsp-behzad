package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateRepository extends BaseRepository<TemplateEntity, UUID> {
    
    @Override
    @NonNull
    List<TemplateEntity> findAll();
    
    Optional<TemplateEntity> findByCode(String code);
    
    /**
     * جستجوی templates بر اساس code یا title
     * برای استفاده در autocomplete
     * 
     * @param codeQuery بخشی از کد قالب
     * @param titleQuery بخشی از عنوان قالب
     * @param pageable اطلاعات صفحه‌بندی
     * @return لیست templates
     */
    @org.springframework.data.jpa.repository.Query("SELECT t FROM TemplateEntity t WHERE " +
           "(:codeQuery IS NULL OR :codeQuery = '' OR LOWER(t.code) LIKE LOWER(CONCAT('%', :codeQuery, '%'))) AND " +
           "(:titleQuery IS NULL OR :titleQuery = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :titleQuery, '%'))) " +
           "ORDER BY t.title ASC")
    List<TemplateEntity> findByCodeContainingIgnoreCaseOrTitleContainingIgnoreCase(
            @org.springframework.data.repository.query.Param("codeQuery") String codeQuery,
            @org.springframework.data.repository.query.Param("titleQuery") String titleQuery,
            org.springframework.data.domain.Pageable pageable);
}



