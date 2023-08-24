package com.jfrog.ide.idea.ui.webview;

import com.jfrog.ide.common.nodes.*;
import com.jfrog.ide.common.nodes.subentities.*;
import com.jfrog.ide.common.scan.ComponentPrefix;
import com.jfrog.ide.idea.scan.ScannerBase;
import com.jfrog.ide.idea.ui.webview.model.Cve;
import com.jfrog.ide.idea.ui.webview.model.Evidence;
import com.jfrog.ide.idea.ui.webview.model.License;
import com.jfrog.ide.idea.ui.webview.model.*;
import org.apache.commons.collections4.CollectionUtils;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public class WebviewObjectConverter {
    public static DependencyPage convertIssueToDepPage(VulnerabilityNode vulnerabilityNode) {
        ExtendedInformation extendedInformation = null;
        if (vulnerabilityNode.getResearchInfo() != null) {
            ResearchInfo issueResearchInfo = vulnerabilityNode.getResearchInfo();
            Collection<SeverityReason> severityReasons = CollectionUtils.emptyIfNull(issueResearchInfo.getSeverityReasons());
            JfrogResearchSeverityReason[] researchSeverityReasons = severityReasons.stream().map(severityReason -> new JfrogResearchSeverityReason(severityReason.getName(), severityReason.getDescription(), severityReason.isPositive())).toArray(JfrogResearchSeverityReason[]::new);
            extendedInformation = new ExtendedInformation(issueResearchInfo.getShortDescription(), issueResearchInfo.getFullDescription(), issueResearchInfo.getSeverity().name(), issueResearchInfo.getRemediation(), researchSeverityReasons);
        }
        DependencyNode dependency = vulnerabilityNode.getParentArtifact();
        String[] watchNames = null;
        if (vulnerabilityNode.getWatchNames() != null) {
            watchNames = vulnerabilityNode.getWatchNames().toArray(new String[0]);
        }
        License[] licenses = null;
        if (dependency.getLicenses() != null) {
            licenses = dependency.getLicenses().stream().map(depLicense -> new License(depLicense.getName(), depLicense.getMoreInfoUrl())).toArray(License[]::new);
        }
        return new DependencyPage()
                .id(vulnerabilityNode.getIssueId())
                .component(dependency.getArtifactId())
                .componentType(getPackageTypeName(dependency.getComponentId()))
                .version(dependency.getVersion())
                .severity(vulnerabilityNode.getSeverity(false).name())
                .license(licenses)
                .summary(vulnerabilityNode.getSummary())
                .fixedVersion(convertVersionRanges(vulnerabilityNode.getFixedVersions()))
                .infectedVersion(convertVersionRanges(vulnerabilityNode.getInfectedVersions()))
                .references(convertReferences(vulnerabilityNode.getReferences()))
                .cve(convertCve(vulnerabilityNode.getCve(), convertApplicableDetails(vulnerabilityNode.getApplicableInfo())))
                .impactGraph(convertImpactGraph(dependency.getImpactTree()))
                .watchName(watchNames)
                .edited(vulnerabilityNode.getLastUpdated())
                .extendedInformation(extendedInformation);
    }

    public static IssuePage convertFileIssueToIssuePage(FileIssueNode fileIssueNodeNode) {
        return new IssuePage()
                .type(ConvertPageType(fileIssueNodeNode.getReporterType()))
                .severity(fileIssueNodeNode.getSeverity().name())
                .description(fileIssueNodeNode.getReason())
                .header(fileIssueNodeNode.getTitle())
                .location(convertFileLocation(fileIssueNodeNode));
    }

    public static IssuePage convertEosIssueToEosIssuePage(EosIssueNode eosIssueNode) {
        return new EosIssuePage(convertFileIssueToIssuePage(eosIssueNode))
                .setAnalysisSteps(convertCodeFlowsToLocations(eosIssueNode.getCodeFlows()))
                .setRuleID(eosIssueNode.getRuleID());
    }

    private static Location[] convertCodeFlowsToLocations(FindingInfo[][] codeFlows) {
        if (codeFlows != null && codeFlows.length > 0) {
            Location[] locations = new Location[codeFlows[0].length];
            for (int i = 0; i < codeFlows[0].length; i++) {
                FindingInfo codeFlow = codeFlows[0][i];
                locations[i] = new Location(
                        codeFlow.getFilePath(),
                        Paths.get(codeFlow.getFilePath()).getFileName().toString(),
                        codeFlow.getRowStart(),
                        codeFlow.getColStart(),
                        codeFlow.getRowEnd(),
                        codeFlow.getColEnd(),
                        codeFlow.getLineSnippet());
            }
            return locations;
        }
        return null;
    }

    private static String ConvertPageType(SourceCodeScanType reporterType) {
        return switch (reporterType) {
            case SECRETS -> "SECRETS";
            case IAC -> "IAC";
            case EOS -> "EOS";
            default -> "EMPTY";
        };
    }

    private static Location convertFileLocation(FileIssueNode fileIssueNodeNode) {
        return new Location(
                fileIssueNodeNode.getFilePath(),
                Paths.get(fileIssueNodeNode.getFilePath()).getFileName().toString(),
                fileIssueNodeNode.getRowStart() + 1,
                fileIssueNodeNode.getColStart() + 1,
                fileIssueNodeNode.getRowEnd() + 1,
                fileIssueNodeNode.getColEnd() + 1,
                fileIssueNodeNode.getLineSnippet());
    }

    private static ApplicableDetails convertApplicableDetails(ApplicableInfo applicableInfo) {
        ApplicableDetails applicableDetails = null;
        if (applicableInfo != null) {
            if (applicableInfo.isApplicable()) {
                String searchTarget = applicableInfo.getSearchTarget();
                List<com.jfrog.ide.common.nodes.subentities.Evidence> evidencesInfo = applicableInfo.getEvidences();
                Evidence[] evidences = new Evidence[evidencesInfo.size()];
                var i = 0;
                for (var evidenceInfo : evidencesInfo) {
                    evidences[i++] = new Evidence(evidenceInfo.getReason(), evidenceInfo.getFilePathEvidence(), evidenceInfo.getCodeEvidence());
                }
                applicableDetails = new ApplicableDetails(true, evidences, searchTarget);
            } else {
                // If we know the issue is not applicable, adds the relevant ApplicableDetails.
                applicableDetails = new ApplicableDetails(false, null, null);
            }
        }
        return applicableDetails;
    }

    public static DependencyPage convertLicenseToDepPage(LicenseViolationNode license) {
        DependencyNode dependency = license.getParentArtifact();
        String[] watchNames = null;
        if (license.getWatchNames() != null) {
            watchNames = license.getWatchNames().toArray(new String[0]);
        }
        return new DependencyPage()
                .id(license.getName())
                .component(dependency.getArtifactId())
                .componentType(getPackageTypeName(dependency.getComponentId()))
                .version(dependency.getVersion())
                .severity(license.getSeverity().name())
                .references(convertReferences(license.getReferences()))
                .impactGraph(convertImpactGraph(dependency.getImpactTree()))
                .watchName(watchNames)
                .edited(license.getLastUpdated());
    }

    private static ImpactGraph convertImpactGraph(ImpactTree impactTree) {
        return new ImpactGraph(convertImpactGraphNode(impactTree.getRoot()), impactTree.getImpactPathsCount(), ScannerBase.IMPACT_PATHS_LIMIT);
    }

    private static ImpactGraphNode convertImpactGraphNode(ImpactTreeNode impactTreeNode) {
        ImpactGraphNode[] children = impactTreeNode.getChildren().stream().map(WebviewObjectConverter::convertImpactGraphNode).toArray(ImpactGraphNode[]::new);
        return new ImpactGraphNode(impactTreeNode.getName(), children);
    }

    private static Cve convertCve(com.jfrog.ide.common.nodes.subentities.Cve cve, ApplicableDetails applicableDetails) {
        return new Cve(
                cve.getCveId(),
                cve.getCvssV2Score(),
                cve.getCvssV2Vector(),
                cve.getCvssV3Score(),
                cve.getCvssV3Vector(),
                applicableDetails
        );
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

    private static String getPackageTypeName(String componentId) {
        String GENERIC_PKG_TYPE = "Generic";
        String[] compIdParts = componentId.split("://");
        if (compIdParts.length != 2) {
            return GENERIC_PKG_TYPE;
        }
        try {
            ComponentPrefix prefix = ComponentPrefix.valueOf(compIdParts[0].toUpperCase());
            return prefix.getPackageTypeName();
        } catch (IllegalArgumentException e) {
            return GENERIC_PKG_TYPE;
        }
    }
}
