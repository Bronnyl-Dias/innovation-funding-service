package org.innovateuk.ifs.finance.handler;

import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.finance.domain.ApplicationFinance;
import org.innovateuk.ifs.finance.domain.ApplicationFinanceRow;
import org.innovateuk.ifs.finance.domain.ProjectFinanceRow;
import org.innovateuk.ifs.finance.handler.item.FinanceRowHandler;
import org.innovateuk.ifs.finance.resource.category.ChangedFinanceRowPair;
import org.innovateuk.ifs.finance.resource.category.FinanceRowCostCategory;
import org.innovateuk.ifs.finance.resource.cost.FinanceRowItem;
import org.innovateuk.ifs.finance.resource.cost.FinanceRowType;
import org.innovateuk.ifs.finance.resource.cost.SpendProfileRowType;

import java.util.List;
import java.util.Map;

/**
 * Action to retrieve the finances of the organisations
 */
public interface OrganisationFinanceHandler {
    Iterable<ApplicationFinanceRow> initialiseCostType(ApplicationFinance applicationFinance, SpendProfileRowType costType);
    Map<SpendProfileRowType,FinanceRowCostCategory> getOrganisationFinances(Long applicationFinanceId, Competition competition);

    ApplicationFinanceRow costItemToCost(FinanceRowItem costItem);
    ProjectFinanceRow costItemToProjectCost(FinanceRowItem costItem);
    FinanceRowItem costToCostItem(ApplicationFinanceRow cost);
    FinanceRowItem costToCostItem(ProjectFinanceRow cost);
    FinanceRowHandler getCostHandler(SpendProfileRowType costType);
    List<FinanceRowItem> costToCostItem(List<ApplicationFinanceRow> costs);
    ApplicationFinanceRow updateCost(ApplicationFinanceRow financeRow);
    ApplicationFinanceRow addCost(Long applicationFinanceId, Long questionId, ApplicationFinanceRow financeRow);

    List<ApplicationFinanceRow> costItemsToCost(List<FinanceRowItem> costItems);

    Map<SpendProfileRowType, FinanceRowCostCategory> getProjectOrganisationFinances(Long projectFinanceId, Competition competition);
    Map<SpendProfileRowType, List<ChangedFinanceRowPair<FinanceRowItem, FinanceRowItem>>> getProjectOrganisationFinanceChanges(Long projectFinanceId);
}
