/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.sql;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;

import net.sf.jabref.Globals;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Dialog box for collecting database connection strings from the user
 *
 * @author pattonlk
 */
public class DBConnectDialog extends JDialog {

    private static final long serialVersionUID = 7395276764910821176L;
    // input fields
    private final JComboBox<String> cmbServerType = new JComboBox<String>();
    private final JTextField txtServerHostname = new JTextField(40);
    private final JTextField txtDatabase = new JTextField(40);
    private final JTextField txtUsername = new JTextField(40);
    private final JPasswordField pwdPassword = new JPasswordField(40);

    private DBStrings dbStrings = new DBStrings();

    private boolean connectToDB;


    /** Creates a new instance of DBConnectDialog */
    public DBConnectDialog(JFrame parent, DBStrings dbs) {

        super(parent, Localization.lang("Connect to SQL database"), true);

        this.setResizable(false);
        this.setLocationRelativeTo(parent);

        dbStrings = dbs;

        // build collections of components
        ArrayList<JLabel> lhs = new ArrayList<JLabel>();
        JLabel lblServerType = new JLabel();
        lhs.add(lblServerType);
        JLabel lblServerHostname = new JLabel();
        lhs.add(lblServerHostname);
        JLabel lblDatabase = new JLabel();
        lhs.add(lblDatabase);
        JLabel lblUsername = new JLabel();
        lhs.add(lblUsername);
        JLabel lblPassword = new JLabel();
        lhs.add(lblPassword);

        ArrayList<JComponent> rhs = new ArrayList<JComponent>();
        rhs.add(cmbServerType);
        rhs.add(txtServerHostname);
        rhs.add(txtDatabase);
        rhs.add(txtUsername);
        rhs.add(pwdPassword);

        // setup label text
        lblServerType.setText(Localization.lang("Server Type :"));
        lblServerHostname.setText(Localization.lang("Server Hostname :"));
        lblDatabase.setText(Localization.lang("Database :"));
        lblUsername.setText(Localization.lang("Username :"));
        lblPassword.setText(Localization.lang("Password :"));

        // set label text alignment
        for (JLabel label : lhs) {
            label.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        // set button text
        JButton btnConnect = new JButton();
        btnConnect.setText(Localization.lang("Connect"));
        JButton btnCancel = new JButton();
        btnCancel.setText(Localization.lang("Cancel"));

        // init input fields to current DB strings
        String srvSel = dbStrings.getServerType();
        String[] srv = dbStrings.getServerTypes();
        for (String aSrv : srv) {
            cmbServerType.addItem(aSrv);
        }

        cmbServerType.setSelectedItem(srvSel);
        txtServerHostname.setText(dbStrings.getServerHostname());
        txtDatabase.setText(dbStrings.getDatabase());
        txtUsername.setText(dbStrings.getUsername());
        pwdPassword.setText(dbStrings.getPassword());

        // construct dialog
        FormBuilder builder = FormBuilder.create().layout(new
                FormLayout("right:pref, 4dlu, fill:pref", "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref"));

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add labels and input fields
        builder.add(lblServerType).xy(1, 1);
        builder.add(cmbServerType).xy(3, 1);
        builder.add(lblServerHostname).xy(1, 3);
        builder.add(txtServerHostname).xy(3, 3);
        builder.add(lblDatabase).xy(1, 5);
        builder.add(txtDatabase).xy(3, 5);
        builder.add(lblUsername).xy(1, 7);
        builder.add(txtUsername).xy(3, 7);
        builder.add(lblPassword).xy(1, 9);
        builder.add(pwdPassword).xy(3, 9);

        // add the panel to the CENTER of your dialog:
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        // add buttons are added in a similar way:
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(btnConnect);
        bb.addButton(btnCancel);
        bb.addGlue();

        // add the buttons to the SOUTH of your dialog:
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        ActionListener connectAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String errorMessage = checkInput();

                if (errorMessage == null) {
                    storeSettings();
                    setVisible(false);
                    setConnectToDB(true);
                } else {
                    JOptionPane.showMessageDialog(null, errorMessage,
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                }

            }
        };

        btnConnect.addActionListener(connectAction);
        txtDatabase.addActionListener(connectAction);
        txtServerHostname.addActionListener(connectAction);
        txtUsername.addActionListener(connectAction);
        pwdPassword.addActionListener(connectAction);

        AbstractAction cancelAction = new AbstractAction() {

            private static final long serialVersionUID = 812282483921018251L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
                setConnectToDB(false);
            }
        };
        btnCancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);
    }

    /**
     * Checks the user input, and ensures that required fields have entries
     *
     * @return 
     *      Appropriate error message to be displayed to user
     */
    private String checkInput() {

        String[] fields = {"Server Hostname", "Database", "Username"};
        String[] errors = new String[fields.length];
        int cnt = 0;

        if (txtServerHostname.getText().trim().isEmpty()) {
            errors[cnt] = fields[0];
            cnt++;
        }

        if (txtDatabase.getText().trim().isEmpty()) {
            errors[cnt] = fields[1];
            cnt++;
        }

        if (txtUsername.getText().trim().isEmpty()) {
            errors[cnt] = fields[2];
            cnt++;
        }

        String errMsg = Localization.lang("Please specify the ");

        switch (cnt) {
        case 0:
            errMsg = null;
            break;
        case 1:
            errMsg = errMsg + errors[0] + '.';
            break;
        case 2:
            errMsg = errMsg + errors[0] + " and " + errors[1] + '.';
            break;
        case 3:
            errMsg = errMsg + errors[0] + ", " + errors[1] + ", and " + errors[2] + '.';
            break;
        default:

        }

        return errMsg;
    }

    /**
     * Save user input.
     */
    private void storeSettings() {
        dbStrings.setServerType(cmbServerType.getSelectedItem().toString());
        dbStrings.setServerHostname(txtServerHostname.getText());
        dbStrings.setDatabase(txtDatabase.getText());
        dbStrings.setUsername(txtUsername.getText());

        // Store these settings so they appear as default next time:
        dbStrings.storeToPreferences();

        char[] pwd = pwdPassword.getPassword();
        String tmp = "";
        for (char aPwd : pwd) {
            tmp = tmp + aPwd;
        }
        dbStrings.setPassword(tmp);
        Arrays.fill(pwd, '0');

    }

    public DBStrings getDBStrings() {
        return dbStrings;
    }

    public void setDBStrings(DBStrings dbStrings) {
        this.dbStrings = dbStrings;
    }

    public boolean getConnectToDB() {
        return connectToDB;
    }

    private void setConnectToDB(boolean connectToDB) {
        this.connectToDB = connectToDB;
    }

}
