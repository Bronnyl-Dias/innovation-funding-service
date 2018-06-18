package org.innovateuk.ifs.application.areas.viewmodel;

import org.innovateuk.ifs.category.resource.ResearchCategoryResource;

import java.util.List;

/**
 * View Model for a Research category selection overview.
 */
public class ResearchCategoryViewModel {

    private String currentCompetitionName;
    private Long applicationId;
    private Long questionId;
    private List<ResearchCategoryResource> availableResearchCategories;
    private boolean hasApplicationFinances;

    public List<ResearchCategoryResource> getAvailableResearchCategories() {
        return availableResearchCategories;
    }

    public void setAvailableResearchCategories(List<ResearchCategoryResource> availableResearchCategories) {
        this.availableResearchCategories = availableResearchCategories;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
    public String getCurrentCompetitionName() {
        return currentCompetitionName;
    }

    public void setCurrentCompetitionName(String currentCompetitionName) {
        this.currentCompetitionName = currentCompetitionName;
    }

    public boolean getHasApplicationFinances() {
        return hasApplicationFinances;
    }

    public void setHasApplicationFinances(boolean hasApplicationFinances) {
        this.hasApplicationFinances = hasApplicationFinances;
    }
}
