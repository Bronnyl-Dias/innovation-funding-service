package com.worth.ifs.competitionsetup.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.CharMatcher;
import com.worth.ifs.application.service.CategoryService;
import com.worth.ifs.application.service.CompetitionService;
import com.worth.ifs.category.resource.CategoryResource;
import com.worth.ifs.commons.error.Error;
import com.worth.ifs.commons.security.UserAuthenticationService;
import com.worth.ifs.competition.resource.CompetitionResource;
import com.worth.ifs.competition.resource.CompetitionResource.Status;
import com.worth.ifs.competition.resource.CompetitionSetupSection;
import com.worth.ifs.competitionsetup.form.*;
import com.worth.ifs.competitionsetup.model.Question;
import com.worth.ifs.competitionsetup.model.Funder;
import com.worth.ifs.competitionsetup.service.CompetitionSetupMilestoneService;
import com.worth.ifs.competitionsetup.service.CompetitionSetupQuestionService;
import com.worth.ifs.competitionsetup.service.CompetitionSetupService;
import com.worth.ifs.profiling.ProfileExecution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Controller for showing and handling the different competition setup sections
 */
@Controller
@RequestMapping("/competition/setup")
public class CompetitionSetupController {

    private static final Log LOG = LogFactory.getLog(CompetitionSetupController.class);
    private static final String COMPETITION_ID_KEY = "competitionId";
    private static final String COMPETITION_SETUP_FORM_KEY = "competitionSetupForm";
    private static final String SECTION_PATH_KEY = "sectionPath";

    @Autowired
    private UserAuthenticationService userAuthenticationService;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private CompetitionSetupService competitionSetupService;

    @Autowired
    private CompetitionSetupQuestionService competitionSetupQuestionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CompetitionSetupMilestoneService competitionSetupMilestoneService;

    @Autowired
    private Validator validator;

    public static final String READY_TO_OPEN_KEY = "readyToOpen";

    @RequestMapping(value = "/{competitionId}", method = RequestMethod.GET)
    public String initCompetitionSetupSection(Model model, @PathVariable(COMPETITION_ID_KEY) Long competitionId) {

        CompetitionResource competition = competitionService.getById(competitionId);

        if(isSendToDashboard(competition)) {
        	LOG.error("Competition is not found in setup state");
            return "redirect:/dashboard";
        }

        CompetitionSetupSection section = CompetitionSetupSection.fromPath("home");
        competitionSetupService.populateCompetitionSectionModelAttributes(model, competition, section);

        model.addAttribute(READY_TO_OPEN_KEY, competitionSetupService.isCompetitionReadyToOpen(competition));
        return "competition/setup";
    }

    @RequestMapping(value = "/{competitionId}/section/{sectionPath}/edit", method = RequestMethod.POST)
    public String setSectionAsIncomplete(@PathVariable(COMPETITION_ID_KEY) Long competitionId, @PathVariable(SECTION_PATH_KEY) String sectionPath) {

    	CompetitionSetupSection section = CompetitionSetupSection.fromPath(sectionPath);
    	if(section == null) {
    		LOG.error("Invalid section path specified: " + sectionPath);
            return "redirect:/dashboard";
    	}

        competitionService.setSetupSectionMarkedAsIncomplete(competitionId, section);
        competitionSetupService.setCompetitionAsCompetitionSetup(competitionId);

        return "redirect:/competition/setup/" + competitionId + "/section/" + section.getPath();
    }

