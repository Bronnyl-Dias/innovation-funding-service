package org.innovateuk.ifs.management.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.innovateuk.ifs.assessment.service.ReviewPanelInviteRestService;
import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.commons.security.SecuredBySpring;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.controller.ValidationHandler;
import org.innovateuk.ifs.invite.resource.ExistingUserStagedInviteListResource;
import org.innovateuk.ifs.invite.resource.ExistingUserStagedInviteResource;
import org.innovateuk.ifs.management.controller.CompetitionManagementAssessorProfileController.AssessorProfileOrigin;
import org.innovateuk.ifs.management.form.InviteNewAssessorsForm;
import org.innovateuk.ifs.management.form.PanelSelectionForm;
import org.innovateuk.ifs.management.model.PanelInviteAssessorsAcceptedModelPopulator;
import org.innovateuk.ifs.management.model.PanelInviteAssessorsFindModelPopulator;
import org.innovateuk.ifs.management.model.PanelInviteAssessorsInviteModelPopulator;
import org.innovateuk.ifs.management.viewmodel.ReviewPanelInviteAssessorsFindViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.innovateuk.ifs.util.BackLinkUtil.buildOriginQueryString;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.innovateuk.ifs.util.MapFunctions.asMap;

/**
 * This controller will handle all Competition Management requests related to inviting assessors to an Assessment Panel.
 */
@Controller
@RequestMapping("/assessment/panel/competition/{competitionId}/assessors")
@SecuredBySpring(value = "Controller", description = "TODO", securedType = CompetitionManagementPanelInviteAssessorsController.class)
@PreAuthorize("hasAnyAuthority('comp_admin','project_finance')")
public class CompetitionManagementPanelInviteAssessorsController extends CompetitionManagementCookieController<PanelSelectionForm> {

    private static final String SELECTION_FORM = "assessorPanelSelectionForm";
    private static final String FORM_ATTR_NAME = "form";

    @Autowired
    private ReviewPanelInviteRestService reviewPanelInviteRestService;

    @Autowired
    private PanelInviteAssessorsFindModelPopulator panelInviteAssessorsFindModelPopulator;

    @Autowired
    private PanelInviteAssessorsInviteModelPopulator panelInviteAssessorsInviteModelPopulator;

    @Autowired
    private PanelInviteAssessorsAcceptedModelPopulator panelInviteAssessorsAcceptedModelPopulator;

    @Override
    protected String getCookieName() {
        return SELECTION_FORM;
    }

    protected Class<PanelSelectionForm> getFormType() {
        return PanelSelectionForm.class;
    }

    @GetMapping
    public String assessors(@PathVariable("competitionId") long competitionId) {
        return format("redirect:/competition/%s/assessors/panel-find", competitionId);
    }

    @GetMapping("/find")
    public String find(Model model,
                       @ModelAttribute(name = SELECTION_FORM, binding = false) PanelSelectionForm selectionForm,
                       @SuppressWarnings("unused") BindingResult bindingResult,
                       @PathVariable("competitionId") long competitionId,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam MultiValueMap<String, String> queryParams,
                       HttpServletRequest request,
                       HttpServletResponse response) {

        String originQuery = buildOriginQueryString(AssessorProfileOrigin.PANEL_FIND, queryParams);
        updateSelectionForm(request, response, competitionId, selectionForm);
        ReviewPanelInviteAssessorsFindViewModel reviewPanelInviteAssessorsFindViewModel = panelInviteAssessorsFindModelPopulator.populateModel(competitionId, page, originQuery);

        model.addAttribute("model", reviewPanelInviteAssessorsFindViewModel);
        model.addAttribute("originQuery", originQuery);

        return "assessors/panel-find";
    }

