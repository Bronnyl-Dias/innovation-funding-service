package org.innovateuk.ifs.finance.domain;

import org.innovateuk.ifs.application.domain.Application;
import org.innovateuk.ifs.application.repository.ApplicationRepository;
import org.innovateuk.ifs.category.domain.ResearchCategory;
import org.innovateuk.ifs.category.repository.ResearchCategoryRepository;
import org.innovateuk.ifs.commons.BaseIntegrationTest;
import org.innovateuk.ifs.competition.publiccontent.resource.FundingType;
import org.innovateuk.ifs.finance.repository.ApplicationFinanceRepository;
import org.innovateuk.ifs.finance.repository.OrganisationSizeRepository;
import org.innovateuk.ifs.finance.resource.ApplicationFinanceResource;
import org.innovateuk.ifs.finance.transactional.FinanceRowService;
import org.innovateuk.ifs.testdata.builders.ApplicationDataBuilder;
import org.innovateuk.ifs.testdata.builders.ApplicationFinanceDataBuilder;
import org.innovateuk.ifs.testdata.builders.CompetitionDataBuilder;
import org.innovateuk.ifs.testdata.builders.ServiceLocator;
import org.innovateuk.ifs.testdata.builders.data.ApplicationData;
import org.innovateuk.ifs.testdata.builders.data.CompetitionData;
import org.innovateuk.ifs.user.domain.Organisation;
import org.innovateuk.ifs.user.repository.OrganisationRepository;
import org.innovateuk.ifs.user.resource.OrganisationTypeEnum;
import org.innovateuk.ifs.user.resource.RoleResource;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.innovateuk.ifs.user.transactional.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.testdata.builders.ApplicationDataBuilder.newApplicationData;
import static org.innovateuk.ifs.testdata.builders.CompetitionDataBuilder.newCompetitionData;
import static org.innovateuk.ifs.user.builder.RoleResourceBuilder.newRoleResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.junit.Assert.assertEquals;

public class GrantClaimMaximumIntegrationTest extends BaseIntegrationTest {

    private static final Optional<Long> SMALL_SIZE = Optional.of(1L);
    private static final Optional<Long> MEDIUM_SIZE = Optional.of(2L);
    private static final Optional<Long> LARGE_SIZE = Optional.of(3L);
    private static final Optional<Long> NO_SIZE = Optional.empty();

    @Autowired
    private GenericApplicationContext applicationContext;

    @Autowired
    private UserService userService;

    @Autowired
    private FinanceRowService financeRowService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ResearchCategoryRepository researchCategoryRepository;

    @Autowired
    private OrganisationSizeRepository organisationSizeRepository;

    @Autowired
    private ApplicationFinanceRepository applicationFinanceRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private CompetitionDataBuilder competitionDataBuilder;
    private ApplicationDataBuilder applicationDataBuilder;

    @Before
    public void setupData() {

        ServiceLocator serviceLocator = new ServiceLocator(applicationContext, "compadmin@innovateuk.test", "pfifs2100@gmail.vom");
        competitionDataBuilder = newCompetitionData(serviceLocator);
        applicationDataBuilder = newApplicationData(serviceLocator);
    }

    @Test
    @Transactional
    @Rollback
    public void testApcMaximumGrantClaimsForBusiness() {

        // For speed, all of these tests are combined in the same test method so that we don't have to continually recreate the
        // APC competition
        CompetitionData competitionData = createApcCompetition("Feasibility studies");

        // test business types
        ApplicationData businessApplicationData = createApplication(competitionData, "steve.smith@empire.com", "Empire Ltd", SMALL_SIZE, false);

        // test business types - small
        assertApcMaximumGrant(businessApplicationData, "Feasibility studies", SMALL_SIZE, 70);
        assertApcMaximumGrant(businessApplicationData, "Industrial research", SMALL_SIZE, 70);
        assertApcMaximumGrant(businessApplicationData, "Experimental development", SMALL_SIZE, 45);

        // test business types - medium
        assertApcMaximumGrant(businessApplicationData, "Feasibility studies", MEDIUM_SIZE, 60);
        assertApcMaximumGrant(businessApplicationData, "Industrial research", MEDIUM_SIZE, 60);
        assertApcMaximumGrant(businessApplicationData, "Experimental development", MEDIUM_SIZE, 35);

        // test business types - large
        assertApcMaximumGrant(businessApplicationData, "Feasibility studies", LARGE_SIZE, 50);
        assertApcMaximumGrant(businessApplicationData, "Industrial research", LARGE_SIZE, 50);
        assertApcMaximumGrant(businessApplicationData, "Experimental development", LARGE_SIZE, 25);

        // test research types
        ApplicationData researchApplicationData = createApplication(competitionData, "pete.tom@egg.com", "EGGS", Optional.empty(), true);

        assertApcMaximumGrant(researchApplicationData, "Feasibility studies", NO_SIZE, 100);
        assertApcMaximumGrant(researchApplicationData, "Industrial research", NO_SIZE, 100);
        assertApcMaximumGrant(researchApplicationData, "Experimental development", NO_SIZE, 100);
    }

