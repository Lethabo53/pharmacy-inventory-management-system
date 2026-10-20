package panels;

import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.User;
import utils.Refreshable;
import utils.Session;
import utils.UiUtil;

/** Create, edit, reset password and delete Cashier accounts. */
public class ManageUsersPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final JTextField txtUsername = new JTextField(16);
    private final JTextField txtFullName = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(16);

    private final DefaultTableModel model = UiUtil.readOnlyModel("ID", "Username", "Full Name", "Role");
    private final JTable table = new JTable(model);
    private List<User> current = new ArrayList<>();
    private User selected;

    public ManageUsersPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JLabel("Only Cashier accounts are listed. Select a row to rename, reset the password or delete."),
                BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Cashier account"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Username:"), g);
        g.gridx = 1; form.add(txtUsername, g);
        g.gridx = 2; form.add(new JLabel("Full name:"), g);
        g.gridx = 3; form.add(txtFullName, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
        g.gridx = 1; form.add(txtPassword, g);
        g.gridx = 2; g.gridwidth = 2;
        form.add(new JLabel("(required for new accounts and 'Reset password', min 6 characters)"), g);

        JButton btnAdd = new JButton("Create cashier");
        JButton btnUpdate = new JButton("Update name");
        JButton btnReset = new JButton("Reset password");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnReset); btns.add(btnDelete); btns.add(btnClear);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 4;
        form.add(btns, g);

        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> updateName());
        btnReset.addActionListener(e -> resetPassword());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearForm());
        return form;
    }

    @Override
    public void refresh() {
        try {
            current = userDAO.findCashiers();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (User u : current) rows.add(new Object[]{u.getId(), u.getUsername(), u.getFullName(), u.getRole()});
        UiUtil.fill(model, rows);
        selected = null;
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        selected = current.get(table.convertRowIndexToModel(row));
        txtUsername.setText(selected.getUsername());
        txtFullName.setText(selected.getFullName());
        txtPassword.setText("");
    }

    private void clearForm() {
        selected = null;
        txtUsername.setText(""); txtFullName.setText(""); txtPassword.setText("");
        table.clearSelection();
    }

    private boolean passwordOk(String pw) {
        if (pw.length() < 6) {
            UiUtil.error(this, "Password must be at least 6 characters.");
            return false;
        }
        return true;
    }

    private void add() {
        String u = txtUsername.getText().trim();
        String n = txtFullName.getText().trim();
        String pw = new String(txtPassword.getPassword());
        if (u.isEmpty() || n.isEmpty()) { UiUtil.error(this, "Username and full name are required."); return; }
        if (!passwordOk(pw)) return;
        try {
            userDAO.insertCashier(new User(0, u, "Cashier", n), pw);
            clearForm();
            refresh();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UiUtil.error(this, "That username is already taken.");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void updateName() {
        if (selected == null) { UiUtil.info(this, "Select a cashier first."); return; }
        String n = txtFullName.getText().trim();
        if (n.isEmpty()) { UiUtil.error(this, "Full name is required."); return; }
        try {
            userDAO.updateFullName(selected.getId(), n);
            clearForm();
            refresh();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void resetPassword() {
        if (selected == null) { UiUtil.info(this, "Select a cashier first."); return; }
        String pw = new String(txtPassword.getPassword());
        if (!passwordOk(pw)) return;
        try {
            userDAO.resetPassword(selected.getId(), pw);
            UiUtil.info(this, "Password updated.");
            txtPassword.setText("");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void delete() {
        if (selected == null) { UiUtil.info(this, "Select a cashier first."); return; }
        if (selected.getId() == Session.userId()) { UiUtil.error(this, "You cannot delete your own account."); return; }
        if (!UiUtil.confirm(this, "Delete this cashier account?")) return;
        try {
            userDAO.deleteCashier(selected.getId());
            clearForm();
            refresh();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UiUtil.error(this, "This cashier has processed sales, so the account cannot be deleted\n"
                    + "without losing the sales history. Reset their password instead to lock them out.");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
