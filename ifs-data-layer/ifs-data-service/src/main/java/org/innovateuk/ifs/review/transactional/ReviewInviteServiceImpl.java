package org.innovateuk.ifs.review.transactional;


import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.category.mapper.InnovationAreaMapper;
import org.innovateuk.ifs.category.resource.InnovationAreaResource;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.competition.repository.CompetitionRepository;
import org.innovateuk.ifs.invite.domain.ParticipantStatus;
import org.innovateuk.ifs.invite.domain.competition.*;
import org.innovateuk.ifs.invite.mapper.ParticipantStatusMapper;
import org.innovateuk.ifs.invite.mapper.ReviewParticipantMapper;
import org.innovateuk.ifs.invite.repository.CompetitionParticipantRepository;
import org.innovateuk.ifs.invite.repository.ReviewInviteRepository;
import org.innovateuk.ifs.invite.repository.ReviewParticipantRepository;
import org.innovateuk.ifs.invite.resource.*;
import org.innovateuk.ifs.notifications.resource.ExternalUserNotificationTarget;
import org.innovateuk.ifs.notifications.resource.Notification;
import org.innovateuk.ifs.notifications.resource.NotificationTarget;
import org.innovateuk.ifs.notifications.resource.SystemNotificationSource;
import org.innovateuk.ifs.notifications.service.NotificationTemplateRenderer;
import org.innovateuk.ifs.notifications.service.senders.NotificationSender;
import org.innovateuk.ifs.profile.domain.Profile;
import org.innovateuk.ifs.profile.repository.ProfileRepository;
import org.innovateuk.ifs.review.domain.Review;
import org.innovateuk.ifs.review.mapper.ReviewInviteMapper;
import org.innovateuk.ifs.review.repository.ReviewRepository;
import org.innovateuk.ifs.review.resource.ReviewState;
import org.innovateuk.ifs.security.LoggedInUserSupplier;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.user.repository.UserRepository;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.workflow.domain.ActivityState;
import org.innovateuk.ifs.workflow.domain.ActivityType;
import org.innovateuk.ifs.workflow.repository.ActivityStateRepository;
import org.innovateuk.ifs.workflow.resource.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.innovateuk.ifs.commons.error.CommonErrors.notFoundError;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.*;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.invite.constant.InviteStatus.CREATED;
import static org.innovateuk.ifs.invite.constant.InviteStatus.OPENED;
import static org.innovateuk.ifs.invite.domain.Invite.generateInviteHash;
import static org.innovateuk.ifs.invite.domain.ParticipantStatus.*;
import static org.innovateuk.ifs.invite.domain.competition.CompetitionParticipantRole.PANEL_ASSESSOR;
import static org.innovateuk.ifs.util.CollectionFunctions.mapWithIndex;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.innovateuk.ifs.util.EntityLookupCallbacks.find;
import static org.innovateuk.ifs.util.MapFunctions.asMap;
import static org.innovateuk.ifs.util.StringFunctions.plainTextToHtml;
import static org.innovateuk.ifs.util.StringFunctions.stripHtml;


/*
 * Service for managing {@link ReviewInvite}s.
 */
@Service
@Transactional
public class ReviewInviteServiceImpl implements ReviewInviteService {

    private static final String WEB_CONTEXT = "/assessment";
    private static final DateTimeFormatter detailsFormatter = ofPattern("d MMM yyyy");

    @Autowired
    private ReviewInviteRepository reviewInviteRepository;

    @Autowired
    private CompetitionParticipantRepository competitionParticipantRepository;

    @Autowired
    private ReviewParticipantRepository reviewParticipantRepository;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Autowired
    private InnovationAreaMapper innovationAreaMapper;

    @Autowired
    private ReviewInviteMapper reviewInviteMapper;

    @Autowired
    private ParticipantStatusMapper participantStatusMapper;

