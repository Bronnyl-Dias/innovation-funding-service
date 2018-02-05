package org.innovateuk.ifs.invite.repository;

import org.innovateuk.ifs.invite.domain.ParticipantStatus;
import org.innovateuk.ifs.invite.domain.competition.AssessmentReviewPanelParticipant;
import org.innovateuk.ifs.invite.domain.competition.CompetitionParticipantRole;
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
public interface AssessmentPanelParticipantRepository extends PagingAndSortingRepository<AssessmentReviewPanelParticipant, Long> {

    String USERS_WITH_ASSESSMENT_PANEL_INVITE = "SELECT invite.user.id " +
            "FROM AssessmentReviewPanelInvite invite " +
            "WHERE invite.competition.id = :competitionId " +
            "AND invite.user IS NOT NULL";

    String BY_COMP_AND_STATUS_ON_PANEL = "SELECT assessmentPanelParticipant " +
            "FROM AssessmentReviewPanelParticipant assessmentPanelParticipant " +
            "WHERE assessmentPanelParticipant.competition.id = :competitionId " +
            "AND assessmentPanelParticipant.role = 'PANEL_ASSESSOR' " +
            "AND assessmentPanelParticipant.status IN :status " +
            "AND assessmentPanelParticipant.user.id IN (" + USERS_WITH_ASSESSMENT_PANEL_INVITE + ")";

    @Query(BY_COMP_AND_STATUS_ON_PANEL)
    Page<AssessmentReviewPanelParticipant> getPanelAssessorsByCompetitionAndStatusContains(@Param("competitionId") long competitionId,
                                                                                           @Param("status") List<ParticipantStatus> status,
                                                                                           Pageable pageable);

    @Query(BY_COMP_AND_STATUS_ON_PANEL)
    List<AssessmentReviewPanelParticipant> getPanelAssessorsByCompetitionAndStatusContains(@Param("competitionId") long competitionId,
                                                                                           @Param("status") List<ParticipantStatus> status);

    @Override
    List<AssessmentReviewPanelParticipant> findAll();

    AssessmentReviewPanelParticipant getByInviteHash(String hash);

    List<AssessmentReviewPanelParticipant> findByUserIdAndRole(long userId, CompetitionParticipantRole role);

    int countByCompetitionIdAndRoleAndStatusAndInviteIdIn(long competitionId,
                                                          CompetitionParticipantRole role,
                                                          ParticipantStatus status,
                                                          List<Long> inviteIds);
}