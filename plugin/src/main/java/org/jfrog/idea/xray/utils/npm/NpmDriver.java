package org.jfrog.idea.xray.utils.npm;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.idea.xray.utils.Utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Created by Yahav Itzhak on 17 Dec 2017.
 */
public class NpmDriver {

    private static String exeNpmCommand(List<String> args) throws InterruptedException, IOException {
        args.add(0, "npm");
        Process process = null;
        try {
            process = Utils.exeCommand(args);
            try (StringWriter writer = new StringWriter()){
                IOUtils.copy(process.getInputStream(), writer, "UTF-8");
                int errCode = process.waitFor();
                if (errCode != 0) {
                    String errMsg;
                    try (StringWriter errWriter = new StringWriter()) {
                        IOUtils.copy(process.getErrorStream(), errWriter, "UTF-8");
                        errMsg = errWriter.toString();
                    }
                    if (StringUtils.isBlank(errMsg)) {
                        errMsg = writer.toString();
                    }
                    throw new IOException("'" + String.join(" ", args) + "' command failed with error code " + errCode + ": " + errMsg);
                }
                return writer.toString();
            }
        } finally {
            if (process != null) {
                process.getInputStream().close();
                process.getErrorStream().close();
                process.getOutputStream().close();
            }
        }
    }

    public static boolean isInstalled() {
        List<String> args = Lists.newArrayList("version");
        try {
            exeNpmCommand(args);
        } catch (IOException|InterruptedException e) {
            return false;
        }
        return true;
    }

    public void install(String appDir) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList("install", "--only=production", "--prefix", appDir);
        exeNpmCommand(args);
    }

    public JsonNode list(String appDir) throws IOException, InterruptedException {
        List<String> args = Lists.newArrayList("ls", "--prefix", appDir, "--json");
        String npmLsJson = exeNpmCommand(args);
        JsonFactory factory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(factory);
        return mapper.readTree(npmLsJson);
    }
}