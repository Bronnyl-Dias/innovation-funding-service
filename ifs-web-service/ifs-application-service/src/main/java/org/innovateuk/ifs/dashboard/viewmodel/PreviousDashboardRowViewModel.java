package org.innovateuk.ifs.dashboard.viewmodel;

import org.innovateuk.ifs.application.resource.ApplicationState;

/**
 * Applicant dashboard row view model
 */
public class PreviousDashboardRowViewModel extends AbstractApplicantDashboardRowViewModel {

    private final ApplicationState applicationState;

    public PreviousDashboardRowViewModel(String title, long applicationId, String competitionTitle, ApplicationState applicationState) {
        super(title, applicationId, competitionTitle);
        this.applicationState = applicationState;
    }

    /* View logic */
    public boolean isRejected() {
        return ApplicationState.REJECTED.equals(applicationState);
    }

    public boolean isApproved() {
        return ApplicationState.APPROVED.equals(applicationState);
    }

    public boolean isCreatedOrOpen() {
        return ApplicationState.OPEN.equals(applicationState)
                ||  ApplicationState.CREATED.equals(applicationState);
    }

    public boolean isInformedIneligible() {
        return ApplicationState.INELIGIBLE_INFORMED.equals(applicationState);
    }

    @Override
    public String getLinkUrl() {
        return String.format("/application/%s/summary", getApplicationNumber());
    }
}
