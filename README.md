# 🚀 First Time Setup Guide - Pokemon Card Planning

**For new developers starting from scratch (no database)**

This guide will help you set up the complete development environment when you have **nothing** on your machine.

---

## 📋 Prerequisites

Before starting, ensure you have:

- ✅ **Java 17+** installed (`java -version`)
- ✅ **Maven 3.8+** installed (`mvn -version`)
- ✅ **Node.js 18+** and npm (`node -v` and `npm -v`)
- ✅ **MariaDB/MySQL 10.6+** installed and running
- ✅ **Git** installed
- ✅ Access to the **Symfony backend API** (for data synchronization)

---

## 🎯 Architecture Overview

This project uses **TWO databases**:

```
┌─────────────────────┐           ┌──────────────────────┐
│   Symfony Backend   │           │  Spring Boot Backend │
│   (External API)    │           │   (This project)     │
│                     │           │                      │
│   Database: "dev"   │  Sync →   │  Database:           │
│   Port: 8000        │           │  "dev-planning"      │
│                     │           │  Port: 8080          │
│   Tables:           │           │                      │
│   - order           │           │  Tables:             │
│   - card            │           │  - order (synced)    │
│   - card_*          │           │  - card (synced)     │
│                     │           │  - j_employee (new)  │
│                     │           │  - j_planning (new)  │
│                     │           │  - j_group (new)     │
└─────────────────────┘           └──────────────────────┘
```

**Key Points:**
- **Symfony tables** (`order`, `card`, etc.) are synced via API
- **Planning tables** (`j_employee`, `j_planning`, etc.) are created by Spring Boot
- You don't need direct access to the Symfony database

---

## 🔧 Step-by-Step Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/YOUR_ORG/pokemon-planning.git
cd pokemon-planning
```

---

### Step 2: Create Database

**Option A: Let Spring Boot create it automatically**

Spring Boot can create the database if you add `createDatabaseIfNotExist=true` to the connection string.

**Option B: Create it manually (recommended for first time)**

```bash
# Connect to MySQL/MariaDB as root
mysql -u root -p

# Create database
CREATE DATABASE IF NOT EXISTS `dev-planning` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

# Create user (if needed)
CREATE USER IF NOT EXISTS 'your_username'@'localhost' 
  IDENTIFIED BY 'your_password';

# Grant permissions
GRANT ALL PRIVILEGES ON `dev-planning`.* 
  TO 'your_username'@'localhost';

FLUSH PRIVILEGES;

# Verify
USE `dev-planning`;
SHOW TABLES;  -- Should be empty for now

EXIT;
```

---

### Step 3: Configure Backend (Spring Boot)

Create your local configuration file:

```bash
# Copy the template
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

**Edit `src/main/resources/application-local.properties`:**

```properties
# Application
spring.application.name=pokemon-card-planning
server.port=8080

# ============================================================
# DATABASE CONFIGURATION - IMPORTANT!
# ============================================================
# Option A: Let Spring create the database (easier)
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true

# Option B: Database already created manually
# spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true

spring.datasource.username=YOUR_DATABASE_USERNAME
spring.datasource.password=YOUR_DATABASE_PASSWORD
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# ============================================================
# JPA/HIBERNATE - TABLE CREATION STRATEGY
# ============================================================
# IMPORTANT: For first-time setup, use 'update' to create tables
# After tables exist, you can change to 'validate'
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# ============================================================
# LIQUIBASE - CREATES PLANNING TABLES
# ============================================================
# Enable Liquibase to create j_employee, j_planning, j_group, etc.
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development
spring.liquibase.default-schema=dev-planning

# ============================================================
# SYMFONY API - FOR DATA SYNCHRONIZATION
# ============================================================
# Update this with your Symfony API URL
symfony.api.base-url=http://localhost:8000
symfony.api.timeout=30

# CORS
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Logging
logging.level.root=INFO
logging.level.com.pcagrade=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.liquibase=INFO

# Planning Configuration
planning.card.processing.time=3
planning.employee.break.time=15
planning.workday.start=08:00
planning.workday.end=17:00

# Order table compatibility
order.table.readonly=true
order.status.integer.mapping=true
order.adapter.service.enabled=true
```

