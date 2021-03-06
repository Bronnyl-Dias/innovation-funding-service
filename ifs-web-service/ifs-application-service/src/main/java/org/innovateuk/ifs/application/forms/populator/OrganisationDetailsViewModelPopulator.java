package org.innovateuk.ifs.application.forms.populator;

import org.innovateuk.ifs.applicant.resource.AbstractApplicantResource;
import org.innovateuk.ifs.applicant.resource.ApplicantResource;
import org.innovateuk.ifs.application.forms.viewmodel.QuestionOrganisationDetailsViewModel;
import org.innovateuk.ifs.invite.InviteService;
import org.innovateuk.ifs.invite.resource.ApplicationInviteResource;
import org.innovateuk.ifs.organisation.resource.OrganisationResource;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.resource.Role;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Populating viewModel for Organisation Details
 */
@Component
public class OrganisationDetailsViewModelPopulator {

    protected InviteService inviteService;

    public OrganisationDetailsViewModelPopulator(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    public <R extends AbstractApplicantResource> QuestionOrganisationDetailsViewModel populateModel(final R applicantResource) {
        final SortedSet<OrganisationResource> organisations = getApplicationOrganisations(applicantResource);
        final List<String> activeApplicationOrganisationNames = organisations.stream().map(OrganisationResource::getName).collect(Collectors.toList());
        final List<String> pendingOrganisationNames = inviteService.getPendingInvitationsByApplicationId(applicantResource.getApplication().getId()).stream()
                .map(ApplicationInviteResource::getInviteOrganisationNameConfirmedSafe)
                .distinct()
                .filter(orgName -> StringUtils.hasText(orgName)
                        && activeApplicationOrganisationNames.stream().noneMatch(organisationName -> organisationName.equals(orgName))).collect(Collectors.toList());
        final Optional<OrganisationResource> leadOrganisation = getApplicationLeadOrganisation(applicantResource);
        OrganisationResource foundLeadOrganisation = leadOrganisation.isPresent() ? leadOrganisation.get() : null;

        return new QuestionOrganisationDetailsViewModel(getAcademicOrganisations(organisations), organisations, pendingOrganisationNames, foundLeadOrganisation);
    }

    private SortedSet<OrganisationResource> getAcademicOrganisations(final SortedSet<OrganisationResource> organisations) {
        final Comparator<OrganisationResource> compareById =
                Comparator.comparingLong(OrganisationResource::getId);
        final Supplier<TreeSet<OrganisationResource>> supplier = () -> new TreeSet<>(compareById);
        final ArrayList<OrganisationResource> organisationList = new ArrayList<>(organisations);

        return organisationList.stream()
                .filter(o -> OrganisationTypeEnum.RESEARCH.getId() == o.getOrganisationType())
                .collect(Collectors.toCollection(supplier));
    }

    private <R extends AbstractApplicantResource> SortedSet<OrganisationResource> getApplicationOrganisations(final R applicantResource) {
        final Comparator<OrganisationResource> compareById =
                Comparator.comparingLong(OrganisationResource::getId);
        final Supplier<SortedSet<OrganisationResource>> supplier = () -> new TreeSet<>(compareById);

        return applicantResource.getApplicants().stream()
                .filter(applicant -> applicant.getProcessRole().getRoleName().equals(Role.LEADAPPLICANT.getName())
                        || applicant.getProcessRole().getRoleName().equals(Role.COLLABORATOR.getName()))
                .map(ApplicantResource::getOrganisation)
                .collect(Collectors.toCollection(supplier));
    }

    private <R extends AbstractApplicantResource> Optional<OrganisationResource> getApplicationLeadOrganisation(R applicantResource) {
        return applicantResource.getApplicants().stream()
                .filter(applicant -> applicant.getProcessRole().getRoleName().equals(Role.LEADAPPLICANT.getName()))
                .map(ApplicantResource::getOrganisation)
                .findFirst();
    }
}
