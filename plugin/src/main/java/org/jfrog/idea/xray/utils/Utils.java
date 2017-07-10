package org.jfrog.idea.xray.utils;

import com.jfrog.xray.client.services.system.Version;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by romang on 5/8/17.
 */
public class Utils {

    public final static String MINIMAL_XRAY_VERSION_SUPPORTED = "1.7.2.3";

    public static boolean isXrayVersionSupported(Version version) {
        return version.isAtLeast(MINIMAL_XRAY_VERSION_SUPPORTED);
    }

    /**
     * Removes the componentId prefix, for example:
     * gav://org.jenkins-ci.main:maven-plugin:2.15.1 to org.jenkins-ci.main:maven-plugin:2.15.1
     *
     * @param componentId
     * @return
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
        FileInputStream fis = new FileInputStream(file);

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        }
        byte[] mdbytes = md.digest();

        // convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
