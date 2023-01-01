package com.jfrog.ide.idea.ui.webview;

import com.jfrog.ide.common.tree.*;
import com.jfrog.ide.idea.ui.webview.model.*;
import com.jfrog.ide.idea.ui.webview.model.Cve;
import com.jfrog.ide.idea.ui.webview.model.License;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public class WebviewObjectConverter {
    public static DependencyPage convertIssueToDepPage(IssueNode issueNode) {
        ExtendedInformation extendedInformation = null;
        if (issueNode.getResearchInfo() != null) {
            ResearchInfo issueResearchInfo = issueNode.getResearchInfo();
            JfrogResearchSeverityReason[] severityReasons = Arrays.stream(issueResearchInfo.getSeverityReasons()).map(severityReason -> new JfrogResearchSeverityReason(severityReason.getName(), severityReason.getDescription(), severityReason.isPositive())).toArray(JfrogResearchSeverityReason[]::new);
            extendedInformation = new ExtendedInformation(issueResearchInfo.getShortDescription(), issueResearchInfo.getFullDescription(), issueResearchInfo.getSeverity().name(), issueResearchInfo.getRemediation(), severityReasons);
        }
        DependencyNode dependency = issueNode.getParentArtifact();
        String[] watchNames = null;
        if (issueNode.getWatchNames() != null) {
            watchNames = issueNode.getWatchNames().toArray(new String[0]);
        }
        License[] licenses = null;
        if (dependency.getLicenses() != null) {
            licenses = dependency.getLicenses().stream().map(depLicense -> new License(depLicense.getName(), depLicense.getMoreInfoUrl())).toArray(License[]::new);
        }
        return new DependencyPage(
                issueNode.getIssueId(),
                dependency.getGeneralInfo().getArtifactId(),
                dependency.getGeneralInfo().getPkgType(),
                dependency.getGeneralInfo().getVersion(),
                issueNode.getSeverity().name(),
                licenses,
                issueNode.getSummary(),
                convertVersionRanges(issueNode.getFixedVersions()),
                convertVersionRanges(issueNode.getInfectedVersions()),
                convertReferences(issueNode.getReferences()),
                convertCve(issueNode.getCve()),
                convertImpactPath(dependency.getImpactPaths()),
                watchNames,
                issueNode.getLastUpdated(),
                extendedInformation
        );
    }

    public static DependencyPage convertLicenseToDepPage(LicenseViolationNode license) {
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
        ImpactedPath[] children = impactTreeNode.getChildren().stream().map(WebviewObjectConverter::convertImpactPath).toArray(ImpactedPath[]::new);
        return new ImpactedPath(removeComponentIdPrefix(impactTreeNode.getName()), children);
    }

    private static Cve convertCve(com.jfrog.ide.common.tree.Cve cve) {
        return new Cve(
            cve.getCveId(),
            cve.getCvssV2Score(),
            cve.getCvssV2Vector(),
            cve.getCvssV3Score(),
            cve.getCvssV3Vector()
        );
    }

    private static String removeComponentIdPrefix(String compId) {
        return StringUtils.substringAfter(compId, "://");
    }

    private static String[] convertVersionRanges(List<String> xrayVerRanges) {
        if (xrayVerRanges == null) {
            return new String[0];
        }
        return xrayVerRanges.stream().map(WebviewObjectConverter::convertVersionRange).toArray(String[]::new);
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
        if (parts.length == 1) {
            String singleVer = parts[0];
            if (singleVer.charAt(0) == downInclude && singleVer.charAt(singleVer.length() - 1) == upInclude) {
                // Remove [ and ]
                return singleVer.substring(1, singleVer.length() - 1);
            }
        }

        if (parts.length != 2) {
            // Cannot convert
            return xrayVerRange;
        }

        // Parse both parts of the version range
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
        return xrayReferences.stream().map(xrRef -> {
            if (!xrRef.startsWith("[")) {
                return new Reference(xrRef, null);
            }
            String[] parts = xrRef.split("]\\(");
            if (parts.length != 2 || !parts[1].endsWith(")")) {
                return new Reference(xrRef, null);
            }
            return new Reference(parts[1].substring(0, parts[1].length() - 1), parts[0].substring(1));
        }).toArray(Reference[]::new);
    }
}
