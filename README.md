# Omni-Channel POS & Inventory System
**Enterprise Distributed Microservices Architecture**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.1-blue.svg)](https://spring.io/projects/spring-cloud)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.7-black.svg)](https://kafka.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

An enterprise-grade, cloud-native Point of Sale (POS) and Multi-Branch Inventory backend built with **Java 21**, **Spring Boot 3.2**, **Spring Cloud (Eureka & Gateway)**, and **Apache Kafka**.

Designed using clean architecture, event-driven choreographies, and isolated database-per-service patterns, this platform solves distributed race conditions, guarantees eventual data consistency across high-throughput checkouts, and secures perimeter access via stateless HMAC-SHA256 JWT tokens.

---

## Architecture Overview

```mermaid
graph TD
    Client["Client / Postman / Mobile POS"] -->|"HTTP / REST"| Gateway["API Gateway (Port 8080)<br/>• Route Routing<br/>• JWT Authentication Filter<br/>• Context Propagation"]

    subgraph Service Discovery
        Eureka["Eureka Discovery Server<br/>(Port 8761)"]
    end

    Gateway -.->|Resolve Routes| Eureka

    subgraph Microservices Domain
        Auth["Auth Service (Port 8084)<br/>JWT Issue & Verify"]
        Catalog["Catalog Service (Port 8081)<br/>Product Management"]
        Inventory["Inventory Service (Port 8082)<br/>Optimistic Lock @Version"]
        Order["Order Service (Port 8083)<br/>Saga Orchestrator"]
    end

    Gateway -->|/api/auth/**| Auth
    Gateway -->|/api/catalog/**| Catalog
    Gateway -->|/api/inventory/**| Inventory
    Gateway -->|/api/order/**| Order

    subgraph Database Per Service Pattern
        DBCatalog[("MySQL: pos_catalog")]
        DBInventory[("MySQL: pos_inventory")]
        DBOrder[("MySQL: pos_order")]
    end

    Catalog --> DBCatalog
    Inventory --> DBInventory
    Order --> DBOrder

    subgraph Distributed Messaging & Saga Bus
        Kafka["Apache Kafka (Port 9092)<br/>• order-created-topic<br/>• inventory-reserved-topic<br/>• inventory-failed-topic"]
    end

    Order -->|"1. Publish OrderCreated"| Kafka
    Kafka -->|"2. Consume OrderCreated"| Inventory
    Inventory -->|"3a. Success: InventoryReserved<br/>3b. Failure: InventoryFailed"| Kafka
    Kafka -->|"4. Update Status: SUCCESS / FAILED"| Order
```

---

## Core Distributed Engineering Features

### 1. Distributed Transactions via Saga Choreography (Kafka)
In a database-per-service architecture, ACID transactions cannot span multiple databases without heavy two-phase commit (2PC) locks. We employ **Saga Pattern via Event Choreography**:

```mermaid
sequenceDiagram
    autonumber
    actor Cashier
    participant GW as API Gateway
    participant OS as Order Service
    participant K as Kafka Event Bus
    participant IS as Inventory Service

    Cashier->>GW: POST /api/order/orders (Bearer JWT)
    GW->>OS: Forward with X-User-ID & X-User-Role
    OS->>OS: Save Order as PENDING in pos_order
    OS->>K: Produce OrderCreatedEvent (order-created-topic)
    OS-->>Cashier: 201 Created (Status: PENDING)

    K->>IS: Consume OrderCreatedEvent
    alt Sufficient Stock Available
        IS->>IS: Atomic stock deduction with @Version increment
        IS->>K: Produce InventoryReservedEvent (inventory-reserved-topic)
        K->>OS: Consume InventoryReservedEvent
        OS->>OS: Update Order status -> SUCCESS
    else Out of Stock or Optimistic Lock Conflict
        IS->>IS: Compensating Action: Roll back previous item deductions
        IS->>K: Produce InventoryFailedEvent (inventory-failed-topic)
        K->>OS: Consume InventoryFailedEvent
        OS->>OS: Update Order status -> FAILED (persist reason)
    end
```

### 2. High-Concurrency Stock Protection (Optimistic Locking)
- **Problem**: Simultaneous multi-terminal checkouts for identical SKU risk overselling and negative inventory.
- **Solution**: The `Stock` entity implements JPA `@Version` optimistic locking.
- When concurrent updates encounter a stale version, Spring Data JPA throws `ObjectOptimisticLockingFailureException`. The `GlobalExceptionHandler` converts this to HTTP `409 CONFLICT` or triggers compensating rollbacks within the Saga processor.

### 3. Perimeter Security & Role-Based Access Control (RBAC)
- **API Gateway**: Non-blocking reactive `JwtAuthenticationFilter` (order -1) validates HMAC-SHA256 tokens, extracts `userId` and `roles`, and propagates verified `X-User-ID` and `X-User-Role` headers downstream.
- **Downstream Services**: Enforce fine-grained roles (`ROLE_ADMIN`, `ROLE_CASHIER`) without coupling to authentication databases.

---

## Tech Stack & Tooling

| Component | Technology | Description |
|---|---|---|
| **Language** | Java 21 LTS | Sealed Types, Records, Virtual Thread compatibility |
| **Framework** | Spring Boot 3.2.5 | Enterprise application backbone |
| **Service Discovery** | Netflix Eureka Server | Dynamic service registration & health heartbeat |
| **API Gateway** | Spring Cloud Gateway | Non-blocking reactive routing & perimeter security |
| **Messaging** | Apache Kafka 3.7 | Distributed event streaming for Saga choreography |
| **Databases** | MySQL 8.0 | Separate isolated physical schema per domain |
| **Persistence** | Spring Data JPA / Hibernate 6.4 | Clean repository abstraction & optimistic locking |
| **Security** | JJWT (io.jsonwebtoken 0.12.5) | Cryptographic HMAC-SHA256 token verification |
| **Testing** | `@EmbeddedKafka`, MockMvc, H2 | Zero-dependency integration testing suites |

---

## Service Catalog & Port Allocation

| Service | Port | Database / State | Responsibility |
|---|---|---|---|
| `eureka-server` | `8761` | — | Centralized service discovery registry |
| `api-gateway` | `8080` | — | Entry point, JWT authentication, downstream routing |
| `auth-service` | `8084` | In-Memory / Auth | User authentication, token issuance & verification |
| `catalog-service` | `8081` | `pos_catalog` | Product catalog, pricing, SKU classification |
| `inventory-service` | `8082` | `pos_inventory` | Multi-branch stock tracking, optimistic concurrency |
| `order-service` | `8083` | `pos_order` | Checkout handling, Saga transaction choreography |

---

## How to Run Locally

### 1. Prerequisites
- **JDK 21** (`java -version`)
- **MySQL 8.0** running on `localhost:3306` with schemas:
  ```sql
  CREATE DATABASE IF NOT EXISTS pos_catalog;
  CREATE DATABASE IF NOT EXISTS pos_inventory;
  CREATE DATABASE IF NOT EXISTS pos_order;
  ```
- **Apache Kafka** extracted at `C:\kafka`.

### 2. Start Infrastructure
**Terminal 1 — Zookeeper (Port 2181):**
```cmd
cd /d C:\kafka
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

**Terminal 2 — Kafka Broker (Port 9092):**
```cmd
cd /d C:\kafka
bin\windows\kafka-server-start.bat config\server.properties
```

### 3. Start Microservices
Launch each module in order via terminal or VS Code Spring Boot Dashboard:
```bash
# 1. Service Registry
cd eureka-server && .\mvnw.cmd spring-boot:run

# 2. API Gateway
cd api-gateway && .\mvnw.cmd spring-boot:run

# 3. Auth Service
cd auth-service && .\mvnw.cmd spring-boot:run

# 4. Catalog Service
cd catalog-service && .\mvnw.cmd spring-boot:run

# 5. Inventory Service
cd inventory-service && .\mvnw.cmd spring-boot:run

# 6. Order Service
cd order-service && .\mvnw.cmd spring-boot:run
```

Eureka Dashboard: `http://localhost:8761`

---

## Automated Verification & Testing

Run all unit and integration test suites across all modules:
```bash
.\mvnw.cmd test
```

Tests run in-memory using H2 database and `@EmbeddedKafka` without needing live MySQL or external Kafka servers.
