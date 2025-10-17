# 🔄 Data Synchronization Setup Guide

## Overview

This guide explains how to synchronize data from your **Symfony backend (dev)** to your **Spring Boot planning system (dev-planning)**.

---

## 📋 Architecture

```
┌─────────────────┐           ┌──────────────────┐
│   Symfony App   │           │  Spring Boot App │
│   (Backend)     │           │   (Planning)     │
│                 │           │                  │
│   Database:     │   Sync    │   Database:      │
│   "dev"         │ ────────> │   "dev-planning" │
│                 │           │                  │
│  - orders       │           │  - orders        │
│  - cards        │           │  - cards         │
│  - etc.         │           │  - planning    │
└─────────────────┘           └──────────────────┘
```

**Key Points:**
- **dev** database is managed by Symfony (read-only for Spring Boot)
- **dev-planning** database is managed by Spring Boot (read/write)
- Synchronization copies data from dev → dev-planning
- Planning tables (employee, planning, etc.) only exist in dev-planning

---

## 🚀 Setup Instructions

### Step 1: Add Sync Controller to Backend

Copy `DataSyncController.java` to:
```
src/main/java/com/pcagrade/order/controller/DataSyncController.java
```

### Step 2: Configure Database Access

Your Spring Boot app needs access to **both** databases.

**Option A: Update application-local.properties**
```properties
# Main database (dev-planning)
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=ia
spring.datasource.password=foufafou

# The "dev" database is accessible via cross-database queries
# MySQL/MariaDB allows: SELECT * FROM dev.table_name
```

**Option B: Multiple DataSource (Advanced)**

If you want separate connections, configure two datasources:
```java
@Configuration
public class DataSourceConfig {
    @Primary
    @Bean(name = "planningDataSource")
    public DataSource planningDataSource() {
        // dev-planning database
    }
    
    @Bean(name = "devDataSource")
    public DataSource devDataSource() {
        // dev database (read-only)
    }
}
```

For this guide, **Option A is simpler and recommended**.

### Step 3: Add Frontend Component (Optional)

1. Copy `DataSync.vue` to:
   ```
   src/main/frontend/src/views/DataSync.vue
   ```

2. Add route in `src/main/frontend/src/router/index.ts`:
   ```typescript
   {
     path: '/sync',
     name: 'DataSync',
     component: () => import('../views/DataSync.vue')
   }
   ```

3. Add menu item in navigation:
   ```vue
   <router-link to="/sync">🔄 Data Sync</router-link>
   ```

### Step 4: Restart Backend

```bash
./mvnw spring-boot:run
```

---

## 📡 API Endpoints

### Check Sync Status
```bash
GET /api/sync/status
```

**Response:**
```json
{
  "success": true,
  "tableComparison": [
    {
      "table": "order",
      "devCount": 1500,
      "devPlanningCount": 1500,
      "difference": 0,
      "inSync": true
    },
    ...
  ],
  "timestamp": "2025-10-07T10:30:00"
}
```

### Sync All Tables
```bash
POST /api/sync/all
```

**Response:**
```json
{
  "success": true,
  "message": "✅ Full synchronization completed",
  "syncedTables": [
    "Orders: 0 deleted, 1500 inserted",
    "Cards: 0 deleted, 50000 inserted",
    ...
  ],
  "timestamp": "2025-10-07T10:30:00"
}
```

### Sync Orders Only
```bash
POST /api/sync/orders
```

### Sync Cards Only
```bash
POST /api/sync/cards
```

### Incremental Sync
```bash
POST /api/sync/incremental?since=2025-10-01
```

Syncs only records modified after specified date.

---

## 💻 Usage Examples

### Via cURL

**Check sync status:**
```bash
curl http://localhost:8080/api/sync/status
```

**Sync all data:**
```bash
curl -X POST http://localhost:8080/api/sync/all
```

**Sync only orders:**
```bash
curl -X POST http://localhost:8080/api/sync/orders
```

### Via SQL Script

Run the manual sync script:
```bash
mysql -u ia -pfoufafou < sync_data.sql
```

### Via Frontend

