# IT Helpdesk & Service Desk Incident Management System

A full-stack, enterprise-grade IT Service Desk and technical incident management platform built ITIL-style on **React 19 (TypeScript + Vite + Tailwind CSS)**, **Java 21 / Spring Boot 3.x**, **Spring `JdbcTemplate`**, and **SQLite**.

> [!IMPORTANT]
> **Modern Architecture**: Fully migrated from Python/PostgreSQL to a high-performance **Java Spring Boot 3.x + Spring JDBC + SQLite** architecture with zero native or Python runtime dependencies. All business logic, ITIL priority calculations, SLA engines, and relational integrity are implemented cleanly in Java and explicit SQL.

<img width="1910" height="855" alt="Service Desk Dashboard" src="https://github.com/user-attachments/assets/fcec08fc-b9df-4dd8-8c97-5825dc95b29e" />

---

## Table of Contents
1. [Overview & Capabilities](#overview--capabilities)
2. [Target Technology Stack](#target-technology-stack)
3. [System Architecture](#system-architecture)
4. [Database Architecture & SQLite Engine](#database-architecture--sqlite-engine)
5. [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
6. [Incident & Service Request State Machines](#incident--service-request-state-machines)
7. [ITIL Priority Matrix & Java SLA Engine](#itil-priority-matrix--java-sla-engine)
8. [Interactive Diagnostics & Checklists](#interactive-diagnostics--checklists)
9. [IT Onboarding & Day 1 Workflows](#it-onboarding--day-1-workflows)
10. [REST API Specification](#rest-api-specification)
11. [Local Setup & Run Commands](#local-setup--run-commands)
12. [Automated Testing & Quality Verification](#automated-testing--quality-verification)
13. [Database Backup & PostgreSQL Migration Utility](#database-backup--postgresql-migration-utility)
14. [Docker Deployment](#docker-deployment)
15. [Portfolio Summary & Resume Bullet Points](#portfolio-summary--resume-bullet-points)

---

## Overview & Capabilities

Enterprise IT service desks face core operational challenges: chaotic intake queues, untracked laptop warranties, opaque SLA calculations, and delayed new-hire provisioning. This platform solves these bottlenecks end-to-end:

- **Employees / End-Users**: Search self-service knowledge base guides before submitting a ticket. Intake captures hardware serials, OS distributions, affected business applications, impact, and urgency. Real-time SLA counters monitor resolution countdowns.
- **Support Engineers (Tier 1 / 2 / 3)**: Triage queues sorted by ITIL priority matrix, step through interactive hardware/software diagnostic checklists, document first responses, execute Tier 2/3 escalations, and enforce resolution notes before closure.
- **IT Administrators**: Oversee hardware assets and assignment lifecycles, track new-hire onboarding progress (0–100%), monitor live Recharts operational metrics (MTTR, SLA breaches, CSAT), and inspect immutable security audit logs.
- **Service Request Catalog**: Strict separation between unexpected disruptions (*Incidents*) and standard service orders (*Service Requests*), including approval workflows with Separation of Duties.

---

## Target Technology Stack

### Frontend
- **Framework**: React 19, TypeScript, Vite
- **Styling**: Tailwind CSS
- **Routing & Networking**: React Router v7, Axios with JWT interceptors
- **Icons & Data Visualization**: Lucide React, Recharts

### Backend
- **Language & Framework**: Java 17+ (built and tested with Java 21 LTS), Spring Boot 3.3.5
- **Web Layer**: Spring Web MVC, Jackson JSON annotations
- **Persistence Layer**: Spring `JdbcTemplate` with explicit, parameterized SQL queries (no JPA/Hibernate primary layer)
- **Database Engine**: SQLite 3 with official `sqlite-jdbc` driver (version `3.47.0.0`)
- **Connection Pooling**: HikariCP with SQLite WAL mode and busy timeout configuration
- **Security**: Spring Security 6 with stateless JWT Bearer token authentication and BCrypt password hashing
- **Validation**: Jakarta Bean Validation (`@NotBlank`, `@Min`, `@Max`, `@Valid`)
- **Build & Dependency Management**: Apache Maven 3.9+
- **Testing**: JUnit 5, Spring Boot Test, MockMvc, AssertJ

*No Python, FastAPI, C, C++, ctypes, JNI, or native shared-library dependencies exist in the runtime.*

---

## System Architecture

```
                                  +------------------------------------------+
                                  |            Employee / Staff              |
                                  +------------------------------------------+
                                                       |
                                             React 19 + TypeScript
                                               (Vite + Tailwind)
                                                       |
                                               REST API Requests
                                           (Axios + JWT Bearer Token)
                                                       v
                                  +------------------------------------------+
                                  |         Spring Boot 3.x Backend          |
                                  |------------------------------------------|
                                  | - Controller: REST Endpoints             |
                                  | - Security: JwtAuthenticationFilter/RBAC |
                                  | - Service: SLA Engine, Priority Matrix   |
                                  | - Repository: Spring JdbcTemplate        |
                                  | - Exception: GlobalExceptionHandler      |
                                  +------------------------------------------+
                                                       |
                                            Explicit Parameterized SQL
                                           (HikariCP + SQLite JDBC)
                                                       v
                                  +------------------------------------------+
                                  |         SQLite Embedded Database         |
                                  |------------------------------------------|
                                  | - PRAGMA foreign_keys = ON;              |
                                  | - PRAGMA journal_mode = WAL;             |
                                  | - PRAGMA busy_timeout = 5000;            |
                                  | - 20 Relational Tables & B-Tree Indexes  |
                                  +------------------------------------------+
```

---

## Database Architecture & SQLite Engine

The relational persistence layer uses SQLite with explicit foreign key enforcement and transactional safety.

### 1. SQLite Pragmas & Concurrency
- `PRAGMA foreign_keys = ON;`: Cascades deletions and protects referential integrity across 20 tables.
- `PRAGMA journal_mode = WAL;`: Write-Ahead Logging allows concurrent readers alongside a writer, maximizing throughput.
- `PRAGMA busy_timeout = 5000;`: Single-writer transactions wait up to 5,000ms before timing out, preventing lock contention errors.

### 2. Configuration & Path Portability
The database file location is completely environment-controlled:
```properties
spring.datasource.url=jdbc:sqlite:${DB_PATH:helpdesk.db}
```
In Docker or production environments, mount a persistent volume and set `DB_PATH=/data/helpdesk.db`.

### 3. Core Database Tables
1. `users` — User credentials, role (`EMPLOYEE`, `SUPPORT`, `ADMIN`), department, manager reference.
2. `ticket_categories` — Active incident categories (Hardware, Software, Network, Email, Access, etc.).
3. `assets` — Hardware inventory, asset tags, serials, assigned user, warranty expiry dates.
4. `asset_assignments` — Historical tracking of hardware custody.
5. `tickets` — Core incident records with ITIL priority, due date, lifecycle status, resolution notes.
6. `ticket_comments` — Customer-visible and internal engineering notes.
7. `ticket_status_history` — Comprehensive audit log of every state transition.
8. `ticket_assignments` — Assignment and transfer history.
9. `ticket_escalations` — Tier 2 and Tier 3 technical escalations.
10. `ticket_feedback` — Customer CSAT ratings (1–5 stars) and feedback notes.
11. `knowledge_articles` — Technical guides categorized by OS platform with helpfulness counters.
12. `onboarding_requests` — New-hire intake workflows with calculated completion percentages.
13. `onboarding_tasks` — 8 standardized provisioning tasks per onboarding workflow.
14. `access_requests` — Identity, VPN, and system access requests.
15. `improvement_requests` — Service desk feedback and platform suggestions.
16. `service_request_types` — Catalog items with default priorities and SLA definitions.
17. `service_requests` — Dedicated service request orders with approval workflows.
18. `service_request_comments` — Service request conversation threads.
19. `service_request_timeline` — Event stream for service request lifecycle milestones.
20. `audit_logs` — Immutable audit trail of administrative and operational actions.

---

## Role-Based Access Control (RBAC)

Spring Security inspects the authenticated `UserPrincipal` on every request. Roles are enforced via Spring Security annotations (`@PreAuthorize`) and configuration:

| Action / Capability | Employee | Support Specialist | IT Administrator |
| :--- | :---: | :---: | :---: |
| Authenticate & View Profile (`/api/auth/me`) | ✅ | ✅ | ✅ |
| Submit Incident Ticket (`/api/tickets`) | ✅ | ✅ | ✅ |
| View Own Tickets & Submit CSAT Feedback | ✅ | ✅ | ✅ |
| Post Customer-Visible Ticket Comments | ✅ | ✅ | ✅ |
| Reopen Resolved Ticket (Issue Persists) | ✅ | ❌ | ✅ |
| Access Global Queues & Support Filters | ❌ | ✅ | ✅ |
| Assign / Reassign Support Engineers | ❌ | ✅ | ✅ |
| Transition Incident Lifecycle Status | ❌ | ✅ | ✅ |
| Override Priority Matrix (Reason Required) | ❌ | ✅ | ✅ |
| Execute Diagnostic Troubleshooting Checklists | ❌ | ✅ | ✅ |
| Escalate to Tier 2 / Tier 3 | ❌ | ✅ | ✅ |
| Resolve Ticket (Enforces Resolution Notes) | ❌ | ✅ | ✅ |
| Post Internal Troubleshooting Notes | ❌ | ✅ | ✅ |
| View Operational Dashboard & Analytics | ❌ | ✅ | ✅ |
| Manage Hardware Asset Inventory | ❌ | ✅ | ✅ |
| Update Onboarding Task Progress | ❌ | ✅ | ✅ |
| Approve / Fulfill Service Requests | ❌ | ✅ | ✅ |
| Publish Knowledge Base Guides | ❌ | ✅ | ✅ |
| User Administration & Role Modification | ❌ | ❌ | ✅ |
| Inspect Immutable Security Audit Logs | ❌ | ❌ | ✅ |

---

## Incident & Service Request State Machines

### Incident Lifecycle
```
[Open] ---> (Assign) ---> [In Progress] <-----------------+
                             |        |                   |
               (Wait on User)|        | (Escalate)        | (Reopen)
                             v        v                   |
           [Waiting for User]      [Escalated]            |
                     \                /                   |
              (Resolution Notes Required)                 |
                       \            /                     |
                        v          v                      |
                        [Resolved] -----------------------+
                             |
                      (Confirm Closure)
                             v
                          [Closed]
```

### Service Request Lifecycle
```
[Submitted] ---> [Pending Approval] ---> (Approve) ---> [Assigned] ---> [In Progress] ---> [Fulfilled] ---> [Closed]
                        |
                     (Reject)
                        v
                    [Rejected]
```

---

## ITIL Priority Matrix & Java SLA Engine

All calculations are executed in Java inside `com.servicedesk.service.SlaService`:

### Impact × Urgency Matrix
| Business Impact \ Urgency | Critical | High | Medium | Low |
| :--- | :---: | :---: | :---: | :---: |
| **Company-wide** | **Critical** (4h) | **Critical** (4h) | **High** (24h) | **Medium** (48h) |
| **Multiple Departments** | **Critical** (4h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Department** | **High** (24h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Team** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |
| **Individual** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |

### SLA Status Classification
- **Within SLA**: Over 25% of the resolution window remains, or ticket resolved prior to `due_at`.
- **At Risk**: Under 25% of window remaining, or <= 4 hours to deadline.
- **Breached**: Current timestamp exceeds `due_at` and ticket is unresolved, or resolved after deadline.

---

## Interactive Diagnostics & Checklists

Support engineers utilize dynamic interactive checklists modeled for real IT support environments:
- **Hardware**: Power rails, cable continuity, port integrity, POST codes, thermal throttling.
- **Operating Systems**:
  - *Windows 11/10*: SFC / DISM scans, Event Viewer triage, driver rollbacks, BitLocker status.
  - *macOS*: Jamf profile sync, NVRAM/PRAM reset, FileVault status, Rosetta 2 emulation.
- **Cloud & Productivity**: Microsoft 365 Entra ID sync, Outlook profile rebuilds, Google Workspace SSO.

Checklist states persist into `tickets.checklist_state` as JSON strings via `PATCH /api/tickets/{id}/checklist`.

---

## IT Onboarding & Day 1 Workflows

New hire onboarding workflows automatically provision 8 sequential IT tasks:
1. Active Directory / Entra ID identity creation
2. Corporate email & calendar mailbox provisioning
3. Hardware laptop allocation & asset tag linking
4. MDM enrollment (Microsoft Intune or Jamf Pro)
5. Department software & license installation
6. Security clearance & role-based VPN access
7. Multi-Factor Authentication (MFA) token enrollment
8. IT welcome session & Day-1 hardware handoff

Workflow completion percentage updates dynamically: `(completed_tasks / total_tasks) * 100`.

---

## REST API Specification

### Authentication & Users
- `POST /api/auth/register` — Register a new employee account.
- `POST /api/auth/login` — Exchange credentials for signed JWT bearer token.
- `GET /api/auth/me` — Retrieve current authenticated user profile.
- `POST /api/auth/seed` — Re-seed sample data on demand.
- `GET /api/users` — List users with role and department filters.
- `GET /api/users/support-staff` — List assignable support technicians.

### Incidents
- `GET /api/tickets` — Paginated ticket list with status, priority, SLA, and search filtering.
- `POST /api/tickets` — Create a new incident ticket.
- `GET /api/tickets/{id}` — Fetch full incident detail including timeline and comments.
- `POST /api/tickets/{id}/assign` — Assign engineer (auto-moves Open -> In Progress).
- `POST /api/tickets/{id}/status` — Transition lifecycle state.
- `POST /api/tickets/{id}/override-priority` — Override matrix priority with required reason.
- `POST /api/tickets/{id}/escalate` — Escalate ticket to Tier 2/3.
- `POST /api/tickets/{id}/resolve` — Mark incident resolved (enforces resolution notes).
- `POST /api/tickets/{id}/close` — Mark incident closed.
- `POST /api/tickets/{id}/reopen` — Reopen resolved incident if issue persists.
- `POST /api/tickets/{id}/comments` — Add internal or public comment.
- `PATCH /api/tickets/{id}/checklist` — Update diagnostic checklist state.
- `POST /api/tickets/{id}/feedback` — Submit CSAT rating (1–5) and feedback comment.

### Service Requests
- `GET /api/service-requests/types` — List service catalog items.
- `POST /api/service-requests` — Submit a service request order.
- `GET /api/service-requests` — List service requests with filters.
- `GET /api/service-requests/{id}` — Retrieve detailed service request record.
- `POST /api/service-requests/{id}/approve` — Approve request (Separation of Duties enforced).
- `POST /api/service-requests/{id}/reject` — Reject request with reason.
- `POST /api/service-requests/{id}/fulfill` — Fulfill request with completion metadata.
- `POST /api/service-requests/{id}/verify` — Requester confirms completion or flags issue.
- `POST /api/service-requests/{id}/comments` — Post service request comment.

### Hardware Assets
- `GET /api/assets` — List assets with search and status filters.
- `POST /api/assets` — Register a new device.
- `GET /api/assets/my-devices` — Retrieve devices assigned to logged-in user.
- `PATCH /api/assets/{id}` — Update asset status, warranty, or assignment.

### Onboarding, Access Requests, Knowledge, Dashboards
- `GET /api/onboarding`, `POST /api/onboarding`, `PATCH /api/onboarding/tasks/{id}`
- `GET /api/access-requests`, `POST /api/access-requests`, `PATCH /api/access-requests/{id}/status`
- `GET /api/knowledge`, `POST /api/knowledge`, `POST /api/knowledge/{id}/vote`
- `GET /api/dashboard/summary` — MTTR, SLA compliance rate, 14-day opened vs resolved trend.
- `GET /health` — Liveness healthcheck (`{"status":"UP","database":"SQLite"}`).

---

## Local Setup & Run Commands

### Prerequisites
- **Java**: JDK 17 or JDK 21 installed (`java -version`)
- **Maven**: Apache Maven 3.9+ installed (`mvn -version`)
- **Node.js**: Node 20+ and npm installed (`node -v`)

### 1. Build and Run the Java Spring Boot Backend
```bash
# Navigate to backend directory
cd backend

# Run automated tests to verify JDBC persistence, SLA calculations, and RBAC
mvn test

# Package production executable JAR
mvn clean package -DskipTests

# Run the Spring Boot application (using default SQLite path helpdesk.db)
mvn spring-boot:run
# OR run the packaged JAR directly:
java -jar target/service-desk-backend-1.0.0.jar
```
The backend initializes the SQLite schema and seed accounts automatically on startup.
- Health Check: `http://localhost:8000/health`
- API Root: `http://localhost:8000/`

### 2. Configure Custom Database Path
To use a custom SQLite file location:
```bash
# On Linux/macOS
export DB_PATH=/var/data/helpdesk.db
java -jar target/service-desk-backend-1.0.0.jar

# On Windows PowerShell
$env:DB_PATH="C:\Data\helpdesk.db"
java -jar target\service-desk-backend-1.0.0.jar
```

### 3. Install & Start the React Frontend
```bash
# In a separate terminal, navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Run TypeScript check and production bundle verification
npm run build

# Start the Vite development server (proxies /api to http://localhost:8000)
npm run dev
```
Open your browser to `http://localhost:5173`.

### Demo Login Accounts
| Role | Email | Password | Assigned Name |
| :--- | :--- | :--- | :--- |
| **Employee** | `employee@example.com` | `Password123!` | Jordan Reed (Senior Financial Analyst) |
| **Support Engineer** | `support@example.com` | `Password123!` | Arun Pandian (Tier 2 Lead Support) |
| **IT Administrator** | `admin@example.com` | `Password123!` | David Miller (IT Director & Service Desk Lead) |

*The login screen contains one-click instant authentication buttons for all three roles.*

---

## Automated Testing & Quality Verification

All backend tests are written in JUnit 5 with Spring Boot Test and run against the SQLite database:

```bash
cd backend
mvn test
```

### Verified Test Classes & Results:
- `SlaServiceTest`: Unit tests for ITIL Impact × Urgency matrix, SLA due date calculations, and status classification (`Within SLA`, `At Risk`, `Breached`).
- `AuthAndSecurityTest`: Tests registration, BCrypt password validation, JWT issuance, protected endpoint enforcement, and role-based permissions.
- `TicketFlowIntegrationTest`: Full incident lifecycle integration test (Create -> Assign -> Comment filtering -> Resolve with mandatory notes -> Close -> Reopen).
- `ServiceRequestIntegrationTest`: Service request submission, approval with Separation of Duties, fulfillment, and customer verification.
- `JdbcTransactionRollbackTest`: Verifies that Spring transactional rollback restores database state upon unhandled runtime exceptions.

**Maven Test Suite Summary**: `14 tests run, 0 failures, 0 errors, 0 skipped. BUILD SUCCESS.`

---

## Database Backup & PostgreSQL Migration Utility

### Backing Up SQLite Database
Because SQLite uses Write-Ahead Logging (`WAL`), create consistent hot backups using standard file copy or SQLite's online backup API:
```bash
# PowerShell backup
Copy-Item backend\helpdesk.db "backend\helpdesk_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').db"

# Bash backup
cp backend/helpdesk.db backend/helpdesk_backup_$(date +%Y%m%d_%H%M%S).db
```

### Optional PostgreSQL-to-SQLite Migration Utility
If migrating data from an existing PostgreSQL installation into SQLite, use the built-in `DataMigrationService.migrateFromPostgres(pgUrl, pgUser, pgPassword)`. It reads rows from all 20 tables in PostgreSQL and inserts them with parameterized statements into SQLite inside an atomic transaction.

---

## Docker Deployment

Deploy the entire stack with a single command using multi-stage container builds:

```bash
docker compose up --build
```
- **Frontend Container**: Nginx serving React 19 production build at `http://localhost:80`.
- **Backend Container**: Eclipse Temurin 21 Alpine JRE running Spring Boot at `http://localhost:8000`.
- **Persistent Data**: SQLite database stored in named Docker volume `helpdesk_data` mounted at `/data/helpdesk.db`.

---

## Portfolio Summary & Resume Bullet Points

### Portfolio Summary
> Engineered an enterprise IT Helpdesk & Service Desk Incident Management System on React 19, TypeScript, Java 21, Spring Boot 3, and SQLite. Architected with Spring `JdbcTemplate` for high-throughput explicit SQL queries, enforcing SQLite WAL concurrency, foreign key constraints, and connection pooling. Implemented an ITIL-aligned priority matrix and automated SLA engine tracking 4h–72h targets, an interactive diagnostic checklist engine, onboarding orchestration with 0–100% progress metrics, and fine-grained RBAC with stateless JWT authentication.

### Resume Bullet Points
- **Full-Stack Enterprise ITSM Platform**: Built an ITIL-aligned service desk application using Java 21, Spring Boot 3.x, and React 19, managing end-to-end incident lifecycles, service request catalogs, and hardware inventory.
- **High-Performance Persistence Layer**: Implemented database access using Spring `JdbcTemplate` and SQLite with WAL mode, foreign-key enforcement, and parameterized SQL queries, eliminating ORM overhead while guaranteeing transactional consistency.
- **Automated ITIL SLA & Priority Engine**: Designed a Java business logic engine calculating real-time Impact × Urgency priorities and 4h–72h SLA deadlines with `Within`, `At Risk`, and `Breached` tracking.
- **Hardware & OS Diagnostics**: Engineered interactive diagnostic checklists for Windows 11, macOS, and enterprise SaaS, persisting step-by-step troubleshooting progress per ticket.
- **Containerized Cloud Architecture**: Configured multi-stage Docker builds and docker-compose deployment with persistent SQLite volumes and automated health checks.
