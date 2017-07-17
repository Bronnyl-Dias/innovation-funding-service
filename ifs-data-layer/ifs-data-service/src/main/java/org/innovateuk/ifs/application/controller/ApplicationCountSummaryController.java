package org.innovateuk.ifs.application.controller;

import org.innovateuk.ifs.application.resource.ApplicationCountSummaryPageResource;
import org.innovateuk.ifs.application.transactional.ApplicationCountSummaryService;
import org.innovateuk.ifs.commons.rest.RestResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for exposing statistical data on applications
 */
@RestController
@RequestMapping("/applicationCountSummary")
public class ApplicationCountSummaryController {

    @Autowired
    private ApplicationCountSummaryService applicationCountSummaryService;

    private static final String DEFAULT_PAGE_SIZE = "20";

    @GetMapping("/findByCompetitionId/{competitionId}")
    public RestResult<ApplicationCountSummaryPageResource> getApplicationCountSummariesByCompetitionId(@PathVariable("competitionId") Long competitionId,
                                                                                                       @RequestParam(value = "page",defaultValue = "0") int pageIndex,
                                                                                                       @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                                                       @RequestParam(value = "filter", required = false) Optional<String> filter) {
        return applicationCountSummaryService.getApplicationCountSummariesByCompetitionId(competitionId, pageIndex, pageSize, filter).toGetResponse();
    }

    @GetMapping("/findByCompetitionIdAndInnovationArea/{competitionId}")
    public RestResult<ApplicationCountSummaryPageResource> getApplicationCountSummariesByCompetitionIdAndInnovationArea(@PathVariable("competitionId") Long competitionId,
                                                                                                                        @RequestParam(value = "page",defaultValue = "0") int pageIndex,
                                                                                                                        @RequestParam(value = "size", defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
                                                                                                                        @RequestParam(value = "sortField") String sortField,
                                                                                                                        @RequestParam(value = "innovationArea", required = false) Optional<Long> innovationArea) {
        return applicationCountSummaryService.getApplicationCountSummariesByCompetitionIdAndInnovationArea(competitionId, pageIndex, pageSize, innovationArea, sortField).toGetResponse();
    }
}
