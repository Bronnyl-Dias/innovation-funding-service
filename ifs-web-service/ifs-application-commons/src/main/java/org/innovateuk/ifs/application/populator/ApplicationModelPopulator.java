package org.innovateuk.ifs.application.populator;

import org.innovateuk.ifs.application.finance.view.ApplicationFinanceOverviewModelManager;
import org.innovateuk.ifs.application.finance.view.FinanceViewHandlerProvider;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.application.service.QuestionRestService;
import org.innovateuk.ifs.application.service.QuestionService;
import org.innovateuk.ifs.application.service.SectionService;
import org.innovateuk.ifs.assessment.service.AssessmentRestService;
import org.innovateuk.ifs.assessment.service.AssessorFormInputResponseRestService;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.service.CompetitionRestService;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.Form;
import org.innovateuk.ifs.form.resource.QuestionResource;
import org.innovateuk.ifs.form.resource.QuestionType;
import org.innovateuk.ifs.form.resource.SectionResource;
import org.innovateuk.ifs.organisation.resource.OrganisationResource;
import org.innovateuk.ifs.user.resource.ProcessRoleResource;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.innovateuk.ifs.util.CollectionFunctions.simpleFindFirst;

@Component
public class ApplicationModelPopulator {
    public static final String MODEL_ATTRIBUTE_FORM = "form";

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserRestService userRestService;

    @Autowired
    protected QuestionService questionService;

    @Autowired
    protected QuestionRestService questionRestService;

    @Autowired
    protected ProcessRoleService processRoleService;

    @Autowired
    protected SectionService sectionService;

    @Autowired
    protected ApplicationFinanceOverviewModelManager applicationFinanceOverviewModelManager;

    @Autowired
    protected OrganisationService organisationService;

    @Autowired
    protected OrganisationRestService organisationRestService;

    @Autowired
    protected FinanceViewHandlerProvider financeViewHandlerProvider;

    @Autowired
    protected OrganisationDetailsModelPopulator organisationDetailsModelPopulator;

    @Autowired
    protected AssessorFormInputResponseRestService assessorFormInputResponseRestService;

    @Autowired
    protected AssessmentRestService assessmentRestService;

    @Autowired
    private CompetitionRestService competitionRestService;

    @Autowired
    private ApplicationSectionAndQuestionModelPopulator applicationSectionAndQuestionModelPopulator;

    public ApplicationResource addApplicationAndSections(ApplicationResource application,
                                                         CompetitionResource competition,
                                                         UserResource user,
                                                         Optional<SectionResource> section,
                                                         Optional<Long> currentQuestionId,
                                                         Model model,
                                                         ApplicationForm form,
                                                         List<ProcessRoleResource> userApplicationRoles) {
        return addApplicationAndSections(application, competition, user, section, currentQuestionId, model, form, userApplicationRoles, Optional.empty());
    }

    public ApplicationResource addApplicationWithoutDetails(ApplicationResource application,
                                                            CompetitionResource competition,
                                                            Model model) {

        model.addAttribute("completedQuestionsPercentage", application.getCompletion());
        model.addAttribute("currentApplication", application);
        model.addAttribute("currentCompetition", competition);

        return application;
    }

    public ApplicationResource addApplicationAndSections(ApplicationResource application,
                                                         CompetitionResource competition,
                                                         UserResource user,
                                                         Optional<SectionResource> section,
                                                         Optional<Long> currentQuestionId,
                                                         Model model,
                                                         ApplicationForm form,
                                                         List<ProcessRoleResource> userApplicationRoles,
                                                         Optional<Boolean> markAsCompleteEnabled) {

        application = addApplicationDetails(application, competition, user, section, currentQuestionId, model, form, userApplicationRoles, markAsCompleteEnabled);

        model.addAttribute("completedQuestionsPercentage", application.getCompletion());
        applicationSectionAndQuestionModelPopulator.addSectionDetails(model, section);

        return application;
    }

    private ApplicationResource addApplicationDetails(ApplicationResource application,
                                                     CompetitionResource competition,
                                                     UserResource user,
                                                     Optional<SectionResource> section,
                                                     Optional<Long> currentQuestionId,
                                                     Model model,
                                                     ApplicationForm form,
                                                     List<ProcessRoleResource> userApplicationRoles,
                                                     Optional<Boolean> markAsCompleteEnabled) {
        model.addAttribute("currentApplication", application);
        model.addAttribute("currentCompetition", competition);

        Optional<OrganisationResource> userOrganisation = getUserOrganisation(user.getId(), userApplicationRoles);
        model.addAttribute("userOrganisation", userOrganisation.orElse(null));

        if (form == null) {
            form = new ApplicationForm();
        }
        form.setApplication(application);

        Map<Long, Set<Long>> completedSectionsByOrganisation = sectionService.getCompletedSectionsByOrganisation(application.getId());

        applicationSectionAndQuestionModelPopulator.addQuestionsDetails(model, application, form);
        addUserDetails(model, user, userApplicationRoles);
        addApplicationFormDetailInputs(application, form);
        applicationSectionAndQuestionModelPopulator.addMappedSectionsDetails(model, application, competition, section, userOrganisation, user.getId(), completedSectionsByOrganisation, markAsCompleteEnabled);

        applicationSectionAndQuestionModelPopulator.addAssignableDetails(model, application, userOrganisation.orElse(null), user, section, currentQuestionId);
        applicationSectionAndQuestionModelPopulator.addCompletedDetails(model, application, userOrganisation, completedSectionsByOrganisation);

        model.addAttribute(MODEL_ATTRIBUTE_FORM, form);
        return application;
    }


