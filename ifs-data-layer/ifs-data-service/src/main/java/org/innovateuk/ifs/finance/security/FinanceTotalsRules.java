package org.innovateuk.ifs.finance.security;

import org.innovateuk.ifs.application.resource.ApplicationResource;
import org.innovateuk.ifs.commons.security.PermissionRule;
import org.innovateuk.ifs.commons.security.PermissionRules;
import org.innovateuk.ifs.security.BasePermissionRules;
import org.innovateuk.ifs.user.resource.UserResource;
import org.springframework.stereotype.Component;

import static org.innovateuk.ifs.util.SecurityRuleUtil.isInternal;
import static org.innovateuk.ifs.util.SecurityRuleUtil.isSystemRegistrationUser;


/**
 * Defines rules for the FinanceTotalsSender service.
 */
@Component
@PermissionRules
public class FinanceTotalsRules extends BasePermissionRules {
    @PermissionRule(value = "SEND_APPLICATION_TOTALS_ON_SUBMIT", description = "Internal users and lead applicants are allowed to send the application totals.")
    public boolean leadApplicantAndInternalUsersCanUpdateTotalsForAnApplication(final ApplicationResource applicationResource,
                                                                                final UserResource user) {
        return isLeadApplicant(applicationResource.getId(), user) || isInternal(user) || isSystemRegistrationUser(user);
    }
}