    @Autowired
    private ReviewParticipantMapper reviewParticipantMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private NotificationTemplateRenderer renderer;

    @Autowired
    private SystemNotificationSource systemNotificationSource;

    @Autowired
    private LoggedInUserSupplier loggedInUserSupplier;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ActivityStateRepository activityStateRepository;

    enum Notifications {
        INVITE_ASSESSOR_TO_PANEL,
        INVITE_ASSESSOR_GROUP_TO_PANEL
    }

    @Value("${ifs.web.baseURL}")
    private String webBaseUrl;

    @Override
    public ServiceResult<AssessorInvitesToSendResource> getAllInvitesToSend(long competitionId) {
        return getCompetition(competitionId).andOnSuccess(competition -> {
            List<ReviewInvite> invites = reviewInviteRepository.getByCompetitionIdAndStatus(competition.getId(), CREATED);

            List<String> recipients = simpleMap(invites, ReviewInvite::getName);
            recipients.sort(String::compareTo);

            return serviceSuccess(new AssessorInvitesToSendResource(
                    recipients,
                    competition.getId(),
                    competition.getName(),
                    getInvitePreviewContent(competition)
            ));
        });
    }

    @Override
    public ServiceResult<AssessorInvitesToSendResource> getAllInvitesToResend(long competitionId, List<Long> inviteIds) {
        return getCompetition(competitionId).andOnSuccess(competition -> {

            List<ReviewInvite> invites = reviewInviteRepository.getByIdIn(inviteIds);
            List<String> recipients = simpleMap(invites, ReviewInvite::getName);
            recipients.sort(String::compareTo);

            return serviceSuccess(new AssessorInvitesToSendResource(
                    recipients,
                    competition.getId(),
                    competition.getName(),
                    getInvitePreviewContent(competition)
            ));
        });
    }

    @Override
    public ServiceResult<Void> sendAllInvites(long competitionId, AssessorInviteSendResource assessorInviteSendResource) {
        return getCompetition(competitionId).andOnSuccess(competition -> {

            String customTextPlain = stripHtml(assessorInviteSendResource.getContent());
            String customTextHtml = plainTextToHtml(customTextPlain);

            return ServiceResult.processAnyFailuresOrSucceed(simpleMap(
                    reviewInviteRepository.getByCompetitionIdAndStatus(competition.getId(), CREATED),
                    invite -> {
                        reviewParticipantRepository.save(
                                new ReviewParticipant(invite.send(loggedInUserSupplier.get(), now()))
                        );

                        return sendInviteNotification(
                                assessorInviteSendResource.getSubject(),
                                customTextPlain,
                                customTextHtml,
                                invite,
                                Notifications.INVITE_ASSESSOR_GROUP_TO_PANEL
                        );
                    }
            ));
        });
    }

    @Override
    public ServiceResult<Void> resendInvites(List<Long> inviteIds, AssessorInviteSendResource assessorInviteSendResource) {
        String customTextPlain = stripHtml(assessorInviteSendResource.getContent());
        String customTextHtml = plainTextToHtml(customTextPlain);

        return ServiceResult.processAnyFailuresOrSucceed(simpleMap(
                reviewInviteRepository.getByIdIn(inviteIds),
                invite -> sendInviteNotification(
                        assessorInviteSendResource.getSubject(),
                        customTextPlain,
                        customTextHtml,
                        invite.sendOrResend(loggedInUserSupplier.get(), now()),
                        Notifications.INVITE_ASSESSOR_GROUP_TO_PANEL
                )
        ));
    }

    @Override
        public ServiceResult<AvailableAssessorPageResource> getAvailableAssessors(long competitionId, Pageable pageable) {
            final Page<AssessmentParticipant> pagedResult = competitionParticipantRepository.findParticipantsNotOnAssessmentPanel(competitionId, pageable);

            return serviceSuccess(new AvailableAssessorPageResource(
                    pagedResult.getTotalElements(),
                    pagedResult.getTotalPages(),
                    simpleMap(pagedResult.getContent(), this::mapToAvailableAssessorResource),
                    pagedResult.getNumber(),
                    pagedResult.getSize()
            ));
        }

