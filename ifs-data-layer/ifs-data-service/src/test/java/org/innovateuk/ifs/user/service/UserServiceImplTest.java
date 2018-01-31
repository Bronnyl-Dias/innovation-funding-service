package org.innovateuk.ifs.user.service;

import org.innovateuk.ifs.BaseServiceUnitTest;
import org.innovateuk.ifs.commons.error.CommonErrors;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.notifications.resource.Notification;
import org.innovateuk.ifs.notifications.resource.NotificationMedium;
import org.innovateuk.ifs.token.domain.Token;
import org.innovateuk.ifs.token.resource.TokenType;
import org.innovateuk.ifs.user.builder.OrganisationBuilder;
import org.innovateuk.ifs.user.domain.Role;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.resource.*;
import org.innovateuk.ifs.user.transactional.UserService;
import org.innovateuk.ifs.user.transactional.UserServiceImpl;
import org.innovateuk.ifs.userorganisation.domain.UserOrganisation;
import org.innovateuk.ifs.userorganisation.mapper.UserOrganisationMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.USER_SEARCH_INVALID_INPUT_LENGTH;
import static org.innovateuk.ifs.user.builder.RoleBuilder.newRole;
import static org.innovateuk.ifs.user.builder.RoleResourceBuilder.newRoleResource;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.user.builder.UserOrganisationResourceBuilder.newUserOrganisationResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.user.resource.UserRoleType.externalApplicantRoles;
import static org.innovateuk.ifs.userorganisation.builder.UserOrganisationBuilder.newUserOrganisation;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests of the UserService class
 */
public class UserServiceImplTest extends BaseServiceUnitTest<UserService> {

    private static final String WEB_BASE_URL = "baseUrl";

    @Captor
    ArgumentCaptor<Notification> notificationArgumentCaptor;

    @Mock
    UserOrganisationMapper userOrganisationMapperMock;

    @Test
    public void testChangePassword() {

        User user = newUser().build();
        UserResource userResource = newUserResource().withUID("myuid").build();

        Token token = new Token(TokenType.RESET_PASSWORD, null, 123L, null, null, null);
        when(tokenServiceMock.getPasswordResetToken("myhash")).thenReturn(ServiceResult.serviceSuccess(token));
        when(userRepositoryMock.findOne(123L)).thenReturn(user);
        when(userMapperMock.mapToResource(user)).thenReturn(userResource);
        when(passwordPolicyValidatorMock.validatePassword("mypassword", userResource)).thenReturn(ServiceResult.serviceSuccess());
        when(idpServiceMock.updateUserPassword("myuid", "mypassword")).thenReturn(ServiceResult.serviceSuccess("mypassword"));

        service.changePassword("myhash", "mypassword").getSuccessObjectOrThrowException();

        verify(tokenRepositoryMock).delete(token);
    }

