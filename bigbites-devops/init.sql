-- BigBites DB init – runs only on first container start
CREATE DATABASE IF NOT EXISTS BigBites;
USE BigBites;

-- Spring JPA (ddl-auto: update) will create the tables automatically.
-- This file is a placeholder for any seed data or manual DDL you need.

-- Example: ensure the admin user row exists so LOW_STOCK_ALERT notifications work
-- INSERT IGNORE INTO users (id, email) VALUES (1, 'admin@bigbites.com');
