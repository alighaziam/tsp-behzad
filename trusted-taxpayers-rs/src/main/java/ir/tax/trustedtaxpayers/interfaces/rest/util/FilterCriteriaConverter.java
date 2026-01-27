package ir.tax.trustedtaxpayers.interfaces.rest.util;

import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;

import java.util.List;

public final class FilterCriteriaConverter {

    private FilterCriteriaConverter() {}

    public static List<FilterCriterionDto> toCriteria(List<FilterRequest> filters) {
        if (filters == null || filters.isEmpty()) {
            return java.util.List.of();
        }
        return filters.stream()
                .map(f -> {
                    var strategy = ir.tax.trustedtaxpayers.interfaces.rest.util.filter.FilterStrategyRegistry.resolve(f.getOperand());
                    String operand = strategy.normalizedOperand();
                    String value = strategy.normalizeValue(f.getValue());
                    return new FilterCriterionDto(f.getKey(), operand, value);
                })
                .toList();
    }
}


