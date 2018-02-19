package org.innovateuk.ifs.invite.repository;

import org.innovateuk.ifs.invite.domain.ParticipantStatus;
import org.innovateuk.ifs.invite.domain.competition.CompetitionParticipantRole;
import org.innovateuk.ifs.invite.domain.competition.InterviewParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * This interface is used to generate Spring Data Repositories.
 * For more info:
 * http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories
 */
public interface InterviewParticipantRepository extends PagingAndSortingRepository<InterviewParticipant, Long> {

    String USERS_WITH_INTERVIEW_PANEL_INVITE = "SELECT invite.user.id " +
            "FROM InterviewInvite invite " +
            "WHERE invite.competition.id = :competitionId " +
            "AND invite.user IS NOT NULL";

    String BY_COMP_AND_STATUS_ON_PANEL = "SELECT assessmentInterviewPanelParticipant " +
            "FROM InterviewParticipant assessmentInterviewPanelParticipant " +
            "WHERE assessmentInterviewPanelParticipant.competition.id = :competitionId " +
            "AND assessmentInterviewPanelParticipant.role = org.innovateuk.ifs.invite.domain.competition.CompetitionParticipantRole.INTERVIEW_ASSESSOR " +
            "AND assessmentInterviewPanelParticipant.status IN :status " +
            "AND assessmentInterviewPanelParticipant.user.id IN (" + USERS_WITH_INTERVIEW_PANEL_INVITE + ")";

    @Query(BY_COMP_AND_STATUS_ON_PANEL)
    Page<InterviewParticipant> getInterviewPanelAssessorsByCompetitionAndStatusContains(@Param("competitionId") long competitionId,
                                                                                        @Param("status") List<ParticipantStatus> status,
                                                                                        Pageable pageable);

    @Query(BY_COMP_AND_STATUS_ON_PANEL)
    List<InterviewParticipant> getInterviewPanelAssessorsByCompetitionAndStatusContains(@Param("competitionId") long competitionId,
                                                                                        @Param("status") List<ParticipantStatus> status);

    @Override
    List<InterviewParticipant> findAll();

    InterviewParticipant getByInviteHash(String hash);

    List<InterviewParticipant> findByUserIdAndRole(long userId, CompetitionParticipantRole role);

    int countByCompetitionIdAndRoleAndStatusAndInviteIdIn(long competitionId,
                                                          CompetitionParticipantRole role,
                                                          ParticipantStatus status,
                                                          List<Long> inviteIds);
}