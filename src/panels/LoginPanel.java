package panels;

import dao.UserDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.function.Consumer;
import javax.swing.*;
import model.User;
import utils.UiUtil;

/** Login form. Calls onSuccess with the authenticated user. */
public class LoginPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final UserDAO userDAO = new UserDAO();
    private final Consumer<User> onSuccess;
    private final JTextField txtUser = new JTextField(18);
    private final JPasswordField txtPass = new JPasswordField(18);
    private final JLabel lblStatus = new JLabel(" ");
    private final JButton btnLogin = new JButton("Login");
    private int failedAttempts = 0;

    public LoginPanel(Consumer<User> onSuccess) {
        this.onSuccess = onSuccess;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("HealthFirst Pharmacy", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        JLabel sub = new JLabel("Inventory Management System", SwingConstants.CENTER);
        JPanel head = new JPanel(new GridLayout(2, 1));
        head.add(title);
        head.add(sub);
        add(head, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Username:"), g);
        g.gridx = 1; form.add(txtUser, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
        g.gridx = 1; form.add(txtPass, g);
        add(form, BorderLayout.CENTER);

        JButton btnExit = new JButton("Exit");
        lblStatus.setForeground(new Color(180, 30, 30));
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.add(btnLogin);
        buttons.add(btnExit);
        JPanel south = new JPanel(new GridLayout(2, 1, 0, 6));
        south.add(lblStatus);
        south.add(buttons);
        add(south, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> doLogin());
        btnExit.addActionListener(e -> System.exit(0));
    }

    public JButton getLoginButton() { return btnLogin; }

    private void doLogin() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Please enter username and password.");
            return;
        }
        try {
            User u = userDAO.authenticate(user, pass);
            if (u != null) {
                onSuccess.accept(u);
                return;
            }
            failedAttempts++;
            lblStatus.setText("Invalid username or password (attempt " + failedAttempts + ").");
            txtPass.setText("");
            txtPass.requestFocusInWindow();
            if (failedAttempts >= 5) {
                UiUtil.error(this, "Too many failed attempts. The application will close.");
                System.exit(1);
            }
        } catch (SQLException ex) {
            lblStatus.setText("Cannot connect to the database.");
            UiUtil.dbError(this, ex);
        }
    }
}
