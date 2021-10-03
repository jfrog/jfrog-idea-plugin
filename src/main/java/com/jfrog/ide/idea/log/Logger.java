package com.jfrog.ide.idea.log;

import com.intellij.notification.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jfrog.build.api.util.Log;

/**
 * @author yahavi
 */
public class Logger implements Log {
    private static final long serialVersionUID = 1L;

    private static final NotificationGroup EVENT_LOG_NOTIFIER = NotificationGroupManager.getInstance().getNotificationGroup("JFrogLog");
    private static final NotificationGroup BALLOON_NOTIFIER = NotificationGroupManager.getInstance().getNotificationGroup("JFrogBalloon");
    private static final com.intellij.openapi.diagnostic.Logger ideaLogger = com.intellij.openapi.diagnostic.Logger.getInstance(Logger.class);
    private static Notification lastNotification;

    private static final String INFORMATION_TITLE = "JFrog";
    private static final String ERROR_TITLE = "JFrog Xray scan failed";

    public static Logger getInstance() {
        return ServiceManager.getService(Logger.class);
    }

    private Logger() {
    }

    @Override
    public void debug(String message) {
        ideaLogger.debug(message);
    }

    @Override
    public void info(String message) {
        ideaLogger.info(message);
        NotificationType notificationType = NotificationType.INFORMATION;
        log(INFORMATION_TITLE, message, notificationType);
    }

    @Override
    public void warn(String message) {
        ideaLogger.warn(message);
        NotificationType notificationType = NotificationType.WARNING;
        log(INFORMATION_TITLE, message, notificationType);
    }

    /**
     * Log an error.
     * Notice - For the best user experience, make sure that only interactive actions should call this method.
     *
     * @param message - The message to log
     */
    @Override
    public void error(String message) {
        ideaLogger.error(message);
        NotificationType notificationType = NotificationType.ERROR;
        popupBalloon(message, notificationType);
        log(ERROR_TITLE, message, notificationType);
    }

    /**
     * Log an error.
     * Notice - For the best user experience, make sure that only interactive actions should call this method.
     *
     * @param message - The message to log
     * @param t       - The exception raised
     */
    @Override
    public void error(String message, Throwable t) {
        ideaLogger.error(message, t);
        NotificationType notificationType = NotificationType.ERROR;
        popupBalloon(message, notificationType);
        String title = StringUtils.defaultIfBlank(t.getMessage(), ERROR_TITLE);
        log(title, message + System.lineSeparator() + ExceptionUtils.getStackTrace(t), notificationType);
    }

    private static void log(String title, String details, NotificationType notificationType) {
        if (StringUtils.isBlank(details)) {
            details = title;
        }
        Notifications.Bus.notify(EVENT_LOG_NOTIFIER.createNotification(title, prependPrefix(details, notificationType), notificationType, null));
    }

    private static void popupBalloon(String content, NotificationType notificationType) {
        if (lastNotification != null) {
            lastNotification.hideBalloon();
        }
        if (StringUtils.isBlank(content)) {
            content = ERROR_TITLE;
        }
        Notification notification = BALLOON_NOTIFIER.createNotification(ERROR_TITLE, content, notificationType, null);
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

    /**
     * Add a log message with an open settings link.
     * Usage example:
     * Logger.openSettings("It looks like Gradle home was not properly set in your project.
     * Click <a href=\"#settings\">here</a> to set Gradle home.", project, GradleConfigurable.class);
     *
     * @param details      - The log message
     * @param project      - IDEA project
     * @param configurable - IDEA settings to open
     */
    public static void addOpenSettingsLink(String details, Project project, Class<? extends Configurable> configurable) {
        EVENT_LOG_NOTIFIER.createNotification(INFORMATION_TITLE, prependPrefix(details, NotificationType.INFORMATION), NotificationType.INFORMATION,
                ((notification, event) -> ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable))).notify(project);
    }
}
