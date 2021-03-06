package org.innovateuk.ifs.publiccontent.controller;

import org.innovateuk.ifs.competition.publiccontent.resource.PublicContentResource;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.service.CompetitionRestService;
import org.innovateuk.ifs.competitionsetup.core.service.CompetitionSetupService;
import org.innovateuk.ifs.controller.ValidationHandler;
import org.innovateuk.ifs.publiccontent.form.AbstractPublicContentForm;
import org.innovateuk.ifs.publiccontent.formpopulator.PublicContentFormPopulator;
import org.innovateuk.ifs.publiccontent.modelpopulator.PublicContentViewModelPopulator;
import org.innovateuk.ifs.publiccontent.saver.PublicContentFormSaver;
import org.innovateuk.ifs.publiccontent.service.PublicContentService;
import org.innovateuk.ifs.publiccontent.viewmodel.AbstractPublicContentViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.Optional;
import java.util.function.Supplier;

import static org.innovateuk.ifs.competitionsetup.CompetitionSetupController.COMPETITION_ID_KEY;

/**
 * Abstract controller for all public content sections.
 */
public abstract class AbstractPublicContentSectionController<M extends AbstractPublicContentViewModel, F extends AbstractPublicContentForm> {

    protected static final String TEMPLATE_FOLDER = "competition/";
    protected static final String FORM_ATTR_NAME = "form";

    @Autowired
    protected PublicContentService publicContentService;

    @Autowired
    protected CompetitionRestService competitionRestService;

    @Autowired
    protected CompetitionSetupService competitionSetupService;

    protected abstract PublicContentViewModelPopulator<M> modelPopulator();
    protected abstract PublicContentFormPopulator<F> formPopulator();
    protected abstract PublicContentFormSaver<F> formSaver();

    @GetMapping("/{competitionId}")
    public String readOnly(Model model, @PathVariable(COMPETITION_ID_KEY) long competitionId) {
        return readOnly(competitionId, model, Optional.empty());
    }

    @GetMapping("/{competitionId}/edit")
    public String edit(Model model, @PathVariable(COMPETITION_ID_KEY) long competitionId) {
        return edit(competitionId, model, Optional.empty());
    }

    @PostMapping(value = "/{competitionId}/edit")
    public String markAsComplete(Model model,
                                 @PathVariable(COMPETITION_ID_KEY) long competitionId,
                                 @Valid @ModelAttribute(FORM_ATTR_NAME) F form,
                                 BindingResult bindingResult,
                                 ValidationHandler validationHandler) {
        return markAsComplete(competitionId, model, form, validationHandler);
    }

    private String readOnly(long competitionId, Model model, Optional<F> form) {
        return getPage(competitionId, model, form, true);
    }

    private String edit(long competitionId, Model model, Optional<F> form) {
        return getPage(competitionId, model, form, false);
    }

    protected String getPage(long competitionId, Model model, Optional<F> form, boolean readOnly) {
        PublicContentResource publicContent = publicContentService.getCompetitionById(competitionId);

        CompetitionResource competition = competitionRestService.getCompetitionById(competitionId).getSuccess();

        if (!competition.isNonIfs() && !competitionSetupService.isInitialDetailsCompleteOrTouched(competition.getId())) {
            return "redirect:/competition/setup/" + competition.getId();
        }

        model.addAttribute("model", modelPopulator().populate(publicContent, readOnly));
        if(form.isPresent()) {
            model.addAttribute("form", form.get());
        } else {
            model.addAttribute("form", formPopulator().populate(publicContent));
        }
        return TEMPLATE_FOLDER + "public-content-form";
    }

    private String markAsComplete(long competitionId, Model model, F form, ValidationHandler validationHandler) {
        CompetitionResource competition = competitionRestService.getCompetitionById(competitionId)
                .getSuccess();

        if (!competition.isNonIfs() && !competitionSetupService.isInitialDetailsCompleteOrTouched(competitionId)) {
            return "redirect:/competition/setup/" + competition.getId();
        }

        Supplier<String> successView = () -> getPage(competitionId, model, Optional.of(form), true);
        Supplier<String> failureView = () -> getPage(competitionId, model, Optional.of(form), false);

        PublicContentResource publicContent = publicContentService.getCompetitionById(competitionId);
        return validationHandler.performActionOrBindErrorsToField("", failureView, successView, () -> formSaver().markAsComplete(form, publicContent));
    }

}
