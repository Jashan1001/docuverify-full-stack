# DocuVerify — Multi-Tenant Document Verification & Trust Platform

A production-grade Spring Boot backend for secure, tamper-proof document verification with multi-tenant support, JWT auth, Redis rate limiting, and public QR/link-based verification.

---

## 🚀 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Database | PostgreSQL |
| Caching / Rate Limit | Redis |
| File Storage | AWS S3 |
| ORM | Spring Data JPA + Hibernate |
| Build | Maven |
| Java | 17 |

---

## 📁 Project Structure

```
com.docuverify
├── config/          # SecurityConfig, JwtFilter, RateLimitFilter, RedisConfig, S3Config
├── controller/      # AuthController, DocumentController, VerificationController, PublicController
├── service/         # AuthService, DocumentService, VerificationService, TokenService, AuditLogService, StorageService
├── repository/      # JPA repositories for all entities
├── entity/          # User, Document, Institution, RefreshToken, VerificationLog
├── dto/             # Request/Response DTOs + ApiResponse wrapper
├── security/        # JwtUtil, CustomUserDetailsService
├── enums/           # Role, DocumentStatus, AuditAction
└── exception/       # GlobalExceptionHandler + custom exceptions
```

---

## ⚙️ Setup

### Prerequisites
- Java 17
- PostgreSQL 14+
- Redis 7+
- AWS S3 bucket (or use LocalStack for local dev)

### 1. Clone and configure

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/docuverify
spring.datasource.username=YOUR_PG_USER
spring.datasource.password=YOUR_PG_PASSWORD

aws.access-key=YOUR_ACCESS_KEY
aws.secret-key=YOUR_SECRET_KEY
aws.s3.bucket=YOUR_BUCKET_NAME
aws.s3.region=ap-south-1
```

### 2. Create the database

```sql
CREATE DATABASE docuverify;
```

### 3. Run

```bash
./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

---

## 🔐 Auth Flow

```
POST /api/auth/register   → Register user
POST /api/auth/login      → Get Access + Refresh tokens
POST /api/auth/refresh    → Rotate tokens (refresh token rotation)
POST /api/auth/logout     → Revoke all refresh tokens
GET  /api/auth/me         → Get current user
```

**Access Token:** 15 min lifetime, sent as `Authorization: Bearer <token>`  
**Refresh Token:** 7 days, stored in DB with revocation support

---

## 📄 Document Flow

```
POST   /api/documents                  → Upload document (multipart)
GET    /api/documents/my               → My documents (paginated)
GET    /api/documents/{id}             → Get by ID
PATCH  /api/documents/{id}/submit      → Submit for review (UPLOADED → UNDER_REVIEW)
DELETE /api/documents/{id}             → Delete (non-APPROVED only)
GET    /api/documents/pending          → Pending review queue (verifiers)
```

---

## ✅ Verification Flow

```
POST /api/verification/approve         → UNDER_REVIEW → APPROVED
POST /api/verification/reject          → UNDER_REVIEW → REJECTED
GET  /api/verification/logs/{docId}    → Full audit trail

GET  /api/public/verify/{token}        → Public QR/link verification (no auth)
GET  /api/public/health                → Health check
```

---

## 🛡️ Security Features

- **BCrypt** password hashing
- **RBAC**: `ROLE_USER`, `ROLE_VERIFIER`, `ROLE_ADMIN`, `ROLE_INSTITUTION_ADMIN`
- **JWT** access tokens (15 min) + refresh token rotation (7 days)
- **Token blacklisting** on logout via DB revocation flag
- **Redis rate limiting**: 100 req/min per user (IP fallback for public endpoints)
- **SHA-256 file hashing** for tamper detection and deduplication

---

## 🏢 Multi-Tenancy

Each `Institution` is a tenant. Users belong to institutions, and documents are scoped to institutions. Verifiers only see documents from their own institution.

---

## 📜 Audit Logs

Every state change is immutably logged in `verification_logs`:
- `UPLOADED` → `SUBMITTED_FOR_REVIEW` → `APPROVED` / `REJECTED`
- `PUBLIC_VERIFIED` logged on every public QR scan
- Stores: actor email, IP address, timestamp, remarks

---

## 🔄 Document State Machine

```
UPLOADED ──→ UNDER_REVIEW ──→ APPROVED
                    └──────────→ REJECTED
```

---

## 🧪 Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory DB via `application-test.properties`.