**⚠️ CRITICAL SETTINGS for first-time setup:**
- ✅ `spring.jpa.hibernate.ddl-auto=update` (creates tables)
- ✅ `spring.liquibase.enabled=true` (creates planning tables)
- ✅ `createDatabaseIfNotExist=true` (optional, creates database)

---

### Step 4: Start Backend

```bash
# From project root
./mvnw clean spring-boot:run
```

**What happens during first startup:**

1. ✅ Spring Boot connects to MariaDB
2. ✅ Database `dev-planning` is created (if using `createDatabaseIfNotExist=true`)
3. ✅ **Liquibase creates planning tables:**
   - `j_employee`
   - `j_planning`
   - `j_order_status_mapping`
   - `j_priority`
   - `j_group`
   - `j_employee_group`
   - And other planning tables
4. ✅ Hibernate validates/creates entity tables
5. ✅ Backend starts on `http://localhost:8080`

**Expected console output:**
```
INFO  liquibase.changelog - Creating database dev-planning
INFO  liquibase.changelog - ChangeSet db/changelog/db.changelog-master.yml::001-create-employee-table::pokemon-card-planning ran successfully
INFO  liquibase.changelog - ChangeSet db/changelog/db.changelog-master.yml::002-create-planning-table::pokemon-card-planning ran successfully
...
INFO  c.p.o.OrderManagementApplication - Started OrderManagementApplication in 12.5 seconds
```

**Verify backend is running:**
```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

---

### Step 5: Synchronize Data from Symfony

**⚠️ IMPORTANT:** At this point, planning tables exist, but Symfony tables are **EMPTY**.

You need to synchronize data from the Symfony backend:

```bash
# Check Symfony API connectivity
curl http://localhost:8080/api/sync/symfony-health

# Check sync status (will show tables are out of sync)
curl http://localhost:8080/api/sync/status

# Sync ALL data from Symfony (orders, cards, translations, certifications)
curl -X POST http://localhost:8080/api/sync/all
```

**Or use the UI:**
1. Navigate to: `http://localhost:3000/sync` (after frontend setup)
2. Click "🔍 Check Status"
3. Click "🚀 Sync All Data"

**This will:**
- ✅ Fetch orders from Symfony API
- ✅ Fetch cards from Symfony API
- ✅ Fetch card translations
- ✅ Fetch card certifications
- ✅ Insert all data into `dev-planning`

**Verify data was synced:**
```bash
# Check orders
curl http://localhost:8080/api/orders | jq '. | length'
# Should return > 0

# Or check database directly
mysql -u your_username -p dev-planning -e "SELECT COUNT(*) FROM \`order\`;"
```

---

### Step 6: Setup Frontend (Vue.js)

```bash
cd src/main/frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will be available at: `http://localhost:5173` or `http://localhost:3000`

---

## 🧪 Verify Everything Works

### 1. Backend Health Check
```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 2. Check Database Tables
```bash
mysql -u your_username -p dev-planning

SHOW TABLES;
```

**You should see:**
```
+---------------------------+
| Tables_in_dev-planning    |
+---------------------------+
| card                      |
| card_certification        |
| card_translation          |
| order                     |
| j_employee                |
| j_employee_group          |
| j_group                   |
| j_order_status_mapping    |
| j_planning                |
| j_priority                |
| DATABASECHANGELOG         |
| DATABASECHANGELOGLOCK     |
+---------------------------+
```

### 3. Test API Endpoints
```bash
# Employees (should work even if empty)
curl http://localhost:8080/api/employees

# Orders (should return data after sync)
curl http://localhost:8080/api/orders | jq '. | length'

# Sync status
curl http://localhost:8080/api/sync/status
```

### 4. Test Frontend
Open `http://localhost:5173` in your browser:
- ✅ Dashboard should load
- ✅ Orders page should show orders (after sync)
- ✅ Employees page should work
- ✅ Planning page should work

