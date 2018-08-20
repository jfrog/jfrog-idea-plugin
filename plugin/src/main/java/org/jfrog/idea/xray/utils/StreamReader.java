package org.jfrog.idea.xray.utils;

import java.io.*;

public class StreamReader implements Runnable {
    private String output;
    private InputStream inputStream;

    public StreamReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try {
            output = readStream(inputStream);
        } catch (IOException e) {
            output = e.toString();
        }
    }

    public String getOutput() {
        return this.output;
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (Reader reader = new InputStreamReader(in);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}