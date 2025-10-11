# 🔧 Compilation Fixes Guide

Complete guide to fix all compilation errors for card synchronization.

---

## 📋 Issues to Fix

1. **Missing Lombok annotations** (`@Slf4j` for logging)
2. **CardCertification @Immutable** (needs to be writable)
3. **Missing setters/getters** in entities
4. **UlidConverter.hexToUuid()** method missing
5. **Duplicate MinimalSyncController** class

---

## 🚀 Fix 1: Add @Slf4j to Services

Add this annotation to all service classes that use `log`:

### EmployeeService.java

```java
package com.pcagrade.order.service;

import lombok.extern.slf4j.Slf4j;  // ← ADD THIS IMPORT
import org.springframework.stereotype.Service;
// ... other imports

@Slf4j  // ← ADD THIS ANNOTATION
@Service
public class EmployeeService {
    // ... rest of class
}
```

### PlanningService.java

```java
package com.pcagrade.order.service;

import lombok.extern.slf4j.Slf4j;  // ← ADD THIS IMPORT
import org.springframework.stereotype.Service;
// ... other imports

@Slf4j  // ← ADD THIS ANNOTATION
@Service
public class PlanningService {
    // ... rest of class
}
```

### EnhancedGroupController.java

```java
package com.pcagrade.order.controller;

import lombok.extern.slf4j.Slf4j;  // ← ADD THIS IMPORT
import org.springframework.web.bind.annotation.*;
// ... other imports

@Slf4j  // ← ADD THIS ANNOTATION
@RestController
@RequestMapping("/api/v2/groups")
public class EnhancedGroupController {
    // ... rest of class
}
```

---

## 🚀 Fix 2: Remove @Immutable from CardCertification

Edit `src/main/java/com/pcagrade/order/entity/CardCertification.java`:

```java
package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.ColumnDefault;
// @Immutable REMOVED - entity is now writable for sync  ← KEEP THIS COMMENT
// import org.hibernate.annotations.Immutable;  ← COMMENT THIS OUT OR DELETE

import java.time.Instant;
import java.util.UUID;

/**
 * CardCertification entity
 * NOW WRITABLE - synced from Symfony API for planning
 *
 * Represents a certified card within an order.
 * Contains both certification details and planning flags.
 */
@Data  // ← ENSURE THIS IS PRESENT
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "card_certification")
// DO NOT ADD @Immutable here - this entity must be writable
public class CardCertification extends AbstractUlidEntity {
    // ... rest of class
}
```

**Critical:** Ensure the class does NOT have `@Immutable` annotation.

---

## 🚀 Fix 3: Add @Data to Employee and Group

### Employee.java

```java
package com.pcagrade.order.entity;

import lombok.Data;  // ← ADD THIS IMPORT
import lombok.EqualsAndHashCode;
// ... other imports

@Data  // ← ADD THIS ANNOTATION
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "j_employee")
public class Employee extends AbstractUlidEntity {
    // ... rest of class
}
```

### Group.java

```java
package com.pcagrade.order.entity;

import lombok.Data;  // ← ADD THIS IMPORT
import lombok.EqualsAndHashCode;
// ... other imports

@Data  // ← ADD THIS ANNOTATION
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "j_group")
public class Group extends AbstractUlidEntity {
    // ... rest of class
}
```

---

## 🚀 Fix 4: Add UlidConverter.hexToUuid()

Create or update `src/main/java/com/pcagrade/order/util/UlidConverter.java`:

Use the artifact I created named "UlidConverter.java (avec hexToUuid)" - copy that entire file.

Key method:

```java
public static UUID hexToUuid(String hexString) {
    if (hexString == null || hexString.isEmpty()) {
        throw new IllegalArgumentException("Hex string cannot be null or empty");
    }

    String cleanHex = hexString.replaceAll("[-\\s]", "");

    if (cleanHex.length() != 32) {
        throw new IllegalArgumentException(
            "Hex string must be 32 characters long (got " + cleanHex.length() + "): " + hexString
        );
    }

    String formatted = cleanHex.toLowerCase()
        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
    
    return UUID.fromString(formatted);
}
```

---

## 🚀 Fix 5: Delete Duplicate Controller File

```bash
# Delete the wrong file
rm src/main/java/com/pcagrade/order/service/MinimalSyncService.java

# Keep only the correct file
# src/main/java/com/pcagrade/order/controller/MinimalSyncController.java
```

**OR** if you need MinimalSyncService, ensure it contains a service class, not a controller.

---

## 🚀 Fix 6: Update CardTranslation for getName()

Edit `src/main/java/com/pcagrade/order/entity/CardTranslation.java`:

```java
@Getter
@Setter
@Immutable  // ← This can stay @Immutable (it's read-only)
@Entity
@Table(name = "card_translation")
public class CardTranslation extends AbstractUlidEntity {
    
    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;  // ← Ensure this field exists
    
    // ... rest of fields
}
```

---

## 📝 Quick Fix Script

Create a file `fix-compilation.sh`:

