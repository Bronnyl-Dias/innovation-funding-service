package org.innovateuk.ifs.projectdetails;

import org.innovateuk.ifs.address.resource.AddressResource;
import org.innovateuk.ifs.address.resource.OrganisationAddressType;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.invite.resource.ProjectInviteResource;
import org.innovateuk.ifs.project.resource.ProjectOrganisationCompositeId;

import java.time.LocalDate;
import java.util.List;

/**
 * A service for dealing with Project Details Resources via the appropriate Rest services
 */
public interface ProjectDetailsService {

    ServiceResult<Void> updateFinanceContact(ProjectOrganisationCompositeId composite, Long financeContactUserId);

    ServiceResult<Void> updatePartnerProjectLocation(long projectId, long organisationId, String postcode);

    ServiceResult<Void> updateProjectManager(Long projectId, Long projectManagerUserId);

    ServiceResult<Void> updateProjectStartDate(Long projectId, LocalDate projectStartDate);

    ServiceResult<Void> updateProjectDuration(long projectId, long durationInMonths);

    ServiceResult<Void> updateAddress(Long leadOrganisationId, Long projectId, OrganisationAddressType addressType, AddressResource address);

    ServiceResult<Void> inviteFinanceContact (Long projectId, ProjectInviteResource projectInviteResource);

    ServiceResult<Void> inviteProjectManager (Long projectId, ProjectInviteResource projectInviteResource);

    ServiceResult<Void> saveProjectInvite(ProjectInviteResource projectInviteResource);

    ServiceResult<List<ProjectInviteResource>> getInvitesByProject(Long projectId);
}
