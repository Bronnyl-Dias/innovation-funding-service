package org.innovateuk.ifs.invite.transactional;

import org.innovateuk.ifs.BaseServiceUnitTest;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.commons.error.CommonFailureKeys;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.invite.builder.RoleInviteBuilder;
import org.innovateuk.ifs.invite.domain.ApplicationInvite;
import org.innovateuk.ifs.invite.domain.InviteOrganisation;
import org.innovateuk.ifs.invite.domain.ProjectInvite;
import org.innovateuk.ifs.invite.domain.RoleInvite;
import org.innovateuk.ifs.invite.resource.ExternalInviteResource;
import org.innovateuk.ifs.invite.resource.RoleInvitePageResource;
import org.innovateuk.ifs.invite.resource.RoleInviteResource;
import org.innovateuk.ifs.notifications.resource.ExternalUserNotificationTarget;
import org.innovateuk.ifs.notifications.resource.NotificationTarget;
import org.innovateuk.ifs.project.domain.Project;
import org.innovateuk.ifs.project.transactional.EmailService;
import org.innovateuk.ifs.user.builder.UserResourceBuilder;
import org.innovateuk.ifs.user.domain.Organisation;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.resource.SearchCategory;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.*;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.invite.builder.ApplicationInviteBuilder.newApplicationInvite;
import static org.innovateuk.ifs.invite.builder.InviteOrganisationBuilder.newInviteOrganisation;
import static org.innovateuk.ifs.invite.builder.ProjectInviteBuilder.newProjectInvite;
import static org.innovateuk.ifs.invite.builder.RoleInviteBuilder.newRoleInvite;
import static org.innovateuk.ifs.invite.builder.RoleInviteResourceBuilder.newRoleInviteResource;
import static org.innovateuk.ifs.invite.constant.InviteStatus.CREATED;
import static org.innovateuk.ifs.invite.constant.InviteStatus.SENT;
import static org.innovateuk.ifs.invite.transactional.InviteUserServiceImpl.Notifications.INVITE_INTERNAL_USER;
import static org.innovateuk.ifs.project.builder.ProjectBuilder.newProject;
import static org.innovateuk.ifs.user.builder.OrganisationBuilder.newOrganisation;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.util.MapFunctions.asMap;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class InviteUserServiceImplTest extends BaseServiceUnitTest<InviteUserServiceImpl> {

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<RoleInvite> roleInviteArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsArgumentCaptor;

    private static String webBaseUrl = "base";

    private UserResource invitedUser = null;

    @Before
    public void setUp() {

        invitedUser = UserResourceBuilder.newUserResource()
                .withFirstName("Astle")
                .withLastName("Pimenta")
                .withEmail("Astle.Pimenta@innovateuk.gov.uk")
                .build();
    }

    @Override
    protected InviteUserServiceImpl supplyServiceUnderTest() {
        InviteUserServiceImpl inviteService = new InviteUserServiceImpl();
        ReflectionTestUtils.setField(inviteService, "webBaseUrl", webBaseUrl);
        return inviteService;
    }

    @Test
    public void saveUserInviteWhenUserDetailsMissing() throws Exception {

        UserResource invitedUser = UserResourceBuilder.newUserResource().build();

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, UserRoleType.SUPPORT);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(USER_ROLE_INVITE_INVALID));
    }

    @Test
    public void saveUserInviteWhenUserRoleIsNotSpecified() throws Exception {

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, null);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(USER_ROLE_INVITE_INVALID));
    }

    @Test
    public void saveUserInviteWhenRoleSpecifiedIsNotInternalRole() {

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, UserRoleType.COLLABORATOR);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(NOT_AN_INTERNAL_USER_ROLE));

    }

    @Test
    public void saveUserInviteWhenEmailDomainIsIncorrect() throws Exception {

        UserRoleType adminRoleType = UserRoleType.SUPPORT;
        invitedUser.setEmail("Astle.Pimenta@gmail.com");

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, adminRoleType);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(USER_ROLE_INVITE_INVALID_EMAIL));
    }

    @Test
    public void saveUserInviteWhenUserAlreadyInvited() throws Exception {

        UserRoleType adminRoleType = UserRoleType.SUPPORT;

        Role role = Role.SUPPORT;
        RoleInvite roleInvite = new RoleInvite();

        when(userRepositoryMock.findByEmail(invitedUser.getEmail())).thenReturn(Optional.empty());
        when(inviteRoleRepositoryMock.findByEmail(invitedUser.getEmail())).thenReturn(Collections.singletonList(roleInvite));

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, adminRoleType);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(USER_ROLE_INVITE_TARGET_USER_ALREADY_INVITED));

    }

    @Test
    public void inviteInternalUserSendEmailSucceeds() throws Exception {
        Role role = Role.IFS_ADMINISTRATOR;
        RoleInvite expectedRoleInvite = newRoleInvite().withEmail("Astle.Pimenta@innovateuk.gov.uk").withName("Astle Pimenta").withRole(role).withStatus(CREATED).withHash("").build();
        // hash is random, so capture RoleInvite value to verify other fields
        when(inviteRoleRepositoryMock.save(any(RoleInvite.class))).thenReturn(expectedRoleInvite);

        Role roleResource = Role.IFS_ADMINISTRATOR;

        NotificationTarget notificationTarget = new ExternalUserNotificationTarget("Astle Pimenta", "Astle.Pimenta@innovateuk.gov.uk");
        when(emailService.sendEmail(eq(singletonList(notificationTarget)), any(), eq(INVITE_INTERNAL_USER))).thenReturn(serviceSuccess());

        when(loggedInUserSupplierMock.get()).thenReturn(newUser().build());

        expectedRoleInvite.setHash("1234");
        when(inviteRoleRepositoryMock.save(any(RoleInvite.class))).thenReturn(expectedRoleInvite);

        when(userRepositoryMock.findByEmail(invitedUser.getEmail())).thenReturn(Optional.empty());

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, UserRoleType.IFS_ADMINISTRATOR);

        verify(inviteRoleRepositoryMock, times(2)).save(roleInviteArgumentCaptor.capture());
        verify(emailService).sendEmail(any(), paramsArgumentCaptor.capture(), any());

        List<RoleInvite> captured = roleInviteArgumentCaptor.getAllValues();
        assertEquals("Astle.Pimenta@innovateuk.gov.uk", captured.get(0).getEmail());
        assertEquals("Astle Pimenta", captured.get(0).getName());
        assertEquals(role, captured.get(0).getTarget());
        assertEquals(CREATED, captured.get(0).getStatus());

        assertEquals("Astle.Pimenta@innovateuk.gov.uk", captured.get(1).getEmail());
        assertEquals("Astle Pimenta", captured.get(1).getName());
        assertEquals(role, captured.get(1).getTarget());
        assertEquals(loggedInUserSupplierMock.get(), captured.get(1).getSentBy());
        assertFalse(now().isBefore(captured.get(1).getSentOn()));
        assertEquals(SENT, captured.get(1).getStatus());
        assertFalse(captured.get(1).getHash().isEmpty());

        Map<String, Object> capturedParams = paramsArgumentCaptor.getValue();
        assertTrue(capturedParams.containsKey("role"));
        assertTrue(capturedParams.get("role").equals("IFS Administrator"));
        assertTrue(capturedParams.containsKey("inviteUrl"));
        assertTrue(((String)capturedParams.get("inviteUrl")).startsWith(webBaseUrl + InviteUserServiceImpl.WEB_CONTEXT + "/" + expectedRoleInvite.getHash() + "/register"));

        assertTrue(result.isSuccess());
    }

    @Test
    public void inviteInternalUserSendEmailFails() throws Exception {
        Role role = Role.SUPPORT;
        RoleInvite expectedRoleInvite = newRoleInvite().withEmail("Astle.Pimenta@innovateuk.gov.uk").withName("Astle Pimenta").withRole(role).withStatus(CREATED).withHash("").build();
        // hash is random, so capture RoleInvite value to verify other fields
        when(inviteRoleRepositoryMock.save(any(RoleInvite.class))).thenReturn(expectedRoleInvite);
        when(userRepositoryMock.findByEmail(invitedUser.getEmail())).thenReturn(Optional.empty());

        NotificationTarget notificationTarget = new ExternalUserNotificationTarget("Astle Pimenta", "Astle.Pimenta@innovateuk.gov.uk");
        when(emailService.sendEmail(eq(singletonList(notificationTarget)), any(), eq(INVITE_INTERNAL_USER))).thenReturn(ServiceResult.serviceFailure(CommonFailureKeys.GENERAL_UNEXPECTED_ERROR));

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, UserRoleType.SUPPORT);

        verify(inviteRoleRepositoryMock, times(1)).save(roleInviteArgumentCaptor.capture());
        verify(emailService).sendEmail(any(), paramsArgumentCaptor.capture(), any());

        List<RoleInvite> captured = roleInviteArgumentCaptor.getAllValues();
        assertEquals("Astle.Pimenta@innovateuk.gov.uk", captured.get(0).getEmail());
        assertEquals("Astle Pimenta", captured.get(0).getName());
        assertEquals(role, captured.get(0).getTarget());
        assertEquals(CREATED, captured.get(0).getStatus());

        Map<String, Object> capturedParams = paramsArgumentCaptor.getValue();
        assertTrue(capturedParams.containsKey("role"));
        assertTrue(capturedParams.get("role").equals("IFS Support User"));
        assertTrue(capturedParams.containsKey("inviteUrl"));
        assertTrue(((String)capturedParams.get("inviteUrl")).startsWith(webBaseUrl + InviteUserServiceImpl.WEB_CONTEXT + "/" + expectedRoleInvite.getHash() + "/register"));

        assertTrue(result.isFailure());
        assertEquals(1, result.getErrors().size());
        assertEquals(CommonFailureKeys.GENERAL_UNEXPECTED_ERROR.name(), result.getErrors().get(0).getErrorKey());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getErrors().get(0).getStatusCode());
    }

    @Test
    public void saveUserInviteWhenEmailAlreadyTaken() throws Exception {
        Role role = Role.IFS_ADMINISTRATOR;
        RoleInvite expectedRoleInvite = newRoleInvite().withEmail("Astle.Pimenta@innovateuk.gov.uk").withName("Astle Pimenta").withRole(role).withStatus(CREATED).withHash("").build();
        // hash is random, so capture RoleInvite value to verify other fields
        when(inviteRoleRepositoryMock.save(any(RoleInvite.class))).thenReturn(expectedRoleInvite);

        Role roleResource = Role.IFS_ADMINISTRATOR;

        NotificationTarget notificationTarget = new ExternalUserNotificationTarget("Astle Pimenta", "Astle.Pimenta@innovateuk.gov.uk");
        when(emailService.sendEmail(eq(singletonList(notificationTarget)), any(), eq(INVITE_INTERNAL_USER))).thenReturn(serviceSuccess());

        when(loggedInUserSupplierMock.get()).thenReturn(newUser().build());

        expectedRoleInvite.setHash("1234");
        when(inviteRoleRepositoryMock.save(any(RoleInvite.class))).thenReturn(expectedRoleInvite);
        when(userRepositoryMock.findByEmail(invitedUser.getEmail())).thenReturn(Optional.of(newUser().build()));

        ServiceResult<Void> result = service.saveUserInvite(invitedUser, UserRoleType.IFS_ADMINISTRATOR);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(USER_ROLE_INVITE_EMAIL_TAKEN));
        verify(inviteRoleRepositoryMock, never()).save(Mockito.any(RoleInvite.class));
        verify(emailService, never()).sendEmail(any(), paramsArgumentCaptor.capture(), any());
    }

    @Test
    public void testGetInvite(){
        RoleInvite roleInvite = newRoleInvite().build();
        when(inviteRoleRepositoryMock.getByHash("SomeInviteHash")).thenReturn(roleInvite);
        when(roleInviteMapperMock.mapToResource(roleInvite)).thenReturn(newRoleInviteResource().build());
        ServiceResult<RoleInviteResource> result = service.getInvite("SomeInviteHash");
        assertTrue(result.isSuccess());
    }

    @Test
    public void testCheckExistingUser(){
        RoleInvite roleInvite = newRoleInvite().build();
        when(inviteRoleRepositoryMock.getByHash("SomeInviteHash")).thenReturn(roleInvite);
        when(userRepositoryMock.findByEmail(roleInvite.getEmail())).thenReturn(Optional.of(newUser().build()));
        ServiceResult<Boolean> result = service.checkExistingUser("SomeInviteHash");
        assertTrue(result.isSuccess());
        assertTrue(result.getSuccess());
    }

    @Test
    public void findPendingInternalUsers() {
        Pageable pageable = new PageRequest(0, 5);

        Role role = Role.IFS_ADMINISTRATOR;

        List<RoleInvite> roleInvites = RoleInviteBuilder.newRoleInvite()
                .withRole(role)
                .withName("Arden Pimenta")
                .withEmail("Arden.Pimenta@innovateuk.test")
                .build(4);
        Page<RoleInvite> page = new PageImpl<>(roleInvites, pageable, 4L);

        RoleInviteResource roleInviteResource = new RoleInviteResource();
        roleInviteResource.setName("Arden Pimenta");
        roleInviteResource.setEmail("Arden.Pimenta@innovateuk.test");
        roleInviteResource.setRoleName("ifs_administrator");

        when(inviteRoleRepositoryMock.findByStatus(SENT, pageable)).thenReturn(page);
        when(roleInviteMapperMock.mapToResource(Mockito.any(RoleInvite.class))).thenReturn(roleInviteResource);

        ServiceResult<RoleInvitePageResource> result = service.findPendingInternalUserInvites(pageable);
        assertTrue(result.isSuccess());

        RoleInvitePageResource resultObject = result.getSuccess();
        assertEquals(5, resultObject.getSize());
        assertEquals(1, resultObject.getTotalPages());
        assertEquals(4, resultObject.getContent().size());
        assertEquals(roleInviteResource, resultObject.getContent().get(0));

    }

    @Test
    public void findPendingInternalUsersEnsureSortedByName() {
        Pageable pageable = new PageRequest(0, 5);

        Role role = Role.IFS_ADMINISTRATOR;

        RoleInvite roleInvite1 = RoleInviteBuilder.newRoleInvite()
                .withId(1L)
                .withRole(role)
                .withName("Rianne Almeida")
                .withEmail("Rianne.Almeida@innovateuk.test")
                .build();

        RoleInvite roleInvite2 = RoleInviteBuilder.newRoleInvite()
                .withId(2L)
                .withRole(role)
                .withName("Arden Pimenta")
                .withEmail("Arden.Pimenta@innovateuk.test")
                .build();

        List<RoleInvite> roleInvites = new ArrayList<>();
        roleInvites.add(roleInvite1);
        roleInvites.add(roleInvite2);
        Page<RoleInvite> page = new PageImpl<>(roleInvites, pageable, 4L);

        RoleInviteResource roleInviteResource1 = new RoleInviteResource();
        roleInviteResource1.setName("Rianne Almeida");
        roleInviteResource1.setEmail("Rianne.Almeida@innovateuk.test");
        roleInviteResource1.setRoleName("ifs_administrator");

        RoleInviteResource roleInviteResource2 = new RoleInviteResource();
        roleInviteResource2.setName("Arden Pimenta");
        roleInviteResource2.setEmail("Arden.Pimenta@innovateuk.test");
        roleInviteResource2.setRoleName("ifs_administrator");

        when(inviteRoleRepositoryMock.findByStatus(SENT, pageable)).thenReturn(page);
        when(roleInviteMapperMock.mapToResource(roleInvite1)).thenReturn(roleInviteResource1);
        when(roleInviteMapperMock.mapToResource(roleInvite2)).thenReturn(roleInviteResource2);

        ServiceResult<RoleInvitePageResource> result = service.findPendingInternalUserInvites(pageable);
        assertTrue(result.isSuccess());

        RoleInvitePageResource resultObject = result.getSuccess();

        // Ensure they are sorted by name
        assertEquals(roleInviteResource2, resultObject.getContent().get(0));
        assertEquals(roleInviteResource1, resultObject.getContent().get(1));

    }

    @Test
    public void findExternalInvitesWhenSearchStringIsNull(){

        String searchString = null;
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
    }

    @Test
    public void findExternalInvitesWhenSearchStringIsEmpty(){

        String searchString = "";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
    }

    @Test
    public void findExternalInvitesWhenSearchStringLengthLessThan5(){

        String searchString = "a";
        String searchStringExpr = "%a%";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
    }

    @Test
    public void findExternalInvitesWhenSearchStringIsAllSpaces(){

        String searchString = "          ";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchString, EnumSet.of(CREATED, SENT));
    }

    @Test
    public void findExternalInvitesWhenSearchCategoryIsName() {

        String searchString = "smith";
        String searchStringExpr = "%smith%";
        SearchCategory searchCategory = SearchCategory.NAME;

        List<ApplicationInvite> applicationInvites = setUpMockingCreateApplicationInvites();
        List<ProjectInvite> projectInvites = setUpMockingCreateProjectInvites();

        when(applicationInviteRepositoryMock.findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(applicationInvites);
        when(inviteProjectRepositoryMock.findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(projectInvites);

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertFindExternalInvites(result);

        verify(applicationInviteRepositoryMock).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

    }

    @Test
    public void findExternalInvitesWhenSearchCategoryIsOrganisationName() {

        String searchString = "smith";
        String searchStringExpr = "%smith%";
        SearchCategory searchCategory = SearchCategory.ORGANISATION_NAME;

        List<ApplicationInvite> applicationInvites = setUpMockingCreateApplicationInvites();
        List<ProjectInvite> projectInvites = setUpMockingCreateProjectInvites();

        when(applicationInviteRepositoryMock.findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(applicationInvites);
        when(inviteProjectRepositoryMock.findByOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(projectInvites);

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertFindExternalInvites(result);

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock).findByOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

    }

    @Test
    public void findExternalInvitesWhenSearchCategoryIsEmail() {

        String searchString = "smith";
        String searchStringExpr = "%smith%";
        SearchCategory searchCategory = SearchCategory.EMAIL;

        List<ApplicationInvite> applicationInvites = setUpMockingCreateApplicationInvites();
        List<ProjectInvite> projectInvites = setUpMockingCreateProjectInvites();

        when(applicationInviteRepositoryMock.findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(applicationInvites);
        when(inviteProjectRepositoryMock.findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT))).thenReturn(projectInvites);

        ServiceResult<List<ExternalInviteResource>> result = service.findExternalInvites(searchString, searchCategory);

        assertFindExternalInvites(result);

        verify(applicationInviteRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock, never()).findByInviteOrganisationOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(applicationInviteRepositoryMock).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

        verify(inviteProjectRepositoryMock, never()).findByNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock, never()).findByOrganisationNameLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));
        verify(inviteProjectRepositoryMock).findByEmailLikeAndStatusIn(searchStringExpr, EnumSet.of(CREATED, SENT));

    }

    @Test
    public void resendInternalUserInvite() {

        Role targetRole = Role.PROJECT_FINANCE;

        RoleInvite existingInvite = newRoleInvite().
                withName("name").
                withEmail("e@mail.com").
                withTarget(targetRole).
                withHash("hashhashhash").
                build();

        when(inviteRoleRepositoryMock.findOne(123L)).thenReturn(existingInvite);

        Role roleResource = Role.PROJECT_FINANCE;

        NotificationTarget notificationTarget = new ExternalUserNotificationTarget(existingInvite.getName(), existingInvite.getEmail());

        Map<String, Object> emailTemplateArgs = asMap("role", roleResource.getDisplayName(),
                "inviteUrl", "base/management/registration/hashhashhash/register");

        when(emailService.sendEmail(singletonList(notificationTarget), emailTemplateArgs, INVITE_INTERNAL_USER)).thenReturn(serviceSuccess());

        User loggedInUser = newUser().build();
        when(loggedInUserSupplierMock.get()).thenReturn(loggedInUser);

        ServiceResult<Void> result = service.resendInternalUserInvite(123L);
        assertTrue(result.isSuccess());

        // assert the email was sent with the correct hash, and that the invite was saved (not strictly necessary
        // in this case to explicitly save, but is reused code with creating invites also)
        verify(emailService).sendEmail(singletonList(notificationTarget), emailTemplateArgs, INVITE_INTERNAL_USER);
        verify(inviteRoleRepositoryMock).save(existingInvite);

        // and verify that the sent on field has been updated so that this link will not expire soon
        assertThat(existingInvite.getSentBy(), equalTo(loggedInUser));
        assertThat(existingInvite.getSentOn(), lessThanOrEqualTo(now()));
        assertThat(existingInvite.getSentOn().plus(50, MILLIS), greaterThan(now()));

    }

    @Test
    public void resendInternalUserInviteButInviteNotFound() {

        when(inviteRoleRepositoryMock.findOne(123L)).thenReturn(null);

        ServiceResult<Void> result = service.resendInternalUserInvite(123L);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(RoleInvite.class, 123L)));

        // assert the email was sent with the correct hash, and that the invite was saved (not strictly necessary
        // in this case to explicitly save, but is reused code with creating invites also)
        verify(inviteRoleRepositoryMock).findOne(123L);
        verifyNoMoreInteractions(inviteRoleRepositoryMock, emailService);
    }

    private List<ApplicationInvite> setUpMockingCreateApplicationInvites() {

        Application app = newApplication().build();
        List<Organisation> appOrganisations = newOrganisation().withName("Tesla", "Columbia Data Products").build(2);
        List<InviteOrganisation> inviteOrganisations = newInviteOrganisation()
                .withOrganisation(appOrganisations.get(0), appOrganisations.get(1), null)
                .withOrganisationName(null, null, "Rolls Royce Plc").build(3);

        List<ApplicationInvite> applicationInvites = newApplicationInvite().withApplication(app)
                .withEmail("x@email.com", "b@email.com", "zz@email.com")
                .withInviteOrganisation(inviteOrganisations.get(0), inviteOrganisations.get(1), inviteOrganisations.get(2)).build(3);

        return applicationInvites;
    }

    private List<ProjectInvite> setUpMockingCreateProjectInvites() {

        Application app = newApplication().build();
        Project prj = newProject().withApplication(app).build();

        List<Organisation> organisations = newOrganisation().withName("Cardiff Electric", "Mutiny").build(2);
        List<ProjectInvite> projectInvites = newProjectInvite().withProject(prj)
                .withOrganisation(organisations.get(0), organisations.get(1))
                .withEmail("z@email.com", "u@email.com").build(2);

        return projectInvites;
    }

    private void assertFindExternalInvites(ServiceResult<List<ExternalInviteResource>> result) {
        assertTrue(result.isSuccess());
        assertEquals(5, result.getSuccess().size());
        assertEquals("b@email.com", result.getSuccess().get(0).getEmail());
        assertEquals("Columbia Data Products", result.getSuccess().get(0).getOrganisationName());

        assertEquals("u@email.com", result.getSuccess().get(1).getEmail());
        assertEquals("Mutiny", result.getSuccess().get(1).getOrganisationName());

        assertEquals("x@email.com", result.getSuccess().get(2).getEmail());
        assertEquals("Tesla", result.getSuccess().get(2).getOrganisationName());

        assertEquals("z@email.com", result.getSuccess().get(3).getEmail());
        assertEquals("Cardiff Electric", result.getSuccess().get(3).getOrganisationName());

        // The one without pre-exiting organisation has name set correctly
        assertEquals("zz@email.com", result.getSuccess().get(4).getEmail());
        assertEquals("Rolls Royce Plc", result.getSuccess().get(4).getOrganisationName());
    }
}