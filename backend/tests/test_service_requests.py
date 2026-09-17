import pytest
from app.core.security import create_access_token
from app.db.models import (
    ServiceRequest,
    ServiceRequestApprovalStatus,
    ServiceRequestStatus,
    ServiceRequestType,
    SupportTeam,
    User,
    UserRole,
)


def get_auth_token(client, email, password="Password123!"):
    resp = client.post("/api/auth/login", json={"email": email, "password": password})
    assert resp.status_code == 200, f"Login failed for {email}: {resp.text}"
    return resp.json()["access_token"]


@pytest.fixture
def auth_headers(client):
    """Provides authorization headers for employee, support engineer, and admin."""
    emp_token = get_auth_token(client, "employee@example.com")
    sup_token = get_auth_token(client, "support@example.com")
    adm_token = get_auth_token(client, "admin@example.com")

    return {
        "employee": {"Authorization": f"Bearer {emp_token}"},
        "support": {"Authorization": f"Bearer {sup_token}"},
        "admin": {"Authorization": f"Bearer {adm_token}"},
    }



def test_get_service_request_types(client, auth_headers):
    """Verifies that service request catalog types can be fetched."""
    resp = client.get("/api/service-request-types", headers=auth_headers["employee"])
    assert resp.status_code == 200
    types = resp.json()
    assert len(types) >= 15
    type_names = [t["name"] for t in types]
    assert "New Laptop" in type_names
    assert "Microsoft 365 License" in type_names
    assert "Password Reset" in type_names
    assert "Software Installation" in type_names


def test_create_service_request_numbering_and_approval_required(client, db, auth_headers):
    """Verifies that SR creation assigns an SR-YYYY-XXXXXX number and detects approval requirement."""
    # Find M365 License type (which requires approval)
    m365_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Microsoft 365 License").first()
    assert m365_type is not None

    payload = {
        "request_type_id": m365_type.id,
        "title": "Microsoft 365 Copilot License Request",
        "description": "Requesting Copilot AI license for productivity analysis and spreadsheet drafting.",
        "business_justification": "Approved by Finance VP for executive drafting workflows.",
        "application_name": "Microsoft 365 Copilot",
    }

    resp = client.post("/api/service-requests", json=payload, headers=auth_headers["employee"])
    assert resp.status_code == 201
    data = resp.json()

    assert data["request_number"].startswith("SR-2026-")
    assert data["approval_required"] is True
    assert data["status"] == ServiceRequestStatus.PENDING_APPROVAL
    assert data["approval_status"] == ServiceRequestApprovalStatus.PENDING
    assert data["title"] == payload["title"]


def test_approval_workflow_success(client, db, auth_headers):
    """Support or Admin approves pending request -> transitions to Approved."""
    emp_user = db.query(User).filter(User.email == "employee@example.com").first()
    vpn_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "VPN Access").first()

    # Create request
    sr_payload = {
        "request_type_id": vpn_type.id,
        "title": "Remote Production VPN Access",
        "description": "Need remote VPN access to troubleshoot deployment issues outside office hours.",
        "business_justification": "Primary on-call engineer for Q3 deployments.",
    }
    create_resp = client.post("/api/service-requests", json=sr_payload, headers=auth_headers["employee"])
    assert create_resp.status_code == 201
    sr_id = create_resp.json()["id"]

    # Admin approves
    approve_resp = client.post(
        f"/api/service-requests/{sr_id}/approve",
        json={"approved": True, "reason": "Authorized by director."},
        headers=auth_headers["admin"],
    )
    assert approve_resp.status_code == 200
    data = approve_resp.json()
    assert data["status"] == ServiceRequestStatus.APPROVED
    assert data["approval_status"] == ServiceRequestApprovalStatus.APPROVED
    assert data["approver_name"] is not None


def test_rejection_requires_reason(client, db, auth_headers):
    """Rejection requires mandatory reason; transitions to Rejected."""
    hw_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Hardware Replacement").first()
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": hw_type.id,
            "title": "Curved Gaming Monitor Upgrade",
            "description": "Want an ultrawide curved monitor for home office desk.",
            "business_justification": "Improves screen real estate.",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]

    # Reject without reason should fail validation
    fail_resp = client.post(
        f"/api/service-requests/{sr_id}/reject",
        json={"reason": "a"},  # Min length 3
        headers=auth_headers["admin"],
    )
    assert fail_resp.status_code == 422

    # Reject with valid reason
    reject_resp = client.post(
        f"/api/service-requests/{sr_id}/reject",
        json={"reason": "Device not covered by standard corporate hardware catalog budget."},
        headers=auth_headers["admin"],
    )
    assert reject_resp.status_code == 200
    assert reject_resp.json()["status"] == ServiceRequestStatus.REJECTED
    assert reject_resp.json()["approval_status"] == ServiceRequestApprovalStatus.REJECTED


