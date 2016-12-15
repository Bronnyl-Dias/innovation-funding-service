package org.innovateuk.ifs.assessment.documentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.innovateuk.ifs.BaseControllerMockMVCTest;
import org.innovateuk.ifs.assessment.controller.CompetitionInviteController;
import org.innovateuk.ifs.invite.resource.*;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;

import java.util.List;

import static java.util.Optional.ofNullable;
import static org.innovateuk.ifs.assessment.builder.CompetitionInviteResourceBuilder.newCompetitionInviteResource;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.documentation.AssessorCreatedInviteResourceDocs.assessorCreatedInviteResourceFields;
import static org.innovateuk.ifs.documentation.AssessorInviteOverviewResourceDocs.assessorInviteOverviewResourceFields;
import static org.innovateuk.ifs.documentation.AvailableAssessorResourceDocs.availableAssessorResourceFields;
import static org.innovateuk.ifs.documentation.CompetitionInviteDocs.*;
import static org.innovateuk.ifs.invite.builder.AssessorCreatedInviteResourceBuilder.newAssessorCreatedInviteResource;
import static org.innovateuk.ifs.invite.builder.AssessorInviteOverviewResourceBuilder.newAssessorInviteOverviewResource;
import static org.innovateuk.ifs.invite.builder.AvailableAssessorResourceBuilder.newAvailableAssessorResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.util.JsonMappingUtil.toJson;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CompetitionInviteControllerDocumentation extends BaseControllerMockMVCTest<CompetitionInviteController> {

    private RestDocumentationResultHandler document;

    @Override
    protected CompetitionInviteController supplyControllerUnderTest() {
        return new CompetitionInviteController();
    }

    @Before
    public void setup() {
        this.document = document("competitioninvite/{method-name}",
                preprocessResponse(prettyPrint()));
    }

    @Test
    public void getInvite() throws Exception {
        String hash = "invitehash";
        CompetitionInviteResource competitionInviteResource = competitionInviteResourceBuilder.build();

        when(competitionInviteServiceMock.getInvite(hash)).thenReturn(serviceSuccess(competitionInviteResource));

        mockMvc.perform(get("/competitioninvite/getInvite/{hash}", hash))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("hash").description("hash of the invite being requested")
                        ),
                        responseFields(competitionInviteFields)
                ));
    }

    @Test
    public void openInvite() throws Exception {
        String hash = "invitehash";
        CompetitionInviteResource competitionInviteResource = competitionInviteResourceBuilder.build();

        when(competitionInviteServiceMock.openInvite(hash)).thenReturn(serviceSuccess(competitionInviteResource));

        mockMvc.perform(post("/competitioninvite/openInvite/{hash}", hash))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("hash").description("hash of the invite being opened")
                        ),
                        responseFields(competitionInviteFields)
                ));
    }

    @Test
    public void acceptInvite() throws Exception {
        String hash = "invitehash";
        UserResource user = newUserResource().build();

        login(user);

        when(competitionInviteServiceMock.acceptInvite(hash, user)).thenReturn(serviceSuccess());

        mockMvc.perform(post("/competitioninvite/acceptInvite/{hash}", hash))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("hash").description("hash of the invite being accepted")
                        )
                ));
    }

    @Test
    public void rejectInvite() throws Exception {
        String hash = "invitehash";
        CompetitionRejectionResource compRejection = competitionInviteResource;

        when(competitionInviteServiceMock.rejectInvite(hash, compRejection.getRejectReason(), ofNullable(compRejection.getRejectComment()))).thenReturn(serviceSuccess());

        mockMvc.perform(post("/competitioninvite/rejectInvite/{hash}", hash)
                .contentType(APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(compRejection)))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        requestFields(competitionRejectionFields),
                        pathParameters(
                                parameterWithName("hash").description("hash of the invite being rejected")
                        )
                ));
    }

    @Test
    public void checkExistingUser() throws Exception {
        String hash = "invitehash";

        when(competitionInviteServiceMock.checkExistingUser(hash)).thenReturn(serviceSuccess(Boolean.TRUE));

        mockMvc.perform(get("/competitioninvite/checkExistingUser/{hash}", hash))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("hash").description("hash of the invite being checked")
                        )
                ));
    }

    @Test
    public void getAvailableAssessors() throws Exception {
        long competitionId = 1L;
        List<AvailableAssessorResource> expectedAvailableAssessorResources = newAvailableAssessorResource().build(2);

        when(competitionInviteServiceMock.getAvailableAssessors(competitionId)).thenReturn(serviceSuccess(expectedAvailableAssessorResources));

        mockMvc.perform(get("/competitioninvite/getAvailableAssessors/{competitionId}", competitionId))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("competitionId").description("Id of the competition")
                        ),
                        responseFields(
                                fieldWithPath("[]").description("List of available assessors for the competition")
                        ).andWithPrefix("[].", availableAssessorResourceFields)
                ));

        verify(competitionInviteServiceMock, only()).getAvailableAssessors(competitionId);
    }

    @Test
    public void getCreatedInvites() throws Exception {
        long competitionId = 1L;
        List<AssessorCreatedInviteResource> expectedAssessorCreatedInviteResources = newAssessorCreatedInviteResource().build(2);

        when(competitionInviteServiceMock.getCreatedInvites(competitionId)).thenReturn(serviceSuccess(expectedAssessorCreatedInviteResources));

        mockMvc.perform(get("/competitioninvite/getCreatedInvites/{competitionId}", 1L))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("competitionId").description("Id of the competition")
                        ),
                        responseFields(
                                fieldWithPath("[]").description("List of the created assessor invites for the competition")
                        ).andWithPrefix("[].", assessorCreatedInviteResourceFields)
                ));

        verify(competitionInviteServiceMock, only()).getCreatedInvites(competitionId);
    }

    @Test
    public void getInvitationOverview() throws Exception {
        long competitionId = 1L;
        List<AssessorInviteOverviewResource> expectedAssessorInviteOverviewResources = newAssessorInviteOverviewResource().build(2);

        when(competitionInviteServiceMock.getInvitationOverview(competitionId)).thenReturn(serviceSuccess(expectedAssessorInviteOverviewResources));

        mockMvc.perform(get("/competitioninvite/getInvitationOverview/{competitionId}", 1L))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        pathParameters(
                                parameterWithName("competitionId").description("Id of the competition")
                        ),
                        responseFields(
                                fieldWithPath("[]").description("List of overviews representing each of the assessor invites for the competition")
                        ).andWithPrefix("[].", assessorInviteOverviewResourceFields)
                ));

        verify(competitionInviteServiceMock, only()).getInvitationOverview(competitionId);
    }

    @Test
    public void inviteUser() throws Exception {
        ExistingUserStagedInviteResource existingUserStagedInviteResource = new ExistingUserStagedInviteResource("firstname.lastname@example.com", 1L);

        when(competitionInviteServiceMock.inviteUser(existingUserStagedInviteResource)).thenReturn(serviceSuccess(competitionInviteResourceBuilder.build()));

        mockMvc.perform(post("/competitioninvite/inviteUser")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(existingUserStagedInviteResource)))
                .andExpect(status().isOk())
                .andDo(this.document.snippets(
                        responseFields(competitionInviteFields)
                ));

        verify(competitionInviteServiceMock, only()).inviteUser(existingUserStagedInviteResource);
    }

    @Test
    public void deleteInvite() throws Exception {
        String email = "firstname.lastname@email.com";
        long competitionId = 1L;

        when(competitionInviteServiceMock.deleteInvite(email, competitionId)).thenReturn(serviceSuccess());

        mockMvc.perform(delete("/competitioninvite/deleteInvite")
                .param("email", email)
                .param("competitionId", String.valueOf(competitionId)))
                .andExpect(status().isNoContent())
                .andDo(this.document.snippets(
                        requestParameters(
                                parameterWithName("email").description("Email address of the invite"),
                                parameterWithName("competitionId").description("Id of the competition")
                        )
                ));

        verify(competitionInviteServiceMock, only()).deleteInvite(email, competitionId);
    }
}
