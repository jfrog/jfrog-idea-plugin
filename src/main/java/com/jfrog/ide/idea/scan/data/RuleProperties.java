package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class RuleProperties {

    @JsonProperty("conclusion")
    private String conclusion;

    @JsonProperty("applicability")
    private String applicability;

}
