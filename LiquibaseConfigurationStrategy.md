# 🔧 Liquibase Configuration Strategy - Pokemon Card Planning

## 📋 Overview

**Hybrid Approach:**
- ✅ **Liquibase manages**: `j_employee`, `j_planning`, `j_group`, `j_employee_group` (new planning tables)
- 🔒 **Read-only tables**: `order`, `card`, `card_translation`, `card_certification` (shared with other projects)

---

## 🎯 Strategy

### Tables Managed by Liquibase (Your Project)
```
✅ j_employee                - Employee management
✅ j_planning                - Planning assignments
✅ j_group                   - User groups/roles
✅ j_employee_group          - Employee-group relationships
✅ j_status_workflow         - Status workflow definitions
✅ j_delai_priority_mapping  - Priority mappings
```

### Tables Read-Only (Shared/Legacy)
```
🔒 order                     - Shared with Symfony project
🔒 card                      - Shared card catalog
🔒 card_translation          - Card translations
🔒 card_certification        - Card certifications
🔒 card_certification_order  - Certification-order links
```

---

## ⚙️ Configuration Files

### 1. application-local.properties (Development)

```properties
# ===============================================
# LOCAL DEVELOPMENT
# ===============================================

# Database
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false
spring.datasource.username=ia
spring.datasource.password=foufafou

# Hibernate - Let Liquibase handle schema
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Liquibase - ENABLED for dev
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development
spring.liquibase.default-schema=dev-planning

# Drop and recreate on each restart (dev only!)
spring.liquibase.drop-first=false

# Planning configuration
planning.card.processing.time=3
```

### 2. application-docker.properties (Docker/Staging)

```properties
# ===============================================
# DOCKER / STAGING
# ===============================================

# Database
spring.datasource.url=jdbc:mariadb://database:3306/dev-planning?useSSL=false
spring.datasource.username=ia
spring.datasource.password=foufafou

# Hibernate - Validate only, no changes
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Liquibase - ENABLED for staging
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=docker
spring.liquibase.default-schema=dev-planning
spring.liquibase.drop-first=false

# Planning configuration
planning.card.processing.time=3
```

### 3. application-prod.properties (Production)

```properties
# ===============================================
# PRODUCTION
# ===============================================

# Database (use environment variables)
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

# Hibernate - VALIDATE ONLY (never modify!)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Liquibase - ENABLED for production migrations
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=production
spring.liquibase.default-schema=dev-planning
spring.liquibase.drop-first=false

# Planning configuration
planning.card.processing.time=3
```

---

## 📁 Liquibase Changelog Structure

### File Structure
```
src/main/resources/db/changelog/
├── db.changelog-master.yml          # Main changelog
├── changes/
│   ├── 001-create-employee-table.yml
│   ├── 002-create-planning-table.yml
│   ├── 003-create-group-table.yml
│   ├── 004-create-employee-group-table.yml
│   ├── 005-create-status-workflow.yml
│   └── 006-create-delai-mapping.yml
└── data/
    ├── 001-initial-groups.yml
    └── 002-initial-status-workflow.yml
```

### db.changelog-master.yml

```yaml
databaseChangeLog:
  # ========================================
  # SCHEMA CHANGES
  # ========================================
  
  - include:
      file: db/changelog/changes/001-create-employee-table.yml
      
  - include:
      file: db/changelog/changes/002-create-planning-table.yml
      
  - include:
      file: db/changelog/changes/003-create-group-table.yml
      
  - include:
      file: db/changelog/changes/004-create-employee-group-table.yml
      
  - include:
      file: db/changelog/changes/005-create-status-workflow.yml
      
  - include:
      file: db/changelog/changes/006-create-delai-mapping.yml

  # ========================================
  # INITIAL DATA
  # ========================================
  
  - include:
      file: db/changelog/data/001-initial-groups.yml
      context: development,docker,production
      
  - include:
      file: db/changelog/data/002-initial-status-workflow.yml
      context: development,docker,production
```

---

## 🔨 Entity Annotations

