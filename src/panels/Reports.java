package panels;

import dao.MedicineDAO;
import dao.SaleDAO;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Medicine;
import utils.CsvExporter;
import utils.Refreshable;
import utils.UiUtil;

/** Reports: Sales (daily + top sellers), Expiry (next month), Low stock. */
public class Reports extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final SaleDAO saleDAO = new SaleDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    // sales
    private final JTextField txtFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
    private final JTextField txtTo = new JTextField(LocalDate.now().toString(), 10);
    private final DefaultTableModel dailyModel = UiUtil.readOnlyModel("Date", "Sales", "Revenue (R)");
    private final DefaultTableModel topModel = UiUtil.readOnlyModel("Medicine", "Units sold", "Revenue (R)");
    private final JTable dailyTable = new JTable(dailyModel);
    private final JLabel lblSales = new JLabel(" ");

    // expiry
    private final DefaultTableModel expiryModel = UiUtil.readOnlyModel(
            "ID", "Medicine", "Company", "In Stock", "Expiry", "Days left", "Status", "Supplier");
    private final JTable expiryTable = new JTable(expiryModel);
    private final JLabel lblExpiry = new JLabel(" ");

    // low stock
    private final DefaultTableModel lowModel = UiUtil.readOnlyModel(
            "ID", "Medicine", "Company", "In Stock", "Reorder Lvl", "Supplier");
    private final JTable lowTable = new JTable(lowModel);
    private final JLabel lblLow = new JLabel(" ");

    public Reports() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.LEFT);
        tabs.addTab("Sales Report", buildSalesTab());
        tabs.addTab("Expiry Report", buildListTab("Medicines already expired or expiring within the next month:",
                expiryTable, lblExpiry, "expiry_report.csv", this::loadExpiry));
        tabs.addTab("Low Stock", buildListTab("Medicines at or below their reorder level:",
                lowTable, lblLow, "low_stock_report.csv", this::loadLow));
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        loadSales();
        loadExpiry();
        loadLow();
    }

    // ------------------------------------------------------------------ sales
    private JPanel buildSalesTab() {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("From (yyyy-MM-dd):"));
        top.add(txtFrom);
        top.add(new JLabel("To:"));
        top.add(txtTo);
        JButton btnGo = new JButton("Generate");
        JButton btnCsv = new JButton("Export daily CSV");
        top.add(btnGo); top.add(btnCsv);
        p.add(top, BorderLayout.NORTH);

        JScrollPane daily = new JScrollPane(dailyTable);
        daily.setBorder(BorderFactory.createTitledBorder("Sales per day"));
        JScrollPane topSellers = new JScrollPane(new JTable(topModel));
        topSellers.setBorder(BorderFactory.createTitledBorder("Top 10 medicines"));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, daily, topSellers);
        split.setResizeWeight(0.5);
        p.add(split, BorderLayout.CENTER);

        lblSales.setFont(lblSales.getFont().deriveFont(Font.BOLD, 14f));
        p.add(lblSales, BorderLayout.SOUTH);

        btnGo.addActionListener(e -> loadSales());
        btnCsv.addActionListener(e -> CsvExporter.export(this, dailyTable, "sales_report.csv"));
        return p;
    }

    private void loadSales() {
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
            List<Object[]> daily = saleDAO.dailySummary(from, to);
            UiUtil.fill(dailyModel, daily);
            UiUtil.fill(topModel, saleDAO.topSellers(from, to));

            BigDecimal revenue = BigDecimal.ZERO;
            long count = 0;
            for (Object[] r : daily) {
                count += ((Number) r[1]).longValue();
                revenue = revenue.add(new BigDecimal(String.valueOf(r[2])));
            }
            BigDecimal avg = count == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
            lblSales.setText(count + " sale(s)   |   Revenue: " + UiUtil.money(revenue)
                    + "   |   Average sale: " + UiUtil.money(avg));
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    // ------------------------------------------------------- expiry / low stock
    private JPanel buildListTab(String heading, JTable table, JLabel summary, String csvName, Runnable reload) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel(heading));
        JButton btn = new JButton("Refresh");
        JButton btnCsv = new JButton("Export CSV");
        top.add(btn); top.add(btnCsv);
        p.add(top, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        summary.setFont(summary.getFont().deriveFont(Font.BOLD, 14f));
        p.add(summary, BorderLayout.SOUTH);
        btn.addActionListener(e -> reload.run());
        btnCsv.addActionListener(e -> CsvExporter.export(this, table, csvName));
        return p;
    }

    private void loadExpiry() {
        try {
            List<Object[]> rows = new ArrayList<>();
            int expired = 0;
            for (Medicine m : medicineDAO.findExpiringWithinMonth()) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), m.getExpiryDate());
                boolean isExpired = m.isExpired();
                if (isExpired) expired++;
                rows.add(new Object[]{m.getId(), m.getName(), m.getCompany(), m.getQuantity(),
                        m.getExpiryDate(), days, isExpired ? "EXPIRED" : "Expiring soon", m.getSupplierName()});
            }
            UiUtil.fill(expiryModel, rows);
            lblExpiry.setText(rows.size() + " item(s) need attention (" + expired + " already expired)");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }

    private void loadLow() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Medicine m : medicineDAO.findLowStock()) {
                rows.add(new Object[]{m.getId(), m.getName(), m.getCompany(), m.getQuantity(),
                        m.getReorderLevel(), m.getSupplierName()});
            }
            UiUtil.fill(lowModel, rows);
            lblLow.setText(rows.size() + " item(s) to reorder");
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
