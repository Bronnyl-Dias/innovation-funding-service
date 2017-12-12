package org.innovateuk.ifs.assessment.dashboard.viewmodel;

import org.innovateuk.ifs.assessment.profile.viewmodel.AssessorProfileStatusViewModel;

import java.util.List;

/**
 * Holder of model attributes for the Assessor Dashboard.
 */
public class AssessorDashboardViewModel {

    private List<AssessorDashboardActiveCompetitionViewModel> activeCompetitions;
    private List<AssessorDashboardUpcomingCompetitionViewModel> upcomingCompetitions;
    private List<AssessorDashboardPendingInviteViewModel> pendingInvites;
    private List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelInvites;
    private List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelAccepted;
    private AssessorProfileStatusViewModel profileStatus;

    public AssessorDashboardViewModel(
            AssessorProfileStatusViewModel profileStatus,
            List<AssessorDashboardActiveCompetitionViewModel> activeCompetitions,
            List<AssessorDashboardUpcomingCompetitionViewModel> upcomingCompetitions,
            List<AssessorDashboardPendingInviteViewModel> pendingInvites,
            List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelInvites,
            List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelAccepted
    ) {
        this.profileStatus = profileStatus;
        this.activeCompetitions = activeCompetitions;
        this.upcomingCompetitions = upcomingCompetitions;
        this.pendingInvites = pendingInvites;
        this.assessmentPanelInvites = assessmentPanelInvites;
        this.assessmentPanelAccepted = assessmentPanelAccepted;
    }

    public List<AssessorDashboardActiveCompetitionViewModel> getActiveCompetitions() {
        return activeCompetitions;
    }

    public void setActiveCompetitions(List<AssessorDashboardActiveCompetitionViewModel> activeCompetitions) {
        this.activeCompetitions = activeCompetitions;
    }

    public List<AssessorDashboardUpcomingCompetitionViewModel> getUpcomingCompetitions() {
        return upcomingCompetitions;
    }

    public void setUpcomingCompetitions(List<AssessorDashboardUpcomingCompetitionViewModel> upcomingCompetitions) {
        this.upcomingCompetitions = upcomingCompetitions;
    }

    public void setAssessmentPanelInvites(List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelInvites) {
        this.assessmentPanelInvites = assessmentPanelInvites;
    }

    public List<AssessorDashboardAssessmentPanelInviteViewModel> getAssessmentPanelInvites() {
        return assessmentPanelInvites;
    }

    public List<AssessorDashboardAssessmentPanelInviteViewModel> getAssessmentPanelAccepted() {
        return assessmentPanelAccepted;
    }

    public void setAssessmentPanelAccepted(List<AssessorDashboardAssessmentPanelInviteViewModel> assessmentPanelAccepted) {
        this.assessmentPanelAccepted = assessmentPanelAccepted;
    }

    public AssessorProfileStatusViewModel getProfileStatus() {
        return profileStatus;
    }

    public List<AssessorDashboardPendingInviteViewModel> getPendingInvites() {
        return pendingInvites;
    }
}
