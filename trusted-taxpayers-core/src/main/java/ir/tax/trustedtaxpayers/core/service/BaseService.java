package ir.tax.trustedtaxpayers.core.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * Interface پایه برای Service Layer
 * 
 * <p>این interface عملیات CRUD پایه را برای تمام Entityها تعریف می‌کند.</p>
 * 
 * @param <T> نوع Entity
 * @param <ID> نوع شناسه
 */
public interface BaseService<T, ID> {

    /**
     * ذخیره یک Entity جدید
     * 
     * @param entity Entity برای ذخیره
     * @return Entity ذخیره شده
     */
    T save(T entity);

    /**
     * به‌روزرسانی یک Entity موجود
     * 
     * @param id شناسه Entity
     * @param entity Entity با مقادیر جدید
     * @return Entity به‌روزرسانی شده
     * @throws ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException اگر Entity یافت نشود
     */
    T update(ID id, T entity);

    /**
     * حذف یک Entity با شناسه
     * 
     * @param id شناسه Entity
     * @throws ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException اگر Entity یافت نشود
     */
    void deleteById(ID id);

    /**
     * پیدا کردن یک Entity با شناسه
     * 
     * @param id شناسه Entity
     * @return Optional از Entity
     */
    Optional<T> findById(ID id);

    /**
     * پیدا کردن تمام Entityها
     * 
     * @return لیست تمام Entityها
     */
    List<T> findAll();

    /**
     * پیدا کردن تمام Entityها با صفحه‌بندی
     * 
     * @param pageable اطلاعات صفحه‌بندی
     * @return Page از Entityها
     */
    Page<T> findAll(Pageable pageable);

    /**
     * بررسی وجود Entity با شناسه
     * 
     * @param id شناسه Entity
     * @return true اگر Entity وجود داشته باشد
     */
    boolean exists(ID id);
    
    /**
     * پیدا کردن Entityها با استفاده از Specification و صفحه‌بندی
     * 
     * @param spec Specification برای فیلتر کردن
     * @param pageable اطلاعات صفحه‌بندی
     * @return Page از Entityهای فیلتر شده
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);
}

