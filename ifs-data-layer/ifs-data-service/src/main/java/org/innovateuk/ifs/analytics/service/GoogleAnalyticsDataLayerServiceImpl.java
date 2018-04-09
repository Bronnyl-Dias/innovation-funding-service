package org.innovateuk.ifs.analytics.service;

import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.repository.CompetitionRepository;
import org.innovateuk.ifs.transactional.BaseTransactionalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;

@Service
public class GoogleAnalyticsDataLayerServiceImpl extends BaseTransactionalService implements GoogleAnalyticsDataLayerService {

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Override
    public ServiceResult<String> getCompetitionNameByApplicationId(long applicationId) {
        Application application = applicationRepository.findById(applicationId);
        return find(competitionRepository
                .findById(application.getCompetition().getId()), notFoundError(Competition.class, applicationId))
                .andOnSuccessReturn(Competition::getName);
    }

    @Override
    public ServiceResult<String> getCompetitionName(long competitionId) {
        return find(competitionRepository
                .findById(competitionId), notFoundError(Competition.class, competitionId))
                .andOnSuccessReturn(Competition::getName);
    }

    @Override
    public ServiceResult<String> getCompetitionNameByProjectId(long projectId) {
        Application application = applicationRepository.findByProjectId(projectId);

        return find(competitionRepository
                .findById(application.getCompetition().getId()), notFoundError(Competition.class, projectId))
                .andOnSuccessReturn(Competition::getName);
    }

    @Override
    public ServiceResult<String> getCompetitionNameByAssessmentId(long assessmentId) {
        Application application = applicationRepository.findByAssessmentId(assessmentId);

        return find(competitionRepository
                .findById(application.getCompetition().getId()), notFoundError(Competition.class, assessmentId))
                .andOnSuccessReturn(Competition::getName);
    }
}