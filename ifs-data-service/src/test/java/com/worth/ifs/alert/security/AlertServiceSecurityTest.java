package com.worth.ifs.alert.security;

import com.worth.ifs.BaseServiceSecurityTest;
import com.worth.ifs.alert.domain.AlertType;
import com.worth.ifs.alert.resource.AlertResource;
import com.worth.ifs.alert.transactional.AlertService;
import com.worth.ifs.commons.service.ServiceResult;
import com.worth.ifs.user.resource.UserResource;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.worth.ifs.alert.builder.AlertResourceBuilder.newAlertResource;
import static com.worth.ifs.alert.domain.AlertType.MAINTENANCE;
import static com.worth.ifs.commons.service.ServiceResult.serviceSuccess;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class AlertServiceSecurityTest extends BaseServiceSecurityTest<AlertService> {

    private AlertPermissionRules alertPermissionRules;
    private AlertLookupStrategy alertLookupStrategy;

    private static final int ARRAY_SIZE_FOR_TESTS = 2;

    @Override
    protected Class<? extends AlertService> getServiceClass() {
        return TestAlertService.class;
    }

    @Before
    public void setUp() throws Exception {
        alertPermissionRules = getMockPermissionRulesBean(AlertPermissionRules.class);
        alertLookupStrategy = getMockPermissionEntityLookupStrategiesBean(AlertLookupStrategy.class);
    }

    @Test
    public void test_findAllVisible() throws Exception {
        service.findAllVisible();
        verify(alertPermissionRules, times(ARRAY_SIZE_FOR_TESTS)).anyoneCanViewAlerts(isA(AlertResource.class), isA(UserResource.class));
    }

    @Test
    public void test_findAllVisibleByType() throws Exception {
        service.findAllVisibleByType(MAINTENANCE);
        verify(alertPermissionRules, times(ARRAY_SIZE_FOR_TESTS)).anyoneCanViewAlerts(isA(AlertResource.class), isA(UserResource.class));
    }

    @Test
    public void test_findById() throws Exception {
        when(alertLookupStrategy.getAlertResource(9999L)).thenReturn(newAlertResource()
                .withId(9999L)
                .build());

        assertAccessDenied(
                () -> service.findById(9999L),
                () -> {
                    verify(alertPermissionRules).anyoneCanViewAlerts(isA(AlertResource.class), isA(UserResource.class));
                });
    }

    @Test
    public void test_create() throws Exception {
        final AlertResource alertResource = newAlertResource()
                .build();
        assertAccessDenied(
                () -> service.create(alertResource),
                () -> {
                    verify(alertPermissionRules).competitionsAdminCanCreateAlerts(isA(AlertResource.class), isA(UserResource.class));
                });
    }

    @Test
    public void test_delete() throws Exception {
        when(alertLookupStrategy.getAlertResource(9999L)).thenReturn(newAlertResource()
                .withId(9999L)
                .build());

        assertAccessDenied(
                () -> service.delete(9999L),
                () -> {
                    verify(alertPermissionRules).competitionsAdminCanDeleteAlerts(isA(AlertResource.class), isA(UserResource.class));
                });
    }

    public static class TestAlertService implements AlertService {
        @Override
        public ServiceResult<List<AlertResource>> findAllVisible() {
            return serviceSuccess(newAlertResource().build(ARRAY_SIZE_FOR_TESTS));
        }

        @Override
        public ServiceResult<List<AlertResource>> findAllVisibleByType(final AlertType type) {
            return serviceSuccess(newAlertResource().build(ARRAY_SIZE_FOR_TESTS));
        }

        @Override
        public ServiceResult<AlertResource> findById(final Long id) {
            return null;
        }

        @Override
        public ServiceResult<AlertResource> create(final AlertResource alertResource) {
            return null;
        }

        @Override
        public ServiceResult<Void> delete(final Long id) {
            return null;
        }

        @Override
        public ServiceResult<Void> deleteAllByType(final AlertType type) {
            return null;
        }
    }
}
