package org.innovateuk.ifs.analytics;

import org.innovateuk.ifs.BaseUnitTestMocksTest;
import org.innovateuk.ifs.analytics.service.GoogleAnalyticsDataLayerRestService;
import org.innovateuk.ifs.commons.rest.RestResult;
import org.innovateuk.ifs.commons.security.authentication.user.UserAuthentication;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.user.resource.UserRoleType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.innovateuk.ifs.analytics.GoogleAnalyticsDataLayerInterceptor.ANALYTICS_DATA_LAYER_NAME;
import static org.innovateuk.ifs.user.builder.RoleResourceBuilder.newRoleResource;
import static org.innovateuk.ifs.user.builder.UserResourceBuilder.newUserResource;
import static org.innovateuk.ifs.util.CollectionFunctions.simpleMap;
import static org.innovateuk.ifs.util.JsonMappingUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

public class GoogleAnalyticsDataLayerInterceptorTest extends BaseUnitTestMocksTest {

    @Mock
    private GoogleAnalyticsDataLayerRestService googleAnalyticsDataLayerRestServiceMock;

    @InjectMocks
    private GoogleAnalyticsDataLayerInterceptor googleAnalyticsDataLayerInterceptor;

    @Mock
    private HttpServletRequest httpServletRequestMock;

    @Mock
    private HttpServletResponse httpServletResponseMock;

    private ModelAndView mav;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        mav = new ModelAndView();
        setAuthenticatedRoleTypes();
    }

    @Test
    public void postHandle() {
        final String expectedCompName = "competition name";
        final long expectedCompetitionId = 7L;

        when(googleAnalyticsDataLayerRestServiceMock.getCompetitionName(expectedCompetitionId)).thenReturn(RestResult.restSuccess(toJson(expectedCompName)));

        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(singletonMap("competitionId", Long.toString(expectedCompetitionId)));

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setCompetitionName(expectedCompName);
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));

        verify(googleAnalyticsDataLayerRestServiceMock, only()).getCompetitionName(expectedCompetitionId);
    }

    @Test
    public void postHandle_applicationId() {
        final String expectedCompName = "competition name";
        final long expectedApplicationId = 7L;

        when(googleAnalyticsDataLayerRestServiceMock.getCompetitionNameForApplication(expectedApplicationId)).thenReturn(RestResult.restSuccess(toJson(expectedCompName)));

        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(singletonMap("applicationId", Long.toString(expectedApplicationId)));

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setCompetitionName(expectedCompName);
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));

        verify(googleAnalyticsDataLayerRestServiceMock, only()).getCompetitionNameForApplication(expectedApplicationId);
    }

    @Test
    public void postHandle_projectId() {
        final String expectedCompName = "competition name";
        final long expectedProjectId = 7L;

        when(googleAnalyticsDataLayerRestServiceMock.getCompetitionNameForProject(expectedProjectId)).thenReturn(RestResult.restSuccess(toJson(expectedCompName)));

        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(singletonMap("projectId", Long.toString(expectedProjectId)));

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setCompetitionName(expectedCompName);
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));

        verify(googleAnalyticsDataLayerRestServiceMock, only()).getCompetitionNameForProject(expectedProjectId);
    }

    @Test
    public void postHandle_assessmentId() {
        final String expectedCompName = "competition name";
        final long expectedAssessmentId = 7L;

        when(googleAnalyticsDataLayerRestServiceMock.getCompetitionNameForAssessment(expectedAssessmentId)).thenReturn(RestResult.restSuccess(toJson(expectedCompName)));

        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(singletonMap("assessmentId", Long.toString(expectedAssessmentId)));

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setCompetitionName(expectedCompName);
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));

        verify(googleAnalyticsDataLayerRestServiceMock, only()).getCompetitionNameForAssessment(expectedAssessmentId);
    }

    @Test
    public void postHandle_noParam() {
        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(emptyMap());

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));

        verifyZeroInteractions(googleAnalyticsDataLayerRestServiceMock);
    }

    @Test
    public void postHandle_singleRole() {
        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(emptyMap());

        UserRoleType [] expectedUserRoleTypes = setAuthenticatedRoleTypes(UserRoleType.COMP_ADMIN);

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setUserRole(String.join(",", simpleMap(expectedUserRoleTypes, UserRoleType::getName)));

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));
    }

    @Test
    public void postHandle_multipleRoles() {
        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(emptyMap());

        UserRoleType [] expectedUserRoleTypes = setAuthenticatedRoleTypes(UserRoleType.COMP_ADMIN, UserRoleType.IFS_ADMINISTRATOR);

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setUserRole(String.join(",", simpleMap(expectedUserRoleTypes, UserRoleType::getName)));

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));
    }

    @Test
    public void postHandle_anon() {
        when(httpServletRequestMock.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE)).thenReturn(emptyMap());

        googleAnalyticsDataLayerInterceptor.postHandle(httpServletRequestMock, httpServletResponseMock, null, mav);

        GoogleAnalyticsDataLayer expectedDataLayer = new GoogleAnalyticsDataLayer();
        expectedDataLayer.setUserRole("anonymous");

        assertEquals(expectedDataLayer, mav.getModel().get(ANALYTICS_DATA_LAYER_NAME));
    }

    private UserRoleType[] setAuthenticatedRoleTypes(UserRoleType... expectedUserRoleTypes) {
        UserResource user = newUserResource()
                .withRolesGlobal(newRoleResource().withType(expectedUserRoleTypes).build(expectedUserRoleTypes.length))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(user));
        return expectedUserRoleTypes;
    }
}