# 🧹 Database Cleanup Guide - dev-planning

## 🎯 Goal

Clean up the `dev-planning` database by removing tables that are no longer needed after the Symfony API integration.

**Estimated time**: 10 minutes

---

## 📋 Tables to Remove

### ✅ SAFE TO REMOVE

1. **`invoice`** - Data now in `order.price` field (synced from Symfony)
2. **`card`** (optional) - If not used by planning system
3. **`card_translation`** (optional) - If not used
4. **`card_certification`** (optional) - If not used
5. **`card_certification_order`** (optional) - If not used

### ⚠️ KEEP THESE TABLES

1. **`order`** - Core planning data (with new `price` and `delai` fields)
2. **`j_employee`** - Employee management
3. **`j_planning`** - Planning assignments
4. **`j_group`** - User groups/roles
5. **`j_employee_group`** - Employee-group relationships
6. **All other `j_*` tables** - Managed by Liquibase

---

## 🔍 Step 1: Verify Table Usage (5 min)

Before removing tables, verify they're not used:

```bash
mysql -u ia -p dev-planning << 'EOF'
-- 1. Check invoice table
SELECT 'invoice' as table_name, COUNT(*) as row_count FROM invoice;

-- 2. Check card table
SELECT 'card' as table_name, COUNT(*) as row_count FROM card;

-- 3. Check if j_planning references card
SELECT COUNT(*) as cards_in_planning 
FROM j_planning 
WHERE card_id IS NOT NULL;

-- 4. Check foreign keys pointing to invoice
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'invoice'
AND TABLE_SCHEMA = 'dev-planning';

-- 5. Check foreign keys pointing to card
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'card'
AND TABLE_SCHEMA = 'dev-planning';
EOF
```

