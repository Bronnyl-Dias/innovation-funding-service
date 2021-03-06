package org.innovateuk.ifs.project.eligibility.controller;

import org.innovateuk.ifs.AbstractApplicationMockMVCTest;
import org.innovateuk.ifs.applicant.resource.ApplicantResource;
import org.innovateuk.ifs.applicant.service.ApplicantRestService;
import org.innovateuk.ifs.application.finance.view.DefaultProjectFinanceModelManager;
import org.innovateuk.ifs.application.finance.view.ProjectFinanceFormHandler;
import org.innovateuk.ifs.application.finance.view.ProjectFinanceOverviewModelManager;
import org.innovateuk.ifs.application.finance.viewmodel.FinanceViewModel;
import org.innovateuk.ifs.application.populator.ApplicationModelPopulator;
import org.innovateuk.ifs.application.populator.ApplicationNavigationPopulator;
import org.innovateuk.ifs.application.populator.ApplicationSectionAndQuestionModelPopulator;
import org.innovateuk.ifs.application.populator.OpenProjectFinanceSectionModelPopulator;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.file.resource.FileEntryResource;
import org.innovateuk.ifs.finance.ProjectFinanceService;
import org.innovateuk.ifs.finance.resource.ApplicationFinanceResource;
import org.innovateuk.ifs.finance.resource.cost.FinanceRowItem;
import org.innovateuk.ifs.finance.resource.cost.Materials;
import org.innovateuk.ifs.finance.service.ProjectFinanceRowRestService;
import org.innovateuk.ifs.financecheck.FinanceCheckService;
import org.innovateuk.ifs.financecheck.eligibility.form.FinanceChecksEligibilityForm;
import org.innovateuk.ifs.financecheck.eligibility.viewmodel.FinanceChecksEligibilityViewModel;
import org.innovateuk.ifs.form.Form;
import org.innovateuk.ifs.form.resource.SectionType;
import org.innovateuk.ifs.organisation.resource.OrganisationResource;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.project.ProjectService;
import org.innovateuk.ifs.project.finance.resource.EligibilityRagStatus;
import org.innovateuk.ifs.project.finance.resource.EligibilityResource;
import org.innovateuk.ifs.project.finance.resource.EligibilityState;
import org.innovateuk.ifs.project.finance.resource.FinanceCheckEligibilityResource;
import org.innovateuk.ifs.project.finance.service.ProjectFinanceRestService;
import org.innovateuk.ifs.project.resource.ProjectResource;
import org.innovateuk.ifs.user.resource.FinanceUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.innovateuk.ifs.applicant.builder.ApplicantResourceBuilder.newApplicantResource;
import static org.innovateuk.ifs.applicant.builder.ApplicantSectionResourceBuilder.newApplicantSectionResource;
import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.application.service.Futures.settable;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.ELIGIBILITY_HAS_ALREADY_BEEN_APPROVED;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.file.builder.FileEntryResourceBuilder.newFileEntryResource;
import static org.innovateuk.ifs.finance.builder.ApplicationFinanceResourceBuilder.newApplicationFinanceResource;
import static org.innovateuk.ifs.form.builder.SectionResourceBuilder.newSectionResource;
import static org.innovateuk.ifs.form.resource.SectionType.PROJECT_COST_FINANCES;
import static org.innovateuk.ifs.organisation.builder.OrganisationResourceBuilder.newOrganisationResource;
import static org.innovateuk.ifs.project.builder.ProjectResourceBuilder.newProjectResource;
import static org.innovateuk.ifs.project.finance.builder.FinanceCheckEligibilityResourceBuilder.newFinanceCheckEligibilityResource;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleFilter;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FinanceChecksEligibilityControllerTest extends AbstractApplicationMockMVCTest<FinanceChecksEligibilityController> {
    @Spy
    @InjectMocks
    private ProjectFinanceOverviewModelManager projectFinanceOverviewModelManager;

    @Spy
    @InjectMocks
    private OpenProjectFinanceSectionModelPopulator openFinanceSectionModel;

    @Mock
    private ApplicationModelPopulator applicationModelPopulator;

    @Mock
    private ApplicationSectionAndQuestionModelPopulator applicationSectionAndQuestionModelPopulator;

    @Mock
    private ApplicationNavigationPopulator applicationNavigationPopulator;

    @Mock
    private ApplicantRestService applicantRestService;

    @Mock
    private Model model;

    @Mock
    private ProjectFinanceFormHandler projectFinanceFormHandler;

    @Mock
    private ProjectService projectService;

    @Mock
    private FinanceCheckService financeCheckServiceMock;

    @Mock
    private FinanceUtil financeUtilMock;

    @Mock
    private DefaultProjectFinanceModelManager defaultProjectFinanceModelManager;

    @Mock
    private ProjectFinanceService projectFinanceService;

    @Mock
    private ProjectFinanceRestService projectFinanceRestService;

    @Mock
    private ProjectFinanceRowRestService projectFinanceRowRestService;

    private OrganisationResource industrialOrganisation;

    private OrganisationResource academicOrganisation;

    private ApplicationResource application = newApplicationResource().withId(123L).build();

    private ProjectResource project = newProjectResource().withId(1L).withName("Project1").withApplication(application).build();

    private FinanceCheckEligibilityResource eligibilityOverview = newFinanceCheckEligibilityResource().build();

    @Before
    public void setUp() {

        super.setUp();

        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupFinances();
        this.setupInvites();
        this.setupQuestionStatus(applications.get(0));

        application = applications.get(0);
        project.setApplication(application.getId());
        project.setCompetition(competitionId);

        industrialOrganisation = newOrganisationResource()
                .withId(2L)
                .withName("Industrial Org")
                .withCompaniesHouseNumber("123456789")
                .withOrganisationTypeName(OrganisationTypeEnum.BUSINESS.name())
                .withOrganisationType(OrganisationTypeEnum.BUSINESS.getId())
                .build();

        academicOrganisation = newOrganisationResource()
                .withId(1L)
                .withName("Academic Org")
                .withOrganisationTypeName(OrganisationTypeEnum.RESEARCH.name())
                .withOrganisationType(OrganisationTypeEnum.RESEARCH.getId())
                .build();

        when(questionService.getMarkedAsComplete(anyLong(), anyLong())).thenReturn(settable(new HashSet<>()));
        when(sectionService.getAllByCompetitionId(anyLong())).thenReturn(sectionResources);
        when(applicationService.getById(application.getId())).thenReturn(application);

        ApplicantResource applicant = newApplicantResource().withProcessRole(processRoles.get(0)).withOrganisation(industrialOrganisation).build();
        when(applicantRestService.getSection(loggedInUser.getId(), application.getId(), simpleFilter(sectionResources, s -> s.getType().equals(PROJECT_COST_FINANCES)).get(0).getId())).thenReturn(
                newApplicantSectionResource()
                        .withApplication(application)
                        .withCompetition(competitionResource)
                        .withCurrentApplicant(applicant)
                        .withApplicants(asList(applicant))
                        .withSection(newSectionResource().withType(SectionType.FINANCE).build())
                        .withCurrentUser(loggedInUser).build());
        when(userRestService.retrieveUserById(loggedInUser.getId())).thenReturn(restSuccess(loggedInUser));

        when(projectService.getById(project.getId())).thenReturn(project);
        when(projectService.getByApplicationId(application.getId())).thenReturn(project);
        when(organisationRestService.getOrganisationById(industrialOrganisation.getId())).thenReturn(restSuccess(industrialOrganisation));
        when(organisationRestService.getOrganisationById(academicOrganisation.getId())).thenReturn(restSuccess(academicOrganisation));
        when(projectService.getLeadOrganisation(project.getId())).thenReturn(industrialOrganisation);
        when(financeCheckServiceMock.getFinanceCheckEligibilityDetails(project.getId(), industrialOrganisation.getId())).thenReturn(eligibilityOverview);
        when(financeViewHandlerProvider.getProjectFinanceModelManager(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(defaultProjectFinanceModelManager);
        when(financeViewHandlerProvider.getProjectFinanceFormHandler(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(projectFinanceFormHandler);

        when(financeUtilMock.isUsingJesFinances(competitionResource, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(Boolean.FALSE);
        when(financeUtilMock.isUsingJesFinances(competitionResource, OrganisationTypeEnum.RESEARCH.getId())).thenReturn(Boolean.TRUE);

        ApplicationFinanceResource appFinanceResource = newApplicationFinanceResource().withFinanceFileEntry(123L).build();
        when(financeService.getApplicationFinanceByApplicationIdAndOrganisationId(application.getId(), 2L)).thenReturn(appFinanceResource);
        when(financeService.getApplicationFinanceByApplicationIdAndOrganisationId(application.getId(), 1L)).thenReturn(appFinanceResource);
        FileEntryResource jesFile = newFileEntryResource().withId(987L).withName("Jes1").build();
        when(financeService.getFinanceEntry(123L)).thenReturn(restSuccess(jesFile));

        FinanceViewModel financeViewModel = new FinanceViewModel();
        financeViewModel.setOrganisationGrantClaimPercentage(74);

        when(defaultProjectFinanceModelManager.getFinanceViewModel(anyLong(), anyList(), anyLong(), any(Form.class), anyLong())).thenReturn(financeViewModel);
        when(projectFinanceRestService.getFinanceTotals(project.getId())).thenReturn(restSuccess(Collections.emptyList()));
    }

    @Test
    public void testViewEligibilityLeadOrg() throws Exception {

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        when(projectService.getLeadOrganisation(project.getId())).thenReturn(industrialOrganisation);

        MvcResult result = mockMvc.perform(get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility",
                project.getId(), industrialOrganisation.getId())).
                andExpect(status().isOk()).
                andExpect(model().attributeExists("model")).
                andExpect(view().name("project/financecheck/eligibility")).
                andReturn();

        assertViewEligibilityDetails(eligibility, result, true, industrialOrganisation.getName(), false, "Jes1");

    }

    @Test
    public void testViewEligibilityNonLeadOrg() throws Exception {

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        when(projectService.getLeadOrganisation(project.getId())).thenReturn(academicOrganisation);

        MvcResult result = mockMvc.perform(get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility",
                project.getId(), industrialOrganisation.getId())).
                andExpect(status().isOk()).
                andExpect(model().attributeExists("model")).
                andExpect(view().name("project/financecheck/eligibility")).
                andReturn();

        assertViewEligibilityDetails(eligibility, result, false, industrialOrganisation.getName(), false, "Jes1");

    }

    @Test
    public void testViewEligibilityLeadOrgIsAcademic() throws Exception {

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        when(projectService.getLeadOrganisation(project.getId())).thenReturn(academicOrganisation);
        when(financeViewHandlerProvider.getProjectFinanceModelManager(competitionResource, OrganisationTypeEnum.RESEARCH.getId())).thenReturn(defaultProjectFinanceModelManager);
        when(financeViewHandlerProvider.getProjectFinanceFormHandler(competitionResource, OrganisationTypeEnum.RESEARCH.getId())).thenReturn(projectFinanceFormHandler);

        MvcResult result = mockMvc.perform(get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility",
                project.getId(), academicOrganisation.getId())).
                andExpect(status().isOk()).
                andExpect(model().attributeExists("summaryModel")).
                andExpect(view().name("project/financecheck/eligibility")).
                andReturn();

        assertViewEligibilityDetails(eligibility, result, true, academicOrganisation.getName(), true, "Jes1");

    }

    @Test
    public void testViewEligibilityLeadOrgIsAcademicNoJesFileEntry() throws Exception {

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        when(projectService.getLeadOrganisation(project.getId())).thenReturn(academicOrganisation);
        when(financeViewHandlerProvider.getProjectFinanceModelManager(competitionResource, OrganisationTypeEnum.RESEARCH.getId())).thenReturn(defaultProjectFinanceModelManager);
        when(financeViewHandlerProvider.getProjectFinanceFormHandler(competitionResource, OrganisationTypeEnum.RESEARCH.getId())).thenReturn(projectFinanceFormHandler);

        ApplicationFinanceResource appFinanceResource = newApplicationFinanceResource().build();
        when(financeService.getApplicationFinanceByApplicationIdAndOrganisationId(application.getId(), 1L)).thenReturn(appFinanceResource);

        MvcResult result = mockMvc.perform(get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility",
                project.getId(), academicOrganisation.getId())).
                andExpect(status().isOk()).
                andExpect(model().attributeExists("summaryModel")).
                andExpect(view().name("project/financecheck/eligibility")).
                andReturn();

        assertViewEligibilityDetails(eligibility, result, true, academicOrganisation.getName(), true, null);

    }

    private void setUpViewEligibilityMocking(EligibilityResource eligibility) {

        eligibility.setEligibilityApprovalDate(LocalDate.now());
        eligibility.setEligibilityApprovalUserFirstName("Lee");
        eligibility.setEligibilityApprovalUserLastName("Bowman");

        when(projectFinanceService.getEligibility(project.getId(), industrialOrganisation.getId())).thenReturn(eligibility);
        when(projectFinanceService.getEligibility(project.getId(), academicOrganisation.getId())).thenReturn(eligibility);
    }

    private void assertViewEligibilityDetails(EligibilityResource eligibility, MvcResult result, boolean expectedIsLeadPartnerOrganisation, String organisationName, boolean expectedIsUsingJesFinances, String expectedJesFilename) {

        Map<String, Object> model = result.getModelAndView().getModel();

        FinanceChecksEligibilityViewModel viewModel = (FinanceChecksEligibilityViewModel) model.get("summaryModel");

        assertEquals(expectedIsLeadPartnerOrganisation, viewModel.isLeadPartnerOrganisation());
        assertTrue(viewModel.getOrganisationName().equals(organisationName));
        assertTrue(viewModel.getProjectName().equals(project.getName()));

        assertTrue(viewModel.isEligibilityApproved());
        assertEquals(eligibility.getEligibilityRagStatus(), viewModel.getEligibilityRagStatus());
        assertEquals(eligibility.getEligibilityApprovalDate(), viewModel.getApprovalDate());
        assertEquals(eligibility.getEligibilityApprovalUserFirstName(), viewModel.getApproverFirstName());
        assertEquals(eligibility.getEligibilityApprovalUserLastName(), viewModel.getApproverLastName());

        FinanceChecksEligibilityForm form = (FinanceChecksEligibilityForm) model.get("eligibilityForm");
        assertTrue(form.isConfirmEligibilityChecked());
        assertEquals(eligibility.getEligibilityRagStatus(), form.getEligibilityRagStatus());

        assertFalse(viewModel.isExternalView());
        assertEquals(expectedIsUsingJesFinances, viewModel.isUsingJesFinances());
        if (null != viewModel.getJesFileDetails()) {
            assertEquals(expectedJesFilename, viewModel.getJesFileDetails().getFilename());
        }
    }

    @Test
    public void testConfirmEligibilityWhenConfirmEligibilityNotChecked() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.UNSET)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("confirm-eligibility", "").
                        param("confirmEligibilityChecked", "false").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check/organisation/" + organisationId + "/eligibility"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.UNSET);

    }

    @Test
    public void testConfirmEligibilityWhenConfirmEligibilityChecked() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.RED)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("confirm-eligibility", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check/organisation/" + organisationId + "/eligibility"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.RED);

    }

    @Test
    public void testConfirmEligibilityWhenSaveEligibilityReturnsFailure() throws Exception {

        when(projectFinanceService.saveEligibility(project.getId(), industrialOrganisation.getId(), EligibilityState.APPROVED, EligibilityRagStatus.RED)).
                thenReturn(serviceFailure(ELIGIBILITY_HAS_ALREADY_BEEN_APPROVED));

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", project.getId(), industrialOrganisation.getId()).
                        param("confirm-eligibility", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().isOk()).
                andExpect(view().name("project/financecheck/eligibility"));

        verify(projectFinanceService).saveEligibility(project.getId(), industrialOrganisation.getId(), EligibilityState.APPROVED, EligibilityRagStatus.RED);

    }

    @Test
    public void testConfirmEligibilitySuccess() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.GREEN)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("confirm-eligibility", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "GREEN")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check/organisation/" + organisationId + "/eligibility"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.APPROVED, EligibilityRagStatus.GREEN);

    }

    @Test
    public void testSaveAndContinueWhenConfirmEligibilityNotChecked() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.UNSET)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("save-and-continue", "").
                        param("confirmEligibilityChecked", "false").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.UNSET);

    }

    @Test
    public void testSaveAndContinueWhenConfirmEligibilityChecked() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.RED)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("save-and-continue", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.RED);

    }

    @Test
    public void testSaveAndContinueWhenSaveEligibilityReturnsFailure() throws Exception {

        when(projectFinanceService.saveEligibility(project.getId(), industrialOrganisation.getId(), EligibilityState.REVIEW, EligibilityRagStatus.RED)).
                thenReturn(serviceFailure(ELIGIBILITY_HAS_ALREADY_BEEN_APPROVED));

        EligibilityResource eligibility = new EligibilityResource(EligibilityState.APPROVED, EligibilityRagStatus.GREEN);
        setUpViewEligibilityMocking(eligibility);

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", project.getId(), industrialOrganisation.getId()).
                        param("save-and-continue", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "RED")).
                andExpect(status().isOk()).
                andExpect(view().name("project/financecheck/eligibility"));

        verify(projectFinanceService).saveEligibility(project.getId(), industrialOrganisation.getId(), EligibilityState.REVIEW, EligibilityRagStatus.RED);

    }

    @Test
    public void testSaveAndContinueSuccess() throws Exception {

        Long projectId = 1L;
        Long organisationId = 2L;

        when(projectFinanceService.saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.GREEN)).
                thenReturn(serviceSuccess());

        mockMvc.perform(
                post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("save-and-continue", "").
                        param("confirmEligibilityChecked", "true").
                        param("eligibilityRagStatus", "GREEN")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check"));

        verify(projectFinanceService).saveEligibility(projectId, organisationId, EligibilityState.REVIEW, EligibilityRagStatus.GREEN);

    }

    @Test
    public void testAjaxAddCost() throws Exception {
        Long projectId = 1L;
        Long organisationId = 2L;
        Long questionId = 3L;

        FinanceRowItem costItem = new Materials();
        when(projectFinanceFormHandler.addCostWithoutPersisting(anyLong(), anyLong(), anyLong())).thenReturn(costItem);
        mockMvc.perform(
                get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility/add_cost/{questionId}", projectId, organisationId, questionId)).
                andExpect(status().isOk());
    }

    @Test
    public void testAjaxRemoveCost() throws Exception {
        Long projectId = 1L;
        Long organisationId = 2L;
        Long costId = 3L;

        when(projectFinanceRowRestService.delete(projectId, organisationId, costId)).thenReturn(restSuccess());
        mockMvc.perform(
                get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility/remove_cost/{costId}", projectId, organisationId, costId)).
                andExpect(status().isOk());
    }

    @Test
    public void testProjectFinanceFormSubmit() throws Exception {
        Long projectId = 1L;
        Long organisationId = 2L;

        mockMvc.perform(post("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility", projectId, organisationId).
                        param("save-eligibility", "")).
                andExpect(status().is3xxRedirection()).
                andExpect(view().name("redirect:/project/" + projectId + "/finance-check/organisation/" + 2 +"/eligibility"));
    }

    @Test
    public void testEligibiltiyChanges() throws Exception {
        Long projectId = 1L;
        Long organisationId = 2L;

        mockMvc.perform(get("/project/{projectId}/finance-check/organisation/{organisationId}/eligibility/changes", projectId, organisationId)).
                andExpect(status().isOk()).
                andExpect(view().name("project/financecheck/eligibility-changes")).
                andReturn();
    }

    @Override
    protected FinanceChecksEligibilityController supplyControllerUnderTest() {
        return new FinanceChecksEligibilityController();
    }
}
