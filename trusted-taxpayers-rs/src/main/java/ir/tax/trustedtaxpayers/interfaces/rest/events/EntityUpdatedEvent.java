package ir.tax.trustedtaxpayers.interfaces.rest.events;

public class EntityUpdatedEvent<T, ID> {
    private final Class<T> entityClass;
    private final ID id;
    private final T entity;

    public EntityUpdatedEvent(Class<T> entityClass, ID id, T entity) {
        this.entityClass = entityClass;
        this.id = id;
        this.entity = entity;
    }

    public Class<T> getEntityClass() { return entityClass; }
    public ID getId() { return id; }
    public T getEntity() { return entity; }
}