    @Override
    public ServiceResult<List<Long>> getAvailableAssessorIds(long competitionId) {
        List<AssessmentParticipant> result = competitionParticipantRepository.findParticipantsNotOnAssessmentPanel(competitionId);

        return serviceSuccess(simpleMap(result, competitionParticipant -> competitionParticipant.getUser().getId()));
    }

    private AvailableAssessorResource mapToAvailableAssessorResource(CompetitionParticipant participant) {
        User assessor = participant.getUser();
        Profile profile = profileRepository.findOne(assessor.getProfileId());

        AvailableAssessorResource availableAssessor = new AvailableAssessorResource();
        availableAssessor.setId(assessor.getId());
        availableAssessor.setEmail(assessor.getEmail());
        availableAssessor.setName(assessor.getName());
        availableAssessor.setBusinessType(profile.getBusinessType());
        availableAssessor.setCompliant(profile.isCompliant(assessor));
        availableAssessor.setInnovationAreas(simpleMap(profile.getInnovationAreas(), innovationAreaMapper::mapToResource));

        return availableAssessor;
    }

    @Override
    public ServiceResult<AssessorCreatedInvitePageResource> getCreatedInvites(long competitionId, Pageable pageable) {
        Page<ReviewInvite> pagedResult = reviewInviteRepository.getByCompetitionIdAndStatus(competitionId, CREATED, pageable);

        List<AssessorCreatedInviteResource> createdInvites = simpleMap(
                pagedResult.getContent(),
                competitionInvite -> {
                    AssessorCreatedInviteResource assessorCreatedInvite = new AssessorCreatedInviteResource();
                    assessorCreatedInvite.setName(competitionInvite.getName());
                    assessorCreatedInvite.setInnovationAreas(getInnovationAreasForInvite(competitionInvite));
                    assessorCreatedInvite.setCompliant(isUserCompliant(competitionInvite));
                    assessorCreatedInvite.setEmail(competitionInvite.getEmail());
                    assessorCreatedInvite.setInviteId(competitionInvite.getId());

                    if (competitionInvite.getUser() != null) {
                        assessorCreatedInvite.setId(competitionInvite.getUser().getId());
                    }

                    return assessorCreatedInvite;
                }
        );

        return serviceSuccess(new AssessorCreatedInvitePageResource(
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                createdInvites,
                pagedResult.getNumber(),
                pagedResult.getSize()
        ));
    }

    @Override
    public ServiceResult<Void> inviteUsers(List<ExistingUserStagedInviteResource> stagedInvites) {
        return serviceSuccess(mapWithIndex(stagedInvites, (i, invite) ->
                getUserById(invite.getUserId()).andOnSuccess(user ->
                        getByEmailAndCompetition(user.getEmail(), invite.getCompetitionId()).andOnFailure(() ->
                                inviteUserToCompetition(user, invite.getCompetitionId())
                        )))).andOnSuccessReturnVoid();
    }

