package org.innovateuk.ifs.application.service;


import org.innovateuk.ifs.organisation.resource.OrganisationResource;
import org.innovateuk.ifs.organisation.resource.OrganisationSearchResult;
import org.innovateuk.ifs.user.resource.ProcessRoleResource;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

/**
 * Interface for CRUD operations on {@link OrganisationResource} related data.
 */
public interface OrganisationService {

    OrganisationResource getOrganisationById(Long organisationId);

    OrganisationResource getPrimaryForUser(Long userId);

    OrganisationResource getByUserAndApplicationId(long userId, long applicationId);

    OrganisationResource getByUserAndProjectId(long userId, long projectId);

    OrganisationResource getOrganisationByIdForAnonymousUserFlow(Long organisationId);

    OrganisationResource createOrMatch(OrganisationResource organisation);

    OrganisationResource createAndLinkByInvite(OrganisationResource organisation, String inviteHash);

    OrganisationResource updateNameAndRegistration(OrganisationResource organisation);

    OrganisationSearchResult getCompanyHouseOrganisation(String organisationId);

    Long getOrganisationType(Long userId, Long applicationId);

    Optional<OrganisationResource> getOrganisationForUser(Long userId, List<ProcessRoleResource> userApplicationRoles);

    SortedSet<OrganisationResource> getApplicationOrganisations(List<ProcessRoleResource> userApplicationRoles);

    SortedSet<OrganisationResource> getAcademicOrganisations(SortedSet<OrganisationResource> organisations);

    Optional<OrganisationResource> getApplicationLeadOrganisation(List<ProcessRoleResource> userApplicationRoles);
}
