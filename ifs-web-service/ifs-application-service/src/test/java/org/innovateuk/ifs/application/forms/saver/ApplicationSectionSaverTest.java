package org.innovateuk.ifs.application.forms.saver;

import org.innovateuk.ifs.application.finance.view.DefaultFinanceFormHandler;
import org.innovateuk.ifs.application.finance.view.FinanceFormHandler;
import org.innovateuk.ifs.application.finance.view.FinanceViewHandlerProvider;
import org.innovateuk.ifs.application.overheads.OverheadFileSaver;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.application.service.QuestionService;
import org.innovateuk.ifs.application.service.SectionService;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.error.ValidationMessages;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.service.CompetitionRestService;
import org.innovateuk.ifs.filter.CookieFlashMessageFilter;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.resource.SectionResource;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.service.OrganisationService;
import org.innovateuk.ifs.user.service.UserRestService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.application.forms.ApplicationFormUtil.*;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder.newCompetitionResource;
import static org.innovateuk.ifs.form.builder.SectionResourceBuilder.newSectionResource;
import static org.innovateuk.ifs.user.builder.ProcessRoleResourceBuilder.newProcessRoleResource;
import static org.innovateuk.ifs.util.MapFunctions.asMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Tests {@link ApplicationSectionSaver}
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationSectionSaverTest {

    @InjectMocks
    private ApplicationSectionSaver sectionSaver;

    @Mock
    private OrganisationService organisationService;

    @Mock
    private FinanceViewHandlerProvider financeViewHandlerProvider;

    @Mock
    private UserRestService userRestService;

    @Mock
    private SectionService sectionService;

    @Mock
    private QuestionService questionService;

    @Mock
    private CookieFlashMessageFilter cookieFlashMessageFilter;

    @Mock
    private OverheadFileSaver overheadFileSaver;

    @Mock
    private ApplicationSectionFinanceSaver financeSaver;

    @Mock
    private ApplicationQuestionFileSaver fileSaver;

    @Mock
    private ApplicationQuestionNonFileSaver nonFileSaver;

    @Mock
    private CompetitionRestService competitionRestService;

    private final ApplicationResource application = newApplicationResource().withId(1234L).build();
    private final CompetitionResource competition = newCompetitionResource().withIncludeJesForm(true).build();
    private final Long competitionId = 23412L;
    private final ApplicationForm form = new ApplicationForm();
    private final Long sectionId = 912509L;
    private final Long userId = 812482L;
    private final Long processRoleId = 2312412L;
    private final Boolean validFinanceTerms = false;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final SectionResource section = newSectionResource().withId(sectionId).build();


    @Before
    public void setup() {
        when(userRestService.findProcessRole(userId, application.getId())).thenReturn(restSuccess(newProcessRoleResource().withId(processRoleId).build()));
        when(sectionService.getById(sectionId)).thenReturn(section);

        when(organisationService.getOrganisationType(userId, application.getId())).thenReturn(OrganisationTypeEnum.BUSINESS.getId());
        FinanceFormHandler defaultFinanceFormHandler = mock(DefaultFinanceFormHandler.class);
        when(defaultFinanceFormHandler.update(request, userId, application.getId(), competitionId)).thenReturn(new ValidationMessages());
        when(competitionRestService.getCompetitionById(competitionId)).thenReturn(restSuccess(competition));
        when(financeViewHandlerProvider.getFinanceFormHandler(competition, OrganisationTypeEnum.BUSINESS.getId())).thenReturn(defaultFinanceFormHandler);
        when(overheadFileSaver.isOverheadFileRequest(request)).thenReturn(false);
    }

    @Test
    public void saveApplicationForm_SaveSection() {
        Map<String, String[]> params = asMap();
        when(request.getParameterMap()).thenReturn(params);

        ValidationMessages result = sectionSaver.saveApplicationForm(application, competitionId, form, sectionId, userId, request, response, validFinanceTerms);

        assertFalse(result.hasErrors());
        verify(organisationService, times(1)).getOrganisationType(userId, application.getId());
    }

    @Test
    public void saveApplicationForm_SaveSection_OverHeadFileRequest() {
        Map<String, String[]> params = asMap();
        when(request.getParameterMap()).thenReturn(params);
        when(overheadFileSaver.isOverheadFileRequest(request)).thenReturn(true);
        when(overheadFileSaver.handleOverheadFileRequest(request)).thenReturn(new ValidationMessages());

        ValidationMessages result = sectionSaver.saveApplicationForm(application, competitionId, form, sectionId, userId, request, response, validFinanceTerms);

        assertFalse(result.hasErrors());
        verify(overheadFileSaver, times(1)).handleOverheadFileRequest(request);
    }

    @Test
    public void saveApplicationForm_SaveSection_OverHeadFileRequest_withErrors() {
        Map<String, String[]> params = asMap();
        when(request.getParameterMap()).thenReturn(params);
        when(overheadFileSaver.isOverheadFileRequest(request)).thenReturn(true);

        ValidationMessages messages = new ValidationMessages();
        messages.addError(new Error("Some error", BAD_REQUEST));
        when(overheadFileSaver.handleOverheadFileRequest(request)).thenReturn(messages);

        ValidationMessages result = sectionSaver.saveApplicationForm(application, competitionId, form, sectionId, userId, request, response, validFinanceTerms);

        assertTrue(result.hasErrors());
        verify(overheadFileSaver, times(1)).handleOverheadFileRequest(request);
    }

    @Test
    public void saveApplicationForm_MarkAsComplete() {
        Map<String, String[]> params = asMap(MARK_SECTION_AS_COMPLETE, new String[]{});
        when(request.getParameterMap()).thenReturn(params);

        ValidationMessages result = sectionSaver.saveApplicationForm(application, competitionId, form, sectionId, userId, request, response, validFinanceTerms);

        assertFalse(result.hasErrors());
        verify(financeSaver, times(1)).handleStateAid(params, application, form, section);
    }

    @Test
    public void saveApplicationForm_MarkAsIncomplete() {
        Map<String, String[]> params = asMap(MARK_SECTION_AS_INCOMPLETE, new String[]{});
        when(request.getParameterMap()).thenReturn(params);

        ValidationMessages result = sectionSaver.saveApplicationForm(application, competitionId, form, sectionId, userId, request, response, validFinanceTerms);

        assertFalse(result.hasErrors());
        verify(financeSaver, times(1)).handleStateAid(params, application, form, section);
    }
}
