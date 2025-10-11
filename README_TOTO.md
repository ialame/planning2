# 🔍 Debug: 0 Cards Synced

**Problem:** API returns `"synced_count": 0` even though it received 10 cards from Symfony.

**This means:** Cards were received but not saved to the database.

---

## 🎯 Root Cause

Cards need valid `order_id` references. If the orders don't exist in the database, cards will be skipped.

---

## ✅ Solution: Sync Orders First

### Step 1: Sync Orders Before Cards

```bash
# 1. First, sync orders
curl -X POST "http://localhost:8080/api/sync/orders" | jq

# Expected output:
{
  "success": true,
  "synced_count": 8000,  # ← Should show number of orders synced
  "message": "Synced 8000/8000 orders"
}
```

### Step 2: Now Sync Cards

```bash
# 2. Now sync cards (they'll find their orders)
curl -X POST "http://localhost:8080/api/sync/cards?limit=10" | jq

# Expected output:
{
  "success": true,
  "synced_count": 10,  # ← Should now be > 0
  "message": "Successfully synced 10/10 cards"
}
```

---

## 🔍 Verify the Problem

### Check if Orders Exist

```bash
# Count orders in database
mysql -u ia -pfoufafou dev-planning -e "SELECT COUNT(*) as order_count FROM \`order\`;"

# Expected: Should show > 0 orders
```

**If 0 orders:** Run order sync first!

### Check Symfony API Data

```bash
# See what Symfony is sending
curl "http://localhost:8000/api/planning/export/cards?limit=1" | jq

# Look for:
{
  "cards": [
    {
      "id": "...",
      "order_id": "018120f0621bf00c6a18f55ddd3b80d1",  # ← Card has order reference
      "card_name": "122/119",
      ...
    }
  ]
}
```

**If `order_id` is null or missing:** Problem is in Symfony API.

---

## 📊 Detailed Diagnostics

### Check Spring Boot Logs

```bash
# Look for warning messages
tail -f target/logs/spring.log | grep "⚠️"

# Common messages:
# ⚠️ Card has no order_id, skipping
# ⚠️ Order 018120f0... not found for card ..., skipping
```

### Check Sync Service Behavior

The `CardCertificationSyncService` skips cards if:

1. **No `id`**: Card has no certification ID
   ```
   ⚠️ Card has no id, skipping
   ```

2. **No `order_id`**: Card has no order reference
   ```
   ⚠️ Card 018120c2... has no order_id, skipping
   ```

3. **Order not found**: Referenced order doesn't exist in database
   ```
   ⚠️ Order 018120f0... not found for card ..., skipping
   ```

---

## 🚀 Complete Fix Procedure

### 1. Check Database Setup

```bash
# Verify tables exist
mysql -u ia -pfoufafou dev-planning -e "SHOW TABLES;"

# Should include:
# - order
# - card_certification
```

### 2. Run Database Setup (If Not Done)

```bash
# Setup card_certification table with sync columns
mysql -u ia -pfoufafou dev-planning < sql_setup_cards.sql

# Verify columns were added
mysql -u ia -pfoufafou dev-planning -e "DESCRIBE card_certification;" | grep symfony
```

### 3. Sync in Correct Order

```bash
# STEP 1: Sync orders (creates order records)
curl -X POST "http://localhost:8080/api/sync/orders" | jq '.synced_count'

# Should output a number > 0, e.g., 8000

# STEP 2: Sync cards (links to orders)
curl -X POST "http://localhost:8080/api/sync/cards?limit=100" | jq '.synced_count'

# Should now output a number > 0, e.g., 100
```

### 4. Verify Data

```bash
# Check orders
mysql -u ia -pfoufafou dev-planning -e "
SELECT COUNT(*) as total_orders FROM \`order\`;
"

# Check cards
mysql -u ia -pfoufafou dev-planning -e "
SELECT COUNT(*) as total_cards 
FROM card_certification 
WHERE symfony_certification_id IS NOT NULL;
"

# Check card-order links
mysql -u ia -pfoufafou dev-planning -e "
SELECT 
    c.card_name,
    HEX(c.order_id) as order_id,
    o.order_number
FROM card_certification c
LEFT JOIN \`order\` o ON c.order_id = o.id
WHERE c.symfony_certification_id IS NOT NULL
LIMIT 5;
"
```

---

## 🧪 Test Script

Create `test-card-sync-full.sh`:

