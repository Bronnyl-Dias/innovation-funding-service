package org.innovateuk.ifs.assessment.invite.viewmodel;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.innovateuk.ifs.invite.resource.InterviewInviteResource;

import java.time.ZonedDateTime;

/**
 * ViewModel of a InterviewInvite.
 */
public class InterviewInviteViewModel  extends BaseInviteViewModel {

    private ZonedDateTime interviewDate;

    public InterviewInviteViewModel(String panelInviteHash, InterviewInviteResource invite, boolean userLoggedIn) {
        super(panelInviteHash, invite.getCompetitionId(), invite.getCompetitionName(), userLoggedIn);
        this.interviewDate = invite.getInterviewDate();
    }

    public String getPanelInviteHash() {
        return getInviteHash();
    }

    public ZonedDateTime getInterviewDate() {
        return interviewDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        InterviewInviteViewModel that = (InterviewInviteViewModel) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(interviewDate, that.interviewDate)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(interviewDate)
                .toHashCode();
    }
}
