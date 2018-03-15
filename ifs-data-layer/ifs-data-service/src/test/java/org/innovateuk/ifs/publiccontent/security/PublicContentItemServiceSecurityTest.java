package org.innovateuk.ifs.publiccontent.security;

import org.innovateuk.ifs.BaseServiceSecurityTest;
import org.innovateuk.ifs.commons.service.ServiceResult;
import org.innovateuk.ifs.competition.publiccontent.resource.PublicContentItemPageResource;
import org.innovateuk.ifs.competition.publiccontent.resource.PublicContentItemResource;
import org.innovateuk.ifs.competition.resource.CompetitionCompositeId;
import org.innovateuk.ifs.competition.security.CompetitionLookupStrategy;
import org.innovateuk.ifs.publiccontent.transactional.PublicContentItemService;
import org.innovateuk.ifs.user.resource.Role;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.user.resource.Role.SYSTEM_REGISTRATION_USER;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PublicContentItemServiceSecurityTest extends BaseServiceSecurityTest<PublicContentItemService> {
    private PublicContentItemPermissionRules rules;
    private CompetitionLookupStrategy competitionLookupStrategies;

    @Before
    public void setUp() throws Exception {
        rules = getMockPermissionRulesBean(PublicContentItemPermissionRules.class);
        competitionLookupStrategies = getMockPermissionEntityLookupStrategiesBean(CompetitionLookupStrategy.class);
        initMocks(this);
    }

    @Override
    protected Class<? extends PublicContentItemService> getClassUnderTest() {
        return TestPublicContentItemService.class;
    }

    @Test
    public void testFindFilteredItemsByCompetitionId() {
        runAsRole(SYSTEM_REGISTRATION_USER, () -> classUnderTest.findFilteredItems(Optional.of(1L), Optional.of("test"), Optional.of(1), 1));
    }

    @Test
    public void testGetByCompetitionId() {
        when(competitionLookupStrategies.getCompetitionCompositeId(1L)).thenReturn(CompetitionCompositeId.id(1L));
        assertAccessDenied(() -> classUnderTest.byCompetitionId(1L), this::verifyPermissionRules);
    }

    private void verifyPermissionRules() {
        verify(rules).allUsersCanViewPublishedContent(any(), any());
    }

    private void runAsRole(Role roleType, Runnable serviceCall) {
        setLoggedInUser(
                newUserResource()
                        .withRolesGlobal(singletonList(Role.getByName(roleType.getName())))
                        .build());
        serviceCall.run();
    }

    public static class TestPublicContentItemService implements PublicContentItemService {
        @Override
        public ServiceResult<PublicContentItemPageResource> findFilteredItems(Optional<Long> innovationAreaId, Optional<String> searchString, Optional<Integer> pageNumber, Integer pageSize) { return null; }

        @Override
        public ServiceResult<PublicContentItemResource> byCompetitionId(Long id) { return null; }

    }
}
