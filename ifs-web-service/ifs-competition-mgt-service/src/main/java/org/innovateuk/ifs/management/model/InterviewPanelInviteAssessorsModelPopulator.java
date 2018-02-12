package org.innovateuk.ifs.management.model;

import org.apache.commons.lang3.StringUtils;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.management.viewmodel.InviteAssessorsViewModel;
import org.springframework.stereotype.Component;

/**
 * Build the model for the Invite assessors view.
 */
@Component
abstract class InterviewPanelInviteAssessorsModelPopulator<ViewModelType extends InviteAssessorsViewModel> {

    public ViewModelType populateModel(CompetitionResource competition) {
        ViewModelType model = populateCompetitionDetails(createModel(), competition);
        populateCompetitionInnovationSectorAndArea(model, competition);
        return model;
    }

    protected abstract ViewModelType createModel();

    private ViewModelType populateCompetitionDetails(ViewModelType model, CompetitionResource competition) {
        model.setCompetitionId(competition.getId());
        model.setCompetitionName(competition.getName());
        return model;
    }

    private void populateCompetitionInnovationSectorAndArea(ViewModelType model, CompetitionResource competition) {
        model.setInnovationSector(competition.getInnovationSectorName());
        model.setInnovationArea(StringUtils.join(competition.getInnovationAreaNames(),", "));
    }
}
