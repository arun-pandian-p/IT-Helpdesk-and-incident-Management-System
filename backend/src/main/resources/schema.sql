-- ==============================================================================
-- Service Desk SQLite Schema
-- Enables foreign key constraints, indexes, and full relational integrity
-- ==============================================================================

PRAGMA foreign_keys = ON;

-- 1. Users
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'EMPLOYEE',
    department TEXT NOT NULL DEFAULT 'General',
    job_title TEXT,
    phone TEXT,
    location TEXT DEFAULT 'Headquarters',
    manager_id TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 2. Ticket Categories
CREATE TABLE IF NOT EXISTS ticket_categories (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    description TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL
);

-- 3. Assets
CREATE TABLE IF NOT EXISTS assets (
    id TEXT PRIMARY KEY,
    asset_tag TEXT UNIQUE NOT NULL,
    device_type TEXT NOT NULL,
    manufacturer TEXT NOT NULL,
    model TEXT NOT NULL,
    serial_number TEXT UNIQUE NOT NULL,
    status TEXT NOT NULL DEFAULT 'In Use',
    assigned_user_id TEXT,
    purchase_date TEXT,
    warranty_expiry TEXT,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_assets_tag ON assets(asset_tag);
CREATE INDEX IF NOT EXISTS idx_assets_status ON assets(status);
CREATE INDEX IF NOT EXISTS idx_assets_assigned_user ON assets(assigned_user_id);

-- 4. Asset Assignments
CREATE TABLE IF NOT EXISTS asset_assignments (
    id TEXT PRIMARY KEY,
    asset_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    assigned_by TEXT NOT NULL,
    assigned_at TEXT NOT NULL,
    returned_at TEXT,
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 5. Tickets
CREATE TABLE IF NOT EXISTS tickets (
    id TEXT PRIMARY KEY,
    ticket_number TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category_id TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'Medium',
    status TEXT NOT NULL DEFAULT 'Open',
    impact TEXT NOT NULL DEFAULT 'Individual',
    urgency TEXT NOT NULL DEFAULT 'Medium',
    priority_override_reason TEXT,
    asset_id TEXT,
    device_type TEXT,
    os_name TEXT,
    application_name TEXT,
    location TEXT,
    created_by TEXT NOT NULL,
    assigned_to TEXT,
    due_at TEXT NOT NULL,
    first_response_at TEXT,
    resolved_at TEXT,
    closed_at TEXT,
    resolution_notes TEXT,
    is_escalated INTEGER NOT NULL DEFAULT 0,
    escalation_level INTEGER NOT NULL DEFAULT 1,
    checklist_state TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES ticket_categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_tickets_number ON tickets(ticket_number);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_created_by ON tickets(created_by);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);

-- 6. Ticket Comments
CREATE TABLE IF NOT EXISTS ticket_comments (
    id TEXT PRIMARY KEY,
    ticket_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    comment TEXT NOT NULL,
    is_internal INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_ticket_comments_ticket ON ticket_comments(ticket_id);

-- 7. Ticket Status History
CREATE TABLE IF NOT EXISTS ticket_status_history (
    id TEXT PRIMARY KEY,
    ticket_id TEXT NOT NULL,
    old_status TEXT NOT NULL,
    new_status TEXT NOT NULL,
    changed_by TEXT NOT NULL,
    changed_at TEXT NOT NULL,
    reason TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_status_hist_ticket ON ticket_status_history(ticket_id);

-- 8. Ticket Assignments
CREATE TABLE IF NOT EXISTS ticket_assignments (
    id TEXT PRIMARY KEY,
    ticket_id TEXT NOT NULL,
    assigned_to TEXT NOT NULL,
    assigned_by TEXT NOT NULL,
    assigned_at TEXT NOT NULL,
    unassigned_at TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 9. Ticket Escalations
CREATE TABLE IF NOT EXISTS ticket_escalations (
    id TEXT PRIMARY KEY,
    ticket_id TEXT NOT NULL,
    from_engineer TEXT NOT NULL,
    to_engineer TEXT,
    reason TEXT NOT NULL,
    escalation_level INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    resolved_at TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (from_engineer) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_engineer) REFERENCES users(id) ON DELETE SET NULL
);

-- 10. Ticket Feedback
CREATE TABLE IF NOT EXISTS ticket_feedback (
    id TEXT PRIMARY KEY,
    ticket_id TEXT UNIQUE NOT NULL,
    user_id TEXT NOT NULL,
    rating INTEGER NOT NULL,
    comments TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 11. Knowledge Articles
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    os_target TEXT,
    content TEXT NOT NULL,
    tags TEXT,
    view_count INTEGER NOT NULL DEFAULT 0,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    not_helpful_count INTEGER NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_kb_category ON knowledge_articles(category);
CREATE INDEX IF NOT EXISTS idx_kb_title ON knowledge_articles(title);

-- 12. Onboarding Requests
CREATE TABLE IF NOT EXISTS onboarding_requests (
    id TEXT PRIMARY KEY,
    employee_name TEXT NOT NULL,
    department TEXT NOT NULL,
    job_title TEXT NOT NULL,
    manager_name TEXT NOT NULL,
    start_date TEXT NOT NULL,
    location TEXT NOT NULL DEFAULT 'Headquarters',
    device_requirement TEXT NOT NULL DEFAULT 'Standard Laptop',
    software_requirement TEXT,
    access_requirement TEXT,
    status TEXT NOT NULL DEFAULT 'In Progress',
    progress_percent INTEGER NOT NULL DEFAULT 0,
    created_by TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 13. Onboarding Tasks
CREATE TABLE IF NOT EXISTS onboarding_tasks (
    id TEXT PRIMARY KEY,
    request_id TEXT NOT NULL,
    task_name TEXT NOT NULL,
    owner_id TEXT,
    status TEXT NOT NULL DEFAULT 'Not Started',
    due_date TEXT,
    completed_at TEXT,
    notes TEXT,
    FOREIGN KEY (request_id) REFERENCES onboarding_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_onboarding_tasks_req ON onboarding_tasks(request_id);

-- 14. Access Requests
CREATE TABLE IF NOT EXISTS access_requests (
    id TEXT PRIMARY KEY,
    request_type TEXT NOT NULL,
    requested_system TEXT NOT NULL,
    access_level TEXT NOT NULL DEFAULT 'Read/Write',
    reason TEXT NOT NULL,
    requested_by TEXT NOT NULL,
    approved_by TEXT,
    assigned_engineer_id TEXT,
    status TEXT NOT NULL DEFAULT 'Requested',
    completed_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_engineer_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 15. Improvement Requests
CREATE TABLE IF NOT EXISTS improvement_requests (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    business_benefit TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Proposed',
    priority TEXT NOT NULL DEFAULT 'Medium',
    submitted_by TEXT NOT NULL,
    assigned_to TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (submitted_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

-- 16. Service Request Types
CREATE TABLE IF NOT EXISTS service_request_types (
    id TEXT PRIMARY KEY,
    name TEXT UNIQUE NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'General IT',
    default_priority TEXT NOT NULL DEFAULT 'Medium',
    approval_required INTEGER NOT NULL DEFAULT 0,
    default_sla_hours INTEGER NOT NULL DEFAULT 24,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- 17. Service Requests
CREATE TABLE IF NOT EXISTS service_requests (
    id TEXT PRIMARY KEY,
    request_number TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    priority TEXT NOT NULL DEFAULT 'Medium',
    status TEXT NOT NULL DEFAULT 'Submitted',
    request_type_id TEXT NOT NULL,
    requester_id TEXT NOT NULL,
    approval_required INTEGER NOT NULL DEFAULT 0,
    approval_status TEXT NOT NULL DEFAULT 'None',
    approver_id TEXT,
    approval_reason TEXT,
    approved_at TEXT,
    assigned_to TEXT,
    assigned_team TEXT DEFAULT 'Service Desk',
    business_justification TEXT NOT NULL,
    required_date TEXT,
    asset_id TEXT,
    application_name TEXT,
    access_level TEXT,
    software_name TEXT,
    software_version TEXT,
    device_type TEXT,
    fulfillment_details TEXT,
    resolution_notes TEXT,
    due_at TEXT NOT NULL,
    fulfilled_at TEXT,
    closed_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (request_type_id) REFERENCES service_request_types(id) ON DELETE RESTRICT,
    FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (approver_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_sr_number ON service_requests(request_number);
CREATE INDEX IF NOT EXISTS idx_sr_status ON service_requests(status);
CREATE INDEX IF NOT EXISTS idx_sr_priority ON service_requests(priority);
CREATE INDEX IF NOT EXISTS idx_sr_requester ON service_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_sr_assigned ON service_requests(assigned_to);

-- 18. Service Request Comments
CREATE TABLE IF NOT EXISTS service_request_comments (
    id TEXT PRIMARY KEY,
    request_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    comment TEXT NOT NULL,
    is_internal INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    FOREIGN KEY (request_id) REFERENCES service_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 19. Service Request Timeline
CREATE TABLE IF NOT EXISTS service_request_timeline (
    id TEXT PRIMARY KEY,
    request_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    actor_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY (request_id) REFERENCES service_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 20. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    details TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
