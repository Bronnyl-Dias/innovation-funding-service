package org.innovateuk.ifs.invite.transactional;

import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.transactional.ApplicationService;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.invite.domain.ApplicationInvite;
import org.innovateuk.ifs.invite.domain.InviteOrganisation;
import org.innovateuk.ifs.invite.repository.ApplicationInviteRepository;
import org.innovateuk.ifs.invite.repository.InviteOrganisationRepository;
import org.innovateuk.ifs.user.domain.Organisation;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.domain.Role;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.repository.OrganisationRepository;
import org.innovateuk.ifs.user.repository.ProcessRoleRepository;
import org.innovateuk.ifs.user.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.user.resource.UserRoleType.COLLABORATOR;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;

/**
 * Service for accepting collaborator's {@link ApplicationInvite}s
 * and initialising their state on the application correctly.
 */
@Service
public class AcceptInviteServiceImpl extends BaseInviteService implements AcceptInviteService {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptInviteServiceImpl.class);

    @Autowired
    private ApplicationInviteRepository applicationInviteRepository;

    @Autowired
    private InviteOrganisationRepository inviteOrganisationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProcessRoleRepository processRoleRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Override
    @Transactional
    public ServiceResult<Void> acceptInvite(String inviteHash, Long userId) {
        return find(invite(inviteHash), user(userId)).andOnSuccess((invite, user) -> {
            if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
                LOG.error(
                        "Invite (id: {}) email address does not match user's (id: {})",
                        invite.getId(),
                        user.getId()
                );

                return serviceFailure(new Error(
                        "Invite email address not the same as the user's email address",
                        NOT_ACCEPTABLE
                ));
            }

            invite.open();

            if (invite.getInviteOrganisation().getOrganisation() == null) {
                assignOrganisationToInvite(invite, user);
            }

            initializeInvitee(invite, user);

            return serviceSuccess();
        });
    }

    private void assignOrganisationToInvite(ApplicationInvite invite, User user) {
        List<Organisation> organisations = organisationRepository.findByUsers(user);

        if (organisations.isEmpty()) {
            return;
        }

        // A bit contentious, but we assume that the first organisation
        // that a user belongs to is their 'current' organisation.
        Organisation usersCurrentOrganisation = organisations.get(0);

        Optional<InviteOrganisation> existingCollaboratorInviteOrganisation =
                inviteOrganisationRepository.findFirstByOrganisationIdAndInvitesApplicationId(
                        usersCurrentOrganisation.getId(),
                        invite.getTarget().getId()
                );

        if (existingCollaboratorInviteOrganisation.isPresent()) {
            replaceInviteOrganisationOnInvite(invite, existingCollaboratorInviteOrganisation.get());
        } else {
            invite.getInviteOrganisation().setOrganisation(usersCurrentOrganisation);
            applicationInviteRepository.save(invite);
        }
    }

    private void replaceInviteOrganisationOnInvite(
            ApplicationInvite invite,
            InviteOrganisation inviteOrganisation
    ) {
        InviteOrganisation currentInviteOrganisation = invite.getInviteOrganisation();
        currentInviteOrganisation.removeInvite(invite);
        inviteOrganisationRepository.saveAndFlush(currentInviteOrganisation);

        if (currentInviteOrganisation.getInvites().isEmpty()) {
            inviteOrganisationRepository.delete(currentInviteOrganisation);
        }

        invite.setInviteOrganisation(inviteOrganisation);
        applicationInviteRepository.save(invite);
    }

    private void initializeInvitee(ApplicationInvite invite, User user) {
        Application application = invite.getTarget();
        Role role = roleRepository.findOneByName(COLLABORATOR.getName());
        Organisation organisation = invite.getInviteOrganisation().getOrganisation();

        ProcessRole processRole = new ProcessRole(user, application.getId(), role, organisation.getId());
        processRoleRepository.save(processRole);
        application.addProcessRole(processRole);

        updateApplicationProgress(application);
    }

    private void updateApplicationProgress(Application application) {
        BigDecimal completion = applicationService
                .getProgressPercentageBigDecimalByApplicationId(application.getId())
                .getSuccessObject();
        application.setCompletion(completion);
    }
}
