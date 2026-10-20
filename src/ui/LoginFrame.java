package ui;

import javax.swing.JFrame;
import model.User;
import panels.LoginPanel;
import utils.Session;

/** Login window. Redirects to the Admin or Cashier dashboard by role. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    public LoginFrame() {
        super("HealthFirst Pharmacy - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        LoginPanel panel = new LoginPanel(this::onLogin);
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.getLoginButton());
        setSize(420, 320);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void onLogin(User user) {
        Session.start(user);
        dispose();
        if (user.isAdmin()) new AdminDashboard().setVisible(true);
        else new CashierDashboard().setVisible(true);
    }
}
