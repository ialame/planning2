# 🔧 Code Migration Fixes - Replace OrderPriority with Delai

## 📋 Files to Fix

Based on grep results, you need to update:
1. ✅ `OrderService.java` - 4 occurrences
2. ✅ `OrderController.java` - 2 occurrences
3. ⚠️ `Order_old.java` - Can be deleted (backup file)

---

## 🔨 Fix 1: OrderService.java

**File**: `src/main/java/com/pcagrade/order/service/OrderService.java`

### Change 1: Line 71 (setPriority)

**BEFORE:**
```java
order.setPriority(Order.OrderPriority.FAST);
```

**AFTER:**
```java
order.setDelai("F");  // F = Fast (2 weeks)
```

### Change 2: Line 358 (searchOrders method signature)

**BEFORE:**
```java
public List<Order> searchOrders(String searchTerm, Integer status, Order.OrderPriority priority) {
    return orderRepository.findAll().stream()
            .filter(order -> searchTerm == null ||
                    order.getOrderNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))
            .filter(order -> status == null || order.getStatus().equals(status))
            .filter(order -> priority == null || order.getPriority().equals(priority))
            .collect(Collectors.toList());
}
```

**AFTER:**
```java
public List<Order> searchOrders(String searchTerm, Integer status, String delai) {
    return orderRepository.findAll().stream()
            .filter(order -> searchTerm == null ||
                    order.getOrderNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))
            .filter(order -> status == null || order.getStatus().equals(status))
            .filter(order -> delai == null || delai.equals(order.getDelai()))
            .collect(Collectors.toList());
}
```

### Change 3: Lines 443-449 (priority checks)

**BEFORE:**
```java
if (order.getPriority() == Order.OrderPriority.EXCELSIOR) {
    // ...
} else if (order.getPriority() == Order.OrderPriority.FAST_PLUS) {
    // ...
} else if (order.getPriority() == Order.OrderPriority.FAST) {
    // ...
} else if (order.getPriority() == Order.OrderPriority.CLASSIC) {
    // ...
}
```

**AFTER:**
```java
String delai = order.getDelai();
if ("X".equals(delai)) {  // Express (was EXCELSIOR)
    // ...
} else if ("F+".equals(delai)) {  // Fast+ (was FAST_PLUS)
    // ...
} else if ("F".equals(delai)) {  // Fast (was FAST)
    // ...
} else if ("C".equals(delai)) {  // Classic (was CLASSIC)
    // ...
} else if ("E".equals(delai)) {  // Economy (new)
    // ...
}
```

---

## 🔨 Fix 2: OrderController.java

**File**: `src/main/java/com/pcagrade/order/controller/OrderController.java`

### Change: Lines 531-534

**BEFORE:**
```java
Order.OrderPriority orderPriority = null;
if (priority != null && !priority.isEmpty()) {
    orderPriority = Order.OrderPriority.valueOf(priority.toUpperCase());
}
```

**AFTER:**
```java
String delai = null;
if (priority != null && !priority.isEmpty()) {
    // Map old priority names to delai codes
    delai = switch (priority.toUpperCase()) {
        case "EXCELSIOR" -> "X";
        case "FAST_PLUS" -> "F+";
        case "FAST" -> "F";
        case "CLASSIC" -> "C";
        case "ECONOMY" -> "E";
        default -> priority; // If already in delai format (X, F+, F, C, E)
    };
}
```

Then update the method call (probably around line 535):

**BEFORE:**
```java
orderService.searchOrders(searchTerm, statusInt, orderPriority);
```

**AFTER:**
```java
orderService.searchOrders(searchTerm, statusInt, delai);
```

---

## 🗑️ Fix 3: Delete Old File

**File**: `src/main/java/com/pcagrade/order/entity/Order_old.java`

This is a backup file that can be safely deleted:

```bash
# Optional: Keep a backup
cp src/main/java/com/pcagrade/order/entity/Order_old.java ~/backup_Order_old.java

# Delete the old file
rm src/main/java/com/pcagrade/order/entity/Order_old.java
```

---

## 📝 Complete Fixed Code Snippets

### OrderService.java - Complete Fixed Methods

