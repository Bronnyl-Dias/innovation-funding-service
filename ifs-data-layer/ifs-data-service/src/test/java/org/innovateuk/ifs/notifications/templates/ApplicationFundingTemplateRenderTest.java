package org.innovateuk.ifs.notifications.templates;

import org.innovateuk.ifs.commons.BaseIntegrationTest;
import org.innovateuk.ifs.notifications.resource.UserNotificationSource;
import org.innovateuk.ifs.notifications.resource.UserNotificationTarget;
import org.innovateuk.ifs.notifications.service.FreemarkerNotificationTemplateRenderer;
import org.innovateuk.ifs.notifications.service.senders.email.EmailNotificationSender;
import org.innovateuk.ifs.user.domain.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.innovateuk.ifs.util.MapFunctions.asMap;

public class ApplicationFundingTemplateRenderTest extends BaseIntegrationTest {

    @Autowired
    private FreemarkerNotificationTemplateRenderer renderer;

    @Test
    public void rendersUnescapedMessageBody() {
        String htmlMessage =
                "<p><strong>I am strong</strong></p>" +
                "<p><em>I am italic.</em></p>";

        String renderedTemplate = renderer.renderTemplate(
                new UserNotificationSource(new User()),
                new UserNotificationTarget(new User()),
                getTemplatePath("application_funding_text_html.html"),
                asMap("message", htmlMessage)
        )
                .getSuccessObjectOrThrowException();

        assertThat(renderedTemplate).contains(htmlMessage);
    }

    private String getTemplatePath(String templateFile) {
        return EmailNotificationSender.EMAIL_NOTIFICATION_TEMPLATES_PATH + templateFile;
    }
}
