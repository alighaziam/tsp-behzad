package ir.tax.trustedtaxpayers.model.ports;

import ir.tax.trustedtaxpayers.application.dto.FilterCriterionDto;
import ir.tax.trustedtaxpayers.application.SearchResult;
import ir.tax.trustedtaxpayers.beans.model.UserSummary;

import java.util.List;

public interface UserQueryPort {
    SearchResult<UserSummary> search(List<FilterCriterionDto> filters, int page, int size, String sortBy, boolean ascending);
}


