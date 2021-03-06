package org.innovateuk.ifs.user.service;

import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.invite.resource.EditUserResource;
import org.innovateuk.ifs.registration.resource.InternalUserRegistrationResource;
import org.innovateuk.ifs.user.resource.*;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Interface for CRUD operations on {@link UserResource} related data.
 */
public interface UserRestService {
    RestResult<UserResource> retrieveUserResourceByUid(String uid);

    RestResult<UserResource> retrieveUserById(Long id);

    RestResult<List<UserResource>> findAll();

    RestResult<List<UserOrganisationResource>> findExternalUsers(String searchString, SearchCategory searchCategory);

    RestResult<List<UserResource>> findByUserRole(Role role);

    RestResult<UserPageResource> getActiveInternalUsers(int pageNumber, int pageSize);

    RestResult<UserPageResource> getInactiveInternalUsers(int pageNumber, int pageSize);

    RestResult<ProcessRoleResource> findProcessRole(Long userId, Long applicationId);

    RestResult<List<ProcessRoleResource>> findProcessRole(Long applicationId);

    RestResult<List<ProcessRoleResource>> findProcessRoleByUserId(Long userId);

    RestResult<List<UserResource>> findAssignableUsers(Long applicationId);

    RestResult<UserResource> findUserByEmail(String email);

    Future<RestResult<ProcessRoleResource[]>> findAssignableProcessRoles(Long applicationId);

    RestResult<Boolean> userHasApplicationForCompetition(Long userId, Long competitionId);

    Future<RestResult<ProcessRoleResource>> findProcessRoleById(Long processRoleId);

    RestResult<Void> verifyEmail(String hash);

    RestResult<Void> resendEmailVerificationNotification(String email);

    Future<RestResult<Void>> sendPasswordResetNotification(String email);

    RestResult<Void> checkPasswordResetHash(String hash);

    RestResult<Void> resetPassword(String hash, String password);

    RestResult<UserResource> createLeadApplicantForOrganisationWithCompetitionId(String firstName, String lastName, String password, String email, String title,
                                                                                 String phoneNumber, Long organisationId,
                                                                                 Long competitionId, Boolean allowMarketingEmails);

    RestResult<UserResource> createLeadApplicantForOrganisation(String firstName, String lastName, String password, String email, String title,
                                                                String phoneNumber, Long organisationId, Boolean allowMarketingEmails);

    RestResult<UserResource> updateDetails(Long id, String email, String firstName, String lastName, String title, String phoneNumber, boolean allowMarketingEmails);

    RestResult<Void> createInternalUser(String inviteHash, InternalUserRegistrationResource internalUserRegistrationResource);

    RestResult<Void> editInternalUser(EditUserResource editUserResource);

    RestResult<Void> agreeNewSiteTermsAndConditions(long userId);

    RestResult<Void> deactivateUser(Long userId);
    RestResult<Void> reactivateUser(Long userId);
    RestResult<Void> grantRole(Long userId, Role targetRole);

}