    @Override
    public ServiceResult<AssessorInviteOverviewPageResource> getInvitationOverview(long competitionId,
                                                                                   Pageable pageable,
                                                                                   List<ParticipantStatus> statuses) {
        Page<ReviewParticipant> pagedResult = reviewParticipantRepository.getPanelAssessorsByCompetitionAndStatusContains(
                    competitionId,
                    statuses,
                    pageable);

        List<AssessorInviteOverviewResource> inviteOverviews = simpleMap(
                pagedResult.getContent(),
                participant -> {
                    AssessorInviteOverviewResource assessorInviteOverview = new AssessorInviteOverviewResource();
                    assessorInviteOverview.setName(participant.getInvite().getName());
                    assessorInviteOverview.setStatus(participantStatusMapper.mapToResource(participant.getStatus()));
                    assessorInviteOverview.setDetails(getDetails(participant));
                    assessorInviteOverview.setInviteId(participant.getInvite().getId());

                    if (participant.getUser() != null) {
                        Profile profile = profileRepository.findOne(participant.getUser().getProfileId());

                        assessorInviteOverview.setId(participant.getUser().getId());
                        assessorInviteOverview.setBusinessType(profile.getBusinessType());
                        assessorInviteOverview.setCompliant(profile.isCompliant(participant.getUser()));
                        assessorInviteOverview.setInnovationAreas(simpleMap(profile.getInnovationAreas(), innovationAreaMapper::mapToResource));
                    }

                    return assessorInviteOverview;
                });

        return serviceSuccess(new AssessorInviteOverviewPageResource(
                pagedResult.getTotalElements(),
                pagedResult.getTotalPages(),
                inviteOverviews,
                pagedResult.getNumber(),
                pagedResult.getSize()
        ));
    }

    private String getDetails(ReviewParticipant participant) {
        String details = null;

        if (participant.getStatus() == REJECTED) {
            details = "Invite declined";
        } else if (participant.getStatus() == PENDING) {
            if (participant.getInvite().getSentOn() != null) {
                details = format("Invite sent: %s", participant.getInvite().getSentOn().format(detailsFormatter));
            }
        }

        return details;
    }

    @Override
    public ServiceResult<List<Long>> getNonAcceptedAssessorInviteIds(long competitionId) {
        List<ReviewParticipant> participants = reviewParticipantRepository.getPanelAssessorsByCompetitionAndStatusContains(
                competitionId,
                asList(PENDING, REJECTED));

        return serviceSuccess(simpleMap(participants, participant -> participant.getInvite().getId()));
    }

    private ServiceResult<ReviewInvite> inviteUserToCompetition(User user, long competitionId) {
        return getCompetition(competitionId)
                .andOnSuccessReturn(
                        competition -> reviewInviteRepository.save(new ReviewInvite(user, generateInviteHash(), competition))
                );

    }

    @Override
    public ServiceResult<List<ReviewParticipantResource>> getAllInvitesByUser(long userId) {
        List<ReviewParticipantResource> reviewParticipantResources =
                reviewParticipantRepository
                .findByUserIdAndRole(userId, PANEL_ASSESSOR)
                .stream()
                .filter(participant -> now().isBefore(participant.getInvite().getTarget().getAssessmentPanelDate()))
                .map(reviewParticipantMapper::mapToResource)
                .collect(toList());

        reviewParticipantResources.forEach(this::determineStatusOfPanelApplications);

        return serviceSuccess(reviewParticipantResources);
    }

    private ServiceResult<Competition> getCompetition(long competitionId) {
        return find(competitionRepository.findOne(competitionId), notFoundError(Competition.class, competitionId));
    }

    private String getInvitePreviewContent(NotificationTarget notificationTarget, Map<String, Object> arguments) {

        return renderer.renderTemplate(systemNotificationSource, notificationTarget, "invite_assessors_to_assessors_panel_text.txt",
                arguments).getSuccess();
    }

    private String getInvitePreviewContent(Competition competition) {
        NotificationTarget notificationTarget = new ExternalUserNotificationTarget("", "");

        return getInvitePreviewContent(notificationTarget, asMap(
                "competitionName", competition.getName()
        ));
    }

