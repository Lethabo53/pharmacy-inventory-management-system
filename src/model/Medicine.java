package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Medicine {
    private int id;
    private String name;
    private String company;
    private String type;
    private BigDecimal price = BigDecimal.ZERO;
    private int quantity;
    private int reorderLevel = 10;
    private LocalDate expiryDate;     // may be null
    private int supplierId;           // 0 = none
    private String supplierName;      // filled by joins

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }
}
