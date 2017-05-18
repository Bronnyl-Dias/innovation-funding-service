package org.innovateuk.ifs.application.transactional;

import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.domain.FundingDecisionStatus;
import org.innovateuk.ifs.application.mapper.ApplicationSummaryMapper;
import org.innovateuk.ifs.application.mapper.ApplicationSummaryPageMapper;
import org.innovateuk.ifs.application.resource.ApplicationState;
import org.innovateuk.ifs.application.resource.ApplicationSummaryPageResource;
import org.innovateuk.ifs.application.resource.ApplicationSummaryResource;
import org.innovateuk.ifs.application.resource.comparators.*;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.transactional.BaseTransactionalService;
import org.innovateuk.ifs.workflow.resource.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.innovateuk.ifs.application.resource.ApplicationState.INELIGIBLE;
import static org.innovateuk.ifs.application.resource.ApplicationState.INELIGIBLE_INFORMED;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.util.CollectionFunctions.asLinkedSet;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMapSet;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
public class ApplicationSummaryServiceImpl extends BaseTransactionalService implements ApplicationSummaryService {

    public static final Set<ApplicationState> SUBMITTED_APPLICATION_STATES = asLinkedSet(
            ApplicationState.APPROVED,
            ApplicationState.REJECTED,
            ApplicationState.SUBMITTED);

    public static final Set<State> SUBMITTED_STATES = SUBMITTED_APPLICATION_STATES
            .stream().map(ApplicationState::getBackingState).collect(toSet());

    public static final Set<State> NOT_SUBMITTED_STATES = simpleMapSet(asLinkedSet(
            ApplicationState.CREATED,
            ApplicationState.OPEN), ApplicationState::getBackingState);

    public static final Set<State> INELIGIBLE_STATES = simpleMapSet(asLinkedSet(
            ApplicationState.INELIGIBLE,
            INELIGIBLE_INFORMED), ApplicationState::getBackingState);

    public static final Set<State> CREATED_AND_OPEN_STATUSES = simpleMapSet(asLinkedSet(
            ApplicationState.CREATED,
            ApplicationState.OPEN), ApplicationState::getBackingState);

    public static final Set<State> FUNDING_DECISIONS_MADE_STATUSES = simpleMapSet(asLinkedSet(
            ApplicationState.APPROVED,
            ApplicationState.REJECTED), ApplicationState::getBackingState);

    public static final Set<State> SUBMITTED_AND_INELIGIBLE_STATES = Stream.concat(
            SUBMITTED_STATES.stream(),
            INELIGIBLE_STATES.stream()).collect(toSet());

    private static final Map<String, Sort> SORT_FIELD_TO_DB_SORT_FIELDS = new HashMap<String, Sort>() {{
        put("name", new Sort(ASC, new String[]{"name", "id"}));
        put("duration", new Sort(ASC, new String[]{"durationInMonths", "id"}));
        put("percentageComplete", new Sort(DESC, "completion").and(new Sort(ASC, "id")));
    }};

    // TODO These comparators are used to sort application after loading them in memory.
    // TODO The code currently is retrieving to many of them and this sorting should be done in the database query.
    // TODO Ideally they should all be replaced
    private static final Map<String, Comparator<ApplicationSummaryResource>> SUMMARY_COMPARATORS =
            new HashMap<String, Comparator<ApplicationSummaryResource>>() {{
        put("lead", new ApplicationSummaryResourceLeadComparator());
        put("leadApplicant", new ApplicationSummaryResourceLeadApplicantComparator());
        put("numberOfPartners", new ApplicationSummaryResourceNumberOfPartnersComparator());
        put("grantRequested", new ApplicationSummaryResourceGrantRequestedComparator());
        put("totalProjectCost", new ApplicationSummaryResourceTotalProjectCostComparator());
    }};

    private static final Collection<String> FIELDS_NOT_SORTABLE_IN_DB = SUMMARY_COMPARATORS.keySet();

    @Autowired
    private ApplicationSummaryMapper applicationSummaryMapper;

    @Autowired
    private ApplicationSummaryPageMapper applicationSummaryPageMapper;


    @Override
    public ServiceResult<ApplicationSummaryPageResource> getApplicationSummariesByCompetitionId(
            Long competitionId, String sortBy, int pageIndex, int pageSize, Optional<String> filter) {
        String filterStr = filter.map(String::trim).orElse("");
        return applicationSummaries(sortBy, pageIndex, pageSize,
                pageable -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateNotIn(competitionId, INELIGIBLE_STATES, filterStr, pageable),
                () -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateNotIn(competitionId, INELIGIBLE_STATES, filterStr));
    }

