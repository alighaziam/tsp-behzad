package ir.tax.trustedtaxpayers.interfaces.rest.events;

public class EntityCreatedEvent<T> {
    private final Class<T> entityClass;
    private final T entity;

    public EntityCreatedEvent(Class<T> entityClass, T entity) {
        this.entityClass = entityClass;
        this.entity = entity;
    }

    public Class<T> getEntityClass() { return entityClass; }
    public T getEntity() { return entity; }
}


