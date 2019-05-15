package com.jfrog.ide.idea.log;

import com.intellij.notification.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jfrog.build.api.util.Log;

/**
 * @author yahavi
 */
public class Logger implements Log {
    private static final long serialVersionUID = 1L;

    private static final NotificationGroup EVENT_LOG_NOTIFIER = new NotificationGroup("JFROG_LOG", NotificationDisplayType.NONE, true);
    private static final NotificationGroup BALLOON_NOTIFIER = new NotificationGroup("JFROG_BALLOON", NotificationDisplayType.BALLOON, false);
    private static Notification lastNotification;
    private static Logger instance;

    private static final String INFORMATION_TITLE = "JFrog Xray";
    private static final String ERROR_TITLE = "JFrog Xray scan failed";

    private Logger() {
    }

    public static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    @Override
    public void debug(String message) {
        throw new NotImplementedException("Debug logging is not supported");
    }

    @Override
    public void info(String message) {
        NotificationType notificationType = NotificationType.INFORMATION;
        log(INFORMATION_TITLE, message, notificationType);
    }

    @Override
    public void warn(String message) {
        NotificationType notificationType = NotificationType.WARNING;
        log(INFORMATION_TITLE, message, notificationType);
    }

    @Override
    public void error(String message) {
        NotificationType notificationType = NotificationType.ERROR;
        popupBalloon(ERROR_TITLE, message, notificationType);
        log(ERROR_TITLE, message, notificationType);
    }

    @Override
    public void error(String message, Throwable t) {
        NotificationType notificationType = NotificationType.ERROR;
        popupBalloon(ERROR_TITLE, message, notificationType);
        String title = StringUtils.defaultIfBlank(t.getMessage(), ERROR_TITLE);
        log(title, message + System.lineSeparator() + ExceptionUtils.getStackTrace(t), notificationType);
    }

    private static void log(String title, String details, NotificationType notificationType) {
        if (StringUtils.isBlank(details)) {
            details = title;
        }
        Notifications.Bus.notify(EVENT_LOG_NOTIFIER.createNotification(title, prependPrefix(details, notificationType), notificationType, null));
    }

    public static void popupBalloon(String title, String content, NotificationType notificationType) {
        if (lastNotification != null) {
            lastNotification.hideBalloon();
        }
        if (StringUtils.isBlank(content)) {
            content = title;
        }
        Notification notification = BALLOON_NOTIFIER.createNotification(title, content, notificationType, null);
        lastNotification = notification;
        Notifications.Bus.notify(notification);
    }

    private static String prependPrefix(String message, NotificationType notificationType) {
        switch (notificationType) {
            case WARNING:
                return "[WARN] " + message;
            case ERROR:
                return "[ERROR] " + message;
        }
        return "[INFO] " + message;
    }
}
