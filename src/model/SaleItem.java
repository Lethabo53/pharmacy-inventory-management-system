package model;

import java.math.BigDecimal;

public class SaleItem {
    private int id;
    private int saleId;
    private int medicineId;
    private String medicineName;
    private int quantity;
    private BigDecimal priceAtSale = BigDecimal.ZERO;

    public SaleItem() {}

    public SaleItem(int medicineId, String medicineName, int quantity, BigDecimal priceAtSale) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtSale() { return priceAtSale; }
    public void setPriceAtSale(BigDecimal priceAtSale) { this.priceAtSale = priceAtSale; }

    public BigDecimal getSubtotal() {
        return priceAtSale.multiply(BigDecimal.valueOf(quantity));
    }
}
