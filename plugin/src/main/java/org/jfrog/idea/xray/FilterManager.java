package org.jfrog.idea.xray;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;
import org.jfrog.idea.xray.persistency.types.Severity;
import org.jfrog.idea.xray.persistency.types.Issue;
import org.jfrog.idea.xray.persistency.types.License;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by romang on 4/16/17.
 */

@State(name = "FilterManager", storages = {@Storage(file = "FilterManager.xml")})
public final class FilterManager implements PersistentStateComponent<FilterManager> {

    public Set<Severity> selectedSeverity = new HashSet<>();
    public Set<License> selectedLicenses = new HashSet<>();

    public static FilterManager getInstance(Project project) {
        return ServiceManager.getService(project, FilterManager.class);
    }

    @Nullable
    @Override
    public FilterManager getState() {
        return this;
    }

    @Override
    public void loadState(FilterManager state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public boolean isFiltered(Issue issue) {
        if (selectedSeverity.isEmpty()) {
            return true;
        }
        if (selectedSeverity.contains(issue.getSeverity())) {
            return true;
        }
        return false;
    }

    public boolean isFiltered(License license) {
        if (selectedLicenses.isEmpty() || selectedLicenses.contains(license)) {
            return true;
        }
        return false;
    }

    public boolean isIssuesFiltered(ScanTreeNode node) {
        Set<Issue> issues = node.getAllIssues();
        boolean issuesSelected = issues.isEmpty() && selectedSeverity.isEmpty();
        for (Issue issue : issues) {
            if (isFiltered(issue)) {
                issuesSelected = true;
                break;
            }
        }
        return issuesSelected;
    }

    public boolean isLicenseFiltered(ScanTreeNode node) {
        Set<License> licenses = node.getLicenses();
        if (licenses.isEmpty()) {
            License license = new License();
            license.name = "Unknown";
            licenses = Collections.singleton(license);
        }
        boolean licensesSelected = false;
        for (License license : licenses) {
            if (isFiltered(license)) {
                licensesSelected = true;
                break;
            }
        }
        return licensesSelected;
    }

    public TreeModel filterComponents(TreeModel scanResults) {
        TreeModel issuesTree = new DefaultTreeModel(new ScanTreeNode("All Components"), false);
        getFilterredComponents((ScanTreeNode) scanResults.getRoot(), (ScanTreeNode) issuesTree.getRoot(), true);
        return issuesTree;
    }

    private void getFilterredComponents(ScanTreeNode node, ScanTreeNode filteredNode, boolean rootNode) {
        for (int i = 0; i < node.getChildCount(); i++) {
            ScanTreeNode unfilteredChildNode = (ScanTreeNode) node.getChildAt(i);
            // Filter licenses
            if (rootNode && !isLicenseFiltered(unfilteredChildNode)) {
                continue;
            }

            // Filter issues
            if (!isIssuesFiltered(unfilteredChildNode)) {
                getFilterredComponents(unfilteredChildNode, filteredNode, false);
            } else {
                ScanTreeNode filteredChildNode = (ScanTreeNode) unfilteredChildNode.clone();
                filteredNode.add(filteredChildNode);
                getFilterredComponents(unfilteredChildNode, filteredChildNode, false);
            }
        }
    }

    public TableModel filterIssues(Set<Issue> allIssues) {
        Set<Issue> filteredIssues = new HashSet<>();
        allIssues.forEach(xrayIssue -> {
            if (isFiltered(xrayIssue)) {
                filteredIssues.add(xrayIssue);
            }
        });

        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return Issue.class;
                }
                return super.getColumnClass(columnIndex);
            }
        };

        model.addColumn("issues", filteredIssues.toArray());
        return model;
    }
}
