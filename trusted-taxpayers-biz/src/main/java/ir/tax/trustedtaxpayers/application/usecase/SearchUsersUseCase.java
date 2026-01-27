package ir.tax.trustedtaxpayers.application.usecase;

import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.application.SearchResult;
import ir.tax.trustedtaxpayers.beans.model.UserSummary;
import ir.tax.trustedtaxpayers.model.ports.UserQueryPort;

import java.util.List;

public class SearchUsersUseCase {
    private final UserQueryPort userQueryPort;

    public SearchUsersUseCase(UserQueryPort userQueryPort) {
        this.userQueryPort = userQueryPort;
    }

    public SearchResult<UserSummary> execute(List<FilterCriterionDto> filters, Integer page, Integer size, String sortBy, Boolean asc) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? 20 : Math.min(Math.max(size, 1), 100);
        String sort = (sortBy == null || sortBy.isBlank()) ? "username" : sortBy;
        boolean ascending = asc == null || Boolean.TRUE.equals(asc);
        return userQueryPort.search(filters, p, s, sort, ascending);
    }
}



