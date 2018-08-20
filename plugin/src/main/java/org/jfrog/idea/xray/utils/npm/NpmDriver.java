package org.jfrog.idea.xray.utils.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.StreamUtil;
import org.jfrog.idea.xray.utils.StreamReader;
import org.jfrog.idea.xray.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yahav Itzhak on 17 Dec 2017.
 */
public class NpmDriver {

    private static ObjectReader jsonReader = new ObjectMapper().reader();

    /**
     * Execute a npm command in the current directory.
     *
     * @param args - Command arguments
     * @return NpmCommandRes
     */
    private static NpmCommandRes exeNpmCommand(List<String> args) throws InterruptedException, IOException {
        File execDir = new File(".");
        return exeNpmCommand(execDir, args);
    }

    /**
     * Execute a npm command.
     *
     * @param execDir - The execution dir (Usually path to project)
     * @param args    - Command arguments
     * @return NpmCommandRes
     */
    private static NpmCommandRes exeNpmCommand(File execDir, List<String> args) throws InterruptedException, IOException {
        args.add(0, "npm");
        Process process = null;
        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            NpmCommandRes npmCommandRes = new NpmCommandRes();
            process = Utils.exeCommand(execDir, args);
            StreamReader inputStreamReader = new StreamReader(process.getInputStream());
            StreamReader errorStreamReader = new StreamReader(process.getErrorStream());
            service.submit(inputStreamReader);
            service.submit(errorStreamReader);
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                service.shutdownNow();
                npmCommandRes.res = inputStreamReader.getOutput();
                npmCommandRes.err = errorStreamReader.getOutput();
            } else {
                npmCommandRes.err = String.format("Process execution %s timed out.", String.join(" ", args));
            }
            npmCommandRes.exitValue = process.exitValue();

            return npmCommandRes;
        } finally {
            closeStreams(process);
        }
    }

    private static void closeStreams(Process process) {
        if (process != null) {
            StreamUtil.closeStream(process.getInputStream());
            StreamUtil.closeStream(process.getOutputStream());
            StreamUtil.closeStream(process.getErrorStream());
        }
    }

    public static boolean isNpmInstalled() {
        List<String> args = Lists.newArrayList("version");
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(args);
            return npmCommandRes.isOk();
        } catch (IOException | InterruptedException e) {
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
        } catch (IOException | InterruptedException e) {
            throw new IOException("'npm install' failed: " + e.getMessage(), e);
        }
    }

    public JsonNode list(String appDir) throws IOException {
        File execDir = new File(appDir);
        List<String> args = Lists.newArrayList("ls", "--json");
        try {
            NpmCommandRes npmCommandRes = exeNpmCommand(execDir, args);
            return jsonReader.readTree(npmCommandRes.res);
        } catch (IOException | InterruptedException e) {
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