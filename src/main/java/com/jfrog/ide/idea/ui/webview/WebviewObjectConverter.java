package com.jfrog.ide.idea.ui.webview;

import com.jfrog.ide.common.tree.DependencyNode;
import com.jfrog.ide.common.tree.ImpactTreeNode;
import com.jfrog.ide.common.tree.Issue;
import com.jfrog.ide.common.tree.LicenseViolation;
import com.jfrog.ide.idea.ui.webview.model.*;

import java.util.Arrays;
import java.util.List;

public class WebviewObjectConverter {
    public static DependencyPage convertIssueToDepPage(Issue issue) {
        ExtendedInformation extendedInformation = null;
        if (issue.getResearchInfo() != null) {
            com.jfrog.ide.common.tree.ResearchInfo issueResearchInfo = issue.getResearchInfo();
            JfrogResearchSeverityReason[] severityReasons = Arrays.stream(issueResearchInfo.getSeverityReasons()).map(severityReason -> new JfrogResearchSeverityReason(severityReason.getName(), severityReason.getDescription(), severityReason.isPositive())).toArray(JfrogResearchSeverityReason[]::new);
            extendedInformation = new ExtendedInformation(issueResearchInfo.getShortDescription(), issueResearchInfo.getFullDescription(), issueResearchInfo.getSeverity().name(), issueResearchInfo.getRemediation(), severityReasons);
        }
        DependencyNode dependency = issue.getParentArtifact();
        String[] watchNames = null;
        if (issue.getWatchNames() != null) {
            watchNames = issue.getWatchNames().toArray(new String[0]);
        }
        License[] licenses = null;
        if (dependency.getLicenses() != null) {
            List<com.jfrog.ide.common.tree.License> depLicenses = dependency.getLicenses();
            licenses = new License[depLicenses.size()];
            for (int licIndex = 0; licIndex < licenses.length; licIndex++) {
                com.jfrog.ide.common.tree.License depLicense = depLicenses.get(licIndex);
                licenses[licIndex] = new License(depLicense.getName(), depLicense.getMoreInfoUrl());
            }
        }
        return new DependencyPage(
                issue.getIssueId(),
                dependency.getGeneralInfo().getArtifactId(),
                dependency.getGeneralInfo().getPkgType(),
                dependency.getGeneralInfo().getVersion(),
                issue.getSeverity().name(),
                licenses,
                issue.getSummary(),
                convertVersionRanges(issue.getFixedVersions()),
                convertVersionRanges(issue.getInfectedVersions()),
                convertReferences(issue.getReferences()),
                new Cve(
                        issue.getCve().getCveId(),
                        issue.getCve().getCvssV2Score(),
                        issue.getCve().getCvssV2Vector(),
                        issue.getCve().getCvssV3Score(),
                        issue.getCve().getCvssV3Vector()
                ),
                convertImpactPath(dependency.getImpactPaths()),
                watchNames,
                issue.getLastUpdated(),
                extendedInformation
        );
    }

    public static DependencyPage convertLicenseToDepPage(LicenseViolation license) {
        DependencyNode dependency = license.getParentArtifact();
        String[] watchNames = null;
        if (license.getWatchNames() != null) {
            watchNames = license.getWatchNames().toArray(new String[0]);
        }
        return new DependencyPage(
                license.getName(),
                dependency.getGeneralInfo().getArtifactId(),
                dependency.getGeneralInfo().getPkgType(),
                dependency.getGeneralInfo().getVersion(),
                license.getSeverity().name(),
                null,
                null,
                null,
                null,
                convertReferences(license.getReferences()),
                new Cve(null, null, null, null, null),
                convertImpactPath(dependency.getImpactPaths()),
                watchNames,
                license.getLastUpdated(),
                null
        );
    }

    private static ImpactedPath convertImpactPath(ImpactTreeNode impactTreeNode) {
        ImpactedPath[] children = new ImpactedPath[impactTreeNode.getChildren().size()];
        for (int childIndex = 0; childIndex < children.length; childIndex++) {
            children[childIndex] = convertImpactPath(impactTreeNode.getChildren().get(childIndex));
        }
        return new ImpactedPath(removeComponentIdPrefix(impactTreeNode.getName()), children);
    }

