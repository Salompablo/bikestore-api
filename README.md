<div align="center">
  
  # 🚴‍♂️ Bikes Asaro API
  
  **A robust, production-ready RESTful backend powering a modern bicycle e-commerce platform.**
  
  [![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3+-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
  <br>
  [![AWS S3](https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazon-s3&logoColor=white)](https://aws.amazon.com/s3/)
  [![Mercado Pago](https://img.shields.io/badge/Mercado_Pago-00B1EA?style=for-the-badge&logo=mercadopago&logoColor=white)](https://www.mercadopago.com/)
  [![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)](https://jwt.io)
  [![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://swagger.io/)

  *Clean Code Architecture • Domain-Driven Design • Secure Integrations*

</div>

---

## 📖 Table of Contents
- [✨ Features](#-features)
- [🏗️ Architecture & Best Practices](#-architecture--best-practices)
- [🛒 Order & Payment Flow](#-order--payment-flow)
- [📂 Project Structure](#-project-structure)
- [🚀 Getting Started](#-getting-started)
- [📚 API Documentation](#-api-documentation)
- [🔮 Future Roadmap](#-future-roadmap)
- [👨‍💻 Author](#-author)

---

## ✨ Features

* **Advanced Security & Authentication:** * Stateless JWT authentication with Role-Based Access Control (`ADMIN` vs `CUSTOMER`).
  * Social Login via Google OAuth2.
  * Account lifecycle management: Email verification (via Resend), deactivation, and secure password reset.
* **Catalog & Inventory:** * Searchable, filterable, and paginated product catalog.
  * Secure, direct image uploads to AWS S3 with strict 5MB payload limits.
* **Smart Checkout:** * Real-time shopping cart validation.
  * Dynamic shipping cost calculations based on destination ZIP code and cart weight.
* **Order Management:** * Customers can track their paginated order history.
  * Admins can update fulfillment and shipping statuses.

---

## 🏗️ Architecture & Best Practices

This project is built with enterprise-grade standards to ensure maintainability, scalability, and strict separation of concerns using a **Layered Architecture**.

```mermaid
graph TD
    Client[Client Application / Frontend]
    
    subgraph "Bikes Asaro API"
        Controllers[Controllers Layer<br/><i>Presentation & HTTP Routing</i>]
        Services[Services Layer<br/><i>Business Logic & Transactions</i>]
        Repositories[Repositories Layer<br/><i>Spring Data JPA Interfaces</i>]
    end
    
    DB[(PostgreSQL Database)]
    
    subgraph "External Integrations"
        S3[AWS S3<br/><i>Image Storage</i>]
        MP[Mercado Pago API<br/><i>Payments</i>]
        Resend[Resend API<br/><i>Emails</i>]
    end
    
    Client -->|HTTP / JSON| Controllers
    Controllers -->|DTOs| Services
    Services -->|Entities| Repositories
    Repositories -->|JPA / SQL| DB
    
    Services -->|Upload Files| S3
    Services -->|Create Preference / Verify| MP
    Services -->|Send OTPs| Resend
```

### Architectural Highlights:
* **Immutability via Records:** Extensive use of Java 16+ `record` classes for all Data Transfer Objects (DTOs), guaranteeing thread safety and eliminating boilerplate.
* **Strict DTO Pattern:** Complete decoupling of JPA Entities and API Responses. Sensitive data never leaks to the client.
* **Generic Pagination Wrapper (`PageResponse<T>`):** Spring Data's native `Page` objects are intentionally hidden. A static factory method safely maps them to a clean, standardized pagination metadata object.
* **Global Exception Handling:** A centralized `@RestControllerAdvice` captures errors and returns a standardized JSON `ErrorResponse`.
* **Meta-Annotations for Swagger:** Complex OpenAPI configurations are abstracted into custom annotations (`@ApiAdminErrors`, `@ApiPublicErrors`, `@ApiNotFound`), keeping controllers clean.

---

## 🛒 Order & Payment Flow

This API integrates deeply with **Mercado Pago** to ensure secure, asynchronous payment processing and reliable inventory management.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client (Frontend)
    participant API as Bikes Asaro Backend
    participant DB as PostgreSQL DB
    participant MP as Mercado Pago API
    
    %% Phase 1: Preference Creation
    rect rgb(240, 248, 255)
    Note over C, MP: 1. Checkout & Preference Creation
    C->>API: POST /checkout/create-preference (Cart Data)
    activate API
    API->>DB: Validate Stock & Calculate Totals
    API->>MP: Create Payment Preference
    MP-->>API: Returns preferenceId
    API-->>C: Returns preferenceId
    deactivate API
    end
    
    %% Phase 2: User Payment
    rect rgb(255, 250, 240)
    Note over C, MP: 2. User Payment Execution
    C->>MP: Completes Payment Flow securely on Mercado Pago
    end
    
    %% Phase 3: Webhook & Fulfillment
    rect rgb(240, 255, 240)
    Note over MP, DB: 3. Asynchronous Fulfillment (Webhook)
    MP-->>API: POST /webhook/mercadopago (Payment Event)
    activate API
    API->>MP: Call MP API to verify Payment Status
    MP-->>API: Confirms Payment is APPROVED
    API->>DB: Update Order Status to PAID
    API->>DB: Deduct Purchased Inventory
    API-->>MP: 200 OK (Acknowledge Webhook)
    deactivate API
    end
```

---

## 📂 Project Structure

```text
src/main/java/com/bikestore/api/
├── annotation/     # Custom Swagger meta-annotations
├── config/         # Security, CORS, AWS S3, Mercado Pago, Swagger setup
├── controller/     # REST API endpoints grouped by domain
├── dto/            # Immutable Request/Response records and wrappers
├── entity/         # JPA Domain models (User, Product, Order)
├── exception/      # Custom exceptions and GlobalExceptionHandler
├── mapper/         # Layer transformations (Entity <-> DTO)
├── repository/     # Spring Data JPA interfaces
├── security/       # JWT Filters and Authentication providers
└── service/        # Core business logic and external API integrations
```

---

## 🚀 Getting Started

### Prerequisites
* **Java 21** or higher.
* **PostgreSQL** running locally or via Docker.
* Developer accounts for AWS (S3), Mercado Pago, and Resend.

### 1. Clone the repository
```bash
git clone [https://github.com/salompablo/bikestore-api.git](https://github.com/salompablo/bikestore-api.git)
cd bikestore-api/api
```

### 2. Environment Variables
Create a `.env` or `application-dev.properties` file in the root directory:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/bikestore
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Security
JWT_SECRET=your_super_secret_256_bit_jwt_key
GOOGLE_CLIENT_ID=your_google_oauth_client_id

# Mercado Pago
MP_ACCESS_TOKEN=your_mercado_pago_access_token

# AWS S3 (Media Storage)
AWS_REGION=sa-east-1
AWS_BUCKET_NAME=your_bucket_name
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key

# Resend (Transactional Emails)
RESEND_API_KEY=your_resend_api_key
```

### 3. Build & Run
Using the included Maven wrapper:
```bash
./mvnw clean install
./mvnw spring-boot:run
```
The server will start at `http://localhost:8080`.

---

## 📚 API Documentation

This API is fully documented using OpenAPI 3. Once the application is running, you can explore endpoints, view schemas, and execute test requests via the Swagger UI:

👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

---

## 🔮 Future Roadmap

- [ ] **Unit & Integration Testing:** Implement robust test coverage using JUnit 5, Mockito, and Testcontainers.
- [ ] **Caching Layer:** Integrate Redis to cache the product catalog and categories, reducing database load.
- [ ] **Rate Limiting:** Protect public endpoints (like login and registration) against brute-force attacks using Bucket4j.
- [ ] **Database Migrations:** Integrate Flyway or Liquibase for versioned database schema management.

---

## 👨‍💻 Author

**Pablo Salom Pita** Full-Stack Developer

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/pablo-salom/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/salompablo)
