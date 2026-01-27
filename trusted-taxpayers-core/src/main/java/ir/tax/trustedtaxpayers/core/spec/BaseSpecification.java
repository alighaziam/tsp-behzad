package ir.tax.trustedtaxpayers.core.spec;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BaseSpecification<T> implements Specification<T> {

    private final List<FilterRequest> filters;

    public BaseSpecification(List<FilterRequest> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Predicate predicate = cb.conjunction();

        for (FilterRequest filter : filters) {
            if (filter == null || filter.getKey() == null || filter.getValue() == null) continue;

            String key = filter.getKey();
            Object value = filter.getValue();
            String op = filter.getOperand() != null ? filter.getOperand().trim().toLowerCase() : "=";

            try {
                // پشتیبانی از dot-path (رابطه ها) و چند-کلید با OR (a|b)
                String[] orKeys = key.split("\\|");
                if (orKeys.length > 1) {
                    Predicate orPredicate = cb.disjunction();
                    for (String k : orKeys) {
                        Path<?> p = resolvePath(root, k.trim());
                        orPredicate = cb.or(orPredicate, buildPredicate(cb, p, op, value));
                    }
                    predicate = cb.and(predicate, orPredicate);
                    continue;
                }

                Path<?> path = resolvePath(root, key);
                
                predicate = cb.and(predicate, buildPredicate(cb, path, op, value));
            } catch (Exception ignored) {
                // اگر فیلد وجود نداشت یا خطایی رخ داد، نادیده بگیر
            }
        }
        
        return predicate;
    }

    private Predicate buildPredicate(CriteriaBuilder cb, Path<?> path, String op, Object value) {

        Class<?> type = path.getJavaType();
        String normalizedOp = op == null ? "" : op.trim().toLowerCase();

        switch (normalizedOp) {

            // ===== EQUAL =====
            case "=", "eq", "equal" -> {
                return cb.equal(path, castValue(value, type));
            }

            // ===== NOT EQUAL =====
            case "!=", "ne", "notequal" -> {
                return cb.notEqual(path, castValue(value, type));
            }

            // ===== GREATER THAN =====
            case ">" -> {
                @SuppressWarnings("unchecked")
                Comparable<Object> comp = (Comparable<Object>) castValue(value, type);
                @SuppressWarnings("unchecked")
                Expression<Comparable<Object>> compPath = (Expression<Comparable<Object>>) path;
                return cb.greaterThan(compPath, comp);
            }

            case ">=" -> {
                @SuppressWarnings("unchecked")
                Comparable<Object> comp = (Comparable<Object>) castValue(value, type);
                @SuppressWarnings("unchecked")
                Expression<Comparable<Object>> compPath = (Expression<Comparable<Object>>) path;
                return cb.greaterThanOrEqualTo(compPath, comp);
            }

            // ===== LESS THAN =====
            case "<" -> {
                @SuppressWarnings("unchecked")
                Comparable<Object> comp = (Comparable<Object>) castValue(value, type);
                @SuppressWarnings("unchecked")
                Expression<Comparable<Object>> compPath = (Expression<Comparable<Object>>) path;
                return cb.lessThan(compPath, comp);
            }

            case "<=" -> {
                @SuppressWarnings("unchecked")
                Comparable<Object> comp = (Comparable<Object>) castValue(value, type);
                @SuppressWarnings("unchecked")
                Expression<Comparable<Object>> compPath = (Expression<Comparable<Object>>) path;
                return cb.lessThanOrEqualTo(compPath, comp);
            }

            // ===== LIKE =====
            case "like", "~" -> {
                String stringValue = String.valueOf(value);
                return cb.like(
                        cb.lower(path.as(String.class)),
                        "%" + stringValue.toLowerCase() + "%"
                );
            }

            // ===== IN =====
            case "in" -> {
                CriteriaBuilder.In<Object> inClause = cb.in(path);

                if (value instanceof java.util.Collection<?> coll) {
                    for (Object v : coll) {
                        inClause.value(castValue(v, type));
                    }
                } else if (value != null && value.getClass().isArray()) {
                    int len = java.lang.reflect.Array.getLength(value);
                    for (int i = 0; i < len; i++) {
                        inClause.value(castValue(java.lang.reflect.Array.get(value, i), type));
                    }
                } else {
                    inClause.value(castValue(value, type));
                }
                return inClause;
            }

            // ===== NULL CHECKS =====
            case "isnull" -> {
                return cb.isNull(path);
            }

            case "notnull" -> {
                return cb.isNotNull(path);
            }

            // ===== BETWEEN =====
            case "between" -> {
                if (value instanceof java.util.List<?> list && list.size() == 2) {
                    @SuppressWarnings("unchecked")
                    Comparable<Object> from = (Comparable<Object>) castValue(list.get(0), type);
                    @SuppressWarnings("unchecked")
                    Comparable<Object> to = (Comparable<Object>) castValue(list.get(1), type);
                    @SuppressWarnings("unchecked")
                    Expression<Comparable<Object>> compPath = (Expression<Comparable<Object>>) path;
                    return cb.between(compPath, from, to);
                }
                return cb.conjunction();
            }

            // ===== DEFAULT =====
            default -> {
                return cb.conjunction();
            }
        }
    }


    private Path<?> resolvePath(From<?, ?> root, String key) {
        if (key == null || key.isBlank()) return root;
        if (!key.contains(".")) return root.get(key);
        String[] parts = key.split("\\.");
        Path<?> current = root;
        for (String part : parts) {
            if (current instanceof From<?, ?> from) {
                try {
                    current = from.get(part);
                } catch (IllegalArgumentException e) {
                    // تلاش برای join در صورت عدم وجود attribute مستقیم
                    current = from.join(part, JoinType.LEFT);
                }
            } else {
                current = current.get(part);
            }
        }
        return current;
    }

    private Object castValue(Object value, Class<?> type) {
        if (value == null || type == null) return null;
        if (type.isInstance(value)) return value;
        String s = String.valueOf(value);
        if (java.util.UUID.class.equals(type)) {
            try {
                return java.util.UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                return value;
            }
        }
        if (Integer.class.equals(type) || int.class.equals(type)) {
            try {
                return Integer.valueOf(s);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        if (Long.class.equals(type) || long.class.equals(type)) {
            try {
                return Long.valueOf(s);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        if (Double.class.equals(type) || double.class.equals(type)) {
            try {
                return Double.valueOf(s);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        if (Float.class.equals(type) || float.class.equals(type)) {
            try {
                return Float.valueOf(s);
            } catch (NumberFormatException e) {
                return value;
            }
        }
        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return Boolean.valueOf(s);
        }
        return s;
    }
}

