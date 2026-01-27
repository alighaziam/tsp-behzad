package ir.tax.trustedtaxpayers.interfaces.rest.util;

import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.RoleEntity;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class RoleFilter {

    private RoleFilter() {}

    public static List<RoleEntity> apply(List<RoleEntity> roles, List<FilterCriterionDto> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return roles;
        }
        Stream<RoleEntity> stream = roles.stream();
        for (FilterCriterionDto c : criteria) {
            String field = c.field();
            String operand = c.operand();
            String value = c.value();
            if (field == null || operand == null || value == null) {
                continue;
            }
            if ("name".equalsIgnoreCase(field)) {
                stream = filterByName(stream, operand, value);
            }
        }
        return stream.toList();
    }

    private static Stream<RoleEntity> filterByName(Stream<RoleEntity> stream, String operand, String value) {
        String v = value.toLowerCase(Locale.ROOT);
        if ("CONTAINS".equalsIgnoreCase(operand) || "LIKE".equalsIgnoreCase(operand)) {
            return stream.filter(r -> r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(v));
        } else if ("EQ".equalsIgnoreCase(operand) || "=".equals(operand)) {
            return stream.filter(r -> r.getName() != null && r.getName().equalsIgnoreCase(value));
        }
        return stream;
    }
}


