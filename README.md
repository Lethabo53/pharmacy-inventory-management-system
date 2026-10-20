# HealthFirst Pharmacy Inventory Management System (PIMS)

Java Swing + JDBC + MySQL desktop app with Admin and Cashier roles.

## Package structure

```
src/
  config/   DBConfig
  model/    User, Supplier, Medicine, Sale, SaleItem
  dao/      UserDAO, SupplierDAO, MedicineDAO, SaleDAO
  panels/   LoginPanel, AdminPanel, ManageMedicinePanel, ManageSuppliersPanel,
            ManageUsersPanel, ManageSalesPanel, Reports, POSPanel, StockCheckPanel
  ui/       Main, LoginFrame, AdminDashboard, CashierDashboard, BillWindow
  utils/    Session, SecurityUtil, UiUtil, CsvExporter, Refreshable
schema.sql  database and sample data
```

## Run from source (NetBeans + XAMPP)

1. Install **XAMPP**, open its Control Panel and start **Apache** and **MySQL**.
2. Open `http://localhost/phpmyadmin`, click **Import**, choose `schema.sql` and click **Import**. This creates the `pims_db` database.
3. Download **MySQL Connector/J** (the `.jar` file) from dev.mysql.com.
4. Open the project in **NetBeans**, right-click **Libraries**, choose **Add JAR/Folder...** and select the connector jar.
5. In the project root (next to `src`) create a file named `db.properties`:
```
   db.url=jdbc:mysql://localhost:3306/pims_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=
```
   XAMPP's default is user `root` with an empty password. This file is not in the repository, so each person creates their own.
6. Set the main class to `ui.Main` (**Project Properties, Run**) and press **F6**.

## Default logins

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Cashier | `cashier1` | `cashier123` |

## Features

- **Login**: role-based redirect; error on failed attempts; closes after 5.
- **Cashier**: POS (search, cart, remove, clear, transactional checkout), bill window (print/save), read-only stock check.
- **Admin**: medicines CRUD (expired = red, low stock = orange), suppliers CRUD, cashier accounts, sales history with bill reprint,
  reports (sales per day + top 10, expiry within 1 month, low stock) with CSV export.
