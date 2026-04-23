# DocuVerify: End-to-End Flow & Architecture

This document explains the entire lifecycle of a document within the DocuVerify platform, detailing the steps from user registration to public cryptographic verification.

---

## 1. Platform Setup & Role Assignment

The platform operates on a strict Role-Based Access Control (RBAC) system.

- **Platform Admin:** The system seeds a default Platform Admin (`admin@docuverify.com`) and a "Default Institution" on startup.
- **Institutions:** The Platform Admin can create and manage multiple Institutions (e.g., universities, corporations) via the Admin Dashboard.
- **User Registration:** End-users register via the public registration page. To streamline onboarding, they are assigned `ROLE_USER` and attached to the "Default Institution" automatically.
- **Role Upgrades:** The Platform Admin can promote users to `ROLE_VERIFIER` or `ROLE_INSTITUTION_ADMIN` and securely restrict their access to a specific institution.

---

## 2. Document Upload & Cryptographic Hashing

- **Upload:** An authenticated user (`ROLE_USER`) uploads a document (PDF, TXT, PNG, etc.) from their dashboard.
- **Hashing (SHA-256):** Before saving the file, the backend generates a **SHA-256 hash** of the file's raw bytes. This acts as a digital fingerprint. If a single pixel or character in the file changes later, the hash will drastically change, exposing the tampering.
- **Storage:** The physical file is saved to the local file system (in the `uploads/` directory), and its metadata (name, original hash, uploader ID, institution ID) is securely saved to the PostgreSQL database with an initial status of `UPLOADED`.

---

## 3. Submission & Review Queue

- **Submission:** The uploader submits the uploaded document for verification. The document's status safely transitions from `UPLOADED` to `UNDER_REVIEW`.
- **Review Queue:** Users with the `ROLE_VERIFIER` (or higher) role within the same institution can view the "Review Queue" dashboard, which fetches all documents currently awaiting verification.

---

## 4. Verification & Approval

- **Verifier Action:** A verifier inspects the submitted document and can either:
  - **Approve:** The document's status updates to `APPROVED`.
  - **Reject:** The document's status updates to `REJECTED`.
- **Audit Logging:** Every state transition (upload, submit, approve, reject) generates an immutable `VerificationLog` entry in the database. This acts as a permanent audit trail tracking _who_ performed the action, _when_, and from _what IP address_.
- **Public Token:** Upon approval, the system generates a unique public identifier called the `verificationToken`.

---

## 5. Public Verification & Tamper Check

- **Sharing:** The original uploader receives the `verificationToken` and can share it (or a direct URL) with external third parties (e.g., employers, auditors, government agencies).
- **Public Proof:** Anyone can visit the public verification portal on the application and enter the token without needing to log in or create an account.
- **Real-Time Tamper Check:** When the token is queried, the backend fetches the physical file from the disk, recalculates its SHA-256 hash in real-time, and compares it against the original hash generated at the time of upload.
- **Result:** The system returns a cryptographically backed proof of authenticity. It displays the document's metadata, validation timestamp, and a crucial `tamperDetected` boolean flag confirming the file has not been altered since the moment it was uploaded.
