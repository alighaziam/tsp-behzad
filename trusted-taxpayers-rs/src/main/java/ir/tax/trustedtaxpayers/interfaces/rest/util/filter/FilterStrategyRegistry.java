package ir.tax.trustedtaxpayers.interfaces.rest.util.filter;

import java.util.List;

public final class FilterStrategyRegistry {
    private static final List<FilterStrategy> STRATEGIES = List.of(
            new LikeFilterStrategy(),
            new BetweenFilterStrategy(),
            new InFilterStrategy(),
            new EqualsFilterStrategy()
    );

    private FilterStrategyRegistry() {}

    public static FilterStrategy resolve(String operand) {
        if (operand != null) {
            for (FilterStrategy s : STRATEGIES) {
                if (s.supports(operand)) {
                    return s;
                }
            }
        }
        return new DefaultFilterStrategy(operand);
    }
}