def test_separation_of_duties_self_approval(client, db, auth_headers):
    """User cannot approve their own service request."""
    admin_user = db.query(User).filter(User.email == "admin@example.com").first()
    laptop_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "New Laptop").first()

    # Admin submits request for themselves
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": laptop_type.id,
            "title": "Admin Personal Laptop Request",
            "description": "Need new test laptop.",
            "business_justification": "Testing purposes.",
        },
        headers=auth_headers["admin"],
    )
    sr_id = create_resp.json()["id"]

    # Admin attempts to approve their own request -> 403 Forbidden
    approve_resp = client.post(
        f"/api/service-requests/{sr_id}/approve",
        json={"approved": True, "reason": "Self approving."},
        headers=auth_headers["admin"],
    )
    assert approve_resp.status_code == 403
    assert "Separation of Duties" in approve_resp.json()["detail"]


def test_assignment_to_team_and_engineer(client, db, auth_headers):
    """Assigning request sets engineer and team, transitions status to Assigned."""
    pwd_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Password Reset").first()
    support_user = db.query(User).filter(User.email == "support@example.com").first()

    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": pwd_type.id,
            "title": "Account Locked Out Assistance",
            "description": "Account locked out following multiple password attempts.",
            "business_justification": "Cannot log in to workstation.",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]

    assign_resp = client.post(
        f"/api/service-requests/{sr_id}/assign",
        json={"assigned_to": support_user.id, "assigned_team": SupportTeam.SERVICE_DESK},
        headers=auth_headers["support"],
    )
    assert assign_resp.status_code == 200
    data = assign_resp.json()
    assert data["assigned_to"] == support_user.id
    assert data["assigned_team"] == SupportTeam.SERVICE_DESK
    assert data["status"] == ServiceRequestStatus.ASSIGNED


def test_fulfillment_and_user_confirmation_lifecycle(client, db, auth_headers):
    """Complete lifecycle: Submitted -> In Progress -> Fulfilled -> Confirmed (Closed)."""
    sw_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Software Installation").first()
    support_user = db.query(User).filter(User.email == "support@example.com").first()

    # 1. Create request
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": sw_type.id,
            "title": "Postman Pro Installation",
            "description": "Need Postman desktop app installed.",
            "business_justification": "API integration tests.",
            "software_name": "Postman",
            "software_version": "10.24",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]

    # 2. Approve
    client.post(
        f"/api/service-requests/{sr_id}/approve",
        json={"approved": True},
        headers=auth_headers["admin"],
    )

    # 3. Assign and move to In Progress
    client.post(
        f"/api/service-requests/{sr_id}/assign",
        json={"assigned_to": support_user.id, "assigned_team": SupportTeam.ENDPOINT_SUPPORT},
        headers=auth_headers["support"],
    )
    client.post(
        f"/api/service-requests/{sr_id}/status",
        json={"status": ServiceRequestStatus.IN_PROGRESS},
        headers=auth_headers["support"],
    )

    # 4. Fulfill with technical completion details
    fulfill_resp = client.post(
        f"/api/service-requests/{sr_id}/fulfill",
        json={
            "fulfillment_details": {
                "software_name": "Postman Pro",
                "software_version": "10.24.0",
                "installation_status": "Installed & License Activated",
                "license_key": "LIC-POSTMAN-2026-9912",
            },
            "resolution_notes": "Software pushed via Microsoft Intune and verified operational.",
        },
        headers=auth_headers["support"],
    )
    assert fulfill_resp.status_code == 200
    data = fulfill_resp.json()
    assert data["status"] == ServiceRequestStatus.FULFILLED
    assert data["fulfilled_at"] is not None

    # 5. User confirms completion -> transitions to Closed
    confirm_resp = client.post(
        f"/api/service-requests/{sr_id}/confirm",
        json={"confirmed": True, "feedback_notes": "Postman is working great. Thank you!"},
        headers=auth_headers["employee"],
    )
    assert confirm_resp.status_code == 200
    assert confirm_resp.json()["status"] == ServiceRequestStatus.CLOSED
    assert confirm_resp.json()["closed_at"] is not None


def test_user_reports_problem_reopens_to_in_progress(client, db, auth_headers):
    """User reporting a problem on fulfilled request transitions it back to In Progress."""
    pwd_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Password Reset").first()
    support_user = db.query(User).filter(User.email == "support@example.com").first()

    # Create & move to In Progress
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": pwd_type.id,
            "title": "Active Directory Password Reset",
            "description": "Forgot domain password.",
            "business_justification": "Cannot access workstation.",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]
    client.post(
        f"/api/service-requests/{sr_id}/status",
        json={"status": ServiceRequestStatus.IN_PROGRESS},
        headers=auth_headers["support"],
    )

    # Fulfill
    client.post(
        f"/api/service-requests/{sr_id}/fulfill",
        json={"fulfillment_details": {"mfa_verified": True}},
        headers=auth_headers["support"],
    )

    # User reports problem
    report_resp = client.post(
        f"/api/service-requests/{sr_id}/report-problem",
        json={"problem_description": "The temporary password did not work on the VPN portal."},
        headers=auth_headers["employee"],
    )
    assert report_resp.status_code == 200
    data = report_resp.json()
    assert data["status"] == ServiceRequestStatus.IN_PROGRESS
    assert data["fulfilled_at"] is None


