package org.innovateuk.ifs.application.repository;

import org.innovateuk.ifs.BaseRepositoryIntegrationTest;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.resource.ApplicationState;
import org.innovateuk.ifs.assessment.domain.Assessment;
import org.innovateuk.ifs.assessment.repository.AssessmentRepository;
import org.innovateuk.ifs.assessment.resource.AssessmentState;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.repository.CompetitionRepository;
import org.innovateuk.ifs.interview.domain.InterviewAssignment;
import org.innovateuk.ifs.interview.repository.InterviewAssignmentRepository;
import org.innovateuk.ifs.interview.resource.InterviewAssignmentState;
import org.innovateuk.ifs.project.domain.Project;
import org.innovateuk.ifs.project.repository.ProjectRepository;
import org.innovateuk.ifs.workflow.domain.ActivityState;
import org.innovateuk.ifs.workflow.domain.ActivityType;
import org.innovateuk.ifs.workflow.repository.ActivityStateRepository;
import org.innovateuk.ifs.workflow.resource.State;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.application.resource.ApplicationState.CREATED;
import static org.innovateuk.ifs.application.resource.ApplicationState.SUBMITTED;
import static org.innovateuk.ifs.assessment.builder.AssessmentBuilder.newAssessment;
import static org.innovateuk.ifs.application.resource.ApplicationState.submittedAndFinishedStates;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.id;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.interview.builder.InterviewAssignmentBuilder.newInterviewAssignment;
import static org.innovateuk.ifs.project.builder.ProjectBuilder.newProject;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.junit.Assert.assertEquals;

@Rollback
public class ApplicationRepositoryIntegrationTest extends BaseRepositoryIntegrationTest<ApplicationRepository> {

    @Autowired
    private ActivityStateRepository activityStateRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private InterviewAssignmentRepository interviewAssignmentRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    @Override
    protected void setRepository(ApplicationRepository repository) {
        this.repository = repository;
    }

    @Test
    public void findByApplicationProcessActivityStateStateIn() {
        Collection<State> states = simpleMap(submittedAndFinishedStates, ApplicationState::getBackingState);

        List<Application> applicationList = simpleMap(EnumSet.complementOf(EnumSet.of(ApplicationState.IN_PANEL)),
                this::createApplicationByState);

        long initial = repository.findByApplicationProcessActivityStateStateIn(states).count();

        repository.save(applicationList);

        Stream<Application> applications = repository.findByApplicationProcessActivityStateStateIn(states);

        assertEquals(initial + 5, applications.count());
    }

    @Test
    public void findSubmittedApplicationsNotOnInterviewPanel() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        List<Application> applications = newApplication()
                .withCompetition(competition)
                .withActivityState(CREATED, SUBMITTED)
                .with(id(null))
                .build(2);

        applicationRepository.save(applications);

        Pageable pageable = new PageRequest(0, 20);

        Page<Application> invitableApplications = repository.findSubmittedApplicationsNotOnInterviewPanel(competition
                .getId(), pageable);

        assertEquals(1, invitableApplications.getTotalElements());
    }

    @Test
    public void findSubmittedApplicationsNotOnInterviewPanel_noApplications() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        Pageable pageable = new PageRequest(0, 20);

        Page<Application> applications = repository.findSubmittedApplicationsNotOnInterviewPanel(competition.getId(),
                pageable);

        assertEquals(0, applications.getTotalElements());
    }

    @Test
    public void findSubmittedApplicationsNotOnInterviewPanel_staged() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);


        List<Application> applications = newApplication()
                .withCompetition(competition)
                .withActivityState(SUBMITTED)
                .with(id(null))
                .build(2);

        applicationRepository.save(applications);

        InterviewAssignment interviewPanel = newInterviewAssignment()
                .with(id(null))
                .withState(InterviewAssignmentState.CREATED)
                .withTarget(applications.get(0))
                .build();

        interviewAssignmentRepository.save(interviewPanel);

        Pageable pageable = new PageRequest(1, 20);

        Page<Application> invitableApplications = repository.findSubmittedApplicationsNotOnInterviewPanel(competition
                .getId(), pageable);

        assertEquals(1, invitableApplications.getTotalElements());
    }

    @Test
    public void findSubmittedApplicationsNotOnInterviewPanel_inviteSent() {
        Competition competition = newCompetition().with(id(null)).build();
        competitionRepository.save(competition);

        List<Application> applications = newApplication()
                .with(id(null))
                .withCompetition(competition)
                .withActivityState(SUBMITTED)
                .build(2);

        applicationRepository.save(applications);

        InterviewAssignment interviewAssignment = newInterviewAssignment()
                .with(id(null))
                .withState(InterviewAssignmentState.AWAITING_FEEDBACK_RESPONSE)
                .withTarget(applications.get(0))
                .build();

        interviewAssignmentRepository.save(interviewAssignment);

        Pageable pageable = new PageRequest(1, 20);

        Page<Application> invitableApplications = repository.findSubmittedApplicationsNotOnInterviewPanel(competition
                .getId(), pageable);

        assertEquals(1, invitableApplications.getTotalElements());
    }

    @Test
    public void findByProjectId() {
        Application application = applicationRepository.save(newApplication().withId(17L).build());
        Project project = projectRepository.save(newProject()
                .withApplication(application)
                .withId(17L)
                .withName("Project Name")
                .build()
        );

        Application retrieved = repository.findByProjectId(project.getId());

        assertEquals(application, retrieved);
    }

    @Test
    public void findByAssessmentId() {
        Application application = repository.save(newApplication().build());

        Assessment assessment = assessmentRepository.save(newAssessment()
                .withApplication(application)
                .withActivityState(AssessmentState.SUBMITTED)
                .build()
        );

        Application retrieved = repository.findByAssessmentId(assessment.getId());

        assertEquals(application, retrieved);
    }

    private Application createApplicationByState(ApplicationState applicationState) {
        Application application = newApplication()
                .with(id(null))
                .withApplicationState(applicationState)
                .build();
        application.getApplicationProcess().setActivityState(applicationState);
        return application;
    }
}