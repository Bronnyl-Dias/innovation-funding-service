package org.innovateuk.ifs.interview.builder;

import org.innovateuk.ifs.BaseBuilder;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.interview.domain.InterviewAssignment;
import org.innovateuk.ifs.interview.resource.InterviewAssignmentState;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.workflow.domain.ActivityState;

import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.setField;
import static org.innovateuk.ifs.base.amend.BaseBuilderAmendFunctions.uniqueIds;
import static org.innovateuk.ifs.workflow.domain.ActivityType.ASSESSMENT_INTERVIEW_PANEL;

public class InterviewAssignmentBuilder extends BaseBuilder<InterviewAssignment, InterviewAssignmentBuilder> {

    private InterviewAssignmentBuilder(List<BiConsumer<Integer, InterviewAssignment>> multiActions) {
        super(multiActions);
    }

    public static InterviewAssignmentBuilder newInterviewAssignment() {
        return new InterviewAssignmentBuilder(emptyList()).with(uniqueIds());
    }

    @Override
    protected InterviewAssignmentBuilder createNewBuilderWithActions(List<BiConsumer<Integer, InterviewAssignment>> actions) {
        return new InterviewAssignmentBuilder(actions);
    }

    @Override
    protected InterviewAssignment createInitial() {
        return new InterviewAssignment();
    }

    public InterviewAssignmentBuilder withId(Long... ids) {
        return withArray((id, invite) -> setField("id", id, invite), ids);
    }

    public InterviewAssignmentBuilder withTarget(Application... applications) {
        return withArray((application, invite) -> invite.setTarget(application), applications);
    }

    public InterviewAssignmentBuilder withParticipant(ProcessRole... participants) {
        return withArray((participant, invite) -> invite.setParticipant(participant), participants);
    }

    public InterviewAssignmentBuilder withState(InterviewAssignmentState... states) {
        return withArray((state, invite) -> invite.setActivityState(new ActivityState(ASSESSMENT_INTERVIEW_PANEL, state.getBackingState())), states);
    }
}