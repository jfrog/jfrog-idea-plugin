package org.jfrog.idea.xray;

import com.google.common.collect.Maps;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;
import org.jfrog.idea.xray.persistency.types.Severity;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by romang on 4/16/17.
 */
public class FilterManager {

    public Map<Severity, Boolean> selectedSeverities = Maps.newTreeMap(Collections.reverseOrder());
    public Map<License, Boolean> selectedLicenses = Maps.newHashMap();

    public static FilterManager getInstance(Project project) {
        return ServiceManager.getService(project, FilterManager.class);
    }

    public void setLicenses(Set<License> scanLicenses) {
        scanLicenses.forEach(license -> {
            if (!selectedLicenses.containsKey(license)) {
                selectedLicenses.put(license, true);
            }
        });
    }

    private boolean isSeveritySelected(Issue issue) {
        Severity severity = issue.getSeverity();
        return severity != null && selectedSeverities.get(severity);
    }

    private boolean isSeveritySelected(ScanTreeNode node) {
        if (node.getIssues().size() == 0 && selectedSeverities.get(Severity.Normal)) {
            return true;
        }
        for (Issue issue : node.getIssues()) {
            if (isSeveritySelected(issue)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLicenseSelected(License license) {
        return selectedLicenses.containsKey(license) && selectedLicenses.get(license);
    }

    private boolean isLicenseSelected(ScanTreeNode node) {
        for (License license : node.getLicenses()) {
            if (isLicenseSelected(license)) {
                return true;
            }
        }
        return false;
    }

    public Set<Issue> filterIssues(Set<Issue> allIssues) {
        return allIssues
                .stream()
                .filter(this::isSeveritySelected)
                .collect(Collectors.toSet());
    }

    /**
     * Filter scan results
     * @param unfilteredRoot In - The scan results
     * @param issuesFilteredRoot Out - Filtered issues tree
     * @param LicenseFilteredRoot Out - Filtered licenses tree
     */
    public void applyFilters(ScanTreeNode unfilteredRoot, ScanTreeNode issuesFilteredRoot, ScanTreeNode LicenseFilteredRoot) {
        applyFilters(unfilteredRoot, issuesFilteredRoot, LicenseFilteredRoot, new MutableBoolean(), new MutableBoolean());
    }

    private void applyFilters(ScanTreeNode unfilteredRoot, ScanTreeNode issuesFilteredRoot, ScanTreeNode licenseFilteredRoot, MutableBoolean severitySelected, MutableBoolean licenseSelected) {
        severitySelected.setValue(isSeveritySelected(unfilteredRoot));
        licenseSelected.setValue(isLicenseSelected(unfilteredRoot));
        for (int i = 0; i < unfilteredRoot.getChildCount(); i++) {
            ScanTreeNode unfilteredChild = (ScanTreeNode) unfilteredRoot.getChildAt(i);
            ScanTreeNode filteredSeverityChild = getFilteredTreeNode(unfilteredChild);
            ScanTreeNode filteredLicenseChild = (ScanTreeNode) unfilteredChild.clone();
            MutableBoolean childSeveritySelected = new MutableBoolean();
            MutableBoolean childLicenseSelected = new MutableBoolean();
            applyFilters(unfilteredChild, filteredSeverityChild, filteredLicenseChild, childSeveritySelected, childLicenseSelected);
            if (childSeveritySelected.booleanValue()) {
                severitySelected.setValue(true);
                issuesFilteredRoot.add(filteredSeverityChild);
            }
            if (childLicenseSelected.booleanValue()) {
                licenseSelected.setValue(true);
                licenseFilteredRoot.add(filteredLicenseChild);
            }
        }
    }

    private ScanTreeNode getFilteredTreeNode(ScanTreeNode unfilteredChild) {
        ScanTreeNode filteredSeverityChild = (ScanTreeNode) unfilteredChild.clone();
        filteredSeverityChild.setIssues(filterIssues(unfilteredChild.getIssues()));
        return filteredSeverityChild;
    }
}