```java
// Line ~71
private Order createDefaultOrder() {
    Order order = new Order();
    order.setDelai("F");  // Default to Fast (2 weeks)
    order.setStatus(1);   // New order
    return order;
}

// Line ~358
/**
 * Search orders by various criteria
 * @param searchTerm search term
 * @param status order status filter
 * @param delai delivery priority filter (X, F+, F, C, E)
 * @return list of filtered orders
 */
@Transactional(readOnly = true)
public List<Order> searchOrders(String searchTerm, Integer status, String delai) {
    return orderRepository.findAll().stream()
            .filter(order -> searchTerm == null ||
                    order.getOrderNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))
            .filter(order -> status == null || order.getStatus().equals(status))
            .filter(order -> delai == null || delai.equals(order.getDelai()))
            .collect(Collectors.toList());
}

// Lines ~443-449
private int calculatePriorityScore(Order order) {
    String delai = order.getDelai();
    if (delai == null) return 50; // Default score
    
    return switch (delai) {
        case "X" -> 100;   // Express - Highest priority
        case "F+" -> 80;   // Fast+
        case "F" -> 60;    // Fast
        case "C" -> 40;    // Classic
        case "E" -> 20;    // Economy - Lowest priority
        default -> 50;     // Unknown
    };
}
```

### OrderController.java - Complete Fixed Method

```java
// Around line 531
@GetMapping("/search")
public ResponseEntity<List<Order>> searchOrders(
        @RequestParam(required = false) String searchTerm,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority) {
    
    Integer statusInt = null;
    if (status != null && !status.isEmpty()) {
        statusInt = Integer.parseInt(status);
    }
    
    String delai = null;
    if (priority != null && !priority.isEmpty()) {
        // Map old priority enum names to delai codes
        delai = switch (priority.toUpperCase()) {
            case "EXCELSIOR" -> "X";
            case "FAST_PLUS" -> "F+";
            case "FAST" -> "F";
            case "CLASSIC" -> "C";
            case "ECONOMY" -> "E";
            // If already in delai format
            case "X", "F+", "F", "C", "E" -> priority;
            default -> "F"; // Default to Fast
        };
    }
    
    List<Order> orders = orderService.searchOrders(searchTerm, statusInt, delai);
    return ResponseEntity.ok(orders);
}
```

---

## ✅ Step-by-Step Application

### Step 1: Add Delai enum (5 min)

Create `src/main/java/com/pcagrade/order/entity/Delai.java` with the content from the "Delai.java" artifact.

### Step 2: Update OrderService.java (5 min)

Apply the 3 changes shown above:
1. Line 71: Change `setPriority` to `setDelai`
2. Line 358: Change method signature and filter
3. Lines 443-449: Replace enum checks with String checks

### Step 3: Update OrderController.java (3 min)

Apply the changes around line 531:
- Replace `OrderPriority` with String `delai`
- Add mapping from old enum names to delai codes

### Step 4: Delete old file (1 min)

```bash
rm src/main/java/com/pcagrade/order/entity/Order_old.java
```

### Step 5: Compile (2 min)

```bash
mvn clean compile
```

Should see: **BUILD SUCCESS**

---

## 🧪 Testing

After fixing, test:

```bash
# 1. Compile
mvn clean compile

# 2. Run tests
mvn test

# 3. Start application
mvn spring-boot:run

# 4. Test search endpoint
curl "http://localhost:8080/api/orders/search?priority=F" | jq
curl "http://localhost:8080/api/orders/search?priority=FAST" | jq  # Should still work (mapped to F)
```

---

## 📊 Migration Mapping

| Old OrderPriority | New Delai Code | Description |
|-------------------|----------------|-------------|
| EXCELSIOR         | X              | Express (1 day) |
| FAST_PLUS         | F+             | Fast+ (1 week) |
| FAST              | F              | Fast (2 weeks) |
| CLASSIC           | C              | Classic (1 month) |
| (new)             | E              | Economy (3 months) |

---

## 🎯 Verification Checklist

After applying all fixes:

- [ ] ✅ `Delai.java` enum created
- [ ] ✅ `OrderService.java` line 71 updated (setDelai)
- [ ] ✅ `OrderService.java` line 358 updated (method signature)
- [ ] ✅ `OrderService.java` lines 443-449 updated (switch on delai)
- [ ] ✅ `OrderController.java` lines 531-534 updated (mapping)
- [ ] ✅ `Order_old.java` deleted
- [ ] ✅ `mvn clean compile` succeeds
- [ ] ✅ Application starts without errors
- [ ] ✅ Search API works with both old and new priority formats

---

## 🐛 Common Issues

### Issue: "Cannot find symbol: method getDelai()"

**Solution**: Make sure your Order entity has the `delai` field and getter:
```java
@Column(name = "delai", length = 3, insertable = false, updatable = false)
private String delai;

public String getDelai() { return delai; }
```

### Issue: "Cannot find symbol: method setDelai()"

**Solution**: Add the setter (or use Lombok @Data):
```java
public void setDelai(String delai) { this.delai = delai; }
```

---

## 🎉 Success!

After these changes:
- ✅ No more `OrderPriority` enum references
- ✅ Using `delai` String field (X, F+, F, C, E)
- ✅ Backward compatibility (old API params mapped to new format)
- ✅ Clean compilation
- ✅ Ready for Symfony API integration

**Total time**: ~15 minutes