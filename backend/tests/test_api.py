import pytest


def get_auth_token(client, email, password):
    resp = client.post("/api/auth/login", json={"email": email, "password": password})
    assert resp.status_code == 200
    return resp.json()["access_token"]


def test_health_check(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "version": "1.0.0"}


def test_auth_login_success(client):
    response = client.post("/api/auth/login", json={"email": "employee@example.com", "password": "Password123!"})
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["user"]["email"] == "employee@example.com"
    assert data["user"]["role"] == "EMPLOYEE"


def test_auth_login_invalid(client):
    response = client.post("/api/auth/login", json={"email": "employee@example.com", "password": "WrongPassword"})
    assert response.status_code == 401
    assert "Invalid email or password" in response.json()["detail"]


def test_auth_me(client):
    token = get_auth_token(client, "support@example.com", "Password123!")
    response = client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "support@example.com"
    assert data["role"] == "SUPPORT"


def test_ticket_creation_and_numbering(client):
    token = get_auth_token(client, "employee@example.com", "Password123!")

    # Get a category ID
    cat_resp = client.get("/api/categories", headers={"Authorization": f"Bearer {token}"})
    assert cat_resp.status_code == 200
    cat_id = cat_resp.json()[0]["id"]

    # Create ticket
    payload = {
        "title": "Unable to connect to company VPN test incident",
        "description": "VPN connection drops every 5 minutes when connecting from home office.",
        "category_id": cat_id,
        "impact": "Individual",
        "urgency": "High",
        "device_type": "Laptop",
        "os_name": "Windows 11",
        "application_name": "GlobalProtect VPN",
    }
    create_resp = client.post("/api/tickets", json=payload, headers={"Authorization": f"Bearer {token}"})
    assert create_resp.status_code == 201
    ticket = create_resp.json()
    assert ticket["ticket_number"].startswith("INC-")
    assert ticket["title"] == payload["title"]
    assert ticket["status"] == "Open"
    assert ticket["priority"] == "Medium"  # Individual + High = Medium
    assert ticket["sla_status"] == "Within SLA"
    assert ticket["due_at"] is not None


