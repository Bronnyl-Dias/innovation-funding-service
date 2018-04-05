package org.innovateuk.ifs.application.transactional;

import org.innovateuk.ifs.BaseServiceSecurityTest;
import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.domain.IneligibleOutcome;
import org.innovateuk.ifs.application.resource.ApplicationPageResource;
import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.application.resource.ApplicationState;
import org.innovateuk.ifs.application.resource.CompletedPercentageResource;
import org.innovateuk.ifs.application.security.ApplicationLookupStrategy;
import org.innovateuk.ifs.application.security.ApplicationPermissionRules;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder;
import org.innovateuk.ifs.competition.resource.CompetitionResource;
import org.innovateuk.ifs.competition.resource.CompetitionStatus;
import org.innovateuk.ifs.competition.security.CompetitionLookupStrategy;
import org.innovateuk.ifs.competition.security.CompetitionPermissionRules;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.method.P;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;
import static org.innovateuk.ifs.application.builder.ApplicationResourceBuilder.newApplicationResource;
import static org.innovateuk.ifs.application.builder.IneligibleOutcomeBuilder.newIneligibleOutcome;
import static org.innovateuk.ifs.application.resource.ApplicationState.SUBMITTED;
import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.competition.builder.CompetitionResourceBuilder.newCompetitionResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.user.resource.Role.APPLICANT;
import static org.innovateuk.ifs.user.resource.Role.SYSTEM_REGISTRATION_USER;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Testing the security annotations on the ApplicationService interface
 */
public class ApplicationServiceSecurityTest extends BaseServiceSecurityTest<ApplicationService> {
    private ApplicationPermissionRules applicationRules;
    private CompetitionPermissionRules competitionRules;
    private ApplicationLookupStrategy applicationLookupStrategy;
    private CompetitionLookupStrategy competitionLookupStrategy;

    @Before
    public void lookupPermissionRules() {
        applicationRules = getMockPermissionRulesBean(ApplicationPermissionRules.class);
        competitionRules = getMockPermissionRulesBean(CompetitionPermissionRules.class);
        applicationLookupStrategy = getMockPermissionEntityLookupStrategiesBean(ApplicationLookupStrategy.class);
        competitionLookupStrategy = getMockPermissionEntityLookupStrategiesBean(CompetitionLookupStrategy.class);
    }

    @Test
    public void testGetApplicationResource() {
        final long applicationId = 1L;
        when(applicationLookupStrategy.getApplicationResource(applicationId)).thenReturn(newApplicationResource().build());
        assertAccessDenied(
                () -> classUnderTest.getApplicationById(applicationId),
                () -> {
                    verify(applicationRules).usersConnectedToTheApplicationCanView(isA(ApplicationResource.class), isA(UserResource.class));
                    verify(applicationRules).internalUsersCanViewApplications(isA(ApplicationResource.class), isA(UserResource.class));
                    verify(applicationRules).innovationLeadAssginedToCompetitionCanViewApplications(isA(ApplicationResource.class), isA(UserResource.class));
                }
        );
    }

    @Test
    public void testCreateApplicationByAppNameForUserIdAndCompetitionId() {
        Long competitionId = 123L;
        Long userId = 456L;
        setLoggedInUser(newUserResource().withId(userId).withRolesGlobal(singletonList(APPLICANT)).build());
        when(competitionLookupStrategy.getCompetititionResource(competitionId)).thenReturn(newCompetitionResource().withId(competitionId).withCompetitionStatus(CompetitionStatus.READY_TO_OPEN).build());
        assertAccessDenied(
                () -> classUnderTest.createApplicationByApplicationNameForUserIdAndCompetitionId("An application", competitionId, userId),
                () -> {
                    verify(applicationRules).userCanCreateNewApplication(isA(CompetitionResource.class), isA(UserResource.class));
                }
        );
    }

    @Test
    public void testCreateApplicationByAppNameForUserIdAndCompetitionId_deniedIfNotLoggedIn() {

        setLoggedInUser(null);
        try {
            classUnderTest.createApplicationByApplicationNameForUserIdAndCompetitionId("An application", 123L, 456L);
            fail("Should not have been able to create an Application without first logging in");
        } catch (AccessDeniedException e) {
            // expected behaviour
        }
    }

    @Test
    public void testCreateApplicationByAppNameForUserIdAndCompetitionId_deniedIfNoGlobalRolesAtAll() {

        try {
            classUnderTest.createApplicationByApplicationNameForUserIdAndCompetitionId("An application", 123L, 456L);
            fail("Should not have been able to create an Application without the global Applicant role");
        } catch (AccessDeniedException e) {
            // expected behaviour
        }
    }

