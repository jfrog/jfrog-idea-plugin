package com.jfrog.ide.idea.ui.webview.model;

import java.io.Serializable;

public class DependencyPage implements Serializable {
    String id;
    String name;
    String type;
    String version;
    String severity;
    License license;
    String summary;
    String[] fixedVersion;
    String[] infectedVersion;
    Reference[] references;
    Cve cve;
    ImpactedPath impactedPath;

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String[] getInfectedVersion() {
        return infectedVersion;
    }

    public void setInfectedVersion(String[] infectedVersion) {
        this.infectedVersion = infectedVersion;
    }

    public Reference[] getReferences() {
        return references;
    }

    public void setReferences(Reference[] references) {
        this.references = references;
    }

    public ImpactedPath getImpactedPath() {
        return impactedPath;
    }

    public void setImpactedPath(ImpactedPath impactedPath) {
        this.impactedPath = impactedPath;
    }

    public ResearchInfo getResearchInfo() {
        return researchInfo;
    }

    public void setResearchInfo(ResearchInfo researchInfo) {
        this.researchInfo = researchInfo;
    }

    ResearchInfo researchInfo;
    public DependencyPage() {
    }

    public DependencyPage(String id, String name, String type, String version, String severity, License license, String summary, String[] fixedVersion, String[] infectedVersion, Reference[] references, Cve cve, ImpactedPath impactedPath, ResearchInfo researchInfo) {
        this.id = id;
        this.name = name;
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
        this.researchInfo = researchInfo;
    }

    public Cve getCve() {
        return cve;
    }

    public void setCve(Cve cve) {
        this.cve = cve;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }


    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }


    public String[] getFixedVersion() {
        return fixedVersion;
    }

    public void setFixedVersion(String[] fixedVersion) {
        this.fixedVersion = fixedVersion;
    }
}
