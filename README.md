
# IT Helpdesk & End-User Support Management System

An internal service desk and technical support app, built ITIL-style, on **FastAPI**, **PostgreSQL**, **SQLAlchemy**, **Alembic**, **React (TypeScript + Vite)**, **Tailwind CSS**, and **Recharts**.

> [!IMPORTANT]
> **Disclaimer**: This is a portfolio project. It borrows ITIL concepts to model real incident management, hardware diagnostics, and SLA tracking, but it isn't ITIL-certified and doesn't claim to be.
<img width="1910" height="855" alt="image" src="https://github.com/user-attachments/assets/fcec08fc-b9df-4dd8-8c97-5825dc95b29e" />

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

Most IT teams know this pain: tickets pile up with no clear order, laptops get handed out with no record of who has what, SLA deadlines slip quietly until someone complains, and new hires sit around on day one waiting for accounts that should've been ready.

This platform is my attempt to fix that, end to end.

- **Employees** search a knowledge base before opening a ticket, and when they do open one, they attach the real context: device serial, OS, which app is broken, how bad it is. They can request access and watch their SLA countdown in real time.
- **Support engineers** work a queue sorted by an Impact × Urgency matrix, run through diagnostic checklists instead of guessing, track first-response and resolution SLAs, escalate up to Tier 2/3 when needed, and close tickets with resolution notes and a CSAT rating attached.
- **IT managers and admins** watch the numbers that actually matter: MTTR, first-response speed, SLA compliance, CSAT. They also own hardware inventory (warranties, who has what), track new-hire onboarding progress from 0 to 100%, and can pull up an audit trail that can't be edited after the fact.

---

## Key Capabilities & Features

1. **Incident & Ticket Lifecycle Management**
   - Tickets get a readable ID: `INC-YYYY-XXXXXX`, not a random UUID nobody can reference on a call.
   - Status moves through a real state machine: `Open` → `In Progress` → `Waiting for User` → `Waiting for Vendor` → `Escalated` → `Resolved` → `Closed`.
   - If a "resolved" issue comes back, the ticket reopens straight into `In Progress`.
   - You can't resolve a ticket without writing down what actually fixed it.

2. **Dedicated Service Request Management (ITIL Catalog & Order Fulfillment)**
   - Strict architectural and operational separation from Incidents:
     - **Incident (`INC-YYYY-XXXXXX`)**: Unplanned interruption or reduction in quality ("Something is broken").
     - **Service Request (`SR-YYYY-XXXXXX`)**: Standard workplace service, new equipment, license, or access order ("User requesting routine assistance or item").
   - Formal multi-stage lifecycle: `Submitted` → `Pending Approval` → `Approved`/`Rejected` → `Assigned` → `In Progress` → `Fulfilled` → `Closed` (or `In Progress` on reported issue) / `Cancelled`.
   - **Separation of Duties**: Requesters are strictly prohibited from approving their own service requests.
   - **Technical Fulfillment**: Recording mandatory completion metadata (Asset Tag, Serial Number, License Key, System Role) and technical resolution notes.
   - **Customer Verification**: Requester confirms fulfillment or reports problems directly back to engineering.
   - Dedicated `/service-desk` portal landing page, `/service-desk/all` unified operational queue, and `/service-requests` catalog queue.

3. **Impact & Urgency Priority Matrix**
   - Priority (`Critical`, `High`, `Medium`, `Low`) gets calculated from business impact (`Individual` up to `Company-wide`) crossed with urgency.
   - Support engineers can override that suggestion, but only with a reason on record.

4. **Automated SLA Engine**
   - Target windows: Critical 4h, High 24h, Medium 48h, Low 72h (Service requests customized per catalog type: e.g. 4h–72h).
   - Live status: `Within SLA`, `At Risk` (25% of the window left or less), or `Breached`.
   - Every timestamp that matters gets captured automatically: `created_at`, `first_response_at`, `due_at`, `resolved_at`, `fulfilled_at`, `closed_at`.