```bash
#!/bin/bash

echo "🧪 Testing Card Sync (Complete)"

# Step 1: Check Symfony API
echo "1️⃣ Checking Symfony API..."
SYMFONY_CARDS=$(curl -sf "http://localhost:8000/api/planning/export/cards?limit=1" | jq -r '.count // 0')
echo "   Symfony has: $SYMFONY_CARDS cards available"

if [ "$SYMFONY_CARDS" -eq 0 ]; then
    echo "❌ No cards available in Symfony API"
    exit 1
fi

# Step 2: Check Spring Boot health
echo "2️⃣ Checking Spring Boot..."
HEALTH=$(curl -sf http://localhost:8080/api/sync/health | jq -r '.symfony_api // "disconnected"')
echo "   Symfony API: $HEALTH"

if [ "$HEALTH" != "connected" ]; then
    echo "❌ Cannot connect to Symfony via Spring Boot"
    exit 1
fi

# Step 3: Sync orders first
echo "3️⃣ Syncing orders..."
ORDER_SYNC=$(curl -sf -X POST "http://localhost:8080/api/sync/orders" | jq -r '.synced_count // 0')
echo "   Synced: $ORDER_SYNC orders"

if [ "$ORDER_SYNC" -eq 0 ]; then
    echo "⚠️  Warning: No orders synced"
fi

# Step 4: Count orders in database
echo "4️⃣ Checking database..."
DB_ORDERS=$(mysql -u ia -pfoufafou dev-planning -sNe "SELECT COUNT(*) FROM \`order\`;" 2>/dev/null || echo "0")
echo "   Orders in DB: $DB_ORDERS"

if [ "$DB_ORDERS" -eq 0 ]; then
    echo "❌ No orders in database - cards will fail to sync"
    exit 1
fi

# Step 5: Sync cards
echo "5️⃣ Syncing cards..."
CARD_SYNC=$(curl -sf -X POST "http://localhost:8080/api/sync/cards?limit=10" | jq -r '.synced_count // 0')
echo "   Synced: $CARD_SYNC cards"

if [ "$CARD_SYNC" -gt 0 ]; then
    echo "✅ SUCCESS! Cards synced successfully"
else
    echo "❌ FAILED: No cards synced"
    echo ""
    echo "Checking logs for errors..."
    tail -20 target/logs/spring.log | grep -E "⚠️|❌|ERROR"
fi

# Step 6: Verify cards in database
echo "6️⃣ Verifying database..."
DB_CARDS=$(mysql -u ia -pfoufafou dev-planning -sNe "SELECT COUNT(*) FROM card_certification WHERE symfony_certification_id IS NOT NULL;" 2>/dev/null || echo "0")
echo "   Cards in DB: $DB_CARDS"

if [ "$DB_CARDS" -gt 0 ]; then
    echo ""
    echo "✅ All tests passed!"
    echo ""
    echo "Sample cards:"
    mysql -u ia -pfoufafou dev-planning -e "
    SELECT 
        SUBSTRING(symfony_certification_id, 1, 12) as cert_id,
        card_name,
        grading_completed,
        certification_completed,
        scanning_completed,
        packaging_completed
    FROM card_certification
    WHERE symfony_certification_id IS NOT NULL
    LIMIT 5;" 2>/dev/null
else
    echo "❌ No cards found in database"
fi
```

Run it:
```bash
chmod +x test-card-sync-full.sh
./test-card-sync-full.sh
```

---

## 🔧 Common Fixes

### Problem 1: "Order not found"

**Cause:** Orders not synced yet.

**Fix:**
```bash
curl -X POST "http://localhost:8080/api/sync/orders"
```

### Problem 2: "Card has no order_id"

**Cause:** Symfony API not returning `order_id` field.

**Fix:** Check Symfony export endpoint:
```bash
curl "http://localhost:8000/api/planning/export/cards?limit=1" | jq '.cards[0] | keys'

# Should include "order_id"
```

### Problem 3: Database columns missing

**Cause:** `sql_setup_cards.sql` not run.

**Fix:**
```bash
mysql -u ia -pfoufafou dev-planning < sql_setup_cards.sql
```

### Problem 4: CardCertification requires more fields

**Cause:** Entity has required fields (@NotNull) that we're not setting.

**Fix:** Check which fields are required:
```bash
mysql -u ia -pfoufafou dev-planning -e "DESCRIBE card_certification;" | grep "NOT NULL"
```

If there are required fields missing, we need to either:
- Set them with default values in the sync service
- Make them nullable in the database

---

## 📈 Expected Results

After fixing:

```bash
# Sync orders
curl -X POST "http://localhost:8080/api/sync/orders" | jq
```
```json
{
  "success": true,
  "synced_count": 8234,
  "message": "Synced 8234/8234 orders"
}
```

```bash
# Sync cards
curl -X POST "http://localhost:8080/api/sync/cards?limit=100" | jq
```
```json
{
  "success": true,
  "synced_count": 100,
  "total_cards": 100,
  "error_count": 0,
  "message": "Successfully synced 100/100 cards",
  "stats": {
    "total_cards": 100,
    "incomplete_cards": 45,
    "completion_percentage": 55.0
  }
}
```

---

## 💡 Pro Tip

Always use the `/api/sync/all` endpoint for convenience:

```bash
# Syncs both orders and cards in correct order
curl -X POST "http://localhost:8080/api/sync/all" | jq

# Response includes both results:
{
  "success": true,
  "orders": {
    "synced_count": 8234
  },
  "cards": {
    "synced_count": 125000
  }
}
```

---

## 🎯 Quick Checklist

When you see `synced_count: 0`:

- [ ] Database setup complete (`sql_setup_cards.sql` run)
- [ ] Orders synced first (`/api/sync/orders`)
- [ ] Orders exist in database (query `order` table)
- [ ] Symfony API returns cards with `order_id` field
- [ ] Spring Boot logs don't show "⚠️ Order not found" warnings
- [ ] CardCertification table has required columns

**Fix in this order:**
1. Setup database
2. Sync orders
3. Sync cards
4. Verify data

**You should be syncing successfully now! 🚀**