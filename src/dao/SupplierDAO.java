package dao;

import config.DBConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Supplier;

public class SupplierDAO {

    public List<Supplier> findAll() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT supplier_id, name, contact_person, phone, email, address FROM suppliers ORDER BY name";
        try (Connection c = DBConfig.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Supplier s = new Supplier(rs.getInt("supplier_id"), rs.getString("name"));
                s.setContactPerson(rs.getString("contact_person"));
                s.setPhone(rs.getString("phone"));
                s.setEmail(rs.getString("email"));
                s.setAddress(rs.getString("address"));
                list.add(s);
            }
        }
        return list;
    }

    public void insert(Supplier s) throws SQLException {
        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?,?,?,?,?)";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, s);
            ps.executeUpdate();
        }
    }

    public void update(Supplier s) throws SQLException {
        String sql = "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE supplier_id=?";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, s);
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DBConfig.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM suppliers WHERE supplier_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void bind(PreparedStatement ps, Supplier s) throws SQLException {
        ps.setString(1, s.getName());
        ps.setString(2, s.getContactPerson());
        ps.setString(3, s.getPhone());
        ps.setString(4, s.getEmail());
        ps.setString(5, s.getAddress());
    }
}
