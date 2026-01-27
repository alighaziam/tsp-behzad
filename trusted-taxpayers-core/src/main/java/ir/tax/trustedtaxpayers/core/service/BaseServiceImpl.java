package ir.tax.trustedtaxpayers.core.service;

import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

/**
 * پیاده‌سازی پایه برای BaseService
 * 
 * <p>این کلاس عملیات CRUD پایه را برای تمام Entityها فراهم می‌کند.</p>
 * 
 * @param <T> نوع Entity
 * @param <ID> نوع شناسه
 */
@Slf4j
public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {
    protected final BaseRepository<T, ID> repository;

    /**
     * Constructor با dependency injection
     * 
     * @param repository Repository مربوط به Entity
     * @throws IllegalArgumentException اگر repository null باشد
     */
    protected BaseServiceImpl(BaseRepository<T, ID> repository) {
        Assert.notNull(repository, "Repository must not be null");
        this.repository = repository;
    }

    @Override
    @Transactional
    public T save(T entity) {
        Assert.notNull(entity, "Entity must not be null");
        log.debug("Saving entity: {}", entity);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public T update(ID id, T entity) {
        Assert.notNull(id, "ID must not be null");
        Assert.notNull(entity, "Entity must not be null");
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Entity with ID " + id + " not found");
        }

        // برای اینکه entity جدید ID درست بگیره:
        setEntityId(entity, id);

        log.debug("Updating entity with ID: {}", id);
        return repository.save(entity);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        Assert.notNull(id, "ID must not be null");
        
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Entity with ID " + id + " not found");
        }
        
        log.debug("Deleting entity with ID: {}", id);
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "ID must not be null");
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        log.debug("Finding all entities");
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        Assert.notNull(pageable, "Pageable must not be null");
        log.debug("Finding all entities with pagination: {}", pageable);
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(ID id) {
        Assert.notNull(id, "ID must not be null");
        return repository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        Assert.notNull(spec, "Specification must not be null");
        Assert.notNull(pageable, "Pageable must not be null");
        log.debug("Finding entities with specification and pagination");
        return repository.findAll(spec, pageable);
    }
    
    /**
     * تنظیم ID در Entity با استفاده از reflection
     * 
     * @param entity Entity برای تنظیم ID
     * @param id ID برای تنظیم
     */
    protected void setEntityId(T entity, ID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            if (!field.canAccess(entity)) {
                field.setAccessible(true);
            }
            field.set(entity, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Entity might not have an 'id' field or it's not accessible
            // This is expected for some entity types
            log.trace("Could not set ID field on entity: {}", e.getMessage());
        }
    }
}

