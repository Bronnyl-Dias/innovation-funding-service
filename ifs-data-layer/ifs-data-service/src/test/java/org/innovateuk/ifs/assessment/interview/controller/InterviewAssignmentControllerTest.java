package org.innovateuk.ifs.assessment.interview.controller;

import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.interview.controller.InterviewAssignmentController;
import org.innovateuk.ifs.invite.resource.*;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static com.google.common.primitives.Longs.asList;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.invite.builder.AvailableApplicationPageResourceBuilder.newAvailableApplicationPageResource;
import static org.innovateuk.ifs.invite.builder.AvailableApplicationResourceBuilder.newAvailableApplicationResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentCreatedInviteResourceBuilder.newInterviewAssignmentStagedApplicationResource;
import static org.innovateuk.ifs.invite.builder.InterviewAssignmentStagedApplicationPageResourceBuilder.newInterviewAssignmentStagedApplicationPageResource;
import static org.innovateuk.ifs.invite.builder.StagedApplicationListResourceBuilder.newStagedApplicationListResource;
import static org.innovateuk.ifs.invite.builder.StagedApplicationResourceBuilder.newStagedApplicationResource;
import static org.innovateuk.ifs.util.JsonMappingUtil.toJson;
import static org.mockito.Mockito.*;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InterviewAssignmentControllerTest extends BaseControllerMockMVCTest<InterviewAssignmentController> {

    private static final long COMPETITION_ID = 1L;

    @Override
    protected InterviewAssignmentController supplyControllerUnderTest() {
        return new InterviewAssignmentController();
    }

    @Test
    public void getAvailableApplications() throws Exception {
        int page = 5;
        int pageSize = 30;

        List<AvailableApplicationResource> expectedAvailableApplicationResources = newAvailableApplicationResource().build(2);

        AvailableApplicationPageResource expectedAvailableApplications = newAvailableApplicationPageResource()
                .withContent(expectedAvailableApplicationResources)
                .withNumber(page)
                .withTotalElements(300L)
                .withTotalPages(10)
                .withSize(30)
                .build();

        Pageable pageable = new PageRequest(page, pageSize, new Sort(ASC, "id"));

        when(interviewAssignmentInviteServiceMock.getAvailableApplications(COMPETITION_ID, pageable))
                .thenReturn(serviceSuccess(expectedAvailableApplications));

        mockMvc.perform(get("/interview-panel/available-applications/{competition-id}", COMPETITION_ID)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(pageSize))
                .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(expectedAvailableApplications)));

        verify(interviewAssignmentInviteServiceMock, only()).getAvailableApplications(COMPETITION_ID, pageable);
    }

    @Test
    public void getStagedApplications() throws Exception {
        int page = 5;
        int pageSize = 30;

        List<InterviewAssignmentStagedApplicationResource> expectedStagedApplicationResources = newInterviewAssignmentStagedApplicationResource().build(2);

        InterviewAssignmentStagedApplicationPageResource expectedStagedApplications = newInterviewAssignmentStagedApplicationPageResource()
                .withContent(expectedStagedApplicationResources)
                .withNumber(page)
                .withTotalElements(300L)
                .withTotalPages(10)
                .withSize(30)
                .build();

        Pageable pageable = new PageRequest(page, pageSize, new Sort(ASC, "id"));

        when(interviewAssignmentInviteServiceMock.getStagedApplications(COMPETITION_ID, pageable))
                .thenReturn(serviceSuccess(expectedStagedApplications));

        mockMvc.perform(get("/interview-panel/staged-applications/{competition-id}", COMPETITION_ID)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(pageSize))
                .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(expectedStagedApplications)));

        verify(interviewAssignmentInviteServiceMock, only()).getStagedApplications(COMPETITION_ID, pageable);

    }

    @Test
    public void getAvailableApplicationIds() throws Exception {
        List<Long> expectedAvailableApplicationIds = asList(1L, 2L);

        when(interviewAssignmentInviteServiceMock.getAvailableApplicationIds(COMPETITION_ID))
                .thenReturn(serviceSuccess(expectedAvailableApplicationIds));

        mockMvc.perform(get("/interview-panel/available-application-ids/{competitionId}", COMPETITION_ID))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(expectedAvailableApplicationIds)));

        verify(interviewAssignmentInviteServiceMock, only()).getAvailableApplicationIds(COMPETITION_ID);

    }

    @Test
    public void assignApplications() throws Exception {
        StagedApplicationListResource applications = newStagedApplicationListResource()
                .withInvites(newStagedApplicationResource().build(2))
                .build();

        when(interviewAssignmentInviteServiceMock.assignApplications(applications.getInvites())).thenReturn(serviceSuccess());

        mockMvc.perform(post("/interview-panel/assign-applications")
                .contentType(APPLICATION_JSON)
                .content(toJson(applications)))
                .andExpect(status().isOk());

        verify(interviewAssignmentInviteServiceMock, only()).assignApplications(applications.getInvites());
    }

    @Test
    public void unstageApplication() throws Exception {
        long applicationId = 1L;

        when(interviewAssignmentInviteServiceMock.unstageApplication(applicationId)).thenReturn(serviceSuccess());

        mockMvc.perform(post("/interview-panel/unstage-application/{applicationId}", applicationId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(interviewAssignmentInviteServiceMock, only()).unstageApplication(applicationId);
    }

    @Test
    public void unstageApplications() throws Exception {
        long competitionId = 1L;
        when(interviewAssignmentInviteServiceMock.unstageApplications(competitionId)).thenReturn(serviceSuccess());

        mockMvc.perform(post("/interview-panel/unstage-applications/{competitionId}", competitionId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(interviewAssignmentInviteServiceMock, only()).unstageApplications(competitionId);
    }

    @Test
    public void getEmailTemplate() throws Exception {
        ApplicantInterviewInviteResource interviewInviteResource = new ApplicantInterviewInviteResource("content");

        when(interviewAssignmentInviteServiceMock.getEmailTemplate()).thenReturn(serviceSuccess(interviewInviteResource));

        mockMvc.perform(get("/interview-panel/email-template")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(interviewInviteResource)));

        verify(interviewAssignmentInviteServiceMock, only()).getEmailTemplate();
    }

    @Test
    public void sendInvites() throws Exception {
        long competitionId = 1L;
        AssessorInviteSendResource sendResource = new AssessorInviteSendResource("Subject", "Content");

        when(interviewAssignmentInviteServiceMock.sendInvites(competitionId, sendResource)).thenReturn(serviceSuccess());

        mockMvc.perform(post("/interview-panel/send-invites/{competitionId}", competitionId)
                .contentType(APPLICATION_JSON)
                .content(toJson(sendResource)))
                .andExpect(status().isOk());

        verify(interviewAssignmentInviteServiceMock, only()).sendInvites(competitionId, sendResource);
    }

    @Test
    public void isApplicationAssigned() throws Exception {
        long applicationId = 1L;
        when(interviewAssignmentInviteServiceMock.isApplicationAssigned(applicationId)).thenReturn(serviceSuccess(true));

        mockMvc.perform(get("/interview-panel/is-assigned/{applicationId}", applicationId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(interviewAssignmentInviteServiceMock, only()).isApplicationAssigned(applicationId);
    }

}