### Liquibase-Managed Entities (Writable)

```java
/**
 * Employee Entity - MANAGED BY LIQUIBASE
 */
@Entity
@Table(name = "j_employee")
@Data
public class Employee extends AbstractUlidEntity {
    // ✅ Liquibase creates and updates this table
    // ✅ Application can INSERT, UPDATE, DELETE
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    // ... other fields
}
```

```java
/**
 * Planning Entity - MANAGED BY LIQUIBASE
 */
@Entity
@Table(name = "j_planning")
@Data
public class Planning extends AbstractUlidEntity {
    // ✅ Liquibase creates and updates this table
    // ✅ Application can INSERT, UPDATE, DELETE
    
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    
    // ... other fields
}
```

### Read-Only Entities (Shared)

```java
/**
 * Order Entity - READ-ONLY (Shared with Symfony)
 * ⚠️ DO NOT MODIFY - Managed by external system
 */
@Entity
@Table(name = "`order`")
@Immutable  // ← Marks as read-only
@Data
public class Order extends AbstractUlidEntity {
    // 🔒 Table exists in database (not created by Liquibase)
    // 🔒 Application can ONLY read (SELECT)
    
    @Column(name = "num_commande", insertable = false, updatable = false)
    private String orderNumber;
    
    @Column(name = "nombre_cartes", insertable = false, updatable = false)
    private Integer cardCount;
    
    // ... all fields marked as insertable=false, updatable=false
}
```

```java
/**
 * Card Entity - READ-ONLY (Shared catalog)
 * ⚠️ DO NOT MODIFY - Shared with other projects
 */
@Entity
@Table(name = "card")
@Immutable  // ← Marks as read-only
@Data
public class Card extends AbstractUlidEntity {
    // 🔒 Shared table, read-only access
    
    @Column(name = "name", insertable = false, updatable = false)
    private String name;
    
    // ... other fields
}
```

---

## 🚀 Migration Steps

### Step 1: Backup Current Database

```bash
# Backup before enabling Liquibase
mysqldump -u ia -pfoufafou dev-planning > backup_before_liquibase.sql
```

### Step 2: Initial Liquibase Setup

Since tables already exist, we need to "baseline" Liquibase:

```sql
-- Connect to database
USE dev-planning;

-- Create Liquibase tracking tables manually
CREATE TABLE IF NOT EXISTS DATABASECHANGELOG (
    ID VARCHAR(255) NOT NULL,
    AUTHOR VARCHAR(255) NOT NULL,
    FILENAME VARCHAR(255) NOT NULL,
    DATEEXECUTED DATETIME NOT NULL,
    ORDEREXECUTED INT NOT NULL,
    EXECTYPE VARCHAR(10) NOT NULL,
    MD5SUM VARCHAR(35),
    DESCRIPTION VARCHAR(255),
    COMMENTS VARCHAR(255),
    TAG VARCHAR(255),
    LIQUIBASE VARCHAR(20),
    CONTEXTS VARCHAR(255),
    LABELS VARCHAR(255),
    DEPLOYMENT_ID VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS DATABASECHANGELOGLOCK (
    ID INT NOT NULL,
    LOCKED BOOLEAN NOT NULL,
    LOCKGRANTED DATETIME,
    LOCKEDBY VARCHAR(255),
    PRIMARY KEY (ID)
);

-- Mark existing tables as already applied
INSERT INTO DATABASECHANGELOG 
(ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE, MD5SUM, DESCRIPTION)
VALUES 
('001-create-employee-table', 'migration', 'db/changelog/changes/001-create-employee-table.yml', 
 NOW(), 1, 'EXECUTED', NULL, 'Table j_employee already exists'),
 
('002-create-planning-table', 'migration', 'db/changelog/changes/002-create-planning-table.yml', 
 NOW(), 2, 'EXECUTED', NULL, 'Table j_planning already exists');
```

### Step 3: Update Configuration

```bash
# Edit application-docker.properties
spring.liquibase.enabled=true
```

### Step 4: Test in Development First

