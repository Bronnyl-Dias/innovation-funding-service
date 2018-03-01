package org.innovateuk.ifs.application.transactional;

import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.application.resource.ApplicationIneligibleSendResource;
import org.innovateuk.ifs.application.resource.ApplicationState;
import org.innovateuk.ifs.application.workflow.configuration.ApplicationWorkflowHandler;
import org.innovateuk.ifs.commons.error.Error;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.domain.Competition;
import org.innovateuk.ifs.email.resource.EmailAddress;
import org.innovateuk.ifs.email.resource.EmailContent;
import org.innovateuk.ifs.notifications.resource.ExternalUserNotificationTarget;
import org.innovateuk.ifs.notifications.resource.Notification;
import org.innovateuk.ifs.notifications.resource.NotificationTarget;
import org.innovateuk.ifs.notifications.resource.SystemNotificationSource;
import org.innovateuk.ifs.notifications.service.NotificationService;
import org.innovateuk.ifs.notifications.service.senders.NotificationSender;
import org.innovateuk.ifs.user.domain.ProcessRole;
import org.innovateuk.ifs.user.domain.Role;
import org.innovateuk.ifs.user.domain.User;
import org.innovateuk.ifs.workflow.resource.State;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.LambdaMatcher.createLambdaMatcher;
import static org.innovateuk.ifs.application.builder.ApplicationBuilder.newApplication;
import static org.innovateuk.ifs.application.builder.ApplicationIneligibleSendResourceBuilder.newApplicationIneligibleSendResource;
import static org.innovateuk.ifs.application.transactional.ApplicationNotificationServiceImpl.Notifications.APPLICATION_SUBMITTED;
import static org.innovateuk.ifs.commons.error.CommonFailureKeys.APPLICATION_MUST_BE_INELIGIBLE;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceFailure;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionBuilder.newCompetition;
import static org.innovateuk.ifs.email.builders.EmailContentResourceBuilder.newEmailContentResource;
import static org.innovateuk.ifs.notifications.resource.NotificationMedium.EMAIL;
import static org.innovateuk.ifs.user.builder.ProcessRoleBuilder.newProcessRole;
import static org.innovateuk.ifs.user.builder.RoleBuilder.newRole;
import static org.innovateuk.ifs.user.builder.UserBuilder.newUser;
import static org.innovateuk.ifs.user.resource.UserRoleType.COLLABORATOR;
import static org.innovateuk.ifs.user.resource.UserRoleType.LEADAPPLICANT;
import static org.innovateuk.ifs.util.CollectionFunctions.asLinkedSet;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMapSet;
import static org.innovateuk.ifs.util.MapFunctions.asMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * TODO: Add description
 */
public class ApplicationNotificationServiceImplTest {
    @Mock
    private ApplicationRepository applicationRepositoryMock;

    @Mock
    private NotificationService notificationServiceMock;

    @Mock
    private NotificationSender notificationSenderMock;

    @Mock
    private SystemNotificationSource systemNotificationSourceMock;

    @Mock
    private ApplicationWorkflowHandler applicationWorkflowHandlerMock;

    @InjectMocks
    private ApplicationNotificationService service;

