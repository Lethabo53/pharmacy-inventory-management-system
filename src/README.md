# HealthFirst Pharmacy Inventory Management System (PIMS)

Java Swing + JDBC + MySQL desktop app with Admin and Cashier roles.

## Package structure

```
src/
  config/   DBConfig                      JDBC connection (reads db.properties)
  model/    User, Supplier, Medicine, Sale, SaleItem
  dao/      UserDAO, SupplierDAO, MedicineDAO, SaleDAO     all SQL lives here
  panels/   LoginPanel, AdminPanel, ManageMedicinePanel, ManageSuppliersPanel,
            ManageUsersPanel, ManageSalesPanel, Reports, POSPanel, StockCheckPanel
  ui/       Main, LoginFrame, AdminDashboard, CashierDashboard, BillWindow
  utils/    Session, SecurityUtil, UiUtil, CsvExporter, Refreshable
schema.sql       database + sample data
db.properties    connection settings
build.sh         builds dist/ and web/downloads/PIMS.zip
web/             download page for Apache
DEPLOY_APACHE.md Apache/XAMPP instructions
```

## Run from source

1. `mysql -u root -p < schema.sql`
2. Put the MySQL Connector/J jar in `lib/`
3. Edit `db.properties`
4. `./run.sh` (Windows: `run.bat`)  - JDK 11+

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

See `DEPLOY_APACHE.md` for putting it on Apache.
