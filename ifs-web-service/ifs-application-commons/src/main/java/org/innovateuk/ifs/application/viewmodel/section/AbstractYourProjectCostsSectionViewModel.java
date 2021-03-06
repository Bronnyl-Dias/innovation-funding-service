package org.innovateuk.ifs.application.viewmodel.section;

import org.innovateuk.ifs.applicant.resource.ApplicantQuestionResource;
import org.innovateuk.ifs.applicant.resource.ApplicantSectionResource;
import org.innovateuk.ifs.application.viewmodel.NavigationViewModel;
import org.innovateuk.ifs.application.viewmodel.forminput.AbstractFormInputViewModel;
import org.innovateuk.ifs.form.resource.QuestionResource;

import java.util.List;
import java.util.Optional;

import static org.innovateuk.ifs.competition.resource.ApplicationFinanceType.STANDARD_WITH_VAT;
import static org.innovateuk.ifs.competition.resource.CompetitionStatus.OPEN;

/**
 * Abstract view model for project costs section.
 */
public abstract class AbstractYourProjectCostsSectionViewModel extends AbstractSectionViewModel {
    private List<ApplicantQuestionResource> costQuestions;
    private ApplicantQuestionResource applicantQuestion;
    private boolean complete;

    public AbstractYourProjectCostsSectionViewModel(ApplicantSectionResource applicantResource, List<AbstractFormInputViewModel> formInputViewModels, NavigationViewModel navigationViewModel, boolean allReadOnly, Optional<Long> applicantOrganisationId, boolean readOnlyAllApplicantApplicationFinances) {
        super(applicantResource, formInputViewModels, navigationViewModel, allReadOnly, applicantOrganisationId, readOnlyAllApplicantApplicationFinances);
    }

    public abstract String getFinanceView();

    public List<ApplicantQuestionResource> getCostQuestions() {
        return costQuestions;
    }

    public void setCostQuestions(List<ApplicantQuestionResource> costQuestions) {
        this.costQuestions = costQuestions;
    }

    public ApplicantQuestionResource getApplicantQuestion() {
        return applicantQuestion;
    }

    public void setApplicantQuestion(ApplicantQuestionResource applicantQuestion) {
        this.applicantQuestion = applicantQuestion;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public QuestionResource getQuestion() {
        return applicantQuestion.getQuestion();
    }

    public boolean getShowTerms() {
        return !(getCompetition().showJesFinances(getCurrentApplicant().getOrganisation().getOrganisationType()) ||
                isAllReadOnly() ||
                getApplication().getCompetitionStatus().isLaterThan(OPEN));
    }

    public boolean isIncludeVat() {
        return STANDARD_WITH_VAT.equals(this.getCompetition().getApplicationFinanceType());
    }
}

