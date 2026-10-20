package panels;

import dao.MedicineDAO;
import dao.SaleDAO;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Medicine;
import model.Sale;
import model.SaleItem;
import ui.BillWindow;
import utils.Refreshable;
import utils.Session;
import utils.UiUtil;

/** Point of Sale: search, add to cart, remove, clear cart, checkout + bill. */
public class POSPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final SaleDAO saleDAO = new SaleDAO();

    private final JTextField txtSearch = new JTextField(22);
    private final DefaultTableModel medModel =
            UiUtil.readOnlyModel("ID", "Name", "Type", "Price", "In Stock", "Expiry");
    private final JTable medTable = new JTable(medModel);
    private final JSpinner spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private List<Medicine> found = new ArrayList<>();

    private final List<SaleItem> cart = new ArrayList<>();
    private final DefaultTableModel cartModel = UiUtil.readOnlyModel("Medicine", "Price", "Qty", "Subtotal");
    private final JTable cartTable = new JTable(cartModel);
    private final JLabel lblTotal = new JLabel("Total: R 0.00");

    public POSPanel() {
        setLayout(new GridLayout(1, 2, 10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildSearchSide());
        add(buildCartSide());
        refresh();
    }

    private JPanel buildSearchSide() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createTitledBorder("Find medicine"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Name:"));
        top.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        top.add(btnSearch);
        p.add(top, BorderLayout.NORTH);

        medTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medTable.setAutoCreateRowSorter(true);
        p.add(new JScrollPane(medTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JLabel("Quantity:"));
        bottom.add(spnQty);
        JButton btnAdd = new JButton("Add to cart");
        bottom.add(btnAdd);
        p.add(bottom, BorderLayout.SOUTH);

        btnSearch.addActionListener(e -> refresh());
        txtSearch.addActionListener(e -> refresh());
        btnAdd.addActionListener(e -> addToCart());
        medTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addToCart();
            }
        });
        return p;
    }

    private JPanel buildCartSide() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createTitledBorder("Cart"));
        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 20f));
        JButton btnRemove = new JButton("Remove selected");
        JButton btnClear = new JButton("Clear cart");
        JButton btnCheckout = new JButton("Checkout");
        btnCheckout.setFont(btnCheckout.getFont().deriveFont(Font.BOLD));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(btnRemove);
        buttons.add(btnClear);
        buttons.add(btnCheckout);
        JPanel south = new JPanel(new GridLayout(2, 1, 0, 8));
        south.add(lblTotal);
        south.add(buttons);
        p.add(south, BorderLayout.SOUTH);

        btnRemove.addActionListener(e -> removeSelected());
        btnClear.addActionListener(e -> clearCart(true));
        btnCheckout.addActionListener(e -> checkout());
        return p;
    }

    @Override
    public void refresh() {
        try {
            found = medicineDAO.search(txtSearch.getText());
            List<Object[]> rows = new ArrayList<>();
            for (Medicine m : found) {
                rows.add(new Object[]{m.getId(), m.getName(), m.getType(), m.getPrice(),
                        m.getQuantity(), m.getExpiryDate()});
            }
            UiUtil.fill(medModel, rows);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void addToCart() {
        int row = medTable.getSelectedRow();
        if (row < 0) {
            UiUtil.info(this, "Select a medicine first.");
            return;
        }
        Medicine m = found.get(medTable.convertRowIndexToModel(row));
        int qty = (Integer) spnQty.getValue();

        if (m.isExpired()) {
            UiUtil.error(this, m.getName() + " has expired and cannot be sold.");
            return;
        }
        SaleItem existing = null;
        for (SaleItem i : cart) if (i.getMedicineId() == m.getId()) existing = i;
        int already = existing == null ? 0 : existing.getQuantity();
        if (already + qty > m.getQuantity()) {
            UiUtil.error(this, "Only " + m.getQuantity() + " in stock for " + m.getName()
                    + (already > 0 ? " (" + already + " already in cart)." : "."));
            return;
        }
        if (existing != null) existing.setQuantity(already + qty);
        else cart.add(new SaleItem(m.getId(), m.getName(), qty, m.getPrice()));
        spnQty.setValue(1);
        refreshCart();
    }

    private void removeSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            UiUtil.info(this, "Select a cart line to remove.");
            return;
        }
        cart.remove(row);
        refreshCart();
    }

    private void clearCart(boolean ask) {
        if (cart.isEmpty()) return;
        if (ask && !UiUtil.confirm(this, "Clear all items from the cart?")) return;
        cart.clear();
        refreshCart();
    }

    private void refreshCart() {
        cartModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem i : cart) {
            cartModel.addRow(new Object[]{i.getMedicineName(), i.getPriceAtSale(), i.getQuantity(), i.getSubtotal()});
            total = total.add(i.getSubtotal());
        }
        lblTotal.setText("Total: " + UiUtil.money(total));
    }

    private void checkout() {
        if (cart.isEmpty()) {
            UiUtil.info(this, "The cart is empty.");
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem i : cart) total = total.add(i.getSubtotal());
        if (!UiUtil.confirm(this, "Complete sale for " + UiUtil.money(total) + "?")) return;

        Sale sale;
        try {
            sale = saleDAO.createSale(Session.userId(), Session.fullName(), new ArrayList<>(cart));
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            refresh();
            return;
        }
        new BillWindow(SwingUtilities.getWindowAncestor(this), sale).setVisible(true);
        cart.clear();
        refreshCart();
        refresh();
    }
}
