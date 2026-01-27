package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.BaseInfoEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository برای BaseInfoEntity
 */
public interface BaseInfoRepository extends BaseRepository<BaseInfoEntity, UUID> {
    
    /**
     * یافتن اطلاعات پایه بر اساس category که active باشند و logically deleted نباشند
     * 
     * @param category دسته‌بندی مورد نظر
     * @return لیست اطلاعات پایه
     */
    @Query("SELECT b FROM BaseInfoEntity b WHERE " +
           "b.category = :category AND " +
           "b.isActive = true")
    List<BaseInfoEntity> findByCategory(@Param("category") String category);
    
    @Query("SELECT b FROM BaseInfoEntity b WHERE " +
           "b.category = :category AND " +
           "b.title = :title AND " +
           "b.isActive = true")
    java.util.Optional<BaseInfoEntity> findByCategoryAndTitle(
        @Param("category") String category,
        @Param("title") String title
    );
}


