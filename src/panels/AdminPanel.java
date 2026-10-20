package panels;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import utils.Refreshable;

/** The administrator's main content: one tab per management area. */
public class AdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    public AdminPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Manage Medicines", new ManageMedicinePanel());
        tabs.addTab("Manage Suppliers", new ManageSuppliersPanel());
        tabs.addTab("Manage Users", new ManageUsersPanel());
        tabs.addTab("Sales", new ManageSalesPanel());
        tabs.addTab("Reports", new Reports());

        // reload a tab's data whenever it is opened
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });
        add(tabs, BorderLayout.CENTER);
    }
}
