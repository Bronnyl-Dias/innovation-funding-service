package org.innovateuk.ifs.assessment.transactional;

import org.innovateuk.ifs.BaseUnitTestMocksTest;
import org.innovateuk.ifs.assessment.domain.Assessment;
import org.innovateuk.ifs.assessment.domain.AssessmentParticipant;
import org.innovateuk.ifs.assessment.mapper.AssessmentParticipantMapper;
import org.innovateuk.ifs.assessment.repository.AssessmentParticipantRepository;
import org.innovateuk.ifs.assessment.repository.AssessmentRepository;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.mapper.CompetitionParticipantRoleMapper;
import org.innovateuk.ifs.invite.resource.CompetitionParticipantResource;
import org.innovateuk.ifs.invite.resource.CompetitionParticipantRoleResource;
import org.innovateuk.ifs.invite.resource.ParticipantStatusResource;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.innovateuk.ifs.assessment.builder.AssessmentBuilder.newAssessment;
import static org.innovateuk.ifs.assessment.builder.AssessmentParticipantBuilder.newAssessmentParticipant;
import static org.innovateuk.ifs.assessment.resource.AssessmentState.OPEN;
import static org.innovateuk.ifs.assessment.resource.AssessmentState.*;
import static org.innovateuk.ifs.competition.domain.CompetitionParticipantRole.ASSESSOR;
import static org.innovateuk.ifs.competition.resource.CompetitionStatus.*;
import static org.innovateuk.ifs.invite.builder.CompetitionParticipantResourceBuilder.newCompetitionParticipantResource;
import static org.innovateuk.ifs.invite.resource.ParticipantStatusResource.ACCEPTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

public class CompetitionParticipantServiceImplTest extends BaseUnitTestMocksTest {

    @Mock
    private AssessmentRepository assessmentRepositoryMock;

    @Mock
    private AssessmentParticipantRepository assessmentParticipantRepositoryMock;

    @Mock
    private AssessmentParticipantMapper assessmentParticipantMapperMock;

    @Mock
    private CompetitionParticipantRoleMapper competitionParticipantRoleMapperMock;

    @InjectMocks
    private CompetitionParticipantService competitionParticipantService = new CompetitionParticipantServiceImpl();

