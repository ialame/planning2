# 📊 Database Tables Overview - Pokemon Card Planning

## 🎯 Table Management Strategy

This document defines which tables are managed by Liquibase and which are read-only.

---

## ✅ Tables Managed by Liquibase (Writable)

These tables are **created and modified** by Liquibase. Your application has **full read/write** access.

| Table | Purpose | Entity | Liquibase File |
|-------|---------|--------|----------------|
| `j_employee` | Employee management | `Employee.java` | `001-create-employee-table.yml` |
| `j_planning` | Order-employee assignments | `Planning.java` | `002-create-planning-table.yml` |
| `j_group` | User groups/roles | `Group.java` | `003-create-group-table.yml` |
| `j_employee_group` | Employee-group relationships | (Junction table) | `004-create-employee-group-table.yml` |
| `j_status_workflow` | Status workflow definitions | `StatusWorkflow.java` | `005-create-status-workflow-table.yml` |
| `j_delai_priority_mapping` | Priority code mappings | `DelaiPriorityMapping.java` | `006-create-delai-priority-mapping.yml` |

**Naming Convention:** All planning system tables start with `j_` prefix

**Operations Allowed:**
```sql
✅ SELECT
✅ INSERT
✅ UPDATE
✅ DELETE
```

**Entity Annotation:**
```java
@Entity
@Table(name = "j_employee")
// NO @Immutable annotation
@Data
public class Employee extends AbstractUlidEntity {
    // Full read/write entity
}
```

---

## 🔒 Tables Read-Only (Shared with Other Projects)

These tables are **NOT managed** by Liquibase. Your application has **read-only** access.

### Core Order Management

| Table | Purpose | Owner | Access |
|-------|---------|-------|--------|
| `order` | Main order table | Symfony project | 🔒 Read-only |

**Why read-only:**
- Shared with Symfony project (legacy system)
- Contains orders since 2016
- Modified by external system
- Critical business data

**Data Source:** Synced from `dev` database

**Filter:** Only orders since 2025-06-01 for planning

---

### Card Catalog (Shared)

| Table | Purpose | Owner | Access |
|-------|---------|-------|--------|
| `card` | Pokemon card catalog | Shared catalog | 🔒 Read-only |
| `card_translation` | Card name translations | Shared catalog | 🔒 Read-only |
| `card_certification` | Card grading/certification | Certification system | 🔒 Read-only |
| `card_certification_order` | Links certifications to orders | Certification system | 🔒 Read-only |

**Why read-only:**
- Shared across multiple projects
- Master card database
- Modified by external systems
- Reference data only

**Data Source:** Synced from `dev` database

---

### Financial Data

| Table | Purpose | Owner | Access |
|-------|---------|-------|--------|
| `invoice` | Billing and invoices | Financial system | 🔒 Read-only |

**Why read-only:**
- Managed by financial/accounting system
- Contains sensitive billing data
- Modified by external system
- Audit trail requirements

**Data Source:** Synced from `dev` database or external system

---

## 📋 Complete Table Summary

### By Management Type

```
LIQUIBASE-MANAGED (6 tables):
├── j_employee                 ✅ Read/Write
├── j_planning                 ✅ Read/Write
├── j_group                    ✅ Read/Write
├── j_employee_group           ✅ Read/Write
├── j_status_workflow          ✅ Read/Write
└── j_delai_priority_mapping   ✅ Read/Write

READ-ONLY (6 tables):
├── order                      🔒 Read-only (Symfony)
├── card                       🔒 Read-only (Catalog)
├── card_translation           🔒 Read-only (Catalog)
├── card_certification         🔒 Read-only (Certification)
├── card_certification_order   🔒 Read-only (Certification)
└── invoice                    🔒 Read-only (Financial)

TOTAL: 12 tables
```

### By Data Source

```
CREATED BY LIQUIBASE:
└── All j_* tables

SYNCED FROM dev DATABASE:
├── order
├── card
├── card_translation
├── card_certification
├── card_certification_order
└── invoice
```

---

## 🔧 Entity Configuration

### Writable Entities (Liquibase-Managed)

