package org.innovateuk.ifs.management.application.list.populator;

import org.innovateuk.ifs.application.resource.PreviousApplicationPageResource;
import org.innovateuk.ifs.application.service.ApplicationRestService;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.service.CompetitionRestService;
import org.innovateuk.ifs.management.application.list.viewmodel.PreviousApplicationsViewModel;
import org.innovateuk.ifs.management.navigation.Pagination;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.innovateuk.ifs.user.resource.Role.IFS_ADMINISTRATOR;

/**
 * Builds the Competition Management Previous Applications view model.
 */
@Component
public class PreviousApplicationsModelPopulator {

    @Autowired
    private CompetitionRestService competitionRestService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationRestService applicationRestService;

    public PreviousApplicationsViewModel populateModel(long competitionId, int pageNumber, int pageSize, String sortField, String filter, UserResource loggedInUser, String existingQueryString) {

        CompetitionResource competition = competitionRestService.getCompetitionById(competitionId).getSuccess();

        PreviousApplicationPageResource previousApplicationsPagedResult = applicationRestService
                .findPreviousApplications(competitionId, pageNumber, pageSize, sortField, filter)
                .getSuccess();

        boolean isIfsAdmin = userService.existsAndHasRole(loggedInUser.getId(), IFS_ADMINISTRATOR);

        return new PreviousApplicationsViewModel(competitionId, competition.getName(), isIfsAdmin,
                previousApplicationsPagedResult.getContent(),
                previousApplicationsPagedResult.getTotalElements(),
                new Pagination(previousApplicationsPagedResult, existingQueryString));
    }
}
