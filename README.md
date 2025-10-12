# 🃏 Pokemon Card Planning System

**Production-ready planning and workflow management system for Pokemon card grading operations**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-blue.svg)](https://vuejs.org/)
[![MariaDB](https://img.shields.io/badge/MariaDB-10.11-blue.svg)](https://mariadb.org/)
[![Symfony](https://img.shields.io/badge/Symfony-5.4-black.svg)](https://symfony.com/)

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Quick Start](#quick-start)
5. [Database Setup](#database-setup)
6. [Backend Setup (Spring Boot)](#backend-setup-spring-boot)
7. [Frontend Setup (Vue.js)](#frontend-setup-vuejs)
8. [Symfony API](#symfony-api)
9. [Usage](#usage)
10. [Troubleshooting](#troubleshooting)
11. [API Documentation](#api-documentation)

---

## 🎯 Overview

This system manages the complete workflow for Pokemon card grading operations:

- **Order Management**: Track orders with priorities (Express, Fast+, Fast, Classic, Economy)
- **Employee Management**: Manage employees with roles and skills
- **Automated Planning**: Generate optimal work schedules based on priorities and employee availability
- **Progress Tracking**: Monitor card processing through stages (grading, certification, scanning, packaging)
- **Real-time Sync**: Synchronize data from Symfony backend via REST API

### Key Features

✅ **Automatic Database Initialization** - Database, tables, and seed data created automatically  
✅ **Role-Based Access Control** - 7 predefined roles (Admin, Manager, Grader, Certifier, Scanner, Preparer, Viewer)  
✅ **Smart Planning Algorithm** - Assigns tasks based on priorities, deadlines, and employee skills  
✅ **Real-time Statistics** - Live dashboards with order status and employee workload  
✅ **RESTful API** - Symfony API for data synchronization

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      VUE.JS FRONTEND                        │
│            (http://localhost:3000 ou 5173)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 SPRING BOOT BACKEND                         │
│                  (http://localhost:8080)                    │
│  • Order Management      • Planning Engine                  │
│  • Employee Management   • Statistics                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    MARIADB DATABASE                         │
│                    (localhost:3306)                         │
│                    dev-planning                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Data Sync
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     SYMFONY API                             │
│                  (http://localhost:8000)                    │
│                /api/planning/export/*                       │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ Prerequisites

Before you begin, ensure you have the following installed:

### Required

- **Java 21+** - [Download OpenJDK](https://adoptium.net/)
- **Maven 3.8+** - [Download Maven](https://maven.apache.org/download.cgi)
- **Node.js 18+** - [Download Node.js](https://nodejs.org/)
- **MariaDB 10.11+** or MySQL 8+ - [Download MariaDB](https://mariadb.org/download/)

### Optional (for Symfony API sync)

- **PHP 8.1+** - [Download PHP](https://www.php.net/downloads)
- **Symfony CLI** - [Install Symfony](https://symfony.com/download)
- **Composer** - [Download Composer](https://getcomposer.org/)

### Verify Installation

```bash
java -version    # Should show Java 21+
mvn -version     # Should show Maven 3.8+
node -version    # Should show Node 18+
mysql --version  # Should show MariaDB 10.11+ or MySQL 8+
```

---

## 🚀 Quick Start

**For the impatient - Get up and running in 3 commands:**

```bash
# 1. Configure database (one-time setup)
mysql -u root -p
CREATE USER 'ia'@'localhost' IDENTIFIED BY 'foufafou';
GRANT ALL PRIVILEGES ON *.* TO 'ia'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 2. Start backend (creates database + tables + seed data automatically)
mvn spring-boot:run

# 3. In another terminal, start frontend
cd src/main/frontend
npm install
npm run dev
```

🎉 **That's it!** Open http://localhost:3000 and you're ready to go!

The system automatically creates:
- ✅ Database `dev-planning`
- ✅ All required tables
- ✅ 7 default roles (Admin, Manager, Grader, etc.)
- ✅ 5 test employees with assigned roles

---

## 🗄️ Database Setup

### Option 1: Automatic Setup (Recommended)

The application creates everything automatically on first run:

```bash
# Just start the application
mvn spring-boot:run
```

**What happens automatically:**
1. Creates database `dev-planning` if it doesn't exist
2. Creates all tables via Hibernate DDL
3. Populates `j_group` with 7 default roles
4. Creates 5 test employees in `j_employee`
5. Assigns roles to employees to cover all 7 roles

### Option 2: Manual Database Creation

If you prefer to create the database manually:

```bash
mysql -u ia -pfoufafou << 'EOF'
CREATE DATABASE IF NOT EXISTS `dev-planning` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `dev-planning`;

-- Tables will be created automatically by Hibernate
-- Just start the Spring Boot application
EOF
```

### Database Schema

The main tables created automatically:

| Table | Description |
|-------|-------------|
| `order` | Pokemon card orders (synced from Symfony) |
| `card_certification` | Individual cards being processed |
| `j_employee` | Employees with work hours and efficiency |
| `j_group` | Roles (Admin, Manager, Grader, etc.) |
| `j_employee_group` | Employee-to-role assignments |
| `j_planning` | Generated work schedules |

### Seed Data

**7 Default Roles** (created automatically):

| Role | Permission Level | Description |
|------|------------------|-------------|
| ROLE_ADMIN | 10 | System administrators |
| ROLE_MANAGER | 7 | Team managers |
| ROLE_NOTEUR | 5 | Card graders |
| ROLE_CERTIFICATEUR | 5 | Card certifiers/encapsulators |
| ROLE_SCANNER | 4 | Card scanners |
| ROLE_PREPARATEUR | 4 | Order preparers |
| ROLE_VIEWER | 2 | Read-only viewers |

**5 Test Employees** (created automatically):

| Employee | Email | Hours/Day | Efficiency | Roles |
|----------|-------|-----------|------------|-------|
| Sophie Martin | sophie.martin@pcagrade.com | 8h | 1.2 | Grader, Scanner, Viewer |
| Thomas Dubois | thomas.dubois@pcagrade.com | 6h | 1.0 | Certifier, Preparer |
| Marie Bernard | marie.bernard@pcagrade.com | 8h | 1.3 | Scanner, Preparer, Viewer |
| Pierre Petit | pierre.petit@pcagrade.com | 7h | 1.0 | Manager, Admin |
| Julie Moreau | julie.moreau@pcagrade.com | 5h | 0.8 | Grader, Certifier, Viewer |

---

## ⚙️ Backend Setup (Spring Boot)

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd planning
```

### 2. Configure Application Profiles

The application uses different profiles for different environments:

**Development (default):** `application-local.properties`
```properties
# Created automatically - uses localhost:3306
spring.profiles.active=local
```

**Docker:** `application-docker.properties`
```properties
# For Docker containers - uses database:3306
spring.profiles.active=docker
```

**Production:** `application-prod.properties`
```properties
# For production servers
spring.profiles.active=prod
```

### 3. Compile and Run

```bash
# Compile
mvn clean compile

# Run with default profile (local)
mvn spring-boot:run

# OR specify profile explicitly
mvn spring-boot:run -Dspring.profiles.active=local
```

### 4. Verify Backend is Running

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

### 5. Test API Endpoints

```bash
# Get all orders
curl http://localhost:8080/api/orders?page=0&size=10

# Get all employees
curl http://localhost:8080/api/employees

# Get groups/roles
curl http://localhost:8080/api/groups
```

### Backend Configuration

Edit `src/main/resources/application-local.properties` if needed:

```properties
# Database
spring.datasource.url=jdbc:mariadb://localhost:3306/dev-planning?createDatabaseIfNotExist=true
spring.datasource.username=ia
spring.datasource.password=foufafou

# Auto-create tables
spring.jpa.hibernate.ddl-auto=update

# Server port
server.port=8080

# Card processing time (minutes)
planning.card.processing.time=3
```

---

## 🎨 Frontend Setup (Vue.js)

### 1. Install Dependencies

```bash
cd src/main/frontend
npm install
```

### 2. Configure API Base URL

Edit `src/main/frontend/.env` (create if doesn't exist):

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Run Development Server

```bash
npm run dev
```

The frontend will be available at: **http://localhost:5173** or **http://localhost:3000**

### 4. Build for Production

```bash
npm run build

# Output will be in src/main/frontend/dist/
```

### Frontend Features

- **Dashboard** - Overview of orders by priority and status
- **Orders** - List and filter all orders
- **Employees & Planning** - View employee schedules
- **Groups** - Manage employee roles
- **Global Planning** - Generate and view complete work schedule

---

## 🔄 Symfony API

The Symfony API provides data synchronization from your main Pokemon card database.

### Setup Symfony API

**1. Configure Symfony Routes**

Edit `config/routes/api_routes.yaml`:

```yaml
# API routes WITHOUT locale prefix
api_routes:
    resource: ../src/Controller/Api/
    type: annotation
    prefix: /api
```

**2. Create API Controller**

Create file: `src/Controller/Api/PlanningExportController.php`

Copy the complete controller code from the artifact below this README.

**3. Start Symfony Server**

```bash
cd /path/to/symfony-project
symfony server:start
```

Symfony API will be available at: **http://localhost:8000**

### Symfony API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/planning/export/health` | Health check |
| `GET /api/planning/export/orders` | Export orders |
| `GET /api/planning/export/orders?limit=100` | Limit results |
| `GET /api/planning/export/orders?exclude_completed=false` | Include completed |
| `GET /api/planning/export/orders?since_year=2024` | Filter by year |
| `GET /api/planning/export/cards` | Export card certifications |
| `GET /api/planning/export/cards?order_id={hex}` | Cards for specific order |
| `GET /api/planning/export/stats` | Get statistics |
| `GET /api/planning/export/orders/count` | Count orders |
| `GET /api/planning/export/cards/count` | Count cards |

### Test Symfony API

```bash
# Health check
curl http://localhost:8000/api/planning/export/health

# Get orders
curl http://localhost:8000/api/planning/export/orders?limit=10

# Get statistics
curl http://localhost:8000/api/planning/export/stats
```

### Synchronize Data to Spring Boot

Once Symfony API is running, sync data to Spring Boot:

```bash
# Sync orders
curl -X POST http://localhost:8080/api/sync/orders

# Sync cards
curl -X POST http://localhost:8080/api/sync/cards

# Sync everything
curl -X POST http://localhost:8080/api/sync/all
```

---

## 💻 Usage

### 1. Access the Application

Open your browser and go to: **http://localhost:3000**

### 2. Navigate the Interface

**Dashboard**
- View order statistics by priority (Express, Fast+, Fast, Classic)
- See order status breakdown
- Monitor overall system health

**Orders Page**
- View all orders with pagination
- Filter by priority, status, or search terms
- See card counts and order details

**Employees & Planning**
- View all employees with their workload
- Click "View Planning" to see individual schedules
- Monitor employee availability and assignments

**Groups Page**
- View all 7 roles
- Assign/remove roles to employees
- Manage permissions

**Global Planning**
- Generate work schedules for all employees
- View planning by date
- See task assignments by role (Grader, Certifier, etc.)

### 3. Generate Planning

```bash
# Via API
curl -X POST http://localhost:8080/api/planning/generate

# Or use the "Global Planning" page in the UI
```

### 4. Common Workflows

**Add a New Employee:**
1. Go to "Employees & Planning"
2. Click "Add Employee"
3. Fill in details (name, email, work hours)
4. Go to "Groups" page
5. Assign appropriate roles

**Assign Roles to Employee:**
1. Go to "Groups" page
2. Find the employee card
3. Click role buttons (Processor, Supervisor, Manager)
4. Or click "Manage Groups" for detailed assignment

**View Employee Schedule:**
1. Go to "Employees & Planning"
2. Find the employee
3. Click "View Planning"
4. Select date to view schedule

---

## 🐛 Troubleshooting

### Database Connection Issues

**Error:** `Access denied for user 'ia'@'localhost'`

```bash
# Recreate database user
mysql -u root -p
DROP USER IF EXISTS 'ia'@'localhost';
CREATE USER 'ia'@'localhost' IDENTIFIED BY 'foufafou';
GRANT ALL PRIVILEGES ON *.* TO 'ia'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Error:** `Unknown database 'dev-planning'`

```bash
# The database should be created automatically
# If not, check that createDatabaseIfNotExist=true is in the JDBC URL
# Or create manually:
mysql -u ia -pfoufafou
CREATE DATABASE `dev-planning` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### Port Already in Use

**Error:** `Port 8080 is already in use`

```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9

# Or change port in application.properties
server.port=8081
```

### No Roles or Employees Created

**Check logs:**
```bash
# Look for initialization messages
grep -i "group\|employee" logs/spring.log

# Should see:
# ✅ Successfully initialized 7 default roles
# ✅ Successfully created 5 test employees
# ✅ Successfully assigned roles to all test employees
```

**Manual initialization:**
```bash
# If automatic initialization failed, run:
mysql -u ia -pfoufafou dev-planning < scripts/init_groups.sql
mysql -u ia -pfoufafou dev-planning < scripts/init_employees.sql
```

### Frontend Not Loading

```bash
# Clear npm cache and reinstall
cd src/main/frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Symfony API Not Responding

```bash
# Check Symfony is running
curl http://localhost:8000/api/planning/export/health

# Restart Symfony
symfony server:stop
symfony server:start

# Check logs
symfony server:log
```

---

## 📚 API Documentation

### Spring Boot REST API

**Base URL:** `http://localhost:8080`

#### Orders

```bash
# Get paginated orders
GET /api/orders?page=0&size=500&delai=F+&status=2

# Get single order
GET /api/orders/{orderId}

# Get order cards
GET /api/orders/{orderId}/cards
```

#### Employees

```bash
# Get all employees
GET /api/employees

# Get employee details
GET /api/employees/{employeeId}

# Create employee
POST /api/employees
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "workHoursPerDay": 8.0,
  "efficiencyRating": 1.0
}

# Update employee
PUT /api/employees/{employeeId}

# Delete employee
DELETE /api/employees/{employeeId}
```

#### Groups/Roles

```bash
# Get all groups
GET /api/groups

# Get employee groups
GET /api/groups/employee/{employeeId}

# Assign role to employee
POST /api/groups/assign
{
  "employeeId": "abc123...",
  "groupId": "def456..."
}

# Remove role from employee
DELETE /api/groups/remove?employeeId={id}&groupId={id}
```

#### Planning

```bash
# Get planning for date
GET /api/planning?date=2025-10-12

# Get employee planning
GET /api/planning/employee/{employeeId}?date=2025-10-12

# Generate planning
POST /api/planning/generate

# Get planning by status
GET /api/planning/by-status/{status}
```

#### Synchronization

```bash
# Sync orders from Symfony
POST /api/sync/orders

# Sync cards from Symfony
POST /api/sync/cards

# Sync everything
POST /api/sync/all

# Get sync statistics
GET /api/sync/stats
```

---

## 🧪 Testing

### Run Tests

```bash
# Backend tests
mvn test

# Frontend tests
cd src/main/frontend
npm run test
```

### Manual Testing

```bash
# Test complete workflow
./test_complete_initialization.sh

# Test database rebuild
./test_database_rebuild.sh
```

---

## 📦 Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t planning-backend .

# Run with Docker Compose
docker-compose up -d
```

### Production Build

```bash
# Build backend JAR
mvn clean package -DskipTests

# Build frontend
cd src/main/frontend
npm run build

# Run in production
java -jar target/planning-1.0.0.jar --spring.profiles.active=prod
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is proprietary software. All rights reserved.

---

## 👥 Team

- **Development Team** - Initial work and maintenance
- **Product Owner** - Requirements and specifications

---

## 📞 Support

For issues, questions, or contributions:
- Create an issue on GitHub
- Contact the development team
- Check the troubleshooting section above

---

## 🎉 Quick Reference Card

```bash
# START EVERYTHING
mvn spring-boot:run                    # Backend (terminal 1)
cd src/main/frontend && npm run dev    # Frontend (terminal 2)
symfony server:start                   # Symfony API (terminal 3, optional)

# USEFUL COMMANDS
mysql -u ia -pfoufafou dev-planning   # Access database
mvn clean compile                      # Recompile backend
curl http://localhost:8080/api/orders # Test API
curl -X POST http://localhost:8080/api/sync/all # Sync from Symfony

# RESET DATABASE
mysql -u ia -pfoufafou -e "DROP DATABASE \`dev-planning\`;"
mvn spring-boot:run                    # Recreates everything

# URLS
http://localhost:3000                  # Frontend UI
http://localhost:8080                  # Backend API
http://localhost:8000                  # Symfony API
```

---

**Made with ❤️ for Pokemon Card Grading Operations**