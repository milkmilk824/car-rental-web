# DrivePilot Car Rental — Database Guide

## Overview

| Profile | Database | DDL Mode | Use Case |
|---------|----------|----------|----------|
| `dev` (default) | MySQL 8 | `update` | Local development with persistent data |
| `mysql` | MySQL 8 | `validate` | Pre-production / manual schema |
| `test` | H2 in-memory | `update` | `mvn test` |

## Quick Start — MySQL 8 (Dev)

### 1. Install MySQL 8

The local machine already has MySQL 8, so the backend is configured to use MySQL by default. If you need to install it on another machine, use one of these options:

- **Windows**: [MySQL Installer](https://dev.mysql.com/downloads/installer/) or `winget install Oracle.MySQL`
- **Docker**:
  ```bash
  docker run -d --name mysql8 \
    -e MYSQL_ROOT_PASSWORD=root \
    -p 3306:3306 \
    mysql:8.0
  ```
- **macOS**: `brew install mysql@8.0`

### 2. Create Database

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS drivepilot_car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Or use the bundled SQL script:

```bash
mysql -u root -p < src/main/resources/db/mysql/schema.sql
```

### 3. Configure Connection (Optional)

Environment variables override defaults:

| Variable | Default | Description |
|----------|---------|-------------|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/drivepilot_car_rental?...` | Full JDBC URL |
| `MYSQL_USER` | `root` | Database user |
| `MYSQL_PASSWORD` | empty | Database password; set it in your shell before starting the backend |
| `PAYMENT_CALLBACK_SECRET` | `change-me-local-secret` | Payment callback signing secret; override in production |

### 4. Start Backend

**Default profile (dev → MySQL)**:

```powershell
cd E:\workspace\car-rental-backend
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
```

**MySQL validate profile** (requires pre-created schema):

```powershell
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

**With custom connection**:

```powershell
$env:MYSQL_USER="myuser"; $env:MYSQL_PASSWORD="mypass"; mvn spring-boot:run
```

### 5. Verify

```bash
curl http://localhost:8080/api/cars
```

No demo account is inserted by default in MySQL. Create real users through the application or insert operational admin/staff accounts with your own approved password hash.

---

## H2 Test Environment

Tests automatically use H2 (no MySQL needed):

```powershell
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

Test profile config: `src/test/resources/application-test.yml`

---

## SQL Files Reference

| File | Purpose |
|------|---------|
| `src/main/resources/db/mysql/schema.sql` | Full DDL with `drivepilot_car_rental` DB |
| `src/main/resources/db/schema-mysql.sql` | Legacy DDL (also updated) |

Table-column mapping matches every JPA `@Entity` exactly:

- `users` → `User.java`
- `car_category` → `CarCategory.java`
- `store` → `Store.java`
- `car` → `Car.java`
- `car_image` → `CarImage.java`
- `rental_order` → `RentalOrder.java`
- `payment_order` → `PaymentOrder.java`
- `contract` → `Contract.java`
- `comment_record` → `Comment.java`
- `maintenance_record` → `MaintenanceRecord.java`

---

## DataInitializer

The `DataInitializer` Java class is kept for tests and temporary local fixtures only. It is **disabled by default** for MySQL so the application will not insert demo data into your real local database.

**Control toggle** (in `application.yml` or env):

```yaml
app:
  data-init:
    enabled: false  # default for dev/MySQL
```

To explicitly enable temporary sample data on an empty database:

```powershell
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.arguments=--app.data-init.enabled=true"
```

---

## Environment Variables Summary

| Variable | Used In | Default |
|----------|---------|---------|
| `MYSQL_URL` | dev, mysql profiles | `jdbc:mysql://localhost:3306/drivepilot_car_rental?...` |
| `MYSQL_USER` | dev, mysql profiles | `root` |
| `MYSQL_PASSWORD` | dev, mysql profiles | empty |
| `PAYMENT_CALLBACK_SECRET` | all runtime profiles | `change-me-local-secret` |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `CommunicationsException: Communications link failure` | MySQL not running. Start MySQL service. |
| `Access denied for user` | Check `MYSQL_USER` / `MYSQL_PASSWORD` env vars or defaults. |
| `Unknown database 'drivepilot_car_rental'` | Run `schema.sql` or let `ddl-auto: update` create it (dev profile). |
| Tests fail with MySQL connection error | Ensure test uses H2 — check `src/test/resources/application-test.yml`. |
| `Table 'xxx' doesn't exist` | Run `schema.sql` against your MySQL instance. |
