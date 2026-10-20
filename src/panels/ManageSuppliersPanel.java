package panels;

import dao.SupplierDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Supplier;
import utils.Refreshable;
import utils.UiUtil;

/** CRUD for supplier details. */
public class ManageSuppliersPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final JTextField txtName = new JTextField(20);
    private final JTextField txtContact = new JTextField(20);
    private final JTextField txtPhone = new JTextField(14);
    private final JTextField txtEmail = new JTextField(20);
    private final JTextArea txtAddress = new JTextArea(3, 20);

    private final DefaultTableModel model =
            UiUtil.readOnlyModel("ID", "Name", "Contact Person", "Phone", "Email", "Address");
    private final JTable table = new JTable(model);
    private List<Supplier> current = new ArrayList<>();
    private Supplier selected;

    public ManageSuppliersPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildForm(), BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Supplier details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        row(form, g, 0, 0, "Name:", txtName);
        row(form, g, 2, 0, "Contact person:", txtContact);
        row(form, g, 0, 1, "Phone:", txtPhone);
        row(form, g, 2, 1, "Email:", txtEmail);
        txtAddress.setLineWrap(true);
        row(form, g, 0, 2, "Address:", new JScrollPane(txtAddress));

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnDelete); btns.add(btnClear);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 4;
        form.add(btns, g);

        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearForm());
        return form;
    }

    private void row(JPanel p, GridBagConstraints g, int x, int y, String label, JComponent c) {
        g.gridwidth = 1;
        g.gridx = x; g.gridy = y; p.add(new JLabel(label), g);
        g.gridx = x + 1; p.add(c, g);
    }

    @Override
    public void refresh() {
        try {
            current = supplierDAO.findAll();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (Supplier s : current) {
            rows.add(new Object[]{s.getId(), s.getName(), s.getContactPerson(), s.getPhone(),
                    s.getEmail(), s.getAddress()});
        }
        UiUtil.fill(model, rows);
        selected = null;
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        selected = current.get(table.convertRowIndexToModel(row));
        txtName.setText(UiUtil.nz(selected.getName()));
        txtContact.setText(UiUtil.nz(selected.getContactPerson()));
        txtPhone.setText(UiUtil.nz(selected.getPhone()));
        txtEmail.setText(UiUtil.nz(selected.getEmail()));
        txtAddress.setText(UiUtil.nz(selected.getAddress()));
    }

    private void clearForm() {
        selected = null;
        txtName.setText(""); txtContact.setText(""); txtPhone.setText("");
        txtEmail.setText(""); txtAddress.setText("");
        table.clearSelection();
    }

    private Supplier readForm() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) { UiUtil.error(this, "Supplier name is required."); return null; }
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            UiUtil.error(this, "Please enter a valid email address.");
            return null;
        }
        Supplier s = new Supplier(selected == null ? 0 : selected.getId(), name);
        s.setContactPerson(txtContact.getText().trim());
        s.setPhone(txtPhone.getText().trim());
        s.setEmail(email);
        s.setAddress(txtAddress.getText().trim());
        return s;
    }

    private void add() {
        Supplier s = readForm();
        if (s == null) return;
        try {
            supplierDAO.insert(s);
            clearForm();
            refresh();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void update() {
        if (selected == null) { UiUtil.info(this, "Select a supplier in the table first."); return; }
        Supplier s = readForm();
        if (s == null) return;
        try {
            supplierDAO.update(s);
            clearForm();
            refresh();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void delete() {
        if (selected == null) { UiUtil.info(this, "Select a supplier in the table first."); return; }
        if (!UiUtil.confirm(this, "Delete this supplier? Medicines linked to it will remain but show no supplier.")) return;
        try {
            supplierDAO.delete(selected.getId());
            clearForm();
            refresh();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
