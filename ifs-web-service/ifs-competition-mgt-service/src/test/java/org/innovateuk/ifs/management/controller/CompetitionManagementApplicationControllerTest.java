package org.innovateuk.ifs.management.controller;

import org.innovateuk.ifs.AbstractApplicationMockMVCTest;
import org.innovateuk.ifs.applicant.service.ApplicantRestService;
import org.innovateuk.ifs.application.common.populator.ApplicationFinanceSummaryViewModelPopulator;
import org.innovateuk.ifs.application.common.populator.ApplicationFundingBreakdownViewModelPopulator;
import org.innovateuk.ifs.application.common.populator.ApplicationResearchParticipationViewModelPopulator;
import org.innovateuk.ifs.application.common.populator.SummaryViewModelFragmentPopulator;
import org.innovateuk.ifs.application.finance.view.ApplicationFinanceOverviewModelManager;
import org.innovateuk.ifs.application.populator.ApplicationModelPopulator;
import org.innovateuk.ifs.application.populator.ApplicationSectionAndQuestionModelPopulator;
import org.innovateuk.ifs.application.populator.OrganisationDetailsModelPopulator;
import org.innovateuk.ifs.application.populator.forminput.FormInputViewModelGenerator;
import org.innovateuk.ifs.application.populator.researchCategory.ApplicationResearchCategorySummaryModelPopulator;
import org.innovateuk.ifs.application.resource.*;
import org.innovateuk.ifs.application.service.ApplicationSummaryRestService;
import org.innovateuk.ifs.assessment.service.AssessmentRestService;
import org.innovateuk.ifs.category.resource.ResearchCategoryResource;
import org.innovateuk.ifs.category.service.CategoryRestService;
import org.innovateuk.ifs.commons.error.CommonFailureKeys;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.exception.ForbiddenActionException;
import org.innovateuk.ifs.commons.exception.ObjectNotFoundException;
import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.competition.resource.CompetitionStatus;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.invite.InviteService;
import org.innovateuk.ifs.management.application.list.form.ReinstateIneligibleApplicationForm;
import org.innovateuk.ifs.management.application.view.controller.CompetitionManagementApplicationController;
import org.innovateuk.ifs.management.application.view.populator.ApplicationOverviewIneligibilityModelPopulator;
import org.innovateuk.ifs.management.application.view.populator.ApplicationTeamModelManagementPopulator;
import org.innovateuk.ifs.management.application.view.populator.ManageApplicationModelPopulator;
import org.innovateuk.ifs.management.application.view.populator.ReinstateIneligibleApplicationModelPopulator;
import org.innovateuk.ifs.management.application.view.service.CompetitionManagementApplicationServiceImpl;
import org.innovateuk.ifs.management.application.view.viewmodel.ApplicationOverviewIneligibilityViewModel;
import org.innovateuk.ifs.management.application.view.viewmodel.ApplicationTeamViewModel;
import org.innovateuk.ifs.management.application.view.viewmodel.ManageApplicationViewModel;
import org.innovateuk.ifs.management.application.view.viewmodel.ReinstateIneligibleApplicationViewModel;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.resource.ProcessRoleResource;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.applicant.builder.ApplicantQuestionResourceBuilder.newApplicantQuestionResource;
import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.application.builder.FormInputResponseResourceBuilder.newFormInputResponseResource;
import static org.innovateuk.ifs.application.builder.IneligibleOutcomeResourceBuilder.newIneligibleOutcomeResource;
import static org.innovateuk.ifs.application.resource.ApplicationState.SUBMITTED;
import static org.innovateuk.ifs.application.service.Futures.settable;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.id;
import static org.innovateuk.ifs.category.builder.ResearchCategoryResourceBuilder.newResearchCategoryResource;
import static org.innovateuk.ifs.commons.error.CommonErrors.internalServerErrorError;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.APPLICATION_MUST_BE_SUBMITTED;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.GENERAL_NOT_FOUND;
import static org.innovateuk.ifs.commons.rest.RestResult.restFailure;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.file.builder.FileEntryResourceBuilder.newFileEntryResource;
import static org.innovateuk.ifs.user.builder.ProcessRoleResourceBuilder.newProcessRoleResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class CompetitionManagementApplicationControllerTest extends AbstractApplicationMockMVCTest<CompetitionManagementApplicationController> {

    @Spy
    @InjectMocks
    private ApplicationModelPopulator applicationModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationSectionAndQuestionModelPopulator applicationSectionAndQuestionModelPopulator;

    @Mock
    private ApplicationResearchCategorySummaryModelPopulator applicationResearchCategorySummaryModelPopulator;

    @Spy
    @InjectMocks
    private ManageApplicationModelPopulator manageApplicationModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationOverviewIneligibilityModelPopulator applicationOverviewIneligibilityModelPopulator;

    @Spy
    @InjectMocks
    private CompetitionManagementApplicationServiceImpl competitionManagementApplicationServiceImpl;

    @Spy
    @InjectMocks
    private OrganisationDetailsModelPopulator organisationDetailsModelPopulator;

    @Spy
    @InjectMocks
    private SummaryViewModelFragmentPopulator summaryViewModelFragmentPopulator;

    @Spy
    @InjectMocks
    private ApplicationTeamModelManagementPopulator applicationTeamModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationFinanceSummaryViewModelPopulator applicationFinanceSummaryViewModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationFundingBreakdownViewModelPopulator applicationFundingBreakdownViewModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationResearchParticipationViewModelPopulator applicationResearchParticipationViewModelPopulator;

    @Mock
    private ApplicantRestService applicantRestService;

    @Mock
    private FormInputViewModelGenerator formInputViewModelGenerator;

    @Spy
    @InjectMocks
    private ReinstateIneligibleApplicationModelPopulator reinstateIneligibleApplicationModelPopulator;

    @Spy
    @InjectMocks
    private ApplicationTeamModelManagementPopulator applicationTeamManagementModelPopulator;

    @Mock
    private CategoryRestService categoryRestServiceMock;

    @Mock
    private ApplicationSummaryRestService applicationSummaryRestService;

    @Mock
    private InviteService inviteService;

    @Mock
    private ApplicationFinanceOverviewModelManager applicationFinanceOverviewModelManager;

    @Mock
    private AssessmentRestService assessmentRestService;

    @Test
    public void displayApplicationOverviewAsCompAdmin() throws Exception {
        this.setLoggedInUser(newUserResource().withRolesGlobal(singletonList(Role.COMP_ADMIN)).build());
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();


        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getSummaryViewModel().getCurrentApplication().getId(), applications.get(0).getId());
    }

    @Test
    public void displayApplicationOverview_backUrlPreservesQueryParams() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        String expectedBackUrl = "/competition/" + competitionResource.getId() + "/applications/all?param1=abc&param2=def%26ghi";

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("param1", "abc")
                .param("param2", "def&ghi"))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getBackUrl(), expectedBackUrl);
    }

    @Test
    public void displayApplicationOverview_backUrlEncodesReservedChars() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        String expectedBackUrl = "/competition/" + competitionResource.getId() + "/applications/all?p1=%26&p2=%3D&p3=%25&p4=%20";

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("p1", "&")
                .param("p2", "=")
                .param("p3", "%")
                .param("p4", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getBackUrl(), expectedBackUrl);
    }

    @Test
    public void displayApplicationOverview_applicationIneligible() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupEmptyResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        ZonedDateTime now = ZonedDateTime.now();

        applications.get(0).setApplicationState(ApplicationState.INELIGIBLE);
        applications.get(0).setIneligibleOutcome(newIneligibleOutcomeResource()
                .withReason("Reason for removal...")
                .withRemovedBy("Removed by")
                .withRemovedOn(now)
                .build());

        ApplicationOverviewIneligibilityViewModel expectedIneligibility = new ApplicationOverviewIneligibilityViewModel(
                false, "Removed by", now, "Reason for removal...");

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getIneligibility(), expectedIneligibility);
        assertEquals(resultModel.getBackUrl(), "/competition/" + competitionResource.getId() + "/applications/all");
    }

    @Test
    public void readOnlyIneligibleViewModelAttrDependsOnCompetitionState() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupEmptyResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        ZonedDateTime now = ZonedDateTime.now();

        applications.get(0).setApplicationState(ApplicationState.INELIGIBLE);
        applications.get(0).setIneligibleOutcome(newIneligibleOutcomeResource()
                .withReason("Reason for removal...")
                .withRemovedBy("Removed by")
                .withRemovedOn(now)
                .build());

        // Competition is beyond assessment (in panel)

        competitionResource.setCompetitionStatus(CompetitionStatus.FUNDERS_PANEL);

        when(competitionRestService.getCompetitionById(competitionResource.getId())).thenReturn(restSuccess(competitionResource));

        ApplicationOverviewIneligibilityViewModel expectedIneligibility = new ApplicationOverviewIneligibilityViewModel(
                true, "Removed by", now, "Reason for removal...");

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getIneligibility(), expectedIneligibility);
        assertEquals(resultModel.getBackUrl(), "/competition/" + competitionResource.getId() + "/applications/all");

        // Competition still in assessment state

        competitionResource.setCompetitionStatus(CompetitionStatus.IN_ASSESSMENT);

        when(competitionRestService.getCompetitionById(competitionResource.getId())).thenReturn(restSuccess(competitionResource));

        expectedIneligibility = new ApplicationOverviewIneligibilityViewModel(
                false, "Removed by", now, "Reason for removal...");

        result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getIneligibility(), expectedIneligibility);
        assertEquals(resultModel.getBackUrl(), "/competition/" + competitionResource.getId() + "/applications/all");
    }

    @Test
    public void displayApplicationOverview_submittedApplicationsOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationTeam();
        when(assessmentRestService.getByUserAndApplication(anyLong(), anyLong())).thenReturn(restSuccess(emptyList()));
        when(applicationFinanceRestService.getResearchParticipationPercentage(anyLong())).thenReturn(restSuccess(1D));

        assertApplicationOverviewWithBackUrl("SUBMITTED_APPLICATIONS",
                "/competition/" + competitionResource.getId() + "/applications/submitted");
    }

    @Test
    public void displayApplicationOverview_manageApplicationsOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        assertApplicationOverviewWithBackUrl("MANAGE_APPLICATIONS",
                "/assessment/competition/" + competitionResource.getId() + "/applications");
    }

    @Test
    public void displayApplicationOverview_applicationProgressOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        assertApplicationOverviewWithBackUrl("APPLICATION_PROGRESS",
                "/assessment/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/assessors");
    }

    @Test
    public void displayApplicationOverview_fundingApplicationsOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        assertApplicationOverviewWithBackUrl("FUNDING_APPLICATIONS",
                "/competition/" + competitionResource.getId() + "/funding");
    }

    @Test
    public void displayApplicationOverview_IneligibleApplicationsOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationTeam();
        when(assessmentRestService.getByUserAndApplication(anyLong(), anyLong())).thenReturn(restSuccess(emptyList()));
        when(applicationFinanceRestService.getResearchParticipationPercentage(anyLong())).thenReturn(restSuccess(1D));

        assertApplicationOverviewWithBackUrl("INELIGIBLE_APPLICATIONS",
                "/competition/" + competitionResource.getId() + "/applications/ineligible");
    }

    @Test
    public void displayApplicationOverview_assessorProgressOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();

        this.setupApplicationResponses();
        this.setupInvites();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        when(assessmentRestService.getByUserAndApplication(anyLong(), anyLong())).thenReturn(restSuccess(emptyList()));
        when(applicationFinanceRestService.getResearchParticipationPercentage(anyLong())).thenReturn(restSuccess(1D));

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("origin", "ASSESSOR_PROGRESS")
                .param("assessorId", "10"))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");

        assertEquals(resultModel.getBackUrl(), "/assessment/competition/" + competitionResource.getId() + "/assessors/10");
    }

    @Test
    public void displayApplicationOverview_invalidOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        setupApplicantResource();

        mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("origin", "NOT_A_VALID_ORIGIN"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void displayApplicationForCompetitionAdministratorWithCorrectAssessorFeedbackReadonly() throws Exception {
        asList(CompetitionStatus.values()).forEach(status -> {

            this.setupCompetition();
            this.setupApplicationWithRoles();
            this.setupInvites();
            this.setupOrganisationTypes();
            setupApplicantResource();

            competitionResource.setCompetitionStatus(status);
        });
    }

    @Test
    public void reinstateIneligibleApplication() throws Exception {
        long competitionId = 1L;
        long applicationId = 2L;

        when(applicationRestService.updateApplicationState(applicationId, SUBMITTED)).thenReturn(restSuccess());

        mockMvc.perform(post("/competition/{competitionId}/application/{applicationId}/reinstateIneligibleApplication", competitionId, applicationId))
                .andExpect(model().attributeExists("form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(format("/competition/%s/applications/ineligible", competitionId)));

        verify(applicationRestService).updateApplicationState(applicationId, SUBMITTED);
        verifyNoMoreInteractions(applicationRestService);
    }

    @Test
    public void reinstateIneligibleApplication_failureUpdatingState() throws Exception {
        long competitionId = 1L;

        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        setupApplicantResource();
        ApplicationResource applicationResource = newApplicationResource()
                .withCompetition(competitionId)
                .withName("Plastic reprocessing with zero waste")
                .build();

        when(applicationRestService.updateApplicationState(applicationResource.getId(), SUBMITTED)).thenReturn(restFailure(internalServerErrorError()));
        when(applicationRestService.getApplicationById(applicationResource.getId())).thenReturn(restSuccess(applicationResource));

        ReinstateIneligibleApplicationViewModel expectedViewModel = new ReinstateIneligibleApplicationViewModel(competitionId,
                applicationResource.getId(), "Plastic reprocessing with zero waste");

        MvcResult mvcResult = mockMvc.perform(post("/competition/{competitionId}/application/{applicationId}/reinstateIneligibleApplication", competitionId, applicationResource.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("model", expectedViewModel))
                .andExpect(model().hasErrors())
                .andExpect(model().errorCount(1))
                .andExpect(view().name("application/reinstate-ineligible-application-confirm"))
                .andReturn();

        ReinstateIneligibleApplicationForm form = (ReinstateIneligibleApplicationForm) mvcResult.getModelAndView().getModel().get("form");

        BindingResult bindingResult = form.getBindingResult();
        assertEquals(1, bindingResult.getGlobalErrorCount());
        assertFalse(bindingResult.hasFieldErrors());
        assertEquals(CommonFailureKeys.GENERAL_UNEXPECTED_ERROR.getErrorKey(), bindingResult.getGlobalError().getCode());

        InOrder inOrder = inOrder(applicationRestService);
        inOrder.verify(applicationRestService).updateApplicationState(applicationResource.getId(), SUBMITTED);
        inOrder.verify(applicationRestService).getApplicationById(applicationResource.getId());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void reinstateIneligibleApplicationConfirm() throws Exception {
        long competitionId = 1L;

        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        setupApplicantResource();
        ApplicationResource applicationResource = newApplicationResource()
                .withCompetition(competitionId)
                .withName("Plastic reprocessing with zero waste")
                .build();

        when(applicationRestService.getApplicationById(applicationResource.getId())).thenReturn(restSuccess(applicationResource));

        ReinstateIneligibleApplicationViewModel expectedViewModel = new ReinstateIneligibleApplicationViewModel(competitionId,
                applicationResource.getId(), "Plastic reprocessing with zero waste");

        mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}/reinstateIneligibleApplication/confirm",
                competitionId, applicationResource.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("model", expectedViewModel))
                .andExpect(view().name("application/reinstate-ineligible-application-confirm"));

        verify(applicationRestService).getApplicationById(applicationResource.getId());
        verifyNoMoreInteractions(applicationRestService);
    }

    @Test
    public void markAsIneligible() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        setupApplicantResource();

        IneligibleOutcomeResource ineligibleOutcomeResource = newIneligibleOutcomeResource().withReason("coz").build();
        when(applicationService.markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource))).thenReturn(serviceSuccess());

        mockMvc.perform(post("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("markAsIneligible", "")
                .param("ineligibleReason", "coz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/competition/" + competitionResource.getId() + "/applications/ineligible"));

        verify(applicationService).markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource));
    }

    @Test
    public void markAsIneligible_notSubmitted() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupApplicationTeam();
        setupApplicantResource();

        IneligibleOutcomeResource ineligibleOutcomeResource = newIneligibleOutcomeResource().withReason("coz").build();
        ProcessRoleResource userApplicationRole = newProcessRoleResource().withApplication(applications.get(0).getId()).withOrganisation(organisations.get(0).getId()).build();
        List<ResearchCategoryResource> researchCategories = newResearchCategoryResource().build(3);

        when(formInputResponseRestService.getResponsesByApplicationId(applications.get(0).getId())).thenReturn(restSuccess(new ArrayList<>()));
        when(financeViewHandlerProvider.getFinanceModelManager(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(defaultFinanceModelManager);
        when(questionService.getMarkedAsComplete(anyLong(), anyLong())).thenReturn(settable(new HashSet<>()));
        when(userRestService.findProcessRole(loggedInUser.getId(), applications.get(0).getId())).thenReturn(restSuccess(userApplicationRole));
        when(categoryRestServiceMock.getResearchCategories()).thenReturn(restSuccess(researchCategories));
        when(applicationService.markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource))).thenReturn(serviceFailure(new Error(APPLICATION_MUST_BE_SUBMITTED)));

        mockMvc.perform(post("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "")
                .param("markAsIneligible", "")
                .param("ineligibleReason", "coz"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("competition-mgt-application-overview"));

        verify(applicationService).markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource));
    }

    @Test
    public void markAsIneligible_noReason() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupApplicationTeam();
        setupApplicantResource();

        ProcessRoleResource userApplicationRole = newProcessRoleResource().withApplication(applications.get(0).getId()).withOrganisation(organisations.get(0).getId()).build();
        List<ResearchCategoryResource> researchCategories = newResearchCategoryResource().build(3);

        when(formInputResponseRestService.getResponsesByApplicationId(applications.get(0).getId())).thenReturn(restSuccess(new ArrayList<>()));
        when(financeViewHandlerProvider.getFinanceModelManager(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(defaultFinanceModelManager);
        when(questionService.getMarkedAsComplete(anyLong(), anyLong())).thenReturn(settable(new HashSet<>()));
        when(userRestService.findProcessRole(loggedInUser.getId(), applications.get(0).getId())).thenReturn(restSuccess(userApplicationRole));
        when(categoryRestServiceMock.getResearchCategories()).thenReturn(restSuccess(researchCategories));

        MvcResult result = mockMvc.perform(post("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "")
                .param("markAsIneligible", ""))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ApplicationForm form = (ApplicationForm) result.getModelAndView().getModel().get("form");
        BindingResult bindingResult = form.getBindingResult();
        assertTrue(bindingResult.hasErrors());
        assertEquals(0, bindingResult.getGlobalErrorCount());
        assertEquals(1, bindingResult.getFieldErrorCount());
        assertTrue(bindingResult.hasFieldErrors("ineligibleReason"));
        assertEquals("validation.field.must.not.be.blank", bindingResult.getFieldError("ineligibleReason").getCode());

    }

    @Test
    public void showApplicationTeam() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        ApplicationTeamResource teamResource = new ApplicationTeamResource();
        ApplicationTeamOrganisationResource leadOrg = new ApplicationTeamOrganisationResource();
        leadOrg.setOrganisationName("lead");
        ApplicationTeamUserResource leaderUser = new ApplicationTeamUserResource();
        leaderUser.setEmail("lee.der@email.com");
        leaderUser.setName("Lee Der");
        leaderUser.setPhoneNumber("0800 0800");
        leaderUser.setLead(true);
        leadOrg.setUsers(Collections.singletonList(leaderUser));
        ApplicationTeamOrganisationResource partnerOrg = new ApplicationTeamOrganisationResource();
        partnerOrg.setOrganisationName("Partner");
        ApplicationTeamUserResource partnerUser = new ApplicationTeamUserResource();
        partnerUser.setEmail("pard.ner@email.com");
        partnerUser.setName("Pard Ner");
        partnerUser.setPhoneNumber("0900 9999");
        partnerUser.setLead(false);
        partnerOrg.setUsers(Collections.singletonList(partnerUser));
        teamResource.setLeadOrganisation(leadOrg);
        teamResource.setPartnerOrganisations(Collections.singletonList(partnerOrg));
        when(applicationRestService.getApplicationById(applications.get(0).getId())).thenReturn(restSuccess(applications.get(0)));
        when(applicationSummaryRestService.getApplicationTeam(applications.get(0).getId())).thenReturn(restSuccess(teamResource));
        MvcResult mvcResult = mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/team"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("application/team-read-only"))
                .andReturn();

        ApplicationTeamViewModel resultModel = (ApplicationTeamViewModel) mvcResult.getModelAndView().getModel().get("model");
        assertEquals(applications.get(0).getId().longValue(), resultModel.getApplicationId());
        assertEquals(applications.get(0).getCompetition().longValue(), resultModel.getCompetitionId());
        assertEquals(applications.get(0).getName(), resultModel.getApplicationName());
        assertEquals("lead", resultModel.getTeam().getLeadOrganisation().getOrganisationName());
        assertEquals("lee.der@email.com", resultModel.getTeam().getLeadOrganisation().getUsers().get(0).getEmail());
        assertEquals("pard.ner@email.com", resultModel.getTeam().getPartnerOrganisations().get(0).getUsers().get(0).getEmail());
        verify(applicationRestService).getApplicationById(applications.get(0).getId());
        verify(applicationSummaryRestService).getApplicationTeam(applications.get(0).getId());
    }

    @Test
    public void showApplicationTeamNoApplication() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();

        when(applicationRestService.getApplicationById(applications.get(0).getId())).thenThrow(new ForbiddenActionException());
        mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/team"))
                .andExpect(status().isForbidden())
                .andExpect(view().name("forbidden"));
        verify(applicationRestService).getApplicationById(applications.get(0).getId());
    }

    @Test
    public void showApplicationTeamNoTeam() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();

        when(applicationRestService.getApplicationById(applications.get(0).getId())).thenReturn(restSuccess(applications.get(0)));
        when(applicationSummaryRestService.getApplicationTeam(applications.get(0).getId())).thenThrow(new ObjectNotFoundException());
        mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/team"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("404"));
        verify(applicationRestService).getApplicationById(applications.get(0).getId());
        verify(applicationSummaryRestService).getApplicationTeam(applications.get(0).getId());
    }

    @Test
    public void markAsIneligible_correctBackUrlAfterSubmission() throws Exception {
        String reason = "Test reason";

        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupApplicationTeam();

        IneligibleOutcomeResource ineligibleOutcomeResource = newIneligibleOutcomeResource().withReason(reason).build();
        ProcessRoleResource userApplicationRole = newProcessRoleResource().withApplication(applications.get(0).getId()).withOrganisation(organisations.get(0).getId()).build();
        List<ResearchCategoryResource> researchCategories = newResearchCategoryResource().build(3);

        when(formInputResponseRestService.getResponsesByApplicationId(applications.get(0).getId())).thenReturn(restSuccess(new ArrayList<>()));
        when(financeViewHandlerProvider.getFinanceModelManager(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(defaultFinanceModelManager);
        when(questionService.getMarkedAsComplete(anyLong(), anyLong())).thenReturn(settable(new HashSet<>()));
        when(userRestService.findProcessRole(loggedInUser.getId(), applications.get(0).getId())).thenReturn(restSuccess(userApplicationRole));
        when(categoryRestServiceMock.getResearchCategories()).thenReturn(restSuccess(researchCategories));
        when(applicationService.markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource))).thenReturn(serviceFailure(new Error(APPLICATION_MUST_BE_SUBMITTED)));
        when(assessmentRestService.getByUserAndApplication(anyLong(), anyLong())).thenReturn(restSuccess(emptyList()));
        when(applicationFinanceRestService.getResearchParticipationPercentage(anyLong())).thenReturn(restSuccess(1D));

        MvcResult result = mockMvc.perform(post("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "?origin=SUBMITTED_APPLICATIONS&page=2&sort=name")
                .param("markAsIneligible", "")
                .param("ineligibleReason", reason))
                .andExpect(status().is2xxSuccessful())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getBackUrl(), "/competition/" + competitionResource.getId() + "/applications/submitted?page=2&sort=name");

        verify(applicationService).markAsIneligible(eq(applications.get(0).getId()), eq(ineligibleOutcomeResource));
    }

    @Test
    public void displayApplicationOverview_projectSetupManagementStatusOrigin() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        assertApplicationOverviewWithBackUrl("PROJECT_SETUP_MANAGEMENT_STATUS",
                "/project-setup-management/competition/" + competitionResource.getId() + "/status");
    }

    @Test
    public void downloadAsInternalUser() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();

        Role.internalRoles().forEach(role -> {
            try {
                setLoggedInUser(newUserResource().withRolesGlobal(singletonList(role)).build());
                Long formInputId = 35L;
                long processRoleId = role.ordinal(); // mapping role ordinal as process role (just for mocking)
                List<FormInputResponseResource> inputResponse = newFormInputResponseResource().withUpdatedBy(processRoleId).build(1);
                when(formInputResponseRestService.getByFormInputIdAndApplication(formInputId, applications.get(0).getId())).thenReturn(RestResult.restSuccess(inputResponse));

                ProcessRoleResource processRoleResource = newProcessRoleResource().withId(processRoleId).build();
                when(processRoleService.getById(processRoleId)).thenReturn(settable(processRoleResource));
                ByteArrayResource bar = new ByteArrayResource("File contents".getBytes());
                when(formInputResponseRestService.getFile(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(restSuccess(bar));
                FileEntryResource fileEntryResource = newFileEntryResource().with(id(999L)).withName("file1").withMediaType("text/csv").build();
                FormInputResponseFileEntryResource formInputResponseFileEntryResource = new FormInputResponseFileEntryResource(fileEntryResource, 123L, 456L, 789L);
                when(formInputResponseRestService.getFileDetails(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(RestResult.restSuccess(formInputResponseFileEntryResource));

                mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/forminput/" + formInputId + "/download/file1.csv"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(("text/csv")))
                        .andExpect(header().string("Content-Type", "text/csv"))
                        .andExpect(header().string("Content-disposition", "inline; filename=\"file1\""))
                        .andExpect(content().string("File contents"));

                verify(formInputResponseRestService).getFile(formInputId, applications.get(0).getId(), processRoleId);
                verify(formInputResponseRestService).getFileDetails(formInputId, applications.get(0).getId(), processRoleId);

            } catch (Exception e) {
                fail();
            }
        });
    }

    @Test
    public void downloadAsPartner() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        UserResource user = newUserResource().withRolesGlobal(Collections.singletonList(Role.PARTNER)).build();
        setLoggedInUser(user);

        Long formInputId = 35L;
        long processRoleId = 73L;
        ProcessRoleResource processRoleResource = newProcessRoleResource().withId(processRoleId).build();
        when(userRestService.findProcessRole(user.getId(), applications.get(0).getId())).thenReturn(restSuccess(processRoleResource));
        ByteArrayResource bar = new ByteArrayResource("File contents".getBytes());
        when(formInputResponseRestService.getFile(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(restSuccess(bar));
        FileEntryResource fileEntryResource = newFileEntryResource().with(id(999L)).withName("file1").withMediaType("text/csv").build();
        FormInputResponseFileEntryResource formInputResponseFileEntryResource = new FormInputResponseFileEntryResource(fileEntryResource, 123L, 456L, 789L);
        when(formInputResponseRestService.getFileDetails(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(RestResult.restSuccess(formInputResponseFileEntryResource));

        mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/forminput/" + formInputId + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(("text/csv")))
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-disposition", "inline; filename=\"file1\""))
                .andExpect(content().string("File contents"));

        verify(formInputResponseRestService).getFile(formInputId, applications.get(0).getId(), processRoleId);
        verify(formInputResponseRestService).getFileDetails(formInputId, applications.get(0).getId(), processRoleId);
    }

    @Test
    public void downloadAsPartnerFileDetailsNotFound() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        UserResource user = newUserResource().withRolesGlobal(Collections.singletonList(Role.PARTNER)).build();
        setLoggedInUser(user);

        Long formInputId = 35L;
        long processRoleId = 73L;
        ProcessRoleResource processRoleResource = newProcessRoleResource().withId(processRoleId).build();
        when(userRestService.findProcessRole(user.getId(), applications.get(0).getId())).thenReturn(restSuccess(processRoleResource));
        ByteArrayResource bar = new ByteArrayResource("File contents".getBytes());
        when(formInputResponseRestService.getFile(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(restSuccess(bar));
        when(formInputResponseRestService.getFileDetails(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(RestResult.restFailure(GENERAL_NOT_FOUND));

        mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/forminput/" + formInputId + "/download"))
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("404"));

        verify(formInputResponseRestService).getFile(formInputId, applications.get(0).getId(), processRoleId);
        verify(formInputResponseRestService).getFileDetails(formInputId, applications.get(0).getId(), processRoleId);
    }

    @Test
    public void downloadAsPartnerFileNotFound() throws Exception {
        this.setupCompetition();
        this.setupApplicationWithRoles();
        UserResource user = newUserResource().withRolesGlobal(Collections.singletonList(Role.PARTNER)).build();
        setLoggedInUser(user);

        Long formInputId = 35L;
        long processRoleId = 73L;
        ProcessRoleResource processRoleResource = newProcessRoleResource().withId(processRoleId).build();
        when(userRestService.findProcessRole(user.getId(), applications.get(0).getId())).thenReturn(restSuccess(processRoleResource));
        when(formInputResponseRestService.getFile(formInputId, applications.get(0).getId(), processRoleId)).thenReturn(restFailure(GENERAL_NOT_FOUND));

        mockMvc.perform(get("/competition/" + competitionResource.getId() + "/application/" + applications.get(0).getId() + "/forminput/" + formInputId + "/download"))
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("404"));

        verify(formInputResponseRestService).getFile(formInputId, applications.get(0).getId(), processRoleId);
    }

    @Test
    public void displayApplicationOverviewAsSupport() throws Exception {
        this.setLoggedInUser(newUserResource().withRolesGlobal(singletonList(Role.SUPPORT)).build());
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupApplicationTeam();
        setupApplicantResource();

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.isCanReinstate(), false);
        assertEquals(resultModel.isReadOnly(), true);

    }

    @Test
    public void displayApplicationOverviewAsInnovationLead() throws Exception {
        this.setLoggedInUser(newUserResource().withRolesGlobal(singletonList(Role.INNOVATION_LEAD)).build());
        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupOrganisationTypes();
        this.setupResearchCategories();
        this.setupFinances();
        this.setupApplicationTeam();
        setupApplicantResource();

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.isReadOnly(), false);
        assertEquals(resultModel.isCanReinstate(), false);
    }

    private void assertApplicationOverviewWithBackUrl(final String origin, final String expectedBackUrl) throws Exception {
        this.setupApplicationResponses();
        this.setupInvites();
        this.setupResearchCategories();

        MvcResult result = mockMvc.perform(get("/competition/{competitionId}/application/{applicationId}", competitionResource.getId(), applications.get(0).getId())
                .param("origin", origin))
                .andExpect(status().isOk())
                .andExpect(view().name("competition-mgt-application-overview"))
                .andReturn();

        ManageApplicationViewModel resultModel = (ManageApplicationViewModel) result.getModelAndView().getModel().get("model");
        assertEquals(resultModel.getBackUrl(), expectedBackUrl);
    }

    private void setupEmptyResponses() {
        when(formInputResponseRestService.getResponsesByApplicationId(applications.get(0).getId())).thenReturn(restSuccess(new ArrayList<>()));
    }

    private void setupApplicationTeam() {
        when(applicationRestService.showApplicationTeam(applications.get(0).getId(), loggedInUser.getId())).thenReturn(restSuccess(true));
    }

    private List<ResearchCategoryResource> setupResearchCategories() {
        List<ResearchCategoryResource> researchCategories = newResearchCategoryResource().build(3);
        when(categoryRestServiceMock.getResearchCategories()).thenReturn(restSuccess(researchCategories));
        return researchCategories;
    }

    private void setupApplicantResource() {
        when(applicantRestService.getQuestion(anyLong(), anyLong(), anyLong())).thenReturn(newApplicantQuestionResource().build());
        when(formInputViewModelGenerator.fromQuestion(any(), any())).thenReturn(Collections.emptyList());
        when(assessmentRestService.getByUserAndApplication(anyLong(), anyLong())).thenReturn(restSuccess(emptyList()));
        when(applicationFinanceRestService.getResearchParticipationPercentage(anyLong())).thenReturn(restSuccess(1D));
    }

    @Override
    protected CompetitionManagementApplicationController supplyControllerUnderTest() {
        return new CompetitionManagementApplicationController();
    }
}
