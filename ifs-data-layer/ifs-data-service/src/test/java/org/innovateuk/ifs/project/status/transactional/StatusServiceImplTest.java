package org.innovateuk.ifs.project.status.transactional;

import org.innovateuk.ifs.BaseServiceUnitTest;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.builder.CompetitionBuilder;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.repository.CompetitionRepository;
import org.innovateuk.ifs.file.domain.FileEntry;
import org.innovateuk.ifs.finance.domain.ApplicationFinance;
import org.innovateuk.ifs.finance.mapper.ApplicationFinanceMapper;
import org.innovateuk.ifs.finance.repository.ApplicationFinanceRepository;
import org.innovateuk.ifs.finance.resource.ApplicationFinanceResource;
import org.innovateuk.ifs.finance.transactional.FinanceService;
import org.innovateuk.ifs.organisation.domain.Organisation;
import org.innovateuk.ifs.organisation.domain.OrganisationType;
import org.innovateuk.ifs.organisation.repository.OrganisationRepository;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.project.bankdetails.domain.BankDetails;
import org.innovateuk.ifs.project.bankdetails.repository.BankDetailsRepository;
import org.innovateuk.ifs.project.constant.ProjectActivityStates;
import org.innovateuk.ifs.project.core.builder.PartnerOrganisationBuilder;
import org.innovateuk.ifs.project.core.domain.PartnerOrganisation;
import org.innovateuk.ifs.project.core.domain.Project;
import org.innovateuk.ifs.project.core.domain.ProjectUser;
import org.innovateuk.ifs.project.core.mapper.ProjectUserMapper;
import org.innovateuk.ifs.project.core.repository.PartnerOrganisationRepository;
import org.innovateuk.ifs.project.core.repository.ProjectRepository;
import org.innovateuk.ifs.project.core.repository.ProjectUserRepository;
import org.innovateuk.ifs.project.core.util.ProjectUsersHelper;
import org.innovateuk.ifs.project.finance.resource.EligibilityState;
import org.innovateuk.ifs.project.finance.resource.ViabilityState;
import org.innovateuk.ifs.project.financechecks.service.FinanceCheckService;
import org.innovateuk.ifs.project.financechecks.workflow.financechecks.configuration.EligibilityWorkflowHandler;
import org.innovateuk.ifs.project.financechecks.workflow.financechecks.configuration.ViabilityWorkflowHandler;
import org.innovateuk.ifs.project.grantofferletter.configuration.workflow.GrantOfferLetterWorkflowHandler;
import org.innovateuk.ifs.project.grantofferletter.resource.GrantOfferLetterState;
import org.innovateuk.ifs.project.grantofferletter.resource.GrantOfferLetterStateResource;
import org.innovateuk.ifs.project.monitoringofficer.builder.MonitoringOfficerBuilder;
import org.innovateuk.ifs.project.monitoringofficer.domain.MonitoringOfficer;
import org.innovateuk.ifs.project.monitoringofficer.repository.MonitoringOfficerRepository;
import org.innovateuk.ifs.project.projectdetails.workflow.configuration.ProjectDetailsWorkflowHandler;
import org.innovateuk.ifs.project.resource.ApprovalType;
import org.innovateuk.ifs.project.resource.ProjectPartnerStatusResource;
import org.innovateuk.ifs.project.resource.ProjectState;
import org.innovateuk.ifs.project.resource.ProjectUserResource;
import org.innovateuk.ifs.project.spendprofile.configuration.workflow.SpendProfileWorkflowHandler;
import org.innovateuk.ifs.project.spendprofile.domain.SpendProfile;
import org.innovateuk.ifs.project.spendprofile.repository.SpendProfileRepository;
import org.innovateuk.ifs.project.spendprofile.transactional.SpendProfileService;
import org.innovateuk.ifs.project.status.resource.CompetitionProjectsStatusResource;
import org.innovateuk.ifs.project.status.resource.ProjectStatusResource;
import org.innovateuk.ifs.project.status.resource.ProjectTeamStatusResource;
import org.innovateuk.ifs.security.LoggedInUserSupplier;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.repository.UserRepository;
import org.innovateuk.ifs.user.resource.Role;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.file.builder.FileEntryBuilder.newFileEntry;
import static org.innovateuk.ifs.finance.builder.ApplicationFinanceBuilder.newApplicationFinance;
import static org.innovateuk.ifs.finance.builder.ApplicationFinanceResourceBuilder.newApplicationFinanceResource;
import static org.innovateuk.ifs.invite.builder.ProjectInviteBuilder.newProjectInvite;
import static org.innovateuk.ifs.invite.domain.ProjectParticipantRole.PROJECT_FINANCE_CONTACT;
import static org.innovateuk.ifs.invite.domain.ProjectParticipantRole.PROJECT_PARTNER;
import static org.innovateuk.ifs.organisation.builder.OrganisationBuilder.newOrganisation;
import static org.innovateuk.ifs.organisation.builder.OrganisationTypeBuilder.newOrganisationType;
import static org.innovateuk.ifs.project.bankdetails.builder.BankDetailsBuilder.newBankDetails;
import static org.innovateuk.ifs.project.builder.ProjectPartnerStatusResourceBuilder.newProjectPartnerStatusResource;
import static org.innovateuk.ifs.project.builder.ProjectTeamStatusResourceBuilder.newProjectTeamStatusResource;
import static org.innovateuk.ifs.project.builder.ProjectUserResourceBuilder.newProjectUserResource;
import static org.innovateuk.ifs.project.constant.ProjectActivityStates.*;
import static org.innovateuk.ifs.project.core.builder.PartnerOrganisationBuilder.newPartnerOrganisation;
import static org.innovateuk.ifs.project.core.builder.ProjectBuilder.newProject;
import static org.innovateuk.ifs.project.core.builder.ProjectUserBuilder.newProjectUser;
import static org.innovateuk.ifs.project.monitoringofficer.builder.MonitoringOfficerBuilder.newMonitoringOfficer;
import static org.innovateuk.ifs.project.spendprofile.builder.SpendProfileBuilder.newSpendProfile;
import static org.innovateuk.ifs.user.builder.ProcessRoleBuilder.newProcessRole;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.user.resource.Role.COMP_ADMIN;
import static org.innovateuk.ifs.util.MapFunctions.asMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.powermock.api.mockito.PowerMockito.when;

public class StatusServiceImplTest extends BaseServiceUnitTest<StatusService> {

    private Application application;
    private Role partnerRole;
    private User u;
    private List<PartnerOrganisation> po;
    private List<ProjectUserResource> puResource;
    private List<ProjectUser> pu;
    private Organisation o;
    private Project project;
    private Project p;
    private BankDetails bankDetails;
    private SpendProfile spendProfile;

    @Mock
    private ApplicationRepository applicationRepositoryMock;

