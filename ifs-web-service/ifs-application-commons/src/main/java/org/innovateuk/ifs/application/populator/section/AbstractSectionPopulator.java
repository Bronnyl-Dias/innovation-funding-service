package org.innovateuk.ifs.application.populator.section;

import org.innovateuk.ifs.applicant.resource.ApplicantSectionResource;
import org.innovateuk.ifs.application.populator.ApplicationNavigationPopulator;
import org.innovateuk.ifs.application.populator.BaseModelPopulator;
import org.innovateuk.ifs.application.viewmodel.NavigationViewModel;
import org.innovateuk.ifs.application.viewmodel.section.AbstractSectionViewModel;
import org.innovateuk.ifs.form.ApplicationForm;
import org.innovateuk.ifs.form.resource.SectionType;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import java.util.Optional;

/**
 * Abstract populator section view models.
 */
public abstract class AbstractSectionPopulator<M extends AbstractSectionViewModel> extends BaseModelPopulator {

    private final ApplicationNavigationPopulator navigationPopulator;

    protected AbstractSectionPopulator(final ApplicationNavigationPopulator navigationPopulator) {
        this.navigationPopulator = navigationPopulator;
    }

    public M populate(
            ApplicantSectionResource section,
            ApplicationForm form,
            Model model,
            BindingResult bindingResult,
            Boolean readOnly,
            Optional<Long> applicantOrganisationId,
            Boolean readOnlyAllApplicantApplicationFinances
    ) {
        M viewModel = createNew(
                section,
                form,
                readOnly,
                applicantOrganisationId,
                readOnlyAllApplicantApplicationFinances
        );
        populateNoReturn(section, form, viewModel, model, bindingResult, readOnly, applicantOrganisationId);
        return viewModel;
    }

    protected abstract void populateNoReturn(
            ApplicantSectionResource section,
            ApplicationForm form,
            M viewModel,
            Model model,
            BindingResult bindingResult,
            Boolean readOnly,
            Optional<Long> applicantOrganisationId
    );

    protected abstract M createNew(
            ApplicantSectionResource section,
            ApplicationForm form,
            Boolean readOnly,
            Optional<Long> applicantOrganisationId,
            Boolean readOnlyAllApplicantApplicationFinances
    );

    public abstract SectionType getSectionType();

    protected NavigationViewModel getNavigationViewModel(ApplicantSectionResource applicantSection) {
        return navigationPopulator.addNavigation(
                applicantSection.getSection(),
                applicantSection.getApplication().getId()
        );

    }
}