    @RequestMapping(value = "/{competitionId}/section/application/question/{questionId}", method = RequestMethod.GET)
    public String editCompetitionSetupApplication(@PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              @PathVariable("questionId") Long questionId,
                                              Model model) {

    	CompetitionSetupSection section = CompetitionSetupSection.APPLICATION_FORM;
        CompetitionResource competition = competitionService.getById(competitionId);

        if(isSendToDashboard(competition)) {
            LOG.error("Competition is not found in setup state");
            return "redirect:/dashboard";
        }

        competitionSetupService.populateCompetitionSectionModelAttributes(model, competition, section);
        ApplicationFormForm competitionSetupForm = (ApplicationFormForm) competitionSetupService.getSectionFormData(competition, section);

        addQuestionToUpdate(questionId, model, competitionSetupForm);
        model.addAttribute("competitionSetupForm", competitionSetupForm);

        return "competition/setup";
    }

    @RequestMapping(value = "/{competitionId}/section/{sectionPath}", method = RequestMethod.GET)
    public String editCompetitionSetupSection(@PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              @PathVariable(SECTION_PATH_KEY) String sectionPath,
                                              Model model) {

        CompetitionSetupSection section = CompetitionSetupSection.fromPath(sectionPath);
        if(section == null) {
            LOG.error("Invalid section path specified: " + sectionPath);
            return "redirect:/dashboard";
        }

        CompetitionResource competition = competitionService.getById(competitionId);

        if(isSendToDashboard(competition)) {
        	LOG.error("Competition is not found in setup state");
            return "redirect:/dashboard";
        }

        competitionSetupService.populateCompetitionSectionModelAttributes(model, competition, section);
        model.addAttribute("competitionSetupForm", competitionSetupService.getSectionFormData(competition, section));

        if(model.containsAttribute("isInitialComplete")) {
            Map<String, Object> modelMap = model.asMap();

            if(!(Boolean) modelMap.get("isInitialComplete") && !section.equals(CompetitionSetupSection.INITIAL_DETAILS)) {
                LOG.error("User should first fill fill the initial details");
                return "redirect:/dashboard";
            }
        }

        return "competition/setup";
    }

    /**
     * This method is for supporting ajax saving from the application form.
     */
    @ProfileExecution
    @RequestMapping(value = "/{competitionId}/section/{sectionPath}/saveFormElement", method = RequestMethod.POST)
    @ResponseBody
    public JsonNode saveFormElement(@RequestParam("fieldName") String fieldName,
                                    @RequestParam("value") String value,
                                    @RequestParam(name = "objectId", required = false) Long objectId,
                                    @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                    @PathVariable(SECTION_PATH_KEY) String sectionPath,
                                    HttpServletRequest request) {

        CompetitionResource competitionResource = competitionService.getById(competitionId);
        CompetitionSetupSection section = CompetitionSetupSection.fromPath(sectionPath);

        List<String> errors = new ArrayList<>();
        try {
            errors = toStringList(competitionSetupService.autoSaveCompetitionSetupSection(competitionResource, section, fieldName, value, Optional.ofNullable(objectId)));

            return this.createJsonObjectNode(errors.isEmpty(), errors);
        } catch (Exception e) {
            errors.add(e.getMessage());
            return this.createJsonObjectNode(false, errors);
        }
    }

    private List<String> toStringList(List<Error> errors) {
        List<String> returnList = new ArrayList<>();
        errors.forEach(error -> returnList.add(error.getErrorKey()));
        return returnList;
    }

