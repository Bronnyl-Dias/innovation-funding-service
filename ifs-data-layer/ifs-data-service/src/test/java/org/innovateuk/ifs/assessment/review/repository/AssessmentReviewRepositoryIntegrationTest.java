package org.innovateuk.ifs.assessment.review.repository;

import org.innovateuk.ifs.BaseRepositoryIntegrationTest;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.assessment.review.domain.AssessmentReview;
import org.innovateuk.ifs.assessment.review.repository.AssessmentReviewRepository;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.repository.CompetitionRepository;
import org.innovateuk.ifs.invite.constant.InviteStatus;
import org.innovateuk.ifs.invite.domain.ParticipantStatus;
import org.innovateuk.ifs.invite.domain.competition.AssessmentReviewPanelInvite;
import org.innovateuk.ifs.invite.domain.competition.AssessmentReviewPanelParticipant;
import org.innovateuk.ifs.invite.repository.AssessmentPanelInviteRepository;
import org.innovateuk.ifs.invite.repository.AssessmentPanelParticipantRepository;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.repository.ProcessRoleRepository;
import org.innovateuk.ifs.user.repository.UserRepository;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.innovateuk.ifs.workflow.domain.ActivityType;
import org.innovateuk.ifs.workflow.repository.ActivityStateRepository;
import org.innovateuk.ifs.workflow.resource.State;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.assessment.review.builder.AssessmentPanelInviteBuilder.newAssessmentPanelInvite;
import static org.innovateuk.ifs.assessment.review.builder.AssessmentReviewBuilder.newAssessmentReview;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.id;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.user.builder.ProcessRoleBuilder.newProcessRole;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssessmentReviewRepositoryIntegrationTest extends BaseRepositoryIntegrationTest<AssessmentReviewRepository> {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityStateRepository activityStateRepository;

    @Autowired
    private ProcessRoleRepository processRoleRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private AssessmentPanelInviteRepository assessmentPanelInviteRepository;

    @Autowired
    private AssessmentPanelParticipantRepository assessmentPanelParticipantRepository;

    @Autowired
    @Override
    protected void setRepository(AssessmentReviewRepository repository) {
        this.repository = repository;
    }

    @Before
    public void setup() {
        loginPaulPlum();
    }

    @Test
    public void existsByParticipantUserAndTarget() {
        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        Application application = newApplication().with(id(null)).build();
        applicationRepository.save(application);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.CREATED));
        repository.save(assessmentReview);

        assertTrue(repository.existsByParticipantUserAndTargetAndActivityStateStateNot(user, application, State.WITHDRAWN)); // probably should be notExists if that's allowed
    }

    @Test
    public void existsByParticipantUserAndTarget_notExists() {
        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        Application application = newApplication().with(id(null)).build();
        applicationRepository.save(application);

        Application application2 = newApplication().with(id(null)).build();
        applicationRepository.save(application2);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.CREATED));
        repository.save(assessmentReview);

        assertFalse(repository.existsByParticipantUserAndTargetAndActivityStateStateNot(user, application2, State.WITHDRAWN));
    }

    @Test
    public void existsByTargetIdAndActivityStateState() {

        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        Application application = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .build();
        applicationRepository.save(application);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.CREATED));
        repository.save(assessmentReview);


        assertTrue(repository.existsByTargetCompetitionIdAndActivityStateState(competition.getId(), State.CREATED));
    }

    @Test
    public void existsByTargetIdAndActivityStateState_notExists() {
        Competition competition = newCompetition().with(id(null)).build();
        Competition competition2 = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);
        competitionRepository.save(competition2);

        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        Application application = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .build();
        applicationRepository.save(application);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.CREATED));
        repository.save(assessmentReview);


        assertFalse(repository.existsByTargetCompetitionIdAndActivityStateState(competition2.getId(), State.CREATED));
    }

    @Test
    public void notifiable() {

        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        AssessmentReviewPanelInvite assessmentReviewPanelInvite = newAssessmentPanelInvite()
                .with(id(null))
                .withCompetition(competition)
                .withUser(user)
                .withEmail("tom@poly.io")
                .withStatus(InviteStatus.SENT)
                .withName("tom baldwin")
                .build();
        assessmentPanelInviteRepository.save(assessmentReviewPanelInvite);

        AssessmentReviewPanelParticipant assessmentReviewPanelParticipant = new AssessmentReviewPanelParticipant(assessmentReviewPanelInvite);
        assessmentReviewPanelParticipant.setStatus(ParticipantStatus.ACCEPTED);
        assessmentPanelParticipantRepository.save(assessmentReviewPanelParticipant);

        Application application = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .withInAssessmentReviewPanel(true)
                .build();
        applicationRepository.save(application);

        assertTrue(repository.notifiable(competition.getId()));
    }

    @Test
    public void notifiable_assigned() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        AssessmentReviewPanelInvite competitionAssessmentInvite = newAssessmentPanelInvite()
                .with(id(null))
                .withCompetition(competition)
                .withUser(user)
                .withEmail("tom@poly.io")
                .withStatus(InviteStatus.SENT)
                .withName("tom baldwin")
                .build();

        assessmentPanelInviteRepository.save(competitionAssessmentInvite);

        AssessmentReviewPanelParticipant competitionAssessmentParticipant = new AssessmentReviewPanelParticipant(competitionAssessmentInvite);
        competitionAssessmentParticipant.setStatus(ParticipantStatus.ACCEPTED);

        assessmentPanelParticipantRepository.save(competitionAssessmentParticipant);

        Application application = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .withInAssessmentReviewPanel(true)
                .build();
        applicationRepository.save(application);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.CREATED));

        repository.save(assessmentReview);

        assertFalse(repository.notifiable(competition.getId()));
    }

    @Test
    public void notifiable_assigned_withdrawn() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        User user = newUser()
                .with(id(null))
                .withUid("foo")
                .build();

        userRepository.save(user);

        AssessmentReviewPanelInvite assessmentReviewPanelInvite = newAssessmentPanelInvite()
                .with(id(null))
                .withCompetition(competition)
                .withUser(user)
                .withEmail("tom@poly.io")
                .withStatus(InviteStatus.SENT)
                .withName("tom baldwin")
                .build();
        assessmentPanelInviteRepository.save(assessmentReviewPanelInvite);

        AssessmentReviewPanelParticipant competitionAssessmentParticipant = new AssessmentReviewPanelParticipant(assessmentReviewPanelInvite);
        competitionAssessmentParticipant.setStatus(ParticipantStatus.ACCEPTED);
        assessmentPanelParticipantRepository.save(competitionAssessmentParticipant);

        Application application = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .withInAssessmentReviewPanel(true)
                .build();
        applicationRepository.save(application);

        ProcessRole processRole = newProcessRole()
                .with(id(null))
                .withUser(user)
                .withApplication(application)
                .withRole(UserRoleType.PANEL_ASSESSOR)
                .build();
        processRoleRepository.save(processRole);

        AssessmentReview assessmentReview =
                newAssessmentReview()
                        .with(id(null))
                        .withParticipant(processRole)
                        .withTarget(application)
                        .build();
        assessmentReview.setActivityState(activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.WITHDRAWN));

        repository.save(assessmentReview);

        assertTrue(repository.notifiable(competition.getId()));
    }
}