    @Override
    public ServiceResult<ApplicationSummaryPageResource> getSubmittedApplicationSummariesByCompetitionId(
            Long competitionId,
            String sortBy,
            int pageIndex,
            int pageSize,
            Optional<String> filter,
            Optional<FundingDecisionStatus> fundingFilter) {
        String filterStr = filter.map(String::trim).orElse("");
        return applicationSummaries(sortBy, pageIndex, pageSize,
                pageable -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, SUBMITTED_STATES, filterStr, fundingFilter.orElse(null), pageable),
                () -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, SUBMITTED_STATES, filterStr, fundingFilter.orElse(null)));
    }

    @Override
    public ServiceResult<ApplicationSummaryPageResource> getNotSubmittedApplicationSummariesByCompetitionId(
            Long competitionId, String sortBy, int pageIndex, int pageSize) {

        return applicationSummaries(sortBy, pageIndex, pageSize,
                pageable -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, NOT_SUBMITTED_STATES, "", null, pageable),
                () -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, NOT_SUBMITTED_STATES, "", null));
    }

    @Override
    public ServiceResult<ApplicationSummaryPageResource> getWithFundingDecisionApplicationSummariesByCompetitionId(
            long competitionId,
            String sortBy,
            int pageIndex,
            int pageSize,
            Optional<String> filter, Optional<Boolean> sendFilter,
            Optional<FundingDecisionStatus> fundingFilter) {
        String filterStr = filter.map(String::trim).orElse("");
        return applicationSummaries(sortBy, pageIndex, pageSize,
                pageable -> applicationRepository.findByCompetitionIdAndFundingDecisionIsNotNull(
                        competitionId,
                        filterStr,
                        sendFilter.orElse(null),
                        fundingFilter.orElse(null),
                        pageable),
                () -> applicationRepository.findByCompetitionIdAndFundingDecisionIsNotNull(
                        competitionId,
                        filterStr,
                        sendFilter.orElse(null),
                        fundingFilter.orElse(null)));
    }

    @Override
    public ServiceResult<ApplicationSummaryPageResource> getIneligibleApplicationSummariesByCompetitionId(
            long competitionId,
            String sortBy,
            int pageIndex,
            int pageSize,
            Optional<String> filter,
            Optional<Boolean> informFilter) {
        String filterStr = filter.map(String::trim).orElse("");
        Set<State> states = informFilter.map(i -> i ? singleton(INELIGIBLE_INFORMED.getBackingState()) : singleton(INELIGIBLE.getBackingState())).orElse(INELIGIBLE_STATES);
        return applicationSummaries(sortBy, pageIndex, pageSize,
                pageable -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, states, filterStr, null, pageable),
                () -> applicationRepository.findByCompetitionIdAndApplicationProcessActivityStateStateInAndIdLike(
                        competitionId, states, filterStr, null));
    }

    private ServiceResult<ApplicationSummaryPageResource> applicationSummaries(
            String sortBy,
            int pageIndex,
            int pageSize,
            Function<Pageable, Page<Application>> paginatedApplicationsSupplier,
            Supplier<List<Application>> nonPaginatedApplicationsSupplier) {
        Sort sortField = getApplicationSummarySortField(sortBy);
        Pageable pageable = new PageRequest(pageIndex, pageSize, sortField);

        if (canUseSpringDataPaginationForSummaryResults(sortBy)) {
            Page<Application> applicationResults = paginatedApplicationsSupplier.apply(pageable);
            return find(applicationResults, notFoundError(Page.class)).andOnSuccessReturn(applicationSummaryPageMapper::mapToResource);
        }

        List<Application> resultsList = nonPaginatedApplicationsSupplier.get();

        ApplicationSummaryPageResource result = new ApplicationSummaryPageResource();
        result.setContent(sortAndRestrictSummaryResults(resultsList, pageable, sortBy));

        result.setNumber(pageable.getPageNumber());
        result.setSize(pageable.getPageSize());
        result.setTotalElements(resultsList.size());
        result.setTotalPages((resultsList.size() + pageable.getPageSize() - 1) / pageable.getPageSize());
        return find(result, notFoundError(ApplicationSummaryPageResource.class));
    }

    private List<ApplicationSummaryResource> sortAndRestrictSummaryResults(
            List<Application> resultsList, Pageable pageable, String sortBy) {
        return resultsList.stream()
                .map(applicationSummaryMapper::mapToResource)
                .sorted((i1, i2) -> {
                    Comparator<ApplicationSummaryResource> comparatorForField = SUMMARY_COMPARATORS.get(sortBy);
                    if (comparatorForField == null) {
                        return 0;
                    }
                    return comparatorForField.compare(i1, i2);
                })
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(toList());
    }

    private boolean canUseSpringDataPaginationForSummaryResults(String sortBy) {
        return !FIELDS_NOT_SORTABLE_IN_DB.stream().anyMatch(field -> field.equals(sortBy));
    }

    private Sort getApplicationSummarySortField(String sortBy) {
        Sort result = SORT_FIELD_TO_DB_SORT_FIELDS.get(sortBy);
        return result != null ? result : new Sort(ASC, new String[]{"id"});
    }
}
