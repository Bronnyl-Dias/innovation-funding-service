package org.innovateuk.ifs.project.status.security;

import org.innovateuk.ifs.BasePermissionRulesTest;
import org.innovateuk.ifs.application.builder.ApplicationResourceBuilder;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.commons.BaseIntegrationTest;
import org.innovateuk.ifs.commons.error.exception.ForbiddenActionException;
import org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.organisation.resource.OrganisationCompositeId;
import org.innovateuk.ifs.project.ProjectServiceImpl;
import org.innovateuk.ifs.project.builder.ProjectResourceBuilder;
import org.innovateuk.ifs.project.constant.ProjectActivityStates;
import org.innovateuk.ifs.project.resource.*;
import org.innovateuk.ifs.project.sections.SectionAccess;
import org.innovateuk.ifs.project.status.resource.ProjectTeamStatusResource;
import org.innovateuk.ifs.user.resource.OrganisationResource;
import org.innovateuk.ifs.user.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.CANNOT_GET_ANY_USERS_FOR_PROJECT;
import static org.innovateuk.ifs.project.builder.ProjectPartnerStatusResourceBuilder.newProjectPartnerStatusResource;
import static org.innovateuk.ifs.project.builder.ProjectResourceBuilder.newProjectResource;
import static org.innovateuk.ifs.project.builder.ProjectTeamStatusResourceBuilder.newProjectTeamStatusResource;
import static org.innovateuk.ifs.project.builder.ProjectUserResourceBuilder.newProjectUserResource;
import static org.innovateuk.ifs.project.sections.SectionAccess.ACCESSIBLE;
import static org.innovateuk.ifs.project.sections.SectionAccess.NOT_ACCESSIBLE;
import static org.innovateuk.ifs.user.builder.OrganisationResourceBuilder.newOrganisationResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.user.resource.OrganisationTypeEnum.BUSINESS;
import static org.innovateuk.ifs.user.resource.Role.FINANCE_CONTACT;
import static org.innovateuk.ifs.user.resource.Role.PARTNER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SetupSectionsPermissionRulesTest extends BasePermissionRulesTest<SetupSectionsPermissionRules> {

    @Mock
    private SetupSectionsPermissionRules.SetupSectionPartnerAccessorSupplier accessorSupplier;

    @Mock
    private SetupSectionAccessibilityHelper accessor;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UserResource user = newUserResource().build();

    @Before
    public void setupAccessorLookup() {
        when(accessorSupplier.apply(isA(ProjectTeamStatusResource.class))).thenReturn(accessor);
    }

    @Test(expected = ForbiddenActionException.class)
    public void projectDetailsSectionAccess() {
        assertScenariousForSections(SetupSectionAccessibilityHelper::canAccessProjectDetailsSection, () -> rules.partnerCanAccessProjectDetailsSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void projectManagerPageAccess() {
        assertLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::leadCanAccessProjectManagerPage,
                () -> rules.leadCanAccessProjectManagerPage(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void projectStartDatePageAccess() {
        assertLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::leadCanAccessProjectStartDatePage,
                () -> rules.leadCanAccessProjectStartDatePage(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void projectAddressPageAccess() {
        assertLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::leadCanAccessProjectAddressPage,
                () -> rules.leadCanAccessProjectAddressPage(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void financeContactPageAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessFinanceContactPage, () -> rules.partnerCanAccessFinanceContactPage(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void partnerProjectLocationPageAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessPartnerProjectLocationPage, () -> rules.partnerCanAccessProjectLocationPage(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void monitoringOfficerSectionAccess() {
        Long projectId = 123L;
        Long applicationId = 1L;
        Long competitionId = 11L;

        CompetitionResource competitionInDb = CompetitionResourceBuilder.newCompetitionResource()
                .withLocationPerPartner(true)
                .build();

        ApplicationResource applicationInDb = ApplicationResourceBuilder.newApplicationResource()
                .withId(applicationId)
                .withCompetition(competitionId)
                .build();

        ProjectResource projectInDb = ProjectResourceBuilder.newProjectResource()
                .withId(projectId)
                .withApplication(applicationInDb.getId())
                .build();

        when(projectServiceMock.getById(projectId)).thenReturn(projectInDb);
        when(applicationServiceMock.getById(applicationId)).thenReturn(applicationInDb);
        when(competitionServiceMock.getById(competitionId)).thenReturn(competitionInDb);


        assertNonLeadPartnerSuccessfulAccess((setupSectionAccessibilityHelper, organisation) -> setupSectionAccessibilityHelper.canAccessMonitoringOfficerSection(organisation, true),
                () -> rules.partnerCanAccessMonitoringOfficerSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void bankDetailsSectionAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessBankDetailsSection, () -> rules.partnerCanAccessBankDetailsSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void spendProfileSectionAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessSpendProfileSection, () -> rules.partnerCanAccessSpendProfileSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void editSpendProfileSectionAccess() {
        ProjectOrganisationCompositeId projectOrganisationCompositeId = new ProjectOrganisationCompositeId(123L, 22L);
        assertNonLeadPartnerSuccessfulAccess((setupSectionAccessibilityHelper, organisation) -> setupSectionAccessibilityHelper.canEditSpendProfileSection(organisation, projectOrganisationCompositeId.getOrganisationId()),
                () -> rules.partnerCanEditSpendProfileSection(projectOrganisationCompositeId, user));
    }

    @Test
    public void otherDocumentsSectionAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessOtherDocumentsSection, () -> rules.partnerCanAccessOtherDocumentsSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void submitOtherDocumentsSectionSuccessfulAccessByProjectManager() {
        long projectId = 123;
        UserResource user = newUserResource().build();
        ProjectResource project = newProjectResource()
                .withId(projectId)
                .withOtherDocumentsApproved(ApprovalType.UNSET).build();
        when(projectServiceMock.getById(projectId)).thenReturn(project);
        when(projectServiceMock.isProjectManager(user.getId(), projectId)).thenReturn(true);
        when(otherDocumentsServiceMock.isOtherDocumentSubmitAllowed(projectId)).thenReturn(true);
        assertTrue(rules.projectManagerCanSubmitOtherDocumentsSection(ProjectCompositeId.id(projectId), user));
    }

    @Test
    public void submitOtherDocumentsSectionUnsuccessfulAccessByNonProjectManager() {
        long projectId = 123;
        UserResource user = newUserResource().build();
        ProjectResource project = newProjectResource()
                .withId(projectId)
                .withOtherDocumentsApproved(ApprovalType.UNSET).build();
        when(projectServiceMock.getById(projectId)).thenReturn(project);
        when(projectServiceMock.isProjectManager(user.getId(), projectId)).thenReturn(false);
        when(otherDocumentsServiceMock.isOtherDocumentSubmitAllowed(projectId)).thenReturn(true);
        assertFalse(rules.projectManagerCanSubmitOtherDocumentsSection(ProjectCompositeId.id(projectId), user));
    }

    @Test
    public void grantOfferLetterSectionAccess() {
        assertNonLeadPartnerSuccessfulAccess(SetupSectionAccessibilityHelper::canAccessGrantOfferLetterSection, () -> rules.partnerCanAccessGrantOfferLetterSection(ProjectCompositeId.id(123L), user));
    }

    @Test
    public void signedGrantOfferLetterSuccessfulAccessByLead() {
        UserResource userRes = new UserResource();
        userRes.setId(1L);
        when(projectServiceMock.isUserLeadPartner(123L, 1L)).thenReturn(true);
        assertTrue(rules.leadPartnerAccess(ProjectCompositeId.id(123L), userRes));
    }

    @Test
    public void signedGrantOfferLetterUnSuccessfulAccessByNonLead() {
        UserResource userRes = new UserResource();
        userRes.setId(1L);
        when(projectServiceMock.isUserLeadPartner(123L, 1L)).thenReturn(false);
        assertFalse(rules.leadPartnerAccess(ProjectCompositeId.id(123L), userRes));
    }

    @Test
    public void markSpendProfileIncompleteAccess() {
        ProjectUserResource leadPartnerProjectUserResource = newProjectUserResource().withUser(user.getId()).build();

        when(projectServiceMock.getLeadPartners(123L)).thenReturn(singletonList(leadPartnerProjectUserResource));
        assertTrue(rules.userCanMarkSpendProfileIncomplete(ProjectCompositeId.id(123L), user));
        verify(projectServiceMock).getLeadPartners(123L);
    }

    @Test
    public void userCannotMarkOwnOrganisationAsIncomplete() {
        Long userId = 1L;
        Long organisationId = 2L;
        UserResource userResource = newUserResource().withId(userId).build();
        OrganisationResource organisationResource = newOrganisationResource().withId(organisationId).build();

        when(organisationServiceMock.getOrganisationForUser(userId)).thenReturn(organisationResource);
        assertFalse(rules.userCannotMarkOwnSpendProfileIncomplete(OrganisationCompositeId.id(organisationId), userResource));
        verify(organisationServiceMock).getOrganisationForUser(userId);
    }

    @Test
    public void userCanMarkOtherOrganisationAsIncomplete() {
        Long userId = 1L;
        Long organisationId = 2L;
        Long otherOrganisationId = 3L;
        UserResource userResource = newUserResource().withId(userId).build();
        OrganisationResource organisationResource = newOrganisationResource().withId(organisationId).build();

        when(organisationServiceMock.getOrganisationForUser(userId)).thenReturn(organisationResource);
        assertTrue(rules.userCannotMarkOwnSpendProfileIncomplete(OrganisationCompositeId.id(otherOrganisationId), userResource));
        verify(organisationServiceMock).getOrganisationForUser(userId);
    }

    @Test
    public void partnerAccess() {
        long projectId = 123L;
        long organisationId = 234L;

        UserResource user = newUserResource().withRolesGlobal(singletonList(PARTNER)).build();

        BaseIntegrationTest.setLoggedInUser(user);

        OrganisationResource o = newOrganisationResource().withId(organisationId).build();

        ProjectPartnerStatusResource partnerStatus = newProjectPartnerStatusResource().withProjectDetailsStatus(ProjectActivityStates.COMPLETE).withOrganisationId(organisationId).withOrganisationType(OrganisationTypeEnum.valueOf(BUSINESS.toString())).build();
        List<ProjectUserResource> pu = newProjectUserResource().withProject(projectId).withOrganisation(o.getId()).withUser(user.getId()).build(1);
        pu.get(0).setRoleName(PARTNER.getName());

        ProjectTeamStatusResource teamStatus = newProjectTeamStatusResource().withPartnerStatuses(Collections.singletonList(partnerStatus)).build();

        when(projectServiceMock.getOrganisationIdFromUser(projectId, user)).thenReturn(organisationId);
        when(statusServiceMock.getProjectTeamStatus(projectId, Optional.of(user.getId()))).thenReturn(teamStatus);
        when(accessor.canAccessFinanceChecksSection(any())).thenReturn(ACCESSIBLE);

        assertTrue(rules.partnerCanAccessFinanceChecksSection(ProjectCompositeId.id(123L), user));

        verify(accessor).canAccessFinanceChecksSection(any());
    }

    @Test
    public void partnerNoAccess() {
        long projectId = 123L;
        long organisationId = 234L;

        UserResource user = newUserResource().withRolesGlobal(singletonList(PARTNER)).build();

        BaseIntegrationTest.setLoggedInUser(user);

        OrganisationResource o = newOrganisationResource().withId(organisationId).build();

        ProjectPartnerStatusResource partnerStatus = newProjectPartnerStatusResource().withProjectDetailsStatus(ProjectActivityStates.COMPLETE).withOrganisationId(organisationId).withOrganisationType(OrganisationTypeEnum.valueOf(BUSINESS.toString())).build();
        List<ProjectUserResource> pu = newProjectUserResource().withProject(projectId).withOrganisation(o.getId()).withUser(user.getId()).build(1);
        pu.get(0).setRoleName(PARTNER.getName());

        ProjectTeamStatusResource teamStatus = newProjectTeamStatusResource().withPartnerStatuses(Collections.singletonList(partnerStatus)).build();
        when(projectServiceMock.getProjectUsersForProject(projectId)).thenReturn(pu);
        when(statusServiceMock.getProjectTeamStatus(projectId, Optional.of(user.getId()))).thenReturn(teamStatus);
        when(accessor.canAccessFinanceChecksSection(any())).thenReturn(NOT_ACCESSIBLE);
        when(projectServiceMock.getOrganisationIdFromUser(projectId, user)).thenReturn(organisationId);

        assertFalse(rules.partnerCanAccessFinanceChecksSection(ProjectCompositeId.id(123L), user));

    }

    @Test
    public void financeContactAccess() {
        long projectId = 123L;
        long organisationId = 234L;

        UserResource user = newUserResource().withRolesGlobal(singletonList(FINANCE_CONTACT)).build();

        BaseIntegrationTest.setLoggedInUser(user);

        OrganisationResource o = newOrganisationResource().withId(organisationId).build();

        ProjectPartnerStatusResource partnerStatus = newProjectPartnerStatusResource().withProjectDetailsStatus(ProjectActivityStates.COMPLETE).withOrganisationId(organisationId).withOrganisationType(OrganisationTypeEnum.valueOf(BUSINESS.toString())).build();
        List<ProjectUserResource> pu = newProjectUserResource().withProject(projectId).withOrganisation(o.getId()).withUser(user.getId()).build(2);
        pu.get(0).setRoleName(PARTNER.getName());
        pu.get(1).setRoleName(FINANCE_CONTACT.getName());

        ProjectTeamStatusResource teamStatus = newProjectTeamStatusResource().withPartnerStatuses(Collections.singletonList(partnerStatus)).build();

        when(projectServiceMock.getOrganisationIdFromUser(projectId, user)).thenReturn(organisationId);
        when(statusServiceMock.getProjectTeamStatus(projectId, Optional.of(user.getId()))).thenReturn(teamStatus);
        when(accessor.canAccessFinanceChecksSection(any())).thenReturn(ACCESSIBLE);

        assertTrue(rules.partnerCanAccessFinanceChecksSection(ProjectCompositeId.id(123L), user));

        verify(statusServiceMock).getProjectTeamStatus(projectId, Optional.of(user.getId()));
    }

    private void assertLeadPartnerSuccessfulAccess(BiFunction<SetupSectionAccessibilityHelper, OrganisationResource, SectionAccess> accessorCheck,
                                                   Supplier<Boolean> ruleCheck) {

        ProjectTeamStatusResource teamStatus = newProjectTeamStatusResource().
                withProjectLeadStatus(newProjectPartnerStatusResource().
                        withOrganisationId(456L).
                        withOrganisationType(BUSINESS).
                        withIsLeadPartner(true).
                        build()).
                build();

        when(projectServiceMock.getOrganisationIdFromUser(123L, user)).thenReturn(456L);

        when(statusServiceMock.getProjectTeamStatus(123L, Optional.of(user.getId()))).thenReturn(teamStatus);

        OrganisationResource expectedOrganisation = new OrganisationResource();
        expectedOrganisation.setId(456L);
        expectedOrganisation.setOrganisationType(
                teamStatus.getPartnerStatusForOrganisation(456L).get().getOrganisationType().getId());

        when(accessorCheck.apply(accessor, expectedOrganisation)).thenReturn(ACCESSIBLE);

        assertTrue(ruleCheck.get());

        verify(statusServiceMock).getProjectTeamStatus(123L, Optional.of(user.getId()));
    }

    private void assertNonLeadPartnerSuccessfulAccess(BiFunction<SetupSectionAccessibilityHelper, OrganisationResource, SectionAccess> accessorCheck,
                                                      Supplier<Boolean> ruleCheck) {
        ProjectTeamStatusResource teamStatus = newProjectTeamStatusResource().
                withProjectLeadStatus(newProjectPartnerStatusResource().
                        withOrganisationId(456L).
                        withOrganisationType(BUSINESS).
                        withIsLeadPartner(true).
                        build()).
                withPartnerStatuses(newProjectPartnerStatusResource().
                        withOrganisationId(789L).
                        withOrganisationType(BUSINESS).
                        build(1)).
                build();

        List<ProjectUserResource> projectUsers = newProjectUserResource().
                withUser(user.getId()).
                withOrganisation(789L).
                withRole(PARTNER).
                build(1);

        when(projectServiceMock.getProjectUsersForProject(123L)).thenReturn(projectUsers);

        when(statusServiceMock.getProjectTeamStatus(123L, Optional.of(user.getId()))).thenReturn(teamStatus);

        when(projectServiceMock.getOrganisationIdFromUser(123L, user)).thenReturn(789L);

        OrganisationResource expectedOrganisation = new OrganisationResource();
        expectedOrganisation.setId(789L);
        expectedOrganisation.setOrganisationType(
                teamStatus.getPartnerStatusForOrganisation(789L).get().getOrganisationType().getId());

        when(accessorCheck.apply(accessor, expectedOrganisation)).thenReturn(ACCESSIBLE);

        assertTrue(ruleCheck.get());

        verify(statusServiceMock).getProjectTeamStatus(123L, Optional.of(user.getId()));
        accessorCheck.apply(verify(accessor), expectedOrganisation);
    }

    private void assertNotOnProjectExpectations(Supplier<Boolean> ruleCheck) {
        when(projectServiceMock.getOrganisationIdFromUser(123L, user)).thenThrow(new ForbiddenActionException(CANNOT_GET_ANY_USERS_FOR_PROJECT.getErrorKey(), singletonList(123L)));

        assertFalse(ruleCheck.get());

        verify(statusServiceMock, never()).getProjectTeamStatus(123L, Optional.of(user.getId()));
    }

    private void assertForbiddenExpectations(Supplier<Boolean> ruleCheck) {
        when(projectService.getOrganisationIdFromUser(123L, user)).thenThrow(new ForbiddenActionException(CANNOT_GET_ANY_USERS_FOR_PROJECT.getErrorKey(), singletonList(123L)));

        assertFalse(ruleCheck.get());
    }

    private void assertScenariousForSections(BiFunction<SetupSectionAccessibilityHelper, OrganisationResource, SectionAccess> accessorCheck, Supplier<Boolean> ruleCheck) {
        assertLeadPartnerSuccessfulAccess(accessorCheck, ruleCheck);
        resetMocks();

        assertNonLeadPartnerSuccessfulAccess(accessorCheck, ruleCheck);
        resetMocks();

        assertNotOnProjectExpectations(ruleCheck);
        resetMocks();

        assertForbiddenExpectations(ruleCheck);
    }

    private void resetMocks() {
        reset(projectServiceMock, statusServiceMock, accessor);
    }

    @Override
    protected SetupSectionsPermissionRules supplyPermissionRulesUnderTest() {
        return new SetupSectionsPermissionRules();
    }
}
