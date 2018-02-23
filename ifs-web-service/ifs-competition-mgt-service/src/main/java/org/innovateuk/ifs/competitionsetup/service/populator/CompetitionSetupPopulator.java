package org.innovateuk.ifs.competitionsetup.service.populator;

import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.resource.CompetitionSetupSection;
import org.innovateuk.ifs.competition.service.CompetitionSetupRestService;
import org.innovateuk.ifs.competitionsetup.service.CompetitionSetupService;
import org.innovateuk.ifs.competitionsetup.viewmodel.CompetitionStateSetupViewModel;
import org.innovateuk.ifs.competitionsetup.viewmodel.fragments.GeneralSetupViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * General populator for non section specific
 */
@Service
public class CompetitionSetupPopulator {
    @Autowired
    private CompetitionSetupService competitionSetupService;

    @Autowired
    private CompetitionSetupRestService competitionSetupRestService;

    public GeneralSetupViewModel populateGeneralModelAttributes(CompetitionResource competitionResource, CompetitionSetupSection section) {
        Map<CompetitionSetupSection, Optional<Boolean>> statuses = competitionSetupRestService.getSectionStatuses(competitionResource.getId())
                .getSuccess();

        boolean editable = isSectionEditable(statuses, section, competitionResource);

        GeneralSetupViewModel viewModel = new GeneralSetupViewModel(editable, competitionResource, section, CompetitionSetupSection.values(),
                competitionSetupService.isInitialDetailsCompleteOrTouched(competitionResource.getId()));

        if (section.hasDisplayableSetupFragment()) {
            viewModel.setCurrentSectionFragment("section-" + section.getPath());
        }

        viewModel.setState(populateCompetitionStateModelAttributes(competitionResource, section));

        return viewModel;
    }

    private boolean isSectionEditable(Map<CompetitionSetupSection, Optional<Boolean>> statuses, CompetitionSetupSection section, CompetitionResource competitionResource) {
        return (!statuses.get(section).isPresent() || !statuses.get(section).get())
                && !section.preventEdit(competitionResource);
    }

    private CompetitionStateSetupViewModel populateCompetitionStateModelAttributes(CompetitionResource competitionResource, CompetitionSetupSection section) {
        return new CompetitionStateSetupViewModel(section.preventEdit(competitionResource),
                competitionResource.isSetupAndLive(),
                competitionResource.getSetupComplete(),
                competitionResource.isInAssessmentState());
    }
}