1. Navigate to: `http://localhost:3000/sync`
2. Click "🔍 Check Status" to see current state
3. Click "🚀 Sync All Data" to synchronize
4. View sync history at the bottom

---

## 🔄 Synchronization Strategies

### Strategy 1: Manual Sync (On-Demand)

**When to use:**
- Development environment
- Small datasets
- When you have full control

**How:**
- Use frontend interface or cURL to trigger sync
- Run SQL script manually

**Pros:** Simple, full control  
**Cons:** Must remember to sync

---

### Strategy 2: Scheduled Sync (Cron)

**When to use:**
- Production environment
- Regular data updates
- Automated workflow

**Setup:**

**Option A: Linux Cron**
```bash
# Edit crontab
crontab -e

# Add: Sync every 6 hours
0 */6 * * * curl -X POST http://localhost:8080/api/sync/all
```

**Option B: Spring Scheduler**

Add to your Spring Boot app:
```java
@Component
@EnableScheduling
public class DataSyncScheduler {
    
    @Autowired
    private DataSyncController syncController;
    
    // Run every 6 hours
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledSync() {
        log.info("🔄 Starting scheduled sync");
        syncController.syncAll();
    }
}
```

**Pros:** Automated, reliable  
**Cons:** May sync unchanged data

---

### Strategy 3: Incremental Sync

**When to use:**
- Large datasets
- Frequent updates
- Performance-sensitive

**Setup:**
```bash
# Sync only recent changes (last 24 hours)
curl -X POST http://localhost:8080/api/sync/incremental
```

**Schedule:**
```bash
# Every hour - incremental
0 * * * * curl -X POST http://localhost:8080/api/sync/incremental

# Once per day - full sync
0 3 * * * curl -X POST http://localhost:8080/api/sync/all
```

**Pros:** Fast, efficient  
**Cons:** More complex, requires modification date tracking

---

### Strategy 4: Real-Time Sync (Advanced)

**When to use:**
- Mission-critical data
- Real-time planning requirements

**Implementation options:**

**Option A: Database Triggers**
```sql
-- Trigger on dev.order insert/update
CREATE TRIGGER sync_order_to_planning
AFTER INSERT ON dev.card_order
FOR EACH ROW
BEGIN
    INSERT INTO `dev-planning`.card_order VALUES (NEW.*);
END;
```

**Option B: Message Queue**
```
Symfony → RabbitMQ/Kafka → Spring Boot
         (on data change)    (sync handler)
```

**Option C: API Webhook**

In Symfony, after order create/update:
```php
// Call Spring Boot sync endpoint
$client->post('http://spring-boot:8080/api/sync/orders');
```

**Pros:** Near real-time, no delay  
**Cons:** Complex setup, potential performance impact

---

## ⚙️ Configuration Options

### Sync Behavior

Control what gets synced by modifying `DataSyncController.java`:

**Selective table sync:**
```java
// Sync only specific statuses
String sql = """
    INSERT INTO card_order 
    SELECT * FROM dev.card_order 
    WHERE status IN (2, 3, 4, 10)
    """;
```

**Preserve existing data:**
```java
// Use INSERT IGNORE or ON DUPLICATE KEY UPDATE
String sql = """
    INSERT INTO card_order 
    SELECT * FROM dev.card_order
    ON DUPLICATE KEY UPDATE
        status = VALUES(status),
        date_modification = VALUES(date_modification)
    """;
```

### Error Handling

Add retry logic:
```java
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))
public String syncOrders() {
    // sync logic
}
```

### Logging

Configure detailed sync logs in `application.properties`:
```properties
logging.level.com.pcagrade.order.controller.DataSyncController=DEBUG
```

---

## 🔍 Monitoring & Verification

### Check Sync Status Regularly

```bash
# Via API
curl http://localhost:8080/api/sync/status | jq

# Via SQL
mysql -u ia -pfoufafou << 'EOF'
SELECT 
    'order' as table_name,
    (SELECT COUNT(*) FROM dev.card_order) as dev,
    (SELECT COUNT(*) FROM `dev-planning`.card_order) as planning,
    (SELECT COUNT(*) FROM dev.card_order) - 
    (SELECT COUNT(*) FROM `dev-planning`.card_order) as diff
UNION ALL
SELECT 'card',
    (SELECT COUNT(*) FROM dev.`card`),
    (SELECT COUNT(*) FROM `dev-planning`.`card`),
    (SELECT COUNT(*) FROM dev.`card`) - 
    (SELECT COUNT(*) FROM `dev-planning`.`card`);
EOF
```