    @RequestMapping(value = "/{competitionId}/section/initial", method = RequestMethod.POST)
    public String submitInitialSectionDetails(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) InitialDetailsForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.INITIAL_DETAILS, model);
    }

    @RequestMapping(value = "/{competitionId}/section/additional", method = RequestMethod.POST)
    public String submitAdditionalSectionDetails(@ModelAttribute(COMPETITION_SETUP_FORM_KEY) AdditionalInfoForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model, HttpServletRequest request) {
        if (request.getParameterMap().containsKey("generate-code")) {
            CompetitionResource competition = competitionService.getById(competitionId);
            if (competition.getStartDate() != null) {
                String competitionCode = competitionService.generateCompetitionCode(competitionId, competition.getStartDate());
                competitionSetupForm.setCompetitionCode(competitionCode);
                competitionSetupForm.setMarkAsCompleteAction(false);
            }
        } else if (request.getParameterMap().containsKey("add-funder")) {
            List<Funder> funders = competitionSetupForm.getFunders();
            funders.add(new Funder());
            competitionSetupForm.setFunders(funders);
            competitionSetupForm.setMarkAsCompleteAction(false);
        } else if (request.getParameterMap().containsKey("remove-funder")) {
            int removeCoFunderIndex = Integer.valueOf(request.getParameterMap().get("remove-cofunder")[0]);
            competitionSetupForm.getFunders().remove(removeCoFunderIndex);
            competitionSetupForm.setMarkAsCompleteAction(false);
        }

        //Validate after competition code generated and co funders added/removed.
        validator.validate(competitionSetupForm, bindingResult);

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.ADDITIONAL_INFO, model);
    }

    @RequestMapping(value = "/{competitionId}/section/eligibility", method = RequestMethod.POST)
    public String submitEligibilitySectionDetails(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) EligibilityForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {

    	if("yes".equals(competitionSetupForm.getMultipleStream()) && StringUtils.isEmpty(competitionSetupForm.getStreamName())){
    		bindingResult.addError(new FieldError("competitionSetupForm", "streamName", "A stream name is required"));
    	}

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.ELIGIBILITY, model);
    }

    @RequestMapping(value = "/{competitionId}/section/milestones", method = RequestMethod.POST)
    public String submitMilestonesSectionDetails(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) MilestonesForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {
        if (bindingResult.hasErrors()) {
            competitionSetupMilestoneService.sortMilestones(competitionSetupForm);
        }
        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.MILESTONES, model);
    }

    @RequestMapping(value = "/{competitionId}/section/assessors", method = RequestMethod.POST)
    public String submitMilestonesSectionDetails(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) AssessorsForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.ASSESSORS, model);
    }

    @RequestMapping(value = "/{competitionId}/section/application", method = RequestMethod.POST)
    public String submitApplicationFormSectionDetails(@ModelAttribute(COMPETITION_SETUP_FORM_KEY) ApplicationFormForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.APPLICATION_FORM, model);
    }

    @RequestMapping(value = "/{competitionId}/section/application/question/{questionId}", method = RequestMethod.POST)
    public String submitApplicationQuestion(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) ApplicationFormForm competitionSetupForm,
                                            BindingResult bindingResult,
                                            @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                            @PathVariable("questionId") Long questionId,
                                            Model model) {

        competitionSetupQuestionService.updateQuestion(competitionSetupForm.getQuestionToUpdate());

        if(!bindingResult.hasErrors()) {
            return "redirect:/competition/setup/" + competitionId + "/section/application";
        } else {
            competitionSetupService.populateCompetitionSectionModelAttributes(model, competitionService.getById(competitionId), CompetitionSetupSection.APPLICATION_FORM);
            addQuestionToUpdate(questionId, model, competitionSetupForm);
            model.addAttribute("competitionSetupForm", competitionSetupForm);
            return "competition/setup";
        }
    }

    @RequestMapping(value = "/{competitionId}/section/finance", method = RequestMethod.POST)
    public String submitFinanceSectionDetails(@Valid @ModelAttribute(COMPETITION_SETUP_FORM_KEY) FinanceForm competitionSetupForm,
                                              BindingResult bindingResult,
                                              @PathVariable(COMPETITION_ID_KEY) Long competitionId,
                                              Model model) {

        return genericCompetitionSetupSection(competitionSetupForm, bindingResult, competitionId, CompetitionSetupSection.FINANCE, model);
    }

    @RequestMapping(value = "/{competitionId}/ready-to-open", method = RequestMethod.GET)
    public String setAsReadyToOpen(@PathVariable(COMPETITION_ID_KEY) Long competitionId) {
        competitionSetupService.setCompetitionAsReadyToOpen(competitionId);
        return String.format("redirect:/competition/setup/%d", competitionId);
    }

    /* AJAX Function */
    @RequestMapping(value = "/getInnovationArea/{innovationSectorId}", method = RequestMethod.GET)
    @ResponseBody
    public List<CategoryResource> getInnovationAreas(@PathVariable("innovationSectorId") Long innovationSectorId) {

        return categoryService.getCategoryByParentId(innovationSectorId);
    }

    /* AJAX Function */
    @RequestMapping(value = "/{competitionId}/generateCompetitionCode", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode generateCompetitionCode(@PathVariable(COMPETITION_ID_KEY) Long competitionId, HttpServletRequest request) {

        CompetitionResource competition = competitionService.getById(competitionId);
        if (competition.getStartDate() != null) {
            return this.createJsonObjectNode(true, competitionService.generateCompetitionCode(competitionId, competition.getStartDate()));
        }
        else {
            return this.createJsonObjectNode(false, "Please set a start date for your competition before generating the competition code, you can do this in the Initial Details section");
        }
    }


    private String genericCompetitionSetupSection(CompetitionSetupForm competitionSetupForm, BindingResult bindingResult, Long competitionId, CompetitionSetupSection section, Model model) {
        CompetitionResource competition = competitionService.getById(competitionId);

        if(isSendToDashboard(competition)) {
        	LOG.error("Competition is not found in setup state");
            return "redirect:/dashboard";
        }

        if (isSuccessfulSaved(competitionSetupForm, competition, section, bindingResult)) {
            return "redirect:/competition/setup/" + competitionId + "/section/" + section.getPath();
        } else {
            LOG.debug("Form errors");
            competitionSetupService.populateCompetitionSectionModelAttributes(model, competition, section);
            return "competition/setup";
        }
    }

    private Boolean isSuccessfulSaved(CompetitionSetupForm competitionSetupForm, CompetitionResource competition, CompetitionSetupSection section, BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
           return false;
        }

        List<Error> saveSectionResult = competitionSetupService.saveCompetitionSetupSection(competitionSetupForm, competition, section);
        if(saveSectionResult != null && !saveSectionResult.isEmpty()) {
            saveSectionResult.forEach(e -> {
                if(e.getFieldName() != null) {
                    bindingResult.rejectValue(e.getFieldName(), e.getErrorKey());
                } else {
                    ObjectError error = new ObjectError("currentSection", new String[] {e.getErrorKey()}, null, null);
                    bindingResult.addError(error);
                }
            });
        }

        if(bindingResult.hasErrors()) {
           return false;
        }
        return true;
    }

    private boolean isSendToDashboard(CompetitionResource competition) {
        return competition == null ||
                (!Status.COMPETITION_SETUP.equals(competition.getCompetitionStatus()) &&
                !Status.READY_TO_OPEN.equals(competition.getCompetitionStatus()));
    }

    private ObjectNode createJsonObjectNode(boolean success, String message) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("success", success ? "true" : "false");
        node.put("message", CharMatcher.is('\"').trimFrom(message));

        return node;
    }

    private ObjectNode createJsonObjectNode(boolean success, List<String> errors) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("success", success ? "true" : "false");

        if (!success) {
            ArrayNode errorsNode = mapper.createArrayNode();
            errors.stream().forEach(errorsNode::add);
            node.set("validation_errors", errorsNode);
        }

        return node;
    }

    private void addQuestionToUpdate(Long questionId, Model model, ApplicationFormForm applicationFormForm) {
        if(model.containsAttribute("questions")) {
            List<Question> questions = (List<Question>) model.asMap().get("questions");
            Optional<Question> question = questions.stream().filter(questionObject -> questionObject.getId().equals(questionId)).findFirst();
            if(question.isPresent()) {
                applicationFormForm.setQuestionToUpdate(question.get());
            } else {
                LOG.error("Question(" + questionId + ") not found");
            }
        }
    }
}
