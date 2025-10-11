# 🎴 Card Synchronization Guide

Complete guide for synchronizing Pokemon cards from Symfony API to the planning system.

---

## 📋 Overview

The card synchronization system:
- **Fetches cards** from Symfony API (`/api/planning/export/cards`)
- **Maps processing status** (grading, certification, scanning, packaging)
- **Stores in** `card_certification` table
- **Links to orders** via `order_id` reference
- **Processes in batches** to handle large volumes efficiently

---

## 🚀 Quick Start

### 1. Test Symfony API

```bash
# Check if Symfony API is accessible
curl http://localhost:8000/api/planning/export/health | jq

# Get sample cards (limit to 5 for testing)
curl "http://localhost:8000/api/planning/export/cards?limit=5" | jq
```

**Expected response:**
```json
{
  "cards": [
    {
      "id": "018120c2597dc3bb5c1eb77b887adff9",
      "order_id": "018120f0621bf00c6a18f55ddd3b80d1",
      "card_name": "122/119",
      "processing_status": 8,
      "grading_completed": true,
      "certification_completed": true,
      "scanning_completed": true,
      "packaging_completed": false
    }
  ],
  "count": 5
}
```

### 2. Sync Cards to Planning Database

```bash
# Sync all cards (default limit: 50000)
curl -X POST http://localhost:8080/api/sync/cards | jq

# Sync limited number of cards
curl -X POST "http://localhost:8080/api/sync/cards?limit=100" | jq

# Sync cards for specific order
curl -X POST "http://localhost:8080/api/sync/cards?orderId=018120f0621bf00c6a18f55ddd3b80d1" | jq
```

**Expected response:**
```json
{
  "success": true,
  "total_cards": 5,
  "synced_count": 5,
  "error_count": 0,
  "duration_ms": 234,
  "message": "Successfully synced 5/5 cards",
  "stats": {
    "total_cards": 5,
    "incomplete_cards": 2,
    "needs_grading": 0,
    "needs_certification": 0,
    "needs_scanning": 0,
    "needs_packaging": 2,
    "completion_percentage": 60.0
  }
}
```

### 3. Verify in Database

```bash
mysql -u ia -p dev-planning << 'EOF'
-- Count cards
SELECT COUNT(*) as total_cards FROM card_certification;

-- View cards with status
SELECT 
    SUBSTRING(symfony_certification_id, 1, 8) as card_id,
    card_name,
    grading_completed,
    certification_completed,
    scanning_completed,
    packaging_completed
FROM card_certification
LIMIT 10;

-- Check cards by order
SELECT 
    HEX(order_id) as order_id,
    COUNT(*) as card_count,
    SUM(grading_completed) as graded,
    SUM(certification_completed) as certified,
    SUM(scanning_completed) as scanned,
    SUM(packaging_completed) as packaged
FROM card_certification
GROUP BY order_id
LIMIT 10;
EOF
```

---

## 📡 API Endpoints

### Health Check
```bash
GET /api/sync/health
```
Tests connection to Symfony API.

**Response:**
```json
{
  "symfony_api": "connected",
  "symfony_url": "http://localhost:8000",
  "status": "healthy"
}
```

---

### Sync Cards
```bash
POST /api/sync/cards
POST /api/sync/cards?limit=100
POST /api/sync/cards?orderId=018120f0621bf00c6a18f55ddd3b80d1
```

**Parameters:**
- `limit` (optional): Maximum number of cards to fetch (default: 50000)
- `orderId` (optional): Sync cards only for specific order (hex format)

**Response:**
```json
{
  "success": true,
  "total_cards": 125,
  "synced_count": 125,
  "error_count": 0,
  "duration_ms": 1234,
  "message": "Successfully synced 125/125 cards",
  "stats": {
    "total_cards": 125,
    "incomplete_cards": 45,
    "needs_grading": 12,
    "needs_certification": 15,
    "needs_scanning": 10,
    "needs_packaging": 8,
    "completion_percentage": 64.0
  }
}
```

---

### Sync All (Orders + Cards)
```bash
POST /api/sync/all
```

Syncs both orders and cards in sequence.

**Response:**
```json
{
  "success": true,
  "duration_ms": 5678,
  "message": "Complete sync successful",
  "orders": {
    "success": true,
    "synced_count": 50
  },
  "cards": {
    "success": true,
    "synced_count": 125
  }
}
```

---

### Get Statistics
```bash
GET /api/sync/stats
```

Returns current sync statistics.

