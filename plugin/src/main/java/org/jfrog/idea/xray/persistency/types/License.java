package org.jfrog.idea.xray.persistency.types;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by romang on 4/12/17.
 */
public class License implements Comparable<License> {
    public List<String> components = new ArrayList<>();
    public String fullName;
    public String name;
    public List<String> moreInfoUrl = new ArrayList<>();

    public License() {
    }

    public License(com.jfrog.xray.client.services.summary.License license) {
        components = license.getComponents();
        fullName = license.getFullName();
        name = license.getName();
        moreInfoUrl = license.moreInfoUrl();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        License license = (License) o;

        if (!fullName.equals(license.fullName)) return false;
        return name.equals(license.name);
    }

    @Override
    public int hashCode() {
        int result = fullName != null ? fullName.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(@NotNull License o) {
        if (this == o) return 0;
        if (o == null) return 1;
        return name.compareTo(o.name) != 0 ? name.compareTo(o.name) : fullName.compareTo(o.fullName);
    }
}
