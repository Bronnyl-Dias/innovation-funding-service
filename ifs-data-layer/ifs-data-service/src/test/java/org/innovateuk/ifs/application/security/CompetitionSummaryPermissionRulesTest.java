package org.innovateuk.ifs.application.security;

import org.innovateuk.ifs.BasePermissionRulesTest;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.invite.domain.competition.CompetitionAssessmentParticipant;
import org.innovateuk.ifs.invite.domain.competition.CompetitionParticipantRole;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.assessment.builder.CompetitionAssessmentParticipantBuilder.newCompetitionAssessmentParticipant;
import static org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder.newCompetitionResource;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CompetitionSummaryPermissionRulesTest extends BasePermissionRulesTest<CompetitionSummaryPermissionRules> {

    @Override
    protected CompetitionSummaryPermissionRules supplyPermissionRulesUnderTest() {
        return new CompetitionSummaryPermissionRules();
    }

    @Test
    public void testInternalUsersCanViewCompetitionSummaryOtherThanInnovationLeads() {
        CompetitionResource competitionResource = newCompetitionResource().build();
        allGlobalRoleUsers.forEach(user -> {
            if (!user.hasRole(UserRoleType.INNOVATION_LEAD) && allInternalUsers.contains(user)) {
                assertTrue(rules.allInternalUsersCanViewCompetitionSummaryOtherThanInnovationLeads(competitionResource, user));
            } else {
                assertFalse(rules.allInternalUsersCanViewCompetitionSummaryOtherThanInnovationLeads(competitionResource, user));
            }
        });
    }

    @Test
    public void testInnovationLeadsCanViewCompetitionSummaryOnAssginedComps() {
        CompetitionResource competitionResource= newCompetitionResource().build();
        List<Role> innovationLeadRoles = singletonList(Role.INNOVATION_LEAD);
        UserResource innovationLeadAssignedToCompetition = newUserResource().withRolesGlobal(innovationLeadRoles).build();
        UserResource innovationLeadNotAssignedToCompetition = newUserResource().withRolesGlobal(innovationLeadRoles).build();
        List<CompetitionAssessmentParticipant> competitionParticipants = newCompetitionAssessmentParticipant().withUser(newUser().withId(innovationLeadAssignedToCompetition.getId()).build()).build(1);

        when(competitionParticipantRepositoryMock.getByCompetitionIdAndRole(competitionResource.getId(), CompetitionParticipantRole.INNOVATION_LEAD)).thenReturn(competitionParticipants);

        assertTrue(rules.innovationLeadsCanViewCompetitionSummaryOnAssginedComps(competitionResource, innovationLeadAssignedToCompetition));
        assertFalse(rules.innovationLeadsCanViewCompetitionSummaryOnAssginedComps(competitionResource, innovationLeadNotAssignedToCompetition));
    }
}