def test_ticket_assignment_and_status_transitions(client):
    emp_token = get_auth_token(client, "employee@example.com", "Password123!")
    sup_token = get_auth_token(client, "support@example.com", "Password123!")

    cat_id = client.get("/api/categories", headers={"Authorization": f"Bearer {emp_token}"}).json()[0]["id"]

    # Create ticket as employee
    t_resp = client.post(
        "/api/tickets",
        json={
            "title": "Dell monitor flickering artifacts test",
            "description": "Horizontal green lines appear across the external screen.",
            "category_id": cat_id,
            "impact": "Individual",
            "urgency": "Low",
        },
        headers={"Authorization": f"Bearer {emp_token}"},
    )
    assert t_resp.status_code == 201
    ticket_id = t_resp.json()["id"]

    # Support assigns to self -> status should transition to In Progress
    sup_me = client.get("/api/auth/me", headers={"Authorization": f"Bearer {sup_token}"}).json()
    assign_resp = client.post(
        f"/api/tickets/{ticket_id}/assign",
        json={"assigned_to": sup_me["id"]},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert assign_resp.status_code == 200
    assert assign_resp.json()["status"] == "In Progress"
    assert assign_resp.json()["assigned_to"] == sup_me["id"]

    # Support transitions status to Waiting for User
    status_resp = client.post(
        f"/api/tickets/{ticket_id}/status",
        json={"status": "Waiting for User", "reason": "Requested video recording of flickering"},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert status_resp.status_code == 200
    assert status_resp.json()["status"] == "Waiting for User"

    # Support tries to resolve without resolution notes -> should fail with 422
    fail_res = client.post(
        f"/api/tickets/{ticket_id}/resolve",
        json={"resolution_notes": ""},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert fail_res.status_code in [400, 422]

    # Support resolves with valid notes
    res_resp = client.post(
        f"/api/tickets/{ticket_id}/resolve",
        json={"resolution_notes": "Replaced faulty DisplayPort cable. Verified monitor output at 4K 60Hz."},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert res_resp.status_code == 200
    assert res_resp.json()["status"] == "Resolved"
    assert res_resp.json()["resolved_at"] is not None

    # Employee reopens resolved ticket
    reopen_resp = client.post(
        f"/api/tickets/{ticket_id}/reopen",
        headers={"Authorization": f"Bearer {emp_token}"},
    )
    assert reopen_resp.status_code == 200
    assert reopen_resp.json()["status"] == "In Progress"


def test_troubleshooting_checklist_update(client):
    sup_token = get_auth_token(client, "support@example.com", "Password123!")
    emp_token = get_auth_token(client, "employee@example.com", "Password123!")

    cat_id = client.get("/api/categories", headers={"Authorization": f"Bearer {emp_token}"}).json()[0]["id"]
    t_resp = client.post(
        "/api/tickets",
        json={
            "title": "Printer paper jam test",
            "description": "Canon copier tray 2 is reporting sensor obstruction.",
            "category_id": cat_id,
        },
        headers={"Authorization": f"Bearer {emp_token}"},
    )
    ticket_id = t_resp.json()["id"]

    # Support saves checklist progress
    checklist_payload = '{"hw_prn_1": true, "hw_prn_2": true, "hw_prn_3": false}'
    chk_resp = client.patch(
        f"/api/tickets/{ticket_id}/checklist",
        json={"checklist_state": checklist_payload},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert chk_resp.status_code == 200
    assert chk_resp.json()["checklist_state"] == checklist_payload


def test_onboarding_lifecycle(client):
    admin_token = get_auth_token(client, "admin@example.com", "Password123!")

    # Create new onboarding request
    req_payload = {
        "employee_name": "Marcus Aurelius",
        "department": "Engineering",
        "job_title": "Staff Platform Engineer",
        "manager_name": "David Miller",
        "start_date": "2026-10-01",
        "device_requirement": "MacBook Pro 16",
        "software_requirement": "Docker, Kubernetes, AWS CLI",
    }
    resp = client.post("/api/onboarding", json=req_payload, headers={"Authorization": f"Bearer {admin_token}"})
    assert resp.status_code == 201
    data = resp.json()
    assert data["employee_name"] == "Marcus Aurelius"
    assert data["progress_percent"] == 0
    assert len(data["tasks"]) == 8

    # Mark first task completed
    first_task_id = data["tasks"][0]["id"]
    task_up_resp = client.patch(
        f"/api/onboarding/tasks/{first_task_id}",
        json={"status": "Completed", "notes": "Identity provisioned in Microsoft Entra"},
        headers={"Authorization": f"Bearer {admin_token}"},
    )
    assert task_up_resp.status_code == 200

    # Verify updated request progress
    req_check = client.get(f"/api/onboarding/{data['id']}", headers={"Authorization": f"Bearer {admin_token}"})
    assert req_check.status_code == 200
    assert req_check.json()["progress_percent"] == 12  # 1/8 * 100 = 12%


def test_access_request_approval(client):
    emp_token = get_auth_token(client, "employee@example.com", "Password123!")
    sup_token = get_auth_token(client, "support@example.com", "Password123!")

    # Employee creates access request
    create_resp = client.post(
        "/api/access-requests",
        json={
            "request_type": "Application Access",
            "requested_system": "AWS Production ReadOnly",
            "reason": "Needed for troubleshooting telemetry services.",
        },
        headers={"Authorization": f"Bearer {emp_token}"},
    )
    assert create_resp.status_code == 201
    req_id = create_resp.json()["id"]
    assert create_resp.json()["status"] == "Requested"

    # Support approves
    status_resp = client.patch(
        f"/api/access-requests/{req_id}/status",
        json={"status": "Approved"},
        headers={"Authorization": f"Bearer {sup_token}"},
    )
    assert status_resp.status_code == 200
    assert status_resp.json()["status"] == "Approved"


def test_knowledge_base_search_and_vote(client):
    emp_token = get_auth_token(client, "employee@example.com", "Password123!")

    # Search for VPN
    search_resp = client.get("/api/knowledge?search=VPN", headers={"Authorization": f"Bearer {emp_token}"})
    assert search_resp.status_code == 200
    articles = search_resp.json()
    assert len(articles) > 0
    first_article = articles[0]

    # Vote helpful
    vote_resp = client.post(
        f"/api/knowledge/{first_article['id']}/vote?helpful=true",
        headers={"Authorization": f"Bearer {emp_token}"},
    )
    assert vote_resp.status_code == 200
    assert vote_resp.json()["helpful_count"] == first_article["helpful_count"] + 1


def test_dashboard_summary_kpis(client):
    admin_token = get_auth_token(client, "admin@example.com", "Password123!")
    resp = client.get("/api/dashboard/summary", headers={"Authorization": f"Bearer {admin_token}"})
    assert resp.status_code == 200
    data = resp.json()
    assert data["total_tickets"] > 0
    assert "status_distribution" in data
    assert "priority_distribution" in data
    assert "daily_trend" in data
    assert len(data["daily_trend"]) == 14
    assert data["total_assets"] >= 25
