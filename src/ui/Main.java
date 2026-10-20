package ui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // default look and feel
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
