package com.jfrog.xray.client.impl.services.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jfrog.xray.client.services.system.Version;
import org.apache.commons.lang.StringUtils;

/**
 * Created by romang on 2/2/17.
 */
public class VersionImpl implements Version {
    @JsonProperty("xray_version")
    private final String version;
    @JsonProperty("xray_revision")
    private final String revision;

    public VersionImpl() {
        this("", "");
    }

    public VersionImpl(String version, String revision) {
        this.version = version;
        this.revision = revision;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public boolean isAtLeast(String atLeast) {
        if (StringUtils.isBlank(atLeast)) {
            return true;
        }

        String[] versionTokens = StringUtils.split(version, ".");
        String[] otherVersionTokens = StringUtils.split(atLeast, ".");
        for (int tokenIndex = 0; tokenIndex < otherVersionTokens.length; tokenIndex++) {
            String atLeastToken = otherVersionTokens[tokenIndex].trim();
            String versionToken = versionTokens.length < (tokenIndex + 1) ? "0" : versionTokens[tokenIndex].trim();
            int comparison = compareTokens(versionToken, atLeastToken);
            if (comparison != 0) {
                return comparison > 0;
            }
        }
        return true;
    }

    /**
     * @return less than 0 if toCheck is less than atLeast, 0 if they are equal or greater than 0 if toCheck is greater
     * than atLeast
     */
    private static int compareTokens(String toCheckToken, String atLeastToken) {
        int toCheckFirstNumerals = Integer.parseInt(getTokenFirstNumerals(toCheckToken));
        int atLeastFirstNumerals = Integer.parseInt(getTokenFirstNumerals(atLeastToken));

        int compareNumerals = Integer.compare(toCheckFirstNumerals, atLeastFirstNumerals);
        return compareNumerals != 0 ? compareNumerals : toCheckToken.compareTo(atLeastToken);
    }

    private static String getTokenFirstNumerals(String token) {
        StringBuilder numerals = new StringBuilder();
        for (char c : token.toCharArray()) {
            if (!Character.isDigit(c)) {
                break;
            }
            numerals.append(c);
        }
        return numerals.length() > 0 ? numerals.toString() : "0";
    }

}
