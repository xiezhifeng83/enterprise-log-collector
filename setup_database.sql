-- Create database
CREATE DATABASE IF NOT EXISTS logdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user and grant privileges
CREATE USER IF NOT EXISTS 'loguser'@'localhost' IDENTIFIED BY 'logpass';
GRANT ALL PRIVILEGES ON logdb.* TO 'loguser'@'localhost';
FLUSH PRIVILEGES;

-- Select database
USE logdb;

-- Show database info
SELECT 'Database logdb created successfully' AS status;