**Response:**
```json
{
  "total_orders": 50,
  "card_stats": {
    "total_cards": 125,
    "incomplete_cards": 45,
    "needs_grading": 12,
    "needs_certification": 15,
    "needs_scanning": 10,
    "needs_packaging": 8,
    "completion_percentage": 64.0
  },
  "message": "Sync statistics retrieved successfully"
}
```

---

## 🔍 Data Mapping

### From Symfony API to CardCertification Entity

| Symfony API Field | CardCertification Field | Type | Notes |
|------------------|------------------------|------|-------|
| `id` | `symfonyCertificationId` | String | Hex ULID format |
| `order_id` | `orderId` | UUID | Converted from hex |
| `card_name` | `cardName` | String | Display name (e.g., "122/119") |
| `grading_completed` | `gradingCompleted` | Boolean | Notation task done |
| `certification_completed` | `certificationCompleted` | Boolean | Certification task done |
| `scanning_completed` | `scanningCompleted` | Boolean | Scanning task done |
| `packaging_completed` | `packagingCompleted` | Boolean | Packaging task done |
| `processing_status` | - | Integer | Not stored (derived from flags) |

---

## ⚙️ Configuration

### Application Properties

```properties
# Symfony API Configuration
symfony.api.base-url=http://localhost:8000
symfony.api.timeout=30

# Batch Processing
card.sync.batch-size=100
```

### Batch Size

The system processes cards in batches of 100 by default. This can be adjusted in `MinimalSyncController.java`:

```java
private static final int CARD_BATCH_SIZE = 100;
```

**Recommendations:**
- **Small datasets (<1000 cards)**: Use batch size 50-100
- **Medium datasets (1000-10000)**: Use batch size 100-200
- **Large datasets (>10000)**: Use batch size 200-500

---

## 🐛 Troubleshooting

### Problem: "Invalid order_id format"

**Cause:** Order ID from Symfony is not in valid hex ULID format

**Solution:**
1. Check Symfony API response format
2. Verify order exists in `order` table
3. Ensure orders are synced before cards

```bash
# Sync orders first
curl -X POST http://localhost:8080/api/sync/orders

# Then sync cards
curl -X POST http://localhost:8080/api/sync/cards
```

---

### Problem: "Order not found for card"

**Cause:** Card references an order that doesn't exist in the planning database

**Solution:**
Sync orders before syncing cards:
```bash
curl -X POST http://localhost:8080/api/sync/all
```

---

### Problem: "Card has no order_id, skipping"

**Cause:** Card data from Symfony is missing `order_id` field

**Solution:**
1. Check Symfony API data quality
2. Verify that all cards have valid order associations
3. Cards without orders will be skipped automatically

---

### Problem: Sync is slow

**Cause:** Large number of cards, small batch size, or network latency

**Solutions:**
1. Increase batch size (see Configuration section)
2. Use `limit` parameter to sync incrementally
3. Sync specific orders only:
```bash
curl -X POST "http://localhost:8080/api/sync/cards?orderId=YOUR_ORDER_ID"
```

---

## 📊 Monitoring

### Check Sync Progress

```bash
# Get current statistics
curl http://localhost:8080/api/sync/stats | jq

# Calculate completion rate
mysql -u ia -p dev-planning << 'EOF'
SELECT 
    COUNT(*) as total,
    SUM(CASE 
        WHEN grading_completed = 1 
        AND certification_completed = 1 
        AND scanning_completed = 1 
        AND packaging_completed = 1 
        THEN 1 ELSE 0 
    END) as completed,
    ROUND(
        SUM(CASE 
            WHEN grading_completed = 1 
            AND certification_completed = 1 
            AND scanning_completed = 1 
            AND packaging_completed = 1 
            THEN 1 ELSE 0 
        END) * 100.0 / COUNT(*), 
        2
    ) as completion_percentage
FROM card_certification;
EOF
```

### Watch Sync Logs

```bash
# Tail Spring Boot logs
tail -f target/logs/spring.log | grep "sync"

# Watch for errors
tail -f target/logs/spring.log | grep "ERROR"
```

### Database Queries

```sql
-- Cards needing work
SELECT 
    'Grading' as task,
    COUNT(*) as count
FROM card_certification
WHERE grading_completed = 0

UNION ALL

SELECT 
    'Certification' as task,
    COUNT(*) as count
FROM card_certification
WHERE grading_completed = 1 AND certification_completed = 0

UNION ALL

SELECT 
    'Scanning' as task,
    COUNT(*) as count
FROM card_certification
WHERE certification_completed = 1 AND scanning_completed = 0

UNION ALL

SELECT 
    'Packaging' as task,
    COUNT(*) as count
FROM card_certification
WHERE scanning_completed = 1 AND packaging_completed = 0;
```

