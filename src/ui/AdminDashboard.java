package ui;

import java.awt.BorderLayout;
import javax.swing.*;
import panels.AdminPanel;
import utils.Session;

public class AdminDashboard extends JFrame {
    private static final long serialVersionUID = 1L;

    public AdminDashboard() {
        super("HealthFirst Pharmacy - Administrator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 700);
        setLocationRelativeTo(null);

        JLabel who = new JLabel("  Logged in as: " + Session.fullName() + " (Admin)");
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
        add(new AdminPanel(), BorderLayout.CENTER);
    }
}
