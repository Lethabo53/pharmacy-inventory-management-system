package dao;

import config.DBConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.User;
import utils.SecurityUtil;

public class UserDAO {

    /** Returns the matching user, or null if the credentials are wrong. */
    public User authenticate(String username, String password) throws SQLException {
        String sql = "SELECT user_id, username, role, full_name FROM users WHERE username=? AND password=?";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, SecurityUtil.hash(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<User> findCashiers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, username, role, full_name FROM users WHERE role='Cashier' ORDER BY username";
        try (Connection c = DBConfig.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Throws SQLIntegrityConstraintViolationException if the username already exists. */
    public void insertCashier(User u, String plainPassword) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?,?,'Cashier',?)";
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, SecurityUtil.hash(plainPassword));
            ps.setString(3, u.getFullName());
            ps.executeUpdate();
        }
    }

    public void updateFullName(int id, String fullName) throws SQLException {
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET full_name=? WHERE user_id=? AND role='Cashier'")) {
            ps.setString(1, fullName);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void resetPassword(int id, String plainPassword) throws SQLException {
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE users SET password=? WHERE user_id=? AND role='Cashier'")) {
            ps.setString(1, SecurityUtil.hash(plainPassword));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Throws SQLIntegrityConstraintViolationException if the cashier has sales. */
    public void deleteCashier(int id) throws SQLException {
        try (Connection c = DBConfig.getConnection(); PreparedStatement ps = c.prepareStatement(
                "DELETE FROM users WHERE user_id=? AND role='Cashier'")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"),
                rs.getString("role"), rs.getString("full_name"));
    }
}
