package com.jfrog.xray.client.services.summary;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by romang on 2/27/17.
 */
public interface Summary extends Serializable {

    SummaryResponse artifactSummary(List<String> checksums, List<String> paths) throws IOException;

    SummaryResponse buildSummary(String buildName, String buildNumber);

    SummaryResponse componentSummary(Components components) throws IOException;
}