```bash
# Start application in dev mode
./mvnw spring-boot:run -Dspring.profiles.active=local

# Check logs for Liquibase execution
# Should see: "Liquibase: Successfully acquired change log lock"
```

### Step 5: Verify Changes

```sql
-- Check Liquibase tracking
SELECT * FROM DATABASECHANGELOG ORDER BY DATEEXECUTED DESC;

-- Verify tables exist
SHOW TABLES;

-- Check table structure
DESCRIBE j_employee;
DESCRIBE j_planning;
```

---

## 📝 Creating New Changelogs

### Example: Adding New Column to Employee

**File:** `db/changelog/changes/007-add-employee-phone.yml`

```yaml
databaseChangeLog:
  - changeSet:
      id: 007-add-employee-phone
      author: dev-team
      context: development,docker,production
      changes:
        - addColumn:
            tableName: j_employee
            columns:
              - column:
                  name: phone_number
                  type: VARCHAR(20)
                  constraints:
                    nullable: true
        - createIndex:
            tableName: j_employee
            indexName: idx_employee_phone
            columns:
              - column:
                  name: phone_number
      rollback:
        - dropIndex:
            tableName: j_employee
            indexName: idx_employee_phone
        - dropColumn:
            tableName: j_employee
            columnName: phone_number
```

**Add to master:**
```yaml
# db.changelog-master.yml
- include:
    file: db/changelog/changes/007-add-employee-phone.yml
```

---

## ⚠️ Important Rules

### ✅ DO
```
✅ Create changelogs for j_employee, j_planning, j_group
✅ Use contexts (development, docker, production)
✅ Always provide rollback
✅ Test in development first
✅ Backup before migrations
✅ Use semantic versioning (001, 002, 003...)
```

### ❌ DON'T
```
❌ DON'T create changelogs for `order` table
❌ DON'T modify shared tables (card, card_translation)
❌ DON'T use spring.jpa.hibernate.ddl-auto=update with Liquibase
❌ DON'T modify changelogs after they're applied
❌ DON'T skip version numbers
❌ DON'T run migrations without backup
```

---

## 🔍 Verification Checklist

After enabling Liquibase:

- [ ] Tables `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK` exist
- [ ] All j_* tables are tracked in DATABASECHANGELOG
- [ ] Application starts without errors
- [ ] Can insert into j_employee table
- [ ] Can insert into j_planning table
- [ ] CANNOT insert into `order` table (read-only)
- [ ] Hibernate mode is set to `validate`
- [ ] Changelogs are in Git

---

## 🎯 Benefits of This Approach

### For Your Project
```
✅ Full control over planning tables
✅ Version-controlled schema changes
✅ Safe migrations in production
✅ Rollback capability
✅ Team collaboration
```

### For Shared Tables
```
✅ No accidental modifications
✅ Read-only guarantee
✅ Independent from other projects
✅ Safe to sync from Symfony
```

---

## 🚨 Troubleshooting

### Issue: "Liquibase failed to acquire lock"

```sql
-- Clear lock (only if safe!)
UPDATE DATABASECHANGELOGLOCK SET LOCKED = FALSE;
```

### Issue: "ChangeSet already exists"

```sql
-- Check what's already applied
SELECT * FROM DATABASECHANGELOG WHERE ID = 'your-changeset-id';

-- If duplicate, either:
-- 1. Use different ID
-- 2. Remove from tracking (dangerous!)
```

### Issue: "Table already exists"

```yaml
# Use preconditions
- changeSet:
    id: 001-create-employee
    preconditions:
      - not:
          - tableExists:
              tableName: j_employee
    changes:
      - createTable:
          tableName: j_employee
```

---

## 🎉 Summary

**Your Configuration:**
```
✅ Liquibase: ENABLED
✅ Manages: j_employee, j_planning, j_group, j_employee_group
✅ Read-only: order, card, card_translation
✅ Hibernate: validate mode only
✅ Safe for production
```

This gives you the best of both worlds:
- Professional database management for your tables
- Safe read-only access to shared tables
- Full migration history and rollback capability