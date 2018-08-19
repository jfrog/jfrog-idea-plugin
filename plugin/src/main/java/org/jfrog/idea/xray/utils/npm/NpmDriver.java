package org.jfrog.idea.xray.utils.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jfrog.idea.xray.utils.Utils;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jfrog.idea.xray.utils.Utils.readStream;

/**
 * Created by Yahav Itzhak on 17 Dec 2017.
 */
public class NpmDriver {

    private static ObjectReader jsonReader = new ObjectMapper().reader();

    /**
     * Execute a npm command in the current directory.
     * @param args - Command arguments
     * @return NpmCommandRes
     */
    private static NpmCommandRes exeNpmCommand(List<String> args) throws InterruptedException, IOException {
        File execDir = new File(".");
        return exeNpmCommand(execDir, args);
    }

    /**
     * Execute a npm command.
     * @param execDir - The execution dir (Usually path to project)
     * @param args - Command arguments
     * @return NpmCommandRes
     */
    private static NpmCommandRes exeNpmCommand(File execDir, List<String> args) throws InterruptedException, IOException {
        args.add(0, "npm");
        Process process = null;
        try {
            NpmCommandRes npmCommandRes = new NpmCommandRes();
            process = Utils.exeCommand(execDir, args);
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                npmCommandRes.res = readStream(process.getInputStream());
                npmCommandRes.err = readStream(process.getErrorStream());
            } else {
                npmCommandRes.err = String.format("Process execution %s timed out.", String.join(" ", args));
            }
            npmCommandRes.exitValue = process.exitValue();

            return npmCommandRes;
        } finally {
            closeStreams(process);
        }
    }

    private static void closeStreams(Process process) throws IOException {
        if (process != null) {
            if (process.getInputStream() != null) {
                process.getInputStream().close();
            }
            if (process.getOutputStream() != null) {
                process.getOutputStream().close();
            }
            if (process.getErrorStream() != null) {
                process.getErrorStream().close();
            }
        }
    }

    private static String readStream(InputStream in) throws IOException {
        BufferedReader input = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = input.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
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
        try {
            File execDir = new File(appDir);
            List<String> args = Lists.newArrayList("install", "--only=production");
            NpmCommandRes npmCommandRes = exeNpmCommand(execDir, args);
            if (!npmCommandRes.isOk()) {
                throw new IOException(npmCommandRes.err);
            }
        } catch (IOException|InterruptedException e) {
            throw new IOException("'npm install' failed: " + e.getMessage(), e);
        }
    }

    public JsonNode list(String appDir) throws IOException {
        File execDir = new File(appDir);
        List<String> args = Lists.newArrayList("ls", "--json");
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(execDir, args);
            return jsonReader.readTree(npmCommandRes.res);
        } catch (IOException|InterruptedException e) {
            throw new IOException("'npm ls' failed", e);
        }
    }

    private static class NpmCommandRes {
        String res = "{}";
        String err;
        int exitValue;

        private boolean isOk() {
            return exitValue == 0;
        }
    }
}