    @Test
    public void getCompetitionParticipants_withPending() {
        Long assessorId = 1L;
        Long userId = 1L;
        Long competitionId = 2L;

        List<AssessmentParticipant> competitionParticipants = new ArrayList<>();
        AssessmentParticipant competitionParticipant = new AssessmentParticipant();
        competitionParticipants.add(competitionParticipant);

        CompetitionParticipantResource expected = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withStatus(ParticipantStatusResource.PENDING)
                .withCompetitionStatus(IN_ASSESSMENT)
                .build();

        when(assessmentParticipantRepositoryMock.getByUserIdAndRole(userId, ASSESSOR)).thenReturn(competitionParticipants);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant))).thenReturn(expected);
        when(competitionParticipantRoleMapperMock.mapToDomain(CompetitionParticipantRoleResource.ASSESSOR)).thenReturn(ASSESSOR);

        ServiceResult<List<CompetitionParticipantResource>> competitionParticipantServiceResult =
                competitionParticipantService.getCompetitionParticipants(assessorId, CompetitionParticipantRoleResource.ASSESSOR);

        List<CompetitionParticipantResource> found = competitionParticipantServiceResult.getSuccess();

        assertEquals(1, found.size());
        assertSame(expected, found.get(0));
        assertEquals(0L, found.get(0).getSubmittedAssessments());
        assertEquals(0L, found.get(0).getTotalAssessments());
        assertEquals(0L, found.get(0).getPendingAssessments());

        InOrder inOrder = inOrder(competitionParticipantRoleMapperMock, assessmentParticipantRepositoryMock,
                assessmentParticipantMapperMock, assessmentRepositoryMock);
        inOrder.verify(competitionParticipantRoleMapperMock, calls(1)).mapToDomain(any(CompetitionParticipantRoleResource.class));
        inOrder.verify(assessmentParticipantRepositoryMock, calls(1)).getByUserIdAndRole(assessorId, ASSESSOR);
        inOrder.verify(assessmentParticipantMapperMock, calls(1)).mapToResource(any(AssessmentParticipant.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCompetitionParticipants_withAcceptedAndAssessments() {
        Long assessorId = 1L;
        Long userId = 1L;
        Long competitionId = 2L;

        List<AssessmentParticipant> competitionParticipants = new ArrayList<>();
        AssessmentParticipant competitionParticipant = new AssessmentParticipant();
        competitionParticipants.add(competitionParticipant);

        CompetitionParticipantResource expected = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withStatus(ParticipantStatusResource.ACCEPTED)
                .withCompetitionStatus(IN_ASSESSMENT)
                .build();

        List<Assessment> assessments = newAssessment()
                .withProcessState(OPEN, SUBMITTED, PENDING)
                .build(3);

        when(assessmentParticipantRepositoryMock.getByUserIdAndRole(userId, ASSESSOR)).thenReturn(competitionParticipants);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant))).thenReturn(expected);
        when(competitionParticipantRoleMapperMock.mapToDomain(CompetitionParticipantRoleResource.ASSESSOR)).thenReturn(ASSESSOR);
        when(assessmentRepositoryMock.findByParticipantUserIdAndTargetCompetitionIdOrderByActivityStateAscIdAsc(userId, competitionId)).thenReturn(assessments);

        ServiceResult<List<CompetitionParticipantResource>> competitionParticipantServiceResult =
                competitionParticipantService.getCompetitionParticipants(assessorId, CompetitionParticipantRoleResource.ASSESSOR);

        List<CompetitionParticipantResource> found = competitionParticipantServiceResult.getSuccess();

        assertSame(expected, found.get(0));
        assertEquals(1L, found.get(0).getSubmittedAssessments());
        assertEquals(2L, found.get(0).getTotalAssessments());
        assertEquals(1L, found.get(0).getPendingAssessments());

        InOrder inOrder = inOrder(competitionParticipantRoleMapperMock, assessmentParticipantRepositoryMock,
                assessmentParticipantMapperMock, assessmentRepositoryMock);
        inOrder.verify(competitionParticipantRoleMapperMock, calls(1)).mapToDomain(any(CompetitionParticipantRoleResource.class));
        inOrder.verify(assessmentParticipantRepositoryMock, calls(1)).getByUserIdAndRole(assessorId, ASSESSOR);
        inOrder.verify(assessmentParticipantMapperMock, calls(1)).mapToResource(any(AssessmentParticipant.class));
        inOrder.verify(assessmentRepositoryMock, calls(1)).findByParticipantUserIdAndTargetCompetitionIdOrderByActivityStateAscIdAsc(userId, competitionId);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCompetitionParticipants_withRejected() {
        Long assessorId = 1L;
        Long userId = 1L;
        Long competitionId = 2L;

        List<AssessmentParticipant> competitionParticipants = new ArrayList<>();
        AssessmentParticipant competitionParticipant = new AssessmentParticipant();
        competitionParticipants.add(competitionParticipant);

        CompetitionParticipantResource expected = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withStatus(ParticipantStatusResource.REJECTED)
                .withCompetitionStatus(IN_ASSESSMENT)
                .build();

        when(assessmentParticipantRepositoryMock.getByUserIdAndRole(userId, ASSESSOR)).thenReturn(competitionParticipants);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant))).thenReturn(expected);
        when(competitionParticipantRoleMapperMock.mapToDomain(CompetitionParticipantRoleResource.ASSESSOR)).thenReturn(ASSESSOR);

        ServiceResult<List<CompetitionParticipantResource>> competitionParticipantServiceResult =
                competitionParticipantService.getCompetitionParticipants(assessorId, CompetitionParticipantRoleResource.ASSESSOR);

        List<CompetitionParticipantResource> found = competitionParticipantServiceResult.getSuccess();

        assertSame(0, found.size());

        InOrder inOrder = inOrder(competitionParticipantRoleMapperMock, assessmentParticipantRepositoryMock,
                assessmentParticipantMapperMock
        );
        inOrder.verify(competitionParticipantRoleMapperMock, calls(1)).mapToDomain(any(CompetitionParticipantRoleResource.class));
        inOrder.verify(assessmentParticipantRepositoryMock, calls(1)).getByUserIdAndRole(1L, ASSESSOR);
        inOrder.verify(assessmentParticipantMapperMock, calls(1)).mapToResource(any(AssessmentParticipant.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCompetitionParticipants_upcomingAssessment() {
        Long assessorId = 1L;
        Long userId = 1L;
        Long competitionId = 2L;

        List<AssessmentParticipant> competitionParticipants = new ArrayList<>();
        AssessmentParticipant competitionParticipant = new AssessmentParticipant();
        competitionParticipants.add(competitionParticipant);

        CompetitionParticipantResource expected = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withStatus(ParticipantStatusResource.ACCEPTED)
                .withCompetitionStatus(READY_TO_OPEN)
                .build();

        when(assessmentParticipantRepositoryMock.getByUserIdAndRole(userId, ASSESSOR)).thenReturn(competitionParticipants);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant))).thenReturn(expected);
        when(competitionParticipantRoleMapperMock.mapToDomain(CompetitionParticipantRoleResource.ASSESSOR)).thenReturn(ASSESSOR);

        ServiceResult<List<CompetitionParticipantResource>> competitionParticipantServiceResult =
                competitionParticipantService.getCompetitionParticipants(assessorId, CompetitionParticipantRoleResource.ASSESSOR);

        List<CompetitionParticipantResource> found = competitionParticipantServiceResult.getSuccess();

        assertEquals(1, found.size());
        assertSame(expected, found.get(0));
        assertEquals(0L, found.get(0).getSubmittedAssessments());
        assertEquals(0L, found.get(0).getTotalAssessments());
        assertEquals(0L, found.get(0).getPendingAssessments());

        InOrder inOrder = inOrder(competitionParticipantRoleMapperMock, assessmentParticipantRepositoryMock,
                assessmentParticipantMapperMock, assessmentRepositoryMock);
        inOrder.verify(competitionParticipantRoleMapperMock, calls(1)).mapToDomain(any(CompetitionParticipantRoleResource.class));
        inOrder.verify(assessmentParticipantRepositoryMock, calls(1)).getByUserIdAndRole(assessorId, ASSESSOR);
        inOrder.verify(assessmentParticipantMapperMock, calls(1)).mapToResource(any(AssessmentParticipant.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void getCompetitionParticipants_inCompetitionSetup() {
        Long assessorId = 1L;
        Long userId = 1L;
        Long competitionId = 2L;

        List<AssessmentParticipant> competitionParticipants = new ArrayList<>();
        AssessmentParticipant competitionParticipant1 = newAssessmentParticipant().withId(1L).build();
        AssessmentParticipant competitionParticipant2 = newAssessmentParticipant().withId(2L).build();

        competitionParticipants.add(competitionParticipant1);
        competitionParticipants.add(competitionParticipant2);

        CompetitionParticipantResource expected1 = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withCompetitionStatus(COMPETITION_SETUP)
                .withStatus(ParticipantStatusResource.PENDING)
                .build();
        CompetitionParticipantResource expected2 = newCompetitionParticipantResource()
                .withUser(userId)
                .withCompetition(competitionId)
                .withCompetitionStatus(COMPETITION_SETUP)
                .withStatus(ACCEPTED)
                .build();

        when(assessmentParticipantRepositoryMock.getByUserIdAndRole(userId, ASSESSOR)).thenReturn(competitionParticipants);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant1))).thenReturn(expected1);
        when(assessmentParticipantMapperMock.mapToResource(same(competitionParticipant2))).thenReturn(expected2);
        when(competitionParticipantRoleMapperMock.mapToDomain(CompetitionParticipantRoleResource.ASSESSOR)).thenReturn(ASSESSOR);

        ServiceResult<List<CompetitionParticipantResource>> competitionParticipantServiceResult =
                competitionParticipantService.getCompetitionParticipants(assessorId, CompetitionParticipantRoleResource.ASSESSOR);

        List<CompetitionParticipantResource> found = competitionParticipantServiceResult.getSuccess();

        assertSame(0, found.size());

        InOrder inOrder = inOrder(competitionParticipantRoleMapperMock, assessmentParticipantRepositoryMock,
                assessmentParticipantMapperMock
        );
        inOrder.verify(competitionParticipantRoleMapperMock, calls(1)).mapToDomain(any(CompetitionParticipantRoleResource.class));
        inOrder.verify(assessmentParticipantRepositoryMock, calls(1)).getByUserIdAndRole(assessorId, ASSESSOR);
        inOrder.verify(assessmentParticipantMapperMock, calls(2)).mapToResource(any(AssessmentParticipant.class));
        inOrder.verifyNoMoreInteractions();
    }
}