package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.enums.SettingGroupType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SettingEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends BaseRepository<SettingEntity, UUID> {
    
    /**
     * پیدا کردن تنظیمات بر اساس key
     */
    Optional<SettingEntity> findByKey(String key);
    
    /**
     * پیدا کردن تنظیمات بر اساس path
     */
    List<SettingEntity> findByPath(String path);
    
    /**
     * پیدا کردن تنظیمات بر اساس URL
     */
    List<SettingEntity> findByUrl(String url);

    /**
     * پیدا کردن تنظیمات بر اساس گروه
     */
    List<SettingEntity> findByGroupType(SettingGroupType groupType);
    
    /**
     * بررسی وجود تنظیمات با key خاص
     */
    boolean existsByKey(String key);
    
    /**
     * پیدا کردن تنظیمات بر اساس prefix key
     */
    @Query("SELECT s FROM SettingEntity s WHERE s.key LIKE :prefix%")
    List<SettingEntity> findByKeyPrefix(@Param("prefix") String prefix);
}


