package com.jfrog.ide.idea.ui.webview.model;

public class DependencyPage {
    private String id;
    private String component;
    private String type;
    private String version;
    private String severity;
    private License[] license;
    private String summary;
    private String[] fixedVersion;
    private String[] infectedVersion;
    private Reference[] references;
    private Cve cve;
    private ImpactedPath impactedPath;
    private String[] watchName;
    private String edited;
    private ExtendedInformation extendedInformation;

    public DependencyPage() {
    }

    @SuppressWarnings("unused")
    public String getId() {
        return id;
    }

    public DependencyPage id(String id) {
        this.id = id;
        return this;
    }

    @SuppressWarnings("unused")
    public String getComponent() {
        return component;
    }

    public DependencyPage component(String component) {
        this.component = component;
        return this;
    }

    @SuppressWarnings("unused")
    public String getType() {
        return type;
    }

    public DependencyPage type(String type) {
        this.type = type;
        return this;
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return version;
    }

    public DependencyPage version(String version) {
        this.version = version;
        return this;
    }

    @SuppressWarnings("unused")
    public String getSeverity() {
        return severity;
    }

    public DependencyPage severity(String severity) {
        this.severity = severity;
        return this;
    }

    @SuppressWarnings("unused")
    public License[] getLicense() {
        return license;
    }

    public DependencyPage license(License[] license) {
        this.license = license;
        return this;
    }

    @SuppressWarnings("unused")
    public String getSummary() {
        return summary;
    }

    public DependencyPage summary(String summary) {
        this.summary = summary;
        return this;
    }

    @SuppressWarnings("unused")
    public String[] getFixedVersion() {
        return fixedVersion;
    }

    public DependencyPage fixedVersion(String[] fixedVersion) {
        this.fixedVersion = fixedVersion;
        return this;
    }

    @SuppressWarnings("unused")
    public String[] getInfectedVersion() {
        return infectedVersion;
    }

    public DependencyPage infectedVersion(String[] infectedVersion) {
        this.infectedVersion = infectedVersion;
        return this;
    }

    @SuppressWarnings("unused")
    public Reference[] getReferences() {
        return references;
    }

    public DependencyPage references(Reference[] references) {
        this.references = references;
        return this;
    }

    @SuppressWarnings("unused")
    public Cve getCve() {
        return cve;
    }

    public DependencyPage cve(Cve cve) {
        this.cve = cve;
        return this;
    }

    @SuppressWarnings("unused")
    public ImpactedPath getImpactedPath() {
        return impactedPath;
    }

    public DependencyPage impactedPath(ImpactedPath impactedPath) {
        this.impactedPath = impactedPath;
        return this;
    }

    @SuppressWarnings("unused")
    public String[] getWatchName() {
        return watchName;
    }

    public DependencyPage watchName(String[] watchName) {
        this.watchName = watchName;
        return this;
    }

    @SuppressWarnings("unused")
    public String getEdited() {
        return edited;
    }

    public DependencyPage edited(String edited) {
        this.edited = edited;
        return this;
    }

    @SuppressWarnings("unused")
    public ExtendedInformation getExtendedInformation() {
        return extendedInformation;
    }

    public DependencyPage extendedInformation(ExtendedInformation extendedInformation) {
        this.extendedInformation = extendedInformation;
        return this;
    }
}
