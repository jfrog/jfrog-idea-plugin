package com.jfrog.xray.client.impl;

import com.jfrog.xray.client.Xray;
import com.jfrog.xray.client.impl.services.binarymanagers.BinaryManagersImpl;
import com.jfrog.xray.client.impl.services.summary.SummaryImpl;
import com.jfrog.xray.client.impl.services.system.SystemImpl;
import com.jfrog.xray.client.impl.util.URIUtil;
import com.jfrog.xray.client.services.binarymanagers.BinaryManagers;
import com.jfrog.xray.client.services.summary.Summary;
import com.jfrog.xray.client.services.system.System;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;


/**
 * @author Roman Gurevitch
 */
public class XrayImpl implements Xray {
    private static final Logger log = LoggerFactory.getLogger(XrayImpl.class);
    private static final String API_BASE = "/api/v1/";
    private final BasicHttpContext localContext;

    private CloseableHttpClient client;
    private ResponseHandler<HttpResponse> responseHandler = new XrayResponseHandler();
    private String baseApiUrl;

    public XrayImpl(CloseableHttpClient client, String url) {
        this.client = client;
        this.baseApiUrl = URIUtil.concatUrl(url, API_BASE);
        localContext = new BasicHttpContext();
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
    public BinaryManagers binaryManagers() {
        return new BinaryManagersImpl(this);
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
        return execute(request);
    }

    private HttpResponse execute(HttpUriRequest request) throws IOException {
        log.debug("Executing {} request to path '{}', with headers: {}", request.getMethod(), request.getURI(),
                Arrays.toString(request.getAllHeaders()));
        if (localContext != null) {
            return client.execute(request, responseHandler, localContext);
        } else {
            return client.execute(request, responseHandler);
        }
    }

    /**
     * Gets responses from the underlying HttpClient and closes them (so you don't have to) the response body is
     * buffered in an intermediary byte array.
     * Will throw a {@link IOException} if the request failed.
     */
    private class XrayResponseHandler implements ResponseHandler<HttpResponse> {

        @Override
        public HttpResponse handleResponse(HttpResponse response) throws IOException {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusNotOk(statusCode)) {
                // We're using CloseableHttpClient so it's ok
                HttpClientUtils.closeQuietly((CloseableHttpResponse) response);
                throw new IOException(response.getStatusLine().toString());
            }

            // Response entity might be null, 500 and 405 also give the html itself so skip it
            String entity = "";
            if (response.getEntity() != null && statusCode != 500 && statusCode != 405) {
                try {
                    entity = IOUtils.toString(response.getEntity().getContent());
                } catch (IOException | NullPointerException e) {
                    // Null entity - Ignore
                } finally {
                    HttpClientUtils.closeQuietly((CloseableHttpResponse) response);
                }
            }

            HttpResponse newResponse = DefaultHttpResponseFactory.INSTANCE.newHttpResponse(response.getStatusLine(),
                    new HttpClientContext());
            newResponse.setEntity(new StringEntity(entity, Charset.forName("UTF-8")));
            newResponse.setHeaders(response.getAllHeaders());
            return newResponse;
        }
    }
}
