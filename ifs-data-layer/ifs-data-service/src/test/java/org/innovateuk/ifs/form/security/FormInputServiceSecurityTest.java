package org.innovateuk.ifs.form.security;

import org.innovateuk.ifs.BaseServiceSecurityTest;
import org.innovateuk.ifs.form.resource.FormInputResponseCommand;
import org.innovateuk.ifs.form.resource.FormInputResponseResource;
import org.innovateuk.ifs.form.transactional.FormInputService;
import org.innovateuk.ifs.form.transactional.FormInputServiceImpl;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;

import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.form.builder.FormInputResponseResourceBuilder.newFormInputResponseResource;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * Testing how the secured methods in {@link FormInputService} interact with Spring Security
 */
public class FormInputServiceSecurityTest extends BaseServiceSecurityTest<FormInputService> {

    private static final int ARRAY_SIZE_FOR_POST_FILTER_TESTS = 2;

    private FormInputResponsePermissionRules formInputResponsePermissionRules;

    @Before
    public void lookupPermissionRules() {
        formInputResponsePermissionRules = getMockPermissionRulesBean(FormInputResponsePermissionRules.class);
    }

    @Test
    public void testFindResponsesByApplication() {
        long applicationId = 1L;

        when(classUnderTestMock.findResponsesByApplication(applicationId))
                .thenReturn(serviceSuccess(newFormInputResponseResource().build(ARRAY_SIZE_FOR_POST_FILTER_TESTS)));

        classUnderTest.findResponsesByApplication(applicationId);

        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForApplicationWhenSharedBetweenOrganisations(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .assessorCanSeeTheInputResponsesInApplicationsTheyAssess(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .internalUserCanSeeFormInputResponsesForApplications(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForTheirOrganisationAndApplication(isA(FormInputResponseResource.class), isA(UserResource.class));
    }

    @Test
    public void testFindResponsesByFormInputIdAndApplicationId() {
        long applicationId = 1L;
        long formInputResponseId = 2L;

        when(classUnderTestMock.findResponsesByFormInputIdAndApplicationId(applicationId, formInputResponseId))
                .thenReturn(serviceSuccess(newFormInputResponseResource().build(ARRAY_SIZE_FOR_POST_FILTER_TESTS)));

        classUnderTest.findResponsesByFormInputIdAndApplicationId(applicationId, formInputResponseId);

        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForApplicationWhenSharedBetweenOrganisations(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .assessorCanSeeTheInputResponsesInApplicationsTheyAssess(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .internalUserCanSeeFormInputResponsesForApplications(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForTheirOrganisationAndApplication(isA(FormInputResponseResource.class), isA(UserResource.class));
    }


    @Test
    public void testSaveQuestionResponse() {
        final long applicationId = 1L;
        final long formInputId = 2L;
        final long userId = 3L;

        final FormInputResponseCommand formInputResponseCommand = new FormInputResponseCommand(formInputId, applicationId, userId, "test text");

        assertAccessDenied(
                () -> classUnderTest.saveQuestionResponse(formInputResponseCommand),
                () -> verify(formInputResponsePermissionRules)
                        .aConsortiumMemberCanUpdateAFormInputResponse(isA(FormInputResponseCommand.class), isA(UserResource.class))
        );
    }

    @Test
    public void findResponseByApplicationIdAndQuestionName() {
        when(classUnderTestMock.findResponseByApplicationIdAndQuestionName(1L, "name"))
                .thenReturn(serviceSuccess(newFormInputResponseResource().build()));

        assertAccessDenied(
                () -> classUnderTest.findResponseByApplicationIdAndQuestionName(1L, "name"),
                () -> {
                    verify(formInputResponsePermissionRules)
                            .consortiumCanSeeTheInputResponsesForApplicationWhenSharedBetweenOrganisations(
                                    isA(FormInputResponseResource.class), isA(UserResource.class));
                    verify(formInputResponsePermissionRules)
                            .assessorCanSeeTheInputResponsesInApplicationsTheyAssess(
                                    isA(FormInputResponseResource.class), isA(UserResource.class));
                    verify(formInputResponsePermissionRules)
                            .internalUserCanSeeFormInputResponsesForApplications(
                                    isA(FormInputResponseResource.class), isA(UserResource.class));
                    verify(formInputResponsePermissionRules)
                            .consortiumCanSeeTheInputResponsesForTheirOrganisationAndApplication(
                                    isA(FormInputResponseResource.class), isA(UserResource.class));
                });
    }

    @Test
    public void findResponseByApplicationIdAndQuestionId() {
        when(classUnderTestMock.findResponseByApplicationIdAndQuestionId(1L, 2L))
                .thenReturn(serviceSuccess(newFormInputResponseResource().build(ARRAY_SIZE_FOR_POST_FILTER_TESTS)));

        classUnderTest.findResponseByApplicationIdAndQuestionId(1L, 2L);

        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForApplicationWhenSharedBetweenOrganisations(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .assessorCanSeeTheInputResponsesInApplicationsTheyAssess(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .internalUserCanSeeFormInputResponsesForApplications(isA(FormInputResponseResource.class), isA(UserResource.class));
        verify(formInputResponsePermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS))
                .consortiumCanSeeTheInputResponsesForTheirOrganisationAndApplication(isA(FormInputResponseResource.class), isA(UserResource.class));
    }

    @Override
    protected Class<? extends FormInputService> getClassUnderTest() {
        return FormInputServiceImpl.class;
    }
}


