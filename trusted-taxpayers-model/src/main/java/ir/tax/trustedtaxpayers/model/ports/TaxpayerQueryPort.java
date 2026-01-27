package ir.tax.trustedtaxpayers.model.ports;

import ir.tax.trustedtaxpayers.beans.model.Taxpayer;
import ir.tax.trustedtaxpayers.application.SearchResult;

public interface TaxpayerQueryPort {
    SearchResult<Taxpayer> searchByQuery(String queryText, int page, int size, String sortBy, boolean ascending);
}


