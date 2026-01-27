package ir.tax.trustedtaxpayers.interfaces.rest.dto;

/**
 * Enum برای انواع رابطه‌های JPA بین Entity ها
 */
public enum RelationType {
    /**
     * رابطه Many-to-One
     * یک Entity می‌تواند به چند Entity دیگر تعلق داشته باشد
     */
    MANY_TO_ONE("ManyToOne"),
    
    /**
     * رابطه One-to-Many
     * یک Entity می‌تواند چند Entity دیگر داشته باشد
     */
    ONE_TO_MANY("OneToMany"),
    
    /**
     * رابطه One-to-One
     * یک Entity فقط می‌تواند یک Entity دیگر داشته باشد
     */
    ONE_TO_ONE("OneToOne"),
    
    /**
     * رابطه Many-to-Many
     * چند Entity می‌توانند با چند Entity دیگر رابطه داشته باشند
     */
    MANY_TO_MANY("ManyToMany");

    private final String value;

    RelationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * تبدیل String به RelationType
     */
    public static RelationType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (RelationType type : RelationType.values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}

