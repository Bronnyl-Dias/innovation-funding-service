package org.innovateuk.ifs.application.forms.controller;

import org.innovateuk.ifs.AbstractApplicationMockMVCTest;
import org.innovateuk.ifs.applicant.builder.ApplicantSectionResourceBuilder;
import org.innovateuk.ifs.applicant.resource.ApplicantResource;
import org.innovateuk.ifs.applicant.service.ApplicantRestService;
import org.innovateuk.ifs.application.finance.view.ApplicationFinanceOverviewModelManager;
import org.innovateuk.ifs.application.finance.view.DefaultFinanceFormHandler;
import org.innovateuk.ifs.application.finance.viewmodel.ApplicationFinanceOverviewViewModel;
import org.innovateuk.ifs.application.finance.viewmodel.FinanceViewModel;
import org.innovateuk.ifs.application.forms.populator.OrganisationDetailsViewModelPopulator;
import org.innovateuk.ifs.application.forms.populator.QuestionModelPopulator;
import org.innovateuk.ifs.application.forms.saver.ApplicationQuestionSaver;
import org.innovateuk.ifs.application.forms.service.ApplicationRedirectionService;
import org.innovateuk.ifs.application.overheads.OverheadFileSaver;
import org.innovateuk.ifs.application.populator.*;
import org.innovateuk.ifs.application.populator.forminput.FormInputViewModelGenerator;
import org.innovateuk.ifs.application.populator.section.YourFinancesSectionPopulator;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.application.viewmodel.section.YourFinancesSectionViewModel;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.error.ValidationMessages;
import org.innovateuk.ifs.filter.CookieFlashMessageFilter;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.Form;
import org.innovateuk.ifs.form.resource.SectionResource;
import org.innovateuk.ifs.form.resource.SectionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.innovateuk.ifs.applicant.builder.ApplicantQuestionResourceBuilder.newApplicantQuestionResource;
import static org.innovateuk.ifs.applicant.builder.ApplicantResourceBuilder.newApplicantResource;
import static org.innovateuk.ifs.applicant.builder.ApplicantSectionResourceBuilder.newApplicantSectionResource;
import static org.innovateuk.ifs.application.forms.ApplicationFormUtil.*;
import static org.innovateuk.ifs.application.service.Futures.settable;
import static org.innovateuk.ifs.commons.error.Error.fieldError;
import static org.innovateuk.ifs.commons.error.ValidationMessages.noErrors;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.form.builder.SectionResourceBuilder.newSectionResource;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(MockitoJUnitRunner.class)
@TestPropertySource(locations = "classpath:application.properties")
public class ApplicationQuestionControllerTest extends AbstractApplicationMockMVCTest<ApplicationQuestionController> {

    @Spy
    @InjectMocks
    private QuestionModelPopulator questionModelPopulator;

    @Spy
    @InjectMocks
    private OpenSectionModelPopulator openSectionModel;

    @Spy
    @InjectMocks
    private OpenApplicationFinanceSectionModelPopulator openFinanceSectionModel;

    @Spy
    @InjectMocks
    private OrganisationDetailsViewModelPopulator organisationDetailsViewModelPopulator;

    @Mock
    private ApplicationModelPopulator applicationModelPopulator;

    @Mock
    private ApplicationSectionAndQuestionModelPopulator applicationSectionAndQuestionModelPopulator;

    @Mock
    private OverheadFileSaver overheadFileSaver;

    @Mock
    private DefaultFinanceFormHandler defaultFinanceFormHandler;

    @Mock
    private FormInputViewModelGenerator formInputViewModelGenerator;

    @Mock
    private YourFinancesSectionPopulator yourFinancesSectionPopulator;

    @Mock
    private ApplicationNavigationPopulator applicationNavigationPopulator;

    @Mock
    private CookieFlashMessageFilter cookieFlashMessageFilter;

    @Mock
    private ApplicantRestService applicantRestService;

    @Spy
    @InjectMocks
    private ApplicationRedirectionService applicationRedirectionService;

    @Mock
    private ApplicationQuestionSaver applicationSaver;