---

## ⚙️ Configuration for Different Scenarios

### Scenario A: Fresh Install (No Database)
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?createDatabaseIfNotExist=true
spring.jpa.hibernate.ddl-auto=update
spring.liquibase.enabled=true
```

### Scenario B: Database Exists, No Tables
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning
spring.jpa.hibernate.ddl-auto=update
spring.liquibase.enabled=true
```

### Scenario C: Everything Exists (Normal Development)
```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning
spring.jpa.hibernate.ddl-auto=validate
spring.liquibase.enabled=false
```

---

## 🐛 Troubleshooting

### Problem: "Unknown database 'dev-planning'"

**Solution:**
1. Ensure database was created (Step 2)
2. Or add `createDatabaseIfNotExist=true` to connection URL

### Problem: "Table 'order' doesn't exist"

**Solution:**
```bash
# Sync data from Symfony
curl -X POST http://localhost:8080/api/sync/all
```

### Problem: "Liquibase failed"

**Solution:**
Check that MariaDB version is compatible (10.6+):
```bash
mysql -V
```

### Problem: "Cannot connect to Symfony API"

**Solution:**
1. Verify Symfony is running: `curl http://localhost:8000/api/export/health`
2. Update `symfony.api.base-url` in `application-local.properties`

### Problem: Backend starts but no planning tables

**Solution:**
Verify Liquibase is enabled:
```properties
spring.liquibase.enabled=true
```

Check logs for Liquibase errors:
```bash
./mvnw spring-boot:run | grep liquibase
```

---

## 📝 After First Setup

Once everything works, you can optimize configuration:

**For daily development:**
```properties
# Disable Liquibase (tables already exist)
spring.liquibase.enabled=false

# Use validate mode (safer)
spring.jpa.hibernate.ddl-auto=validate

# Reduce logging
logging.level.org.hibernate.SQL=WARN
logging.level.liquibase=INFO
```

---

## 🎉 Summary Checklist

- [ ] Cloned repository
- [ ] Created `dev-planning` database
- [ ] Created `application-local.properties` with credentials
- [ ] Set `spring.jpa.hibernate.ddl-auto=update`
- [ ] Set `spring.liquibase.enabled=true`
- [ ] Started backend successfully
- [ ] Verified planning tables were created by Liquibase
- [ ] Synced data from Symfony API (`/api/sync/all`)
- [ ] Verified orders and cards exist in database
- [ ] Installed frontend dependencies
- [ ] Started frontend successfully
- [ ] Tested all pages in UI

**You're ready to develop!** 🚀

---

## 📚 Additional Resources

- **Data Sync Guide:** `API_SYNC_SETUP_GUIDE.md`
- **Configuration Guide:** `README.md`
- **Backend Update Guide:** `UPDATE_BACKEND_CONFIG.md`
- **API Documentation:** `http://localhost:8080/swagger-ui.html` (if Swagger is enabled)

---

## 💡 Tips for New Developers

1. **Always check sync status** before working with orders:
   ```bash
   curl http://localhost:8080/api/sync/status
   ```

2. **Use Docker** if you have issues with local MariaDB:
   ```bash
   docker run -d --name mariadb \
     -e MYSQL_ROOT_PASSWORD=root \
     -e MYSQL_DATABASE=dev-planning \
     -e MYSQL_USER=ia \
     -e MYSQL_PASSWORD=foufafou \
     -p 3306:3306 \
     mariadb:11.2
   ```

3. **Keep Symfony tables read-only** in this project - they are synced from Symfony

4. **Planning tables are managed by this project** - you can modify them

---

## 🚨 Important Notes

⚠️ **DO NOT commit `application-local.properties`** - it contains credentials

⚠️ **DO NOT modify Symfony tables directly** - use the sync API

⚠️ **Liquibase runs only once** - after first startup, tables are created

✅ **Re-sync data periodically** to stay up-to-date with Symfony backend

✅ **Use `spring.jpa.hibernate.ddl-auto=validate`** after first setup