5. **Interactive Hardware Diagnostics & Troubleshooting Checklists**
   - Covers laptops, desktops, monitors, printers, docking stations, mobile phones.
   - Checklists change based on the problem: power and cable checks for hardware, DISM/sfc for Windows 11, Jamf profiles for macOS, Outlook/Teams/Entra ID for Microsoft 365, and a separate path for Google Workspace.
   - Checkbox progress saves straight to the ticket, so nobody repeats a step the next engineer already tried.

6. **IT Onboarding & Equipment Provisioning**
   - Enter the new hire once (name, department, title, manager, start date, hardware needed).
   - Automatically provisions onboarding service requests and 8 standard setup tasks: identity creation, laptop imaging, Intune/Jamf enrollment, software installs, MFA/VPN setup, orientation.
   - A progress bar updates live as each task closes out.

7. **Access & Account Requests**
   - Covers password resets, MFA re-registration, shared drive access, and cloud app access requests.
   - Each request moves through `Requested` → `Approved` → `Completed` or `Rejected`.

8. **Knowledge Base & OS Support Guides**
   - Self-service articles filtered by platform: Windows 11/10, macOS Sonoma, iOS, Android Enterprise.
   - Users vote articles helpful or not, so the weak ones surface for a rewrite.

9. **In-App Operational Analytics & Dashboards**
   - Recharts-driven views: 14-day volume (opened vs. resolved), lifecycle distribution, priority breakdown, SLA compliance, and service request catalog distribution.

10. **Security, RBAC & Audit Trail**
    - Passwords hashed with Bcrypt, sessions handled with signed JWTs.
    - Every role's permissions are enforced on the backend, not just hidden in the UI.
    - Every creation, status change, and reassignment gets logged, and that log can't be altered after the fact.

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

## Technology Stack

- **Backend**
  - Python 3.12+
  - FastAPI (REST framework)
  - Pydantic v2 & Pydantic-Settings
  - SQLAlchemy 2.0 (ORM)
  - Alembic (database migrations)
  - PostgreSQL / SQLite (dual-mode dialect support)
  - PyJWT & Bcrypt (auth and password security)
  - Pytest & HTTPX (automated test suite)
- **Frontend**
  - React 19
  - TypeScript 5
  - Vite 6
  - Tailwind CSS
  - React Router v6
  - Axios (with JWT interceptors)
  - Recharts (interactive SVG charts)
  - Lucide React (icon set)
- **DevOps & Infrastructure**
  - Docker & Docker Compose
  - Multi-stage Nginx container build
  - Git version control

---

## Database Schema & ERD

### Tables
1. `users` — accounts, credentials, role (`EMPLOYEE`, `SUPPORT`, `ADMIN`), department, job title, phone, location.
2. `ticket_categories` — Hardware, Software, Network, Email, Access/Login, Microsoft 365, Google Workspace, Mobile Device, Security, Printer, VPN, Other.
3. `assets` — hardware inventory: `asset_tag`, `device_type`, `manufacturer`, `model`, `serial_number`, `status`, `assigned_user_id`, `warranty_expiry`.
4. `asset_assignments` — history of who had which device and when.
5. `tickets` — the core incident record: `ticket_number` (`INC-YYYY-XXXXXX`), `impact`, `urgency`, `priority`, `status`, `asset_id`, `device_type`, `os_name`, `application_name`, `due_at`, `first_response_at`, `resolved_at`, `closed_at`, `resolution_notes`, `checklist_state`.
6. `ticket_comments` — public and internal troubleshooting notes.
7. `ticket_status_history` — every lifecycle transition, logged.
8. `ticket_assignments` — who was assigned when.
9. `ticket_escalations` — Tier 2/3 escalations, with the reason recorded.
10. `ticket_feedback` — CSAT ratings (1–5 stars) plus comments.
11. `knowledge_articles` — docs, tagged by `os_target`, with helpfulness votes.
12. `onboarding_requests` — new hire intake: department, hardware needs, `progress_percent`.
13. `onboarding_tasks` — the 8 provisioning tasks per request.
14. `access_requests` — identity requests and their approval status.
15. `improvement_requests` — ideas for improving the service, tracked like anything else.
16. `audit_logs` — who did what, when, immutably.

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
Priority is set automatically the moment a ticket is submitted. Support techs can override it if the math doesn't match reality:

