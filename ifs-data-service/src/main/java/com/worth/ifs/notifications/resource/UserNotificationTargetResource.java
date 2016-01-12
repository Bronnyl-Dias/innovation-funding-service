package com.worth.ifs.notifications.resource;

import com.worth.ifs.user.domain.User;

/**
 * Represents a User as the target of a given Notification
 */
public class UserNotificationTargetResource implements NotificationTarget {

    private User user;

    public UserNotificationTargetResource(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getName() {
        return user.getName();
    }
}
