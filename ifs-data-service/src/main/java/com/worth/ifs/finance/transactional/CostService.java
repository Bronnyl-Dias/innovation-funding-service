package com.worth.ifs.finance.transactional;

import com.worth.ifs.commons.service.ServiceResult;
import com.worth.ifs.file.resource.FileEntryResource;
import com.worth.ifs.file.service.FileAndContents;
import com.worth.ifs.finance.domain.Cost;
import com.worth.ifs.finance.domain.FinanceRowMetaField;
import com.worth.ifs.finance.handler.item.CostHandler;
import com.worth.ifs.finance.resource.ApplicationFinanceResource;
import com.worth.ifs.finance.resource.ApplicationFinanceResourceId;
import com.worth.ifs.finance.resource.FinanceRowMetaFieldResource;
import com.worth.ifs.finance.resource.cost.CostItem;
import com.worth.ifs.security.NotSecured;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

public interface CostService {

    @PreAuthorize("hasPermission(#costFieldId, 'com.worth.ifs.finance.resource.FinanceRowMetaFieldResource', 'READ')")
    ServiceResult<FinanceRowMetaField> getCostFieldById(@P("costFieldId") Long costFieldId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<FinanceRowMetaFieldResource>> findAllCostFields();

    @PostAuthorize("hasPermission(returnObject, 'READ')")
    ServiceResult<CostItem> getCostItem(Long costItemId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<Cost>> getCosts(Long applicationFinanceId, String costTypeName, Long questionId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<CostItem>> getCostItems(Long applicationFinanceId, String costTypeName, Long questionId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<CostItem>> getCostItems(Long applicationFinanceId, Long questionId);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'ADD_COST')")
    ServiceResult<CostItem> addCost(@P("applicationFinanceId") Long applicationFinanceId, Long questionId, CostItem newCostItem);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'ADD_COST')")
    ServiceResult<CostItem> addCostWithoutPersisting(@P("applicationFinanceId") Long applicationFinanceId, Long questionId);
    
    @PreAuthorize("hasPermission(#costId, 'com.worth.ifs.finance.domain.Cost', 'UPDATE')")
    ServiceResult<CostItem> updateCost(@P("costId")Long costId, CostItem newCostItem);

    @PreAuthorize("hasPermission(#costId, 'com.worth.ifs.finance.domain.Cost', 'DELETE')")
    ServiceResult<Void> deleteCost(@P("costId") Long costId);

    @PostAuthorize("hasPermission(returnObject, 'READ')")
    ServiceResult<ApplicationFinanceResource> findApplicationFinanceByApplicationIdAndOrganisation(Long applicationId, Long organisationId);

    @PostFilter("hasPermission(filterObject, 'READ')")
    ServiceResult<List<ApplicationFinanceResource>> findApplicationFinanceByApplication(Long applicationId);

    @PreAuthorize("hasPermission(#applicationId, 'com.worth.ifs.application.resource.ApplicationResource', 'READ_RESEARCH_PARTICIPATION_PERCENTAGE')")
    ServiceResult<Double> getResearchParticipationPercentage(@P("applicationId") Long applicationId);

    @PreAuthorize("hasPermission(#applicationFinanceResourceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'ADD_COST')")
    ServiceResult<ApplicationFinanceResource> addCost(@P("applicationFinanceResourceId") final ApplicationFinanceResourceId applicationFinanceResourceId);

    @PostAuthorize("hasPermission(returnObject, 'READ')")
    ServiceResult<ApplicationFinanceResource> getApplicationFinanceById(Long applicationFinanceId);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'UPDATE_COST')")
    ServiceResult<ApplicationFinanceResource> updateCost(@P("applicationFinanceId")Long applicationFinanceId, ApplicationFinanceResource applicationFinance);

    @PostAuthorize("hasPermission(returnObject, 'READ')")
    ServiceResult<ApplicationFinanceResource> financeDetails(Long applicationId, Long organisationId);

    @PreAuthorize("hasPermission(#applicationId, 'com.worth.ifs.application.resource.ApplicationResource', 'READ_FINANCE_TOTALS')")
    ServiceResult<List<ApplicationFinanceResource>> financeTotals(@P("applicationId") Long applicationId);

    @NotSecured(value = "This is not getting date from the database, just getting a CostHandler", mustBeSecuredByOtherServices = false)
    CostHandler getCostHandler(Long costItemId);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'CREATE_FILE_ENTRY')")
    ServiceResult<FileEntryResource> createFinanceFileEntry(@P("applicationFinanceId")long applicationFinanceId, FileEntryResource fileEntryResource, Supplier<InputStream> inputStreamSupplier);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'UPDATE_FILE_ENTRY')")
    ServiceResult<FileEntryResource> updateFinanceFileEntry(@P("applicationFinanceId")long applicationFinanceId, FileEntryResource fileEntryResource, Supplier<InputStream> inputStreamSupplier);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'DELETE_FILE_ENTRY')")
    ServiceResult<Void> deleteFinanceFileEntry(@P("applicationFinanceId")long applicationFinanceId);

    @PreAuthorize("hasPermission(#applicationFinanceId, 'com.worth.ifs.finance.resource.ApplicationFinanceResource', 'READ_FILE_ENTRY')")
    ServiceResult<FileAndContents> getFileContents(@P("applicationFinanceId")long applicationFinanceId);

}