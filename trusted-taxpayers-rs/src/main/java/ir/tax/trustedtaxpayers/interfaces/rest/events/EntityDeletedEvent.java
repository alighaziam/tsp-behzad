package ir.tax.trustedtaxpayers.interfaces.rest.events;

public class EntityDeletedEvent<T, ID> {
    private final Class<T> entityClass;
    private final ID id;

    public EntityDeletedEvent(Class<T> entityClass, ID id) {
        this.entityClass = entityClass;
        this.id = id;
    }

    public Class<T> getEntityClass() { return entityClass; }
    public ID getId() { return id; }
}