    @Test
    public void testChangePasswordButPasswordValidationFails() {

        User user = newUser().build();
        UserResource userResource = newUserResource().withUID("myuid").build();

        Token token = new Token(TokenType.RESET_PASSWORD, null, 123L, null, null, null);
        when(tokenServiceMock.getPasswordResetToken("myhash")).thenReturn(ServiceResult.serviceSuccess(token));
        when(userRepositoryMock.findOne(123L)).thenReturn(user);
        when(userMapperMock.mapToResource(user)).thenReturn(userResource);
        when(passwordPolicyValidatorMock.validatePassword("mypassword", userResource)).thenReturn(ServiceResult.serviceFailure(CommonErrors.badRequestError("bad password")));

        ServiceResult<Void> result = service.changePassword("myhash", "mypassword");
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(CommonErrors.badRequestError("bad password")));
        verify(tokenRepositoryMock, never()).delete(token);
    }

    @Test
    public void testChangePasswordButPasswordValidationFailsOnIDP() {
        final User user = newUser().build();
        final UserResource userResource = newUserResource().withUID("myuid").build();
        final String password = "mypassword";

        Token token = new Token(TokenType.RESET_PASSWORD, null, 123L, null, null, null);
        when(tokenServiceMock.getPasswordResetToken("myhash")).thenReturn(ServiceResult.serviceSuccess(token));
        when(userRepositoryMock.findOne(123L)).thenReturn(user);
        when(userMapperMock.mapToResource(user)).thenReturn(userResource);

        when(passwordPolicyValidatorMock.validatePassword(password, userResource)).thenReturn(ServiceResult.serviceSuccess());
        when(idpServiceMock.updateUserPassword(anyString(), anyString())).thenReturn(ServiceResult.serviceFailure(CommonErrors.badRequestError("bad password")));

        ServiceResult<Void> result = service.changePassword("myhash", password);
        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(CommonErrors.badRequestError("bad password")));
        verify(tokenRepositoryMock, never()).delete(token);
    }

    @Test
    public void testFindInactiveByEmail() {
        final User user = newUser().build();
        final UserResource userResource = newUserResource()
                .withEmail("a@b.c")
                .withLastName("A")
                .withLastName("Bee")
                .build();
        final String email = "sample@me.com";

        when(userRepositoryMock.findByEmailAndStatus(email, UserStatus.INACTIVE)).thenReturn(of(user));
        when(userMapperMock.mapToResource(user)).thenReturn(userResource);

        final ServiceResult<UserResource> result = service.findInactiveByEmail(email);
        assertTrue(result.isSuccess());
        assertSame(userResource, result.getSuccess());
        verify(userRepositoryMock, only()).findByEmailAndStatus(email, UserStatus.INACTIVE);
    }

    @Test
    public void testSendPasswordResetNotification() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.ACTIVE)
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(notificationServiceMock.sendNotification(any(), eq(NotificationMedium.EMAIL))).thenReturn(ServiceResult.serviceSuccess());

        service.sendPasswordResetNotification(user).getSuccessObjectOrThrowException();

        verify(notificationServiceMock).sendNotification(notificationArgumentCaptor.capture(), eq(NotificationMedium.EMAIL));

        assertEquals(UserServiceImpl.Notifications.RESET_PASSWORD, notificationArgumentCaptor.getValue().getMessageKey());
        assertEquals(user.getEmail(), notificationArgumentCaptor.getValue().getTo().get(0).getEmailAddress());
        assertEquals(user.getName(), notificationArgumentCaptor.getValue().getTo().get(0).getName());
        assertTrue(notificationArgumentCaptor.getValue().getGlobalArguments().get("passwordResetLink").toString().startsWith("baseUrl/login/reset-password/hash/"));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveApplicantNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.APPLICANT)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveApplicantHasVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        asList(
                                newRoleResource()
                                        .withType(UserRoleType.APPLICANT)
                                        .build(),
                                newRoleResource()
                                        .withType(UserRoleType.ASSESSOR)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.of(new Token()));
        when(registrationServiceMock.resendUserVerificationEmail(user)).thenReturn(ServiceResult.serviceSuccess());

        service.sendPasswordResetNotification(user).getSuccessObjectOrThrowException();

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());
    }

    @Test
    public void testSendPasswordResetNotificationInactiveAssessor() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.ASSESSOR)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveProjectFinance() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.PROJECT_FINANCE)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveCompAdmin() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.COMP_ADMIN)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveCompExec() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.COMP_EXEC)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveCompTechnologist() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.INNOVATION_LEAD)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveLeadApplicantNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE).withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.LEADAPPLICANT)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactivePartnerNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE).withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.PARTNER)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveProjectManagerNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE).withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.PROJECT_MANAGER)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveCollaboratorNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.COLLABORATOR)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testSendPasswordResetNotificationInactiveFinanceContactNoVerifyToken() {
        final UserResource user = newUserResource()
                .withStatus(UserStatus.INACTIVE)
                .withRolesGlobal(
                        singletonList(
                                newRoleResource()
                                        .withType(UserRoleType.FINANCE_CONTACT)
                                        .build()))
                .withEmail("a@b.c")
                .withFirstName("A")
                .withLastName("Bee")
                .build();

        when(tokenRepositoryMock.findByTypeAndClassNameAndClassPk(TokenType.VERIFY_EMAIL_ADDRESS, User.class.getCanonicalName(), user.getId())).thenReturn(Optional.empty());
        ServiceResult<Void> result = service.sendPasswordResetNotification(user);

        verify(tokenRepositoryMock).findByTypeAndClassNameAndClassPk(any(), any(), any());

        assertTrue(result.isFailure());
        assertTrue(result.getFailure().is(notFoundError(UserResource.class, user.getEmail(),UserStatus.ACTIVE)));
    }

    @Test
    public void testFindActiveByProcessRoles(){
        Set<Role> internalRoles = new HashSet<>();
        internalRoles.add(newRole().withType(UserRoleType.PROJECT_FINANCE).build());
        Pageable pageable = new PageRequest(0, 5);
        List<User> activeUsers = newUser().withStatus(UserStatus.ACTIVE).withRoles(internalRoles).build(6);
        Page<User> expectedPage = new PageImpl<>(activeUsers, pageable, 6L);

        when(userRepositoryMock.findDistinctByStatusAndRolesNameIn(UserStatus.ACTIVE, UserRoleType.internalRoles().stream().map(UserRoleType::getName).collect(Collectors.toSet()), pageable)).thenReturn(expectedPage);
        when(userMapperMock.mapToResource(any(User.class))).thenReturn(newUserResource().withFirstName("First").build());

        ServiceResult<UserPageResource> result = service.findActiveByProcessRoles(UserRoleType.internalRoles(), pageable);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getSuccess().getSize());
        assertEquals(2, result.getSuccess().getTotalPages());
        assertEquals(6, result.getSuccess().getContent().size());
    }

    @Test
    public void testFindActiveByProcessRolesEnsureSortedByFirstName(){
        Set<Role> internalRoles = new HashSet<>();
        internalRoles.add(newRole().withType(UserRoleType.PROJECT_FINANCE).build());
        Pageable pageable = new PageRequest(0, 5);

        User user1 = newUser()
                .withFirstName("Rianne")
                .withLastName("Almeida")
                .withStatus(UserStatus.ACTIVE).withRoles(internalRoles).build();

        User user2 = newUser()
                .withFirstName("Arden")
                .withLastName("Pimenta")
                .withStatus(UserStatus.ACTIVE).withRoles(internalRoles).build();

        List<User> activeUsers = new ArrayList<>();
        activeUsers.add(user1);
        activeUsers.add(user2);
        Page<User> expectedPage = new PageImpl<>(activeUsers, pageable, 6L);

        UserResource userResource1 = newUserResource()
                .withFirstName("Rianne")
                .withLastName("Almeida")
                .build();

        UserResource userResource2 = newUserResource()
                .withFirstName("Arden")
                .withLastName("Pimenta")
                .build();

        when(userRepositoryMock.findDistinctByStatusAndRolesNameIn(UserStatus.ACTIVE, UserRoleType.internalRoles().stream().map(UserRoleType::getName).collect(Collectors.toSet()), pageable)).thenReturn(expectedPage);
        when(userMapperMock.mapToResource(user1)).thenReturn(userResource1);
        when(userMapperMock.mapToResource(user2)).thenReturn(userResource2);

        ServiceResult<UserPageResource> result = service.findActiveByProcessRoles(UserRoleType.internalRoles(), pageable);

        assertTrue(result.isSuccess());
        UserPageResource resultObject = result.getSuccess();
        assertEquals(userResource2, resultObject.getContent().get(0));
        assertEquals(userResource1, resultObject.getContent().get(1));
    }

    @Test
    public void testFindInactiveByProcessRoles(){
        Set<Role> internalRoles = new HashSet<>();
        internalRoles.add(newRole().withType(UserRoleType.COMP_ADMIN).build());
        Pageable pageable = new PageRequest(0, 5);
        List<User> inactiveUsers = newUser().withStatus(UserStatus.INACTIVE).withRoles(internalRoles).build(4);
        Page<User> expectedPage = new PageImpl<>(inactiveUsers, pageable, 4L);

        when(userRepositoryMock.findDistinctByStatusAndRolesNameIn(UserStatus.INACTIVE, UserRoleType.internalRoles().stream().map(UserRoleType::getName).collect(Collectors.toSet()), pageable)).thenReturn(expectedPage);
        when(userMapperMock.mapToResource(any(User.class))).thenReturn(newUserResource().withFirstName("First").build());

        ServiceResult<UserPageResource> result = service.findInactiveByProcessRoles(UserRoleType.internalRoles(), pageable);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getSuccess().getSize());
        assertEquals(1, result.getSuccess().getTotalPages());
        assertEquals(4, result.getSuccess().getContent().size());
    }

    @Test
    public void testFindInactiveByProcessRolesEnsureSortedByFirstName(){
        Set<Role> internalRoles = new HashSet<>();
        internalRoles.add(newRole().withType(UserRoleType.COMP_ADMIN).build());
        Pageable pageable = new PageRequest(0, 5);

        User user1 = newUser()
                .withFirstName("Rianne")
                .withLastName("Almeida")
                .withStatus(UserStatus.ACTIVE).withRoles(internalRoles).build();

        User user2 = newUser()
                .withFirstName("Arden")
                .withLastName("Pimenta")
                .withStatus(UserStatus.ACTIVE).withRoles(internalRoles).build();

        List<User> inactiveUsers = new ArrayList<>();
        inactiveUsers.add(user1);
        inactiveUsers.add(user2);
        Page<User> expectedPage = new PageImpl<>(inactiveUsers, pageable, 4L);

        UserResource userResource1 = newUserResource()
                .withFirstName("Rianne")
                .withLastName("Almeida")
                .build();

        UserResource userResource2 = newUserResource()
                .withFirstName("Arden")
                .withLastName("Pimenta")
                .build();

        when(userRepositoryMock.findDistinctByStatusAndRolesNameIn(UserStatus.INACTIVE, UserRoleType.internalRoles().stream().map(UserRoleType::getName).collect(Collectors.toSet()), pageable)).thenReturn(expectedPage);
        when(userMapperMock.mapToResource(user1)).thenReturn(userResource1);
        when(userMapperMock.mapToResource(user2)).thenReturn(userResource2);

        ServiceResult<UserPageResource> result = service.findInactiveByProcessRoles(UserRoleType.internalRoles(), pageable);

        assertTrue(result.isSuccess());

        UserPageResource resultObject = result.getSuccess();
        assertEquals(userResource2, resultObject.getContent().get(0));
        assertEquals(userResource1, resultObject.getContent().get(1));
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchStringIsNull(){

        String searchString = null;
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationMapperMock, never()).mapToResource(any(UserOrganisation.class));
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchStringIsEmpty(){

        String searchString = "";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationMapperMock, never()).mapToResource(any(UserOrganisation.class));
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchStringLengthLessThan5(){

        String searchString = "a";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationMapperMock, never()).mapToResource(any(UserOrganisation.class));
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchStringIsAllSpaces(){

        String searchString = "          ";
        SearchCategory searchCategory = SearchCategory.NAME;

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isFailure());
        assertEquals(USER_SEARCH_INVALID_INPUT_LENGTH.getErrorKey(), result.getFailure().getErrors().get(0).getErrorKey());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationMapperMock, never()).mapToResource(any(UserOrganisation.class));
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchCategoryIsName(){

        String searchString = "%well%";
        SearchCategory searchCategory = SearchCategory.NAME;

        List<UserOrganisation> userOrganisations = setUpMockingFindByProcessRolesAndSearchCriteria();

        when(userOrganisationRepositoryMock.findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet())).thenReturn(userOrganisations);

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccess().size());
        assertEquals("Aaron Powell", result.getSuccess().get(0).getName());
        assertEquals("David Wellington", result.getSuccess().get(1).getName());

        verify(userOrganisationRepositoryMock).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
    }

    private List<UserOrganisation> setUpMockingFindByProcessRolesAndSearchCriteria() {

        UserOrganisationResource userOrganisationResource1 = newUserOrganisationResource().withName("Aaron Powell").withEmail("aaron.powell@example.com").withStatus(UserStatus.ACTIVE)
                .withOrganisationId(1L).withOrganisationName("Guitar Gods Ltd").build();
        UserOrganisationResource userOrganisationResource2 = newUserOrganisationResource().withName("David Wellington").withEmail("david.wellington@load.example.com").withStatus(UserStatus.ACTIVE)
                .withOrganisationId(2L).withOrganisationName("Engine Equations Ltd").build();

        List<UserOrganisation> userOrganisations = newUserOrganisation()
                .withUser(newUser().withEmailAddress("aaron.powell@example.com").build(),
                        newUser().withEmailAddress("david.wellington@load.example.com").build())
                .withOrganisation(OrganisationBuilder.newOrganisation().withId(1L).withName("Guitar Gods Ltd").build(),
                        OrganisationBuilder.newOrganisation().withId(2L).withName("Engine Equations Ltd").build())
                .build(2);

        when(userOrganisationMapperMock.mapToResource(userOrganisations.get(0))).thenReturn(userOrganisationResource1);
        when(userOrganisationMapperMock.mapToResource(userOrganisations.get(1))).thenReturn(userOrganisationResource2);

        return userOrganisations;
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchCategoryIsOrganisationName(){

        String searchString = "%Ltd%";
        SearchCategory searchCategory = SearchCategory.ORGANISATION_NAME;

        List<UserOrganisation> userOrganisations = setUpMockingFindByProcessRolesAndSearchCriteria();

        when(userOrganisationRepositoryMock.findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet())).thenReturn(userOrganisations);

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccess().size());
        assertEquals("Guitar Gods Ltd", result.getSuccess().get(0).getOrganisationName());
        assertEquals("Engine Equations Ltd", result.getSuccess().get(1).getOrganisationName());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
    }

    @Test
    public void findByProcessRolesAndSearchCriteriaWhenSearchCategoryIsEmail(){

        String searchString = "%com%";
        SearchCategory searchCategory = SearchCategory.EMAIL;

        List<UserOrganisation> userOrganisations = setUpMockingFindByProcessRolesAndSearchCriteria();

        when(userOrganisationRepositoryMock.findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet())).thenReturn(userOrganisations);

        ServiceResult<List<UserOrganisationResource>> result = service.findByProcessRolesAndSearchCriteria(externalApplicantRoles(), searchString, searchCategory);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSuccess().size());
        assertEquals("aaron.powell@example.com", result.getSuccess().get(0).getEmail());
        assertEquals("david.wellington@load.example.com", result.getSuccess().get(1).getEmail());

        verify(userOrganisationRepositoryMock, never()).findByUserFirstNameLikeOrUserLastNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anyString(), anySet());
        verify(userOrganisationRepositoryMock, never()).findByOrganisationNameLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
        verify(userOrganisationRepositoryMock).findByUserEmailLikeAndUserRolesNameInOrderByIdUserEmailAsc(anyString(), anySet());
    }

    @Override
    protected UserService supplyServiceUnderTest() {
        UserServiceImpl spendProfileService = new UserServiceImpl();
        ReflectionTestUtils.setField(spendProfileService, "webBaseUrl", WEB_BASE_URL);
        return spendProfileService;
    }
}
