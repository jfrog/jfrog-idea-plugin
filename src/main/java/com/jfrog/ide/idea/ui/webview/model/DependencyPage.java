package com.jfrog.ide.idea.ui.webview.model;

public class DependencyPage {
    private final String id;
    private final String component;
    private final String type;
    private final String version;
    private final String severity;
    private final License[] license;
    private final String summary;
    private final String[] fixedVersion;
    private final String[] infectedVersion;
    private final Reference[] references;
    private final Cve cve;
    private final ImpactedPath impactedPath;
    private final String[] watchName;
    private final String edited;
    private final ExtendedInformation extendedInformation;

    public DependencyPage(String id, String component, String type, String version, String severity, License[] license,
                          String summary, String[] fixedVersion, String[] infectedVersion, Reference[] references,
                          Cve cve, ImpactedPath impactedPath, String[] watchName, String edited,
                          ExtendedInformation extendedInformation) {
        this.id = id;
        this.component = component;
        this.type = type;
        this.version = version;
        this.severity = severity;
        this.license = license;
        this.summary = summary;
        this.fixedVersion = fixedVersion;
        this.infectedVersion = infectedVersion;
        this.references = references;
        this.cve = cve;
        this.impactedPath = impactedPath;
        this.watchName = watchName;
        this.edited = edited;
        this.extendedInformation = extendedInformation;
    }

    @SuppressWarnings("unused")
    public License[] getLicense() {
        return license;
    }

    @SuppressWarnings("unused")
    public String getSummary() {
        return summary;
    }

    @SuppressWarnings("unused")
    public String[] getInfectedVersion() {
        return infectedVersion;
    }

    @SuppressWarnings("unused")
    public Reference[] getReferences() {
        return references;
    }

    @SuppressWarnings("unused")
    public ImpactedPath getImpactedPath() {
        return impactedPath;
    }

    @SuppressWarnings("unused")
    public ExtendedInformation getExtendedInformation() {
        return extendedInformation;
    }

    @SuppressWarnings("unused")
    public Cve getCve() {
        return cve;
    }

    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public String getComponent() {
        return component;
    }

    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return version;
    }

    @SuppressWarnings("unused")
    public String getSeverity() {
        return severity;
    }

    @SuppressWarnings("unused")
    public String[] getFixedVersion() {
        return fixedVersion;
    }

    @SuppressWarnings("unused")
    public String[] getWatchName() {
        return watchName;
    }

    @SuppressWarnings("unused")
    public String getEdited() {
        return edited;
    }
}
