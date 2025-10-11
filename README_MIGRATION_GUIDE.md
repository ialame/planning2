# 🚀 Spring Boot Migration Guide - API Symfony Integration

## 📋 Overview

This guide will help you:
- ✅ Update Spring Boot to sync `price` and `delai` from Symfony
- ✅ Remove `priority` field (replaced by `delai`)
- ✅ Clean up database (remove `invoice` table)
- ✅ Configure Spring Boot to use the new API

**Estimated time**: 30 minutes

---

## 🎯 What We're Changing

### Before
```java
// Order entity (OLD)
- priority: Integer (1, 2, 3...)
- No price field
```

### After
```java
// Order entity (NEW)
- delai: String ("X", "F+", "F", "C", "E")
- price: Float (from invoice.total_ttc)
```

---

## 📝 Step-by-Step Implementation

### Step 1: Update Order Entity (5 min)

**File**: `src/main/java/com/pcagrade/order/entity/Order.java`

Add these fields to your Order entity:

```java
/**
 * Price from invoice (total_ttc)
 */
@Column(name = "price", insertable = false, updatable = false)
private Float price;

/**
 * Delivery priority code
 * X = 1 day, F+ = 1 week, F = 2 weeks, C = 1 month, E = 3 months
 */
@Column(name = "delai", length = 3, insertable = false, updatable = false)
private String delai;
```

**Remove or comment out** the old `priority` field:

```java
// @Column(name = "priority", insertable = false, updatable = false)
// private Integer priority;  // ← DEPRECATED, use delai instead
```

**Add getters/setters** if not using Lombok:

```java
public Float getPrice() { return price; }
public void setPrice(Float price) { this.price = price; }

public String getDelai() { return delai; }
public void setDelai(String delai) { this.delai = delai; }
```

---

### Step 2: Update Database Schema (2 min)

Run this SQL to add the new columns:

```sql
-- Add price column
ALTER TABLE `order` 
ADD COLUMN price FLOAT DEFAULT 0.0 AFTER status;

-- Add delai column (if not exists)
-- ALTER TABLE `order` 
-- ADD COLUMN delai VARCHAR(3) DEFAULT 'C' AFTER price;
-- (This column probably already exists from Symfony sync)

-- Remove priority column (optional, after migration)
-- ALTER TABLE `order` DROP COLUMN priority;
```

---

### Step 3: Update MinimalSyncService (10 min)

**File**: `src/main/java/com/pcagrade/order/service/MinimalSyncService.java`

Replace the file content with the updated version from the artifact "MinimalSyncService.java - Updated".

Key changes:
- ✅ Sync 9 fields instead of 8
- ✅ Map `price` from Symfony
- ✅ Map `delai` instead of `priority`
- ✅ Better error handling

---

### Step 4: Update MinimalSyncController (5 min)

**File**: `src/main/java/com/pcagrade/order/controller/MinimalSyncController.java`

Update the Symfony API URL configuration:

```java
@Value("${symfony.api.base-url:http://localhost:8000}")
private String symfonyApiUrl;

// In syncOrders() method:
String ordersUrl = symfonyApiUrl + "/api/planning/export/orders?limit=10000&exclude_completed=true";

// In syncCards() method:
String cardsUrl = symfonyApiUrl + "/api/planning/export/cards?limit=50000";
```

---

### Step 5: Update application-local.properties (2 min)

**File**: `src/main/resources/application-local.properties`

Ensure the Symfony API URL is correct:

```properties
# Symfony API Configuration
symfony.api.base-url=http://localhost:8000
symfony.api.timeout=30
```

---

### Step 6: Clean Cache & Rebuild (3 min)

```bash
# Clean and rebuild
mvn clean compile

# Verify compilation succeeded
# Should see: BUILD SUCCESS
```

---

### Step 7: Test Synchronization (5 min)

```bash
# 1. Start Spring Boot
mvn spring-boot:run

# 2. Test health check
curl http://localhost:8080/api/sync/health | jq

# Expected: {"status":"healthy","symfony_api":"connected"}

# 3. Sync orders
curl -X POST http://localhost:8080/api/sync/orders | jq

# Expected: {"success":true,"synced_count":8000,...}

# 4. Verify in database
mysql -u ia -p dev-planning << 'EOF'
SELECT 
    order_number, 
    delai, 
    price, 
    total_cards, 
    status 
FROM `order` 
LIMIT 10;
EOF
```

