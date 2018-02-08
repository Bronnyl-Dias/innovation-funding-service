package org.innovateuk.ifs.invite.transactional;

import org.innovateuk.ifs.BaseServiceUnitTest;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.invite.domain.ApplicationInvite;
import org.innovateuk.ifs.invite.domain.InviteOrganisation;
import org.innovateuk.ifs.user.domain.Organisation;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.domain.Role;
import org.innovateuk.ifs.user.domain.User;
import org.junit.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.invite.builder.ApplicationInviteBuilder.newApplicationInvite;
import static org.innovateuk.ifs.invite.builder.InviteOrganisationBuilder.newInviteOrganisation;
import static org.innovateuk.ifs.user.builder.OrganisationBuilder.newOrganisation;
import static org.innovateuk.ifs.user.builder.RoleBuilder.newRole;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.user.resource.UserRoleType.COLLABORATOR;
import static org.mockito.Mockito.*;

public class AcceptInviteServiceImplTest extends BaseServiceUnitTest<AcceptInviteServiceImpl> {

    private final String testInviteHash = "abcdef";

    @Override
    protected AcceptInviteServiceImpl supplyServiceUnderTest() {
        return new AcceptInviteServiceImpl();
    }

    @Test
    public void acceptInvite_failsOnEmailAddressMismatch() {
        ApplicationInvite invite = newApplicationInvite()
                .withEmail("james@test.com")
                .build();
        User user = newUser()
                .withEmailAddress("bob@test.com")
                .build();

        when(applicationInviteRepositoryMock.getByHash(testInviteHash)).thenReturn(invite);
        when(userRepositoryMock.findOne(user.getId())).thenReturn(user);

        ServiceResult<Void> result = service.acceptInvite(testInviteHash, user.getId());

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    public void acceptInvite_replaceWithExistingCollaboratorInviteOrganisation() {
        InviteOrganisation inviteOrganisationToBeReplaced = newInviteOrganisation().build();

        ApplicationInvite invite = createAndExpectInvite(inviteOrganisationToBeReplaced);
        User user = createAndExpectInviteUser();
        Organisation usersCurrentOrganisation = createAndExpectUsersCurrentOrganisation(user);

        InviteOrganisation collaboratorInviteOrganisation = newInviteOrganisation()
                .withOrganisation(usersCurrentOrganisation)
                .build();

        when(inviteOrganisationRepositoryMock.findFirstByOrganisationIdAndInvitesApplicationId(
                usersCurrentOrganisation.getId(),
                invite.getTarget().getId()
        ))
                .thenReturn(Optional.of(collaboratorInviteOrganisation));

        when(applicationServiceMock.getProgressPercentageBigDecimalByApplicationId(invite.getTarget().getId()))
                .thenReturn(serviceSuccess(BigDecimal.ONE));

        ServiceResult<Void> result = service.acceptInvite(testInviteHash, user.getId());

        InOrder inOrder = inOrder(inviteOrganisationRepositoryMock, applicationInviteRepositoryMock);
        inOrder.verify(inviteOrganisationRepositoryMock).saveAndFlush(inviteOrganisationToBeReplaced);
        inOrder.verify(inviteOrganisationRepositoryMock).delete(inviteOrganisationToBeReplaced);
        inOrder.verify(applicationInviteRepositoryMock).save(invite);

        assertThat(result.isSuccess()).isTrue();
        assertThat(invite.getInviteOrganisation())
                .isEqualToComparingFieldByField(collaboratorInviteOrganisation);
    }

    @Test
    public void acceptInvite_previousInviteOrganisationIsNotDeletedIfThereAreOtherInvitesAttached() {
        InviteOrganisation inviteOrganisationToBeReplaced = newInviteOrganisation()
                .withInvites(newApplicationInvite().build(2))
                .build();

        ApplicationInvite invite = createAndExpectInvite(inviteOrganisationToBeReplaced);
        User user = createAndExpectInviteUser();
        Organisation usersCurrentOrganisation = createAndExpectUsersCurrentOrganisation(user);

        InviteOrganisation collaboratorInviteOrganisation = newInviteOrganisation()
                .withOrganisation(usersCurrentOrganisation)
                .build();

        when(inviteOrganisationRepositoryMock.findFirstByOrganisationIdAndInvitesApplicationId(
                usersCurrentOrganisation.getId(),
                invite.getTarget().getId()
        ))
                .thenReturn(Optional.of(collaboratorInviteOrganisation));

        when(applicationServiceMock.getProgressPercentageBigDecimalByApplicationId(invite.getTarget().getId()))
                .thenReturn(serviceSuccess(BigDecimal.ONE));

        ServiceResult<Void> result = service.acceptInvite(testInviteHash, user.getId());

        verify(inviteOrganisationRepositoryMock, never()).delete(inviteOrganisationToBeReplaced);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void acceptInvite_assignsUsersCurrentOrganisationIfNoCollaboratorOrganisationExists() {
        ApplicationInvite invite = createAndExpectInvite(newInviteOrganisation().build());
        User user = createAndExpectInviteUser();
        Organisation usersCurrentOrganisation = createAndExpectUsersCurrentOrganisation(user);

        when(inviteOrganisationRepositoryMock.findFirstByOrganisationIdAndInvitesApplicationId(
                usersCurrentOrganisation.getId(),
                invite.getTarget().getId()
        ))
                .thenReturn(Optional.empty());

        when(applicationServiceMock.getProgressPercentageBigDecimalByApplicationId(invite.getTarget().getId()))
                .thenReturn(serviceSuccess(BigDecimal.ONE));

        ServiceResult<Void> result = service.acceptInvite(testInviteHash, user.getId());

        verify(applicationInviteRepositoryMock).save(invite);

        assertThat(result.isSuccess()).isTrue();
        assertThat(invite.getInviteOrganisation().getOrganisation())
                .isEqualToComparingFieldByField(usersCurrentOrganisation);
    }

    @Test
    public void acceptInvite_processRoleIsInitialisedCorrectly() {
        ApplicationInvite invite = createAndExpectInvite(newInviteOrganisation().build());
        User user = createAndExpectInviteUser();
        Organisation usersCurrentOrganisation = createAndExpectUsersCurrentOrganisation(user);

        Role collaboratorRole = newRole(COLLABORATOR).build();

        when(inviteOrganisationRepositoryMock.findFirstByOrganisationIdAndInvitesApplicationId(
                usersCurrentOrganisation.getId(),
                invite.getTarget().getId()
        ))
                .thenReturn(Optional.empty());

        when(roleRepositoryMock.findOneByName(COLLABORATOR.getName())).thenReturn(collaboratorRole);

        ProcessRole expectedProcessRole = new ProcessRole(
                user,
                invite.getTarget().getId(),
                collaboratorRole,
                usersCurrentOrganisation.getId()
        );

        when(processRoleRepositoryMock.save(expectedProcessRole)).thenReturn(expectedProcessRole);
        when(applicationServiceMock.getProgressPercentageBigDecimalByApplicationId(invite.getTarget().getId()))
                .thenReturn(serviceSuccess(BigDecimal.ONE));

        ServiceResult<Void> result = service.acceptInvite(testInviteHash, user.getId());

        verify(applicationInviteRepositoryMock).save(invite);
        verify(processRoleRepositoryMock).save(expectedProcessRole);

        assertThat(result.isSuccess()).isTrue();
        assertThat(invite.getTarget().getProcessRoles())
                .contains(expectedProcessRole);
    }

    private User createAndExpectInviteUser() {
        User user = newUser()
                .withEmailAddress("james@test.com")
                .build();

        when(userRepositoryMock.findOne(user.getId())).thenReturn(user);

        return user;
    }

    private ApplicationInvite createAndExpectInvite(InviteOrganisation inviteOrganisation) {
        ApplicationInvite invite = newApplicationInvite()
                .withEmail("james@test.com")
                .withApplication(newApplication().build())
                .withInviteOrganisation(inviteOrganisation)
                .build();

        when(applicationInviteRepositoryMock.getByHash(testInviteHash)).thenReturn(invite);

        return invite;
    }

    private Organisation createAndExpectUsersCurrentOrganisation(User user) {
        List<Organisation> usersOrganisations = newOrganisation()
                .withUser(singletonList(user))
                .build(1);

        when(organisationRepositoryMock.findByUsers(user)).thenReturn(usersOrganisations);

        return usersOrganisations.get(0);
    }

}
