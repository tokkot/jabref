/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.collab;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import net.sf.jabref.Globals;
import java.awt.Insets;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.undo.NamedCompound;

class ChangeDisplayDialog extends JDialog implements TreeSelectionListener {

    private BibtexDatabase secondary;
    private final JTree tree;
    private final JPanel infoPanel = new JPanel();
    private final JCheckBox cb = new JCheckBox(Globals.lang("Accept change"));
    private final JLabel rootInfo = new JLabel(Globals.lang("Select the tree nodes to view and accept or reject changes") + '.');
    private Change selected = null;
    private JComponent infoShown = null;
    private boolean okPressed = false;


    public ChangeDisplayDialog(JFrame owner, final BasePanel panel,
            BibtexDatabase secondary, final DefaultMutableTreeNode root) {
        super(owner, Globals.lang("External changes"), true);
        this.secondary = secondary;

        // Just to be sure, put in an empty secondary base if none is given:
        if (secondary == null) {
            this.secondary = new BibtexDatabase();
        }
        tree = new JTree(root);
        tree.addTreeSelectionListener(this);
        JSplitPane pane = new JSplitPane();
        pane.setLeftComponent(new JScrollPane(tree));
        JPanel infoBorder = new JPanel();
        pane.setRightComponent(infoBorder);

        cb.setMargin(new Insets(2, 2, 2, 2));
        cb.setEnabled(false);
        infoPanel.setLayout(new BorderLayout());
        infoBorder.setLayout(new BorderLayout());
        infoBorder.setBorder(BorderFactory.createEtchedBorder());
        infoBorder.add(infoPanel, BorderLayout.CENTER);
        setInfo(rootInfo);
        infoPanel.add(cb, BorderLayout.SOUTH);

        JButton ok = new JButton(Globals.lang("Ok"));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(ok);
        JButton cancel = new JButton(Globals.lang("Cancel"));
        buttonPanel.add(cancel);

        getContentPane().add(pane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        cb.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (selected != null) {
                    selected.setAccepted(cb.isSelected());
                }
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                // Perform all accepted changes:
                // Store all edits in an Undoable object:
                NamedCompound ce = new NamedCompound(Globals.lang("Merged external changes"));
                @SuppressWarnings("unchecked")
                Enumeration<Change> enumer = root.children();
                boolean anyDisabled = false;
                while (enumer.hasMoreElements()) {
                    Change c = enumer.nextElement();
                    boolean allAccepted = false;
                    if (c.isAcceptable() && c.isAccepted()) {
                        allAccepted = c.makeChange(panel, ChangeDisplayDialog.this.secondary, ce);
                    }

                    if (!allAccepted) {
                        anyDisabled = true;
                    }
                }
                ce.end();
                panel.undoManager.addEdit(ce);
                if (anyDisabled) {
                    panel.markBaseChanged();
                }
                panel.setUpdatedExternally(false);
                dispose();
                okPressed = true;
            }
        });

        pack();
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void setInfo(JComponent comp) {
        if (infoShown != null) {
            infoPanel.remove(infoShown);
        }
        infoShown = comp;
        infoPanel.add(infoShown, BorderLayout.CENTER);
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    /**
     * valueChanged
     *
     * @param e TreeSelectionEvent
     */
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        Object o = tree.getLastSelectedPathComponent();
        if (o instanceof Change) {
            selected = (Change) o;
            setInfo(selected.description());
            cb.setSelected(selected.isAccepted());
            cb.setEnabled(selected.isAcceptable());
        } else {
            setInfo(rootInfo);
            selected = null;
            cb.setEnabled(false);
        }
    }
}