    private void assertApcMaximumGrant(ApplicationData applicationData, String researchCategoryName, Optional<Long> organisationSize, int expectedMaximumGrant) {

        UserResource leadApplicant = applicationData.getLeadApplicant();
        Organisation leadOrganisation = organisationRepository.findOneByName(applicationData.getApplication().getLeadOrganisationName());

        Long applicationId = applicationData.getApplication().getId();
        Long leadOrganisationId = leadOrganisation.getId();

        ResearchCategory researchCategory = researchCategoryRepository.findByName(researchCategoryName);
        Application application = applicationRepository.findOne(applicationId);
        application.setResearchCategory(researchCategory);

        organisationSize.ifPresent(sizeId -> {
            OrganisationSize size = organisationSizeRepository.findOne(sizeId);
            ApplicationFinance applicationFinance = applicationFinanceRepository.findByApplicationIdAndOrganisationId(applicationId, leadOrganisationId);
            applicationFinance.setOrganisationSize(size);
            applicationFinanceRepository.save(applicationFinance);

        });

        ApplicationFinanceResource financeDetails = getFinanceDetails(leadApplicant, applicationId, leadOrganisationId);

        assertEquals(Integer.valueOf(expectedMaximumGrant), financeDetails.getMaximumFundingLevel());
    }

    private ApplicationData createApplication(CompetitionData competitionData, String applicantEmailAddress, String organisationName, Optional<Long> organisationSize, boolean academic) {

        UserResource applicant = getUser(applicantEmailAddress);
        Organisation applicantOrganisation = organisationRepository.findOneByName(organisationName);

        ApplicationData applicationData = createApcApplication(competitionData, applicant, organisationSize, applicantOrganisation, academic);
        applicationData.getApplication().setLeadOrganisationName(applicantOrganisation.getName());
        return applicationData;
    }

    private ApplicationFinanceResource getFinanceDetails(UserResource applicant, Long applicationId, Long organisationId) {
        setLoggedInUser(applicant);
        return financeRowService.financeDetails(applicationId, organisationId).getSuccess();
    }

    private ApplicationData createApcApplication(CompetitionData competitionData, UserResource applicant, Optional<Long> organisationSize, Organisation applicantOrganisation, boolean academic) {

        return applicationDataBuilder.
                withCompetition(competitionData.getCompetition()).
                withBasicDetails(applicant, "APC Application", "Feasibility studies", false).
                beginApplication().
                withFinances(financeBuilder -> {

                    ApplicationFinanceDataBuilder financeBuilderWithoutCosts = financeBuilder.
                            withOrganisation(applicantOrganisation.getName()).
                            withUser(applicant.getEmail());

                    if (academic) {
                        return financeBuilderWithoutCosts.
                                withAcademicCosts(costBuilder -> costBuilder.
                                        withIndirectCosts(BigDecimal.ZERO));
                    } else {
                        return financeBuilderWithoutCosts.
                                withIndustrialCosts(costBuilder -> costBuilder.
                                        withOrganisationSize(organisationSize.orElse(null)).
                                        withGrantClaim(0));
                    }

                }).
                build();
    }

    private UserResource getUser(String emailAddress) {
        return userService.findByEmail(emailAddress).getSuccess();
    }

    private CompetitionData createApcCompetition(String researchCategory) {

        RoleResource adminRole = newRoleResource().withType(UserRoleType.IFS_ADMINISTRATOR).build();
        setLoggedInUser(newUserResource().withRolesGlobal(singletonList(adminRole)).build());

        CompetitionData competitionCreation = competitionDataBuilder.
                createCompetition().
                build();

        flushAndClearSession();

        CompetitionData competition = competitionDataBuilder.
                withExistingCompetition(competitionCreation).
                withBasicData("APC Competition", "Advanced Propulsion Centre",
                        singletonList("Digital manufacturing"), "Materials and manufacturing",
                        researchCategory, "ian.cooper@innovateuk.test",
                        "john.doe@innovateuk.test", "DET1536/1537", "875",
                        "CCCC", "16014", 1, BigDecimal.valueOf(100L),
                        false, false, false,
                        "single-or-collaborative", singletonList(OrganisationTypeEnum.BUSINESS),
                        50, false, "").
                withApplicationFormFromTemplate().
                withNewMilestones().
                withOpenDate(ZonedDateTime.now().minus(1, ChronoUnit.DAYS)).
                withBriefingDate(addDays(1)).
                withSubmissionDate(addDays(2)).
                withAllocateAssesorsDate(addDays(3)).
                withAssessorBriefingDate(addDays(4)).
                withAssessorAcceptsDate(addDays(5)).
                withAssessorsNotifiedDate(addDays(6)).
                withAssessorEndDate(addDays(7)).
                withAssessmentClosedDate(addDays(8)).
                withLineDrawDate(addDays(9)).
                withAsessmentPanelDate(addDays(10)).
                withPanelDate(addDays(11)).
                withFundersPanelDate(addDays(12)).
                withFundersPanelEndDate(addDays(13)).
                withReleaseFeedbackDate(addDays(14)).
                withFeedbackReleasedDate(addDays(15)).
                withPublicContent(true, "blah", "blah", "blah",
                        "blah", FundingType.GRANT, "blah", singletonList("blah"), false).
                withSetupComplete().
                build();

        flushAndClearSession();

        return competition;
    }

    private ZonedDateTime addDays(int amount) {
        return ZonedDateTime.now().plus(amount, ChronoUnit.DAYS);
    }

    private void flushAndClearSession() {
        entityManager.flush();
        entityManager.clear();
    }
}
