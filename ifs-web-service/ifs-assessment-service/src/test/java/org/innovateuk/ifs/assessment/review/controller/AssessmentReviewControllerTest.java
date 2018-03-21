package org.innovateuk.ifs.assessment.review.controller;

import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.assessment.review.populator.AssessmentReviewModelPopulator;
import org.innovateuk.ifs.assessment.review.viewmodel.AssessmentReviewViewModel;
import org.innovateuk.ifs.review.resource.ReviewRejectOutcomeResource;
import org.innovateuk.ifs.review.resource.ReviewResource;
import org.innovateuk.ifs.user.resource.OrganisationResource;
import org.innovateuk.ifs.user.resource.ProcessRoleResource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingLong;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.id;
import static org.innovateuk.ifs.commons.rest.RestResult.restSuccess;
import static org.innovateuk.ifs.application.builder.FormInputResponseResourceBuilder.newFormInputResponseResource;
import static org.innovateuk.ifs.review.builder.ReviewRejectOutcomeResourceBuilder.newReviewRejectOutcomeResource;
import static org.innovateuk.ifs.review.builder.ReviewResourceBuilder.newReviewResource;
import static org.innovateuk.ifs.user.builder.OrganisationResourceBuilder.newOrganisationResource;
import static org.innovateuk.ifs.user.builder.ProcessRoleResourceBuilder.newProcessRoleResource;
import static org.innovateuk.ifs.user.resource.UserRoleType.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AssessmentReviewControllerTest extends BaseControllerMockMVCTest<AssessmentReviewController> {

    private static final long APPLICATION_ID = 2L;
    private static final long COMPETITION_ID = 3L;
    private static final long REVIEW_ID = 4L;
    private static final String APPLICATION_NAME = "Application name";
    private static final String PROJECT_SUMMARY_QUESTION_NAME = "Project summary";
    private static final String PROJECT_SUMMARY_TEXT = "Project summary text";

    private SortedSet<OrganisationResource> partners;

    private OrganisationResource leadOrganisation;

    private OrganisationResource collaboratorOrganisation1;
    private OrganisationResource collaboratorOrganisation2;

    @Spy
    @InjectMocks
    private AssessmentReviewModelPopulator assessmentReviewModelPopulator;

    @Override
    protected AssessmentReviewController supplyControllerUnderTest() {
        return new AssessmentReviewController();
    }

    @Before
    public void setup() {
        super.setup();

        when(formInputResponseRestService.getByApplicationIdAndQuestionName(APPLICATION_ID, PROJECT_SUMMARY_QUESTION_NAME))
                .thenReturn(restSuccess(newFormInputResponseResource()
                        .withValue(PROJECT_SUMMARY_TEXT)
                        .build()));

        collaboratorOrganisation1 = newOrganisationResource().build();
        collaboratorOrganisation2 = newOrganisationResource().build();
        leadOrganisation = newOrganisationResource().build();

        OrganisationResource otherOrganisation = newOrganisationResource().build();

        List<ProcessRoleResource> processRoleResources = newProcessRoleResource()
                .withOrganisation(collaboratorOrganisation1.getId(),
                        leadOrganisation.getId(),
                        collaboratorOrganisation2.getId(),
                        otherOrganisation.getId())
                .withRoleName(COLLABORATOR.getName(), LEADAPPLICANT.getName(), COLLABORATOR.getName(), ASSESSOR.getName())
                .build(4);

        when(processRoleService.findProcessRolesByApplicationId(APPLICATION_ID)).thenReturn(processRoleResources);
        when(organisationRestService.getOrganisationById(collaboratorOrganisation1.getId())).thenReturn(restSuccess(collaboratorOrganisation1));
        when(organisationRestService.getOrganisationById(collaboratorOrganisation2.getId())).thenReturn(restSuccess(collaboratorOrganisation2));
        when(organisationRestService.getOrganisationById(leadOrganisation.getId())).thenReturn(restSuccess(leadOrganisation));

        partners = new TreeSet<>(comparingLong(OrganisationResource::getId));
        partners.add(collaboratorOrganisation1);
        partners.add(leadOrganisation);
        partners.add(collaboratorOrganisation2);
    }

    @Test
    public void viewAssignment() throws Exception {

        AssessmentReviewViewModel expectedReviewViewModel = new AssessmentReviewViewModel(
                REVIEW_ID,
                COMPETITION_ID,
                APPLICATION_NAME,
                partners,
                leadOrganisation,
                PROJECT_SUMMARY_TEXT
        );

        ReviewResource reviewResource = newReviewResource()
                .withId(REVIEW_ID)
                .withApplication(APPLICATION_ID)
                .withApplicationName(APPLICATION_NAME)
                .withCompetition(COMPETITION_ID)
                .build();

        when(reviewRestService.getAssessmentReview(REVIEW_ID)).thenReturn(restSuccess(reviewResource));

        mockMvc.perform(get("/review/{reviewId}", REVIEW_ID))
                .andExpect(status().isOk())
                .andExpect(model().attribute("model", expectedReviewViewModel))
                .andExpect(view().name("assessment/review-invitation")).andReturn();

        InOrder inOrder = inOrder(reviewRestService, formInputResponseRestService, processRoleService, organisationRestService);
        inOrder.verify(reviewRestService).getAssessmentReview(REVIEW_ID);
        inOrder.verify(formInputResponseRestService).getByApplicationIdAndQuestionName(APPLICATION_ID, PROJECT_SUMMARY_QUESTION_NAME);
        inOrder.verify(processRoleService).findProcessRolesByApplicationId(APPLICATION_ID);
        asList(collaboratorOrganisation1, collaboratorOrganisation2, leadOrganisation).forEach(organisationResource ->
                inOrder.verify(organisationRestService).getOrganisationById(organisationResource.getId()));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void respondToAssignment_accept() throws Exception {
        final boolean accept = true;

        ReviewResource reviewResource = newReviewResource()
                .with(id(REVIEW_ID))
                .withApplication(APPLICATION_ID)
                .withCompetition(COMPETITION_ID)
                .build();

        when(reviewRestService.getAssessmentReview(REVIEW_ID)).thenReturn(restSuccess(reviewResource));
        when(reviewRestService.acceptAssessmentReview(REVIEW_ID)).thenReturn(restSuccess());

        mockMvc.perform(post("/review/{reviewId}/respond", REVIEW_ID)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("reviewAccept", String.valueOf(accept)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(format("/assessor/dashboard/competition/%d/panel", COMPETITION_ID)));

        InOrder inOrder = inOrder(reviewRestService);
        inOrder.verify(reviewRestService).getAssessmentReview(REVIEW_ID);
        inOrder.verify(reviewRestService).acceptAssessmentReview(REVIEW_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void respondToAssignment_reject() throws Exception {
        final boolean accept = false;
        String comment = "comment";

        ReviewResource reviewResource = newReviewResource()
                .with(id(REVIEW_ID))
                .withApplication(APPLICATION_ID)
                .withCompetition(COMPETITION_ID)
                .build();

        ReviewRejectOutcomeResource rejectOutcomeResource = newReviewRejectOutcomeResource()
                .withReason(comment)
                .build();

        when(reviewRestService.getAssessmentReview(REVIEW_ID)).thenReturn(restSuccess(reviewResource));
        when(reviewRestService.rejectAssessmentReview(REVIEW_ID, rejectOutcomeResource)).thenReturn(restSuccess());

        mockMvc.perform(post("/review/{reviewId}/respond", REVIEW_ID)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("reviewAccept", String.valueOf(accept))
                .param("rejectComment", comment))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(format("/assessor/dashboard/competition/%d/panel", COMPETITION_ID)));

        InOrder inOrder = inOrder(reviewRestService);
        inOrder.verify(reviewRestService).getAssessmentReview(REVIEW_ID);
        inOrder.verify(reviewRestService).rejectAssessmentReview(REVIEW_ID, rejectOutcomeResource);
        inOrder.verifyNoMoreInteractions();
    }
}