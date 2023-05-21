package com.jfrog.ide.idea.log;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.jfrog.ide.idea.ui.utils.IconUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.build.api.util.Log;

import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;

/**
 * @author yahavi
 */
public class Logger implements Log {
    private static final long serialVersionUID = 1L;

    private static final NotificationGroup EVENT_LOG_NOTIFIER = NotificationGroupManager.getInstance().getNotificationGroup("JFrog Log");
    private static final NotificationGroup BALLOON_NOTIFIER = NotificationGroupManager.getInstance().getNotificationGroup("JFrog Errors");
    private static final com.intellij.openapi.diagnostic.Logger ideaLogger = com.intellij.openapi.diagnostic.Logger.getInstance(Logger.class);
    private static Notification lastNotification;

    private static final String INFORMATION_TITLE = "JFrog";
    private static final String ERROR_TITLE = "JFrog Xray scan failed";

    public static Logger getInstance() {
        return ApplicationManager.getApplication().getService(Logger.class);
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
        // We log to IntelliJ log in "warn" log level to avoid popup annoying fatal errors
        ideaLogger.warn(message);
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
        // We log to IntelliJ log in "warn" log level to avoid popup annoying fatal errors
        ideaLogger.warn(message, t);
        NotificationType notificationType = NotificationType.ERROR;
        popupBalloon(message, notificationType);
        String title = StringUtils.defaultIfBlank(t.getMessage(), ERROR_TITLE);
        log(title, message + System.lineSeparator() + ExceptionUtils.getStackTrace(t), notificationType);
    }

    private static void log(String title, String details, NotificationType notificationType) {
        if (StringUtils.isBlank(details)) {
            details = title;
        }
        Notifications.Bus.notify(EVENT_LOG_NOTIFIER.createNotification(title, prependPrefix(details, notificationType), notificationType));
    }

    private static void popupBalloon(String content, NotificationType notificationType) {
        if (lastNotification != null) {
            lastNotification.hideBalloon();
        }
        if (StringUtils.isBlank(content)) {
            content = ERROR_TITLE;
        }
        Notification notification = BALLOON_NOTIFIER.createNotification(ERROR_TITLE, content, notificationType);
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
        EVENT_LOG_NOTIFIER.createNotification(INFORMATION_TITLE, prependPrefix(details, NotificationType.INFORMATION), NotificationType.INFORMATION)
                .addAction(new AnAction() {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurable);
                    }
                })
                .notify(project);
    }

    /**
     * Popup a balloon with an actionable link.
     * Usage example:
     * Logger.showActionableBalloon(project,
     * "The scan results have expired. Click <a href=\"here\">here</a> to trigger a scan.",
     * () -> ScanManager.getInstance(project).startScan());
     *
     * @param project     - IDEA project
     * @param htmlContent - The log message
     * @param action      - The action to perform
     */
    public static void showActionableBalloon(Project project, String htmlContent, Runnable action) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(htmlContent,
                        IconUtils.load("jfrog_icon"),
                        JBColor.foreground(),
                        JBColor.background(),
                        event -> {
                            if (event.getEventType() != ACTIVATED) {
                                return;
                            }
                            action.run();
                        })
                .setCloseButtonEnabled(true)
                .setHideOnAction(true)
                .setHideOnClickOutside(true)
                .setHideOnLinkClick(true)
                .setHideOnKeyOutside(true)
                .setDialogMode(true)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(statusBar.getComponent()), Balloon.Position.atRight);
    }
}