    private void addApplicationFormDetailInputs(ApplicationResource application, Form form) {
        Map<String, String> formInputs = form.getFormInput();
        formInputs.put("application_details-title", application.getName());
        formInputs.put("application_details-duration", String.valueOf(application.getDurationInMonths()));
        if (application.getStartDate() == null) {
            formInputs.put("application_details-startdate_day", "");
            formInputs.put("application_details-startdate_month", "");
            formInputs.put("application_details-startdate_year", "");
        } else {
            formInputs.put("application_details-startdate_day", String.valueOf(application.getStartDate().getDayOfMonth()));
            formInputs.put("application_details-startdate_month", String.valueOf(application.getStartDate().getMonthValue()));
            formInputs.put("application_details-startdate_year", String.valueOf(application.getStartDate().getYear()));
        }
        form.setFormInput(formInputs);
    }


    void addUserDetails(Model model, UserResource user, List<ProcessRoleResource> userApplicationRoles) {

        ProcessRoleResource leadApplicantProcessRole =
                simpleFindFirst(userApplicationRoles, role -> role.getRoleName().equals(Role.LEADAPPLICANT.getName())).get();

        boolean userIsLeadApplicant = leadApplicantProcessRole.getUser().equals(user.getId());

        UserResource leadApplicant = userIsLeadApplicant ? user : userRestService.retrieveUserById(leadApplicantProcessRole.getUser()).getSuccess();

        model.addAttribute("userIsLeadApplicant", userIsLeadApplicant);
        model.addAttribute("leadApplicant", leadApplicant);
    }

    public boolean userIsLeadApplicant(ApplicationResource application, Long userId) {
        return userService.isLeadApplicant(userId, application);
    }

    void addOrganisationAndUserFinanceDetails(Long competitionId,
                                                     Long applicationId,
                                                     UserResource user,
                                                     Model model,
                                                     ApplicationForm form,
                                                     Long organisationId) {
        model.addAttribute("currentUser", user);
        SectionResource financeSection = sectionService.getFinanceSection(competitionId);
        boolean hasFinanceSection = financeSection != null;

        if (hasFinanceSection) {
            Optional<Long> optionalOrganisationId = Optional.ofNullable(organisationId);
            applicationFinanceOverviewModelManager.addFinanceDetails(model, competitionId, applicationId);

            List<QuestionResource> costsQuestions = questionRestService.getQuestionsBySectionIdAndType(financeSection.getId(), QuestionType.COST).getSuccess();
            // TODO 4781 NOTE: This code is terrible.  It does nothing if none of below two conditions don't match.  This is not my code RB.
            if (!form.isAdminMode() || optionalOrganisationId.isPresent()) {
                Long organisationType = organisationService.getOrganisationType(user.getId(), applicationId);
                financeViewHandlerProvider.getFinanceModelManager(competitionRestService.getCompetitionById(competitionId).getSuccess(), organisationType).addOrganisationFinanceDetails(model, applicationId, costsQuestions, user.getId(), form, organisationId);
            }
        }
    }

    Optional<OrganisationResource> getUserOrganisation(Long userId,
                                                               List<ProcessRoleResource> userApplicationRoles) {

        return userApplicationRoles.stream()
                .filter(uar -> uar.getUser().equals(userId) && uar.getOrganisationId() != null)
                .map(uar -> organisationRestService.getOrganisationById(uar.getOrganisationId()).getSuccess())
                .findFirst();
    }

    public void addApplicationAndSectionsInternalWithOrgDetails(final ApplicationResource application,
                                                                final CompetitionResource competition,
                                                                final UserResource user, final Model model,
                                                                final ApplicationForm form,
                                                                List<ProcessRoleResource> userApplicationRoles,
                                                                final Optional<Boolean> markAsCompleteEnabled) {
        addApplicationAndSectionsInternalWithOrgDetails(application, competition, user, Optional.empty(), Optional.empty(), model, form, userApplicationRoles, markAsCompleteEnabled);
    }

    public void addApplicationAndSectionsInternalWithOrgDetails(final ApplicationResource application,
                                                                final CompetitionResource competition,
                                                                final UserResource user,
                                                                Optional<SectionResource> section,
                                                                Optional<Long> currentQuestionId,
                                                                final Model model,
                                                                final ApplicationForm form,
                                                                List<ProcessRoleResource> userApplicationRoles,
                                                                final Optional<Boolean> markAsCompleteEnabled) {
        organisationDetailsModelPopulator.populateModel(model, application.getId(), userApplicationRoles);
        addApplicationAndSections(application, competition, user, section, currentQuestionId, model, form,
                userApplicationRoles, markAsCompleteEnabled);
    }
}
