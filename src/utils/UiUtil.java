package utils;

import java.awt.Component;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/** Small Swing helpers: dialogs, table models, money formatting. */
public final class UiUtil {
    private UiUtil() {}

    public static void error(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "PIMS", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void dbError(Component parent, SQLException ex) {
        error(parent, "Database error: " + ex.getMessage());
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    public static void fill(DefaultTableModel model, List<Object[]> rows) {
        model.setRowCount(0);
        for (Object[] r : rows) model.addRow(r);
    }

    public static String money(BigDecimal v) {
        return "R " + String.format(Locale.US, "%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    public static String nz(Object o) {
        return o == null ? "" : o.toString();
    }
}
