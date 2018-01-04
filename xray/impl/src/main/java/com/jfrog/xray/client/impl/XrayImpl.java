package com.jfrog.xray.client.impl;

import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.services.summary.SummaryImpl;
import com.jfrog.xray.client.impl.services.system.SystemImpl;
import com.jfrog.xray.client.impl.util.URIUtil;
import com.jfrog.xray.client.services.summary.Summary;
import com.jfrog.xray.client.services.system.System;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;


/**
 * @author Roman Gurevitch
 */
public class XrayImpl implements Xray {
    private static final Logger log = LoggerFactory.getLogger(XrayImpl.class);
    private static final String API_BASE = "/api/v1/";

    private CloseableHttpClient client;
    private String baseApiUrl;

    public XrayImpl(CloseableHttpClient client, String url) {
        this.client = client;
        this.baseApiUrl = URIUtil.concatUrl(url, API_BASE);
    }

    static public void addContentTypeJsonHeader(Map<String, String> headers) {
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    }

    static public void addContentTypeBinaryHeader(Map<String, String> headers) {
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.DEFAULT_BINARY.getMimeType());
    }

    private static boolean statusNotOk(int statusCode) {
        return statusCode != HttpStatus.SC_OK
                && statusCode != HttpStatus.SC_CREATED
                && statusCode != HttpStatus.SC_ACCEPTED;
    }

    @Override
    public System system() {
        return new SystemImpl(this);
    }

    @Override
    public Summary summary() {
        return new SummaryImpl(this);
    }

    @Override
    public void close() {
        HttpClientUtils.closeQuietly(client);
    }

    private void setHeaders(HttpUriRequest request, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
    }

    public HttpResponse get(String uri, Map<String, String> headers) throws IOException {
        HttpGet getRequest = new HttpGet(createUrl(uri));
        return setHeadersAndExecute(getRequest, headers);
    }

    public HttpResponse head(String uri, Map<String, String> headers) throws IOException {
        HttpHead headRequest = new HttpHead(createUrl(uri));
        return setHeadersAndExecute(headRequest, headers);
    }

    public HttpResponse post(String uri, Map<String, String> headers, InputStream elementInputStream) throws IOException {
        HttpPost postRequest = new HttpPost(createUrl(uri));
        HttpEntity requestEntity = new InputStreamEntity(elementInputStream);
        postRequest.setEntity(requestEntity);
        return setHeadersAndExecute(postRequest, headers);
    }

    private String createUrl(String queryPath) {
        log.debug("Trying to encode uri: '{}' with base url: {}", queryPath, API_BASE);
        return URIUtil.concatUrl(baseApiUrl, queryPath);
    }

    private HttpResponse setHeadersAndExecute(HttpUriRequest request, Map<String, String> headers) throws IOException {
        setHeaders(request, headers);
        HttpResponse response = client.execute(request);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusNotOk(statusCode)) {
            String body = null;
            if (response.getEntity() != null) {
                try {
                    body = readStream(response.getEntity().getContent());
                    EntityUtils.consume(response.getEntity());
                } catch (IOException e) {
                    // Ignore
                }
            }
            String message = String.format("Received %d %s response from Xray", statusCode, statusLine);
            if (StringUtils.isNotBlank(body)) {
                message += ". " + body;
            }
            throw new HttpResponseException(statusCode, message);
        }
        return response;
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (StringWriter writer = new StringWriter()){
            IOUtils.copy(stream, writer, "UTF-8");
            return writer.toString();
        }
    }
}