    @Test
    public void testCreateApplicationByAppNameForUserIdAndCompetitionId_deniedIfNotCorrectGlobalRolesOrASystemRegistrar() {
        EnumSet<Role> nonApplicantRoles = complementOf(of(APPLICANT, SYSTEM_REGISTRATION_USER));

        nonApplicantRoles.forEach(role -> {
            setLoggedInUser(newUserResource().withRolesGlobal(singletonList(Role.getByName(role.getName()))).build());

            try {
                classUnderTest.createApplicationByApplicationNameForUserIdAndCompetitionId("An application", 123L, 456L);
                fail("Should not have been able to create an Application without the global Applicant role or as a system registrar");
            } catch (AccessDeniedException e) {
                // expected behaviour
            }
        });
    }

    @Test
    public void markAsIneligible() {
        when(applicationLookupStrategy.getApplicationResource(1L)).thenReturn(newApplicationResource().build());

        assertAccessDenied(
                () -> classUnderTest.markAsIneligible(1L, newIneligibleOutcome().build()),
                () -> verify(applicationRules).markAsInelgibileAllowedBeforeAssesment(isA(ApplicationResource.class), isA(UserResource.class))
        );
    }

    @Test
    public void updateApplicationState() {
        when(applicationLookupStrategy.getApplicationResource(1L)).thenReturn(newApplicationResource().build());

        assertAccessDenied(
                () -> classUnderTest.updateApplicationState(1L, SUBMITTED),
                () -> {
                    verify(applicationRules).compAdminCanUpdateApplicationState(isA(ApplicationResource.class), isA(UserResource.class));
                    verify(applicationRules).leadApplicantCanUpdateApplicationState(isA(ApplicationResource.class), isA(UserResource.class));
                    verify(applicationRules).projectFinanceCanUpdateApplicationState(isA(ApplicationResource.class), isA(UserResource.class));
                }
        );
    }

    @Test
    public void findUnsuccessfulApplications() {
        Long competitionId = 1L;
        CompetitionResource competitionResource = CompetitionResourceBuilder.newCompetitionResource().build();

        when(competitionLookupStrategy.getCompetititionResource(competitionId)).thenReturn(competitionResource);

        assertAccessDenied(() -> classUnderTest.findUnsuccessfulApplications(competitionId, 0, 0, ""), () -> {
            verify(competitionRules).internalUsersAndIFSAdminCanViewUnsuccessfulApplications(any(CompetitionResource.class), any(UserResource.class));
            verify(competitionRules).innovationLeadForCompetitionCanViewUnsuccessfulApplications(any(CompetitionResource.class), any(UserResource.class));
            verifyNoMoreInteractions(competitionRules);
        });
    }

    @Override
    protected Class<? extends ApplicationService> getClassUnderTest() {
        return TestApplicationService.class;
    }

    /**
     * Dummy implementation (for satisfying Spring Security's need to read parameter information from
     * methods, which is lost when using mocks)
     */
    public static class TestApplicationService implements ApplicationService {

        @Override
        public ServiceResult<ApplicationResource> createApplicationByApplicationNameForUserIdAndCompetitionId(String applicationName, Long competitionId, Long userId) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> getApplicationById(Long id) {
            return null;
        }

        @Override
        public ServiceResult<List<ApplicationResource>> findAll() {
            return null;
        }

        @Override
        public ServiceResult<List<ApplicationResource>> findByUserId(Long userId) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationPageResource> wildcardSearchById(String searchString, Pageable pageable) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> saveApplicationDetails(Long id, ApplicationResource application) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> saveApplicationSubmitDateTime(@P("applicationId") Long id, ZonedDateTime date) {
            return null;
        }

        @Override
        public ServiceResult<CompletedPercentageResource> getProgressPercentageByApplicationId(Long applicationId) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> updateApplicationState(Long id, ApplicationState state) {
            return null;
        }

        @Override
        public ServiceResult<List<ApplicationResource>> getApplicationsByCompetitionIdAndUserId(Long competitionId, Long userId, Role role) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> findByProcessRole(Long id) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationPageResource> findUnsuccessfulApplications(Long competitionId, int pageIndex, int pageSize, String sortField) {
            return serviceSuccess(new ApplicationPageResource());
        }

        @Override
        public ServiceResult<Stream<Application>> getApplicationsByState(Collection<ApplicationState> applicationStates) {
            return null;
        }

        @Override
        public ServiceResult<List<Application>> getApplicationsByCompetitionIdAndState(Long competitionId, Collection<ApplicationState> applicationStates) {
            return null;
        }

        @Override
        public ServiceResult<Void> markAsIneligible(long applicationId, IneligibleOutcome reason) {
            return null;
        }

        @Override
        public ServiceResult<ApplicationResource> setApplicationFundingEmailDateTime(@P("applicationId") final Long applicationId, final ZonedDateTime fundingEmailDate) {
            return null;
        }


        @Override
        public ServiceResult<Boolean> showApplicationTeam(Long applicationId, Long userId) {
            return null;
        }
    }
}