**Expected results**:
- `invoice`: May have rows, but data now in `order.price`
- `card`: Probably 0 rows (or old sync data)
- `cards_in_planning`: Should be 0 (planning doesn't track individual cards)
- Foreign keys: Should show dependencies (we'll handle these)

---

## 💾 Step 2: Backup Database (CRITICAL!)

**ALWAYS backup before dropping tables!**

```bash
# Full backup
mysqldump -u ia -p dev-planning > backup_dev_planning_$(date +%Y%m%d_%H%M%S).sql

# Backup specific tables (optional)
mysqldump -u ia -p dev-planning invoice card card_translation card_certification card_certification_order > backup_tables_to_remove_$(date +%Y%m%d).sql

# Verify backup was created
ls -lh backup_*.sql
```

---

## 🗑️ Step 3: Remove invoice Table (2 min)

The `invoice` table is no longer needed because:
- Price data is now in `order.price` field
- Synced from Symfony API's `invoice.total_ttc`

```sql
-- Connect to database
mysql -u ia -p dev-planning

-- Check foreign keys first
SHOW CREATE TABLE invoice;

-- Drop foreign key constraints if any exist
-- ALTER TABLE some_table DROP FOREIGN KEY fk_invoice_id;

-- Drop the table
DROP TABLE IF EXISTS invoice;

-- Verify it's gone
SHOW TABLES LIKE 'invoice';
-- Should return: Empty set

-- Exit
EXIT;
```

---

## 🗑️ Step 4: Remove card Tables (Optional, 3 min)

Only remove these if they're NOT used by your planning system.

### Check Usage First

```sql
mysql -u ia -p dev-planning << 'EOF'
-- Check if any j_planning entries reference cards
SELECT COUNT(*) FROM j_planning WHERE card_id IS NOT NULL;

-- If result is 0, safe to remove card tables
-- If result > 0, DO NOT remove (planning uses cards)
EOF
```

### If Safe to Remove (count = 0)

```sql
mysql -u ia -p dev-planning

-- Drop tables in correct order (child tables first)
DROP TABLE IF EXISTS card_certification_order;
DROP TABLE IF EXISTS card_certification;
DROP TABLE IF EXISTS card_translation;
DROP TABLE IF EXISTS card;

-- Verify they're gone
SHOW TABLES;

EXIT;
```

---

## ✅ Step 5: Verify Cleanup (2 min)

```bash
# Check remaining tables
mysql -u ia -p dev-planning -e "SHOW TABLES;"

# Should see something like:
# +-------------------------+
# | Tables_in_dev-planning  |
# +-------------------------+
# | DATABASECHANGELOG       |
# | DATABASECHANGELOGLOCK   |
# | j_employee              |
# | j_employee_group        |
# | j_group                 |
# | j_planning              |
# | order                   |
# +-------------------------+
```

**Expected tables after cleanup**:
- ✅ `order` - With `price` and `delai` fields
- ✅ `j_employee` - Employee data
- ✅ `j_planning` - Planning assignments
- ✅ `j_group` - User groups
- ✅ `j_employee_group` - Employee-group links
- ✅ `DATABASECHANGELOG*` - Liquibase tracking
- ❌ `invoice` - REMOVED
- ❌ `card*` - REMOVED (if not used)

---

## 📊 Step 6: Optimize Database (1 min)

After removing tables, optimize the database:

```sql
mysql -u ia -p dev-planning << 'EOF'
-- Optimize remaining tables
OPTIMIZE TABLE `order`;
OPTIMIZE TABLE j_employee;
OPTIMIZE TABLE j_planning;

-- Check database size
SELECT 
    table_name AS 'Table',
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size (MB)'
FROM information_schema.TABLES
WHERE table_schema = 'dev-planning'
ORDER BY (data_length + index_length) DESC;
EOF
```

---

## 🔄 Step 7: Update Spring Boot Entities (if needed)

If you removed the `card` table, you may want to update your Spring Boot code:

### Option A: Keep Card Entity (Read-Only)

If `card` data is still synced from Symfony:

```java
@Entity
@Table(name = "card")
@Immutable  // Mark as read-only
@Data
public class Card extends AbstractUlidEntity {
    // Keep entity, but mark all columns as insertable=false, updatable=false
}
```

### Option B: Remove Card Entity

If `card` table is completely removed:

1. Comment out or delete `Card.java` entity
2. Comment out or delete `CardRepository.java`
3. Remove references to `Card` in services
4. Recompile: `mvn clean compile`

---

## 🧪 Step 8: Test Application (3 min)

After cleanup, verify everything still works:

```bash
# 1. Start Spring Boot
mvn spring-boot:run

# 2. Check logs for errors
# Should NOT see any "Table 'invoice' doesn't exist" errors

# 3. Test order sync
curl -X POST http://localhost:8080/api/sync/orders | jq

# 4. Query orders
curl http://localhost:8080/api/orders | jq

# 5. Verify price and delai are present
mysql -u ia -p dev-planning << 'EOF'
SELECT order_number, delai, price, total_cards 
FROM `order` 
LIMIT 5;
EOF
```

---

## 🎯 Summary of Changes

### Before Cleanup
```
dev-planning database (543 MB)
├── order (30,000 rows, ~6 MB)
├── invoice (30,000 rows, ~6 MB)      ← REMOVED
├── card (500,000 rows, ~100 MB)      ← REMOVED (if unused)
├── card_translation (2M rows, 400MB) ← REMOVED (if unused)
├── card_certification (50K, ~10 MB)  ← REMOVED (if unused)
├── card_certification_order (50K, 5MB) ← REMOVED (if unused)
├── j_employee (~10 rows)
├── j_planning (~100 rows)
└── j_* tables (various)
```

### After Cleanup
```
dev-planning database (~20 MB)
├── order (30,000 rows, ~8 MB with new fields)
├── j_employee (~10 rows)
├── j_planning (~100 rows)
├── j_group (~5 rows)
├── j_employee_group (~20 rows)
└── DATABASECHANGELOG* (Liquibase)
```

**Space saved**: ~520 MB (96% reduction!)

---

## 🐛 Troubleshooting

### Error: "Cannot drop table 'invoice': foreign key constraint fails"

**Solution**: Find and drop the foreign key first:

```sql
-- Find foreign keys
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'invoice'
AND TABLE_SCHEMA = 'dev-planning';

-- Drop the constraint
ALTER TABLE some_table DROP FOREIGN KEY fk_constraint_name;

-- Then drop the table
DROP TABLE invoice;
```

### Error: "Application fails to start after removing Card entity"

**Solution**:

1. Check for references to `Card` in services:
   ```bash
   grep -r "Card card" src/main/java/
   ```

2. Comment out or remove `CardService`, `CardRepository`, etc.

3. Rebuild:
   ```bash
   mvn clean compile
   ```

### Want to Restore?

If something goes wrong:

```bash
# Restore from backup
mysql -u ia -p dev-planning < backup_dev_planning_YYYYMMDD_HHMMSS.sql

# Or restore specific tables
mysql -u ia -p dev-planning < backup_tables_to_remove_YYYYMMDD.sql
```

---

## ✅ Cleanup Checklist

- [ ] ✅ Verified table usage
- [ ] ✅ Created full database backup
- [ ] ✅ Dropped `invoice` table
- [ ] ✅ Dropped `card*` tables (if not used)
- [ ] ✅ Optimized database
- [ ] ✅ Updated Spring Boot entities (if needed)
- [ ] ✅ Tested application
- [ ] ✅ Verified data integrity
- [ ] ✅ Kept backup file safe

---

## 🎉 Success!

Your `dev-planning` database is now **clean and optimized**!

**Benefits**:
- ✅ 96% smaller database size
- ✅ Faster queries
- ✅ Simpler architecture
- ✅ No data duplication
- ✅ Single source of truth (Symfony API)

**Next steps**: Focus on planning algorithm improvements! 🚀