package org.innovateuk.ifs.project.finance;


import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.project.resource.ApprovalType;
import org.innovateuk.ifs.project.resource.SpendProfileCSVResource;
import org.innovateuk.ifs.project.resource.SpendProfileResource;
import org.innovateuk.ifs.project.resource.SpendProfileTableResource;

import java.util.Optional;

/**
 * A service for dealing with a Project's finance operations
 */
public interface ProjectFinanceService {

    ServiceResult<Void> generateSpendProfile(Long projectId);

    ServiceResult<Void> approveOrRejectSpendProfile(Long projectId, ApprovalType approvalType);

    ApprovalType getSpendProfileStatusByProjectId(Long projectId);

    Optional<SpendProfileResource> getSpendProfile(Long projectId, Long organisationId);

    SpendProfileTableResource getSpendProfileTable(Long projectId, Long organisationId);

    SpendProfileCSVResource getSpendProfileCSV(Long projectId, Long organisationId);

    ServiceResult<Void> saveSpendProfile(Long projectId, Long organisationId, SpendProfileTableResource table);

    ServiceResult<Void> markSpendProfileComplete(Long projectId, Long organisationId);

    ServiceResult<Void> markSpendProfileIncomplete(Long projectId, Long organisationId);

    ServiceResult<Void> completeSpendProfilesReview(Long projectId);
}
