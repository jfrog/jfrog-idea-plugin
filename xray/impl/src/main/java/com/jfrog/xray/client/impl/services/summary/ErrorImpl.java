package com.jfrog.xray.client.impl.services.summary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.summary.Error;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorImpl implements Error {

    private String error;
    private String identifier;

    @JsonProperty("error")
    public String getError() {
        return error;
    }

    @JsonProperty("identifier")
    public String getIdentifier() {
        return identifier;
    }
}
