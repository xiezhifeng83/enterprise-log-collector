@echo off
echo ========================================
echo Enterprise Log Collector - Startup Script
echo ========================================
echo.

echo Step 1: Setting up MySQL database...
echo Please run the following SQL commands in MySQL:
echo.
echo CREATE DATABASE IF NOT EXISTS logdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
echo CREATE USER IF NOT EXISTS 'loguser'@'localhost' IDENTIFIED BY 'logpass';
echo GRANT ALL PRIVILEGES ON logdb.* TO 'loguser'@'localhost';
echo FLUSH PRIVILEGES;
echo.
echo Press any key after you have created the database, or press Ctrl+C to exit...
pause

echo.
echo Step 2: Starting Spring Boot application...
cd /d "%~dp0"
call mvn spring-boot:run -Dspring-boot.run.profiles=dev

pause