```bash
#!/bin/bash

echo "🔧 Applying compilation fixes..."

# Fix 1: Add @Slf4j to EmployeeService
if ! grep -q "@Slf4j" src/main/java/com/pcagrade/order/service/EmployeeService.java; then
    echo "Adding @Slf4j to EmployeeService..."
    sed -i '' '1s/^/import lombok.extern.slf4j.Slf4j;\n/' src/main/java/com/pcagrade/order/service/EmployeeService.java
    sed -i '' '/^public class EmployeeService/i\
@Slf4j
' src/main/java/com/pcagrade/order/service/EmployeeService.java
fi

# Fix 2: Add @Slf4j to PlanningService
if ! grep -q "@Slf4j" src/main/java/com/pcagrade/order/service/PlanningService.java; then
    echo "Adding @Slf4j to PlanningService..."
    sed -i '' '1s/^/import lombok.extern.slf4j.Slf4j;\n/' src/main/java/com/pcagrade/order/service/PlanningService.java
    sed -i '' '/^public class PlanningService/i\
@Slf4j
' src/main/java/com/pcagrade/order/service/PlanningService.java
fi

# Fix 3: Add @Slf4j to EnhancedGroupController
if ! grep -q "@Slf4j" src/main/java/com/pcagrade/order/controller/EnhancedGroupController.java; then
    echo "Adding @Slf4j to EnhancedGroupController..."
    sed -i '' '1s/^/import lombok.extern.slf4j.Slf4j;\n/' src/main/java/com/pcagrade/order/controller/EnhancedGroupController.java
    sed -i '' '/^public class EnhancedGroupController/i\
@Slf4j
' src/main/java/com/pcagrade/order/controller/EnhancedGroupController.java
fi

# Fix 4: Remove @Immutable from CardCertification
echo "Removing @Immutable from CardCertification..."
sed -i '' 's/@Immutable/\/\/ @Immutable - REMOVED for sync/g' src/main/java/com/pcagrade/order/entity/CardCertification.java

# Fix 5: Delete duplicate file
if [ -f "src/main/java/com/pcagrade/order/service/MinimalSyncService.java" ]; then
    echo "Removing duplicate MinimalSyncService.java..."
    rm src/main/java/com/pcagrade/order/service/MinimalSyncService.java
fi

echo "✅ Fixes applied! Now rebuild:"
echo "   mvn clean compile"
```

Run it:
```bash
chmod +x fix-compilation.sh
./fix-compilation.sh
```

---

## ✅ Manual Verification Checklist

After applying fixes, verify:

### 1. Check Lombok Annotations

```bash
# Should find @Slf4j in these files:
grep -l "@Slf4j" src/main/java/com/pcagrade/order/service/*.java
grep -l "@Slf4j" src/main/java/com/pcagrade/order/controller/*.java

# Expected output:
# - EmployeeService.java
# - PlanningService.java
# - EnhancedGroupController.java
# - CardCertificationSyncService.java
# - MinimalSyncController.java
```

### 2. Check @Immutable Removal

```bash
# Should NOT find @Immutable in CardCertification
grep "@Immutable" src/main/java/com/pcagrade/order/entity/CardCertification.java

# Expected: No output or commented line
```

### 3. Check @Data Annotations

```bash
# Should find @Data in entity classes
grep -l "@Data" src/main/java/com/pcagrade/order/entity/*.java

# Expected output includes:
# - Employee.java
# - Group.java
# - CardCertification.java
```

### 4. Check UlidConverter Methods

```bash
# Should find hexToUuid method
grep "hexToUuid" src/main/java/com/pcagrade/order/util/UlidConverter.java

# Expected: method definition found
```

### 5. Check for Duplicate Files

```bash
# Should NOT exist:
ls src/main/java/com/pcagrade/order/service/MinimalSyncService.java 2>/dev/null

# Expected: No such file or directory
```

---

## 🔨 Rebuild and Test

```bash
# Clean build
mvn clean

# Compile
mvn compile

# If successful, run tests
mvn test

# Start application
mvn spring-boot:run
```

---

## 🐛 If Still Errors

### Missing log variable

```java
// Before (ERROR):
log.info("Message");

// After (FIX):
// Add at top of class:
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YourClass {
    // Now log.info() works
}
```

### Missing getters/setters

```java
// Before (ERROR):
employee.getFirstName()  // Method not found

// After (FIX):
// Add at top of class:
import lombok.Data;

@Data
public class Employee {
    private String firstName;
    // Lombok generates getters/setters automatically
}
```

### CardCertification still immutable

```java
// Check for this:
@Immutable  // ← DELETE OR COMMENT THIS
@Entity
@Table(name = "card_certification")
public class CardCertification {
```

Remove the `@Immutable` annotation completely.

---

## 📞 Support

If errors persist:

1. Check exact error message
2. Verify Lombok is in pom.xml:
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

3. Reload Maven dependencies:
```bash
mvn dependency:purge-local-repository
mvn clean install
```

4. Ensure IDE has Lombok plugin:
    - IntelliJ: File → Settings → Plugins → Search "Lombok"
    - Eclipse: Install Lombok from https://projectlombok.org/

---

## ✅ Success Indicators

After fixes, you should see:

```
[INFO] BUILD SUCCESS
[INFO] Total time:  5.234 s
[INFO] ------------------------------------------------------------------------
```

Then you can proceed with:

```bash
# Setup database
mysql -u ia -pfoufafou dev-planning < sql_setup_cards.sql

# Test sync
curl -X POST http://localhost:8080/api/sync/cards?limit=10 | jq
```

---

**Follow this guide step by step and all compilation errors will be resolved! 🎯**