### Alert on Sync Failures

Set up monitoring:
```bash
# Check sync status and alert if out of sync
#!/bin/bash
RESPONSE=$(curl -s http://localhost:8080/api/sync/status)
if echo "$RESPONSE" | grep -q '"inSync":false'; then
    echo "⚠️ ALERT: Databases out of sync!"
    # Send email, Slack notification, etc.
fi
```

---

## 🐛 Troubleshooting

### Problem: "Cannot access dev database"

**Solution:** Grant cross-database access
```sql
GRANT SELECT ON dev.* TO 'ia'@'localhost';
FLUSH PRIVILEGES;
```

### Problem: "Foreign key constraint fails"

**Solution:** Sync in correct order (parent tables first)
1. order
2. card
3. card_certification
4. card_translation
5. card_certification_order

Or disable foreign key checks temporarily:
```sql
SET FOREIGN_KEY_CHECKS = 0;
-- sync
SET FOREIGN_KEY_CHECKS = 1;
```

### Problem: "Sync takes too long"

**Solutions:**
1. Use incremental sync instead of full sync
2. Sync during off-peak hours
3. Add indexes on modification_date columns
4. Use smaller batch sizes

### Problem: "Data inconsistency after sync"

**Solutions:**
1. Always sync all related tables together
2. Use transactions
3. Verify foreign key relationships
4. Check for orphaned records

---

## 📊 Performance Considerations

### Large Datasets

For databases with millions of records:

**Option 1: Batch Processing**
```java
// Sync in chunks
int batchSize = 1000;
for (int offset = 0; offset < totalRecords; offset += batchSize) {
    String sql = String.format(
        "INSERT INTO card_order SELECT * FROM dev.card_order LIMIT %d, %d",
        offset, batchSize
    );
    // execute
}
```

**Option 2: Parallel Processing**
```java
@Async
public CompletableFuture<String> syncOrdersAsync() {
    return CompletableFuture.completedFuture(syncOrders());
}

// Sync multiple tables in parallel
CompletableFuture.allOf(
    syncOrdersAsync(),
    syncCardsAsync(),
    syncTranslationsAsync()
).join();
```

### Network Considerations

If databases are on different servers:
- Use `mysqldump` + import
- Setup database replication
- Use CDC (Change Data Capture) tools

---

## 🎯 Best Practices

1. **Always check sync status before generating planning**
2. **Schedule syncs during low-traffic periods**
3. **Monitor sync history and errors**
4. **Keep sync logs for troubleshooting**
5. **Test sync on staging before production**
6. **Have a rollback plan**
7. **Document your sync schedule**

---

## 🔐 Security Considerations

1. **Read-only access:** Grant Spring Boot only SELECT on `dev` database
   ```sql
   GRANT SELECT ON dev.* TO 'planning_user'@'%';
   ```

2. **API authentication:** Protect sync endpoints
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   @PostMapping("/sync/all")
   public ResponseEntity<Map<String, Object>> syncAll() {...}
   ```

3. **Rate limiting:** Prevent abuse
4. **Audit logging:** Track who triggered syncs

---

## 📞 Support

For issues:
1. Check sync status: `/api/sync/status`
2. View backend logs: `tail -f logs/spring-boot-application.log`
3. Test SQL script manually: `sync_data.sql`
4. Verify database connectivity

---

## 🎉 Quick Start Summary

1. **Add controller:** Copy `DataSyncController.java`
2. **Restart backend:** `./mvnw spring-boot:run`
3. **Test sync:** `curl -X POST http://localhost:8080/api/sync/all`
4. **Verify:** `curl http://localhost:8080/api/sync/status`
5. **Schedule:** Set up cron job or Spring scheduler

**You're ready to go!** 🚀