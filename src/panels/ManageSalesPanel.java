package panels;

import dao.SaleDAO;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Sale;
import model.SaleItem;
import ui.BillWindow;
import utils.CsvExporter;
import utils.Refreshable;
import utils.UiUtil;

/** Admin view of all transactions: filter by date, see items, re-open the bill, export. */
public class ManageSalesPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final SaleDAO saleDAO = new SaleDAO();
    private final JTextField txtFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
    private final JTextField txtTo = new JTextField(LocalDate.now().toString(), 10);

    private final DefaultTableModel salesModel =
            UiUtil.readOnlyModel("Sale #", "Date", "Cashier", "Items sold", "Total (R)");
    private final JTable salesTable = new JTable(salesModel);
    private final DefaultTableModel itemModel =
            UiUtil.readOnlyModel("Medicine", "Qty", "Price (R)", "Subtotal (R)");
    private final JLabel lblSummary = new JLabel(" ");
    private List<Sale> current = new ArrayList<>();

    public ManageSalesPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("From (yyyy-MM-dd):"));
        top.add(txtFrom);
        top.add(new JLabel("To:"));
        top.add(txtTo);
        JButton btnGo = new JButton("Show");
        JButton btnToday = new JButton("Today");
        JButton btnBill = new JButton("View bill");
        JButton btnCsv = new JButton("Export CSV");
        top.add(btnGo); top.add(btnToday); top.add(btnBill); top.add(btnCsv);
        add(top, BorderLayout.NORTH);

        salesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        salesTable.setAutoCreateRowSorter(true);
        JScrollPane lower = new JScrollPane(new JTable(itemModel));
        lower.setBorder(BorderFactory.createTitledBorder("Items in selected sale"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(salesTable), lower);
        split.setResizeWeight(0.6);
        add(split, BorderLayout.CENTER);

        lblSummary.setFont(lblSummary.getFont().deriveFont(Font.BOLD, 14f));
        add(lblSummary, BorderLayout.SOUTH);

        btnGo.addActionListener(e -> refresh());
        btnToday.addActionListener(e -> {
            txtFrom.setText(LocalDate.now().toString());
            txtTo.setText(LocalDate.now().toString());
            refresh();
        });
        btnBill.addActionListener(e -> viewBill());
        btnCsv.addActionListener(e -> CsvExporter.export(this, salesTable, "sales.csv"));
        salesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadItems();
        });
        refresh();
    }

    @Override
    public void refresh() {
        LocalDate from, to;
        try {
            from = LocalDate.parse(txtFrom.getText().trim());
            to = LocalDate.parse(txtTo.getText().trim());
        } catch (DateTimeParseException ex) {
            UiUtil.error(this, "Dates must be in the format yyyy-MM-dd.");
            return;
        }
        if (to.isBefore(from)) {
            UiUtil.error(this, "The 'To' date must not be before the 'From' date.");
            return;
        }
        try {
            current = saleDAO.findByDateRange(from, to);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        java.math.BigDecimal revenue = java.math.BigDecimal.ZERO;
        for (Sale s : current) {
            rows.add(new Object[]{s.getId(), s.getSaleDate(), s.getCashierName(), s.getItemCount(), s.getTotal()});
            revenue = revenue.add(s.getTotal());
        }
        UiUtil.fill(salesModel, rows);
        itemModel.setRowCount(0);
        lblSummary.setText(current.size() + " sale(s)   |   Revenue: " + UiUtil.money(revenue));
    }

    private Sale selectedSale() {
        int row = salesTable.getSelectedRow();
        return row < 0 ? null : current.get(salesTable.convertRowIndexToModel(row));
    }

    private void loadItems() {
        Sale s = selectedSale();
        if (s == null) return;
        try {
            List<Object[]> rows = new ArrayList<>();
            for (SaleItem i : saleDAO.findItems(s.getId())) {
                rows.add(new Object[]{i.getMedicineName(), i.getQuantity(), i.getPriceAtSale(), i.getSubtotal()});
            }
            UiUtil.fill(itemModel, rows);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void viewBill() {
        Sale s = selectedSale();
        if (s == null) { UiUtil.info(this, "Select a sale first."); return; }
        try {
            s.setItems(saleDAO.findItems(s.getId()));
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
            return;
        }
        new BillWindow(SwingUtilities.getWindowAncestor(this), s).setVisible(true);
    }
}