```java
/**
 * Employee Entity
 * ✅ Managed by Liquibase
 * ✅ Full read/write access
 */
@Entity
@Table(name = "j_employee")
@Data
@EqualsAndHashCode(callSuper = true)
public class Employee extends AbstractUlidEntity {
    
    @Column(name = "first_name", nullable = false)
    private String firstName;
    
    @Column(name = "last_name", nullable = false)
    private String lastName;
    
    // ... other fields
}
```

### Read-Only Entities (Shared)

```java
/**
 * Order Entity
 * 🔒 Read-only (shared with Symfony)
 * ❌ No write operations allowed
 */
@Entity
@Table(name = "`order`")
@Immutable  // ← CRITICAL: Marks entity as read-only
@Data
@EqualsAndHashCode(callSuper = true)
public class Order extends AbstractUlidEntity {
    
    @Column(name = "num_commande", insertable = false, updatable = false)
    private String orderNumber;
    
    @Column(name = "nombre_cartes", insertable = false, updatable = false)
    private Integer cardCount;
    
    // ALL fields must have insertable=false, updatable=false
}
```

```java
/**
 * Card Entity
 * 🔒 Read-only (shared catalog)
 */
@Entity
@Table(name = "card")
@Immutable
@Data
public class Card extends AbstractUlidEntity {
    
    @Column(name = "name", insertable = false, updatable = false)
    private String name;
    
    // ... other fields
}
```

```java
/**
 * CardCertification Entity
 * 🔒 Read-only (certification system)
 */
@Entity
@Table(name = "card_certification")
@Immutable
@Data
public class CardCertification extends AbstractUlidEntity {
    // Read-only entity for certification data
}
```

```java
/**
 * Invoice Entity
 * 🔒 Read-only (financial system)
 */
@Entity
@Table(name = "invoice")
@Immutable
@Data
public class Invoice extends AbstractUlidEntity {
    
    @Column(name = "invoice_number", insertable = false, updatable = false)
    private String invoiceNumber;
    
    @Column(name = "amount", insertable = false, updatable = false)
    private BigDecimal amount;
    
    // ... other fields
}
```

---

## 🚨 Critical Rules

### DO ✅

```
✅ Use Liquibase for j_* tables
✅ Mark shared tables with @Immutable
✅ Use insertable=false, updatable=false on shared table columns
✅ Only SELECT from shared tables
✅ Sync data regularly from dev database
✅ Test read-only enforcement
```

### DON'T ❌

```
❌ DON'T create Liquibase changelogs for shared tables
❌ DON'T try to INSERT/UPDATE/DELETE in shared tables
❌ DON'T remove @Immutable from shared entities
❌ DON'T add foreign keys to shared tables
❌ DON'T modify shared table structure
❌ DON'T assume shared data is always current (sync may be delayed)
```

---

## 🔄 Data Synchronization

### How Shared Tables Get Updated

```
dev database (Source)
    ↓
    ↓ Sync via SQL or API
    ↓
dev-planning database (Replica)
    ↓
    ↓ Read by Spring Boot
    ↓
Planning Application (Consumer)
```

### Sync Frequency

| Table | Sync Method | Frequency | Notes |
|-------|-------------|-----------|-------|
| `order` | SQL script or API | Daily / On-demand | Filter: date >= 2025-06-01 |
| `card` | SQL script | Weekly | Stable reference data |
| `card_translation` | SQL script | Weekly | Stable reference data |
| `card_certification` | SQL script or API | Daily | Updated by certification system |
| `card_certification_order` | SQL script or API | Daily | Links to orders |
| `invoice` | SQL script or API | Daily | Financial data |

### Sync Endpoints

```bash
# Sync all shared tables
curl -X POST http://localhost:8080/api/sync/all

# Sync orders only
curl -X POST http://localhost:8080/api/sync/orders

# Sync cards only
curl -X POST http://localhost:8080/api/sync/cards

# Check sync status
curl http://localhost:8080/api/sync/status
```

---

## 🧪 Testing Read-Only Enforcement

### Test Script