    private static String removeComponentIdPrefix(String compId) {
        final String prefixSeparator = "://";
        int prefixIndex = compId.indexOf(prefixSeparator);
        if (prefixIndex == -1) {
            return compId;
        }
        return compId.substring(prefixIndex + prefixSeparator.length());
    }

    private static String[] convertVersionRanges(List<String> xrayVerRanges) {
        if (xrayVerRanges == null) {
            return new String[0];
        }
        return xrayVerRanges.stream().map(s -> convertVersionRange(s)).toArray(String[]::new);
    }

    private static String convertVersionRange(String xrayVerRange) {
        final char upInclude = ']';
        final char upNotInclude = ')';
        final char downInclude = '[';
        final char downNotInclude = '(';

        final String lt = "<";
        final String lte = "≤";
        final String gt = ">";
        final String gte = "≥";
        final String versionPlacer = "version";
        final String allVersions = "All versions";

        boolean containsLeft = false;
        boolean containsRight = false;

        String[] parts = xrayVerRange.split(",");
        if (parts.length != 2) {
            if (parts.length == 1) {
                String singleVer = parts[0];
                if (singleVer.charAt(0) == downInclude && singleVer.charAt(singleVer.length() - 1) == upInclude) {
                    // Remove [ and ]
                    return singleVer.substring(1, singleVer.length() - 1);
                }
            }
            // Cannot convert
            return xrayVerRange;
        }

        String leftSide = parts[0];
        String rightSide = parts[1];
        if (leftSide.charAt(0) == downInclude) {
            containsLeft = true;
        } else if (leftSide.charAt(0) != downNotInclude) {
            // Cannot convert
            return xrayVerRange;
        }
        if (rightSide.charAt(rightSide.length() - 1) == upInclude) {
            containsRight = true;
        } else if (rightSide.charAt(rightSide.length() - 1) != upNotInclude) {
            // Cannot convert
            return xrayVerRange;
        }

        // Remove [
        String leftVer = leftSide.substring(1).trim();
        // Remove ]
        String rightVer = rightSide.substring(0, rightSide.length() - 1).trim();
        boolean leftEmpty = leftVer.isEmpty();
        boolean rightEmpty = rightVer.isEmpty();

        if (leftEmpty && rightEmpty) {
            return allVersions;
        }
        if (leftEmpty) {
            if (containsRight) {
                return lte + " " + rightVer;
            }
            return lt + " " + rightVer;
        }
        if (rightEmpty) {
            if (containsLeft) {
                return gte + " " + leftVer;
            }
            return gt + " " + leftVer;
        }

        // Left and right sides are not empty
        String res = leftVer + " ";
        if (containsLeft) {
            res += lte;
        } else {
            res += lt;
        }
        res += " " + versionPlacer + " ";
        if (containsRight) {
            res += lte;
        } else {
            res += lt;
        }
        res += " " + rightVer;
        return res;
    }

    private static Reference[] convertReferences(List<String> xrayReferences) {
        if (xrayReferences == null) {
            return null;
        }
        Reference[] converted = new Reference[xrayReferences.size()];
        for (int refIndex = 0; refIndex < converted.length; refIndex++) {
            String xrRef = xrayReferences.get(refIndex);
            // Check if the resource is just a link or if it's in this format: [title](link)
            if (!xrRef.startsWith("[")) {
                converted[refIndex] = new Reference(xrRef, null);
                continue;
            }
            String[] parts = xrRef.split("]\\(");
            if (parts.length != 2 || !parts[1].endsWith(")")) {
                converted[refIndex] = new Reference(xrRef, null);
                continue;
            }
            converted[refIndex] = new Reference(parts[1].substring(0, parts[1].length() - 1), parts[0].substring(1));
        }
        return converted;
    }
}
