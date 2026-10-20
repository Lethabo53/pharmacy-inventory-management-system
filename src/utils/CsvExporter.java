package utils;

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JTable;

/** Exports the visible contents of a JTable to a CSV file. */
public final class CsvExporter {
    private CsvExporter() {}

    public static void export(Component parent, JTable table, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(defaultName));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        StringBuilder sb = new StringBuilder();
        int cols = table.getColumnCount();
        for (int c = 0; c < cols; c++) {
            if (c > 0) sb.append(',');
            sb.append(quote(table.getColumnName(c)));
        }
        sb.append('\n');
        for (int r = 0; r < table.getRowCount(); r++) {
            for (int c = 0; c < cols; c++) {
                if (c > 0) sb.append(',');
                sb.append(quote(UiUtil.nz(table.getValueAt(r, c))));
            }
            sb.append('\n');
        }
        try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
            w.write(sb.toString());
            UiUtil.info(parent, "Exported to " + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            UiUtil.error(parent, "Export failed: " + ex.getMessage());
        }
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
