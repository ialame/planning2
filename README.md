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
9. [Configuration Files](#configuration-files)
10. [Database Migrations (Liquibase)](#database-migrations-liquibase)
11. [Usage](#usage)
12. [Troubleshooting](#troubleshooting)
13. [API Documentation](#api-documentation)

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
✅ **Database Migrations** - Liquibase for version-controlled schema management

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
│                    planningdb                             │
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

**For the impatient - Get up and running in 5 steps:**

### Step 1: Configure Database User

```bash
mysql -u root -p
```

```sql
CREATE USER 'xyz'@'localhost' IDENTIFIED BY 'abc123';
GRANT ALL PRIVILEGES ON *.* TO 'xyz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Step 2: Clone and Configure

```bash
# Clone repository
git clone <your-repo-url>
cd planning2

# Create local configuration from template
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Edit with your credentials
nano src/main/resources/application-local.properties
```

Update the following in `application-local.properties`:

```properties
spring.datasource.username=xyz
spring.datasource.password=abc123
```

### Step 3: Start Backend

```bash
mvn spring-boot:run
```

The backend will automatically:
- ✅ Create database `planningdb`
- ✅ Run Liquibase migrations
- ✅ Create all tables
- ✅ Populate initial data (roles, employees)

### Step 4: Start Frontend

```bash
cd src/main/frontend
npm install
npm run dev
```

### Step 5: Access Application

🎉 Open http://localhost:3000 and you're ready to go!

---

## 🗄️ Database Setup

### Option 1: Automatic Setup (Recommended)

The application uses **Liquibase** for database versioning and creates everything automatically:

```bash
mvn spring-boot:run
```

**What happens automatically:**
1. Creates database `planningdb` (via `createDatabaseIfNotExist=true`)
2. Runs Liquibase changesets to create tables
3. Populates initial data (roles, status mappings)
4. Application creates test employees via `DataInitializer`

### Option 2: Manual Database Creation

If you prefer manual control:

```bash
mysql -u xyz -pabc123 << 'EOF'
CREATE DATABASE IF NOT EXISTS `planningdb` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `planningdb`;

-- Tables will be created by Liquibase automatically
-- Just start the Spring Boot application
EOF
```

### Database Schema Overview

The main tables managed by Liquibase:

| Table | Description | Created By |
|-------|-------------|------------|
| `order` | Pokemon card orders (synced from Symfony) | Liquibase |
| `card_certification` | Individual cards being processed | Liquibase |
| `j_employee` | Employees with work hours and efficiency | Liquibase |
| `j_group` | Roles (Admin, Manager, Grader, etc.) | Liquibase |
| `j_employee_group` | Employee-to-role assignments | Liquibase |
| `j_planning` | Generated work schedules | Liquibase |
| `DATABASECHANGELOG` | Liquibase version tracking | Liquibase |
| `DATABASECHANGELOGLOCK` | Liquibase execution lock | Liquibase |

### Initial Data (Seed Data)

**7 Default Roles** (populated by Liquibase):

| Role | Permission Level | Description |
|------|------------------|-------------|
| ROLE_ADMIN | 10 | System administrators |
| ROLE_MANAGER | 7 | Team managers |
| ROLE_NOTEUR | 5 | Card graders |
| ROLE_CERTIFICATEUR | 5 | Card certifiers/encapsulators |
| ROLE_SCANNER | 4 | Card scanners |
| ROLE_PREPARATEUR | 4 | Order preparers |
| ROLE_VIEWER | 2 | Read-only viewers |

**5 Test Employees** (created by DataInitializer):

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

Create from template:

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

Edit and update:

```properties
# Database Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/planningdb?createDatabaseIfNotExist=true
spring.datasource.username=xyz
spring.datasource.password=abs123

# Liquibase Configuration
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development

# CORS Configuration
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

**Docker:** `application-docker.properties`

```properties
spring.datasource.url=jdbc:mariadb://database:3306/planningdb
spring.profiles.active=docker
spring.liquibase.contexts=docker
```

**Production:** `application-prod.properties`

```properties
spring.datasource.url=jdbc:mariadb://prod-db-host:3306/planning_prod
spring.profiles.active=prod
spring.liquibase.contexts=production
```

### 3. Compile and Run

```bash
# Compile
mvn clean compile

# Run with default profile (local)
mvn spring-boot:run

# OR specify profile explicitly
mvn spring-boot:run -Dspring.profiles.active=local

# For Docker environment
mvn spring-boot:run -Dspring.profiles.active=docker
```

### 4. Verify Backend is Running

```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}

# Check Liquibase status
curl http://localhost:8080/actuator/liquibase
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

**Step 1: Configure Symfony Routes**

Edit `config/routes/api_routes.yaml`:

```yaml
# API routes WITHOUT locale prefix
api_routes:
    resource: ../src/Controller/Api/
    type: annotation
    prefix: /api
```

**Step 2: Create API Controller**

Create file: `src/Controller/Api/PlanningExportController.php`

```php
<?php
// src/Controller/Api/PlanningExportController.php

namespace App\Controller\Api;

use Doctrine\DBAL\Connection;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

/**
 * Planning Export API - Exports minimal order/card data for Spring Boot planning service
 * Uses native SQL queries for performance (avoids Doctrine memory issues)
 *
 * @Route("/planning/export", name="api_planning_export_")
 */
class PlanningExportController extends AbstractController
{
    private $connection;

    public function __construct(Connection $connection)
    {
        $this->connection = $connection;
    }

    /**
     * Health check endpoint
     *
     * @Route("/health", name="health", methods={"GET"})
     */
    public function health(): JsonResponse
    {
        return $this->json([
            'status' => 'ok',
            'service' => 'Planning Export API',
            'version' => '1.1.0',
            'timestamp' => date('Y-m-d H:i:s'),
            'database' => 'connected',
            'endpoints' => [
                'GET /api/planning/export/health',
                'GET /api/planning/export/orders',
                'GET /api/planning/export/orders?limit={n}',
                'GET /api/planning/export/orders?exclude_completed=false',
                'GET /api/planning/export/orders?since_year=2024',
                'GET /api/planning/export/cards',
                'GET /api/planning/export/cards?order_id={hex_id}',
                'GET /api/planning/export/cards?limit={n}',
                'GET /api/planning/export/cards?exclude_completed=true',
                'GET /api/planning/export/stats',
                'GET /api/planning/export/orders/count',
                'GET /api/planning/export/cards/count',
            ]
        ]);
    }

    /**
     * Export all orders with minimal fields using native SQL
     *
     * @Route("/orders", name="orders", methods={"GET"})
     */
    public function exportOrders(Request $request): JsonResponse
    {
        try {
            $limit = $request->query->get('limit', 1000);
            $excludeCompleted = $request->query->get('exclude_completed', 'true');
            $sinceYear = $request->query->get('since_year', null);

            // Count total
            $countSql = "
                SELECT COUNT(DISTINCT o.id) as total
                FROM `order` o
                WHERE o.annulee = 0
            ";

            $countParams = [];
            $countTypes = [];

            if ($excludeCompleted === 'true') {
                $countSql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $countSql .= " AND YEAR(o.date) >= :since_year";
                $countParams['since_year'] = (int)$sinceYear;
                $countTypes['since_year'] = \PDO::PARAM_INT;
            }

            $countStmt = $this->connection->prepare($countSql);
            foreach ($countParams as $key => $value) {
                $countStmt->bindValue($key, $value, $countTypes[$key]);
            }
            $totalCount = (int)$countStmt->executeQuery()->fetchOne();

            // Main query
            $sql = "
                SELECT 
                    LOWER(HEX(o.id)) as id,
                    o.num_commande as order_number,
                    CONCAT(COALESCE(c.prenom, ''), ' ', COALESCE(c.nom, '')) as customer_name,
                    o.delai as delivery_date,
                    o.date as date,
                    COUNT(cco.card_certification_id) as total_cards,
                    o.status,
                    COALESCE(i.total_ttc, 0.0) as price,
                    o.delai as delai
                FROM `order` o
                LEFT JOIN customer c ON o.customer_id = c.id
                LEFT JOIN card_certification_order cco ON cco.order_id = o.id
                LEFT JOIN invoice i ON i.order_id = o.id
                WHERE o.annulee = 0
            ";

            if ($excludeCompleted === 'true') {
                $sql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $sql .= " AND YEAR(o.date) >= :since_year";
            }

            $sql .= "
                GROUP BY o.id, i.total_ttc
                ORDER BY 
                    CASE o.delai
                        WHEN 'X' THEN 1
                        WHEN 'F+' THEN 2
                        WHEN 'F' THEN 3
                        WHEN 'C' THEN 4
                        WHEN 'E' THEN 5
                        ELSE 6
                    END ASC,
                    o.date DESC
                LIMIT :limit
            ";

            $stmt = $this->connection->prepare($sql);
            $stmt->bindValue('limit', (int)$limit, \PDO::PARAM_INT);
            if ($sinceYear !== null) {
                $stmt->bindValue('since_year', (int)$sinceYear, \PDO::PARAM_INT);
            }

            $result = $stmt->executeQuery();
            $orders = $result->fetchAllAssociative();

            // Format dates
            foreach ($orders as &$order) {
                if (isset($order['delivery_date']) && $order['delivery_date'] instanceof \DateTime) {
                    $order['delivery_date'] = $order['delivery_date']->format('Y-m-d');
                }

                if (isset($order['date'])) {
                    if ($order['date'] instanceof \DateTime) {
                        $order['date'] = $order['date']->format('Y-m-d');
                    } elseif (is_string($order['date'])) {
                        $order['date'] = substr($order['date'], 0, 10);
                    }
                }

                $order['total_cards'] = (int)($order['total_cards'] ?? 0);
                $order['price'] = (float)($order['price'] ?? 0.0);
                $order['status'] = $order['status'] ?? 'PENDING';
            }

            return $this->json([
                'orders' => $orders,
                'count' => $totalCount,
                'returned' => count($orders),
                'limit' => $limit,
                'filters' => [
                    'exclude_completed' => $excludeCompleted === 'true',
                    'since_year' => $sinceYear ? (int)$sinceYear : null,
                    'excluded_statuses' => $excludeCompleted === 'true' ? [5, 6, 8] : []
                ]
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to export orders',
                'message' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Export all cards with US translated names
     *
     * @Route("/cards", name="cards", methods={"GET"})
     */
    public function exportCards(Request $request): JsonResponse
    {
        try {
            $orderId = $request->query->get('order_id');
            $limit = $request->query->get('limit', 5000);
            $excludeCompleted = $request->query->get('exclude_completed', 'true');
            $sinceYear = $request->query->get('since_year', null);

            // Count total
            $countSql = "
                SELECT COUNT(*) as total
                FROM card_certification cc
                JOIN card_certification_order cco ON cco.card_certification_id = cc.id
                INNER JOIN `order` o ON cco.order_id = o.id
                WHERE cc.deleted = 0
                AND o.annulee = 0
            ";

            $countParams = [];
            $countTypes = [];

            if ($excludeCompleted === 'true') {
                $countSql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $countSql .= " AND YEAR(o.date) >= :since_year";
                $countParams['since_year'] = (int)$sinceYear;
                $countTypes['since_year'] = \PDO::PARAM_INT;
            }

            if ($orderId) {
                $countSql .= " AND cco.order_id = UNHEX(REPLACE(:order_id, '-', ''))";
                $countParams['order_id'] = $orderId;
                $countTypes['order_id'] = \PDO::PARAM_STR;
            }

            $countStmt = $this->connection->prepare($countSql);
            foreach ($countParams as $key => $value) {
                $countStmt->bindValue($key, $value, $countTypes[$key]);
            }
            $totalCount = (int)$countStmt->executeQuery()->fetchOne();

            // Main query
            $sql = "
                SELECT 
                    LOWER(HEX(cc.id)) as id,
                    LOWER(HEX(cco.order_id)) as order_id,
                    COALESCE(ct.name, card.num, 'Unknown') as card_name,
                    card.num as card_number,
                    cc.status as processing_status,
                    CASE WHEN cc.note_id IS NOT NULL THEN 1 ELSE 0 END as grading_completed,
                    1 as certification_completed,
                    CASE WHEN cc.photo = 1 THEN 1 ELSE 0 END as scanning_completed,
                    0 as packaging_completed
                FROM card_certification cc
                JOIN card_certification_order cco ON cco.card_certification_id = cc.id
                INNER JOIN `order` o ON cco.order_id = o.id
                JOIN card ON cc.card_id = card.id
                LEFT JOIN card_translation ct ON ct.translatable_id = card.id AND ct.locale = 'us'
                WHERE cc.deleted = 0
                AND o.annulee = 0
            ";

            $params = [];
            $types = [];

            if ($excludeCompleted === 'true') {
                $sql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $sql .= " AND YEAR(o.date) >= :since_year";
                $params['since_year'] = (int)$sinceYear;
                $types['since_year'] = \PDO::PARAM_INT;
            }

            if ($orderId) {
                $sql .= " AND cco.order_id = UNHEX(REPLACE(:order_id, '-', ''))";
                $params['order_id'] = $orderId;
                $types['order_id'] = \PDO::PARAM_STR;
            }

            $sql .= " ORDER BY cc.id ASC LIMIT :limit";
            $params['limit'] = (int)$limit;
            $types['limit'] = \PDO::PARAM_INT;

            $stmt = $this->connection->prepare($sql);
            foreach ($params as $key => $value) {
                $stmt->bindValue($key, $value, $types[$key]);
            }

            $result = $stmt->executeQuery();
            $cards = $result->fetchAllAssociative();

            // Format
            foreach ($cards as &$card) {
                $card['processing_status'] = (int)($card['processing_status'] ?? 0);
                $card['grading_completed'] = (bool)$card['grading_completed'];
                $card['certification_completed'] = (bool)$card['certification_completed'];
                $card['scanning_completed'] = (bool)$card['scanning_completed'];
                $card['packaging_completed'] = (bool)$card['packaging_completed'];
            }

            return $this->json([
                'cards' => $cards,
                'count' => $totalCount,
                'returned' => count($cards),
                'filtered_by_order' => $orderId,
                'limit' => $limit,
                'locale' => 'us',
                'filters' => [
                    'exclude_completed' => $excludeCompleted === 'true',
                    'since_year' => $sinceYear ? (int)$sinceYear : null
                ]
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to export cards',
                'message' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Get statistics about exportable data
     *
     * @Route("/stats", name="stats", methods={"GET"})
     */
    public function exportStats(Request $request): JsonResponse
    {
        try {
            $excludeCompleted = $request->query->get('exclude_completed', 'true');
            $sinceYear = $request->query->get('since_year', null);

            $whereClause = "WHERE o.annulee = 0";
            $params = [];
            $types = [];

            if ($excludeCompleted === 'true') {
                $whereClause .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $whereClause .= " AND YEAR(o.date) >= :since_year";
                $params['since_year'] = (int)$sinceYear;
                $types['since_year'] = \PDO::PARAM_INT;
            }

            // Count total orders
            $sql = "SELECT COUNT(*) as total FROM `order` WHERE annulee = 0";
            $totalOrders = $this->connection->executeQuery($sql)->fetchOne();

            // Count exportable orders
            $sql = "SELECT COUNT(*) as exportable FROM `order` o " . $whereClause;
            $stmt = $this->connection->prepare($sql);
            foreach ($params as $key => $value) {
                $stmt->bindValue($key, $value, $types[$key]);
            }
            $exportableOrders = $stmt->executeQuery()->fetchOne();

            // Count certifications
            $sql = "SELECT COUNT(*) as total FROM card_certification WHERE deleted = 0";
            $totalCertifications = $this->connection->executeQuery($sql)->fetchOne();

            // Count exportable cards
            $sql = "
                SELECT COUNT(*) as exportable
                FROM card_certification cc
                JOIN card_certification_order cco ON cco.card_certification_id = cc.id
                INNER JOIN `order` o ON cco.order_id = o.id
                WHERE cc.deleted = 0
            ";

            if ($excludeCompleted === 'true') {
                $sql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $sql .= " AND YEAR(o.date) >= :since_year";
            }

            $stmt = $this->connection->prepare($sql);
            foreach ($params as $key => $value) {
                $stmt->bindValue($key, $value, $types[$key]);
            }
            $exportableCards = $stmt->executeQuery()->fetchOne();

            return $this->json([
                'total_orders' => (int)$totalOrders,
                'exportable_orders' => (int)$exportableOrders,
                'filtered_orders' => (int)$totalOrders - (int)$exportableOrders,
                'total_certifications' => (int)$totalCertifications,
                'exportable_cards' => (int)$exportableCards,
                'filters' => [
                    'exclude_completed' => $excludeCompleted === 'true',
                    'since_year' => $sinceYear ? (int)$sinceYear : null
                ]
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Failed to get stats',
                'message' => $e->getMessage()
            ], 500);
        }
    }

    /**
     * Get total count of orders
     *
     * @Route("/orders/count", name="orders_count", methods={"GET"})
     */
    public function getOrdersCount(Request $request): JsonResponse
    {
        try {
            $excludeCompleted = $request->query->get('exclude_completed', 'true');
            $sinceYear = $request->query->get('since_year', null);

            $sql = "
                SELECT COUNT(DISTINCT o.id) as total
                FROM `order` o
                WHERE o.annulee = 0
            ";

            $params = [];
            $types = [];

            if ($excludeCompleted === 'true') {
                $sql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $sql .= " AND YEAR(o.date) >= :since_year";
                $params['since_year'] = (int)$sinceYear;
                $types['since_year'] = \PDO::PARAM_INT;
            }

            $stmt = $this->connection->prepare($sql);
            foreach ($params as $key => $value) {
                $stmt->bindValue($key, $value, $types[$key]);
            }

            $count = (int)$stmt->executeQuery()->fetchOne();

            return $this->json([
                'success' => true,
                'count' => $count,
                'filters' => [
                    'exclude_completed' => $excludeCompleted === 'true',
                    'since_year' => $sinceYear ? (int)$sinceYear : null
                ]
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'error' => $e->getMessage(),
                'count' => 0
            ], 500);
        }
    }

    /**
     * Get total count of cards
     *
     * @Route("/cards/count", name="cards_count", methods={"GET"})
     */
    public function getCardsCount(Request $request): JsonResponse
    {
        try {
            $orderId = $request->query->get('order_id');
            $excludeCompleted = $request->query->get('exclude_completed', 'true');
            $sinceYear = $request->query->get('since_year', null);

            $sql = "
                SELECT COUNT(*) as total
                FROM card_certification cc
                JOIN card_certification_order cco ON cco.card_certification_id = cc.id
                INNER JOIN `order` o ON cco.order_id = o.id
                WHERE cc.deleted = 0
                AND o.annulee = 0
            ";

            $params = [];
            $types = [];

            if ($excludeCompleted === 'true') {
                $sql .= " AND o.status NOT IN (5, 6, 8)";
            }

            if ($sinceYear !== null) {
                $sql .= " AND YEAR(o.date) >= :since_year";
                $params['since_year'] = (int)$sinceYear;
                $types['since_year'] = \PDO::PARAM_INT;
            }

            if ($orderId) {
                $sql .= " AND cco.order_id = UNHEX(REPLACE(:order_id, '-', ''))";
                $params['order_id'] = $orderId;
                $types['order_id'] = \PDO::PARAM_STR;
            }

            $stmt = $this->connection->prepare($sql);
            foreach ($params as $key => $value) {
                $stmt->bindValue($key, $value, $types[$key]);
            }

            $count = (int)$stmt->executeQuery()->fetchOne();

            return $this->json([
                'success' => true,
                'count' => $count,
                'filters' => [
                    'exclude_completed' => $excludeCompleted === 'true',
                    'since_year' => $sinceYear ? (int)$sinceYear : null,
                    'order_id' => $orderId
                ]
            ]);

        } catch (\Exception $e) {
            return $this->json([
                'success' => false,
                'error' => $e->getMessage(),
                'count' => 0
            ], 500);
        }
    }
}
```

**Step 3: Start Symfony Server**

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

## 📝 Configuration Files

### Important Configuration Files

The project uses several configuration files that **must NOT** be committed to Git:

#### 1. Local Properties File

**File:** `src/main/resources/application-local.properties`

**Template:** Copy from `application-local.properties.example`

```properties
# Database Configuration
spring.datasource.url=jdbc:mariadb://localhost:3306/planningdb?createDatabaseIfNotExist=true
spring.datasource.username=xyz
spring.datasource.password=abc123
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yml
spring.liquibase.contexts=development

# CORS
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*

# Logging
logging.level.root=INFO
logging.level.com.pcagrade=DEBUG
logging.level.org.springframework.web=DEBUG

# Planning Configuration
planning.card.processing.time=3
planning.employee.break.time=15
planning.workday.start=08:00
planning.workday.end=17:00

# Order table compatibility
order.table.readonly=true
order.status.integer.mapping=true
order.adapter.service.enabled=true
```

#### 2. Frontend Environment File

**File:** `src/main/frontend/.env`

```env
VITE_API_BASE_URL=http://localhost:8080
```

#### 3. .gitignore Configuration

The project includes a comprehensive `.gitignore` to prevent sensitive files from being committed:

```gitignore
# Sensitive Files - NEVER COMMIT
**/application-local.properties
**/application-local.yml
**/.env.local
**/.env.production.local
*-deployment.yaml
*.sql
*secret*
*credentials*
*password*

# Build artifacts
target/
node_modules/
dist/
build/

# IDE files
.idea/
.vscode/
*.iml

# Logs
*.log
logs/
```

### Security Best Practices

⚠️ **NEVER commit:**
- Database credentials
- API keys
- Deployment configurations with production values
- Database dumps (`.sql` files)
- Any file with `secret`, `password`, or `credentials` in the name

✅ **DO commit:**
- Example/template files (`.example` suffix)
- Default development configurations (without credentials)
- Documentation

---

## 🔄 Database Migrations (Liquibase)

The project uses **Liquibase** for version-controlled database schema management.

### Understanding Liquibase

Liquibase tracks all database changes in **changesets**. Each changeset is:
- ✅ Version-controlled
- ✅ Applied only once
- ✅ Tracked in `DATABASECHANGELOG` table
- ✅ Reversible (with rollback)

### Changeset Structure

```
src/main/resources/db/
└── changelog/
    ├── db.changelog-master.yml      # Main changelog file
    ├── changes/
    │   ├── 001-create-initial-tables.yml
    │   ├── 002-create-employee-tables.yml
    │   ├── 003-create-planning-tables.yml
    │   ├── 004-create-status-workflow-table.yml
    │   ├── 005-create-priority-mapping-table.yml
    │   └── 006-create-order-adapter-table.yml
    └── data/
        └── (future data changesets)
```

### Main Changelog File

**File:** `src/main/resources/db/changelog/db.changelog-master.yml`

```yaml
databaseChangeLog:
  # Schema changesets
  - include:
      file: db/changelog/changes/001-create-initial-tables.yml
      context: development,docker,production

  - include:
      file: db/changelog/changes/002-create-employee-tables.yml
      context: development,docker,production

  - include:
      file: db/changelog/changes/003-create-planning-tables.yml
      context: development,docker,production

  - include:
      file: db/changelog/changes/004-create-status-workflow-table.yml
      context: development,docker,production

  - include:
      file: db/changelog/changes/005-create-priority-mapping-table.yml
      context: development,docker,production

  - include:
      file: db/changelog/changes/006-create-order-adapter-table.yml
      context: development,docker,production

  # Future changes go here...
```

### Liquibase Contexts

The project uses **contexts** to control which changesets run in each environment:

| Context | Environment | Description |
|---------|-------------|-------------|
| `development` | Local dev | Full schema + test data |
| `docker` | Docker/Staging | Full schema + minimal data |
| `production` | Production | Full schema only |

### How Liquibase Works

**First Run:**
1. Checks if `DATABASECHANGELOG` table exists
2. If not, creates it
3. Reads `db.changelog-master.yml`
4. Applies all changesets in order
5. Records each changeset in `DATABASECHANGELOG`

**Subsequent Runs:**
1. Checks `DATABASECHANGELOG` table
2. Skips already-applied changesets
3. Applies only new changesets
4. Records new changesets

### Viewing Liquibase Status

```bash
# Check which changesets have been applied
mysql -u xyz -pabc123 planningdb

SELECT * FROM DATABASECHANGELOG ORDER BY DATEEXECUTED DESC;

# Via Spring Boot Actuator
curl http://localhost:8080/actuator/liquibase
```

### Creating New Changesets

**Step 1: Create new changeset file**

```bash
cd src/main/resources/db/changelog/changes
nano 007-add-employee-phone.yml
```

**Step 2: Write changeset**

```yaml
databaseChangeLog:
  - changeSet:
      id: 007-add-employee-phone
      author: your-name
      context: development,docker,production
      changes:
        - addColumn:
            tableName: j_employee
            columns:
              - column:
                  name: phone_number
                  type: VARCHAR(20)
                  constraints:
                    nullable: true
      rollback:
        - dropColumn:
            tableName: j_employee
            columnName: phone_number
```

**Step 3: Add to master changelog**

Edit `db.changelog-master.yml`:

```yaml
databaseChangeLog:
  # ... existing changesets ...
  
  # New changeset
  - include:
      file: db/changelog/changes/007-add-employee-phone.yml
      context: development,docker,production
```

**Step 4: Test**

```bash
# Stop application
# Start application - new changeset will be applied automatically
mvn spring-boot:run

# Check logs for:
# Liquibase: Applying changeset: 007-add-employee-phone
```

### Liquibase Commands

```bash
# Dry run - see what would be executed
mvn liquibase:update -Dliquibase.verbose=true

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Generate SQL for preview
mvn liquibase:updateSQL

# Clear checksums (if changeset was modified)
mvn liquibase:clearCheckSums
```

### Liquibase Best Practices

✅ **DO:**
- Use incremental IDs (001, 002, 003...)
- Provide rollback for all changesets
- Test in development first
- Use contexts for environment control
- Document complex changes
- Keep changesets small and focused

❌ **DON'T:**
- Modify changesets after they're applied
- Delete old changesets
- Skip ID numbers
- Use auto-increment without context
- Commit without testing

### Troubleshooting Liquibase

**Error: Changeset already exists**

```bash
# Clear checksums and retry
mvn liquibase:clearCheckSums
mvn spring-boot:run
```

**Error: Wrong checksum**

```bash
# If you accidentally modified a changeset, you need to:
# 1. Revert the changeset to original
# 2. Create a NEW changeset with your changes
```

**Start from scratch:**

```bash
# Drop database and start fresh
mysql -u xyz -pabc123
DROP DATABASE `planningdb`;
EXIT;

# Restart application - everything will be recreated
mvn spring-boot:run
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
3. Click role buttons (Grader, Certifier, Scanner, etc.)
4. Or click "Manage Groups" for detailed assignment

**View Employee Schedule:**
1. Go to "Employees & Planning"
2. Find the employee
3. Click "View Planning"
4. Select date to view schedule

**Sync Data from Symfony:**
1. Ensure Symfony API is running at http://localhost:8000
2. Go to Spring Boot backend
3. POST to `/api/sync/all`
4. Check logs for sync status

---

## 🐛 Troubleshooting

### Database Issues

**Error:** `Access denied for user 'xyz'@'localhost'`

```bash
# Recreate database user
mysql -u root -p
DROP USER IF EXISTS 'xyz'@'localhost';
CREATE USER 'xyz'@'localhost' IDENTIFIED BY 'abc123';
GRANT ALL PRIVILEGES ON *.* TO 'xyz'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Error:** `Unknown database 'planningdb'`

The database should be created automatically. If not:

```bash
mysql -u xyz -pabc123
CREATE DATABASE `planningdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# Then restart Spring Boot
mvn spring-boot:run
```

**Error:** Liquibase checksum mismatch

```bash
# Clear checksums
mvn liquibase:clearCheckSums

# Or drop and recreate
mysql -u xyz -pabc123 -e "DROP DATABASE \`planningdb\`;"
mvn spring-boot:run
```

### Port Issues

**Error:** `Port 8080 is already in use`

```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9

# Or change port in application-local.properties
server.port=8081
```

**Error:** `Port 3000 is already in use`

```bash
# Frontend will try 5173 automatically
# Or kill the process
lsof -ti:3000 | xargs kill -9
```

### No Initial Data

**Problem:** No roles or employees created

Check logs for initialization:

```bash
grep -i "group\|employee\|liquibase" logs/spring.log
```

Should see:
```
✅ Liquibase: Successfully applied changeset: 001-create-initial-tables
✅ Liquibase: Successfully applied changeset: 002-create-employee-tables
✅ DataInitializer: Created 5 test employees
✅ DataInitializer: Assigned roles to employees
```

### Frontend Issues

**Problem:** Frontend not loading or blank page

```bash
cd src/main/frontend

# Clear and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear browser cache
# Restart dev server
npm run dev
```

**Problem:** API calls failing (CORS errors)

Check `application-local.properties`:

```properties
spring.web.cors.allowed-origins=http://localhost:3000,http://localhost:5173
```

Restart Spring Boot after changing.

### Symfony API Issues

**Problem:** Symfony API not responding

```bash
# Check if running
curl http://localhost:8000/api/planning/export/health

# Restart
cd /path/to/symfony
symfony server:stop
symfony server:start

# Check logs
symfony server:log
```

**Problem:** Wrong API route

Verify `config/routes/api_routes.yaml`:

```yaml
api_routes:
    resource: ../src/Controller/Api/
    type: annotation
    prefix: /api
```

### Sync Issues

**Problem:** Data not syncing from Symfony

```bash
# Test Symfony API manually
curl http://localhost:8000/api/planning/export/orders?limit=1

# Check Spring Boot sync endpoint
curl -X POST http://localhost:8080/api/sync/all

# Check logs
tail -f logs/spring.log | grep -i sync
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
Content-Type: application/json

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
Content-Type: application/json

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

# Integration tests
mvn verify
```

### Manual Testing

```bash
# Test database creation
./scripts/test_database_rebuild.sh

# Test complete initialization
./scripts/test_complete_initialization.sh

# Test Symfony sync
./scripts/test_symfony_sync.sh
```

---

## 📦 Deployment

### Docker Deployment

```bash
# Build Docker image
docker build -t planning-backend .

# Run with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f backend
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

### Environment Variables (Production)

Set these environment variables in production:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:mariadb://prod-db:3306/planning_prod
export SPRING_DATASOURCE_USERNAME=prod_user
export SPRING_DATASOURCE_PASSWORD=secure_password

# Liquibase
export SPRING_LIQUIBASE_CONTEXTS=production

# Server
export SERVER_PORT=8080

# CORS (adjust for your domain)
export SPRING_WEB_CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- ✅ Write tests for new features
- ✅ Follow existing code style
- ✅ Update documentation
- ✅ Create Liquibase changesets for schema changes
- ✅ Test in all environments (development, docker, production)

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
# ========== INITIAL SETUP ==========
# 1. Create database user
mysql -u root -p
CREATE USER 'xyz'@'localhost' IDENTIFIED BY 'abc123';
GRANT ALL PRIVILEGES ON *.* TO 'xyz'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 2. Create local config
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Edit and set your credentials
nano src/main/resources/application-local.properties

# ========== START EVERYTHING ==========
mvn spring-boot:run                    # Backend (terminal 1)
cd src/main/frontend && npm run dev    # Frontend (terminal 2)
symfony server:start                   # Symfony API (terminal 3, optional)

# ========== USEFUL COMMANDS ==========
# Database
mysql -u xyz -pabc123 planningdb                   # Access DB
SELECT * FROM DATABASECHANGELOG;                      # Check Liquibase

# Backend
mvn clean compile                                     # Recompile
curl http://localhost:8080/actuator/health           # Health check
curl http://localhost:8080/actuator/liquibase        # Liquibase status

# Sync
curl -X POST http://localhost:8080/api/sync/all      # Sync from Symfony

# ========== RESET DATABASE ==========
mysql -u xyz -pabc123 -e "DROP DATABASE \`planningdb\`;"
mvn spring-boot:run                                   # Recreates everything

# ========== LIQUIBASE ==========
mvn liquibase:updateSQL                               # Preview changes
mvn liquibase:rollback -Dliquibase.rollbackCount=1   # Rollback last
mvn liquibase:clearCheckSums                          # Clear checksums

# ========== URLS ==========
http://localhost:3000                                 # Frontend UI
http://localhost:8080                                 # Backend API
http://localhost:8000                                 # Symfony API
http://localhost:8080/actuator/health                # Backend health
http://localhost:8000/api/planning/export/health     # Symfony health

# ========== LOGS ==========
tail -f logs/spring.log                               # Spring Boot logs
symfony server:log                                    # Symfony logs
```

---

## 🔍 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Liquibase Documentation](https://docs.liquibase.com/)
- [Vue.js Documentation](https://vuejs.org/guide/introduction.html)
- [Symfony Documentation](https://symfony.com/doc/current/index.html)
- [MariaDB Documentation](https://mariadb.com/kb/en/documentation/)

---

**Made with ❤️ for Pokemon Card Grading Operations**