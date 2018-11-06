package org.innovateuk.ifs.interceptors;

import org.innovateuk.ifs.commons.security.UserAuthenticationService;
import org.innovateuk.ifs.commons.security.authentication.user.UserAuthentication;
import org.innovateuk.ifs.user.resource.Role;
import org.innovateuk.ifs.user.resource.UserResource;
import org.innovateuk.ifs.util.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.innovateuk.ifs.user.resource.Role.*;

/**
 * Have the menu links globally available for each controller.
 * So it does not have to be added to each call separately anymore.
 */
public class MenuLinksHandlerInterceptor extends HandlerInterceptorAdapter {

    public static final String USER_DASHBOARD_LINK = "userDashboardLink";
    public static final String USER_LOGOUT_LINK = "logoutUrl";
    public static final String USER_PROFILE_LINK = "userProfileLink";
    public static final String ASSESSOR_PROFILE_URL = "/assessment/profile/details";
    public static final String USER_PROFILE_URL = "/profile/view";
    public static final String SHOW_MANAGE_USERS_LINK_ATTR = "showManageUsersLink";

    @Autowired
    private UserAuthenticationService userAuthenticationService;

    @Value("${logout.url}")
    private String logoutUrl;

    @Autowired
    private CookieUtil cookieUtil;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null && !(modelAndView.getView() instanceof RedirectView || modelAndView.getViewName().startsWith("redirect:"))) {
            addUserDashboardLink(request, modelAndView);
            addUserProfileLink(request, modelAndView);
            addLogoutLink(modelAndView, logoutUrl);
            addShowManageUsersAttribute(request, modelAndView);
        }
    }

    private void addUserDashboardLink(HttpServletRequest request, ModelAndView modelAndView) {
        String dashboardUrl = getUserDashboardUrl(request);
        modelAndView.getModelMap().addAttribute(USER_DASHBOARD_LINK, dashboardUrl);
    }

    private boolean isMultipleUserStakeholder(HttpServletRequest request){
        UserAuthentication authentication = (UserAuthentication) userAuthenticationService.getAuthentication(request);
        UserResource user = authentication.getDetails();
        String role = cookieUtil.getCookieValue(request, "role");

        return user.hasMoreThanOneRoleOf(ASSESSOR, APPLICANT, STAKEHOLDER) && "stakeholder".equals(role);
    }

    private void addUserProfileLink(HttpServletRequest request, ModelAndView modelAndView) {

        if (isMultipleUserStakeholder(request)) {
            return;
        }
        String profileUrl = getUserProfileUrl(request);
        modelAndView.getModelMap().addAttribute(USER_PROFILE_LINK, profileUrl);
    }

    private void addShowManageUsersAttribute(HttpServletRequest request, ModelAndView modelAndView) {
        UserResource user = userAuthenticationService.getAuthenticatedUser(request);
        modelAndView.getModelMap().addAttribute(SHOW_MANAGE_USERS_LINK_ATTR, user != null && user.hasRole(IFS_ADMINISTRATOR));
    }

    public static void addLogoutLink(ModelAndView modelAndView, String logoutUrl) {
        modelAndView.addObject(USER_LOGOUT_LINK, logoutUrl);
    }

    /**
     * Get the dashboard url, from the Role object.
     */
    private String getUserDashboardUrl(HttpServletRequest request) {
        UserAuthentication authentication = (UserAuthentication) userAuthenticationService.getAuthentication(request);
        if (authentication != null) {
            Optional<SimpleGrantedAuthority> simpleGrantedAuthority = (Optional<SimpleGrantedAuthority>) authentication.getAuthorities().stream().findFirst();
            if (simpleGrantedAuthority.isPresent()) {
                UserResource user = authentication.getDetails();
                String role = cookieUtil.getCookieValue(request, "role");
                if (!role.isEmpty()) {
                    Optional<Role> r = user.getRoles().stream().filter(roleResource -> roleResource.getName().equals(role)).findFirst();
                    if (r.isPresent()) {
                        String url = r.get().getUrl();
                        if (url != null) {
                            return "/" + url;
                        }
                    }
                }
                return "/" + user.getRoles().get(0).getUrl();
            }
        }
        return "/";
    }

    private String getUserProfileUrl(HttpServletRequest request) {
        UserAuthentication authentication = (UserAuthentication) userAuthenticationService.getAuthentication(request);
        if (authentication != null) {
            Optional<SimpleGrantedAuthority> simpleGrantedAuthority = (Optional<SimpleGrantedAuthority>) authentication.getAuthorities().stream().findFirst();
            if (simpleGrantedAuthority.isPresent()) {
                UserResource user = authentication.getDetails();

                //multiple roles
                if (user.hasMoreThanOneRoleOf(ASSESSOR, APPLICANT)) {
                    String role = cookieUtil.getCookieValue(request, "role");
                    if (!role.isEmpty()) {
                        if ("assessor".equals(role)) {
                            return ASSESSOR_PROFILE_URL;
                        }
                        if ("applicant".equals(role)) {
                            return USER_PROFILE_URL;
                        }
                    }
                }
                if (user.hasRole(ASSESSOR)) {
                    return ASSESSOR_PROFILE_URL;
                }
                if (user.hasRole(APPLICANT)) {
                    return USER_PROFILE_URL;
                }
            }
        }
        return "";
    }
}
