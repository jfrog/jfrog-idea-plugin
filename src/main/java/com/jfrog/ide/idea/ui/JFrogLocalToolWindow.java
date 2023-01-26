package com.jfrog.ide.idea.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.*;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.util.ui.UIUtil;
import com.jfrog.ide.common.tree.ApplicableIssueNode;
import com.jfrog.ide.common.tree.IssueNode;
import com.jfrog.ide.common.tree.LicenseViolationNode;
import com.jfrog.ide.common.tree.VulnerabilityNode;
import com.jfrog.ide.idea.actions.CollapseAllAction;
import com.jfrog.ide.idea.actions.ExpandAllAction;
import com.jfrog.ide.idea.events.ApplicationEvents;
import com.jfrog.ide.idea.log.Logger;
import com.jfrog.ide.idea.ui.jcef.message.MessagePacker;
import com.jfrog.ide.idea.ui.utils.ComponentUtils;
import com.jfrog.ide.idea.ui.webview.WebviewObjectConverter;
import com.jfrog.ide.idea.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jfrog.ide.idea.ui.JFrogToolWindow.SCROLL_BAR_SCROLLING_UNITS;

/**
 * @author yahavi
 */
public class JFrogLocalToolWindow extends AbstractJFrogToolWindow {
    private final LocalComponentsTree componentsTree;
    private final JBCefBrowserBase browser;
    private final OnePixelSplitter verticalSplit;
    private final MessagePacker messagePacker;
    private IssueNode selectedIssue;
    private Path tempDirPath;

    /**
     * @param project - Currently opened IntelliJ project
     */
    public JFrogLocalToolWindow(@NotNull Project project) {
        super(project);
        componentsTree = LocalComponentsTree.getInstance(project);
        browser = new JBCefBrowser();

        messagePacker = new MessagePacker(browser);
        initVulnerabilityInfoBrowser();

        JPanel toolbar = createActionToolbar();
        toolbar.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
        JPanel leftPanel = new JBPanel<>(new BorderLayout());
        leftPanel.add(toolbar, BorderLayout.PAGE_START);
        leftPanel.add(createComponentsTreeView());

        verticalSplit = new OnePixelSplitter(false, 0.4f);
        verticalSplit.setFirstComponent(leftPanel);
        setContent(verticalSplit);

        registerListeners();
    }

    @Override
    public JPanel createActionToolbar() {
        DefaultActionGroup actionGroup = new DefaultActionGroup(new CollapseAllAction(componentsTree), new ExpandAllAction(componentsTree));
        actionGroup.addAction(ActionManager.getInstance().getAction("JFrog.RefreshLocal"), Constraints.FIRST);
        return createJFrogToolbar(actionGroup);
    }

    /**
     * Register the issues tree listeners.
     */
    public void registerListeners() {
        // Xray credentials were set listener
        appBusConnection.subscribe(ApplicationEvents.ON_CONFIGURATION_DETAILS_CHANGE, () -> ApplicationManager.getApplication().invokeLater(this::onConfigurationChange));

        // Component selection listener
        componentsTree.addTreeSelectionListener(e -> {
            if (e == null || e.getNewLeadSelectionPath() == null || !(e.getNewLeadSelectionPath().getLastPathComponent() instanceof IssueNode)) {
                verticalSplit.setSecondComponent(null);
                return;
            }

            selectedIssue = (IssueNode) e.getNewLeadSelectionPath().getLastPathComponent();
            updateIssueOrLicenseInWebview(selectedIssue);
            verticalSplit.setSecondComponent(browser.getComponent());
        });
        projectBusConnection.subscribe(ApplicationEvents.ON_SCAN_LOCAL_STARTED, () -> ApplicationManager.getApplication().invokeLater(this::resetViews));
        componentsTree.addRightClickListener();
    }

    private void initVulnerabilityInfoBrowser() {
        if (!JBCefApp.isSupported()) {
            Logger.getInstance().error("Could not open the issue details view - JCEF is not supported");
            return;
        }

        try {
            tempDirPath = Files.createTempDirectory("jfrog-idea-plugin");
            Utils.extractFromResources("/jfrog-ide-webview", tempDirPath);
        } catch (IOException | URISyntaxException e) {
            Logger.getInstance().error(e.getMessage());
            return;
        }

        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                updateIssueOrLicenseInWebview(selectedIssue);
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                Logger.getInstance().error("An error occurred while opening the issue details view: " + errorText);
            }
        }, browser.getCefBrowser());

        String pageUri = tempDirPath.resolve("index.html").toFile().toURI().toString();
        browser.loadURL(pageUri);
    }

    /**
     * Create the components tree panel.
     *
     * @return the components tree panel
     */
    private JComponent createComponentsTreeView() {
        JPanel treePanel = new JBPanel<>(new GridLayout()).withBackground(UIUtil.getTableBackground());
        TreeSpeedSearch treeSpeedSearch = new TreeSpeedSearch(componentsTree, ComponentUtils::getPathSearchString, true);
        treeSpeedSearch.getComponent().getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treePanel.add(treeSpeedSearch.getComponent(), BorderLayout.WEST);
        JScrollPane treeScrollPane = ScrollPaneFactory.createScrollPane(treePanel);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_BAR_SCROLLING_UNITS);
        treeScrollPane.setBorder(null);
        return treeScrollPane;
    }

    private void updateIssueOrLicenseInWebview(IssueNode vulnerabilityOrViolation) {
        if (vulnerabilityOrViolation instanceof VulnerabilityNode) {
            VulnerabilityNode issue = (VulnerabilityNode) vulnerabilityOrViolation;
            messagePacker.send(WebviewObjectConverter.convertIssueToDepPage(issue));
        } else if (vulnerabilityOrViolation instanceof ApplicableIssueNode) {
            ApplicableIssueNode node = (ApplicableIssueNode) vulnerabilityOrViolation;
            messagePacker.send(WebviewObjectConverter.convertIssueToDepPage(node.getIssue()));
            navigateToFile(node);
        } else if (vulnerabilityOrViolation instanceof LicenseViolationNode) {
            LicenseViolationNode license = (LicenseViolationNode) vulnerabilityOrViolation;
            messagePacker.send(WebviewObjectConverter.convertLicenseToDepPage(license));
        }
    }

    private void navigateToFile(ApplicableIssueNode node) {
        ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFile sourceCodeFile = LocalFileSystem.getInstance().findFileByIoFile(new File(node.getFilePath()));
            if (sourceCodeFile == null) {
                return;
            }
            PsiFile targetFile = PsiManager.getInstance(project).findFile(sourceCodeFile);
            if (targetFile == null) {
                return;
            }
            int lineOffset = StringUtil.lineColToOffset(targetFile.getText(), node.getRowStart(), node.getColStart());
            PsiElement element = targetFile.findElementAt(lineOffset);
            if (element instanceof Navigatable) {
                ((Navigatable) element).navigate(true);
            } else targetFile.navigate(true);
        });
    }

    /**
     * Clear the component tree.
     */
    void resetViews() {
        if (componentsTree != null) {
            componentsTree.reset();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        browser.dispose();
        try {
            FileUtils.deleteDirectory(tempDirPath.toFile());
        } catch (IOException e) {
            Logger.getInstance().warn("Temporary directory could not be deleted: " + tempDirPath.toString() + ". Error: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
