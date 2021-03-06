package org.innovateuk.ifs.application.populator.section;

import org.innovateuk.ifs.applicant.resource.ApplicantSectionResource;
import org.innovateuk.ifs.application.populator.ApplicationNavigationPopulator;
import org.innovateuk.ifs.application.populator.forminput.FormInputViewModelGenerator;
import org.innovateuk.ifs.application.resource.QuestionStatusResource;
import org.innovateuk.ifs.application.service.QuestionRestService;
import org.innovateuk.ifs.application.service.QuestionService;
import org.innovateuk.ifs.application.service.SectionService;
import org.innovateuk.ifs.application.viewmodel.section.YourFundingSectionViewModel;
import org.innovateuk.ifs.finance.service.GrantClaimMaximumRestService;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.resource.QuestionResource;
import org.innovateuk.ifs.form.resource.SectionResource;
import org.innovateuk.ifs.form.resource.SectionType;
import org.innovateuk.ifs.organisation.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.service.OrganisationService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.innovateuk.ifs.competition.resource.CompetitionStatus.OPEN;
import static org.innovateuk.ifs.question.resource.QuestionSetupType.RESEARCH_CATEGORY;

/**
 * Your funding populator section view models.
 */
@Component
public class YourFundingSectionPopulator extends AbstractSectionPopulator<YourFundingSectionViewModel> {

    private final SectionService sectionService;
    private final QuestionService questionService;
    private final QuestionRestService questionRestService;
    private final FormInputViewModelGenerator formInputViewModelGenerator;
    private final OrganisationService organisationService;
    private final GrantClaimMaximumRestService grantClaimMaximumRestService;

    public YourFundingSectionPopulator(final ApplicationNavigationPopulator navigationPopulator,
                                       final SectionService sectionService, final QuestionService questionService,
                                       final QuestionRestService questionRestService,
                                       final FormInputViewModelGenerator formInputViewModelGenerator,
                                       final OrganisationService organisationService,
                                       final GrantClaimMaximumRestService grantClaimMaximumRestService) {
        super(navigationPopulator);
        this.sectionService = sectionService;
        this.questionService = questionService;
        this.questionRestService = questionRestService;
        this.formInputViewModelGenerator = formInputViewModelGenerator;
        this.organisationService = organisationService;
        this.grantClaimMaximumRestService = grantClaimMaximumRestService;
    }

    @Override
    protected void populateNoReturn(ApplicantSectionResource section, ApplicationForm form,
                                    YourFundingSectionViewModel viewModel, Model model, BindingResult bindingResult,
                                    Boolean readOnly, Optional<Long> applicantOrganisationId) {
        List<Long> completedSectionIds = sectionService.getCompleted(section.getApplication().getId(), section
                .getCurrentApplicant().getOrganisation().getId());
        viewModel.setComplete(completedSectionIds.contains(section.getSection().getId()));

        Long researchCategoryQuestionId = getResearchCategoryQuestionId(section);
        boolean researchCategoryRequired = isResearchCategoryRequired(section, researchCategoryQuestionId);
        viewModel.setResearchCategoryQuestionId(researchCategoryQuestionId);
        viewModel.setResearchCategoryRequired(researchCategoryRequired);

        long yourOrganisationSectionId = getYourOrganisationSectionId(section);
        boolean yourOrganisationRequired = !completedSectionIds.contains(yourOrganisationSectionId);
        viewModel.setYourOrganisationSectionId(yourOrganisationSectionId);
        viewModel.setYourOrganisationRequired(yourOrganisationRequired);

        viewModel.setFundingSectionLocked(isFundingSectionLocked(section, researchCategoryRequired,
                yourOrganisationRequired));
    }

    private Long getResearchCategoryQuestionId(ApplicantSectionResource section) {
        return questionRestService.getQuestionByCompetitionIdAndQuestionSetupType(section.getCompetition().getId(),
                RESEARCH_CATEGORY).handleSuccessOrFailure(failure -> null, QuestionResource::getId);
    }

    private boolean isResearchCategoryRequired(ApplicantSectionResource section, Long researchCategoryQuestionId) {
        return researchCategoryQuestionId != null && !isResearchCategoryComplete(section, researchCategoryQuestionId);
    }

    private boolean isResearchCategoryComplete(ApplicantSectionResource section, long questionId) {
        long applicationId = section.getApplication().getId();
        long applicantOrganisationId = section.getCurrentApplicant().getOrganisation().getId();

        return questionIsComplete(applicationId, applicantOrganisationId, questionId);
    }

    private boolean questionIsComplete(long applicationId, long organisationId, long questionId) {
        Map<Long, QuestionStatusResource> questionStatuses = questionService
                .getQuestionStatusesForApplicationAndOrganisation(applicationId, organisationId);
        QuestionStatusResource questionStatus = questionStatuses.get(questionId);
        return questionStatus != null && questionStatus.getMarkedAsComplete();
    }

    private long getYourOrganisationSectionId(ApplicantSectionResource section) {
        SectionResource yourOrganisationSection = sectionService.getOrganisationFinanceSection(
                section.getCompetition().getId());
        return yourOrganisationSection.getId();
    }

    private boolean isFundingSectionLocked(ApplicantSectionResource section,
                                           boolean researchCategoryRequired,
                                           boolean yourOrganisationRequired) {
        boolean fieldsRequired = researchCategoryRequired || yourOrganisationRequired;
        return fieldsRequired && isCompetitionOpen(section) && isOrganisationTypeBusiness(section) &&
                !isMaximumFundingLevelOverridden(section);
    }

    private boolean isCompetitionOpen(ApplicantSectionResource section) {
        return !section.getCompetition().getCompetitionStatus().isLaterThan(OPEN);
    }

    private boolean isOrganisationTypeBusiness(ApplicantSectionResource section) {
        return organisationService.getOrganisationType(section.getCurrentUser().getId(),
                section.getApplication().getId()).equals(OrganisationTypeEnum.BUSINESS.getId());
    }

    private boolean isMaximumFundingLevelOverridden(ApplicantSectionResource section) {
        return grantClaimMaximumRestService.isMaximumFundingLevelOverridden(section.getCompetition().getId()).getSuccess();
    }

    @Override
    protected YourFundingSectionViewModel createNew(ApplicantSectionResource section, ApplicationForm form, Boolean
            readOnly, Optional<Long> applicantOrganisationId, Boolean readOnlyAllApplicantApplicationFinances) {
        List<Long> completedSectionIds = sectionService.getCompleted(section.getApplication().getId(), section
                .getCurrentApplicant().getOrganisation().getId());
        return new YourFundingSectionViewModel(
                section,
                formInputViewModelGenerator.fromSection(section, section, form, readOnly),
                getNavigationViewModel(section),
                readOnly || completedSectionIds.contains(section.getSection().getId()),
                applicantOrganisationId,
                readOnlyAllApplicantApplicationFinances);
    }

    @Override
    public SectionType getSectionType() {
        return SectionType.FUNDING_FINANCES;
    }
}