def test_cancellation_workflow(client, db, auth_headers):
    """Requester can cancel unfulfilled request; cancelled request cannot be fulfilled."""
    pwd_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Password Reset").first()
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": pwd_type.id,
            "title": "MFA App Token Reset",
            "description": "Need MFA re-registered.",
            "business_justification": "Device changed.",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]

    # Cancel
    cancel_resp = client.post(
        f"/api/service-requests/{sr_id}/cancel",
        json={"reason": "Found my backup phone with MFA active."},
        headers=auth_headers["employee"],
    )
    assert cancel_resp.status_code == 200
    assert cancel_resp.json()["status"] == ServiceRequestStatus.CANCELLED

    # Attempting to fulfill cancelled request fails
    fail_resp = client.post(
        f"/api/service-requests/{sr_id}/fulfill",
        json={"fulfillment_details": {"status": "Complete"}},
        headers=auth_headers["support"],
    )
    assert fail_resp.status_code == 400


def test_employee_rbac_isolation(client, db, auth_headers):
    """Employee cannot access or comment on another employee's private request."""
    app_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Application Access").first()

    # Admin creates request for themselves
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": app_type.id,
            "title": "Admin Private App Access",
            "description": "Confidential legal app access.",
            "business_justification": "Internal investigation.",
        },
        headers=auth_headers["admin"],
    )
    sr_id = create_resp.json()["id"]

    # Employee tries to get detail -> 403 Forbidden
    get_resp = client.get(f"/api/service-requests/{sr_id}", headers=auth_headers["employee"])
    assert get_resp.status_code == 403

    # Employee tries to comment -> 403 Forbidden
    comment_resp = client.post(
        f"/api/service-requests/{sr_id}/comments",
        json={"comment": "Unauthorized comment."},
        headers=auth_headers["employee"],
    )
    assert comment_resp.status_code == 403


def test_internal_support_note_visibility(client, db, auth_headers):
    """Support can post internal notes; internal notes are hidden from employees."""
    pwd_type = db.query(ServiceRequestType).filter(ServiceRequestType.name == "Password Reset").first()
    create_resp = client.post(
        "/api/service-requests",
        json={
            "request_type_id": pwd_type.id,
            "title": "Assisted Password Reset",
            "description": "Locked out of AD account.",
            "business_justification": "Emergency access.",
        },
        headers=auth_headers["employee"],
    )
    sr_id = create_resp.json()["id"]

    # Support posts internal note
    client.post(
        f"/api/service-requests/{sr_id}/comments",
        json={"comment": "Verified caller voice and employee ID badge.", "is_internal": True},
        headers=auth_headers["support"],
    )

    # Support posts public note
    client.post(
        f"/api/service-requests/{sr_id}/comments",
        json={"comment": "Temporary password dispatched securely via SMS.", "is_internal": False},
        headers=auth_headers["support"],
    )

    # Support views -> sees both
    sup_view = client.get(f"/api/service-requests/{sr_id}", headers=auth_headers["support"]).json()
    assert len(sup_view["comments"]) == 2

    # Employee views -> only sees public note
    emp_view = client.get(f"/api/service-requests/{sr_id}", headers=auth_headers["employee"]).json()
    assert len(emp_view["comments"]) == 1
    assert emp_view["comments"][0]["is_internal"] is False


def test_service_request_dashboard_stats(client, auth_headers):
    """Verifies operational analytics endpoint computes real KPIs and distribution maps."""
    resp = client.get("/api/service-requests/dashboard/stats", headers=auth_headers["admin"])
    assert resp.status_code == 200
    data = resp.json()

    assert "total_requests" in data
    assert "pending_approval" in data
    assert "in_progress" in data
    assert "fulfilled" in data
    assert "sla_compliance_rate" in data
    assert "by_type" in data
    assert "by_category" in data
    assert "by_status" in data
    assert "by_team" in data
    assert "daily_trend" in data
    assert len(data["daily_trend"]) == 14


def test_unified_service_desk_work(client, auth_headers):
    """Verifies unified view combines both Incidents and Service Requests with appropriate reference numbers."""
    resp = client.get("/api/service-desk/unified", headers=auth_headers["admin"])
    assert resp.status_code == 200
    items = resp.json()
    assert len(items) > 0

    ref_numbers = [item["reference_number"] for item in items]
    types = [item["item_type"] for item in items]

    assert any(ref.startswith("SR-2026-") for ref in ref_numbers)
    assert any(ref.startswith("INC-2026-") for ref in ref_numbers)
    assert "Service Request" in types
    assert "Incident" in types
