package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.*;
import panels.POSPanel;
import panels.StockCheckPanel;
import utils.Refreshable;
import utils.Session;

public class CashierDashboard extends JFrame {
    private static final long serialVersionUID = 1L;

    public CashierDashboard() {
        super("HealthFirst Pharmacy - Cashier");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Point of Sale", new POSPanel());
        tabs.addTab("Stock Check", new StockCheckPanel());
        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c instanceof Refreshable) ((Refreshable) c).refresh();
        });

        JLabel who = new JLabel("  Logged in as: " + Session.fullName() + " (Cashier)");
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            Session.end();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 8));
        top.add(who, BorderLayout.WEST);
        top.add(logout, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
