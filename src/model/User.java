package model;

public class User {
    private int id;
    private String username;
    private String role;      // "Admin" or "Cashier"
    private String fullName;

    public User() {}

    public User(int id, String username, String role, String fullName) {
        this.id = id; this.username = username; this.role = role; this.fullName = fullName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isAdmin() { return "Admin".equals(role); }
}