```java
@SpringBootTest
public class ReadOnlyEnforcementTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    @Transactional
    public void testOrderIsReadOnly() {
        // Attempt to create order
        Order order = new Order();
        order.setOrderNumber("TEST-123");
        order.setCardCount(10);
        
        // This should throw exception or be silently ignored
        assertThrows(Exception.class, () -> {
            orderRepository.save(order);
        });
    }
    
    @Test
    public void testOrderCanBeRead() {
        // Reading should work
        List<Order> orders = orderRepository.findAll();
        assertNotNull(orders);
    }
}
```

### Manual Test

```bash
# Try to insert into order table (should fail)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderNumber": "TEST-123", "cardCount": 10}'

# Expected: 400 Bad Request or error about read-only entity
```

---

## 📊 Database Size Estimation

### Planning Tables (Liquibase-Managed)

```
j_employee:              ~50 rows        (~10 KB)
j_planning:              ~10,000 rows    (~2 MB)
j_group:                 ~10 rows        (~1 KB)
j_employee_group:        ~100 rows       (~5 KB)
j_status_workflow:       ~20 rows        (~2 KB)
j_delai_priority_mapping: ~5 rows        (~1 KB)

TOTAL: ~2 MB
```

### Shared Tables (Read-Only)

```
order:                   ~100,000 rows   (~20 MB)
card:                    ~500,000 rows   (~100 MB)
card_translation:        ~2,000,000 rows (~400 MB)
card_certification:      ~50,000 rows    (~10 MB)
card_certification_order: ~50,000 rows   (~5 MB)
invoice:                 ~30,000 rows    (~6 MB)

TOTAL: ~541 MB
```

**Total Database Size:** ~543 MB

---

## 🔍 Verification Queries

### Check Table Ownership

```sql
-- List all tables and their prefixes
SELECT 
    TABLE_NAME,
    CASE 
        WHEN TABLE_NAME LIKE 'j_%' THEN '✅ Liquibase-managed'
        ELSE '🔒 Read-only'
    END as management_type,
    TABLE_ROWS,
    ROUND(DATA_LENGTH / 1024 / 1024, 2) as size_mb
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'dev-planning'
ORDER BY management_type, TABLE_NAME;
```

### Check Entity Annotations

```bash
# Find all @Immutable entities
grep -r "@Immutable" src/main/java/com/pcagrade/order/entity/

# Expected output:
# Order.java:@Immutable
# Card.java:@Immutable
# CardTranslation.java:@Immutable
# CardCertification.java:@Immutable
# Invoice.java:@Immutable
```

### Verify Read-Only Columns

```bash
# Find columns with insertable=false
grep -r "insertable = false" src/main/java/com/pcagrade/order/entity/
```

---

## 🎯 Migration Checklist

When enabling Liquibase:

### Configuration
- [ ] `spring.liquibase.enabled=true`
- [ ] `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Baseline script executed
- [ ] Changelog files in place

### Entities
- [ ] `@Immutable` on: Order, Card, CardTranslation, CardCertification, Invoice
- [ ] `insertable=false, updatable=false` on all read-only entity fields
- [ ] NO `@Immutable` on: Employee, Planning, Group
- [ ] Foreign keys only to managed tables

### Testing
- [ ] Can read from all tables
- [ ] Can write to j_* tables
- [ ] CANNOT write to shared tables
- [ ] Sync process works
- [ ] Application starts without errors

---

## 📞 Need Help?

If you're unsure whether a table should be managed or read-only:

### Ask These Questions:

1. **Who creates records in this table?**
    - Planning system only → Liquibase-managed
    - External system → Read-only

2. **Is this table shared with other projects?**
    - Yes → Read-only
    - No → Liquibase-managed

3. **Does the table name start with `j_`?**
    - Yes → Liquibase-managed
    - No → Likely read-only

4. **Would other systems break if we modified it?**
    - Yes → Read-only
    - No → Liquibase-managed

---

## 🎉 Summary

```
YOUR PLANNING SYSTEM:
✅ Manages: 6 tables (j_*)
🔒 Reads: 6 tables (shared)

STRATEGY:
✅ Liquibase for your tables
🔒 Sync for shared tables
✅ @Immutable for read-only entities
✅ Safe and production-ready
```

This separation ensures:
- Your system has full control over planning data
- Shared data remains safe from accidental modification
- Clear boundaries between systems
- Easy to maintain and understand