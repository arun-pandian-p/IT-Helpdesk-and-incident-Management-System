# IT Helpdesk & End-User Support Management System

An enterprise-grade, **ITIL-inspired** internal service desk and technical support management application built with **FastAPI**, **PostgreSQL**, **SQLAlchemy**, **Alembic**, **React (TypeScript + Vite)**, **Tailwind CSS**, and **Recharts**.

> [!IMPORTANT]
> **Disclaimer**: This project is an **ITIL-inspired educational and portfolio implementation** demonstrating real-world technical support, incident management, hardware diagnostics, and SLA monitoring. It does not claim formal ITIL compliance or official certification.

---

## Table of Contents
1. [Overview & Business Problem](#overview--business-problem)
2. [Key Capabilities & Features](#key-capabilities--features)
3. [System Architecture](#system-architecture)
4. [Technology Stack](#technology-stack)
5. [Database Schema & ERD](#database-schema--erd)
6. [Role-Based Access Control (RBAC)](#role-based-access-control-rbac)
7. [Incident Lifecycle State Machine](#incident-lifecycle-state-machine)
8. [ITIL-Inspired Priority Matrix & SLA Logic](#itil-inspired-priority-matrix--sla-logic)
9. [Hardware Troubleshooting & OS Support Guides](#hardware-troubleshooting--os-support-guides)
10. [IT Onboarding & Day 1 Provisioning Workflows](#it-onboarding--day-1-provisioning-workflows)
11. [Access Requests & Identity Management](#access-requests--identity-management)
12. [In-App Operational Analytics & Dashboards](#in-app-operational-analytics--dashboards)
13. [REST API Specification](#rest-api-specification)
14. [Local Setup & Development](#local-setup--development)
15. [Automated Testing](#automated-testing)
16. [Docker & Container Deployment](#docker--container-deployment)
17. [Cloud Deployment Strategy (Vercel + Render + PostgreSQL)](#cloud-deployment-strategy)
18. [Portfolio Summary & Resume Bullet Points](#portfolio-summary--resume-bullet-points)

---

## Overview & Business Problem

Modern enterprise IT organizations struggle with ticket chaos, uncoordinated hardware allocations, untracked SLA breaches, and disjointed onboarding experiences. 

This platform provides an integrated internal IT service desk enabling:
- **Employees** to self-diagnose using searchable knowledge guides, report hardware/software incidents with full environmental context (device serial, OS, application, impact, urgency), request access, and track live SLA progress.
- **IT Support Engineers** to prioritize queues via an Impact × Urgency matrix, execute interactive diagnostic troubleshooting checklists, track first-response and resolution SLAs, escalate to Tier 2/Tier 3 specialists, document resolution notes, and capture Customer Satisfaction (CSAT) ratings.
- **IT Managers & Admins** to monitor operational KPIs (MTTR, first response velocity, SLA compliance, CSAT), govern hardware inventory lifecycle (warranty tracking, user allocations), supervise new hire onboarding task progression (0–100%), and maintain an immutable compliance audit trail.

---

## Key Capabilities & Features

1. **Incident & Ticket Lifecycle Management**:
   - Human-readable sequential numbering: `INC-YYYY-XXXXXX`.
   - Real-time status state machine: `Open` &rarr; `In Progress` &rarr; `Waiting for User` &rarr; `Waiting for Vendor` &rarr; `Escalated` &rarr; `Resolved` &rarr; `Closed`.
   - Ticket reopening from `Resolved` &rarr; `In Progress` when users report an issue persists.
   - Mandatory resolution notes enforced before an incident can be resolved.
2. **Impact & Urgency Priority Matrix**:
   - Computes suggested priority (`Critical`, `High`, `Medium`, `Low`) based on business impact (`Individual`, `Team`, `Department`, `Multiple Departments`, `Company-wide`) and urgency.
   - Authorized support engineers can override suggested priority with mandatory recorded rationale.
3. **Automated SLA Engine**:
   - Configurable target resolution windows (Critical: 4h, High: 24h, Medium: 48h, Low: 72h).
   - Real-time SLA derivation: `Within SLA`, `At Risk` (approaching deadline or &le; 25% remaining), and `Breached`.
   - Automatic timestamp capture for `created_at`, `first_response_at`, `due_at`, `resolved_at`, and `closed_at`.
4. **Interactive Hardware Diagnostics & Troubleshooting Checklists**:
   - Device context: Laptops, Desktops, Monitors, Printers, Docking Stations, Mobile Phones.
   - Dynamic diagnostic checklists for Hardware (power drain, cable inspection, RAM/battery checks), Operating Systems (Windows 11 DISM/sfc, macOS Jamf profiles), Microsoft 365 (Outlook profiles, Teams cache, Entra ID), and Google Workspace.
   - Saves checkbox states directly to the incident record.
5. **IT Onboarding & Equipment Provisioning**:
   - New hire intake orchestration (Employee, Department, Job Title, Manager, Start Date, Hardware Requirement).
   - Automatically generates 8 standardized IT setup tasks (Identity creation, laptop imaging, Intune/Jamf MDM enrollment, software installation, MFA/VPN setup, orientation).
   - Real-time progress bar (0–100%) recalculating dynamically upon task completion.
6. **Access & Account Requests**:
   - Service catalog for Password Resets, MFA Re-registration, Department Shared Drives, and Cloud Application Access.
   - Security approval flow (`Requested` &rarr; `Approved` &rarr; `Completed` / `Rejected`).
7. **Knowledge Base & OS Support Guides**:
   - Self-service articles with OS platform filtering (Windows 11/10, macOS Sonoma, Apple iOS, Android Enterprise).
   - User feedback thumbs voting (`helpful_count`, `not_helpful_count`).
8. **In-App Operational Analytics & Dashboards**:
   - Recharts-powered interactive analytics: 14-day volume velocity (created vs. resolved), lifecycle status distribution, priority breakdown, and SLA compliance.
9. **Security, RBAC & Regulatory Audit Trail**:
   - Secure Bcrypt password hashing and signed JWT authentication.
   - Role-based authorization enforced on the backend.
   - Immutable audit trail recording every entity creation, status change, and user assignment.

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
                                  |             FastAPI Backend              |
                                  |------------------------------------------|
                                  | - Router Layer (Auth, Tickets, Assets)   |
                                  | - Auth & RBAC Dependencies               |
                                  | - Service Layer (SLA, Priority Matrix)   |
                                  | - Audit Logging Engine                   |
                                  +------------------------------------------+
                                                       |
                                                 SQLAlchemy 2.0
                                                (ORM & Sessions)
                                                       |
                                           Alembic Database Schema
                                                       v
                                  +------------------------------------------+
                                  |         PostgreSQL Database Engine       |
                                  |------------------------------------------|
                                  | users, tickets, ticket_comments,         |
                                  | ticket_status_history, assets,           |
                                  | onboarding_requests, onboarding_tasks,   |
                                  | access_requests, knowledge_articles,     |
                                  | audit_logs                               |
                                  +------------------------------------------+
```

---
<img width="1910" height="855" alt="image" src="https://github.com/user-attachments/assets/fcec08fc-b9df-4dd8-8c97-5825dc95b29e" />

## Technology Stack

- **Backend**:
  - Python 3.12+
  - FastAPI (REST framework)
  - Pydantic v2 & Pydantic-Settings
  - SQLAlchemy 2.0 (ORM)
  - Alembic (Database migrations)
  - PostgreSQL / SQLite (Dual-mode dialect support)
  - PyJWT & Bcrypt (Authentication and password security)
  - Pytest & HTTPX (Automated test suite)
- **Frontend**:
  - React 19
  - TypeScript 5
  - Vite 6
  - Tailwind CSS
  - React Router v6
  - Axios (with JWT interceptors)
  - Recharts (Interactive SVG data visualizations)
  - Lucide React (Enterprise iconography)
- **DevOps & Infrastructure**:
  - Docker & Docker Compose
  - Multi-stage Nginx container build
  - Git version control

---

## Database Schema & ERD

### Tables
1. `users`: Identity accounts, credentials, role (`EMPLOYEE`, `SUPPORT`, `ADMIN`), department, job title, phone, location.
2. `ticket_categories`: Hardware, Software, Network, Email, Access/Login, Microsoft 365, Google Workspace, Mobile Device, Security, Printer, VPN, Other.
3. `assets`: Hardware inventory with `asset_tag`, `device_type`, `manufacturer`, `model`, `serial_number`, `status`, `assigned_user_id`, `warranty_expiry`.
4. `asset_assignments`: History of device allocations and returns.
5. `tickets`: Core incident records with `ticket_number` (`INC-YYYY-XXXXXX`), `impact`, `urgency`, `priority`, `status`, `asset_id`, `device_type`, `os_name`, `application_name`, `due_at`, `first_response_at`, `resolved_at`, `closed_at`, `resolution_notes`, `checklist_state`.
6. `ticket_comments`: Public and internal troubleshooting notes.
7. `ticket_status_history`: Audit trail for every lifecycle status transition.
8. `ticket_assignments`: Engineer allocation logs.
9. `ticket_escalations`: Tier 2 / Tier 3 escalation records with recorded reason.
10. `ticket_feedback`: CSAT ratings (1–5 stars) and user comments.
11. `knowledge_articles`: Searchable documentation with `os_target` and helpfulness votes.
12. `onboarding_requests`: New hire intake details, department, hardware needs, and `progress_percent`.
13. `onboarding_tasks`: 8 discrete technical provisioning tasks per request.
14. `access_requests`: Identity service catalog requests and approval tracking.
15. `improvement_requests`: Continuous service improvement initiatives.
16. `audit_logs`: Immutable security audit log tracking actors, actions, and entities.

---

## Role-Based Access Control (RBAC)

| Feature / Action | Employee | Support Engineer | IT Administrator |
| :--- | :---: | :---: | :---: |
| Authenticate & View Profile | ✅ | ✅ | ✅ |
| Submit Incident Ticket (with Device & Matrix) | ✅ | ✅ | ✅ |
| View Own Tickets | ✅ | ✅ | ✅ |
| Post Customer-Visible Ticket Comments | ✅ | ✅ | ✅ |
| Reopen Resolved Ticket (Issue Persists) | ✅ | ❌ | ✅ |
| Submit CSAT Rating (1–5 Stars) | ✅ | ❌ | ❌ |
| View All Incident Queues & Filters | ❌ | ✅ | ✅ |
| Assign / Reassign Support Engineers | ❌ | ✅ | ✅ |
| Transition Lifecycle Status | ❌ | ✅ | ✅ |
| Override Suggested Priority Matrix | ❌ | ✅ | ✅ |
| Execute Diagnostic Troubleshooting Checklists | ❌ | ✅ | ✅ |
| Escalate to Tier 2 / Tier 3 | ❌ | ✅ | ✅ |
| Resolve Ticket (Enforces Resolution Notes) | ❌ | ✅ | ✅ |
| Post Internal Troubleshooting Notes | ❌ | ✅ | ✅ |
| View Operational Dashboard & Analytics | ❌ | ✅ | ✅ |
| Manage Hardware Asset Inventory | ❌ | ✅ | ✅ |
| Update Onboarding Task Progress | ❌ | ✅ | ✅ |
| Approve / Fulfill Access Requests | ❌ | ✅ | ✅ |
| Publish Knowledge Base Guides | ❌ | ✅ | ✅ |
| Provision User Accounts & Switch Roles | ❌ | ❌ | ✅ |
| Inspect Immutable Security Audit Trail | ❌ | ❌ | ✅ |

---

## Incident Lifecycle State Machine

```
              +--------------+
              |     Open     |
              +--------------+
                     |
       (Assign to Support Engineer)
                     v
              +--------------+
              | In Progress  |<-----------------+
              +--------------+                  |
                |          |                    |
   (Waiting on  |          | (Escalate to       |
     Employee)  v          v  Tier 2/3)         | (Reopen: Issue
         +-----------+  +-----------+           |  Persists)
         |  Waiting  |  | Escalated |           |
         | for User  |  +-----------+           |
         +-----------+         |                |
                |              |                |
                +-------+------+                |
                        |                       |
            (Provide Resolution Notes)          |
                        v                       |
                 +--------------+               |
                 |   Resolved   |---------------+
                 +--------------+
                        |
            (Confirm Final Closure)
                        v
                 +--------------+
                 |    Closed    |
                 +--------------+
```

---

## ITIL-Inspired Priority Matrix & SLA Logic

### Impact × Urgency Matrix
Priority is derived automatically upon submission and can be overridden by support technicians:

| Business Impact \ Urgency | Critical | High | Medium | Low |
| :--- | :---: | :---: | :---: | :---: |
| **Company-wide** | **Critical** (4h) | **Critical** (4h) | **High** (24h) | **Medium** (48h) |
| **Multiple Departments** | **Critical** (4h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Department** | **High** (24h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Team** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |
| **Individual** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |

### SLA Status Classification
- **Within SLA**: Unresolved ticket with time remaining &gt; 25% of target window, or ticket resolved before `due_at`.
- **At Risk**: Time remaining &le; 4 hours or &le; 25% of target window.
- **Breached**: Current timestamp &gt; `due_at` while unresolved, or `resolved_at` &gt; `due_at`.

---

## Demo Accounts & Credentials

For portfolio evaluation, demo accounts are pre-seeded:

| Role | Email | Password | Assigned Name & Department |
| :--- | :--- | :--- | :--- |
| **Employee** | `employee@example.com` | `Password123!` | Jordan Reed (Senior Financial Analyst, Finance) |
| **Support Engineer** | `support@example.com` | `Password123!` | Alex Turner (Tier 2 Lead Support Specialist) |
| **IT Administrator** | `admin@example.com` | `Password123!` | David Miller (IT Director & Service Desk Lead) |

*The login page includes convenient 1-click demo account buttons.*

---

## REST API Specification

### Authentication
- `POST /api/auth/register` — Register a new internal employee account.
- `POST /api/auth/login` — Exchange credentials for signed JWT access token.
- `GET /api/auth/me` — Retrieve current authenticated user profile.

### Incident Management
- `POST /api/tickets` — Create a technical incident (calculates priority matrix, sets SLA due date, logs audit).
- `GET /api/tickets` — Paginated list with filtering by status, priority, category, SLA status, and keyword search.
- `GET /api/tickets/{id}` — Full incident details with timeline, diagnostic checklist, and CSAT feedback.
- `POST /api/tickets/{id}/assign` — Assign incident to support technician (auto-transitions Open &rarr; In Progress).
- `POST /api/tickets/{id}/status` — Update lifecycle status with validation and audit history.
- `POST /api/tickets/{id}/override-priority` — Override matrix priority with recorded rationale.
- `POST /api/tickets/{id}/escalate` — Escalate incident to Tier 2 / Tier 3.
- `POST /api/tickets/{id}/resolve` — Resolve incident (requires resolution notes).
- `POST /api/tickets/{id}/close` — Permanently close incident.
- `POST /api/tickets/{id}/reopen` — Reopen resolved incident back to In Progress.
- `POST /api/tickets/{id}/comments` — Post customer-facing communication or internal support notes.
- `PATCH /api/tickets/{id}/checklist` — Persist diagnostic checklist execution state.
- `POST /api/tickets/{id}/feedback` — Submit CSAT 1–5 star rating and comment.

### Hardware Asset Inventory
- `GET /api/assets` — List computing hardware inventory with device types and serials.
- `GET /api/assets/my-devices` — Retrieve devices allocated to the authenticated employee.
- `POST /api/assets` — Register new computing asset into inventory.
- `PATCH /api/assets/{id}` — Update asset status, warranty, or employee assignment.

### IT Onboarding Workflows
- `GET /api/onboarding` — List active employee onboarding workflows with progress bars.
- `POST /api/onboarding` — Submit new hire onboarding request (auto-generates 8 standardized tasks).
- `GET /api/onboarding/{id}` — Retrieve onboarding details and task breakdown.
- `PATCH /api/onboarding/tasks/{task_id}` — Toggle task status (recalculates 0–100% progress).

### Identity & Access Requests
- `GET /api/access-requests` — List access and permission requests.
- `POST /api/access-requests` — Submit request for password reset, MFA re-enrollment, or shared drive.
- `PATCH /api/access-requests/{id}/status` — Approve, fulfill, or reject access request.

### Knowledge Base & OS Support Guides
- `GET /api/knowledge` — Search support documentation with OS platform and category filtering.
- `GET /api/knowledge/{id}` — View article instructions (increments view count).
- `POST /api/knowledge` — Publish new support guide (Support/Admin).
- `POST /api/knowledge/{id}/vote` — Record helpful / not helpful feedback.

### Operational Analytics
- `GET /api/dashboard/summary` — Comprehensive real-time metrics: MTTR, SLA breaches, status distribution, and 14-day volume velocity.

### Health Check
- `GET /health` — Liveness check returning `{"status": "ok", "version": "1.0.0"}`.

---

## Local Setup & Development

### Prerequisites
- Python 3.12+
- Node.js 20+ & npm
- Git

### 1. Backend Setup
```bash
# Navigate to backend directory
cd backend

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .\.venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run database migrations
alembic upgrade head

# Seed database with 100+ tickets, assets, and demo accounts
python seed.py

# Launch FastAPI development server
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
API Documentation: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)  
Health Check: [http://127.0.0.1:8000/health](http://127.0.0.1:8000/health)

### 2. Frontend Setup
```bash
# In a separate terminal, navigate to frontend
cd frontend

# Install dependencies
npm install

# Start Vite development server
npm run dev
```
Frontend Web Portal: [http://localhost:5173](http://localhost:5173)

---

## Automated Testing

Run the automated backend test suite with `pytest`:
```bash
cd backend
python -m pytest -v
```
**Test Results**: 11 passed in ~4.5 seconds covering:
- JWT Authentication & Unauthorized Handling
- Sequential `INC-YYYY-XXXXXX` ticket number generation
- Impact × Urgency Priority Matrix calculation
- Lifecycle status transitions & invalid transition guards
- Mandatory resolution notes validation
- Interactive troubleshooting checklist state persistence
- IT Onboarding task auto-generation and 0–100% progress engine
- Access request approval workflows
- Knowledge base search & helpfulness voting
- Operational dashboard summary KPIs

---

## Docker & Container Deployment

Run the complete stack (PostgreSQL 16 + FastAPI Backend + React Nginx Frontend) with one command:
```bash
docker compose up --build
```
- Frontend: `http://localhost:80`
- Backend API: `http://localhost:8000`
- PostgreSQL: `localhost:5432`

---

## Cloud Deployment Strategy

### Frontend Deployment (Vercel)
1. Push codebase to GitHub repository.
2. In Vercel, import the repository and select `frontend/` as Root Directory.
3. Framework Preset: **Vite**.
4. Configure Environment Variable:
   - `VITE_API_URL`: URL of deployed FastAPI backend (e.g., `https://api.yourhelpdesk.com`).
5. Deploy.

### Backend Deployment (Render / Railway / Fly.io)
1. Create a Managed PostgreSQL instance on Neon, Supabase, or Render.
2. Deploy the `backend/` directory using the provided `Dockerfile`.
3. Set Environment Variables:
   - `DATABASE_URL`: `postgresql://user:password@host:5432/helpdesk`
   - `JWT_SECRET`: Secure production secret key
   - `CORS_ORIGINS`: `https://your-helpdesk.vercel.app`
4. Health check endpoint: `/health`.

---

## Portfolio Summary & Resume Bullet Points

### Portfolio Summary
> "Architected and built an end-to-end enterprise IT Helpdesk & End-User Support Management System demonstrating practical ITSM and ITIL-inspired capabilities. Engineered a FastAPI and PostgreSQL backend with role-based access control, an Impact × Urgency priority matrix, automated SLA countdowns, and an interactive diagnostic troubleshooting engine for Hardware, OS, Microsoft 365, and Google Workspace issues. Designed a React 19 and Tailwind CSS portal featuring 0–100% IT onboarding task tracking, self-service access requests, and native Recharts operational dashboards."

### Resume Bullet Points
- **Full-Stack ITSM Application**: Built an ITIL-inspired Helpdesk system using FastAPI, PostgreSQL, and React 19, managing the complete incident lifecycle from intake to resolution and closure.
- **Dynamic SLA & Priority Matrix**: Implemented an automated SLA calculation engine and business Impact × Urgency matrix, reducing resolution delays by enforcing 4h–72h targets with real-time breach detection.
- **Interactive Hardware & OS Diagnostics**: Developed technician diagnostic checklist templates for Dell/Apple hardware, Windows 11, macOS, and M365 environments with persistent state tracking.
- **IT Onboarding Orchestration**: Designed an employee onboarding provisioning pipeline that auto-generates 8 standardized technical setup tasks with real-time completion tracking.
- **Operational Analytics & Dashboards**: Created executive KPI dashboards and Recharts visualizations tracking MTTR, incident volume velocity, CSAT satisfaction, and staff utilization.
