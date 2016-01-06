package com.worth.ifs.notifications.service;

import com.worth.ifs.notifications.resource.NotificationMedium;
import com.worth.ifs.notifications.resource.NotificationResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import static com.worth.ifs.notifications.resource.NotificationMedium.LOGGING;

/**
 * A Service that logs outgoing Notifications
 */
@Service
public class LoggingNotificationSendingService implements NotificationSendingService {

    private static final Log LOG = LogFactory.getLog(LoggingNotificationSendingService.class);

    @Override
    public NotificationMedium getNotificationMedium() {
        return LOGGING;
    }

    @Override
    public void sendNotification(NotificationResource notification) {
        LOG.debug("Sending Notification " + notification);
    }
}
