package panels;

import dao.MedicineDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Medicine;
import utils.Refreshable;
import utils.UiUtil;

/** Read-only price / availability lookup for cashiers. */
public class StockCheckPanel extends JPanel implements Refreshable {
    private static final long serialVersionUID = 1L;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final JTextField txtSearch = new JTextField(25);
    private final DefaultTableModel model =
            UiUtil.readOnlyModel("ID", "Name", "Company", "Type", "Price", "In Stock", "Expiry");

    public StockCheckPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Medicine name:"));
        top.add(txtSearch);
        JButton btn = new JButton("Search");
        top.add(btn);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btn.addActionListener(e -> refresh());
        txtSearch.addActionListener(e -> refresh());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            List<Object[]> rows = new ArrayList<>();
            for (Medicine m : medicineDAO.search(txtSearch.getText())) {
                rows.add(new Object[]{m.getId(), m.getName(), m.getCompany(), m.getType(),
                        m.getPrice(), m.getQuantity(), m.getExpiryDate()});
            }
            UiUtil.fill(model, rows);
        } catch (SQLException ex) {
            UiUtil.dbError(this, ex);
        }
    }
}
