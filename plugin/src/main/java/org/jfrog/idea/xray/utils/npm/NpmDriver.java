package org.jfrog.idea.xray.utils.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.lang.StringUtils;
import org.jfrog.idea.xray.utils.Utils;

import java.io.IOException;
import java.util.List;

import static org.jfrog.idea.xray.utils.Utils.readStream;

/**
 * Created by Yahav Itzhak on 17 Dec 2017.
 */
public class NpmDriver {

    private static final Logger logger = Logger.getInstance(NpmDriver.class);
    private static ObjectReader jsonReader = new ObjectMapper().reader();

    private static NpmCommandRes exeNpmCommand(List<String> args) throws InterruptedException, IOException {
        args.add(0, "npm");
        Process process = null;
        try {
            NpmCommandRes npmCommandRes = new NpmCommandRes();
            process = Utils.exeCommand(args);
            npmCommandRes.res = readStream(process.getInputStream());
            if (process.waitFor() != 0) {
                npmCommandRes.err = readStream(process.getErrorStream());
            }
            return npmCommandRes;
        } finally {
            if (process != null) {
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            }
        }
    }

    public static boolean isNpmInstalled() {
        List<String> args = Lists.newArrayList("version");
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(args);
            return npmCommandRes.isOk();
        } catch (IOException|InterruptedException e) {
            return false;
        }
    }

    public void install(String appDir) throws IOException {
        List<String> args = Lists.newArrayList("install", "--only=production", appDir, "--prefix", appDir);
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(args);
            if (!npmCommandRes.isOk()) {
                throw new IOException(npmCommandRes.err);
            }
        } catch (IOException|InterruptedException e) {
            throw new IOException("'npm install' failed", e);
        }
    }

    public JsonNode list(String appDir) throws IOException {
        List<String> args = Lists.newArrayList("ls", "--prefix", appDir, "--json");
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(args);
            JsonNode jsonNode = jsonReader.readTree(npmCommandRes.res);
            if (!npmCommandRes.isOk()) {
                Utils.notify(logger, "JFrog Xray", "JFrog Xray scan encountered errors. See logs for further information.", NotificationType.WARNING);
                Utils.log(logger, "JFrog Xray", npmCommandRes.err, NotificationType.WARNING);
            }
            return jsonNode;
        } catch (IOException|InterruptedException e) {
            throw new IOException("'npm ls' failed", e);
        }
    }

    private static class NpmCommandRes {
        String res;
        String err;

        private boolean isOk() {
            return StringUtils.isBlank(err);
        }
    }
}