package com.jfrog.ide.idea.ui.licenses;

import com.jfrog.ide.idea.ui.ComponentDetail;
import org.jfrog.build.extractor.scan.DependenciesTree;

import javax.swing.*;
import java.awt.*;

/**
 * @author yahavi
 */
class ComponentLicenseDetails extends ComponentDetail {

    private ComponentLicenseDetails(DependenciesTree node) {
        super(node);
    }

    static void createLicenseDetailsView(JPanel panel, DependenciesTree node) {
        if (node == null || node.getGeneralInfo() == null) {
            createComponentInfoNotAvailablePanel(panel);
            return;
        }
        replaceAndUpdateUI(panel, new ComponentLicenseDetails(node), BorderLayout.NORTH);
    }
}