    private ServiceResult<Void> sendInviteNotification(String subject,
                                                       String customTextPlain,
                                                       String customTextHtml,
                                                       ReviewInvite invite,
                                                       Notifications notificationType) {
        NotificationTarget recipient = new ExternalUserNotificationTarget(invite.getName(), invite.getEmail());
        Notification notification = new Notification(
                systemNotificationSource,
                recipient,
                notificationType,
                asMap(
                        "subject", subject,
                        "name", invite.getName(),
                        "competitionName", invite.getTarget().getName(),
                        "inviteUrl", format("%s/invite/panel/%s", webBaseUrl + WEB_CONTEXT, invite.getHash()),
                        "customTextPlain", customTextPlain,
                        "customTextHtml", customTextHtml
                ));

        return notificationSender.sendNotification(notification).andOnSuccessReturnVoid();
    }

    private ServiceResult<User> getUserById(long id) {
        return find(userRepository.findOne(id), notFoundError(User.class, id));
    }

    private ServiceResult<ReviewInvite> getByEmailAndCompetition(String email, long competitionId) {
        return find(reviewInviteRepository.getByEmailAndCompetitionId(email, competitionId), notFoundError(AssessmentInvite.class, email, competitionId));
    }

    private boolean isUserCompliant(ReviewInvite competitionInvite) {
        if (competitionInvite == null || competitionInvite.getUser() == null) {
            return false;
        }
        Profile profile = profileRepository.findOne(competitionInvite.getUser().getProfileId());
        return profile.isCompliant(competitionInvite.getUser());
    }

    private List<InnovationAreaResource> getInnovationAreasForInvite(ReviewInvite competitionInvite) {
        return profileRepository.findOne(competitionInvite.getUser().getProfileId()).getInnovationAreas().stream()
                .map(innovationAreaMapper::mapToResource)
                .collect(toList());
    }

    @Override
    public ServiceResult<ReviewInviteResource> openInvite(String inviteHash) {
        return getByHashIfOpen(inviteHash)
                .andOnSuccessReturn(this::openInvite)
                .andOnSuccessReturn(reviewInviteMapper::mapToResource);
    }

    private ReviewInvite openInvite(ReviewInvite invite) {
        return reviewInviteRepository.save(invite.open());
    }

    @Override
    public ServiceResult<Void> acceptInvite(String inviteHash) {
        return getParticipantByInviteHash(inviteHash)
                .andOnSuccess(ReviewInviteServiceImpl::accept)
                .andOnSuccess(this::assignAllPanelApplicationsToParticipant)
                .andOnSuccessReturnVoid();
    }

    private ServiceResult<Void> assignAllPanelApplicationsToParticipant(ReviewParticipant participant) {
        Competition competition = participant.getProcess();
        List<Application> applicationsInPanel = applicationRepository.findByCompetitionAndInAssessmentReviewPanelTrueAndApplicationProcessActivityStateState(competition, State.SUBMITTED);
        final ActivityState pendingActivityState = activityStateRepository.findOneByActivityTypeAndState(ActivityType.ASSESSMENT_REVIEW, State.PENDING);
        applicationsInPanel.forEach(application -> {
            Review review = new Review(application, participant, Role.PANEL_ASSESSOR);
            review.setActivityState(pendingActivityState);
            reviewRepository.save(review);
        });
        return serviceSuccess();
    }

    private ServiceResult<ReviewParticipant> getParticipantByInviteHash(String inviteHash) {
        return find(reviewParticipantRepository.getByInviteHash(inviteHash), notFoundError(ReviewParticipant.class, inviteHash));
    }

    @Override
    public ServiceResult<Void> rejectInvite(String inviteHash) {
        return getParticipantByInviteHash(inviteHash)
                .andOnSuccess(this::reject)
                .andOnSuccessReturnVoid();
    }

    @Override
    public ServiceResult<Boolean> checkExistingUser(@P("inviteHash") String inviteHash) {
        return getByHash(inviteHash).andOnSuccessReturn(invite -> {
            if (invite.getUser() != null) {
                return TRUE;
            }

            return userRepository.findByEmail(invite.getEmail()).isPresent();
        });
    }

    private ServiceResult<ReviewInvite> getByHash(String inviteHash) {
        return find(reviewInviteRepository.getByHash(inviteHash), notFoundError(AssessmentInvite.class, inviteHash));
    }

