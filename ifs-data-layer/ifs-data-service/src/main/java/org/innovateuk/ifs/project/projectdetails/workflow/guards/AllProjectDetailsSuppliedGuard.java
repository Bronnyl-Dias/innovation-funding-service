package org.innovateuk.ifs.project.projectdetails.workflow.guards;

import org.innovateuk.ifs.project.core.domain.Project;
import org.innovateuk.ifs.project.core.domain.ProjectUser;
import org.innovateuk.ifs.project.resource.ProjectDetailsEvent;
import org.innovateuk.ifs.project.resource.ProjectDetailsState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static org.innovateuk.ifs.util.CollectionFunctions.getOnlyElementOrEmpty;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleFilter;

/**
 * This asserts that all mandatory Project Details have been included prior to allowing them to be submitted.
 */
@Component
public class AllProjectDetailsSuppliedGuard implements Guard<ProjectDetailsState, ProjectDetailsEvent> {

    @Override
    public boolean evaluate(StateContext<ProjectDetailsState, ProjectDetailsEvent> context) {

        Project project = (Project) context.getMessageHeader("target");

        return validateIsReadyForSubmission(project);
    }

    private boolean validateIsReadyForSubmission(final Project project) {
        return project.getAddress() != null &&
                getExistingProjectManager(project).isPresent() &&
                project.getTargetStartDate() != null;
    }

    private Optional<ProjectUser> getExistingProjectManager(Project project) {
        List<ProjectUser> projectUsers = project.getProjectUsers();
        List<ProjectUser> projectManagers = simpleFilter(projectUsers, pu -> pu.getRole().isProjectManager());
        return getOnlyElementOrEmpty(projectManagers);
    }
}
