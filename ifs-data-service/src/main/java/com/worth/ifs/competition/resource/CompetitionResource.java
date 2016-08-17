package com.worth.ifs.competition.resource;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.worth.ifs.application.resource.ApplicationResource;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class CompetitionResource {
    public static final ChronoUnit CLOSING_SOON_CHRONOUNIT = ChronoUnit.HOURS;
    public static final int CLOSING_SOON_AMOUNT = 3;
    private static final DateTimeFormatter ASSESSMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM YYYY");
    private static final DateTimeFormatter START_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/YYYY");

    private Long id;
    private List<Long> applications = new ArrayList<>();
    private List<Long> questions = new ArrayList<>();
    private List<Long> sections = new ArrayList<>();
    private List<Long> milestones = new ArrayList<>();
    private List<CompetitionCoFunderResource> coFunders = new ArrayList<>();

    private String name;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime assessmentStartDate;
    private LocalDateTime assessmentEndDate;
    private LocalDateTime fundersPanelEndDate;
    private LocalDateTime assessorFeedbackDate;
    private Status competitionStatus;
    @Min(0)
    @Max(100)
    private Integer maxResearchRatio;
    @Min(0)
    @Max(100)
    private Integer academicGrantPercentage;
    private Long competitionType;
    private String competitionTypeName;
    private Long executive;
    private Long leadTechnologist;
    private Long innovationSector;
    private String innovationSectorName;
    private Long innovationArea;
    private String innovationAreaName;

    private String pafCode;
    private String budgetCode;
    private String code;

    private boolean multiStream;
    private String streamName;
    private CollaborationLevel collaborationLevel;
    private LeadApplicantType leadApplicantType;
    private Set<Long> researchCategories;

    private Map<CompetitionSetupSection, Boolean> sectionSetupStatus = new HashMap<>();

    private String activityCode;
    private String innovateBudget;
    private String funder;
    private BigDecimal funderBudget;


    public CompetitionResource() {
        // no-arg constructor
    }

    public CompetitionResource(Long id, List<Long> applications, List<Long> questions, List<Long> sections, String name, String description, LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.applications = applications;
        this.questions = questions;
        this.sections = sections;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public CompetitionResource(long id, String name, String description, LocalDateTime startDate, LocalDateTime endDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @JsonIgnore
    public boolean isOpen() {
        return Status.OPEN.equals(competitionStatus);
    }

    public Status getCompetitionStatus() {
        return competitionStatus;
    }

    public void setCompetitionStatus(Status competitionStatus) {
        this.competitionStatus = competitionStatus;
    }

    public List<Long> getSections() {
        return sections;
    }

    public void setSections(List<Long> sections) {
        this.sections = sections;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addApplication(ApplicationResource... apps) {
        if (applications == null) {
            applications = new ArrayList<>();
        }
        this.applications.addAll(Arrays.asList(apps).stream().map(ApplicationResource::getId).collect(Collectors.toList()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getAssessmentEndDate() {
        return assessmentEndDate;
    }

    public String assementEndDateDisplay() {
        if (getAssessmentEndDate() != null) {
            return getAssessmentEndDate().format(ASSESSMENT_DATE_FORMAT);
        }
        return "";
    }

    public String startDateDisplay() {
        if (getStartDate() != null) {
            return getStartDate().format(START_DATE_FORMAT);
        }
        return "";
    }

    public void setAssessmentEndDate(LocalDateTime assessmentEndDate) {
        this.assessmentEndDate = assessmentEndDate;
    }

    public LocalDateTime getAssessmentStartDate() {
        return assessmentStartDate;
    }

    public void setAssessmentStartDate(LocalDateTime assessmentStartDate) {
        this.assessmentStartDate = assessmentStartDate;
    }

    public LocalDateTime getAssessorFeedbackDate() {
        return assessorFeedbackDate;
    }

    public void setAssessorFeedbackDate(LocalDateTime assessorFeedbackDate) {
        this.assessorFeedbackDate = assessorFeedbackDate;
    }

    @JsonIgnore
    public long getDaysLeft() {
        return getDaysBetween(LocalDateTime.now(), this.endDate);
    }

    @JsonIgnore
    public long getAssessmentDaysLeft() {
        return getDaysBetween(LocalDateTime.now(), this.assessmentEndDate);
    }

    @JsonIgnore
    public long getTotalDays() {
        return getDaysBetween(this.startDate, this.endDate);
    }

    @JsonIgnore
    public boolean isClosingSoon() {
        long hoursToGo = CLOSING_SOON_CHRONOUNIT.between(LocalDateTime.now(), this.endDate);
        return isOpen() && hoursToGo < CLOSING_SOON_AMOUNT;
    }


    /* Keep it D.R.Y */

    @JsonIgnore
    public long getAssessmentTotalDays() {
        return getDaysBetween(this.assessmentStartDate, this.assessmentEndDate);
    }

    @JsonIgnore
    public long getStartDateToEndDatePercentage() {
        return getDaysLeftPercentage(getDaysLeft(), getTotalDays());
    }

    @JsonIgnore
    public long getAssessmentDaysLeftPercentage() {
        return getDaysLeftPercentage(getAssessmentDaysLeft(), getAssessmentTotalDays());
    }

    @JsonIgnore
    public List<Long> getApplications() {
        return applications;
    }

    public void setApplications(List<Long> applications) {
        this.applications = applications;
    }

    @JsonIgnore
    public List<Long> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Long> questions) {
        this.questions = questions;
    }

    private long getDaysBetween(LocalDateTime dateA, LocalDateTime dateB) {
        return ChronoUnit.DAYS.between(dateA, dateB);
    }

    private long getDaysLeftPercentage(long daysLeft, long totalDays) {
        if (daysLeft <= 0) {
            return 100;
        }
        double deadlineProgress = 100 - (((double) daysLeft / (double) totalDays) * 100);
        long startDateToEndDatePercentage = (long) deadlineProgress;
        return startDateToEndDatePercentage;
    }

    public Integer getMaxResearchRatio() {
        return maxResearchRatio;
    }

    public void setMaxResearchRatio(Integer maxResearchRatio) {
        this.maxResearchRatio = maxResearchRatio;
    }

    public Integer getAcademicGrantPercentage() {
        return academicGrantPercentage;
    }

    public void setAcademicGrantPercentage(Integer academicGrantPercentage) {
        this.academicGrantPercentage = academicGrantPercentage;
    }

    public LocalDateTime getFundersPanelEndDate() {
        return fundersPanelEndDate;
    }

    public void setFundersPanelEndDate(LocalDateTime fundersPanelEndDate) {
        this.fundersPanelEndDate = fundersPanelEndDate;
    }

    public Long getExecutive() {
        return executive;
    }

    public void setExecutive(Long executive) {
        this.executive = executive;
    }

    public Long getLeadTechnologist() {
        return leadTechnologist;
    }

    public void setLeadTechnologist(Long leadTechnologist) {
        this.leadTechnologist = leadTechnologist;
    }

    public String getPafCode() {
        return pafCode;
    }

    public void setPafCode(String pafCode) {
        this.pafCode = pafCode;
    }

    public String getBudgetCode() {
        return budgetCode;
    }

    public void setBudgetCode(String budgetCode) {
        this.budgetCode = budgetCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getCompetitionType() {
        return competitionType;
    }

    public void setCompetitionType(Long competitionType) {
        this.competitionType = competitionType;
    }

    public String getCompetitionTypeName() {
        return competitionTypeName;
    }

    public void setCompetitionTypeName(String competitionTypeName) { this.competitionTypeName = competitionTypeName; }

    public Long getInnovationSector() {
        return innovationSector;
    }

    public void setInnovationSector(Long innovationSector) {
        this.innovationSector = innovationSector;
    }

    public Long getInnovationArea() {
        return innovationArea;
    }

    public void setInnovationArea(Long innovationArea) {
        this.innovationArea = innovationArea;
    }

    public String getInnovationSectorName() {
        return innovationSectorName;
    }

    public void setInnovationSectorName(String innovationSectorName) {
        this.innovationSectorName = innovationSectorName;
    }

    public String getInnovationAreaName() {
        return innovationAreaName;
    }

    public void setInnovationAreaName(String innovationAreaName) {
        this.innovationAreaName = innovationAreaName;
    }

    public Set<Long> getResearchCategories() {
        return researchCategories;
    }

    public void setResearchCategories(Set<Long> researchCategories) {
        this.researchCategories = researchCategories;
    }

    public List<Long> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<Long> milestones) {
        this.milestones = milestones;
    }

    public boolean isMultiStream() {
        return multiStream;
    }

    public void setMultiStream(boolean multiStream) {
        this.multiStream = multiStream;
    }

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public CollaborationLevel getCollaborationLevel() {
        return collaborationLevel;
    }

    public void setCollaborationLevel(CollaborationLevel collaborationLevel) {
        this.collaborationLevel = collaborationLevel;
    }

    public LeadApplicantType getLeadApplicantType() {
        return leadApplicantType;
    }

    public void setLeadApplicantType(LeadApplicantType leadApplicantType) {
        this.leadApplicantType = leadApplicantType;
    }

    public Map<CompetitionSetupSection, Boolean> getSectionSetupStatus() {
        return sectionSetupStatus;
    }

    public void setSectionSetupStatus(Map<CompetitionSetupSection, Boolean> sectionSetupStatus) {
        this.sectionSetupStatus = sectionSetupStatus;
    }

    public enum Status {
        COMPETITION_SETUP,COMPETITION_SETUP_FINISHED,NOT_STARTED,READY_TO_OPEN,OPEN,CLOSED,IN_ASSESSMENT,FUNDERS_PANEL,ASSESSOR_FEEDBACK,PROJECT_SETUP
    }

    public String getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(String activityCode) {
        this.activityCode = activityCode;
    }

    public String getInnovateBudget() {
        return innovateBudget;
    }

    public void setInnovateBudget(String innovateBudget) {
        this.innovateBudget = innovateBudget;
    }

    public String getFunder() {
        return funder;
    }

    public void setFunder(String funder) {
        this.funder = funder;
    }

    public BigDecimal getFunderBudget() {
        return funderBudget;
    }

    public void setFunderBudget(BigDecimal funderBudget) {
        this.funderBudget = funderBudget;
    }

    public List<CompetitionCoFunderResource> getCoFunders() {
        return coFunders;
    }

    public void setCoFunders(List<CompetitionCoFunderResource> coFunders) {
        this.coFunders = coFunders;
    }
}