    private static ServiceResult<ReviewParticipant> accept(ReviewParticipant participant) {
        User user = participant.getUser();
        if (participant.getInvite().getStatus() != OPENED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_ACCEPT_UNOPENED_INVITE, getInviteCompetitionName(participant)));
        } else if (participant.getStatus() == ACCEPTED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_ACCEPT_ALREADY_ACCEPTED_INVITE, getInviteCompetitionName(participant)));
        } else if (participant.getStatus() == REJECTED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_ACCEPT_ALREADY_REJECTED_INVITE, getInviteCompetitionName(participant)));
        } else {
            return serviceSuccess( participant.acceptAndAssignUser(user));
        }
    }

    private ServiceResult<CompetitionParticipant> reject(ReviewParticipant participant) {
        if (participant.getInvite().getStatus() != OPENED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_REJECT_UNOPENED_INVITE, getInviteCompetitionName(participant)));
        } else if (participant.getStatus() == ACCEPTED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_REJECT_ALREADY_ACCEPTED_INVITE, getInviteCompetitionName(participant)));
        } else if (participant.getStatus() == REJECTED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_PARTICIPANT_CANNOT_REJECT_ALREADY_REJECTED_INVITE, getInviteCompetitionName(participant)));
        } else {
            return serviceSuccess(participant.reject());
        }
    }

    private static String getInviteCompetitionName(ReviewParticipant participant) {
        return participant.getInvite().getTarget().getName();
    }

    private ServiceResult<ReviewInvite> getByHashIfOpen(String inviteHash) {
        return getByHash(inviteHash).andOnSuccess(invite -> {

            if (invite.getTarget().getAssessmentPanelDate() == null || now().isAfter(invite.getTarget().getAssessmentPanelDate())) {
                return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_INVITE_EXPIRED, invite.getTarget().getName()));
            }

            ReviewParticipant participant = reviewParticipantRepository.getByInviteHash(inviteHash);

            if (participant == null) {
                return serviceSuccess(invite);
            }

            if (participant.getStatus() == ACCEPTED || participant.getStatus() == REJECTED) {
                return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_INVITE_CLOSED, invite.getTarget().getName()));
            }
            return serviceSuccess(invite);
        });
    }

    @Override
    public ServiceResult<Void> deleteInvite(String email, long competitionId) {
        return getByEmailAndCompetition(email, competitionId).andOnSuccess(this::deleteInvite);
    }

    @Override
    public ServiceResult<Void> deleteAllInvites(long competitionId) {
        return find(competitionRepository.findOne(competitionId), notFoundError(Competition.class, competitionId))
                .andOnSuccessReturnVoid(competition ->
                        reviewInviteRepository.deleteByCompetitionIdAndStatus(competition.getId(), CREATED));
    }

    private ServiceResult<Void> deleteInvite(ReviewInvite invite) {
        if (invite.getStatus() != CREATED) {
            return ServiceResult.serviceFailure(new Error(ASSESSMENT_PANEL_INVITE_CANNOT_DELETE_ONCE_SENT, invite.getEmail()));
        }

        reviewInviteRepository.delete(invite);
        return serviceSuccess();
    }

    private void determineStatusOfPanelApplications(ReviewParticipantResource reviewParticipantResource) {

        List<Review> reviews = reviewRepository.
                findByParticipantUserIdAndTargetCompetitionIdOrderByActivityStateStateAscIdAsc(
                        reviewParticipantResource.getUserId(),
                        reviewParticipantResource.getCompetitionId());

        reviewParticipantResource.setAwaitingApplications(getApplicationsPendingForPanelCount(reviews));
    }

    private Long getApplicationsPendingForPanelCount(List<Review> reviews) {
        return reviews.stream().filter(review -> review.getActivityState().equals(ReviewState.PENDING)).count();
    }
}