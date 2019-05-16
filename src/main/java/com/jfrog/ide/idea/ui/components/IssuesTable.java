package com.jfrog.ide.idea.ui.components;

import com.intellij.ui.table.JBTable;
import com.jfrog.ide.idea.ui.models.IssuesTableModel;
import com.jfrog.ide.idea.ui.renderers.IssuesTableCellRenderer;

/**
 * Created by Yahav Itzhak on 6 Dec 2017.
 */
public class IssuesTable extends JBTable {

    public IssuesTable() {
        setModel(new IssuesTableModel());
        setShowGrid(true);
        setDefaultRenderer(Object.class, new IssuesTableCellRenderer());
        getTableHeader().setReorderingAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
    }
}