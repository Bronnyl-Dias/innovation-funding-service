package org.innovateuk.ifs.invite.builder;

import org.innovateuk.ifs.BaseBuilder;
import org.innovateuk.ifs.invite.resource.InterviewAssignmentStagedApplicationResource;

import java.util.List;
import java.util.function.BiConsumer;

import static java.util.Collections.emptyList;

public class InterviewAssignmentCreatedInviteResourceBuilder extends BaseBuilder<InterviewAssignmentStagedApplicationResource, InterviewAssignmentCreatedInviteResourceBuilder> {

    private InterviewAssignmentCreatedInviteResourceBuilder(List<BiConsumer<Integer, InterviewAssignmentStagedApplicationResource>> newMultiActions) {
        super(newMultiActions);
    }

    @Override
    protected InterviewAssignmentCreatedInviteResourceBuilder createNewBuilderWithActions(List<BiConsumer<Integer, InterviewAssignmentStagedApplicationResource>> actions) {
        return new InterviewAssignmentCreatedInviteResourceBuilder(actions);
    }

    @Override
    protected InterviewAssignmentStagedApplicationResource createInitial() {
        return new InterviewAssignmentStagedApplicationResource();
    }

    public static InterviewAssignmentCreatedInviteResourceBuilder newInterviewAssignmentStagedApplicationResource() {
        return new InterviewAssignmentCreatedInviteResourceBuilder(emptyList());
    }

    public InterviewAssignmentCreatedInviteResourceBuilder withId(Long... value) {
        return withArraySetFieldByReflection("id", value);
    }

    public InterviewAssignmentCreatedInviteResourceBuilder withApplicationId(Long... value) {
        return withArraySetFieldByReflection("applicationId", value);
    }

    public InterviewAssignmentCreatedInviteResourceBuilder withApplicationName(String... value) {
        return withArraySetFieldByReflection("applicationName", value);
    }

    public InterviewAssignmentCreatedInviteResourceBuilder withLeadOrganisationName(String... value) {
        return withArraySetFieldByReflection("leadOrganisationName", value);
    }
}