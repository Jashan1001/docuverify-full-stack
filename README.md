# DocuVerify — Document Trust Platform

DocuVerify is a full-stack document verification platform built with Spring Boot (Java 21) and React (Vite). It utilizes a modern neo-brutalist aesthetic and cryptographic SHA-256 hashing to ensure document immutability and trust.

## 🚀 Seeded Admin Credentials

The platform automatically seeds an initial platform administrator account upon first backend startup. You can use these credentials to access the administrative dashboard, create institutions, and assign roles to other users.

**Platform Admin Account:**

- **Email:** `admin@docuverify.com`
- **Password:** `ChangeMe@123` (override with `SEEDER_ADMIN_PASSWORD`)

> **Note:** Any new users must be registered via the public registration page. A Platform Admin must then assign them an institution and role before they can upload or verify documents.

## 🛠️ Prerequisites

Make sure the following are installed and running on your system:

- Java 21 (`java -version`)
- Node.js 18+ (`node -v`)
- PostgreSQL 14+ (running on default port 5432)
- Redis 7+ (running on default port 6379)

## 🏃‍♂️ Getting Started

### 1. Database Setup

Ensure your PostgreSQL instance has a database named `docuverify`.

```bash
# If it doesn't exist, create it:
psql -U postgres -c "CREATE DATABASE docuverify;"
```

### 2. Run the Backend (Spring Boot)

The backend runs on port `8080`.

```bash
cd backend
export PGPASSWORD=your_postgres_password # (or update backend/src/main/resources/application.properties)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw spring-boot:run
```

### 3. Run the Frontend (React / Vite)

The frontend runs on port `5173`.

```bash
cd frontend
npm install
npm run dev
```

### 4. Access the Application

Open your browser and navigate to: [http://localhost:5173](http://localhost:5173)

Enjoy verifying!
