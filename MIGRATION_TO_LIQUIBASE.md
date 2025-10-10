# 🔄 Migration to Liquibase - Step-by-Step Guide

## 🎯 Goal

Enable Liquibase to manage your planning tables (`j_employee`, `j_planning`, etc.) while keeping shared tables (`order`, `card`) as read-only.

---

## ⏱️ Estimated Time

- **Preparation**: 10 minutes
- **Baseline**: 5 minutes
- **Testing**: 10 minutes
- **Total**: ~25 minutes

---

## 📋 Prerequisites

Before starting, ensure:
- [ ] Database `dev-planning` exists and is accessible
- [ ] Tables `j_employee` and `j_planning` already exist
- [ ] You have a recent backup
- [ ] Spring Boot application is stopped

---

## 🚀 Step-by-Step Migration

### Step 1: Backup Your Database (CRITICAL!)

```bash
# Create backup before any changes
mysqldump -u ia -pfoufafou dev-planning > backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup was created
ls -lh backup_*.sql
```

**Why this is important:** If anything goes wrong, you can restore instantly.

---

### Step 2: Create Liquibase Changelog Structure

```bash
# Create directory structure
mkdir -p src/main/resources/db/changelog/changes
mkdir -p src/main/resources/db/changelog/data

# Copy the changelog files (provided as artifacts)
# Place them in the correct locations:
# - 001-create-employee-table.yml → changes/
# - 002-create-planning-table.yml → changes/
# - db.changelog-master.yml → db/changelog/
```

**File structure should look like:**
```
src/main/resources/db/
└── changelog/
    ├── db.changelog-master.yml          # Main changelog
    ├── changes/
    │   ├── 001-create-employee-table.yml
    │   └── 002-create-planning-table.yml
    └── data/
        └── (future data migrations)
```

---

### Step 3: Run Baseline Script

This marks existing tables as "already applied" so Liquibase won't try to recreate them.

```bash
# Run the baseline script
mysql -u ia -pfoufafou dev-planning < liquibase_baseline.sql

# You should see output like:
# ========================================
# BASELINE COMPLETED
# ========================================
# changeset_id: 001-create-employee-table
# changeset_id: 002-create-planning-table
```

**Verify baseline succeeded:**
```sql
mysql -u ia -pfoufafou dev-planning -e "SELECT ID, DESCRIPTION FROM DATABASECHANGELOG;"
```

Expected output:
```
+---------------------------+--------------------------------+
| ID                        | DESCRIPTION                    |
+---------------------------+--------------------------------+
| 001-create-employee-table | createTable tableName=j_employee|
| 002-create-planning-table | createTable tableName=j_planning|
+---------------------------+--------------------------------+
```

---

### Step 4: Update Application Configuration

#### For Development (application-local.properties)

```properties
# Enable Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development
spring.liquibase.default-schema=dev-planning

# Change Hibernate mode to validate (don't modify schema!)
spring.jpa.hibernate.ddl-auto=validate
```

#### For Docker (application-docker.properties)

```properties
# Enable Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=docker
spring.liquibase.default-schema=dev-planning

# Change Hibernate mode to validate
spring.jpa.hibernate.ddl-auto=validate
```

---

### Step 5: Update Entity Annotations

#### Entities Managed by Liquibase (Writable)

```java
// ✅ Remove @Immutable from writable entities
@Entity
@Table(name = "j_employee")
// @Immutable  ← REMOVE THIS
@Data
public class Employee extends AbstractUlidEntity {
    // Normal entity...
}
```

```java
@Entity
@Table(name = "j_planning")
// @Immutable  ← REMOVE THIS
@Data
public class Planning extends AbstractUlidEntity {
    // Normal entity...
}
```

#### Entities for Shared Tables (Read-Only)

```java
// ✅ Keep @Immutable for read-only entities
@Entity
@Table(name = "`order`")
@Immutable  // ← KEEP THIS
@Data
public class Order extends AbstractUlidEntity {
    // Read-only entity...
    
    @Column(name = "num_commande", insertable = false, updatable = false)
    private String orderNumber;
    
    // All fields should have insertable=false, updatable=false
}
```

---

### Step 6: Test the Configuration

#### Start the Application

```bash
# Development mode
./mvnw spring-boot:run -Dspring.profiles.active=local

# Watch for Liquibase logs
# Should see:
# Liquibase: Successfully acquired change log lock
# Liquibase: Reading from DATABASECHANGELOG
# Liquibase: Successfully released change log lock
```

#### Verify Liquibase Ran Successfully

Check application logs for:
```
✅ Liquibase: Successfully acquired change log lock
✅ Liquibase: Reading from DATABASECHANGELOG
✅ No new changesets to execute (all already applied)
✅ Liquibase: Successfully released change log lock
```

**If you see errors:**
- Check the error message carefully
- Verify changelog files are in correct locations
- Check database connection

---

### Step 7: Test Database Operations

#### Test Writable Tables (Should Work)

```bash
# Test employee insertion
curl -X POST http://localhost:8080/api/test/employee | jq

# Expected: "success": true, "insertedRows": 3
```

```bash
# Test planning insertion
curl -X POST http://localhost:8080/api/test/planning | jq

# Expected: "success": true
```

#### Test Read-Only Tables (Should Fail Writes)

Try to create an order (should fail or be read-only):
```java
// This should throw an exception or be ignored
Order order = new Order();
order.setOrderNumber("TEST-123");
orderRepository.save(order);  // ❌ Should fail (read-only)
```

