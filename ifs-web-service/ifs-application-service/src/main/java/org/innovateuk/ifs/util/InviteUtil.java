package org.innovateuk.ifs.util;

import org.innovateuk.ifs.filter.CookieFlashMessageFilter;
import org.innovateuk.ifs.commons.error.exception.InvalidURLException;

import javax.servlet.http.HttpServletResponse;

/**
 * A utility class for common invite methods.
 */
public class InviteUtil {
    public static final String INVITE_ALREADY_ACCEPTED = "inviteAlreadyAccepted";
    public static final String INVITE_HASH = "invite_hash";
    public static final String ORGANISATION_TYPE = "organisationType";

    public static String handleAcceptedInvite(CookieFlashMessageFilter cookieFlashMessageFilter, HttpServletResponse response, CookieUtil cookieUtil) {
        cookieUtil.removeCookie(response, INVITE_HASH);
        cookieFlashMessageFilter.setFlashMessage(response, INVITE_ALREADY_ACCEPTED);
        return "redirect:/login";
    }

    public static void handleInvalidInvite(HttpServletResponse response, CookieUtil cookieUtil) throws InvalidURLException {
        cookieUtil.removeCookie(response, INVITE_HASH);
        throw new InvalidURLException("Invite url is not valid", null);
    }
}
