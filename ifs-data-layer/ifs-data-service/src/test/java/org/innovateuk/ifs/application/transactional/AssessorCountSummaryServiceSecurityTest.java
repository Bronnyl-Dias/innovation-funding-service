package org.innovateuk.ifs.application.transactional;

import org.innovateuk.ifs.BaseServiceSecurityTest;
import org.innovateuk.ifs.application.resource.AssessorCountSummaryPageResource;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.user.resource.BusinessType;
import org.innovateuk.ifs.user.resource.Role;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.Optional;

import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;

/**
 * Testing the security annotations on the AssessorCountSummaryService interface
 */
public class AssessorCountSummaryServiceSecurityTest extends BaseServiceSecurityTest<AssessorCountSummaryService> {

    @Test
    public void testGetAssessorCountSummariesByCompetitionId() {
        setLoggedInUser(
                newUserResource()
                        .withRolesGlobal(Collections.singletonList(Role.COMP_ADMIN))
                        .build()
        );
        classUnderTest.getAssessorCountSummariesByCompetitionId(1L,  Optional.empty(), Optional.empty(),0, 0);
    }

    @Test(expected = AccessDeniedException.class)
    public void testGetAssessorCountSummariesByCompetitionId_notCompadmin() {
        setLoggedInUser(newUserResource().build());
        classUnderTest.getAssessorCountSummariesByCompetitionId(1L, Optional.empty(), Optional.empty(),0, 0);
    }

    @Override
    protected Class<? extends AssessorCountSummaryService> getClassUnderTest() {
        return TestAssessorCountSummaryService.class;
    }

    public static class TestAssessorCountSummaryService implements AssessorCountSummaryService {

        @Override
        public ServiceResult<AssessorCountSummaryPageResource> getAssessorCountSummariesByCompetitionId(long competitionId, Optional<Long> innovationSector, Optional<BusinessType> businessType, int pageIndex, int pageSize) {
            return null;
        }
    }
}
