package org.innovateuk.ifs.user.service;

import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.organisation.resource.OrganisationResource;

import java.util.List;

/**
 * Interface for CRUD operations on {@link OrganisationResource} related data.
 */
public interface OrganisationRestService {
    RestResult<List<OrganisationResource>> getOrganisationsByApplicationId(Long applicationId);
    RestResult<OrganisationResource> getOrganisationById(Long organisationId);
    RestResult<OrganisationResource> getOrganisationByIdForAnonymousUserFlow(Long organisationId);
    RestResult<OrganisationResource> getByUserAndApplicationId(long userId, long applicationId);
    RestResult<OrganisationResource> getByUserAndProjectId(long userId, long projectId);
    RestResult<List<OrganisationResource>> getAllByUserId(long userId);
    RestResult<OrganisationResource> createOrMatch(OrganisationResource organisation);
    RestResult<OrganisationResource> createAndLinkByInvite(OrganisationResource organisation, String inviteHash);
    RestResult<OrganisationResource> updateNameAndRegistration(OrganisationResource organisation);

}