    private void updateSelectionForm(HttpServletRequest request,
                                     HttpServletResponse response,
                                     long competitionId,
                                     PanelSelectionForm selectionForm) {
        PanelSelectionForm storedSelectionForm = getSelectionFormFromCookie(request, competitionId).orElse(new PanelSelectionForm());

        PanelSelectionForm trimmedAssessorForm = trimSelectionByFilteredResult(storedSelectionForm, competitionId);
        selectionForm.setSelectedIds(trimmedAssessorForm.getSelectedIds());
        selectionForm.setAllSelected(trimmedAssessorForm.getAllSelected());

        saveFormToCookie(response, competitionId, selectionForm);
    }

    private PanelSelectionForm trimSelectionByFilteredResult(PanelSelectionForm selectionForm,
                                                             long competitionId) {
        List<Long> filteredResults = getAllAssessorIds(competitionId);
        PanelSelectionForm updatedSelectionForm = new PanelSelectionForm();

        selectionForm.getSelectedIds().retainAll(filteredResults);
        updatedSelectionForm.setSelectedIds(selectionForm.getSelectedIds());

        if (updatedSelectionForm.getSelectedIds().equals(filteredResults)  && !updatedSelectionForm.getSelectedIds().isEmpty()) {
            updatedSelectionForm.setAllSelected(true);
        } else {
            updatedSelectionForm.setAllSelected(false);
        }

        return updatedSelectionForm;
    }

    @PostMapping(value = "/find", params = {"selectionId"})
    public @ResponseBody JsonNode selectAssessorForInviteList(
            @PathVariable("competitionId") long competitionId,
            @RequestParam("selectionId") long assessorId,
            @RequestParam("isSelected") boolean isSelected,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean limitExceeded = false;
        try {
            List<Long> assessorIds = getAllAssessorIds(competitionId);
            PanelSelectionForm selectionForm = getSelectionFormFromCookie(request, competitionId).orElse(new PanelSelectionForm());
            if (isSelected) {
                int predictedSize = selectionForm.getSelectedIds().size() + 1;
                if(limitIsExceeded(predictedSize)){
                    limitExceeded = true;
                } else {
                    selectionForm.getSelectedIds().add(assessorId);
                    if (selectionForm.getSelectedIds().containsAll(assessorIds)) {
                        selectionForm.setAllSelected(true);
                    }
                }
            } else {
                selectionForm.getSelectedIds().remove(assessorId);
                selectionForm.setAllSelected(false);
            }
            saveFormToCookie(response, competitionId, selectionForm);
            return createJsonObjectNode(selectionForm.getSelectedIds().size(), selectionForm.getAllSelected(), limitExceeded);
        } catch (Exception e) {
            return createFailureResponse();
        }
    }

    @PostMapping(value = "/find", params = {"addAll"})
    public @ResponseBody JsonNode addAllAssessorsToInviteList(Model model,
                                              @PathVariable("competitionId") long competitionId,
                                              @RequestParam("addAll") boolean addAll,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        try {
            PanelSelectionForm selectionForm = getSelectionFormFromCookie(request, competitionId).orElse(new PanelSelectionForm());

            if (addAll) {
                selectionForm.setSelectedIds(getAllAssessorIds(competitionId));
                selectionForm.setAllSelected(true);
            } else {
                selectionForm.getSelectedIds().clear();
                selectionForm.setAllSelected(false);
            }

            saveFormToCookie(response, competitionId, selectionForm);

            return createSuccessfulResponseWithSelectionStatus(selectionForm.getSelectedIds().size(), selectionForm.getAllSelected(), false);
        } catch (Exception e) {
            return createFailureResponse();
        }
    }

    private List<Long> getAllAssessorIds(long competitionId) {
        return reviewPanelInviteRestService.getAvailableAssessorIds(competitionId).getSuccess();
    }

