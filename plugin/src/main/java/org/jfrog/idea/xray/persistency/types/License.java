package org.jfrog.idea.xray.persistency.types;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by romang on 4/12/17.
 */
public class License implements Comparable<License> {
    public static String UNKNOWN_LICENCE_FULL_NAME = "Unknown license";
    public static String UNKNOWN_LICENCE_NAME = "Unknown";
    public List<String> components = new ArrayList<>();
    public String fullName;
    public String name;
    public List<String> moreInfoUrl = new ArrayList<>();

    public License() {
        this.fullName = UNKNOWN_LICENCE_FULL_NAME;
        this.name = UNKNOWN_LICENCE_NAME;
    }

    public License(com.jfrog.xray.client.services.summary.License license) {
        components = license.getComponents();
        fullName = license.getFullName();
        name = license.getName();
        moreInfoUrl = license.moreInfoUrl();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        License otherLicense = (License) other;
        return StringUtils.equals(fullName, otherLicense.fullName) && StringUtils.equals(name, otherLicense.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fullName);
        result += Objects.hashCode(name);
        return result * 31;
    }

    @Override
    public int compareTo(@NotNull License otherLicense) {
        if (this == otherLicense) {
            return 0;
        }
        if (otherLicense == null) {
            return 1;
        }
        return StringUtils.equals(name, otherLicense.name) ? StringUtils.compare(fullName, otherLicense.fullName) : StringUtils.compare(name, otherLicense.name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}