package org.innovateuk.ifs.assessment.service;

import org.innovateuk.ifs.BaseRestServiceUnitTest;
import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.commons.service.ParameterizedTypeReferences;
import org.innovateuk.ifs.interview.service.InterviewAssignmentRestServiceImpl;
import org.innovateuk.ifs.invite.builder.AssessorInviteSendResourceBuilder;
import org.innovateuk.ifs.invite.resource.*;
import org.junit.Test;

import java.util.List;

import static com.google.common.primitives.Longs.asList;
import static java.lang.String.format;
import static org.innovateuk.ifs.invite.builder.AvailableApplicationPageResourceBuilder.newAvailableApplicationPageResource;
import static org.innovateuk.ifs.invite.builder.AvailableApplicationResourceBuilder.newAvailableApplicationResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentApplicationPageResourceBuilder.newInterviewAssignmentApplicationPageResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentCreatedInviteResourceBuilder.newInterviewAssignmentStagedApplicationResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentInvitedResourceBuilder.newInterviewAssignmentApplicationResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentStagedApplicationPageResourceBuilder.newInterviewAssignmentStagedApplicationPageResource;
import static org.innovateuk.ifs.invite.builder.StagedApplicationListResourceBuilder.newStagedApplicationListResource;
import static org.innovateuk.ifs.invite.builder.StagedApplicationResourceBuilder.newStagedApplicationResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

public class InterviewAssignmentRestServiceImplTest extends BaseRestServiceUnitTest<InterviewAssignmentRestServiceImpl> {

    private static final String REST_URL = "/interview-panel";

    @Override
    protected InterviewAssignmentRestServiceImpl registerRestServiceUnderTest() {
        return new InterviewAssignmentRestServiceImpl();
    }

    @Test
    public void getAvailableApplications() {
        long competitionId = 1L;
        int page = 1;

        AvailableApplicationPageResource expected = newAvailableApplicationPageResource()
                .withContent(newAvailableApplicationResource().build(2))
                .build();

        setupGetWithRestResultExpectations(
                format("%s/%s/%s?page=1", REST_URL, "available-applications", competitionId),
                AvailableApplicationPageResource.class,
                expected
        );

        AvailableApplicationPageResource actual = service.getAvailableApplications(competitionId, page).getSuccess();
        assertEquals(expected, actual);
    }

    @Test
    public void getAvailableApplicationIds() {
        long competitionId = 1L;
        List<Long> expected = asList(1L, 2L);

        setupGetWithRestResultExpectations(
                format("%s/%s/%s", REST_URL, "available-application-ids", competitionId),
                ParameterizedTypeReferences.longsListType(),
                expected
        );

        List<Long> actual = service.getAvailableApplicationIds(competitionId).getSuccess();
        assertEquals(expected, actual);

    }

    @Test
    public void assignApplications() {
        long competitionId = 1L;

        StagedApplicationListResource stagedApplicationListResource = newStagedApplicationListResource()
                .withInvites(
                        newStagedApplicationResource()
                                .withApplicationId(1L, 2L)
                                .withCompetitionId(competitionId)
                                .build(2)
                )
                .build();

        setupPostWithRestResultExpectations(format("%s/%s", REST_URL, "assign-applications"), stagedApplicationListResource, OK);

        RestResult<Void> restResult = service.assignApplications(stagedApplicationListResource);
        assertTrue(restResult.isSuccess());
    }

    @Test
    public void getStagedApplications() {
        long competitionId = 1L;
        int page = 1;
        InterviewAssignmentStagedApplicationPageResource expected = newInterviewAssignmentStagedApplicationPageResource()
                .withContent(newInterviewAssignmentStagedApplicationResource().build(2))
                .build();

        setupGetWithRestResultExpectations(format("%s/%s/%s?page=1", REST_URL, "staged-applications", competitionId), InterviewAssignmentStagedApplicationPageResource.class, expected);

        InterviewAssignmentStagedApplicationPageResource actual = service.getStagedApplications(competitionId, page).getSuccess();
        assertEquals(expected, actual);
    }

    @Test
    public void getAssignedApplications() {
        long competitionId = 1L;
        int page = 1;
        InterviewAssignmentApplicationPageResource expected = newInterviewAssignmentApplicationPageResource()
                .withContent(newInterviewAssignmentApplicationResource().build(2))
                .build();

        setupGetWithRestResultExpectations(format("%s/%s/%s?page=1", REST_URL, "assigned-applications", competitionId), InterviewAssignmentApplicationPageResource.class, expected);

        InterviewAssignmentApplicationPageResource actual = service.getAssignedApplications(competitionId, page).getSuccess();
        assertEquals(expected, actual);
    }

    @Test
    public void unstageApplication() {
        long applicationId = 1L;

        setupPostWithRestResultExpectations(format("%s/%s/%s", REST_URL, "unstage-application", applicationId), OK);

        RestResult<Void> restResult = service.unstageApplication(applicationId);
        assertTrue(restResult.isSuccess());
    }

    @Test
    public void unstageApplications() {
        long competitionId = 1L;

        setupPostWithRestResultExpectations(format("%s/%s/%s", REST_URL, "unstage-applications", competitionId), OK);

        RestResult<Void> restResult = service.unstageApplications(competitionId);
        assertTrue(restResult.isSuccess());
    }

    @Test
    public void getEmailTemplate() {
        ApplicantInterviewInviteResource expected = new ApplicantInterviewInviteResource("Content");

        setupGetWithRestResultExpectations(format("%s/%s", REST_URL, "email-template"), ApplicantInterviewInviteResource.class, expected);

        ApplicantInterviewInviteResource actual = service.getEmailTemplate().getSuccess();
        assertEquals(expected, actual);
    }

    @Test
    public void sendAllInvites() {
        long competitionId = 1L;
        AssessorInviteSendResource sendResource = AssessorInviteSendResourceBuilder.newAssessorInviteSendResource()
                .withContent("content").withSubject("subject").build();

        setupPostWithRestResultExpectations(format("%s/%s/%s", REST_URL, "send-invites", competitionId), sendResource, OK);

        RestResult<Void> actual = service.sendAllInvites(competitionId, sendResource);
        assertTrue(actual.isSuccess());
    }
}