    private static final String WEB_BASE_URL = "www.baseUrl.com" ;
    private static final Set<State> FUNDING_DECISIONS_MADE_STATUSES = simpleMapSet(asLinkedSet(
            ApplicationState.APPROVED,
            ApplicationState.REJECTED), ApplicationState::getBackingState);

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        ReflectionTestUtils.setField(service, "webBaseUrl", WEB_BASE_URL);
    }

    @Test
    public void sendNotificationApplicationSubmitted() {
        User leadUser = newUser().withEmailAddress("leadapplicant@example.com").build();
        Role leadRole = newRole().withType(LEADAPPLICANT).build();
        ProcessRole leadProcessRole = newProcessRole().withUser(leadUser).withRole(leadRole).build();
        Competition competition = newCompetition().build();
        Application application = newApplication().withProcessRoles(leadProcessRole).withCompetition(competition).build();
        when(applicationRepositoryMock.findOne(application.getId())).thenReturn(application);
        when(notificationServiceMock.sendNotification(any(), eq(EMAIL))).thenReturn(ServiceResult.serviceSuccess());

        ServiceResult<Void> result = service.sendNotificationApplicationSubmitted(application.getId());

        verify(notificationServiceMock).sendNotification(createLambdaMatcher(notification -> {
            assertEquals(application.getName(), notification.getGlobalArguments().get("applicationName"));
            assertEquals(competition.getName(), notification.getGlobalArguments().get("competitionName"));
            assertEquals(1, notification.getTo().size());
            assertEquals(leadUser.getEmail(), notification.getTo().get(0).getEmailAddress());
            assertEquals(leadUser.getName(), notification.getTo().get(0).getName());
            assertEquals(APPLICATION_SUBMITTED, notification.getMessageKey());
        }), eq(EMAIL));
        assertTrue(result.isSuccess());
    }

    @Test
    public void notifyApplicantsByCompetition() throws Exception {
        Long competitionId = 1L;
        Long applicationOneId = 2L;
        Long applicationTwoId = 3L;
        Long applicationThreeId = 4L;

        Competition competition = newCompetition()
                .withName("Competition")
                .build();

        List<User> users = newUser()
                .withFirstName("John", "Jane", "Bob")
                .withLastName("Smith", "Jones", "Davies")
                .withEmailAddress("john@smith.com", "jane@jones.com", "bob@davie.com")
                .build(3);

        List<ProcessRole> processRoles = newProcessRole()
                .withUser(users.get(0), users.get(1), users.get(2))
                .withRole(newRole().withType(LEADAPPLICANT).withUrl("url").build())
                .build(3);

        List<Application> applications = newApplication()
                .withCompetition(competition)
                .withId(applicationOneId, applicationTwoId, applicationThreeId)
                .withName("App1", "App2", "App3")
                .build(3);

        applications.get(0).setProcessRoles(singletonList(processRoles.get(0)));
        applications.get(1).setProcessRoles(singletonList(processRoles.get(1)));
        applications.get(2).setProcessRoles(singletonList(processRoles.get(2)));

        processRoles.get(0).setApplicationId(applicationOneId);
        processRoles.get(1).setApplicationId(applicationTwoId);
        processRoles.get(2).setApplicationId(applicationThreeId);

        List<NotificationTarget> notificationTargets = asList(
                new ExternalUserNotificationTarget(users.get(0).getName(), users.get(0).getEmail()),
                new ExternalUserNotificationTarget(users.get(1).getName(), users.get(1).getEmail()),
                new ExternalUserNotificationTarget(users.get(2).getName(), users.get(2).getEmail())
        );

        List<EmailContent> emailContents = newEmailContentResource()
                .build(3);

        List<Notification> notifications = asList(
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(0)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(0).getName(),
                                "applicationName", applications.get(0).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(0).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(1)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(1).getName(),
                                "applicationName", applications.get(1).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(1).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(2)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(2).getName(),
                                "applicationName", applications.get(2).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(2).getRole().getUrl())
                )
        );

        when(applicationRepositoryMock.findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES)).thenReturn(applications);

        when(applicationRepositoryMock.findOne(applicationOneId)).thenReturn(applications.get(0));
        when(applicationRepositoryMock.findOne(applicationTwoId)).thenReturn(applications.get(1));
        when(applicationRepositoryMock.findOne(applicationThreeId)).thenReturn(applications.get(2));

        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(0))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(0), emailContents.get(0))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(1))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(1), emailContents.get(1))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(2))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(2), emailContents.get(2))));

        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(0)),
                Matchers.eq(notificationTargets.get(0)),
                Matchers.eq(emailContents.get(0))))
                .thenReturn(serviceSuccess(
                        singletonList(new EmailAddress(users.get(0).getEmail(), users.get(0).getName()))));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(1)),
                Matchers.eq(notificationTargets.get(1)),
                Matchers.eq(emailContents.get(1))))
                .thenReturn(serviceSuccess(
                        singletonList(new EmailAddress(users.get(1).getEmail(), users.get(1).getName()))));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(2)),
                Matchers.eq(notificationTargets.get(2)),
                Matchers.eq(emailContents.get(2))))
                .thenReturn(serviceSuccess(
                        singletonList(new EmailAddress(users.get(2).getEmail(), users.get(2).getName()))));

        ServiceResult<Void> result = service.notifyApplicantsByCompetition(competitionId);

        InOrder inOrder = inOrder(applicationRepositoryMock, notificationSenderMock);
        inOrder.verify(applicationRepositoryMock).findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES);

        inOrder.verify(applicationRepositoryMock).findOne(applicationOneId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(0));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(0), notificationTargets.get(0), emailContents.get(0));

        inOrder.verify(applicationRepositoryMock).findOne(applicationTwoId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(1));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(1), notificationTargets.get(1), emailContents.get(1));

        inOrder.verify(applicationRepositoryMock).findOne(applicationThreeId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(2));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(2), notificationTargets.get(2), emailContents.get(2));

        inOrder.verifyNoMoreInteractions();

        assertTrue(result.isSuccess());
    }

    @Test
    public void notifyApplicantsByCompetition_oneFailure() throws Exception {
        Long competitionId = 1L;
        Long applicationOneId = 2L;
        Long applicationTwoId = 3L;
        Long applicationThreeId = 4L;

        Competition competition = newCompetition()
                .withName("Competition")
                .build();

        List<User> users = newUser()
                .withFirstName("John", "Jane", "Bob")
                .withLastName("Smith", "Jones", "Davies")
                .withEmailAddress("john@smith.com", "jane@jones.com", "bob@davie.com")
                .build(3);

        List<ProcessRole> processRoles = newProcessRole()
                .withUser(users.get(0), users.get(1), users.get(2))
                .withRole(newRole().withType(LEADAPPLICANT).withUrl("url").build())
                .build(3);

        List<Application> applications = newApplication()
                .withCompetition(competition)
                .withId(applicationOneId, applicationTwoId, applicationThreeId)
                .withName("App1", "App2", "App3")
                .build(3);

        applications.get(0).setProcessRoles(singletonList(processRoles.get(0)));
        applications.get(1).setProcessRoles(singletonList(processRoles.get(1)));
        applications.get(2).setProcessRoles(singletonList(processRoles.get(2)));

        processRoles.get(0).setApplicationId(applicationOneId);
        processRoles.get(1).setApplicationId(applicationTwoId);
        processRoles.get(2).setApplicationId(applicationThreeId);

        List<NotificationTarget> notificationTargets = asList(
                new ExternalUserNotificationTarget(users.get(0).getName(), users.get(0).getEmail()),
                new ExternalUserNotificationTarget(users.get(1).getName(), users.get(1).getEmail()),
                new ExternalUserNotificationTarget(users.get(2).getName(), users.get(2).getEmail())
        );

        List<EmailContent> emailContents = newEmailContentResource()
                .build(3);

        List<Notification> notifications = asList(
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(0)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(0).getName(),
                                "applicationName", applications.get(0).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(0).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(1)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(1).getName(),
                                "applicationName", applications.get(1).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(1).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(2)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(2).getName(),
                                "applicationName", applications.get(2).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(2).getRole().getUrl())
                )
        );

        when(applicationRepositoryMock.findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES)).thenReturn(applications);

        when(applicationRepositoryMock.findOne(applicationOneId)).thenReturn(applications.get(0));
        when(applicationRepositoryMock.findOne(applicationTwoId)).thenReturn(applications.get(1));
        when(applicationRepositoryMock.findOne(applicationThreeId)).thenReturn(applications.get(2));

        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(0))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(0), emailContents.get(0))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(1))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(1), emailContents.get(1))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(2))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(2), emailContents.get(2))));

        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(0)),
                Matchers.eq(notificationTargets.get(0)),
                Matchers.eq(emailContents.get(0))))
                .thenReturn(serviceSuccess(
                        singletonList(new EmailAddress(users.get(0).getEmail(), users.get(0).getName()))));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(1)),
                Matchers.eq(notificationTargets.get(1)),
                Matchers.eq(emailContents.get(1))))
                .thenReturn(serviceSuccess(
                        singletonList(new EmailAddress(users.get(1).getEmail(), users.get(1).getName()))));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(2)),
                Matchers.eq(notificationTargets.get(2)),
                Matchers.eq(emailContents.get(2))))
                .thenReturn(serviceFailure(new Error("error", INTERNAL_SERVER_ERROR)));

        ServiceResult<Void> result = service.notifyApplicantsByCompetition(competitionId);

        InOrder inOrder = inOrder(applicationRepositoryMock, notificationSenderMock);
        inOrder.verify(applicationRepositoryMock).findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES);

        inOrder.verify(applicationRepositoryMock).findOne(applicationOneId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(0));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(0), notificationTargets.get(0), emailContents.get(0));

        inOrder.verify(applicationRepositoryMock).findOne(applicationTwoId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(1));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(1), notificationTargets.get(1), emailContents.get(1));

        inOrder.verify(applicationRepositoryMock).findOne(applicationThreeId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(2));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(2), notificationTargets.get(2), emailContents.get(2));

        inOrder.verifyNoMoreInteractions();

        assertTrue(result.isFailure());
        assertEquals(1, result.getErrors().size());
        assertEquals("error", result.getErrors().get(0).getErrorKey());
        assertEquals(INTERNAL_SERVER_ERROR, result.getErrors().get(0).getStatusCode());
    }

    @Test
    public void notifyApplicantsByCompetition_allFailure() throws Exception {
        Long competitionId = 1L;
        Long applicationOneId = 2L;
        Long applicationTwoId = 3L;
        Long applicationThreeId = 4L;

        Competition competition = newCompetition()
                .withName("Competition")
                .build();

        List<User> users = newUser()
                .withFirstName("John", "Jane", "Bob")
                .withLastName("Smith", "Jones", "Davies")
                .withEmailAddress("john@smith.com", "jane@jones.com", "bob@davie.com")
                .build(3);

        List<ProcessRole> processRoles = newProcessRole()
                .withUser(users.get(0), users.get(1), users.get(2))
                .withRole(newRole().withType(LEADAPPLICANT).withUrl("url").build())
                .build(3);

        List<Application> applications = newApplication()
                .withCompetition(competition)
                .withId(applicationOneId, applicationTwoId, applicationThreeId)
                .withName("App1", "App2", "App3")
                .build(3);

        applications.get(0).setProcessRoles(singletonList(processRoles.get(0)));
        applications.get(1).setProcessRoles(singletonList(processRoles.get(1)));
        applications.get(2).setProcessRoles(singletonList(processRoles.get(2)));

        processRoles.get(0).setApplicationId(applicationOneId);
        processRoles.get(1).setApplicationId(applicationTwoId);
        processRoles.get(2).setApplicationId(applicationThreeId);

        List<NotificationTarget> notificationTargets = asList(
                new ExternalUserNotificationTarget(users.get(0).getName(), users.get(0).getEmail()),
                new ExternalUserNotificationTarget(users.get(1).getName(), users.get(1).getEmail()),
                new ExternalUserNotificationTarget(users.get(2).getName(), users.get(2).getEmail())
        );

        List<EmailContent> emailContents = newEmailContentResource()
                .build(3);

        List<Notification> notifications = asList(
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(0)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(0).getName(),
                                "applicationName", applications.get(0).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(0).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(1)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(1).getName(),
                                "applicationName", applications.get(1).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(1).getRole().getUrl())
                ),
                new Notification(
                        systemNotificationSourceMock,
                        singletonList(notificationTargets.get(2)),
                        ApplicationNotificationServiceImpl.Notifications.APPLICATION_FUNDED_ASSESSOR_FEEDBACK_PUBLISHED,
                        asMap("name", users.get(2).getName(),
                                "applicationName", applications.get(2).getName(),
                                "competitionName", competition.getName(),
                                "dashboardUrl", WEB_BASE_URL + "/" + processRoles.get(2).getRole().getUrl())
                )
        );

        when(applicationRepositoryMock.findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES)).thenReturn(applications);

        when(applicationRepositoryMock.findOne(applicationOneId)).thenReturn(applications.get(0));
        when(applicationRepositoryMock.findOne(applicationTwoId)).thenReturn(applications.get(1));
        when(applicationRepositoryMock.findOne(applicationThreeId)).thenReturn(applications.get(2));

        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(0))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(0), emailContents.get(0))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(1))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(1), emailContents.get(1))));
        when(notificationSenderMock.renderTemplates(Matchers.eq(notifications.get(2))))
                .thenReturn(serviceSuccess(asMap(notificationTargets.get(2), emailContents.get(2))));

        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(0)),
                Matchers.eq(notificationTargets.get(0)),
                Matchers.eq(emailContents.get(0))))
                .thenReturn(serviceFailure(new Error("error", INTERNAL_SERVER_ERROR)));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(1)),
                Matchers.eq(notificationTargets.get(1)),
                Matchers.eq(emailContents.get(1))))
                .thenReturn(serviceFailure(new Error("error", INTERNAL_SERVER_ERROR)));
        when(notificationSenderMock.sendEmailWithContent(
                Matchers.eq(notifications.get(2)),
                Matchers.eq(notificationTargets.get(2)),
                Matchers.eq(emailContents.get(2))))
                .thenReturn(serviceFailure(new Error("error", INTERNAL_SERVER_ERROR)));

        ServiceResult<Void> result = service.notifyApplicantsByCompetition(competitionId);

        InOrder inOrder = inOrder(applicationRepositoryMock, notificationSenderMock);
        inOrder.verify(applicationRepositoryMock).findByCompetitionIdAndApplicationProcessActivityStateStateIn(competitionId, FUNDING_DECISIONS_MADE_STATUSES);

        inOrder.verify(applicationRepositoryMock).findOne(applicationOneId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(0));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(0), notificationTargets.get(0), emailContents.get(0));

        inOrder.verify(applicationRepositoryMock).findOne(applicationTwoId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(1));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(1), notificationTargets.get(1), emailContents.get(1));

        inOrder.verify(applicationRepositoryMock).findOne(applicationThreeId);
        inOrder.verify(notificationSenderMock).renderTemplates(notifications.get(2));
        inOrder.verify(notificationSenderMock)
                .sendEmailWithContent(notifications.get(2), notificationTargets.get(2), emailContents.get(2));

        inOrder.verifyNoMoreInteractions();

        assertTrue(result.isFailure());
        assertEquals(3, result.getErrors().size());
        assertEquals("error", result.getErrors().get(0).getErrorKey());
        assertEquals(INTERNAL_SERVER_ERROR, result.getErrors().get(0).getStatusCode());
        assertEquals("error", result.getErrors().get(1).getErrorKey());
        assertEquals(INTERNAL_SERVER_ERROR, result.getErrors().get(1).getStatusCode());
        assertEquals("error", result.getErrors().get(2).getErrorKey());
        assertEquals(INTERNAL_SERVER_ERROR, result.getErrors().get(2).getStatusCode());
    }

    @Test
    public void informIneligible() throws Exception {
        long applicationId = 1L;
        String subject = "subject";
        String content = "content";
        String email = "email@address.com";
        String firstName = "first";
        String lastName = "last";
        String fullName = String.format("%s %s", firstName, lastName);

        ApplicationIneligibleSendResource resource = newApplicationIneligibleSendResource()
                .withSubject(subject)
                .withContent(content)
                .build();

        User[] users = newUser()
                .withFirstName(firstName, "other")
                .withLastName(lastName, "other")
                .withEmailAddress(email, "other@email.com")
                .buildArray(2, User.class);

        ProcessRole[] processRoles = newProcessRole()
                .withUser(users)
                .withRole(LEADAPPLICANT, COLLABORATOR)
                .buildArray(2, ProcessRole.class);

        Application application = newApplication()
                .withId(applicationId)
                .withProcessRoles(processRoles)
                .build();

        Map<String, Object> expectedNotificationArguments = asMap(
                "subject", subject,
                "bodyPlain", content,
                "bodyHtml", content
        );

        SystemNotificationSource from = systemNotificationSourceMock;
        NotificationTarget to = new ExternalUserNotificationTarget(fullName, email);
        Notification notification = new Notification(
                from,
                singletonList(to),
                ApplicationNotificationServiceImpl.Notifications
                .APPLICATION_INELIGIBLE,
                expectedNotificationArguments
        );

        when(applicationRepositoryMock.findOne(applicationId)).thenReturn(application);
        when(applicationWorkflowHandlerMock.informIneligible(application)).thenReturn(true);
        when(notificationSenderMock.sendNotification(notification)).thenReturn(serviceSuccess(notification));

        ServiceResult<Void> serviceResult = service.informIneligible(applicationId, resource);
        assertTrue(serviceResult.isSuccess());

        InOrder inOrder = inOrder(applicationRepositoryMock, applicationWorkflowHandlerMock, notificationSenderMock);
        inOrder.verify(applicationRepositoryMock).findOne(applicationId);
        inOrder.verify(applicationWorkflowHandlerMock).informIneligible(application);
        inOrder.verify(applicationRepositoryMock).save(application);
        inOrder.verify(notificationSenderMock).sendNotification(notification);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void informIneligible_workflowError() throws Exception {
        long applicationId = 1L;
        String subject = "subject";
        String content = "content";

        ApplicationIneligibleSendResource resource = newApplicationIneligibleSendResource()
                .withSubject(subject)
                .withContent(content)
                .build();

        Application application = newApplication()
                .withId(applicationId)
                .build();

        when(applicationRepositoryMock.findOne(applicationId)).thenReturn(application);
        when(applicationWorkflowHandlerMock.informIneligible(application)).thenReturn(false);

        ServiceResult<Void> serviceResult = service.informIneligible(applicationId, resource);
        assertTrue(serviceResult.isFailure());
        assertEquals(APPLICATION_MUST_BE_INELIGIBLE.getErrorKey(), serviceResult.getErrors().get(0).getErrorKey());

        InOrder inOrder = inOrder(applicationRepositoryMock, applicationWorkflowHandlerMock, notificationSenderMock);
        inOrder.verify(applicationRepositoryMock).findOne(applicationId);
        inOrder.verify(applicationWorkflowHandlerMock).informIneligible(application);
        inOrder.verifyNoMoreInteractions();
    }
}