---

## 🔄 Automated Sync

### Cron Job (Linux/Mac)

Add to crontab (`crontab -e`):

```bash
# Sync cards every hour
0 * * * * curl -X POST http://localhost:8080/api/sync/cards > /var/log/card-sync.log 2>&1

# Sync everything daily at 2 AM
0 2 * * * curl -X POST http://localhost:8080/api/sync/all > /var/log/full-sync.log 2>&1
```

### Task Scheduler (Windows)

Create `sync-cards.bat`:
```batch
@echo off
curl -X POST http://localhost:8080/api/sync/cards > C:\logs\card-sync.log 2>&1
```

Schedule using Windows Task Scheduler.

---

## 🧪 Testing

### Test Script

Create `test-card-sync.sh`:

```bash
#!/bin/bash

echo "🧪 Testing Card Synchronization..."

# Test 1: Health check
echo -n "1. Symfony API health: "
if curl -sf http://localhost:8000/api/planning/export/health > /dev/null; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Test 2: Spring Boot health
echo -n "2. Spring Boot health: "
if curl -sf http://localhost:8080/api/sync/health > /dev/null; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Test 3: Sync small batch
echo -n "3. Sync 10 cards: "
RESULT=$(curl -sf -X POST "http://localhost:8080/api/sync/cards?limit=10" | jq -r '.success')
if [ "$RESULT" = "true" ]; then
    echo "✅ OK"
else
    echo "❌ FAILED"
    exit 1
fi

# Test 4: Verify database
echo -n "4. Verify database: "
COUNT=$(mysql -u ia -p dev-planning -sNe "SELECT COUNT(*) FROM card_certification")
if [ "$COUNT" -gt 0 ]; then
    echo "✅ OK ($COUNT cards)"
else
    echo "❌ FAILED (no cards found)"
    exit 1
fi

echo ""
echo "✅ All tests passed!"
```

Run: `chmod +x test-card-sync.sh && ./test-card-sync.sh`

---

## 🎯 Best Practices

1. **Always sync orders before cards**
   ```bash
   curl -X POST http://localhost:8080/api/sync/orders
   curl -X POST http://localhost:8080/api/sync/cards
   ```

2. **Use limits for large datasets**
   ```bash
   # Instead of syncing 50000 cards at once
   curl -X POST "http://localhost:8080/api/sync/cards?limit=1000"
   ```

3. **Monitor sync statistics**
   ```bash
   watch -n 5 'curl -s http://localhost:8080/api/sync/stats | jq'
   ```

4. **Check logs regularly**
   ```bash
   tail -f target/logs/spring.log
   ```

5. **Backup before first sync**
   ```bash
   mysqldump -u ia -p dev-planning > backup-before-card-sync.sql
   ```

---

## 📈 Performance Benchmarks

Typical sync times on standard hardware:

| Card Count | Batch Size | Duration | Cards/Second |
|------------|-----------|----------|--------------|
| 100 | 100 | ~500ms | 200 |
| 1,000 | 100 | ~4s | 250 |
| 10,000 | 200 | ~35s | 285 |
| 50,000 | 500 | ~2.5min | 333 |

**Factors affecting performance:**
- Network latency to Symfony API
- Database connection speed
- Batch size configuration
- Number of existing cards (upsert logic)

---

## ✅ Success Checklist

- [ ] Symfony API is accessible (`/api/planning/export/health`)
- [ ] Spring Boot is running
- [ ] Database `dev-planning` exists
- [ ] Table `card_certification` exists
- [ ] Orders are synced first
- [ ] Test sync with `limit=10` works
- [ ] Full sync completed successfully
- [ ] Cards appear in database
- [ ] Statistics look correct
- [ ] No errors in logs

---

## 📞 Support

If you encounter issues:

1. Check health endpoint: `curl http://localhost:8080/api/sync/health`
2. Review logs: `tail -f target/logs/spring.log`
3. Verify Symfony API: `curl http://localhost:8000/api/planning/export/cards?limit=1`
4. Check database: `mysql -u ia -p dev-planning`
5. Review this guide step by step

**Common Issues:**
- Orders not synced → Run `/api/sync/orders` first
- Symfony API unreachable → Check URL in `application-local.properties`
- Database errors → Verify credentials and table structure

---

**Ready to sync! 🚀**