package org.innovateuk.ifs.publiccontent.security;

import org.innovateuk.ifs.commons.security.PermissionRule;
import org.innovateuk.ifs.commons.security.PermissionRules;
import org.innovateuk.ifs.competition.publiccontent.resource.ContentGroupCompositeId;
import org.innovateuk.ifs.publiccontent.domain.ContentGroup;
import org.innovateuk.ifs.publiccontent.repository.ContentGroupRepository;
import org.innovateuk.ifs.security.BasePermissionRules;
import org.innovateuk.ifs.user.resource.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.innovateuk.ifs.util.SecurityRuleUtil.isInternal;
import static org.innovateuk.ifs.util.SecurityRuleUtil.isSystemRegistrationUser;

/**
 * Provides the permissions around file downloads for content groups.
 */
@Component
@PermissionRules
public class ContentGroupPermissionRules extends BasePermissionRules {

    @Autowired
    private ContentGroupRepository contentGroupRepository;

    @PermissionRule(value = "DOWNLOAD_CONTENT_GROUP_FILE", description = "Internal users can see all content group files")
    public boolean internalUsersCanViewAllContentGroupFiles(ContentGroupCompositeId contentGroupCompositeId, UserResource user) {
        return isInternal(user);
    }

    @PermissionRule(value = "DOWNLOAD_CONTENT_GROUP_FILE", description = "External users can only see published content group files")
    public boolean externalUsersCanViewPublishedContentGroupFiles(ContentGroupCompositeId contentGroupCompositeId, UserResource user) {
        ContentGroup contentGroup = contentGroupRepository.findOne(contentGroupCompositeId.id());
        if (contentGroup != null) {
            return isSystemRegistrationUser(user) && isPublished(contentGroup);
        }
        return false;
    }

    private boolean isPublished(ContentGroup contentGroup) {
        return contentGroup.getContentSection().getPublicContent().getPublishDate() != null;
    }

}
