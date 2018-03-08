package org.innovateuk.ifs.assessment.security;

import org.innovateuk.ifs.commons.security.PermissionEntityLookupStrategies;
import org.innovateuk.ifs.invite.domain.competition.AssessmentInvite;
import org.springframework.stereotype.Component;

/**
 * Lookup strategy for {@link AssessmentInvite}, used for permissioning.
 */
@Component
@PermissionEntityLookupStrategies
public class CompetitionInviteLookupStrategy {
}
