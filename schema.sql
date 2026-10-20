-- ============================================================
-- HealthFirst Pharmacy Inventory Management System (PIMS)
-- MySQL schema + sample data
-- Run:  mysql -u root -p < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS pims_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pims_db;

DROP TABLE IF EXISTS sale_items;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS users;

-- Table 1: users -------------------------------------------------
CREATE TABLE users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,           -- SHA-256 hex digest
    role      ENUM('Admin','Cashier') NOT NULL,
    full_name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- Table 2: suppliers ---------------------------------------------
CREATE TABLE suppliers (
    supplier_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone          VARCHAR(20),
    email          VARCHAR(100),
    address        TEXT
) ENGINE=InnoDB;

-- Table 3: medicines ---------------------------------------------
CREATE TABLE medicines (
    medicine_id       INT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    company           VARCHAR(100),
    medicine_type     VARCHAR(50),             -- Tablet, Capsule, Syrup, Injection, Cream
    price             DECIMAL(10,2) NOT NULL,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    reorder_level     INT NOT NULL DEFAULT 10,
    expiry_date       DATE,
    supplier_id       INT,
    CONSTRAINT fk_med_supplier FOREIGN KEY (supplier_id)
        REFERENCES suppliers(supplier_id) ON DELETE SET NULL,
    CONSTRAINT chk_price CHECK (price >= 0),
    CONSTRAINT chk_qty   CHECK (quantity_in_stock >= 0)
) ENGINE=InnoDB;

-- Table 4: sales -------------------------------------------------
CREATE TABLE sales (
    sale_id      INT AUTO_INCREMENT PRIMARY KEY,
    sale_date    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL,
    user_id      INT NOT NULL,
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Table 5: sale_items --------------------------------------------
CREATE TABLE sale_items (
    sale_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    sale_id       INT NOT NULL,
    medicine_id   INT NOT NULL,
    quantity_sold INT NOT NULL,
    price_at_sale DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_si_sale FOREIGN KEY (sale_id)     REFERENCES sales(sale_id) ON DELETE CASCADE,
    CONSTRAINT fk_si_med  FOREIGN KEY (medicine_id) REFERENCES medicines(medicine_id)
) ENGINE=InnoDB;

-- ============================================================
-- Sample data
-- Default logins:  admin / admin123   and   cashier1 / cashier123
-- ============================================================
INSERT INTO users (username, password, role, full_name) VALUES
 ('admin',    SHA2('admin123',256),   'Admin',   'Pharmacy Owner'),
 ('cashier1', SHA2('cashier123',256), 'Cashier', 'Thandi Mokoena');

INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
 ('MediSupply Co',  'John Dlamini',  '011 555 0101', 'orders@medisupply.example', '12 Main Rd, Johannesburg'),
 ('PharmaDirect',   'Sarah Naidoo',  '011 555 0102', 'sales@pharmadirect.example', '45 Oak Ave, Sandton'),
 ('CareWell Distributors', 'Peter Khumalo', '011 555 0103', 'info@carewell.example', '8 Park Lane, Pretoria');

INSERT INTO medicines (name, company, medicine_type, price, quantity_in_stock, reorder_level, expiry_date, supplier_id) VALUES
 ('Paracetamol 500mg',   'Panado',   'Tablet',    35.00, 200, 50, DATE_ADD(CURDATE(), INTERVAL 18 MONTH), 1),
 ('Ibuprofen 400mg',     'Advil',    'Tablet',    52.50, 120, 40, DATE_ADD(CURDATE(), INTERVAL 12 MONTH), 1),
 ('Amoxicillin 250mg',   'Cipla',    'Capsule',   89.90,  60, 30, DATE_ADD(CURDATE(), INTERVAL 20 DAY),   2),
 ('Cough Syrup 100ml',   'Benylin',  'Syrup',     64.00,   8, 15, DATE_ADD(CURDATE(), INTERVAL 10 DAY),   2),
 ('Insulin Pen',         'Novo',     'Injection', 310.00, 25, 10, DATE_ADD(CURDATE(), INTERVAL 6 MONTH),  3),
 ('Hydrocortisone Cream','Dermovate','Cream',     78.25,  40, 15, DATE_ADD(CURDATE(), INTERVAL 24 MONTH), 3),
 ('Vitamin C 1000mg',    'Cipla',    'Tablet',    95.00,  90, 25, DATE_SUB(CURDATE(), INTERVAL 5 DAY),    1);
