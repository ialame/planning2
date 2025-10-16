# Pokemon Card Order Planning System

A work planning system for distributing Pokemon card processing tasks among employees based on order priorities (delivery deadlines).

## 🔧 Critical Fix: SQL Reserved Keyword Issue

### Problem
Your application was failing because the table name **"group"** is a **SQL reserved keyword** in MariaDB/MySQL. This causes syntax errors when Hibernate tries to create the table.

### Solution
**Rename the entity from "Group" to "Team"** throughout your entire codebase:

1. **Entity class**: Rename `Group.java` → `Team.java`
2. **Repository**: Rename `GroupRepository.java` → `TeamRepository.java`
3. **All references**: Update all imports and usages
4. **Table name**: The `@Table(name = "team")` annotation will create the correct table

### Files to Update

```bash
# Find all files containing "Group" references
grep -r "Group" src/

# Update these files:
- Entity: Group.java → Team.java
- Repository: GroupRepository → TeamRepository
- Data Initializer: GroupDataInitializer → TeamDataInitializer
- All service and controller files that reference Group
```

## 📋 System Features

### Core Functionality
- **Order Management**: Track Pokemon card grading orders
- **Employee Management**: Manage employees with specific roles
- **Work Planning Algorithm**: Automatically distribute tasks based on:
    - Order priority (delivery deadline)
    - Employee roles and capacity
    - Processing stages (Grading, Certifying, Scanning, Packaging)
- **Time Estimation**: 3 minutes per card per processing stage

### Employee Roles
- `ROLE_GRADER`: Grades Pokemon cards
- `ROLE_CERTIFIER`: Certifies graded cards
- `ROLE_SCANNER`: Scans certified cards
- `ROLE_PACKAGER`: Packages and prepares for shipping

## 🚀 Getting Started

### Prerequisites
- Java 17+
- MariaDB 10.6+
- Maven 3.8+
- Node.js 16+ (for React frontend)

### Database Setup

```sql
CREATE DATABASE planningdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'planning_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON planningdb.* TO 'planning_user'@'localhost';
FLUSH PRIVILEGES;
```

### Application Configuration

**application.properties**:
```properties
# Database Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/planningdb
spring.datasource.username=planning_user
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Server Configuration
server.port=8080
```

### Running the Application

```bash
# Backend (Spring Boot)
mvn clean install
mvn spring-boot:run

# Frontend (React)
cd frontend
npm install
npm start
```

## 📊 API Endpoints

### Planning Endpoints

```http
# Generate work plan for all pending orders
POST /api/planning/generate

# Get all work assignments
GET /api/planning/assignments

# Get assignments for specific employee
GET /api/planning/assignments/employee/{employeeId}

# Get assignments for specific order
GET /api/planning/assignments/order/{orderId}

# Update assignment status
PUT /api/planning/assignments/{assignmentId}/status?status=IN_PROGRESS

# Get employee workload summary
GET /api/planning/workload

# Get pending orders sorted by priority
GET /api/planning/orders/pending

# Get overdue orders
GET /api/planning/orders/overdue
```

### Example Request

```bash
# Generate work plan
curl -X POST http://localhost:8080/api/planning/generate

# Get employee workload
curl http://localhost:8080/api/planning/workload

# Update assignment status
curl -X PUT "http://localhost:8080/api/planning/assignments/{id}/status?status=COMPLETED"
```

## 🎯 Planning Algorithm

The work planning algorithm:

1. **Retrieves pending orders** sorted by delivery deadline (earliest first)
2. **For each processing stage** (Grading → Certifying → Scanning → Packaging):
    - Finds employees with required role
    - Distributes orders using **load balancing**
    - Assigns to employee with minimum current workload
3. **Calculates scheduling**:
    - Start time based on employee's current workload
    - Duration = card_count × 3 minutes
    - End time = start_time + duration

### Priority Calculation
```java
priority_score = delivery_deadline.toEpochSecond()
// Lower score = Higher priority (earlier deadline)
```

## 🗂️ Database Schema

### Main Tables
- `team`: Employee teams/groups
- `employee`: Workers with roles
- `employee_roles`: Employee role assignments
- `employee_team`: Many-to-many relationship
- `card_order`: Pokemon card orders
- `pokemon_card`: Individual cards in orders
- `work_assignment`: Task assignments to employees

## 🧪 Testing

### Sample Data
The application automatically initializes with:
- 4 default teams (one per processing stage)
- 8 sample employees (2 per role)
- 5 sample orders with varying priorities

### Test the Planning Algorithm

```bash
# 1. Check pending orders
curl http://localhost:8080/api/planning/orders/pending

# 2. Generate work plan
curl -X POST http://localhost:8080/api/planning/generate

# 3. View assignments
curl http://localhost:8080/api/planning/assignments

# 4. Check employee workload
curl http://localhost:8080/api/planning/workload
```

## 📈 Frontend Dashboard

Access the React dashboard at: `http://localhost:3000`

### Features
- **Overview Tab**: Visual employee workload distribution
- **Assignments Tab**: Complete list of work assignments
- **Employees Tab**: Employee details and capacity
- **Orders Tab**: Pending orders sorted by priority

## 🔍 Troubleshooting

### Common Issues

1. **SQL Syntax Error with "group"**
    - Solution: Rename to "Team" (see fix above)

2. **Tables not created**
    - Check `spring.jpa.hibernate.ddl-auto=update`
    - Verify database credentials
    - Check MariaDB is running

3. **Frontend can't connect to API**
    - Verify backend is running on port 8080
    - Check CORS configuration
    - Update `API_BASE` in React code if needed

4. **No employees/orders showing**
    - Data initializer runs on startup
    - Check logs for initialization errors
    - Verify database has data: `SELECT * FROM employee;`

## 📝 Code Standards

All code follows these standards:
- ✅ **English only**: Variables, methods, comments, UI text
- ✅ **Clear naming**: Descriptive, self-documenting names
- ✅ **Proper annotations**: JPA, validation, API documentation
- ✅ **Logging**: Info, debug, and error logs
- ✅ **Error handling**: Try-catch blocks with meaningful messages

## 🎨 Architecture

```
┌─────────────────┐
│  React Frontend │ (Planning Dashboard)
└────────┬────────┘
         │ REST API
┌────────▼────────┐
│  Spring Boot    │ (Planning Service)
│   Backend       │
└────────┬────────┘
         │ JPA/Hibernate
┌────────▼────────┐
│    MariaDB      │ (Planning Database)
└─────────────────┘
```

## 📄 License

This project is proprietary software for Pokemon card grading operations.

---

**Need Help?** Check the logs or contact the development team.