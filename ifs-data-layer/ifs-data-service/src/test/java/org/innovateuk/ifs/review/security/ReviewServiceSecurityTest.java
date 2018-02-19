package org.innovateuk.ifs.review.security;

import org.innovateuk.ifs.BaseServiceSecurityTest;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.review.resource.ReviewRejectOutcomeResource;
import org.innovateuk.ifs.review.resource.ReviewResource;
import org.innovateuk.ifs.review.transactional.ReviewService;
import org.innovateuk.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.innovateuk.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.innovateuk.ifs.review.builder.ReviewRejectOutcomeResourceBuilder.newReviewRejectOutcomeResource;
import static org.innovateuk.ifs.review.builder.ReviewResourceBuilder.newReviewResource;
import static org.innovateuk.ifs.user.resource.UserRoleType.COMP_ADMIN;
import static org.innovateuk.ifs.user.resource.UserRoleType.PROJECT_FINANCE;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class ReviewServiceSecurityTest extends BaseServiceSecurityTest<ReviewService> {

    private static final long applicationId = 1L;
    private static final long competitionId = 2L;
    private static final long userId = 3L;
    private static final long reviewId = 4L;
    private static int ARRAY_SIZE_FOR_POST_FILTER_TESTS = 2;
    private ReviewPermissionRules reviewPermissionRules;
    private ReviewLookupStrategy reviewLookupStrategy;


    @Override
    protected Class<? extends ReviewService> getClassUnderTest() {
        return TestReviewService.class;
    }

    @Before
    public void setUp() throws Exception {
        reviewPermissionRules = getMockPermissionRulesBean(ReviewPermissionRules.class);
        reviewLookupStrategy = getMockPermissionEntityLookupStrategiesBean(ReviewLookupStrategy.class);
    }

    @Test
    public void assignApplicationToPanel() throws Exception {
        testOnlyAUserWithOneOfTheGlobalRolesCan(() -> classUnderTest.assignApplicationToPanel(applicationId), COMP_ADMIN, PROJECT_FINANCE);
    }

    @Test
    public void unAssignApplicationFromPanel() throws Exception {
        testOnlyAUserWithOneOfTheGlobalRolesCan(() -> classUnderTest.unassignApplicationFromPanel(applicationId), COMP_ADMIN, PROJECT_FINANCE);
    }

    @Test
    public void getAssessmentReviews() {
        classUnderTest.getReviews(userId, competitionId);
        verify(reviewPermissionRules, times(ARRAY_SIZE_FOR_POST_FILTER_TESTS)).userCanReadAssessmentReviewOnDashboard(isA(ReviewResource.class), isA(UserResource.class));
    }

    @Test
    public void getAssessmentReview() {
        when(reviewLookupStrategy.getAssessmentReviewResource(reviewId)).thenReturn(newReviewResource().withId(reviewId).build());
        assertAccessDenied(
                () -> classUnderTest.getReview(reviewId),
                () -> verify(reviewPermissionRules).userCanReadAssessmentReviews(isA(ReviewResource.class), isA(UserResource.class))
        );
    }

    @Test
    public void acceptAssessmentReview() {
        when(reviewLookupStrategy.getAssessmentReviewResource(reviewId)).thenReturn(newReviewResource().withId(reviewId).build());
        assertAccessDenied(
                () -> classUnderTest.acceptReview(reviewId),
                () -> verify(reviewPermissionRules).userCanUpdateAssessmentReview(isA(ReviewResource.class), isA(UserResource.class))
        );
    }

    @Test
    public void rejectAssessmentReview() {
        ReviewRejectOutcomeResource rejectOutcomeResource = newReviewRejectOutcomeResource().build();
        when(reviewLookupStrategy.getAssessmentReviewResource(reviewId)).thenReturn(newReviewResource().withId(reviewId).build());
        assertAccessDenied(
                () -> classUnderTest.rejectReview(reviewId, rejectOutcomeResource),
                () -> verify(reviewPermissionRules).userCanUpdateAssessmentReview(isA(ReviewResource.class), isA(UserResource.class))
        );
    }

    public static class TestReviewService implements ReviewService {

        @Override
        public ServiceResult<Void> assignApplicationToPanel(long applicationId) {
            return null;
        }

        @Override
        public ServiceResult<Void> unassignApplicationFromPanel(long applicationId) {
            return null;
        }

        @Override
        public ServiceResult<Void> createAndNotifyReviews(long competitionId) {
            return null;
        }

        @Override
        public ServiceResult<Boolean> isPendingReviewNotifications(long competitionId) {
            return null;
        }

        @Override
        public ServiceResult<List<ReviewResource>> getReviews(long userId, long competitionId) {
            return serviceSuccess(newReviewResource().build(ARRAY_SIZE_FOR_POST_FILTER_TESTS));
        }

        @Override
        public ServiceResult<ReviewResource> getReview(long assessmentReviewId) {
            return null;
        }

        @Override
        public ServiceResult<Void> acceptReview(long assessmentReviewId) {
            return null;
        }

        @Override
        public ServiceResult<Void> rejectReview(long assessmentReviewId, ReviewRejectOutcomeResource reviewRejectOutcomeResource) {
            return null;
        }
    }
}