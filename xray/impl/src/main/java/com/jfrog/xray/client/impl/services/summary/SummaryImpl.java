package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jfrog.xray.client.impl.XrayImpl;
import com.jfrog.xray.client.impl.util.ObjectMapperHelper;
import com.jfrog.xray.client.services.summary.Components;
import com.jfrog.xray.client.services.summary.Summary;
import com.jfrog.xray.client.services.summary.SummaryResponse;
import org.apache.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by romang on 2/27/17.
 */
public class SummaryImpl implements Summary {

    private final XrayImpl xray;

    public SummaryImpl(XrayImpl xray) {
        this.xray = xray;
    }

    @Override
    public SummaryResponse artifact(List<String> checksums, List<String> paths) throws IOException {
        if (checksums == null && paths == null) {
            return new SummaryResponseImpl();
        }

        ObjectMapper mapper = ObjectMapperHelper.get();
        ArtifactSummaryBody summaryBody = new ArtifactSummaryBody(checksums, paths);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, summaryBody);
        ByteArrayInputStream content = new ByteArrayInputStream(out.toByteArray());

        Map<String, String> headers = new HashMap<>();
        XrayImpl.addContentTypeJsonHeader(headers);

        HttpResponse response = xray.post("summary/artifact", headers, content);
        return mapper.readValue(response.getEntity().getContent(), SummaryResponseImpl.class);
    }

    @Override
    public SummaryResponse component(Components components) throws IOException {
        if (components == null) {
            return new SummaryResponseImpl();
        }

        ObjectMapper mapper = ObjectMapperHelper.get();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, components);
        ByteArrayInputStream content = new ByteArrayInputStream(out.toByteArray());

        Map<String, String> headers = new HashMap<>();
        XrayImpl.addContentTypeJsonHeader(headers);

        HttpResponse response = xray.post("summary/component", headers, content);
        return mapper.readValue(response.getEntity().getContent(), SummaryResponseImpl.class);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private class ArtifactSummaryBody {

        private List<String> checksums;
        private List<String> paths;

        public ArtifactSummaryBody(List<String> checksums, List<String> paths) {
            this.checksums = checksums;
            this.paths = paths;
        }

        @JsonProperty("checksums")
        public List<String> getChecksums() {
            return checksums;
        }

        @JsonProperty("paths")
        public List<String> getPaths() {
            return paths;
        }
    }

}