    @Mock
    private ProjectRepository projectRepositoryMock;

    @Mock
    private OrganisationRepository organisationRepositoryMock;

    @Mock
    private LoggedInUserSupplier loggedInUserSupplierMock;

    @Mock
    private UserRepository userRepositoryMock;

    @Mock
    private CompetitionRepository competitionRepositoryMock;

    @Mock
    private ProjectUserRepository projectUserRepositoryMock;

    @Mock
    private SpendProfileRepository spendProfileRepositoryMock;

    @Mock
    private BankDetailsRepository bankDetailsRepositoryMock;

    @Mock
    private MonitoringOfficerRepository monitoringOfficerRepositoryMock;

    @Mock
    private ApplicationFinanceRepository applicationFinanceRepositoryMock;

    @Mock
    private ApplicationFinanceMapper applicationFinanceMapperMock;

    @Mock
    private ProjectUserMapper projectUserMapperMock;

    @Mock
    private FinanceService financeServiceMock;

    @Mock
    private ProjectUsersHelper projectUsersHelperMock;

    @Mock
    private SpendProfileService spendProfileServiceMock;

    @Mock
    private ProjectDetailsWorkflowHandler projectDetailsWorkflowHandlerMock;

    @Mock
    private GrantOfferLetterWorkflowHandler golWorkflowHandlerMock;

    @Mock
    private PartnerOrganisationRepository partnerOrganisationRepositoryMock;

    @Mock
    private FinanceCheckService financeCheckServiceMock;

    @Mock
    private EligibilityWorkflowHandler eligibilityWorkflowHandlerMock;

    @Mock
    private ViabilityWorkflowHandler viabilityWorkflowHandlerMock;

    @Mock
    private SpendProfileWorkflowHandler spendProfileWorkflowHandlerMock;

    @Before
    public void setUp() {

        Organisation organisation = newOrganisation().
                withOrganisationType(OrganisationTypeEnum.BUSINESS).
                build();

        Role leadApplicantRole = Role.LEADAPPLICANT;

        Long userId = 7L;
        User user = newUser().
                withId(userId).
                build();

        ProcessRole leadApplicantProcessRole = newProcessRole().
                withOrganisationId(organisation.getId()).
                withRole(leadApplicantRole).
                withUser(user).
                build();

        ProjectUser leadPartnerProjectUser = newProjectUser().
                withOrganisation(organisation).
                withRole(PROJECT_PARTNER).
                withUser(user).
                build();

        Long applicationId = 456L;
        application = newApplication().
                withId(applicationId).
                withProcessRoles(leadApplicantProcessRole).
                withName("My Application").
                withDurationInMonths(5L).
                withStartDate(LocalDate.of(2017, 3, 2)).
                build();

        Long projectId = 123L;
        project = newProject().
                withId(projectId).
                withApplication(application).
                withProjectUsers(singletonList(leadPartnerProjectUser)).
                build();

        OrganisationType businessOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.BUSINESS).build();
        o = organisation;
        o.setOrganisationType(businessOrganisationType);

        partnerRole = Role.FINANCE_CONTACT;

        po = newPartnerOrganisation().
                withOrganisation(o).
                withLeadOrganisation(TRUE).
                build(1);

        u = newUser().
                withEmailAddress("a@b.com").
                withFirstName("A").
                withLastName("B").
                build();

        pu = newProjectUser().
                withRole(PROJECT_FINANCE_CONTACT).
                withUser(u).
                withOrganisation(o).
                withInvite(newProjectInvite().
                        build()).
                build(1);

        p = newProject().
                withProjectUsers(pu).
                withApplication(application).
                withPartnerOrganisations(po).
                withDateSubmitted(ZonedDateTime.now()).
                withOtherDocumentsApproved(ApprovalType.APPROVED).
                withSpendProfileSubmittedDate(ZonedDateTime.now()).
                build();

        puResource = newProjectUserResource().
                withProject(p.getId()).
                withOrganisation(o.getId()).
                withRole(partnerRole.getId()).
                withRoleName(PROJECT_PARTNER.getName()).
                build(1);

        bankDetails = newBankDetails().withOrganisation(o).withApproval(TRUE).build();
        spendProfile = newSpendProfile().withOrganisation(o).withGeneratedDate(Calendar.getInstance()).withMarkedComplete(TRUE).build();

        Mockito.when(applicationRepositoryMock.findOne(applicationId)).thenReturn(application);
        Mockito.when(projectRepositoryMock.findOne(projectId)).thenReturn(project);
        Mockito.when(organisationRepositoryMock.findOne(organisation.getId())).thenReturn(organisation);
        Mockito.when(loggedInUserSupplierMock.get()).thenReturn(newUser().build());

