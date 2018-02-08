package org.innovateuk.ifs.assessment.review.populator;

import org.innovateuk.ifs.application.UserApplicationRole;
import org.innovateuk.ifs.assessment.review.resource.AssessmentReviewResource;
import org.innovateuk.ifs.assessment.review.viewmodel.AssessmentReviewViewModel;
import org.innovateuk.ifs.assessment.service.AssessmentPanelRestService;
import org.innovateuk.ifs.form.resource.FormInputResponseResource;
import org.innovateuk.ifs.form.service.FormInputResponseRestService;
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
        AssessmentReviewResource assessmentReviewResource =
                assessmentPanelRestService.getAssessmentReview(reviewId).getSuccessObjectOrThrowException();

        String projectSummary = getProjectSummary(assessmentReviewResource);
        List<ProcessRoleResource> processRoles = processRoleService.findProcessRolesByApplicationId(assessmentReviewResource.getApplication());
        SortedSet<OrganisationResource> collaborators = getApplicationOrganisations(processRoles);
        OrganisationResource leadPartner = getApplicationLeadOrganisation(processRoles).orElse(null);

        return new AssessmentReviewViewModel(
                reviewId,
                assessmentReviewResource.getCompetition(),
                assessmentReviewResource.getApplicationName(),
                collaborators,
                leadPartner,
                projectSummary);
    }

    private Optional<OrganisationResource> getApplicationLeadOrganisation(List<ProcessRoleResource> userApplicationRoles) {
        return userApplicationRoles.stream()
                .filter(uar -> uar.getRoleName().equals(UserApplicationRole.LEAD_APPLICANT.getRoleName()))
                .map(uar -> organisationRestService.getOrganisationById(uar.getOrganisationId()).getSuccessObjectOrThrowException())
                .findFirst();
    }

    private SortedSet<OrganisationResource> getApplicationOrganisations(List<ProcessRoleResource> userApplicationRoles) {
        Comparator<OrganisationResource> compareById =
                Comparator.comparingLong(OrganisationResource::getId);
        Supplier<SortedSet<OrganisationResource>> supplier = () -> new TreeSet<>(compareById);

        return userApplicationRoles.stream()
                .filter(uar -> uar.getRoleName().equals(UserApplicationRole.LEAD_APPLICANT.getRoleName())
                        || uar.getRoleName().equals(UserApplicationRole.COLLABORATOR.getRoleName()))
                .map(uar -> organisationRestService.getOrganisationById(uar.getOrganisationId()).getSuccessObjectOrThrowException())
                .collect(Collectors.toCollection(supplier));
    }

    private String getProjectSummary(AssessmentReviewResource assessmentReviewResource) {
        FormInputResponseResource formInputResponseResource = formInputResponseRestService.getByApplicationIdAndQuestionName(
                assessmentReviewResource.getApplication(), "Project summary").getSuccessObjectOrThrowException();
        return formInputResponseResource.getValue();
    }
}