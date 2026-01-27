package ir.tax.trustedtaxpayers.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repository پایه برای تمام Entityها
 * 
 * <p>این interface ترکیبی از:</p>
 * <ul>
 *   <li>JpaRepository: عملیات CRUD پایه</li>
 *   <li>JpaSpecificationExecutor: پشتیبانی از Dynamic Queries</li>
 * </ul>
 * 
 * @param <T> نوع Entity
 * @param <ID> نوع شناسه
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
}