        User internalUser = newUser().withRoles(singleton(COMP_ADMIN)).build();
        when(userRepositoryMock.findOne(internalUser.getId())).thenReturn(internalUser);
        setLoggedInUser(newUserResource().withId(internalUser.getId()).withRolesGlobal(singletonList(COMP_ADMIN)).build());
    }

    @Override
    protected StatusService supplyServiceUnderTest() {
        return new StatusServiceImpl();
    }

    @Test
    public void getCompetitionStatus() {
        Long competitionId = 123L;
        String applicationSearchString = "1";

        Competition competition = newCompetition().withId(competitionId).build();

        /**
         * Create 3 organisations:
         * 2 Business, 1 Academic
         ***/
        OrganisationType businessOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.BUSINESS).build();
        OrganisationType academicOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.RESEARCH).build();
        List<Organisation> organisations = newOrganisation().withOrganisationType(businessOrganisationType).build(2);
        organisations.add(newOrganisation().withOrganisationType(academicOrganisationType).build());


        /**
         * Create 3 users, one for each organisation
         */
        List<User> users = newUser().build(3);

        /**
         * Create 3 applications, one for each org, with process roles
         */
        List<ProcessRole> applicantProcessRoles = newProcessRole().withUser(users.get(0), users.get(1), users.get(2)).withRole(Role.LEADAPPLICANT, Role.APPLICANT, Role.APPLICANT).withOrganisationId(organisations.get(0).getId(), organisations.get(1).getId(), organisations.get(2).getId()).build(3);
        List<Application> applications = newApplication().withCompetition(competition).withProcessRoles(applicantProcessRoles.get(0), applicantProcessRoles.get(1), applicantProcessRoles.get(2)).build(3);

        /**
         * Create 3 project with 3 Project Users from 3 different organisations with associated applications
         */
        List<ProjectUser> projectUsers = newProjectUser().withRole(PROJECT_PARTNER).withUser(users.get(0), users.get(1), users.get(2)).withOrganisation(organisations.get(0), organisations.get(1), organisations.get(2)).build(3);
        List<Project> projects = newProject().withApplication(applications.get(0), applications.get(1), applications.get(2)).withProjectUsers(projectUsers).build(3);

        /**
         * Create 3 bank detail records, one for each organisation
         */
        List<BankDetails> bankDetails = newBankDetails().withOrganisation(organisations.get(0), organisations.get(1), organisations.get(2)).build(3);

        /**
         * Build spend profile object for use with one of the partners
         */
        SpendProfile spendProfile = newSpendProfile().build();

        when(competitionRepositoryMock.findOne(competitionId)).thenReturn(competition);

        when(projectRepositoryMock.findOne(projects.get(0).getId())).thenReturn(projects.get(0));
        when(projectRepositoryMock.findOne(projects.get(1).getId())).thenReturn(projects.get(1));
        when(projectRepositoryMock.findOne(projects.get(2).getId())).thenReturn(projects.get(2));

        when(projectRepositoryMock.searchByCompetitionIdAndApplicationIdLikeAndProjectStateNotIn(competitionId, applicationSearchString, singleton(ProjectState.WITHDRAWN))).thenReturn(projects);

        when(projectUserRepositoryMock.findByProjectId(projects.get(0).getId())).thenReturn(projectUsers);
        when(projectUserRepositoryMock.findByProjectId(projects.get(1).getId())).thenReturn(projectUsers);
        when(projectUserRepositoryMock.findByProjectId(projects.get(2).getId())).thenReturn(projectUsers);

        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projects.get(0).getId(), organisations.get(0).getId())).thenReturn(bankDetails.get(0));
        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projects.get(1).getId(), organisations.get(1).getId())).thenReturn(bankDetails.get(1));
        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projects.get(2).getId(), organisations.get(2).getId())).thenReturn(bankDetails.get(2));

        when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(projects.get(0).getId(), organisations.get(0).getId())).thenReturn(Optional.of(spendProfile));
        when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(projects.get(1).getId(), organisations.get(1).getId())).thenReturn(Optional.of(spendProfile));
        when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(projects.get(2).getId(), organisations.get(2).getId())).thenReturn(Optional.of(spendProfile));

        MonitoringOfficer monitoringOfficerInDB = newMonitoringOfficer().build();
        when(monitoringOfficerRepositoryMock.findOneByProjectId(projects.get(0).getId())).thenReturn(monitoringOfficerInDB);
        when(monitoringOfficerRepositoryMock.findOneByProjectId(projects.get(1).getId())).thenReturn(monitoringOfficerInDB);
        when(monitoringOfficerRepositoryMock.findOneByProjectId(projects.get(2).getId())).thenReturn(monitoringOfficerInDB);

        when(organisationRepositoryMock.findOne(organisations.get(0).getId())).thenReturn(organisations.get(0));
        when(organisationRepositoryMock.findOne(organisations.get(1).getId())).thenReturn(organisations.get(1));
        when(organisationRepositoryMock.findOne(organisations.get(2).getId())).thenReturn(organisations.get(2));

        List<ApplicationFinance> applicationFinances = newApplicationFinance().build(3);
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(0).getApplication().getId(), organisations.get(0).getId())).thenReturn(applicationFinances.get(0));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(0).getApplication().getId(), organisations.get(1).getId())).thenReturn(applicationFinances.get(1));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(0).getApplication().getId(), organisations.get(2).getId())).thenReturn(applicationFinances.get(2));

        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(1).getApplication().getId(), organisations.get(0).getId())).thenReturn(applicationFinances.get(0));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(1).getApplication().getId(), organisations.get(1).getId())).thenReturn(applicationFinances.get(1));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(1).getApplication().getId(), organisations.get(2).getId())).thenReturn(applicationFinances.get(2));

        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(2).getApplication().getId(), organisations.get(0).getId())).thenReturn(applicationFinances.get(0));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(2).getApplication().getId(), organisations.get(1).getId())).thenReturn(applicationFinances.get(1));
        when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(projects.get(2).getApplication().getId(), organisations.get(2).getId())).thenReturn(applicationFinances.get(2));

        ApplicationFinanceResource applicationFinanceResource0 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(0).getId()).build();
        when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(0))).thenReturn(applicationFinanceResource0);

        ApplicationFinanceResource applicationFinanceResource1 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(1).getId()).build();
        when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(1))).thenReturn(applicationFinanceResource1);

        ApplicationFinanceResource applicationFinanceResource2 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(2).getId()).build();
        when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(2))).thenReturn(applicationFinanceResource2);

        List<ProjectUserResource> puResource = newProjectUserResource().withProject(projects.get(0).getId()).withOrganisation(organisations.get(0).getId(), organisations.get(1).getId(), organisations.get(2).getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(3);

        when(projectUserMapperMock.mapToResource(projectUsers.get(0))).thenReturn(puResource.get(0));
        when(projectUserMapperMock.mapToResource(projectUsers.get(1))).thenReturn(puResource.get(1));
        when(projectUserMapperMock.mapToResource(projectUsers.get(2))).thenReturn(puResource.get(2));

        when(financeServiceMock.organisationSeeksFunding(projects.get(0).getId(), projects.get(0).getApplication().getId(), organisations.get(0).getId())).thenReturn(serviceSuccess(true));
        when(financeServiceMock.organisationSeeksFunding(projects.get(0).getId(), projects.get(0).getApplication().getId(), organisations.get(1).getId())).thenReturn(serviceSuccess(false));
        when(financeServiceMock.organisationSeeksFunding(projects.get(0).getId(), projects.get(0).getApplication().getId(), organisations.get(2).getId())).thenReturn(serviceSuccess(false));

        when(financeServiceMock.organisationSeeksFunding(projects.get(1).getId(), projects.get(1).getApplication().getId(), organisations.get(0).getId())).thenReturn(serviceSuccess(true));
        when(financeServiceMock.organisationSeeksFunding(projects.get(1).getId(), projects.get(1).getApplication().getId(), organisations.get(1).getId())).thenReturn(serviceSuccess(false));
        when(financeServiceMock.organisationSeeksFunding(projects.get(1).getId(), projects.get(1).getApplication().getId(), organisations.get(2).getId())).thenReturn(serviceSuccess(false));

        when(financeServiceMock.organisationSeeksFunding(projects.get(2).getId(), projects.get(2).getApplication().getId(), organisations.get(0).getId())).thenReturn(serviceSuccess(true));
        when(financeServiceMock.organisationSeeksFunding(projects.get(2).getId(), projects.get(2).getApplication().getId(), organisations.get(1).getId())).thenReturn(serviceSuccess(false));
        when(financeServiceMock.organisationSeeksFunding(projects.get(2).getId(), projects.get(2).getApplication().getId(), organisations.get(2).getId())).thenReturn(serviceSuccess(false));

        when(projectUsersHelperMock.getPartnerOrganisations(projects.get(0).getId())).thenReturn(organisations);
        when(projectUsersHelperMock.getPartnerOrganisations(projects.get(1).getId())).thenReturn(organisations);
        when(projectUsersHelperMock.getPartnerOrganisations(projects.get(2).getId())).thenReturn(organisations);

        when(spendProfileServiceMock.getSpendProfileStatus(projects.get(0).getId())).thenReturn(serviceSuccess(ApprovalType.EMPTY));
        when(spendProfileServiceMock.getSpendProfileStatus(projects.get(1).getId())).thenReturn(serviceSuccess(ApprovalType.EMPTY));
        when(spendProfileServiceMock.getSpendProfileStatus(projects.get(2).getId())).thenReturn(serviceSuccess(ApprovalType.EMPTY));

        ServiceResult<CompetitionProjectsStatusResource> result = service.getCompetitionStatus(competitionId, applicationSearchString);

        assertTrue(result.isSuccess());

        CompetitionProjectsStatusResource competitionProjectsStatusResource = result.getSuccess();
        assertTrue(projectsGetSortedByApplicationId(competitionProjectsStatusResource.getProjectStatusResources()));
        assertEquals(3, competitionProjectsStatusResource.getProjectStatusResources().size());
        assertEquals(new Integer(3), competitionProjectsStatusResource.getProjectStatusResources().get(0).getNumberOfPartners());
        assertEquals(new Integer(3), competitionProjectsStatusResource.getProjectStatusResources().get(1).getNumberOfPartners());
        assertEquals(new Integer(3), competitionProjectsStatusResource.getProjectStatusResources().get(2).getNumberOfPartners());
    }

    private boolean projectsGetSortedByApplicationId(List<ProjectStatusResource> after) {
        return after.stream()
                .sorted(Comparator.comparing(ProjectStatusResource::getApplicationNumber))
                .collect(Collectors.toList())
                .equals(after);
    }

    @Test
    public void getProjectStatusResourceByProject() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, true);

        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusFinanceContactComplete() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(COMPLETE, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusFinanceContactCompleteButPartnerLocationsRequiredAndNotComplete() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, true);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }


    @Test
    public void getProjectStatusFinanceContactCompleteAndPartnerLocationsRequiredAndComplete() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, true);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation()
                .withOrganisation(o)
                .withPostcode("TW14 9QG")
                .build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(COMPLETE, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusFinanceContactIncomplete() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);

        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(Optional.empty());
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(monitoringOfficerRepositoryMock.findOneByProjectId(projectId)).thenReturn(null);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectSpendProfileRejected() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.REJECTED, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);

        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));
        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(REJECTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectGolPrecursorsCompleteAndGolRejected() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.APPROVED, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, ZonedDateTime.now(), false);

        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(REJECTED, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, REJECTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectGolPrecursorsCompleteAndGolApproved() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.APPROVED, Boolean.FALSE,
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, ZonedDateTime.now(), false);

        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, COMPLETE);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectGolPrecursorsCompleteAndGolSent() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.APPROVED, Boolean.FALSE,
                Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, ZonedDateTime.now(), false);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, PENDING);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectGolPrecursorsCompleteAndSignedGolSubmitted() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.APPROVED, Boolean.TRUE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, ZonedDateTime.now(), false);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, ACTION_REQUIRED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectGolPrecursorsCompleteAndGolReadyToSend() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.APPROVED, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, ZonedDateTime.now(), false);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, ACTION_REQUIRED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusResourceByProjectOtherDocumentsRejected() {
        Long projectId = 2345L;

        Project project = createProjectStatusResource(projectId, ApprovalType.APPROVED, ApprovalType.REJECTED, Boolean.FALSE,
                Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, ZonedDateTime.now(), false);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(PENDING, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(REJECTED, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getGrantOfferLetterStatus());

        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));


        when(projectRepositoryMock.findOne(projectId)).thenReturn(null);
        ServiceResult<ProjectStatusResource> resultFailure = service.getProjectStatusByProjectId(projectId);
        assertTrue(resultFailure.isFailure());
    }

    @Test
    public void getProjectStatusBankDetailsCompleteNotApproved() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projectId, organisationId)).thenReturn(new BankDetails());
        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(COMPLETE, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));
    }

    @Test
    public void getProjectStatusBankDetailsIncomplete() {
        Long projectId = 2345L;
        Long organisationId = 123L;
        Long organisationId2 = 234L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        Organisation o2 = newOrganisation().withId(organisationId2).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build(), newPartnerOrganisation().withOrganisation(o2).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projectId, organisationId)).thenReturn(newBankDetails().withApproval(true).build());
        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projectId, organisationId2)).thenReturn(null);
        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId2)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(COMPLETE, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));
    }

    @Test
    public void getProjectStatusBankDetailsApproved() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());

        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projectId, organisationId)).thenReturn(newBankDetails().withApproval(true).build());
        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);

        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(project.getName(), returnedProjectStatusResource.getProjectTitle());
        assertEquals(project.getId(), returnedProjectStatusResource.getProjectNumber());
        assertEquals(Integer.valueOf(1), returnedProjectStatusResource.getNumberOfPartners());

        assertEquals(COMPLETE, returnedProjectStatusResource.getProjectDetailsStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getBankDetailsStatus());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getFinanceChecksStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getSpendProfileStatus());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());
        assertEquals(PENDING, returnedProjectStatusResource.getOtherDocumentsStatus());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getGrantOfferLetterStatus());
        Map<Role, ProjectActivityStates> roles = asMap(COMP_ADMIN, NOT_STARTED);
        assertTrue(roles.equals(returnedProjectStatusResource.getRoleSpecificGrantOfferLetterState()));
    }


    private Project createProjectStatusResource(Long projectId, ApprovalType spendProfileStatus,
                                                ApprovalType otherDocsApproved, Boolean golReadyToApprove, Boolean golIsSent, Boolean golIsApproved,
                                                Boolean golRejected,
                                                ZonedDateTime otherDocsSubmittedDate,
                                                boolean locationPerPartnerRequired) {

        Long competitionId = 112L;
        Competition competition = CompetitionBuilder.newCompetition()
                .withId(competitionId)
                .withLocationPerPartner(locationPerPartnerRequired)
                .build();

        Application application = newApplication()
                .withCompetition(competition)
                .build();
        Organisation organisation = newOrganisation().build();
        Role role = Role.LEADAPPLICANT;
        ProcessRole processRole = newProcessRole().
                withRole(role).
                withApplication(application).
                withOrganisationId(organisation.getId()).
                build();
        PartnerOrganisation partnerOrganisation = newPartnerOrganisation().withOrganisation(organisation).build();
        Project project = newProject().
                withId(projectId).
                withApplication(application).
                withPartnerOrganisations(asList(partnerOrganisation)).
                withOtherDocumentsApproved(otherDocsApproved).
                withOtherDocumentsSubmittedDate(otherDocsSubmittedDate).
                build();

        BankDetails bankDetail = newBankDetails().withProject(project).build();
        SpendProfile spendprofile = newSpendProfile().withOrganisation(organisation).build();
        MonitoringOfficer monitoringOfficer = newMonitoringOfficer().build();

        when(projectRepositoryMock.findOne(projectId)).thenReturn(project);
        when(projectUsersHelperMock.getPartnerOrganisations(project.getId())).thenReturn(asList(organisation));
        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(project.getId(), organisation.getId())).thenReturn(bankDetail);
        when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(project.getId(), organisation.getId())).thenReturn(Optional.of(spendprofile));
        when(monitoringOfficerRepositoryMock.findOneByProjectId(project.getId())).thenReturn(monitoringOfficer);
        when(organisationRepositoryMock.findOne(processRole.getOrganisationId())).thenReturn(organisation);

        when(spendProfileServiceMock.getSpendProfileStatus(projectId)).thenReturn(serviceSuccess(spendProfileStatus));
        when(golWorkflowHandlerMock.isApproved(project)).thenReturn(golIsApproved);
        when(golWorkflowHandlerMock.isRejected(project)).thenReturn(golRejected);
        if (!golIsApproved) {
            when(golWorkflowHandlerMock.isReadyToApprove(project)).thenReturn(golReadyToApprove);
            if (!golReadyToApprove)
                when(golWorkflowHandlerMock.isSent(project)).thenReturn(golIsSent);
        }
        return project;
    }

    /**
     * Tests MO requirement for IFS-1307
     */
    @Test
    public void getProjectStatusShowMOStatusForSupportAndInnoLeadAndStakeholder() {
        Long projectId = 2345L;
        Long organisationId = 123L;

        Project project = createProjectStatusResource(projectId, ApprovalType.EMPTY, ApprovalType.UNSET, Boolean.FALSE,
                Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, null, false);
        Organisation o = newOrganisation().withId(organisationId).build();
        List<PartnerOrganisation> po = asList(newPartnerOrganisation().withOrganisation(o).build());
        project.setPartnerOrganisations(po);
        Optional<ProjectUser> pu = Optional.of(newProjectUser().withRole(PROJECT_FINANCE_CONTACT).build());
        MonitoringOfficer monitoringOfficer = newMonitoringOfficer().build();

        when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(projectId, organisationId)).thenReturn(newBankDetails().withApproval(true).build());
        when(projectUsersHelperMock.getFinanceContact(projectId, organisationId)).thenReturn(pu);
        when(projectDetailsWorkflowHandlerMock.isSubmitted(project)).thenReturn(true);
        when(financeServiceMock.organisationSeeksFunding(any(Long.class), any(Long.class), any(Long.class))).thenReturn(serviceSuccess(Boolean.TRUE));

        // Status shown to support user when MO is set is COMPLETE
        when(monitoringOfficerRepositoryMock.findOneByProjectId(project.getId())).thenReturn(monitoringOfficer);
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.SUPPORT)).build());
        ServiceResult<ProjectStatusResource> result = service.getProjectStatusByProjectId(projectId);
        ProjectStatusResource returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to support user when MO is not set is NOT_STARTED and not ACTION_REQUIRED
        when(monitoringOfficerRepositoryMock.findOneByProjectId(project.getId())).thenReturn(null);
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.SUPPORT)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to innovation lead user when MO is not set is NOT_STARTED and not ACTION_REQUIRED
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.INNOVATION_LEAD)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to stakeholder when MO is set is COMPLETE
        when(monitoringOfficerRepositoryMock.findOneByProjectId(project.getId())).thenReturn(monitoringOfficer);
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.STAKEHOLDER)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(COMPLETE, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to stakeholder when MO is not set is NOT_STARTED and not ACTION_REQUIRED
        when(monitoringOfficerRepositoryMock.findOneByProjectId(project.getId())).thenReturn(null);
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.STAKEHOLDER)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(NOT_STARTED, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to comp admin user when MO is not set is ACTION_REQUIRED
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(COMP_ADMIN)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getMonitoringOfficerStatus());

        // Status shown to project finance user when MO is not set is ACTION_REQUIRED
        when(loggedInUserSupplierMock.get()).thenReturn(newUser().withRoles(singleton(Role.PROJECT_FINANCE)).build());
        result = service.getProjectStatusByProjectId(projectId);
        returnedProjectStatusResource = result.getSuccess();
        assertTrue(result.isSuccess());
        assertEquals(ACTION_REQUIRED, returnedProjectStatusResource.getMonitoringOfficerStatus());
    }

    @Test
    public void getProjectTeamStatus(){
        /**
         * Create 3 organisations:
         * 2 Business, 1 Academic
         * **/
        OrganisationType businessOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.BUSINESS).build();
        OrganisationType academicOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.RESEARCH).build();
        List<Organisation> organisations = new ArrayList<>();
        Organisation leadOrganisation = organisationRepositoryMock.findOne(application.getLeadOrganisationId());
        leadOrganisation.setOrganisationType(businessOrganisationType);
        organisations.add(leadOrganisation);
        leadOrganisation.setOrganisationType(businessOrganisationType);

        Organisation partnerOrganisation1 = newOrganisation().withOrganisationType(businessOrganisationType).build();
        Organisation partnerOrganisation2 = newOrganisation().withOrganisationType(academicOrganisationType).build();

        organisations.add(partnerOrganisation1);
        organisations.add(partnerOrganisation2);

        /**
         * Create 3 users project partner roles for each of the 3 organisations above
         */
        List<User> users = newUser().build(3);
        List<ProjectUser> pu = newProjectUser().withRole(PROJECT_PARTNER).withUser(users.get(0), users.get(1), users.get(2)).withOrganisation(organisations.get(0), organisations.get(1), organisations.get(2)).build(3);

        /**
         * Create a project with 3 Project Users from 3 different organisations with an associated application
         */
        Project p = newProject().withProjectUsers(pu).withApplication(application).build();

        /**
         * Create 3 bank detail records, one for each organisation
         */
        List<BankDetails> bankDetails = newBankDetails().withOrganisation(organisations.get(0), organisations.get(1), organisations.get(2)).build(3);

        /**
         * Build spend profile object for use with one of the partners
         */
        SpendProfile spendProfile = newSpendProfile().build();

        /**
         * Create Finance Check information for each Organisation
         */
        List<PartnerOrganisation> partnerOrganisations = PartnerOrganisationBuilder.newPartnerOrganisation()
                .withProject(p)
                .withOrganisation(leadOrganisation, partnerOrganisation1, partnerOrganisation2)
                .withPostcode(null, "TW14 9QG", " ")
                .build(3);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);

        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);

        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), organisations.get(0).getId())).thenReturn(bankDetails.get(0));

        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), organisations.get(0).getId())).thenReturn(Optional.empty());
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), organisations.get(1).getId())).thenReturn(Optional.empty());
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), organisations.get(2).getId())).thenReturn(Optional.empty());

        MonitoringOfficer monitoringOfficerInDB = MonitoringOfficerBuilder.newMonitoringOfficer().build();
        Mockito.when(monitoringOfficerRepositoryMock.findOneByProjectId(p.getId())).thenReturn(monitoringOfficerInDB);

        Mockito.when(organisationRepositoryMock.findOne(organisations.get(0).getId())).thenReturn(organisations.get(0));
        Mockito.when(organisationRepositoryMock.findOne(organisations.get(1).getId())).thenReturn(organisations.get(1));
        Mockito.when(organisationRepositoryMock.findOne(organisations.get(2).getId())).thenReturn(organisations.get(2));

        List<ApplicationFinance> applicationFinances = newApplicationFinance().build(3);
        Mockito.when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(p.getApplication().getId(), organisations.get(0).getId())).thenReturn(applicationFinances.get(0));
        Mockito.when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(p.getApplication().getId(), organisations.get(1).getId())).thenReturn(applicationFinances.get(1));
        Mockito.when(applicationFinanceRepositoryMock.findByApplicationIdAndOrganisationId(p.getApplication().getId(), organisations.get(2).getId())).thenReturn(applicationFinances.get(2));

        ApplicationFinanceResource applicationFinanceResource0 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(0).getId()).build();
        Mockito.when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(0))).thenReturn(applicationFinanceResource0);

        ApplicationFinanceResource applicationFinanceResource1 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(1).getId()).build();
        Mockito.when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(1))).thenReturn(applicationFinanceResource1);

        ApplicationFinanceResource applicationFinanceResource2 = newApplicationFinanceResource().withGrantClaimPercentage(20).withOrganisation(organisations.get(2).getId()).build();
        Mockito.when(applicationFinanceMapperMock.mapToResource(applicationFinances.get(2))).thenReturn(applicationFinanceResource2);

        List<ProjectUserResource> puResource = newProjectUserResource().withProject(p.getId()).withOrganisation(organisations.get(0).getId(), organisations.get(1).getId(), organisations.get(2).getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(3);

        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(1))).thenReturn(puResource.get(1));
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(2))).thenReturn(puResource.get(2));

        Mockito.when(financeServiceMock.organisationSeeksFunding(p.getId(), p.getApplication().getId(), organisations.get(0).getId())).thenReturn(serviceSuccess(TRUE));
        Mockito.when(financeServiceMock.organisationSeeksFunding(p.getId(), p.getApplication().getId(), organisations.get(1).getId())).thenReturn(serviceSuccess(FALSE));
        Mockito.when(financeServiceMock.organisationSeeksFunding(p.getId(), p.getApplication().getId(), organisations.get(2).getId())).thenReturn(serviceSuccess(TRUE));

        partnerOrganisations.forEach(org ->
                Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(org.getProject().getId(),
                        org.getOrganisation().getId())).thenReturn(org));

        Mockito.when(financeCheckServiceMock.isQueryActionRequired(partnerOrganisations.get(0).getProject().getId(), partnerOrganisations.get(0).getOrganisation().getId())).thenReturn(serviceSuccess(FALSE));
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(partnerOrganisations.get(1).getProject().getId(), partnerOrganisations.get(1).getOrganisation().getId())).thenReturn(serviceSuccess(FALSE));
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(partnerOrganisations.get(2).getProject().getId(), partnerOrganisations.get(2).getOrganisation().getId())).thenReturn(serviceSuccess(TRUE));

        Mockito.when(golWorkflowHandlerMock.getState(p)).thenReturn(GrantOfferLetterState.PENDING);
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ProjectPartnerStatusResource expectedLeadPartnerOrganisationStatus = newProjectPartnerStatusResource().
                withName(organisations.get(0).getName()).
                withOrganisationType(
                        OrganisationTypeEnum.getFromId(organisations.get(0).getOrganisationType().getId())).
                withOrganisationId(organisations.get(0).getId()).
                withProjectDetailsStatus(ACTION_REQUIRED).
                withFinanceContactStatus(ACTION_REQUIRED).
                withPartnerProjectLocationStatus(ACTION_REQUIRED).
                withMonitoringOfficerStatus(NOT_STARTED).
                withBankDetailsStatus(PENDING).
                withFinanceChecksStatus(PENDING).
                withSpendProfileStatus(NOT_STARTED).
                withOtherDocumentsStatus(ACTION_REQUIRED).
                withGrantOfferStatus(NOT_REQUIRED).
                withIsLeadPartner(true).
                build();

        List<ProjectPartnerStatusResource> expectedFullPartnerStatuses = newProjectPartnerStatusResource().
                withName(organisations.get(1).getName(), organisations.get(2).getName()).
                withOrganisationType(
                        OrganisationTypeEnum.getFromId(organisations.get(1).getOrganisationType().getId()),
                        OrganisationTypeEnum.getFromId(organisations.get(2).getOrganisationType().getId())).
                withOrganisationId(organisations.get(1).getId(), organisations.get(2).getId()).
                withProjectDetailsStatus(ACTION_REQUIRED, ACTION_REQUIRED).
                withFinanceContactStatus(ACTION_REQUIRED, ACTION_REQUIRED).
                withPartnerProjectLocationStatus(COMPLETE, ACTION_REQUIRED).
                withMonitoringOfficerStatus(NOT_REQUIRED, NOT_REQUIRED).
                withBankDetailsStatus(NOT_REQUIRED, NOT_STARTED).
                withFinanceChecksStatus(PENDING, ACTION_REQUIRED).
                withSpendProfileStatus(NOT_STARTED, NOT_STARTED).
                withOtherDocumentsStatus(NOT_REQUIRED, NOT_REQUIRED).
                withGrantOfferStatus(NOT_REQUIRED, NOT_REQUIRED).
                build(2);

        ProjectTeamStatusResource expectedProjectTeamStatusResource = newProjectTeamStatusResource().
                withProjectLeadStatus(expectedLeadPartnerOrganisationStatus).
                withPartnerStatuses(expectedFullPartnerStatuses).
                build();

        // try without filtering
        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.empty());
        assertTrue(result.isSuccess());
        assertEquals(expectedProjectTeamStatusResource, result.getSuccess());

        List<ProjectPartnerStatusResource> expectedPartnerStatusesFilteredOnNonLead = newProjectPartnerStatusResource().
                withName(organisations.get(2).getName()).
                withOrganisationType(
                        OrganisationTypeEnum.getFromId(organisations.get(2).getOrganisationType().getId())).
                withOrganisationId(organisations.get(2).getId()).
                withProjectDetailsStatus(ACTION_REQUIRED).
                withFinanceContactStatus(ACTION_REQUIRED).
                withPartnerProjectLocationStatus(ACTION_REQUIRED).
                withMonitoringOfficerStatus(NOT_REQUIRED).
                withBankDetailsStatus(NOT_STARTED).
                withFinanceChecksStatus(ACTION_REQUIRED).
                withSpendProfileStatus(NOT_STARTED).
                withOtherDocumentsStatus(NOT_REQUIRED).
                withGrantOfferStatus(NOT_REQUIRED).
                build(1);

        // try with filtering on a non-lead partner organisation
        ProjectTeamStatusResource expectedProjectTeamStatusResourceFilteredOnNonLead = newProjectTeamStatusResource().
                withProjectLeadStatus(expectedLeadPartnerOrganisationStatus).
                withPartnerStatuses(expectedPartnerStatusesFilteredOnNonLead).
                build();

        ServiceResult<ProjectTeamStatusResource> resultWithNonLeadFilter = service.getProjectTeamStatus(p.getId(), Optional.of(users.get(2).getId()));
        assertTrue(resultWithNonLeadFilter.isSuccess());
        assertEquals(expectedProjectTeamStatusResourceFilteredOnNonLead, resultWithNonLeadFilter.getSuccess());

        // try with filtering on a lead partner organisation
        ProjectTeamStatusResource expectedProjectTeamStatusResourceFilteredOnLead = newProjectTeamStatusResource().
                withProjectLeadStatus(expectedLeadPartnerOrganisationStatus).
                build();

        ServiceResult<ProjectTeamStatusResource> resultWithLeadFilter = service.getProjectTeamStatus(p.getId(), Optional.of(users.get(0).getId()));
        assertTrue(resultWithLeadFilter.isSuccess());
        assertEquals(expectedProjectTeamStatusResourceFilteredOnLead, resultWithLeadFilter.getSuccess());


        // test MO status is pending and not action required when project details submitted
        Mockito.when(projectDetailsWorkflowHandlerMock.isSubmitted(any(Project.class))).thenReturn(true);
        Mockito.when(monitoringOfficerRepositoryMock.findOneByProjectId(p.getId())).thenReturn(null);

        ProjectPartnerStatusResource expectedLeadPartnerOrganisationStatusWhenPDSubmitted = newProjectPartnerStatusResource().
                withName(organisations.get(0).getName()).
                withOrganisationType(
                        OrganisationTypeEnum.getFromId(organisations.get(0).getOrganisationType().getId())).
                withOrganisationId(organisations.get(0).getId()).
                withProjectDetailsStatus(COMPLETE).
                withFinanceContactStatus(ACTION_REQUIRED).
                withPartnerProjectLocationStatus(ACTION_REQUIRED).
                withMonitoringOfficerStatus(PENDING).
                withBankDetailsStatus(PENDING).
                withFinanceChecksStatus(PENDING).
                withSpendProfileStatus(NOT_STARTED).
                withOtherDocumentsStatus(ACTION_REQUIRED).
                withGrantOfferStatus(NOT_REQUIRED).
                withIsLeadPartner(true).
                build();

        ProjectTeamStatusResource expectedProjectTeamStatusResourceWhenPSSubmitted = newProjectTeamStatusResource().
                withProjectLeadStatus(expectedLeadPartnerOrganisationStatusWhenPDSubmitted).
                withPartnerStatuses(expectedFullPartnerStatuses).
                build();

        ServiceResult<ProjectTeamStatusResource> resultForPSSubmmited = service.getProjectTeamStatus(p.getId(), Optional.empty());
        assertTrue(resultForPSSubmmited.isSuccess());
        assertEquals(expectedProjectTeamStatusResourceWhenPSSubmitted, resultForPSSubmmited.getSuccess());
    }

    @Test
    public void isGrantOfferLetterActionRequired() {

        FileEntry golFile = newFileEntry().withFilesizeBytes(10).withMediaType("application/pdf").build();

        List<ProjectUser> pu = newProjectUser().withRole(PROJECT_FINANCE_CONTACT).withUser(u).withOrganisation(o).withInvite(newProjectInvite().build()).build(1);
        List<PartnerOrganisation> po = newPartnerOrganisation().withOrganisation(o).withLeadOrganisation(TRUE).build(1);
        Project p = newProject().withProjectUsers(pu).withApplication(application).withPartnerOrganisations(po).withDateSubmitted(ZonedDateTime.now()).withOtherDocumentsApproved(ApprovalType.APPROVED).withGrantOfferLetter(golFile).build();
        List<ProjectUserResource> puResource = newProjectUserResource().withProject(p.getId()).withOrganisation(o.getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(1);

        BankDetails bankDetails = newBankDetails().withOrganisation(o).withApproval(TRUE).build();
        SpendProfile spendProfile = newSpendProfile().withOrganisation(o).withMarkedComplete(TRUE).build();

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        Mockito.when(golWorkflowHandlerMock.getState(p)).thenReturn(GrantOfferLetterState.SENT);
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && ACTION_REQUIRED.equals(result.getSuccess().getLeadPartnerStatus().getGrantOfferLetterStatus()));
    }

    @Test
    public void isGrantOfferLetterIsPendingLeadPartner() {

        FileEntry golFile = newFileEntry().withFilesizeBytes(10).withMediaType("application/pdf").build();

        List<ProjectUser> pu = newProjectUser().withRole(PROJECT_FINANCE_CONTACT).withUser(u).withOrganisation(o).withInvite(newProjectInvite().build()).build(1);
        List<PartnerOrganisation> po = newPartnerOrganisation().withOrganisation(o).withLeadOrganisation(TRUE).build(1);
        Project p = newProject().withProjectUsers(pu).withApplication(application).withPartnerOrganisations(po).withDateSubmitted(ZonedDateTime.now()).withOtherDocumentsApproved(ApprovalType.APPROVED).withGrantOfferLetter(golFile).withSignedGrantOfferLetter(golFile).build();
        List<ProjectUserResource> puResource = newProjectUserResource().withProject(p.getId()).withOrganisation(o.getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(1);

        BankDetails bankDetails = newBankDetails().withOrganisation(o).withApproval(TRUE).build();
        SpendProfile spendProfile = newSpendProfile().withOrganisation(o).withMarkedComplete(TRUE).build();

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> resultWhenGolIsNotSent = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(resultWhenGolIsNotSent.isSuccess() && PENDING.equals(resultWhenGolIsNotSent.getSuccess().getLeadPartnerStatus().getGrantOfferLetterStatus()));

        // Same flow but when GOL is in Ready To Approve state.
        Mockito.when(golWorkflowHandlerMock.isReadyToApprove(p)).thenReturn(TRUE);

        // Call the service again
        ServiceResult<ProjectTeamStatusResource> resultWhenGolIsReadyToApprove = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(resultWhenGolIsReadyToApprove.isSuccess() && PENDING.equals(resultWhenGolIsReadyToApprove.getSuccess().getLeadPartnerStatus().getGrantOfferLetterStatus()));
    }

    @Test
    public void isGrantOfferLetterIsPendingNonLeadPartner() {
        User u = newUser().withEmailAddress("a@b.com").build();

        OrganisationType businessOrganisationType = newOrganisationType().withOrganisationType(OrganisationTypeEnum.BUSINESS).build();
        Organisation o = organisationRepositoryMock.findOne(application.getLeadOrganisationId());
        o.setOrganisationType(businessOrganisationType);

        FileEntry golFile = newFileEntry().withFilesizeBytes(10).withMediaType("application/pdf").build();

        Organisation nonLeadOrg = newOrganisation().build();
        nonLeadOrg.setOrganisationType(businessOrganisationType);

        List<ProjectUser> pu = newProjectUser().withRole(PROJECT_FINANCE_CONTACT).withUser(u).withOrganisation(nonLeadOrg).withInvite(newProjectInvite().build()).build(1);
        List<PartnerOrganisation> po = newPartnerOrganisation().withOrganisation(nonLeadOrg).withLeadOrganisation(FALSE).build(1);
        Project p = newProject().withProjectUsers(pu).withApplication(application).withPartnerOrganisations(po).withOtherDocumentsApproved(ApprovalType.APPROVED).withGrantOfferLetter(golFile).withSignedGrantOfferLetter(golFile).withDateSubmitted(ZonedDateTime.now()).build();
        List<ProjectUserResource> puResource = newProjectUserResource().withProject(p.getId()).withOrganisation(nonLeadOrg.getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(1);

        BankDetails bankDetails = newBankDetails().withOrganisation(o).withApproval(TRUE).build();
        SpendProfile spendProfile = newSpendProfile().withOrganisation(o).withMarkedComplete(TRUE).build();

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), nonLeadOrg.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(anyLong(), anyLong())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), nonLeadOrg.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        GrantOfferLetterStateResource sentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.SENT, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(sentGrantOfferLetterState));

        // Same flow but when GOL is in Ready To Approve state.
        Mockito.when(golWorkflowHandlerMock.isReadyToApprove(p)).thenReturn(TRUE);

        Mockito.when(financeServiceMock.organisationSeeksFunding(p.getId(), p.getApplication().getId(), o.getId())).thenReturn(serviceSuccess(TRUE));
        Mockito.when(financeServiceMock.organisationSeeksFunding(p.getId(), p.getApplication().getId(), nonLeadOrg.getId())).thenReturn(serviceSuccess(TRUE));
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(anyLong(),anyLong())).thenReturn(serviceSuccess(FALSE));


        // Call the service again
        ServiceResult<ProjectTeamStatusResource> resultWhenGolIsReadyToApprove = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(resultWhenGolIsReadyToApprove.isSuccess() && PENDING.equals(resultWhenGolIsReadyToApprove.getSuccess().getPartnerStatuses().get(0).getGrantOfferLetterStatus()));

    }

    @Test
    public void isGrantOfferLetterComplete() {

        FileEntry golFile = newFileEntry().withFilesizeBytes(10).withMediaType("application/pdf").build();

        List<ProjectUser> pu = newProjectUser().withRole(PROJECT_FINANCE_CONTACT).withUser(u).withOrganisation(o).withInvite(newProjectInvite().build()).build(1);
        List<PartnerOrganisation> po = newPartnerOrganisation().withOrganisation(o).withLeadOrganisation(TRUE).build(1);
        Project p = newProject().withProjectUsers(pu).withApplication(application).withPartnerOrganisations(po).withDateSubmitted(ZonedDateTime.now()).withOtherDocumentsApproved(ApprovalType.APPROVED).withGrantOfferLetter(golFile).withSignedGrantOfferLetter(golFile).withOfferSubmittedDate(ZonedDateTime.now()).build();
        List<ProjectUserResource> puResource = newProjectUserResource().withProject(p.getId()).withOrganisation(o.getId()).withRole(partnerRole.getId()).withRoleName(PROJECT_PARTNER.getName()).build(1);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(golWorkflowHandlerMock.getState(p)).thenReturn(GrantOfferLetterState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && COMPLETE.equals(result.getSuccess().getLeadPartnerStatus().getGrantOfferLetterStatus()));
    }

    @Test
    public void spendProfileNotComplete() {

        spendProfile.setMarkedAsComplete(false);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && ACTION_REQUIRED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileRequiresEligibility() {
        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.empty());
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.REVIEW);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && NOT_STARTED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileRequiresViability() {

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.empty());
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.REVIEW);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && NOT_STARTED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileNotSubmittedViabilityNotApplicable() {

        p.setSpendProfileSubmittedDate(null);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.NOT_APPLICABLE);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && ACTION_REQUIRED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileCompleteNotSubmitted() {

        p.setSpendProfileSubmittedDate(null);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && ACTION_REQUIRED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileCompleteSubmitted() {

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && PENDING.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
    }

    @Test
    public void spendProfileCompleteRejected() {
        p.setSpendProfileSubmittedDate(null);

        Mockito.when(projectRepositoryMock.findOne(p.getId())).thenReturn(p);
        Mockito.when(projectUserRepositoryMock.findByProjectId(p.getId())).thenReturn(pu);
        Mockito.when(projectUserMapperMock.mapToResource(pu.get(0))).thenReturn(puResource.get(0));
        Mockito.when(organisationRepositoryMock.findOne(o.getId())).thenReturn(o);
        Mockito.when(partnerOrganisationRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(po.get(0));
        Mockito.when(bankDetailsRepositoryMock.findByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(bankDetails);
        Mockito.when(spendProfileRepositoryMock.findOneByProjectIdAndOrganisationId(p.getId(), o.getId())).thenReturn(Optional.ofNullable(spendProfile));
        Mockito.when(eligibilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(EligibilityState.APPROVED);
        Mockito.when(viabilityWorkflowHandlerMock.getState(po.get(0))).thenReturn(ViabilityState.APPROVED);
        Mockito.when(financeCheckServiceMock.isQueryActionRequired(p.getId(),o.getId())).thenReturn(serviceSuccess(FALSE));
        GrantOfferLetterStateResource unsentGrantOfferLetterState =
                GrantOfferLetterStateResource.stateInformationForNonPartnersView(GrantOfferLetterState.PENDING, null);
        Mockito.when(golWorkflowHandlerMock.getExtendedState(p)).thenReturn(serviceSuccess(unsentGrantOfferLetterState));

        ServiceResult<ProjectTeamStatusResource> result = service.getProjectTeamStatus(p.getId(), Optional.ofNullable(pu.get(0).getId()));

        assertTrue(result.isSuccess() && ACTION_REQUIRED.equals(result.getSuccess().getLeadPartnerStatus().getSpendProfileStatus()));
        assertTrue(project.getSpendProfileSubmittedDate() == null);
    }

}
