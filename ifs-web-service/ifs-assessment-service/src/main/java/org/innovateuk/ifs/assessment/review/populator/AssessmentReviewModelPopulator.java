package org.innovateuk.ifs.assessment.review.populator;

import org.innovateuk.ifs.application.UserApplicationRole;
import org.innovateuk.ifs.assessment.review.viewmodel.AssessmentReviewViewModel;
import org.innovateuk.ifs.assessment.service.AssessmentPanelRestService;
import org.innovateuk.ifs.form.resource.FormInputResponseResource;
import org.innovateuk.ifs.form.service.FormInputResponseRestService;
import org.innovateuk.ifs.review.resource.ReviewResource;
import org.innovateuk.ifs.user.resource.OrganisationResource;
import org.innovateuk.ifs.user.resource.ProcessRoleResource;
import org.innovateuk.ifs.user.service.OrganisationRestService;
import org.innovateuk.ifs.user.service.ProcessRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Build the model for the Assessment Assignment view.
 */
@Component
public class AssessmentReviewModelPopulator {

    @Autowired
    private ProcessRoleService processRoleService;

    @Autowired
    private FormInputResponseRestService formInputResponseRestService;

    @Autowired
    private OrganisationRestService organisationRestService;

    @Autowired
    private AssessmentPanelRestService assessmentPanelRestService;

    public AssessmentReviewViewModel populateModel(long reviewId) {
        ReviewResource reviewResource =
                assessmentPanelRestService.getAssessmentReview(reviewId).getSuccess();

        String projectSummary = getProjectSummary(reviewResource);
        List<ProcessRoleResource> processRoles = processRoleService.findProcessRolesByApplicationId(reviewResource.getApplication());
        SortedSet<OrganisationResource> collaborators = getApplicationOrganisations(processRoles);
        OrganisationResource leadPartner = getApplicationLeadOrganisation(processRoles).orElse(null);

        return new AssessmentReviewViewModel(
                reviewId,
                reviewResource.getCompetition(),
                reviewResource.getApplicationName(),
                collaborators,
                leadPartner,
                projectSummary);
    }

    private Optional<OrganisationResource> getApplicationLeadOrganisation(List<ProcessRoleResource> userApplicationRoles) {
        return userApplicationRoles.stream()
                .filter(uar -> uar.getRoleName().equals(UserApplicationRole.LEAD_APPLICANT.getRoleName()))
                .map(uar -> organisationRestService.getOrganisationById(uar.getOrganisationId()).getSuccess())
                .findFirst();
    }

    private SortedSet<OrganisationResource> getApplicationOrganisations(List<ProcessRoleResource> userApplicationRoles) {
        Comparator<OrganisationResource> compareById =
                Comparator.comparingLong(OrganisationResource::getId);
        Supplier<SortedSet<OrganisationResource>> supplier = () -> new TreeSet<>(compareById);

        return userApplicationRoles.stream()
                .filter(uar -> uar.getRoleName().equals(UserApplicationRole.LEAD_APPLICANT.getRoleName())
                        || uar.getRoleName().equals(UserApplicationRole.COLLABORATOR.getRoleName()))
                .map(uar -> organisationRestService.getOrganisationById(uar.getOrganisationId()).getSuccess())
                .collect(Collectors.toCollection(supplier));
    }

    private String getProjectSummary(ReviewResource reviewResource) {
        FormInputResponseResource formInputResponseResource = formInputResponseRestService.getByApplicationIdAndQuestionName(
                reviewResource.getApplication(), "Project summary").getSuccess();
        return formInputResponseResource.getValue();
    }
}