    @Mock
    private ApplicationFinanceOverviewModelManager applicationFinanceOverviewModelManager;

    private ApplicationResource application;
    private Long sectionId;
    private Long questionId;
    private Long formInputId;
    private Long costId;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy");
    private ApplicantSectionResourceBuilder sectionBuilder;

    @Override
    protected ApplicationQuestionController supplyControllerUnderTest() {
        return new ApplicationQuestionController();
    }

    @Before
    @Override
    public void setUp() {

        // Process mock annotations
        super.setUp();

        this.setupCompetition();
        this.setupApplicationWithRoles();
        this.setupApplicationResponses();
        this.setupFinances();
        this.setupInvites();
        this.setupQuestionStatus(applications.get(0));

        application = applications.get(0);
        sectionId = 1L;
        questionId = 1L;
        formInputId = 111L;
        costId = 1L;

        // save actions should always succeed.
        when(formInputResponseRestService.saveQuestionResponse(anyLong(), anyLong(), anyLong(), eq(""), anyBoolean())).thenReturn(restSuccess(new ValidationMessages(fieldError("value", "", "Please enter some text 123"))));
        when(formInputResponseRestService.saveQuestionResponse(anyLong(), anyLong(), anyLong(), anyString(), anyBoolean())).thenReturn(restSuccess(noErrors()));
        when(organisationRestService.getOrganisationById(anyLong())).thenReturn(restSuccess(organisations.get(0)));
        when(overheadFileSaver.handleOverheadFileRequest(any())).thenReturn(noErrors());
        when(financeViewHandlerProvider.getFinanceFormHandler(any(), anyLong())).thenReturn(defaultFinanceFormHandler);

        ApplicantResource applicant = newApplicantResource().withProcessRole(processRoles.get(0)).withOrganisation(organisations.get(0)).build();
        when(applicantRestService.getQuestion(anyLong(), anyLong(), anyLong())).thenReturn(newApplicantQuestionResource().withApplication(application).withCompetition(competitionResource).withCurrentApplicant(applicant).withApplicants(asList(applicant)).withQuestion(questionResources.values().iterator().next()).withCurrentUser(loggedInUser).build());
        sectionBuilder =  newApplicantSectionResource().withApplication(application).withCompetition(competitionResource).withCurrentApplicant(applicant).withApplicants(asList(applicant)).withSection(newSectionResource().withType(SectionType.FINANCE).build()).withCurrentUser(loggedInUser);
        when(applicantRestService.getSection(anyLong(), anyLong(), anyLong())).thenReturn(sectionBuilder.build());
        when(formInputViewModelGenerator.fromQuestion(any(), any())).thenReturn(Collections.emptyList());
        when(formInputViewModelGenerator.fromSection(any(), any(), any(), any())).thenReturn(Collections.emptyList());
        when(yourFinancesSectionPopulator.populate(any(), any(), any(), any(), any(), any(), any())).thenReturn(new YourFinancesSectionViewModel(null, null, null, false, Optional.empty(), false));

        ApplicationFinanceOverviewViewModel financeOverviewViewModel = new ApplicationFinanceOverviewViewModel();
        when(applicationFinanceOverviewModelManager.getFinanceDetailsViewModel(competitionResource.getId(), application.getId())).thenReturn(financeOverviewViewModel);

        FinanceViewModel financeViewModel = new FinanceViewModel();
        financeViewModel.setOrganisationGrantClaimPercentage(76);

        when(defaultFinanceModelManager.getFinanceViewModel(anyLong(), anyList(), anyLong(), any(Form.class), anyLong())).thenReturn(financeViewModel);
        when(applicationSaver.saveApplicationForm(anyLong(), any(ApplicationForm.class), anyLong(), anyLong(), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Optional.class)))
                .thenReturn(new ValidationMessages());
    }

    @Test
    public void questionPage() throws Exception {
        ApplicationResource application = applications.get(0);

        when(sectionService.getAllByCompetitionId(anyLong())).thenReturn(sectionResources);
        when(applicationService.getById(application.getId())).thenReturn(application);
        when(questionService.getMarkedAsComplete(anyLong(), anyLong())).thenReturn(settable(new HashSet<>()));

        // just check if these pages are not throwing errors.
        mockMvc.perform(get("/application/1/form/question/10")).andExpect(status().isOk());
        mockMvc.perform(get("/application/1/form/question/21")).andExpect(status().isOk());
        mockMvc.perform(get("/application/1/form/question/edit/1")).andExpect(status().isOk());
        mockMvc.perform(get("/application/1/form/question/edit/1?mark_as_complete=false")).andExpect(status().isOk());
        mockMvc.perform(get("/application/1/form/question/edit/1?mark_as_complete=")).andExpect(status().isOk());

        verify(applicationSaver, never()).saveApplicationForm(anyLong(), any(ApplicationForm.class), anyLong(), anyLong(), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Optional.class));

        mockMvc.perform(get("/application/1/form/question/edit/1?mark_as_complete=true")).andExpect(status().isOk());

        verify(applicationSaver, times(1)).saveApplicationForm(anyLong(), any(ApplicationForm.class), anyLong(), anyLong(), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Optional.class));

        mockMvc.perform(get("/application/1/form/question/edit/21")).andExpect(status().isOk());
    }

    @Test
    public void questionSubmit() throws Exception {
        ApplicationResource application = applications.get(0);

        when(applicationService.getById(application.getId())).thenReturn(application);
        mockMvc.perform(
                post("/application/1/form/question/1")
                        .param("formInput[1]", "Some Value...")

        )
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void questionSubmitEdit() throws Exception {
        ApplicationResource application = applications.get(0);

        when(applicationService.getById(application.getId())).thenReturn(application);
        mockMvc.perform(
                post("/application/1/form/question/1")
                        .param(EDIT_QUESTION, "1_2")
        )
                .andExpect(view().name("application-form"));
        verify(applicationNavigationPopulator).addAppropriateBackURLToModel(any(Long.class), any(Model.class), any(SectionResource.class), any(Optional.class), any(Optional.class), any(Boolean.class));
    }

    @Test
    public void questionSubmitAssign() throws Exception {
        ApplicationResource application = applications.get(0);

        when(applicationService.getById(application.getId())).thenReturn(application);
        mockMvc.perform(
                post("/application/1/form/question/1")
                        .param(ASSIGN_QUESTION_PARAM, "1_2")

        )
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void questionSubmitMarkAsCompleteQuestion() throws Exception {
        ApplicationResource application = applications.get(0);

        when(applicationService.getById(application.getId())).thenReturn(application);
        mockMvc.perform(
                post("/application/1/form/question/1")
                        .param(MARK_AS_COMPLETE, "1")
        ).andExpect(status().is3xxRedirection());
    }

    @Test
    public void questionSubmitSaveElement() throws Exception {
        ApplicationResource application = applications.get(0);

        when(applicationService.getById(application.getId())).thenReturn(application);

        mockMvc.perform(post("/application/1/form/question/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void applicationDetailsFormSubmit_incorrectFileType() throws Exception {
        long formInputId = 2L;
        String fileError = "file error";
        MockMultipartFile file = new MockMultipartFile("formInput[" + formInputId +"]", "filename.txt", "text/plain", "someText".getBytes());

        long fileQuestionId = 31L;
        ValidationMessages validationMessages = new ValidationMessages();
        validationMessages.addError(fieldError("formInput[" + formInputId + "]", new Error(fileError, UNSUPPORTED_MEDIA_TYPE)));
        when(applicationSaver.saveApplicationForm(anyLong(), any(ApplicationForm.class), anyLong(), anyLong(), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Optional.class)))
                .thenReturn(validationMessages);

        MvcResult result = mockMvc.perform(
            fileUpload("/application/{applicationId}/form/question/{questionId}", application.getId(), fileQuestionId)
                        .file(file)
                        .param("upload_file", "")
        ).andReturn();

        BindingResult bindingResult = (BindingResult) result.getModelAndView().getModel().get("org.springframework.validation.BindingResult.form");
        assertEquals(fileError, bindingResult.getFieldError("formInput[" + formInputId + "]").getCode());
    }
}