| Business Impact \ Urgency | Critical | High | Medium | Low |
| :--- | :---: | :---: | :---: | :---: |
| **Company-wide** | **Critical** (4h) | **Critical** (4h) | **High** (24h) | **Medium** (48h) |
| **Multiple Departments** | **Critical** (4h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Department** | **High** (24h) | **High** (24h) | **Medium** (48h) | **Low** (72h) |
| **Team** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |
| **Individual** | **High** (24h) | **Medium** (48h) | **Medium** (48h) | **Low** (72h) |

### SLA Status Classification
- **Within SLA**: more than 25% of the target window left, or already resolved before `due_at`.
- **At Risk**: 4 hours or 25% of the window left, whichever comes first.
- **Breached**: past `due_at` and still open, or was resolved after `due_at`.

---

## Demo Accounts & Credentials

For anyone evaluating the portfolio, these are pre-seeded:

| Role | Email | Password | Assigned Name & Department |
| :--- | :--- | :--- | :--- |
| **Employee** | `employee@example.com` | `Password123!` | Jordan Reed (Senior Financial Analyst, Finance) |
| **Support Engineer** | `support@example.com` | `Password123!` | Alex Turner (Tier 2 Lead Support Specialist) |
| **IT Administrator** | `admin@example.com` | `Password123!` | David Miller (IT Director & Service Desk Lead) |

*The login page has one-click buttons for all three, so you don't have to type these in.*

---

## REST API Specification

### Authentication
- `POST /api/auth/register` — register a new employee account.
- `POST /api/auth/login` — trade credentials for a signed JWT.
- `GET /api/auth/me` — get the current user's profile.

### Incident Management
- `POST /api/tickets` — create a ticket (runs the priority matrix, sets the SLA due date, logs it).
- `GET /api/tickets` — paginated list, filterable by status, priority, category, SLA state, keyword.
- `GET /api/tickets/{id}` — full detail: timeline, checklist, CSAT feedback.
- `POST /api/tickets/{id}/assign` — assign to a technician (auto-moves Open → In Progress).
- `POST /api/tickets/{id}/status` — change status, with validation and an audit entry.
- `POST /api/tickets/{id}/override-priority` — override the matrix, reason required.
- `POST /api/tickets/{id}/escalate` — send to Tier 2/3.
- `POST /api/tickets/{id}/resolve` — resolve (resolution notes required).
- `POST /api/tickets/{id}/close` — close for good.
- `POST /api/tickets/{id}/reopen` — send a resolved ticket back to In Progress.
- `POST /api/tickets/{id}/comments` — post a customer-facing or internal note.
- `PATCH /api/tickets/{id}/checklist` — save checklist progress.
- `POST /api/tickets/{id}/feedback` — submit a 1–5 star CSAT rating and comment.

### Hardware Asset Inventory
- `GET /api/assets` — list inventory with device type and serial.
- `GET /api/assets/my-devices` — devices assigned to the logged-in employee.
- `POST /api/assets` — register a new asset.
- `PATCH /api/assets/{id}` — update status, warranty, or assignment.

### IT Onboarding Workflows
- `GET /api/onboarding` — active onboarding workflows and their progress.
- `POST /api/onboarding` — start a new hire's onboarding (auto-generates the 8 tasks).
- `GET /api/onboarding/{id}` — details and task breakdown for one onboarding.
- `PATCH /api/onboarding/tasks/{task_id}` — toggle a task, progress recalculates.

### Identity & Access Requests
- `GET /api/access-requests` — list access/permission requests.
- `POST /api/access-requests` — submit one (password reset, MFA, shared drive, etc.).
- `PATCH /api/access-requests/{id}/status` — approve, fulfill, or reject.

### Knowledge Base & OS Support Guides
- `GET /api/knowledge` — search docs, filter by OS or category.
- `GET /api/knowledge/{id}` — view an article (bumps the view count).
- `POST /api/knowledge` — publish a new guide (Support/Admin only).
- `POST /api/knowledge/{id}/vote` — mark helpful or not helpful.

### Operational Analytics
- `GET /api/dashboard/summary` — the real-time numbers: MTTR, SLA breaches, status breakdown, 14-day volume.

### Health Check
- `GET /health` — liveness check, returns `{"status": "ok", "version": "1.0.0"}`.

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

Run the backend test suite with `pytest`:
```bash
cd backend
python -m pytest -v
```
**Test Results**: 11 passed in ~4.5 seconds, covering:
- JWT auth and what happens when it's missing
- Sequential `INC-YYYY-XXXXXX` ticket number generation
- The Impact × Urgency priority calculation
- Lifecycle transitions, including the ones that should be blocked
- The mandatory-resolution-notes rule
- Checklist state actually persisting
- Onboarding task auto-generation and the 0–100% progress math
- Access request approval flow
- Knowledge base search and helpfulness voting
- The dashboard summary numbers

---

## Docker & Container Deployment

Run the whole stack (PostgreSQL 16 + FastAPI + React/Nginx) with one command:
```bash
docker compose up --build
```
- Frontend: `http://localhost:80`
- Backend API: `http://localhost:8000`
- PostgreSQL: `localhost:5432`

---

## Cloud Deployment Strategy

### Frontend Deployment (Vercel)
1. Push the codebase to GitHub.
2. In Vercel, import the repo and set `frontend/` as the Root Directory.
3. Framework Preset: **Vite**.
4. Set the environment variable:
   - `VITE_API_URL`: URL of the deployed FastAPI backend (e.g., `https://api.yourhelpdesk.com`).
5. Deploy.

### Backend Deployment (Render / Railway / Fly.io)
1. Spin up a managed PostgreSQL instance on Neon, Supabase, or Render.
2. Deploy the `backend/` directory using the provided `Dockerfile`.
3. Set these environment variables:
   - `DATABASE_URL`: `postgresql://user:password@host:5432/helpdesk`
   - `JWT_SECRET`: a real production secret, not the dev one
   - `CORS_ORIGINS`: `https://your-helpdesk.vercel.app`
4. Health check endpoint: `/health`.

---

## Portfolio Summary & Resume Bullet Points

### Portfolio Summary
> Built an end-to-end IT Helpdesk & End-User Support Management System that models real ITSM and ITIL-style workflows. The backend runs on FastAPI and PostgreSQL, with role-based access control, an Impact × Urgency priority matrix, an automated SLA countdown, and an interactive diagnostic engine covering Hardware, OS, Microsoft 365, and Google Workspace issues. The frontend is React 19 and Tailwind, with 0–100% onboarding tracking, self-service access requests, and live Recharts dashboards.

### Resume Bullet Points
- **Full-Stack ITSM Application**: Built an ITIL-inspired helpdesk on FastAPI, PostgreSQL, and React 19, covering the full incident lifecycle from intake through resolution and closure.
- **Dynamic SLA & Priority Matrix**: Built an automated SLA engine and Impact × Urgency matrix that enforces 4h–72h resolution targets and flags breaches in real time.
- **Interactive Hardware & OS Diagnostics**: Built diagnostic checklist templates for Dell/Apple hardware, Windows 11, macOS, and M365, with checklist state saved as you go.
- **IT Onboarding Orchestration**: Built an onboarding pipeline that auto-generates 8 standard setup tasks per new hire and tracks completion live.
- **Operational Analytics & Dashboards**: Built KPI dashboards with Recharts tracking MTTR, ticket volume, CSAT, and staff workload.
