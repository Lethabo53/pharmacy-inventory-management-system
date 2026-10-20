package ui;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import javax.swing.*;
import model.Sale;
import model.SaleItem;
import utils.UiUtil;

/** Bill window: shows the receipt and lets the user print or save it. */
public class BillWindow extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextArea area = new JTextArea();
    private final int saleId;

    public BillWindow(Window owner, Sale sale) {
        super(owner, "Bill #" + sale.getId(), ModalityType.APPLICATION_MODAL);
        this.saleId = sale.getId();
        setSize(480, 560);
        setLocationRelativeTo(owner);

        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setEditable(false);
        area.setText(buildText(sale));
        area.setCaretPosition(0);

        JButton btnPrint = new JButton("Print");
        JButton btnSave = new JButton("Save to file");
        JButton btnClose = new JButton("Close");
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        south.add(btnPrint);
        south.add(btnSave);
        south.add(btnClose);

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        btnPrint.addActionListener(e -> {
            try {
                area.print();
            } catch (Exception ex) {
                UiUtil.error(this, "Printing failed: " + ex.getMessage());
            }
        });
        btnSave.addActionListener(e -> save());
        btnClose.addActionListener(e -> dispose());
    }

    private static String buildText(Sale sale) {
        String sep = "-".repeat(44) + "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("        HEALTHFIRST PHARMACY\n");
        sb.append("        Thank you for your visit\n");
        sb.append(sep);
        sb.append(String.format("Bill No : %d%n", sale.getId()));
        sb.append(String.format("Date    : %s%n", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(sale.getSaleDate())));
        sb.append(String.format("Cashier : %s%n", sale.getCashierName()));
        sb.append(sep);
        sb.append(String.format("%-20s %4s %8s %9s%n", "Item", "Qty", "Price", "Total"));
        sb.append(sep);
        for (SaleItem i : sale.getItems()) {
            String n = i.getMedicineName();
            if (n.length() > 20) n = n.substring(0, 19) + ".";
            sb.append(String.format("%-20s %4d %8.2f %9.2f%n", n, i.getQuantity(), i.getPriceAtSale(), i.getSubtotal()));
        }
        sb.append(sep);
        sb.append(String.format("%-20s %23s%n", "TOTAL", UiUtil.money(sale.getTotal())));
        sb.append(sep);
        sb.append("Keep medicines out of reach of children.\n");
        return sb.toString();
    }

    private void save() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("bill_" + saleId + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter w = new FileWriter(fc.getSelectedFile())) {
            w.write(area.getText());
            UiUtil.info(this, "Bill saved to " + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            UiUtil.error(this, "Could not save bill: " + ex.getMessage());
        }
    }
}