    @PostMapping(value = "/find/addSelected")
    public String addSelectedAssessorsToInviteList(Model model,
                                                   @PathVariable("competitionId") long competitionId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam Optional<Long> innovationArea,
                                                   @ModelAttribute(SELECTION_FORM) PanelSelectionForm selectionForm,
                                                   ValidationHandler validationHandler,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {

        PanelSelectionForm submittedSelectionForm = getSelectionFormFromCookie(request, competitionId)
                .filter(form -> !form.getSelectedIds().isEmpty())
                .orElse(selectionForm);
        Supplier<String> failureView = () -> redirectToFind(competitionId, page, innovationArea);

        return validationHandler.failNowOrSucceedWith(failureView, () -> {
            RestResult<Void> restResult = reviewPanelInviteRestService.inviteUsers(
                    newSelectionFormToResource(submittedSelectionForm, competitionId));

            return validationHandler.addAnyErrors(restResult)
                    .failNowOrSucceedWith(failureView, () -> {
                        removeCookie(response, competitionId);
                        return redirectToInvite(competitionId, 0);
                    });
        });
    }

    private String redirectToFind(long competitionId, int page, Optional<Long> innovationArea) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/assessment/panel/competition/{competitionId}/assessors/find")
                .queryParam("page", page);

        innovationArea.ifPresent(innovationAreaId -> builder.queryParam("innovationArea", innovationAreaId));

        return "redirect:" + builder.buildAndExpand(asMap("competitionId", competitionId))
                .toUriString();
    }

    @GetMapping("/invite")
    public String invite(Model model,
                         @PathVariable("competitionId") long competitionId,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam MultiValueMap<String, String> queryParams) {

        String originQuery = buildOriginQueryString(AssessorProfileOrigin.PANEL_INVITE, queryParams);

        model.addAttribute("model", panelInviteAssessorsInviteModelPopulator.populateModel(competitionId, page, originQuery));
        model.addAttribute("originQuery", originQuery);

        return "assessors/panel-invite";
    }

    @GetMapping("/accepted")
    public String accepted(Model model,
                           @PathVariable("competitionId") long competitionId,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam MultiValueMap<String, String> queryParams) {
        String originQuery = buildOriginQueryString(AssessorProfileOrigin.PANEL_ACCEPTED, queryParams);

        model.addAttribute("model", panelInviteAssessorsAcceptedModelPopulator.populateModel(
                competitionId,
                page,
                originQuery
        ));

        return "assessors/panel-accepted";
    }

    @PostMapping(value = "/invite", params = {"remove"})
    public String removeInviteFromInviteView(Model model,
                                             @PathVariable("competitionId") long competitionId,
                                             @RequestParam(name = "remove") String email,
                                             @RequestParam(defaultValue = "0") int page,
                                             @SuppressWarnings("unused") @ModelAttribute(FORM_ATTR_NAME) InviteNewAssessorsForm form) {
        deleteInvite(email, competitionId).getSuccess();
        return redirectToInvite(competitionId, page);
    }

    @PostMapping(value = "/invite", params = {"removeAll"})
    public String removeAllInvitesFromInviteView(Model model,
                                                 @PathVariable("competitionId") long competitionId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @SuppressWarnings("unused") @ModelAttribute(FORM_ATTR_NAME) InviteNewAssessorsForm form) {
        deleteAllInvites(competitionId).getSuccess();
        return redirectToInvite(competitionId, page);
    }

    private ServiceResult<Void> deleteInvite(String email, long competitionId) {
        return reviewPanelInviteRestService.deleteInvite(email, competitionId).toServiceResult();
    }

    private ServiceResult<Void> deleteAllInvites(long competitionId) {
        return reviewPanelInviteRestService.deleteAllInvites(competitionId).toServiceResult();
    }

    private String redirectToInvite(long competitionId, int page) {
        return "redirect:" + UriComponentsBuilder.fromPath("/assessment/panel/competition/{competitionId}/assessors/invite")
                .queryParam("page", page)
                .buildAndExpand(asMap("competitionId", competitionId))
                .toUriString();
    }

    private ExistingUserStagedInviteListResource newSelectionFormToResource(PanelSelectionForm form, long competitionId) {
        return new ExistingUserStagedInviteListResource(simpleMap(
                form.getSelectedIds(), id -> new ExistingUserStagedInviteResource(id, competitionId)));
    }
}
