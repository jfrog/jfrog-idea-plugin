package org.jfrog.idea.xray.utils;

import com.intellij.notification.*;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.idea.ui.xray.listeners.IssuesTreeExpansionListener;

import javax.swing.event.TreeExpansionListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public final static String MINIMAL_XRAY_VERSION_SUPPORTED = "1.7.2.3";
    private static final NotificationGroup EVENT_LOG_NOTIFIER = new NotificationGroup("JFROG_LOG", NotificationDisplayType.NONE, true);
    private static final NotificationGroup BALLOON_NOTIFIER = new NotificationGroup("JFROG_BALLOON", NotificationDisplayType.BALLOON, false);
    private static Notification lastNotification;

    public static boolean isXrayVersionSupported(Version version) {
        return version.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED);
    }

    public static void notify(String title, String details, NotificationType level) {
        popupBalloon(title, details, level);
        log(title, details, level);
    }

    public static void notify(String title, Exception exception, NotificationType level) {
        popupBalloon(title, exception.getMessage(), level);
        if (StringUtils.isNotBlank(exception.getMessage())) {
            title = exception.getMessage();
        }
        log(title, Arrays.toString(exception.getStackTrace()), level);
    }

    public static void log(String title, String details, NotificationType level) {
        if (StringUtils.isBlank(details)) {
            details = title;
        }
        Notifications.Bus.notify(EVENT_LOG_NOTIFIER.createNotification(title, details, level, null));
    }

    private static void popupBalloon(String title, String details, NotificationType level) {
        if (lastNotification != null) {
            lastNotification.hideBalloon();
        }
        if (StringUtils.isBlank(details)) {
            details = title;
        }
        Notification notification = BALLOON_NOTIFIER.createNotification(title, details, level, null);
        lastNotification = notification;
        Notifications.Bus.notify(notification);
    }

    /**
     * Removes the componentId prefix, for example:
     * gav://org.jenkins-ci.main:maven-plugin:2.15.1 to org.jenkins-ci.main:maven-plugin:2.15.1
     */
    public static String removeComponentIdPrefix(String componentId) {
        try {
            URI uri = new URI(componentId);
            return uri.getAuthority();
        } catch (URISyntaxException e) {
            return componentId;
        }
    }

    public static String calculateSha256(File file) throws NoSuchAlgorithmException, IOException {
        return calculateChecksum(file, "SHA-256");
    }

    public static String calculateSha1(File file) throws NoSuchAlgorithmException, IOException {
        return calculateChecksum(file, "SHA-1");
    }

    @NotNull
    private static String calculateChecksum(File file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] dataBytes = new byte[1024];
            int nread;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            byte[] mdBytes = md.digest();

            // convert the byte to hex format method 1
            StringBuilder sb = new StringBuilder();
            for (byte mdByte : mdBytes) {
                sb.append(Integer.toString((mdByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static Process exeCommand(List<String> args) throws IOException {
        String strArgs = String.join(" ", args);
        if (isWindows()) {
            return Runtime.getRuntime().exec(new String[]{"cmd", "/c", strArgs});
        }
        if (isMac()) {
            return Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", strArgs}, new String[]{"PATH=$PATH:/usr/local/bin"});
        }
        // Linux
        return Runtime.getRuntime().exec(args.toArray(new String[0]));
    }

    public static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (StringWriter writer = new StringWriter()) {
            IOUtils.copy(stream, writer, "UTF-8");
            return writer.toString();
        }
    }

    public static TreeExpansionListener getIssuesTreeExpansionListener(TreeExpansionListener[] treeExpansionListeners) {
        if (treeExpansionListeners == null) {
            return null;
        }
        for (TreeExpansionListener treeExpansionListener : treeExpansionListeners) {
            if (treeExpansionListener instanceof IssuesTreeExpansionListener) {
                return treeExpansionListener;
            }
        }
        return null;
    }

    /**
     * Returns Set of Paths cleaned of subdirectories.
     * For example the set ["/a", "/b/c", "/a/d"] will become ["/a", "/b/c"]
     *
     * @param projectPaths Paths to filter
     * @return Set of Paths cleaned of subdirectories.
     */
    public static Set<Path> filterProjectPaths(Set<Path> projectPaths) {
        Set<Path> finalPaths = new HashSet<>();
        // Create a sorted by length list of paths
        List<Path> sortedList = projectPaths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .sorted(Comparator.comparingInt(Path::getNameCount))
                .collect(Collectors.toList());
        sortedList.forEach(currentPath -> {
                    boolean isRootPath = true;
                    // Iterate over the sorted by directories count length list.
                    for (Path shorterPath : sortedList) {
                        // CurrentPath is shorter or equals to the shortPath therefore all the next paths can't contain the currentPath
                        if (currentPath.getNameCount() <= shorterPath.getNameCount()) {
                            break;
                        }
                        // The currentPath is subPath and we should not add it to the list
                        if (currentPath.startsWith(shorterPath)) {
                            isRootPath = false;
                            break;
                        }
                    }
                    if (isRootPath) {
                        finalPaths.add(currentPath);
                    }
                }
        );
        return finalPaths;
    }
}