---

### Step 8: Verify Configuration

Run these checks:

```bash
# 1. Check Liquibase tracking
mysql -u ia -pfoufafou dev-planning -e "
SELECT ID, AUTHOR, DATEEXECUTED, EXECTYPE, DESCRIPTION 
FROM DATABASECHANGELOG 
ORDER BY ORDEREXECUTED;"

# 2. Check table structure
mysql -u ia -pfoufafou dev-planning -e "DESCRIBE j_employee;"
mysql -u ia -pfoufafou dev-planning -e "DESCRIBE j_planning;"

# 3. Test insertion via API
curl http://localhost:8080/api/test/all | jq '.overallSuccess'
# Should return: true
```

---

## ✅ Success Criteria

You know the migration succeeded when:

- [x] Application starts without errors
- [x] Logs show "Liquibase: Successfully released change log lock"
- [x] `DATABASECHANGELOG` table exists and has entries
- [x] Can insert into `j_employee` and `j_planning` tables
- [x] CANNOT insert into `order` table (read-only)
- [x] `spring.jpa.hibernate.ddl-auto=validate` is set
- [x] All tests pass

---

## 🐛 Troubleshooting

### Problem: "Liquibase failed to acquire lock"

**Cause:** Lock table stuck from previous run

**Solution:**
```sql
UPDATE DATABASECHANGELOGLOCK SET LOCKED = FALSE WHERE ID = 1;
```

---

### Problem: "ChangeSet already exists"

**Cause:** Changeset ID conflict or already applied

**Solution:**
```sql
-- Check what's applied
SELECT * FROM DATABASECHANGELOG WHERE ID = 'your-changeset-id';

-- If needed, use different ID or verify baseline was correct
```

---

### Problem: "Table 'j_employee' already exists"

**Cause:** Baseline script didn't run or preconditions not working

**Solution:**
```sql
-- Manually mark as applied
INSERT INTO DATABASECHANGELOG 
(ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE, DESCRIPTION)
VALUES 
('001-create-employee-table', 'manual', 
 'db/changelog/changes/001-create-employee-table.yml', 
 NOW(), 1, 'EXECUTED', 'Manual baseline');
```

---

### Problem: Application won't start - "Invalid context"

**Cause:** Wrong context in configuration

**Solution:**
```properties
# Make sure contexts match
spring.liquibase.contexts=development  # or docker, or production
```

In changelog files:
```yaml
- changeSet:
    context: development,docker,production  # Include all contexts
```

---

### Problem: "Cannot insert into table order"

**Cause:** This is EXPECTED - order is read-only!

**Solution:** This is correct behavior. Order table should be read-only.

---

## 🔄 Rollback Plan

If something goes wrong:

### Option 1: Restore Backup
```bash
# Stop application
./mvnw spring-boot:stop

# Restore database
mysql -u ia -pfoufafou dev-planning < backup_YYYYMMDD_HHMMSS.sql

# Revert configuration changes
git checkout application-docker.properties
```

### Option 2: Disable Liquibase
```properties
# Temporarily disable Liquibase
spring.liquibase.enabled=false
spring.jpa.hibernate.ddl-auto=update
```

### Option 3: Clear Liquibase Tracking
```sql
-- Remove all Liquibase tracking (use with caution!)
DROP TABLE IF EXISTS DATABASECHANGELOG;
DROP TABLE IF EXISTS DATABASECHANGELOGLOCK;
```

---

## 📝 Post-Migration Checklist

After successful migration:

- [ ] Backup was created and verified
- [ ] Baseline script executed successfully
- [ ] Application starts without errors
- [ ] Liquibase logs show success
- [ ] Can insert into managed tables
- [ ] Cannot insert into read-only tables
- [ ] All tests pass
- [ ] Configuration committed to Git
- [ ] Team notified of Liquibase activation
- [ ] Documentation updated

---

## 🎯 Next Steps

Now that Liquibase is active:

### Creating New Schema Changes

Always create a new changelog file:

```bash
# Example: Adding phone number to employee
touch src/main/resources/db/changelog/changes/007-add-employee-phone.yml
```

```yaml
databaseChangeLog:
  - changeSet:
      id: 007-add-employee-phone
      author: your-name
      context: development,docker,production
      changes:
        - addColumn:
            tableName: j_employee
            columns:
              - column:
                  name: phone_number
                  type: VARCHAR(20)
```

Update master changelog:
```yaml
# db.changelog-master.yml
- include:
    file: db/changelog/changes/007-add-employee-phone.yml
```

### Best Practices

1. **Always increment changeset IDs** (007, 008, 009...)
2. **Never modify existing changesets** (create new ones)
3. **Always provide rollback** when possible
4. **Test in development** before deploying
5. **Use semantic naming** (007-add-employee-phone, not 007-misc)
6. **Document changes** in comments
7. **Review changesets** in code reviews

---

## 📚 Additional Resources

- [Liquibase Documentation](https://docs.liquibase.com)
- [Spring Boot with Liquibase](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.liquibase)
- [Liquibase Best Practices](https://www.liquibase.org/get-started/best-practices)

---

## 🎉 Congratulations!

You've successfully migrated to Liquibase! Your database changes are now:
- ✅ Version controlled
- ✅ Auditable
- ✅ Rollback-capable
- ✅ Production-safe
- ✅ Team-friendly

Your project now follows industry best practices for database management! 🚀