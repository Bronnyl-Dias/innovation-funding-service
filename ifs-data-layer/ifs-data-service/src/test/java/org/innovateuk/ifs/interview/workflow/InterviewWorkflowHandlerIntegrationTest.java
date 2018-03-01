package org.innovateuk.ifs.interview.workflow;

import org.innovateuk.ifs.interview.domain.Interview;
import org.innovateuk.ifs.interview.repository.InterviewRepository;
import org.innovateuk.ifs.interview.resource.InterviewState;
import org.innovateuk.ifs.interview.workflow.configuration.InterviewWorkflowHandler;
import org.innovateuk.ifs.user.repository.ProcessRoleRepository;
import org.innovateuk.ifs.workflow.BaseWorkflowHandlerIntegrationTest;
import org.innovateuk.ifs.workflow.TestableTransitionWorkflowAction;
import org.innovateuk.ifs.workflow.domain.ActivityState;
import org.innovateuk.ifs.workflow.domain.ActivityType;
import org.innovateuk.ifs.workflow.repository.ActivityStateRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.innovateuk.ifs.interview.builder.InterviewBuilder.newInterview;
import static org.innovateuk.ifs.interview.resource.InterviewState.CREATED;
import static org.innovateuk.ifs.interview.resource.InterviewState.PENDING;
import static org.innovateuk.ifs.workflow.domain.ActivityType.ASSESSMENT_INTERVIEW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
public class InterviewWorkflowHandlerIntegrationTest
        extends BaseWorkflowHandlerIntegrationTest<
        InterviewWorkflowHandler,
        InterviewRepository, TestableTransitionWorkflowAction> {

    private static final ActivityType ACTIVITY_TYPE = ASSESSMENT_INTERVIEW;

    @Autowired
    private InterviewWorkflowHandler workflowHandler;

    private ActivityStateRepository activityStateRepositoryMock;
    private InterviewRepository repositoryMock;

    @Override
    protected void collectMocks(Function<Class<? extends Repository>, Repository> mockSupplier) {
        activityStateRepositoryMock = (ActivityStateRepository) mockSupplier.apply(ActivityStateRepository.class);
        repositoryMock = (InterviewRepository) mockSupplier.apply(InterviewRepository.class);
    }

    @Override
    protected List<Class<? extends Repository>> getRepositoriesToMock() {
        List<Class<? extends Repository>> repositories = new ArrayList<>(super.getRepositoriesToMock());
        repositories.add(ProcessRoleRepository.class);
        return repositories;
    }

    @Test
    public void notifyInvitation() {
        assertStateChangeOnWorkflowHandlerCall(CREATED, PENDING, invite -> workflowHandler.notifyInvitation(invite));
    }

    @Override
    protected Class<TestableTransitionWorkflowAction> getBaseActionType() {
        return TestableTransitionWorkflowAction.class;
    }

    @Override
    protected Class<InterviewWorkflowHandler> getWorkflowHandlerType() {
        return InterviewWorkflowHandler.class;
    }

    @Override
    protected Class<InterviewRepository> getProcessRepositoryType() {
        return InterviewRepository.class;
    }

    private ActivityType getActivityType() {
        return ACTIVITY_TYPE;
    }

    private InterviewRepository getRepositoryMock() {
        return repositoryMock;
    }


    private Interview buildWorkflowProcessWithInitialState(InterviewState initialState) {
        return newInterview().withState(initialState).build();
    }

    private void assertStateChangeOnWorkflowHandlerCall(InterviewState initialState, InterviewState expectedState, Function<Interview, Boolean> workflowHandlerMethod) {
        assertStateChangeOnWorkflowHandlerCall(initialState, expectedState, workflowHandlerMethod, null);
    }

    private void assertStateChangeOnWorkflowHandlerCall(InterviewState initialState, InterviewState expectedState, Function<Interview, Boolean> workflowHandlerMethod, Consumer<Interview> additionalVerifications) {
        Interview workflowProcess = buildWorkflowProcessWithInitialState(initialState);
        when(getRepositoryMock().findOneByTargetId(workflowProcess.getId())).thenReturn(workflowProcess);

        ActivityState expectedActivityState = new ActivityState(getActivityType(), expectedState.getBackingState());
        when(activityStateRepositoryMock.findOneByActivityTypeAndState(getActivityType(), expectedState.getBackingState())).thenReturn(expectedActivityState);

        assertTrue(workflowHandlerMethod.apply(workflowProcess));

        assertEquals(expectedState, workflowProcess.getActivityState());

        verify(activityStateRepositoryMock).findOneByActivityTypeAndState(getActivityType(), expectedState.getBackingState());
        verify(getRepositoryMock()).save(workflowProcess);

        if (additionalVerifications != null) {
            additionalVerifications.accept(workflowProcess);
        }

        verifyNoMoreInteractionsWithMocks();
    }
}