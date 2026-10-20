package dao;

import config.DBConfig;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Sale;
import model.SaleItem;

public class SaleDAO {

    /**
     * Saves a sale, its line items and the stock deduction in ONE transaction.
     * Rolls back (and throws) if any item is out of stock or expired.
     */
    public Sale createSale(int userId, String cashierName, List<SaleItem> items) throws SQLException {
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem i : items) total = total.add(i.getSubtotal());

        try (Connection c = DBConfig.getConnection()) {
            c.setAutoCommit(false);
            try {
                int saleId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO sales (total_amount, user_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setBigDecimal(1, total);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) {
                        k.next();
                        saleId = k.getInt(1);
                    }
                }
                String upd = "UPDATE medicines SET quantity_in_stock = quantity_in_stock - ? "
                        + "WHERE medicine_id = ? AND quantity_in_stock >= ? "
                        + "AND (expiry_date IS NULL OR expiry_date >= CURDATE())";
                String ins = "INSERT INTO sale_items (sale_id, medicine_id, quantity_sold, price_at_sale) VALUES (?,?,?,?)";
                try (PreparedStatement pu = c.prepareStatement(upd); PreparedStatement pi = c.prepareStatement(ins)) {
                    for (SaleItem i : items) {
                        pu.setInt(1, i.getQuantity());
                        pu.setInt(2, i.getMedicineId());
                        pu.setInt(3, i.getQuantity());
                        if (pu.executeUpdate() == 0) {
                            throw new SQLException("Not enough valid stock for " + i.getMedicineName()
                                    + " (it may have just sold out or expired).");
                        }
                        pi.setInt(1, saleId);
                        pi.setInt(2, i.getMedicineId());
                        pi.setInt(3, i.getQuantity());
                        pi.setBigDecimal(4, i.getPriceAtSale());
                        pi.executeUpdate();
                        i.setSaleId(saleId);
                    }
                }
                c.commit();

                Sale s = new Sale();
                s.setId(saleId);
                s.setSaleDate(new Timestamp(System.currentTimeMillis()));
                s.setTotal(total);
                s.setUserId(userId);
                s.setCashierName(cashierName);
                s.setItems(new ArrayList<>(items));
                s.setItemCount(items.stream().mapToInt(SaleItem::getQuantity).sum());
                return s;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    /** Sales whose date falls in [from, to], newest first. Items are not loaded. */
    public List<Sale> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT s.sale_id, s.sale_date, s.total_amount, s.user_id, u.full_name, "
                + "COALESCE(SUM(si.quantity_sold),0) AS items "
                + "FROM sales s JOIN users u ON u.user_id = s.user_id "
                + "LEFT JOIN sale_items si ON si.sale_id = s.sale_id "
                + "WHERE DATE(s.sale_date) BETWEEN ? AND ? "
                + "GROUP BY s.sale_id, s.sale_date, s.total_amount, s.user_id, u.full_name "
                + "ORDER BY s.sale_date DESC";
        List<Sale> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("sale_id"));
                    s.setSaleDate(rs.getTimestamp("sale_date"));
                    s.setTotal(rs.getBigDecimal("total_amount"));
                    s.setUserId(rs.getInt("user_id"));
                    s.setCashierName(rs.getString("full_name"));
                    s.setItemCount(rs.getInt("items"));
                    out.add(s);
                }
            }
        }
        return out;
    }

    public List<SaleItem> findItems(int saleId) throws SQLException {
        String sql = "SELECT si.sale_item_id, si.medicine_id, m.name, si.quantity_sold, si.price_at_sale "
                + "FROM sale_items si JOIN medicines m ON m.medicine_id = si.medicine_id "
                + "WHERE si.sale_id = ? ORDER BY si.sale_item_id";
        List<SaleItem> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleItem i = new SaleItem(rs.getInt("medicine_id"), rs.getString("name"),
                            rs.getInt("quantity_sold"), rs.getBigDecimal("price_at_sale"));
                    i.setId(rs.getInt("sale_item_id"));
                    i.setSaleId(saleId);
                    out.add(i);
                }
            }
        }
        return out;
    }

    /** Rows: {date, number of sales, revenue}. */
    public List<Object[]> dailySummary(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT DATE(sale_date) AS d, COUNT(*) AS n, SUM(total_amount) AS revenue FROM sales "
                + "WHERE DATE(sale_date) BETWEEN ? AND ? GROUP BY DATE(sale_date) ORDER BY d";
        return rows(sql, from, to, 3);
    }

    /** Rows: {medicine, units sold, revenue}, best sellers first (top 10). */
    public List<Object[]> topSellers(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT m.name, SUM(si.quantity_sold) AS units, SUM(si.quantity_sold * si.price_at_sale) AS revenue "
                + "FROM sale_items si JOIN sales s ON s.sale_id = si.sale_id "
                + "JOIN medicines m ON m.medicine_id = si.medicine_id "
                + "WHERE DATE(s.sale_date) BETWEEN ? AND ? "
                + "GROUP BY m.medicine_id, m.name ORDER BY units DESC LIMIT 10";
        return rows(sql, from, to, 3);
    }

    private List<Object[]> rows(String sql, LocalDate from, LocalDate to, int cols) throws SQLException {
        List<Object[]> out = new ArrayList<>();
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] r = new Object[cols];
                    for (int i = 0; i < cols; i++) r[i] = rs.getObject(i + 1);
                    out.add(r);
                }
            }
        }
        return out;
    }
}
