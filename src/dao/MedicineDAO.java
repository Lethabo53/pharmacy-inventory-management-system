package dao;

import config.DBConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Medicine;

public class MedicineDAO {

    private static final String SELECT =
            "SELECT m.medicine_id, m.name, m.company, m.medicine_type, m.price, m.quantity_in_stock, "
          + "m.reorder_level, m.expiry_date, m.supplier_id, s.name AS supplier_name "
          + "FROM medicines m LEFT JOIN suppliers s ON s.supplier_id = m.supplier_id ";

    /** Search by medicine name or company (empty text returns everything). */
    public List<Medicine> search(String text) throws SQLException {
        String like = "%" + (text == null ? "" : text.trim()) + "%";
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE m.name LIKE ? OR m.company LIKE ? ORDER BY m.name")) {
            ps.setString(1, like);
            ps.setString(2, like);
            return list(ps);
        }
    }

    /** Medicines already expired or expiring within the next month. */
    public List<Medicine> findExpiringWithinMonth() throws SQLException {
        String sql = SELECT + "WHERE m.expiry_date IS NOT NULL "
                + "AND m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 1 MONTH) ORDER BY m.expiry_date";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            return list(ps);
        }
    }

    public List<Medicine> findLowStock() throws SQLException {
        String sql = SELECT + "WHERE m.quantity_in_stock <= m.reorder_level ORDER BY m.quantity_in_stock";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            return list(ps);
        }
    }

    public void insert(Medicine m) throws SQLException {
        String sql = "INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, "
                + "reorder_level, expiry_date, supplier_id) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m);
            ps.executeUpdate();
        }
    }

    public void update(Medicine m) throws SQLException {
        String sql = "UPDATE medicines SET name=?, company=?, medicine_type=?, price=?, quantity_in_stock=?, "
                + "reorder_level=?, expiry_date=?, supplier_id=? WHERE medicine_id=?";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, m);
            ps.setInt(9, m.getId());
            ps.executeUpdate();
        }
    }

    /** Throws SQLIntegrityConstraintViolationException if the medicine appears in past sales. */
    public void delete(int id) throws SQLException {
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM medicines WHERE medicine_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------- helpers
    private void bind(PreparedStatement ps, Medicine m) throws SQLException {
        ps.setString(1, m.getName());
        ps.setString(2, m.getCompany());
        ps.setString(3, m.getType());
        ps.setBigDecimal(4, m.getPrice());
        ps.setInt(5, m.getQuantity());
        ps.setInt(6, m.getReorderLevel());
        if (m.getExpiryDate() == null) ps.setNull(7, Types.DATE);
        else ps.setDate(7, Date.valueOf(m.getExpiryDate()));
        if (m.getSupplierId() == 0) ps.setNull(8, Types.INTEGER);
        else ps.setInt(8, m.getSupplierId());
    }

    private List<Medicine> list(PreparedStatement ps) throws SQLException {
        List<Medicine> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Medicine m = new Medicine();
                m.setId(rs.getInt("medicine_id"));
                m.setName(rs.getString("name"));
                m.setCompany(rs.getString("company"));
                m.setType(rs.getString("medicine_type"));
                m.setPrice(rs.getBigDecimal("price"));
                m.setQuantity(rs.getInt("quantity_in_stock"));
                m.setReorderLevel(rs.getInt("reorder_level"));
                Date d = rs.getDate("expiry_date");
                m.setExpiryDate(d == null ? null : d.toLocalDate());
                m.setSupplierId(rs.getInt("supplier_id"));   // 0 when NULL
                m.setSupplierName(rs.getString("supplier_name"));
                out.add(m);
            }
        }
        return out;
    }
}
