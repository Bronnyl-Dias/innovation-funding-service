package org.innovateuk.ifs.competition.service;

import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.commons.service.BaseRestService;
import org.innovateuk.ifs.invite.resource.InviteUserResource;
import org.innovateuk.ifs.invite.resource.RoleInviteResource;
import org.innovateuk.ifs.invite.resource.StakeholderInviteResource;
import org.innovateuk.ifs.user.resource.UserResource;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;
import static org.innovateuk.ifs.commons.service.ParameterizedTypeReferences.userListType;

/**
 * Implements {@link CompetitionSetupStakeholderRestService}
 */
@Service
public class CompetitionSetupStakeholderRestServiceImpl extends BaseRestService implements CompetitionSetupStakeholderRestService {

    private String competitionSetupStakeholderRestURL = "/competition/setup";

    @Override
    public RestResult<Void> inviteStakeholder(InviteUserResource inviteUserResource, long competitionId) {
        return postWithRestResult(format("%s/%s/stakeholder/invite", competitionSetupStakeholderRestURL, competitionId), inviteUserResource, Void.class);
    }

    @Override
    public RestResult<List<UserResource>> findStakeholders(long competitionId) {
        return getWithRestResult(format("%s/%s/stakeholder/find-all", competitionSetupStakeholderRestURL , competitionId), userListType());
    }

    @Override
    public RestResult<StakeholderInviteResource> getInvite(String inviteHash) {
        return getWithRestResultAnonymous(format("%s/get-invite/%s", competitionSetupStakeholderRestURL, inviteHash), StakeholderInviteResource.class);
    }
}