You should see:
```
+---------------+-------+--------+-------------+--------+
| order_number  | delai | price  | total_cards | status |
+---------------+-------+--------+-------------+--------+
| QYRFJGPKY     | C     | 244.51 |          20 |      1 |
| OHPYFSHRL     | C     |  79.48 |           4 |      1 |
+---------------+-------+--------+-------------+--------+
```

---

### Step 8: Clean Up Database (3 min)

See the "DATABASE_CLEANUP_GUIDE.md" artifact for detailed instructions.

Quick version:

```sql
-- 1. Backup first!
mysqldump -u ia -p dev-planning > backup_before_cleanup_$(date +%Y%m%d).sql

-- 2. Drop invoice table (data now in order.price)
DROP TABLE IF EXISTS invoice;

-- 3. Verify card table usage
SELECT COUNT(*) FROM card;  -- If 0, can be dropped

-- 4. Drop card table if unused
-- DROP TABLE IF EXISTS card;
```

---

## 🧪 Verification Checklist

After completing all steps:

- [ ] ✅ Order entity has `price` and `delai` fields
- [ ] ✅ Order entity NO LONGER uses `priority` field
- [ ] ✅ MinimalSyncService syncs 9 fields (including price and delai)
- [ ] ✅ Spring Boot compiles without errors
- [ ] ✅ Health check returns "connected"
- [ ] ✅ Orders sync successfully (8000+ orders)
- [ ] ✅ Database has `price` and `delai` columns populated
- [ ] ✅ `invoice` table removed from dev-planning
- [ ] ✅ All tests pass

---

## 🐛 Troubleshooting

### Error: "Unknown column 'price'"

**Solution**: Run the ALTER TABLE to add the column:
```sql
ALTER TABLE `order` ADD COLUMN price FLOAT DEFAULT 0.0;
```

### Error: "Cannot find symbol: method getDelai()"

**Solution**: Add getter/setter or ensure Lombok is working:
```java
public String getDelai() { return delai; }
public void setDelai(String delai) { this.delai = delai; }
```

### Error: "Symfony API not reachable"

**Solution**:
1. Verify Symfony is running: `curl http://localhost:8000/api/planning/export/health`
2. Check `symfony.api.base-url` in application-local.properties

### Sync returns 0 orders

**Solution**:
- Check filters: default excludes completed orders (status 5,6,8)
- Try: `curl "http://localhost:8000/api/planning/export/orders?limit=100"`
- Verify orders exist: `mysql -u ia -p dev -e "SELECT COUNT(*) FROM \`order\` WHERE annulee = 0 AND status NOT IN (5,6,8);"`

---

## 📊 Expected Results

After migration:

**Database tables**:
- ✅ `order` - With `price` and `delai` fields
- ✅ `card` - Minimal card data (or removed if unused)
- ✅ `j_employee` - Employee planning data
- ✅ `j_planning` - Planning assignments
- ❌ `invoice` - REMOVED (data now in order.price)

**API Performance**:
- Sync 8,000 orders in ~10 seconds
- Sync 50,000 cards in ~30 seconds

**Data Quality**:
- Delai distribution: X (1), F+ (110), F (1,193), C (6,563), E (133)
- Price range: 0 to 500+ EUR
- Total cards: 1 to 150+ per order

---

## 🎉 Success!

You have successfully:
- ✅ Updated Spring Boot to use the new Symfony API
- ✅ Added `price` and `delai` fields to Order
- ✅ Removed `priority` field
- ✅ Cleaned up the database
- ✅ Simplified your architecture

**Next steps**: Use the `delai` field for planning prioritization!

---

## 📞 Support

If you encounter issues:
1. Check logs: `tail -f target/logs/spring.log`
2. Verify Symfony API: `curl http://localhost:8000/api/planning/export/health`
3. Test sync manually: `curl -X POST http://localhost:8080/api/sync/orders`
4. Review this guide step-by-step

**Good luck! 🚀**