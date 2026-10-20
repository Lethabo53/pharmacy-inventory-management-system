package panels;

import dao.MedicineDAO;
import dao.SupplierDAO;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Medicine;
import model.Supplier;
import utils.Refreshable;
import utils.UiUtil;

/** Full CRUD for medicines. Expired rows are red, low-stock rows are orange. */
public class ManageMedicinePanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();

    private final JTextField txtName = new JTextField(16);
    private final JTextField txtCompany = new JTextField(16);
    private final JComboBox<String> cmbType =
            new JComboBox<>(new String[]{"Tablet", "Capsule", "Syrup", "Injection", "Cream"});
    private final JTextField txtPrice = new JTextField(8);
    private final JTextField txtQty = new JTextField(8);
    private final JTextField txtReorder = new JTextField(8);
    private final JTextField txtExpiry = new JTextField(10);
    private final JComboBox<Supplier> cmbSupplier = new JComboBox<>();
    private final JTextField txtSearch = new JTextField(20);

    private final DefaultTableModel model = UiUtil.readOnlyModel(
            "ID", "Name", "Company", "Type", "Price", "In Stock", "Reorder Lvl", "Expiry", "Supplier");
    private final JTable table = new JTable(model);
    private List<Medicine> current = new ArrayList<>();
    private Medicine selected;

    public ManageMedicinePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        cmbType.setEditable(true);
        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        add(buildLegend(), BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Medicine details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        field(form, g, 0, 0, "Name:", txtName);
        field(form, g, 2, 0, "Company:", txtCompany);
        field(form, g, 4, 0, "Type:", cmbType);
        field(form, g, 0, 1, "Price (R):", txtPrice);
        field(form, g, 2, 1, "Quantity:", txtQty);
        field(form, g, 4, 1, "Reorder level:", txtReorder);
        field(form, g, 0, 2, "Expiry (yyyy-MM-dd):", txtExpiry);
        field(form, g, 2, 2, "Supplier:", cmbSupplier);

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.add(btnAdd); btns.add(btnUpdate); btns.add(btnDelete); btns.add(btnClear);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 6;
        form.add(btns, g);

        btnAdd.addActionListener(e -> add());
        btnUpdate.addActionListener(e -> update());
        btnDelete.addActionListener(e -> delete());
        btnClear.addActionListener(e -> clearForm());
        return form;
    }

    private void field(JPanel p, GridBagConstraints g, int x, int y, String label, JComponent c) {
        g.gridwidth = 1;
        g.gridx = x; g.gridy = y; p.add(new JLabel(label), g);
        g.gridx = x + 1; p.add(c, g);
    }

    private JPanel buildTable() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT));
        search.add(new JLabel("Search name/company:"));
        search.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        JButton btnAll = new JButton("Show all");
        search.add(btnSearch);
        search.add(btnAll);
        p.add(search, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setDefaultRenderer(Object.class, new StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelectedIntoForm();
        });
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        btnSearch.addActionListener(e -> loadTable());
        txtSearch.addActionListener(e -> loadTable());
        btnAll.addActionListener(e -> { txtSearch.setText(""); loadTable(); });
        return p;
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel red = new JLabel("  Expired  ");
        red.setOpaque(true); red.setBackground(new Color(255, 205, 205));
        JLabel orange = new JLabel("  At/below reorder level  ");
        orange.setOpaque(true); orange.setBackground(new Color(255, 232, 190));
        p.add(new JLabel("Legend:"));
        p.add(red);
        p.add(orange);
        return p;
    }

    private class StatusRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            if (!sel) {
                c.setBackground(Color.WHITE);
                int mr = t.convertRowIndexToModel(row);
                if (mr < current.size()) {
                    Medicine m = current.get(mr);
                    if (m.isExpired()) c.setBackground(new Color(255, 205, 205));
                    else if (m.isLowStock()) c.setBackground(new Color(255, 232, 190));
                }
            }
            return c;
        }
    }

    // ------------------------------------------------------------------ data
    @Override
    public void refresh() {
        loadSuppliers();
        loadTable();
    }

    private void loadSuppliers() {
        Supplier previous = (Supplier) cmbSupplier.getSelectedItem();
        cmbSupplier.removeAllItems();
        cmbSupplier.addItem(new Supplier(0, "(none)"));
        try {
            for (Supplier s : supplierDAO.findAll()) cmbSupplier.addItem(s);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
        selectSupplier(previous == null ? 0 : previous.getId());
    }

    private void loadTable() {
        try {
            current = medicineDAO.search(txtSearch.getText());
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (Medicine m : current) {
            rows.add(new Object[]{m.getId(), m.getName(), m.getCompany(), m.getType(), m.getPrice(),
                    m.getQuantity(), m.getReorderLevel(), m.getExpiryDate(), m.getSupplierName()});
        }
        UiUtil.fill(model, rows);
        selected = null;
    }

    private void selectSupplier(int id) {
        for (int i = 0; i < cmbSupplier.getItemCount(); i++) {
            if (cmbSupplier.getItemAt(i).getId() == id) { cmbSupplier.setSelectedIndex(i); return; }
        }
        cmbSupplier.setSelectedIndex(0);
    }

    private void loadSelectedIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        selected = current.get(table.convertRowIndexToModel(row));
        txtName.setText(UiUtil.nz(selected.getName()));
        txtCompany.setText(UiUtil.nz(selected.getCompany()));
        cmbType.setSelectedItem(UiUtil.nz(selected.getType()));
        txtPrice.setText(selected.getPrice().toPlainString());
        txtQty.setText(String.valueOf(selected.getQuantity()));
        txtReorder.setText(String.valueOf(selected.getReorderLevel()));
        txtExpiry.setText(UiUtil.nz(selected.getExpiryDate()));
        selectSupplier(selected.getSupplierId());
    }

    private void clearForm() {
        selected = null;
        txtName.setText(""); txtCompany.setText(""); txtPrice.setText("");
        txtQty.setText(""); txtReorder.setText(""); txtExpiry.setText("");
        cmbType.setSelectedIndex(0);
        cmbSupplier.setSelectedIndex(0);
        table.clearSelection();
    }

    // ------------------------------------------------------------------ CRUD
    /** Reads and validates the form; shows an error and returns null if invalid. */
    private Medicine readForm() {
        Medicine m = new Medicine();
        m.setName(txtName.getText().trim());
        m.setCompany(txtCompany.getText().trim());
        m.setType(String.valueOf(cmbType.getSelectedItem()).trim());
        if (m.getName().isEmpty()) { UiUtil.error(this, "Name is required."); return null; }
        try {
            m.setPrice(new BigDecimal(txtPrice.getText().trim()));
            if (m.getPrice().signum() < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UiUtil.error(this, "Price must be a valid non-negative number."); return null;
        }
        try {
            m.setQuantity(Integer.parseInt(txtQty.getText().trim()));
            if (m.getQuantity() < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UiUtil.error(this, "Quantity must be a whole number (0 or more)."); return null;
        }
        try {
            String r = txtReorder.getText().trim();
            m.setReorderLevel(r.isEmpty() ? 10 : Integer.parseInt(r));
            if (m.getReorderLevel() < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UiUtil.error(this, "Reorder level must be a whole number (0 or more)."); return null;
        }
        String ex = txtExpiry.getText().trim();
        if (!ex.isEmpty()) {
            try {
                m.setExpiryDate(LocalDate.parse(ex));
            } catch (DateTimeParseException e) {
                UiUtil.error(this, "Expiry date must be in the format yyyy-MM-dd."); return null;
            }
        }
        Supplier s = (Supplier) cmbSupplier.getSelectedItem();
        m.setSupplierId(s == null ? 0 : s.getId());
        return m;
    }

    private void add() {
        Medicine m = readForm();
        if (m == null) return;
        try {
            medicineDAO.insert(m);
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void update() {
        if (selected == null) { UiUtil.info(this, "Select a medicine in the table first."); return; }
        Medicine m = readForm();
        if (m == null) return;
        m.setId(selected.getId());
        try {
            medicineDAO.update(m);
            clearForm();
            loadTable();
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void delete() {
        if (selected == null) { UiUtil.info(this, "Select a medicine in the table first."); return; }
        if (!UiUtil.confirm(this, "Delete this medicine permanently?")) return;
        try {
            medicineDAO.delete(selected.getId());
            clearForm();
            loadTable();
        } catch (SQLIntegrityConstraintViolationException ex) {
            UiUtil.error(this, "This medicine appears in past sales and cannot be deleted.\n"
                    + "Set its quantity to 0 instead to keep the sales history intact.");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
