package org.innovateuk.ifs.assessment.feedback.viewmodel;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.innovateuk.ifs.assessment.resource.AssessmentResource;
import org.innovateuk.ifs.category.resource.ResearchCategoryResource;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.file.controller.viewmodel.FileDetailsViewModel;
import org.innovateuk.ifs.form.resource.FormInputResource;
import org.innovateuk.ifs.form.resource.QuestionResource;

import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.lowerCase;

/**
 * Holder of model attributes for the feedback given as part of the assessment journey to a question for an application.
 */
public class AssessmentFeedbackViewModel extends BaseAssessmentFeedbackViewModel {

    private final long assessmentId;
    private final long daysLeft;
    private final long daysLeftPercentage;
    private final long applicationId;
    private final String applicationName;
    private final long questionId;
    private final String questionNumber;
    private final String questionShortName;
    private final String questionName;
    private final Integer maximumScore;
    private final String applicantResponse;
    private final List<FormInputResource> assessmentFormInputs;
    private final boolean scoreFormInputExists;
    private final boolean scopeFormInputExists;
    private final boolean appendixExists;
    private final FileDetailsViewModel appendixDetails;
    private final List<ResearchCategoryResource> researchCategories;

    private AssessmentFeedbackViewModel(long assessmentId,
                                       long daysLeft,
                                       long daysLeftPercentage,
                                       long applicationId,
                                       String applicationName,
                                       long questionId,
                                       String questionNumber,
                                       String questionShortName,
                                       String questionName,
                                       Integer maximumScore,
                                       String applicantResponse,
                                       List<FormInputResource> assessmentFormInputs,
                                       boolean scoreFormInputExists,
                                       boolean scopeFormInputExists,
                                       boolean appendixExists,
                                       FileDetailsViewModel appendixDetails,
                                       List<ResearchCategoryResource> researchCategories) {
        this.assessmentId = assessmentId;
        this.daysLeft = daysLeft;
        this.daysLeftPercentage = daysLeftPercentage;
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.questionId = questionId;
        this.questionNumber = questionNumber;
        this.questionShortName = questionShortName;
        this.questionName = questionName;
        this.maximumScore = maximumScore;
        this.applicantResponse = applicantResponse;
        this.assessmentFormInputs = assessmentFormInputs;
        this.scoreFormInputExists = scoreFormInputExists;
        this.scopeFormInputExists = scopeFormInputExists;
        this.appendixExists = appendixExists;
        this.appendixDetails = appendixDetails;
        this.researchCategories = researchCategories;
    }

    public AssessmentFeedbackViewModel(AssessmentResource assessment, CompetitionResource competition, QuestionResource question, String applicantResponse,
                                       List<FormInputResource> assessmentFormInputs,
                                       boolean scoreFormInputExists,
                                       boolean scopeFormInputExists,
                                       FileDetailsViewModel appendixDetails,  List<ResearchCategoryResource> researchCategories
                                       ) {
            this(assessment.getId(),
                    competition.getAssessmentDaysLeft(),
                    competition.getAssessmentDaysLeftPercentage(),
                    assessment.getApplication(),
                    assessment.getApplicationName(),
                    question.getId(),
                    question.getQuestionNumber(),
                    question.getShortName(),
                    question.getName(),
                    question.getAssessorMaximumScore(),
                    applicantResponse,
                    assessmentFormInputs,
                    scoreFormInputExists,
                    scopeFormInputExists,
                    appendixDetails != null,
                    appendixDetails,
                    researchCategories);
    }

    public long getAssessmentId() {
        return assessmentId;
    }

    public long getDaysLeft() {
        return daysLeft;
    }

    public long getDaysLeftPercentage() {
        return daysLeftPercentage;
    }

    public long getApplicationId() {
        return applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public long getQuestionId() {
        return questionId;
    }

    public String getQuestionNumber() {
        return questionNumber;
    }

    public String getQuestionShortName() {
        return questionShortName;
    }

    public String getQuestionName() {
        return questionName;
    }

    public Integer getMaximumScore() {
        return maximumScore;
    }

    public String getApplicantResponse() {
        return applicantResponse;
    }

    public List<FormInputResource> getAssessmentFormInputs() {
        return assessmentFormInputs;
    }

    public boolean isScoreFormInputExists() {
        return scoreFormInputExists;
    }

    public boolean isScopeFormInputExists() {
        return scopeFormInputExists;
    }

    public boolean isAppendixExists() {
        return appendixExists;
    }

    public FileDetailsViewModel getAppendixDetails() {
        return appendixDetails;
    }

    public List<ResearchCategoryResource> getResearchCategories() {
        return researchCategories;
    }

    public String getAppendixFileDescription() {
        return format("View %s appendix", lowerCase(getQuestionShortName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AssessmentFeedbackViewModel that = (AssessmentFeedbackViewModel) o;

        return new EqualsBuilder()
                .append(assessmentId, that.assessmentId)
                .append(daysLeft, that.daysLeft)
                .append(daysLeftPercentage, that.daysLeftPercentage)
                .append(applicationId, that.applicationId)
                .append(questionId, that.questionId)
                .append(scoreFormInputExists, that.scoreFormInputExists)
                .append(scopeFormInputExists, that.scopeFormInputExists)
                .append(appendixExists, that.appendixExists)
                .append(applicationName, that.applicationName)
                .append(questionNumber, that.questionNumber)
                .append(questionShortName, that.questionShortName)
                .append(questionName, that.questionName)
                .append(maximumScore, that.maximumScore)
                .append(applicantResponse, that.applicantResponse)
                .append(assessmentFormInputs, that.assessmentFormInputs)
                .append(appendixDetails, that.appendixDetails)
                .append(researchCategories, that.researchCategories)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(assessmentId)
                .append(daysLeft)
                .append(daysLeftPercentage)
                .append(applicationId)
                .append(applicationName)
                .append(questionId)
                .append(questionNumber)
                .append(questionShortName)
                .append(questionName)
                .append(maximumScore)
                .append(applicantResponse)
                .append(assessmentFormInputs)
                .append(scoreFormInputExists)
                .append(scopeFormInputExists)
                .append(appendixExists)
                .append(appendixDetails)
                .append(researchCategories)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("assessmentId", assessmentId)
                .append("daysLeft", daysLeft)
                .append("daysLeftPercentage", daysLeftPercentage)
                .append("applicationId", applicationId)
                .append("applicationName", applicationName)
                .append("questionId", questionId)
                .append("questionNumber", questionNumber)
                .append("questionShortName", questionShortName)
                .append("questionName", questionName)
                .append("maximumScore", maximumScore)
                .append("applicantResponse", applicantResponse)
                .append("assessmentFormInputs", assessmentFormInputs)
                .append("scoreFormInputExists", scoreFormInputExists)
                .append("scopeFormInputExists", scopeFormInputExists)
                .append("appendixExists", appendixExists)
                .append("appendixDetails", appendixDetails)
                .append("researchCategories", researchCategories)
                .toString();
    }
}