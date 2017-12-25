package org.jfrog.idea.xray.utils;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.jfrog.xray.client.services.system.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public final static String MINIMAL_XRAY_VERSION_SUPPORTED = "1.7.2.3";
    public final static String MINIMAL_XRAY_VERSION_UNSUPPORTED = "2.0";

    public static boolean isXrayVersionSupported(Version version) {
        return version.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED) && !version.isAtLeast(MINIMAL_XRAY_VERSION_UNSUPPORTED);
    }

    public static void notify(Logger logger, String title, String details, NotificationType level) {
        Notifications.Bus.notify(new Notification("JFrog", title, details, level));
        switch (level) {
            case ERROR:
                logger.error(title, details);
                break;
            case WARNING:
                logger.warn(title + ": " + details);
                break;
            default:
                logger.info(title + ": " + details);
        }
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
            return Runtime.getRuntime().exec(new String[]{"cmd", "/c" ,strArgs});
        } else if (isMac()) {
            return Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c" ,strArgs}, new String[]{"PATH=$PATH:/usr/local/bin"});
        } else {
            return Runtime.getRuntime().exec(args.toArray(new String[0]));
        }
    }
}
