from __future__ import annotations

import json
import random
from datetime import datetime, date, timedelta, timezone
from typing import Any, Dict, List, Optional, Tuple
from sqlalchemy.orm import Session

from app.core.security import get_password_hash
from app.db.base import Base, get_utc_now
from app.db.database import engine
from app.services.service_request_service import record_sr_timeline
from app.db.models import (
    AccessRequest,
    AccessRequestStatus,
    Asset,
    AssetAssignment,
    AssetStatus,
    ImprovementRequest,
    ImprovementStatus,
    KnowledgeArticle,
    OnboardingRequest,
    OnboardingTask,
    OnboardingTaskStatus,
    Ticket,
    TicketAssignment,
    TicketCategory,
    TicketComment,
    TicketEscalation,
    TicketFeedback,
    TicketPriority,
    TicketStatus,
    TicketStatusHistory,
    User,
    UserRole,
    ImpactLevel,
    UrgencyLevel,
    ServiceRequest,
    ServiceRequestType,
    ServiceRequestComment,
    ServiceRequestTimeline,
    ServiceRequestStatus,
    ServiceRequestApprovalStatus,
    SupportTeam,
    ServiceRequestCategory,
)
from app.services.audit_service import log_audit
from app.services.onboarding_service import create_default_tasks_for_request
from app.services.ticket_service import (
    calculate_suggested_priority,
    get_sla_target_hours,
)


def seed_database(db: Session):
    """Populates database with comprehensive demo dataset."""
    Base.metadata.create_all(bind=engine)

    # Check if already seeded
    admin_user = db.query(User).filter(User.email == "admin@example.com").first()
    if admin_user:
        if db.query(ServiceRequestType).count() == 0:
            print("Detected existing database without Service Requests. Seeding module data...")
            employee_users = db.query(User).filter(User.role == UserRole.EMPLOYEE).all()
            support_users = db.query(User).filter(User.role == UserRole.SUPPORT).all()
            hardware_assets = db.query(Asset).all()
            seed_service_requests(db, admin_user, employee_users, support_users, hardware_assets)
            print("Service Request module seeded successfully.")
        else:
            print("Database already contains seed data.")
        return

    print("Seeding users...")
    demo_password_hash = get_password_hash("Password123!")

    # 1 Admin
    admin_user = User(
        name="David Miller",
        email="admin@example.com",
        password_hash=demo_password_hash,
        role=UserRole.ADMIN,
        department="IT Infrastructure",
        job_title="IT Director & Service Desk Lead",
        phone="+1-555-0100",
        location="Headquarters - Floor 4",
        is_active=True,
    )
    db.add(admin_user)

    # 5 Support Engineers
    engineers_data = [
        ("Arun Pandian", "support@example.com", "Tier 2 Lead Support Specialist", "+1-555-0101"),
        ("Priya Patel", "priya.patel@example.com", "Senior Systems Support Engineer", "+1-555-0102"),
        ("Marcus Vance", "marcus.vance@example.com", "Network & Security Support Analyst", "+1-555-0103"),
        ("Elena Rostova", "elena.rostova@example.com", "Workplace Technology & Apple Specialist", "+1-555-0104"),
        ("David Kim", "david.kim@example.com", "Service Desk Technician", "+1-555-0105"),
    ]
    engineers = []
    for name, email, title, phone in engineers_data:
        eng = User(
            name=name,
            email=email,
            password_hash=demo_password_hash,
            role=UserRole.SUPPORT,
            department="IT Support Services",
            job_title=title,
            phone=phone,
            location="Headquarters - Service Desk",
            is_active=True,
        )
        db.add(eng)
        engineers.append(eng)

    # 10 Employees
    employees_data = [
        ("Jordan Reed", "employee@example.com", "Finance", "Senior Financial Analyst"),
        ("Sarah Chen", "sarah.chen@example.com", "HR", "HR Operations Manager"),
        ("Michael Scott", "michael.scott@example.com", "Sales", "Regional Sales Director"),
        ("Lisa Ray", "lisa.ray@example.com", "Engineering", "Senior Software Engineer"),
        ("Robert Taylor", "robert.taylor@example.com", "Legal", "Corporate Legal Counsel"),
        ("Emily Watson", "emily.watson@example.com", "Marketing", "Product Marketing Lead"),
        ("Carlos Mendez", "carlos.mendez@example.com", "Operations", "Supply Chain Specialist"),
        ("Amanda Foster", "amanda.foster@example.com", "Customer Success", "Customer Success Lead"),
        ("James Wilson", "james.wilson@example.com", "Product", "Senior UX Designer"),
        ("Rachel Green", "rachel.green@example.com", "HR", "Lead Talent Recruiter"),
    ]
    employees = []
    for name, email, dept, title in employees_data:
        emp = User(
            name=name,
            email=email,
            password_hash=demo_password_hash,
            role=UserRole.EMPLOYEE,
            department=dept,
            job_title=title,
            phone=f"+1-555-02{random.randint(10, 99)}",
            location="Headquarters - Main Campus",
            is_active=True,
        )
        db.add(emp)
        employees.append(emp)

    db.flush()

    # -----------------------------------------------------------------------
    # 12 CATEGORIES
    # -----------------------------------------------------------------------
    print("Seeding categories...")
    categories_data = [
        ("Hardware", "Physical computing equipment, desktops, laptops, monitors, and peripherals"),
        ("Software", "Operating system errors, local application crashes, and licensing issues"),
        ("Network", "LAN, Wi-Fi connectivity, DNS, and corporate internet access"),
        ("Email", "Exchange Online, message deliverability, Outlook profiles, and distribution lists"),
        ("Access/Login", "User accounts, password resets, active directory, and single sign-on"),
        ("Microsoft 365", "Teams, OneDrive, SharePoint, Office desktop apps, and Entra ID"),
        ("Google Workspace", "Gmail, Google Drive, Google Meet, and calendar synchronization"),
        ("Mobile Device", "iOS and Android corporate email, MDM enrollment, and MFA authenticators"),
        ("Security", "Phishing reports, antivirus alerts, suspicious logins, and USB exceptions"),
        ("Printer", "Office multi-function copiers, network print queues, and paper tray issues"),
        ("VPN", "Remote access client, split tunneling, certificate errors, and MFA gateways"),
        ("Other", "General inquiries, IT hardware moves, and miscellaneous requests"),
    ]
    categories = {}
    for name, desc in categories_data:
        cat = TicketCategory(name=name, description=desc, is_active=True)
        db.add(cat)
        categories[name] = cat
    db.flush()

    # -----------------------------------------------------------------------
    # 25 ASSETS
    # -----------------------------------------------------------------------
    print("Seeding hardware inventory...")
    assets_data = [
        ("AST-1001", "Laptop", "Dell", "Latitude 5540", "SN-DL5540-001", employees[0].id),
        ("AST-1002", "Laptop", "Lenovo", "ThinkPad T14s Gen 4", "SN-LNPT14-002", employees[1].id),
        ("AST-1003", "Laptop", "Apple", "MacBook Pro 16-inch M3", "SN-APMBP16-003", employees[2].id),
        ("AST-1004", "Laptop", "Apple", "MacBook Air 15-inch M2", "SN-APMBA15-004", employees[3].id),
        ("AST-1005", "Desktop", "Dell", "OptiPlex 7010 Micro", "SN-DLOP70-005", employees[4].id),
        ("AST-1006", "Laptop", "Dell", "Precision 5680 Workstation", "SN-DLPR56-006", employees[5].id),
        ("AST-1007", "Laptop", "Lenovo", "ThinkPad X1 Carbon Gen 11", "SN-LNX1C-007", employees[6].id),
        ("AST-1008", "Laptop", "Dell", "Latitude 5440", "SN-DL5440-008", employees[7].id),
        ("AST-1009", "Laptop", "Apple", "MacBook Pro 14-inch M3 Pro", "SN-APMBP14-009", employees[8].id),
        ("AST-1010", "Laptop", "Dell", "Latitude 5540", "SN-DL5540-010", employees[9].id),
        ("AST-1011", "Monitor", "Dell", "UltraSharp U2723QE 27-inch 4K", "SN-DLMON27-011", employees[0].id),
        ("AST-1012", "Monitor", "Dell", "UltraSharp U2723QE 27-inch 4K", "SN-DLMON27-012", employees[1].id),
        ("AST-1013", "Monitor", "HP", "E27m G4 QHD Conferencing", "SN-HPE27-013", employees[3].id),
        ("AST-1014", "Mobile Phone", "Apple", "iPhone 15 128GB", "SN-APIP15-014", employees[2].id),
        ("AST-1015", "Mobile Phone", "Samsung", "Galaxy S24 Enterprise", "SN-SMGS24-015", employees[6].id),
        ("AST-1016", "Printer", "HP", "LaserJet Enterprise M507x", "SN-HPLJ507-016", None),
        ("AST-1017", "Printer", "Canon", "imageRUNNER ADVANCE DX C3830i", "SN-CNIR38-017", None),
        ("AST-1018", "Docking Station", "Dell", "Thunderbolt Dock WD22TB4", "SN-DLWD22-018", employees[0].id),
        ("AST-1019", "Docking Station", "CalDigit", "TS4 Thunderbolt 4 Dock", "SN-CDTS4-019", employees[8].id),
        ("AST-1020", "Laptop", "Dell", "Latitude 5540 (Stock)", "SN-DLSTK-020", None),
        ("AST-1021", "Laptop", "Lenovo", "ThinkPad T14 Gen 4 (Stock)", "SN-LNSTK-021", None),
        ("AST-1022", "Laptop", "Apple", "MacBook Air M2 (Stock)", "SN-APSTK-022", None),
        ("AST-1023", "Monitor", "Dell", "P2422H 24-inch FHD (Stock)", "SN-DLMON24-023", None),
        ("AST-1024", "Network Device", "Cisco", "Catalyst 9300 48-Port Switch", "SN-CS9300-024", None),
        ("AST-1025", "Tablet", "Apple", "iPad Air 5th Gen Wi-Fi", "SN-APIPA5-025", employees[7].id),
    ]
    assets_map = {}
    for tag, dtype, mfg, model, sn, uid in assets_data:
        status_val = AssetStatus.IN_STOCK if uid is None else AssetStatus.IN_USE
        asset = Asset(
            asset_tag=tag,
            device_type=dtype,
            manufacturer=mfg,
            model=model,
            serial_number=sn,
            status=status_val,
            assigned_user_id=uid,
            purchase_date=date.today() - timedelta(days=random.randint(60, 500)),
            warranty_expiry=date.today() + timedelta(days=random.randint(100, 800)),
            notes=f"Corporate managed {dtype}",
        )
        db.add(asset)
        assets_map[tag] = asset
    db.flush()

    # -----------------------------------------------------------------------
    # 100+ REALISTIC TICKETS
    # -----------------------------------------------------------------------
    print("Seeding 100+ realistic IT incidents...")
    now = datetime.now(timezone.utc)

    ticket_scenarios = [
        # VPN & Network
        {
            "title": "Unable to connect to company VPN after credential change",
            "desc": "GlobalProtect VPN connection fails with error 'Authentication failed: invalid client certificate or user credentials' after resetting domain password this morning.",
            "cat": "VPN", "device": "Laptop", "os": "Windows 11", "app": "GlobalProtect VPN",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        {
            "title": "Wi-Fi connectivity drops intermittently in Conference Room 4B",
            "desc": "Multiple attendees in executive meeting experiencing frequent Wi-Fi packet loss and Zoom drops on SSID 'Corporate-Secure'.",
            "cat": "Network", "device": "Network Device", "os": "All", "app": "Cisco Wireless",
            "impact": ImpactLevel.TEAM, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        {
            "title": "Shared network drive P:\\Finance shows 'Network path not found'",
            "desc": "Entire finance operations team unable to access month-end close spreadsheets stored on DFS share \\\\corp.local\\shares\\Finance.",
            "cat": "Access/Login", "device": "Desktop", "os": "Windows 11", "app": "Windows Explorer",
            "impact": ImpactLevel.DEPARTMENT, "urgency": UrgencyLevel.CRITICAL, "priority": TicketPriority.HIGH,
        },
        # Hardware
        {
            "title": "Dell Latitude laptop fails to power on after weekend",
            "desc": "Laptop does not respond when pressing power button. AC adapter light turns off when plugged into charging port. Docking station connection also unresponsive.",
            "cat": "Hardware", "device": "Laptop", "os": "Windows 11", "app": "Hardware Chassis",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.CRITICAL, "priority": TicketPriority.HIGH,
        },
        {
            "title": "External Dell 4K monitor displaying flickering green lines",
            "desc": "Secondary monitor connected via USB-C DisplayPort cable flickers with horizontal colored artifacts when waking from sleep.",
            "cat": "Hardware", "device": "Monitor", "os": "Windows 11", "app": "Display Driver",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.MEDIUM, "priority": TicketPriority.MEDIUM,
        },
        {
            "title": "3rd Floor Marketing Canon printer paper jam error 13.00.00",
            "desc": "Main department printer halted with tray 2 paper sensor error and red warning beacon. Several customer proposals queued.",
            "cat": "Printer", "device": "Printer", "os": "Windows 11", "app": "Print Spooler",
            "impact": ImpactLevel.DEPARTMENT, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        # Microsoft 365
        {
            "title": "Outlook desktop client stuck in 'Disconnected' status",
            "desc": "Microsoft 365 Outlook client does not send or receive emails. Status bar shows 'Trying to connect...' and 'Need Password' popup appears but closes immediately.",
            "cat": "Microsoft 365", "device": "Laptop", "os": "Windows 11", "app": "Outlook 365",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.MEDIUM,
        },
        {
            "title": "Microsoft Teams microphone input distorted and robotic",
            "desc": "During internal standup calls, colleagues report that my voice sounds metallic and cut off every 2 seconds. Audio test call confirms high packet latency.",
            "cat": "Microsoft 365", "device": "Laptop", "os": "Windows 11", "app": "Microsoft Teams",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.MEDIUM, "priority": TicketPriority.MEDIUM,
        },
        {
            "title": "OneDrive for Business synchronization stuck processing 4,821 files",
            "desc": "OneDrive client icon shows constant blue sync arrows for over 48 hours. CPU utilization spikes to 95% on OneDrive.exe process.",
            "cat": "Microsoft 365", "device": "Laptop", "os": "Windows 11", "app": "OneDrive Sync",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.MEDIUM, "priority": TicketPriority.MEDIUM,
        },
        # Google Workspace
        {
            "title": "Gmail not receiving external inbound customer invoices",
            "desc": "Vendor reports bounce-back error '550 5.7.26 DKIM/SPF check failed' when sending PDF invoices to finance@company.com.",
            "cat": "Google Workspace", "device": "Laptop", "os": "macOS", "app": "Gmail",
            "impact": ImpactLevel.DEPARTMENT, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        {
            "title": "Google Drive shared folder permissions revoked unexpectedly",
            "desc": "Contractors unable to upload product design files into 'Q3 Marketing Assets' Google Drive folder. Shows 'Access Denied'.",
            "cat": "Google Workspace", "device": "Laptop", "os": "macOS", "app": "Google Drive",
            "impact": ImpactLevel.TEAM, "urgency": UrgencyLevel.MEDIUM, "priority": TicketPriority.MEDIUM,
        },
        # Mobile & Apple macOS
        {
            "title": "Microsoft Authenticator MFA push notifications not arriving on iPhone",
            "desc": "Unable to log into Microsoft 365 or Workday because number matching prompts never show up on iOS device. Have to rely on SMS fallback.",
            "cat": "Mobile Device", "device": "Mobile Phone", "os": "iOS", "app": "Microsoft Authenticator",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        {
            "title": "MacBook Pro kernel panic during Jamf Pro compliance check",
            "desc": "macOS Sonoma rebooted with pink screen crash log referencing com.apple.driver.AppleMobileFileIntegrity while running background security audit.",
            "cat": "Software", "device": "Laptop", "os": "macOS", "app": "Jamf Pro Agent",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.HIGH, "priority": TicketPriority.HIGH,
        },
        # Security & Company-wide
        {
            "title": "Urgent: Suspicious phishing email targeting executive staff",
            "desc": "Multiple executives received email pretending to be HR Director asking to click on 'Updated Compensation Form 2026.docx.exe'.",
            "cat": "Security", "device": "Laptop", "os": "Windows 11", "app": "Exchange Online Protection",
            "impact": ImpactLevel.COMPANY_WIDE, "urgency": UrgencyLevel.CRITICAL, "priority": TicketPriority.CRITICAL,
        },
        {
            "title": "Account locked after exceeding maximum failed login attempts",
            "desc": "Entered old password multiple times after smartcard expired. Windows login screen states 'The referenced account is currently locked out.'",
            "cat": "Access/Login", "device": "Desktop", "os": "Windows 11", "app": "Active Directory",
            "impact": ImpactLevel.INDIVIDUAL, "urgency": UrgencyLevel.CRITICAL, "priority": TicketPriority.HIGH,
        },
    ]

    all_statuses = [
        TicketStatus.OPEN,
        TicketStatus.IN_PROGRESS,
        TicketStatus.WAITING_FOR_USER,
        TicketStatus.WAITING_FOR_VENDOR,
        TicketStatus.ESCALATED,
        TicketStatus.RESOLVED,
        TicketStatus.CLOSED,
    ]

    created_tickets_count = 0
    # Generate 105 tickets
    for i in range(1, 106):
        scenario = random.choice(ticket_scenarios)
        emp = random.choice(employees)
        eng = random.choice(engineers)
        cat = categories.get(scenario["cat"], categories["Software"])

        # Decide status realistically
        if i <= 15:
            status_val = TicketStatus.OPEN
        elif i <= 35:
            status_val = TicketStatus.IN_PROGRESS
        elif i <= 45:
            status_val = TicketStatus.WAITING_FOR_USER
        elif i <= 52:
            status_val = TicketStatus.WAITING_FOR_VENDOR
        elif i <= 60:
            status_val = TicketStatus.ESCALATED
        elif i <= 85:
            status_val = TicketStatus.RESOLVED
        else:
            status_val = TicketStatus.CLOSED

        # Creation timestamp spread over past 14 days
        days_ago = random.randint(0, 14)
        hours_ago = random.randint(1, 23)
        created_time = now - timedelta(days=days_ago, hours=hours_ago)

        priority_val = scenario["priority"]
        target_hours = get_sla_target_hours(priority_val)
        due_at = created_time + timedelta(hours=target_hours)

        # First response time
        first_resp = None
        if status_val != TicketStatus.OPEN:
            first_resp = created_time + timedelta(minutes=random.randint(10, 120))

        # Resolution time
        resolved_time = None
        res_notes = None
        if status_val in [TicketStatus.RESOLVED, TicketStatus.CLOSED]:
            # Introduce a few realistic SLA breaches (e.g. 15% breach rate)
            if random.random() < 0.15:
                # Breached
                resolved_time = due_at + timedelta(hours=random.randint(2, 24))
            else:
                # Within SLA
                resolved_time = created_time + timedelta(hours=min(target_hours * 0.8, random.randint(1, target_hours)))
            res_notes = f"Verified root cause. Completed troubleshooting checklist and confirmed resolution with {emp.name}. Service restored successfully."

        closed_time = resolved_time + timedelta(hours=random.randint(2, 24)) if status_val == TicketStatus.CLOSED and resolved_time else None

        # Sample checklist state
        checklist_data = {
            "step_1": True,
            "step_2": True,
            "step_3": status_val in [TicketStatus.RESOLVED, TicketStatus.CLOSED],
            "step_4": status_val in [TicketStatus.RESOLVED, TicketStatus.CLOSED],
        }

        ticket_number = f"INC-{now.year}-{i:06d}"
        ticket = Ticket(
            ticket_number=ticket_number,
            title=f"{scenario['title']} ({emp.department})" if i > 15 else scenario["title"],
            description=scenario["desc"],
            category_id=cat.id,
            priority=priority_val,
            status=status_val,
            impact=scenario["impact"],
            urgency=scenario["urgency"],
            asset_id=assets_map.get(f"AST-{1001 + (i % 25)}", None).id if (i % 25) < 25 else None,
            device_type=scenario["device"],
            os_name=scenario["os"],
            application_name=scenario["app"],
            location=emp.location,
            created_by=emp.id,
            assigned_to=eng.id if status_val != TicketStatus.OPEN else None,
            due_at=due_at,
            first_response_at=first_resp,
            resolved_at=resolved_time,
            closed_at=closed_time,
            resolution_notes=res_notes,
            is_escalated=(status_val == TicketStatus.ESCALATED),
            escalation_level=2 if status_val == TicketStatus.ESCALATED else 1,
            checklist_state=json.dumps(checklist_data),
            created_at=created_time,
            updated_at=resolved_time or first_resp or created_time,
        )
        db.add(ticket)
        db.flush()

        # Add initial status history
        hist_1 = TicketStatusHistory(
            ticket_id=ticket.id,
            old_status="None",
            new_status=TicketStatus.OPEN,
            changed_by=emp.id,
            changed_at=created_time,
            reason="Ticket reported by employee.",
        )
        db.add(hist_1)

        if status_val != TicketStatus.OPEN:
            hist_2 = TicketStatusHistory(
                ticket_id=ticket.id,
                old_status=TicketStatus.OPEN,
                new_status=status_val,
                changed_by=eng.id,
                changed_at=first_resp or (created_time + timedelta(minutes=30)),
                reason=f"Engineer {eng.name} assigned and transitioned to {status_val}.",
            )
            db.add(hist_2)

            # Assignment record
            assign_rec = TicketAssignment(
                ticket_id=ticket.id,
                assigned_to=eng.id,
                assigned_by=eng.id,
                assigned_at=first_resp or (created_time + timedelta(minutes=30)),
            )
            db.add(assign_rec)

            # Public and internal comments
            comm_1 = TicketComment(
                ticket_id=ticket.id,
                user_id=eng.id,
                comment=f"Hello {emp.name}, I am actively investigating this issue. I have initiated diagnostic checks.",
                is_internal=False,
                created_at=created_time + timedelta(minutes=45),
            )
            comm_2 = TicketComment(
                ticket_id=ticket.id,
                user_id=eng.id,
                comment="Internal note: Checked server logs and verified Intune compliance status. No policy conflicts detected.",
                is_internal=True,
                created_at=created_time + timedelta(minutes=60),
            )
            db.add(comm_1)
            db.add(comm_2)

        # CSAT feedback for resolved/closed
        if status_val in [TicketStatus.RESOLVED, TicketStatus.CLOSED] and random.random() < 0.7:
            fb = TicketFeedback(
                ticket_id=ticket.id,
                user_id=emp.id,
                rating=random.choice([4, 5, 5, 5, 4, 3]),
                comments=random.choice([
                    "Fast resolution! The engineer was very professional.",
                    "Quick response and solved the problem in 20 minutes.",
                    "Great support, appreciated the clear instructions.",
                    "Problem solved. Thank you IT team!",
                ]),
                created_at=resolved_time + timedelta(minutes=30),
            )
            db.add(fb)

        created_tickets_count += 1

    db.flush()

    # -----------------------------------------------------------------------
    # 8 IT ONBOARDING REQUESTS
    # -----------------------------------------------------------------------
    print("Seeding IT onboarding workflows...")
    onboarding_data = [
        ("Alexander Wright", "Engineering", "Backend Software Engineer", "Lisa Ray", 10),
        ("Chloe Bennett", "Finance", "Senior Accounting Specialist", "Jordan Reed", 5),
        ("Daniel Morales", "Sales", "Enterprise Account Executive", "Michael Scott", 14),
        ("Hannah Schmidt", "HR", "People Experience Specialist", "Sarah Chen", 3),
        ("Lucas Silva", "Product", "Product Manager - Analytics", "James Wilson", 21),
        ("Maya Lin", "Marketing", "Content Strategist", "Emily Watson", -2),  # Completed recently
        ("Liam O'Connor", "Operations", "Logistics Coordinator", "Carlos Mendez", -10),  # Completed
        ("Sophia Rossi", "Legal", "Compliance Associate", "Robert Taylor", 7),
    ]

    for name, dept, title, mgr, start_offset in onboarding_data:
        start_d = date.today() + timedelta(days=start_offset)
        is_done = start_offset < 0
        req = OnboardingRequest(
            employee_name=name,
            department=dept,
            job_title=title,
            manager_name=mgr,
            start_date=start_d,
            location="Headquarters",
            device_requirement="MacBook Pro 16" if "Engineering" in dept or "Product" in dept else "Dell Latitude 5540",
            software_requirement="M365, Slack, GitHub Enterprise, JetBrains, Docker",
            access_requirement="AWS Dev/Staging, Jira, Google Workspace, GitHub Org",
            status="Completed" if is_done else "In Progress",
            progress_percent=100 if is_done else random.choice([25, 50, 75]),
            created_by=admin_user.id,
        )
        db.add(req)
        db.flush()

        tasks = create_default_tasks_for_request(db, req)
        if is_done:
            for t in tasks:
                t.status = OnboardingTaskStatus.COMPLETED
                t.completed_at = now - timedelta(days=abs(start_offset))
        else:
            # Mark some tasks completed based on progress
            completed_num = req.progress_percent // 25
            for idx, t in enumerate(tasks):
                if idx < completed_num * 2:
                    t.status = OnboardingTaskStatus.COMPLETED
                    t.completed_at = now - timedelta(days=1)
                elif idx == completed_num * 2:
                    t.status = OnboardingTaskStatus.IN_PROGRESS
    db.flush()

    # -----------------------------------------------------------------------
    # 10 ACCESS REQUESTS
    # -----------------------------------------------------------------------
    print("Seeding access & account requests...")
    access_data = [
        ("Password Reset", "Active Directory / Entra ID", "User Account", "Forgotten smartcard PIN", employees[0].id, AccessRequestStatus.COMPLETED),
        ("MFA Reset", "Microsoft Authenticator", "Security Gateway", "Replaced iPhone with new device", employees[1].id, AccessRequestStatus.COMPLETED),
        ("Shared Drive Access", "DFS Share \\\\corp\\Legal", "Read/Write", "Working on upcoming acquisition contract", employees[4].id, AccessRequestStatus.APPROVED),
        ("VPN Access", "GlobalProtect Production Gateway", "Network Access", "Remote work rotation approval", employees[3].id, AccessRequestStatus.APPROVED),
        ("Application Access", "Salesforce Enterprise CRM", "Sales User", "Transferred from marketing to sales", employees[5].id, AccessRequestStatus.PENDING_APPROVAL),
        ("Application Access", "Jira Software Cloud", "Developer", "New sprint feature assignment", employees[2].id, AccessRequestStatus.REQUESTED),
        ("Shared Drive Access", "\\\\corp\\Marketing\\Campaigns", "Read/Write", "Launching Q4 brand refresh assets", employees[5].id, AccessRequestStatus.COMPLETED),
        ("MFA Reset", "Duo Security Push", "Security Token", "Lost phone during business travel", employees[6].id, AccessRequestStatus.PENDING_APPROVAL),
        ("Password Reset", "SAP ERP System", "Finance Access", "Account locked due to expiration", employees[0].id, AccessRequestStatus.COMPLETED),
        ("Application Access", "Workday HCM Admin", "HR Staff", "Annual benefits open enrollment support", employees[1].id, AccessRequestStatus.REQUESTED),
    ]

    for rtype, sys_name, alevel, rsn, uid, stat in access_data:
        ar = AccessRequest(
            request_type=rtype,
            requested_system=sys_name,
            access_level=alevel,
            reason=rsn,
            requested_by=uid,
            approved_by=admin_user.id if stat in [AccessRequestStatus.APPROVED, AccessRequestStatus.COMPLETED] else None,
            assigned_engineer_id=engineers[0].id if stat == AccessRequestStatus.COMPLETED else None,
            status=stat,
            completed_at=now - timedelta(days=random.randint(1, 5)) if stat == AccessRequestStatus.COMPLETED else None,
        )
        db.add(ar)
    db.flush()

    # -----------------------------------------------------------------------
    # 12 KNOWLEDGE BASE ARTICLES (OS & GUIDANCE)
    # -----------------------------------------------------------------------
    print("Seeding Knowledge Base & OS Support Guides...")
    kb_articles = [
        (
            "How to Connect to Corporate GlobalProtect VPN",
            "Network",
            "All",
            "### Overview\nAll remote employees must connect to the corporate VPN when working offsite to securely access internal resources.\n\n### Step-by-Step Instructions\n1. Open **GlobalProtect** from your system tray or menu bar.\n2. In the Portal address field, enter: `vpn.company.com`.\n3. Click **Connect**.\n4. Complete the Microsoft Entra ID single sign-on prompt.\n5. Approve the **Microsoft Authenticator number matching** push notification on your enrolled mobile device.\n6. The shield icon will turn green when connected.",
            "vpn, remote access, globalprotect, network",
            142, 38, 2,
        ),
        (
            "How to Reset Your Microsoft 365 Password & MFA",
            "Access/Login",
            "All",
            "### Self-Service Password Reset (SSPR)\nIf you know your current password or have your MFA device:\n1. Browse to https://passwordreset.microsoftonline.com\n2. Enter your corporate email address (`user@company.com`).\n3. Verify your identity using your secondary phone or Microsoft Authenticator app.\n4. Choose a new secure password meeting corporate complexity requirements (minimum 14 characters, uppercase, lowercase, numbers, and symbols).\n\n### If Locked Out\nIf you cannot access your Authenticator app, submit an Access Request ticket or contact the IT Service Desk directly.",
            "password, reset, mfa, entra, login",
            210, 65, 4,
        ),
        (
            "Fixing Outlook 365 Desktop Synchronization Issues",
            "Microsoft 365",
            "Windows",
            "### Symptoms\nOutlook shows 'Disconnected', emails remain in the Outbox, or new messages do not appear.\n\n### Quick Troubleshooting Steps\n1. **Check Webmail First**: Log in to [outlook.office.com](https://outlook.office.com) to confirm your mailbox is receiving mail and no service outage exists.\n2. **Clear Windows Credential Manager**:\n   - Close Outlook.\n   - Open Start Menu > Credential Manager > Windows Credentials.\n   - Remove any entries starting with `MicrosoftOffice16_Data`.\n   - Re-open Outlook and sign in.\n3. **Run Outlook in Safe Mode**:\n   - Press `Win + R`, type `outlook.exe /safe`, and press Enter.\n   - If it works, disable conflicting third-party add-ins under File > Options > Add-ins.\n4. **Rebuild Outlook Profile**: Contact IT to generate a clean mail profile if database corruption persists.",
            "outlook, m365, email, sync, disconnected",
            185, 49, 3,
        ),
        (
            "Configuring Corporate Wi-Fi on Windows 11 & macOS",
            "Network",
            "All",
            "### Connecting to 'Corporate-Secure'\n1. Select the **Corporate-Secure** wireless network.\n2. Choose **WPA3-Enterprise (802.1X)** security.\n3. Enter your corporate email address as the username and your domain password.\n4. If prompted to trust the root security certificate issued by `Corp-CA-Root`, click **Accept / Trust**.\n\n### Note for Visitors\nVisitors should connect to `Corporate-Guest` and request a sponsored voucher code at reception.",
            "wifi, wireless, network, 802.1x",
            98, 24, 1,
        ),
        (
            "macOS Ventura & Sonoma: Granting Screen Recording & Accessibility Permissions",
            "Software",
            "macOS",
            "### Overview\nDue to macOS Privacy & Security enhancements, meeting apps (Zoom, Teams, Slack) require explicit permission to share your screen or transmit audio.\n\n### Instructions\n1. Click the Apple logo () > **System Settings**.\n2. Navigate to **Privacy & Security**.\n3. Click on **Screen & System Audio Recording**.\n4. Toggle the switch ON for **Microsoft Teams**, **Zoom**, and **Slack**.\n5. Enter your Mac administrator credentials if prompted.\n6. Select **Quit & Reopen** for changes to take effect.",
            "macos, mac, screen recording, permissions, teams, zoom",
            115, 33, 1,
        ),
        (
            "Enrolling Your iOS Device in Microsoft Intune Company Portal",
            "Mobile Device",
            "iOS",
            "### Instructions for Corporate iPhone / iPad\n1. Download **Microsoft Intune Company Portal** from the Apple App Store.\n2. Sign in with your corporate email and password.\n3. Follow the on-screen prompts to download the Mobile Device Management (MDM) profile.\n4. Open iOS **Settings** > **Profile Downloaded** > Tap **Install**.\n5. Confirm device passcode.\n6. Return to the Company Portal app and tap **Check Compliance**.\n7. Your corporate email and managed apps will automatically configure within 5 minutes.",
            "ios, iphone, intune, company portal, mdm, mobile",
            89, 21, 2,
        ),
        (
            "Setting Up Google Workspace & Drive for Desktop on macOS",
            "Google Workspace",
            "macOS",
            "### Google Drive for Desktop Installation\n1. Download Google Drive desktop client from Google Workspace admin center or Managed Software Center.\n2. Open Google Drive installer and follow prompts.\n3. Click **Sign in with browser** and authorize with your corporate Google identity.\n4. Choose **Stream files** to save local Mac disk space while accessing all Shared Drives.\n5. Google Drive will mount under `/Volumes/GoogleDrive` in Finder.",
            "google, workspace, drive, macos, gdrive",
            72, 18, 0,
        ),
        (
            "Troubleshooting Laptop Docking Station & External Display Failures",
            "Hardware",
            "Windows",
            "### Steps to Restore External Displays on Thunderbolt Docks\n1. Disconnect the USB-C / Thunderbolt cable connecting your dock to the laptop.\n2. Disconnect the AC power brick cable from the docking station for 15 seconds.\n3. Re-plug power into the dock; verify the power indicator LED illuminates.\n4. Reconnect the Thunderbolt cable to your laptop's primary charging port.\n5. Press `Win + P` and select **Extend**.\n6. If displays do not detect, update Dell Command | Update or Lenovo Vantage firmware.",
            "hardware, dock, thunderbolt, display, monitor, dell",
            160, 42, 3,
        ),
        (
            "Android Enterprise Work Profile Setup & MFA",
            "Mobile Device",
            "Android",
            "### Setting Up Managed Work Profile\n1. Install **Intune Company Portal** from the Google Play Store.\n2. Sign in with your company email credentials.\n3. Tap **Accept** to create a segregated Android Work Profile.\n4. Your work apps (Outlook, Teams, Authenticator) will appear inside a separate work tab with briefcase icons.\n5. Personal data remains completely isolated and private.",
            "android, mobile, work profile, mdm, intune",
            54, 15, 1,
        ),
        (
            "How to Request New Software or Hardware Upgrades",
            "Other",
            "All",
            "### Software Licensing & Hardware Procurement Policy\n1. Standard corporate software (M365, Slack, Zoom, Chrome) is pre-installed on all corporate images.\n2. Non-standard software (JetBrains, Adobe Creative Cloud, Figma Professional, AWS IAM) requires manager approval.\n3. Submit an Access Request ticket specifying business justification and cost center.\n4. Hardware replacement cycles occur every 36 months for laptops.",
            "procurement, software, request, license, upgrade",
            95, 27, 2,
        ),
        (
            "Printer Offline / Paper Tray Sensor Reset Guide",
            "Printer",
            "All",
            "### Clearing Network Printer Faults\n1. Verify the printer display panel is awake and not in deep sleep mode.\n2. Open Tray 2 and Tray 3; inspect for bent paper reams or foreign objects.\n3. Reseat the paper drawer firmly until a click sounds.\n4. On your Windows PC, go to Settings > Bluetooth & Devices > Printers & Scanners.\n5. Select the printer, open print queue, and click **Printer > Use Printer Online**.\n6. If stuck in spooler, restart `spooler.exe` service or contact IT.",
            "printer, paper jam, offline, spooler, hp, canon",
            67, 19, 2,
        ),
        (
            "Reporting Phishing Emails & Security Incidents",
            "Security",
            "All",
            "### What to Do if You Receive a Suspicious Email\n1. **Do not click links** or open attachments.\n2. In Outlook, click the **Report Message** ribbon icon and choose **Phishing**.\n3. The message will be forwarded automatically to the IT Security Operations Center (SOC) for sandboxed analysis.\n4. If you already entered credentials on an external form, immediately disconnect your network cable/Wi-Fi and call the Emergency IT line at ext. 9111.",
            "security, phishing, email, suspicious, soc",
            135, 41, 0,
        ),
    ]

    for title, cat, ostarget, body, tags, views, helpful, not_helpful in kb_articles:
        ka = KnowledgeArticle(
            title=title,
            category=cat,
            os_target=ostarget,
            content=body,
            tags=tags,
            view_count=views,
            helpful_count=helpful,
            not_helpful_count=not_helpful,
            created_by=admin_user.id,
        )
        db.add(ka)
    db.flush()

    # -----------------------------------------------------------------------
    # 5 CONTINUOUS PROCESS IMPROVEMENT INITIATIVES
    # -----------------------------------------------------------------------
    print("Seeding continuous improvement initiatives...")
    improvements_data = [
        (
            "Automated Self-Service MFA Device Registration Portal",
            "Technology",
            "Deploy Entra ID temporary access passes via automated SMS verification to reduce tier 1 phone support volume for lost phones.",
            "Reduces password and MFA related ticket volume by an estimated 40% and saves 15 hours of technician time weekly.",
            ImprovementStatus.APPROVED,
            TicketPriority.HIGH,
            engineers[0].id,
            engineers[1].id,
        ),
        (
            "Zero-Touch Automated Laptop Provisioning via Windows Autopilot",
            "Process",
            "Transition new employee imaging from manual USB deployment to Windows Autopilot and Intune cloud provisioning shipped directly from Dell.",
            "Reduces new hire IT setup time from 4 hours per machine to 15 minutes, accelerating Day 1 readiness.",
            ImprovementStatus.IN_PROGRESS,
            TicketPriority.HIGH,
            admin_user.id,
            engineers[0].id,
        ),
        (
            "Enterprise Knowledge Base Integration in Slack / Teams Bot",
            "Technology",
            "Implement an interactive helpdesk bot in Teams that matches user questions to verified KB articles before ticket creation.",
            "Encourages self-service ticket deflection by 25% for routine inquiries like Wi-Fi and VPN setup.",
            ImprovementStatus.UNDER_REVIEW,
            TicketPriority.MEDIUM,
            engineers[3].id,
            admin_user.id,
        ),
        (
            "Standardized Hardware Peripheral Bundles in Office Stockrooms",
            "Process",
            "Pre-package monitors, thunderbolt cables, and ergonomic keyboards into quick-dispense kits with barcode tracking.",
            "Eliminates hardware deployment bottlenecks during peak hiring months.",
            ImprovementStatus.IMPLEMENTED,
            TicketPriority.MEDIUM,
            engineers[4].id,
            engineers[4].id,
        ),
        (
            "Quarterly Customer Satisfaction (CSAT) Review & Feedback Loop",
            "Process",
            "Institute a monthly service review where support leads analyze all tickets rated below 3 stars to identify training gaps.",
            "Drives continuous service improvement and enhances stakeholder trust.",
            ImprovementStatus.IMPLEMENTED,
            TicketPriority.MEDIUM,
            admin_user.id,
            admin_user.id,
        ),
    ]

    for title, cat, desc, ben, stat, pri, sub_id, assign_id in improvements_data:
        imp = ImprovementRequest(
            title=title,
            category=cat,
            description=desc,
            business_benefit=ben,
            status=stat,
            priority=pri,
            submitted_by=sub_id,
            assigned_to=assign_id,
        )
        db.add(imp)
    db.flush()

    # ---------------------------------------------------------------------------
    # 7. SERVICE REQUEST MANAGEMENT MODULE (15 TYPES & 52 SERVICE REQUESTS)
    # ---------------------------------------------------------------------------
    seed_service_requests(db, admin_user, employees, engineers, list(assets_map.values()))

    db.commit()
    print("Database seeding completed successfully! Total 100+ tickets, 16 users, 25 assets, 15 request types, 52 service requests, 8 onboardings.")


def seed_service_requests(
    db: Session,
    admin_user: User,
    employee_users: List[User],
    support_users: List[User],
    hardware_assets: List[Asset],
):
    """Populates 15 standard Service Request catalog types and 52 realistic service requests."""
    print("Seeding Service Request Management module (Catalog & 52 requests)...")

    # A. 15 Standard Service Request Catalog Types
    sr_types_definitions = [
        ("New Laptop", "Request a new or replacement corporate laptop for standard or development workflows.", ServiceRequestCategory.HARDWARE, TicketPriority.HIGH, True, 48),
        ("Software Installation", "Request installation and licensing for approved corporate desktop or cloud software.", ServiceRequestCategory.SOFTWARE, TicketPriority.MEDIUM, True, 24),
        ("Application Access", "Request role-based access or permissions to an enterprise business application.", ServiceRequestCategory.ACCESS, TicketPriority.MEDIUM, True, 24),
        ("Microsoft 365 License", "Provision or upgrade Microsoft 365 licenses (E3, E5, Copilot, Visio, Project).", ServiceRequestCategory.MICROSOFT_365, TicketPriority.HIGH, True, 24),
        ("Email / Outlook Access", "Configure corporate email routing, delegates, or Outlook desktop profile.", ServiceRequestCategory.MICROSOFT_365, TicketPriority.MEDIUM, False, 12),
        ("VPN Access", "Request secure remote VPN credentials and multi-factor client profiles.", ServiceRequestCategory.NETWORK, TicketPriority.HIGH, True, 24),
        ("Shared Folder Access", "Request access permissions to departmental network drives or SharePoint document libraries.", ServiceRequestCategory.ACCESS, TicketPriority.MEDIUM, True, 24),
        ("New Employee Account", "Provision corporate Active Directory credentials, email, and baseline access for a new hire.", ServiceRequestCategory.ACCOUNT, TicketPriority.CRITICAL, False, 12),
        ("Password Reset", "Self-service or assisted domain password reset and MFA re-synchronization.", ServiceRequestCategory.ACCOUNT, TicketPriority.HIGH, False, 4),
        ("Hardware Replacement", "Request replacement or upgrade for defective monitors, docking stations, or peripherals.", ServiceRequestCategory.HARDWARE, TicketPriority.HIGH, True, 72),
        ("Mobile Device Request", "Request corporate smartphone enrollment or cellular data plan provisioning.", ServiceRequestCategory.MOBILE, TicketPriority.MEDIUM, True, 48),
        ("Google Workspace Access", "Provision Google Workspace Enterprise account, shared drive, or Google Meet licenses.", ServiceRequestCategory.GOOGLE_WORKSPACE, TicketPriority.MEDIUM, True, 24),
        ("Printer Access", "Map network office printers, secure badge release, or print queue driver configuration.", ServiceRequestCategory.HARDWARE, TicketPriority.LOW, False, 12),
        ("Shared Mailbox Access", "Request send-as or read permissions to a departmental shared mailbox.", ServiceRequestCategory.MICROSOFT_365, TicketPriority.LOW, False, 16),
        ("General IT Assistance", "Request non-incident technical consultation, standard moves, or equipment advice.", ServiceRequestCategory.GENERAL_IT, TicketPriority.LOW, False, 24),
    ]

    sr_types_map: Dict[str, ServiceRequestType] = {}
    for name, desc, cat, pri, app_req, sla_hrs in sr_types_definitions:
        st = ServiceRequestType(
            name=name,
            description=desc,
            category=cat,
            default_priority=pri,
            approval_required=app_req,
            default_sla_hours=sla_hrs,
            is_active=True,
        )
        db.add(st)
        db.flush()
        sr_types_map[name] = st

    # B. 52 Realistic Service Requests
    raw_sr_specs = [
        # Software Installation (10)
        ("Install Docker Desktop for Container Development", "Software Installation", "Software", TicketPriority.MEDIUM, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Required for local Kubernetes microservice testing and Docker containerization.", "Docker Desktop", "4.28.0", None, 12, True),
        ("Install Slack Desktop Client with Enterprise SSO", "Software Installation", "Software", TicketPriority.LOW, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.SERVICE_DESK, "Official collaboration tool for new team cross-functional sync.", "Slack", "4.36.140", None, 14, True),
        ("Install Visual Studio Code & Python Extensions", "Software Installation", "Software", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Developer IDE required for backend API scripting and diagnostics.", "VS Code", "1.87.2", None, 10, True),
        ("Install Figma Desktop App for UX Design Team", "Software Installation", "Software", TicketPriority.MEDIUM, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "UI wireframing and interactive design prototyping workflows.", "Figma", "116.15.4", None, 8, True),
        ("Install Tableau Desktop for Financial Analytics", "Software Installation", "Software", TicketPriority.HIGH, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Quarterly corporate board revenue forecasting and modeling.", "Tableau Desktop", "2024.1", None, 3, False),
        ("Install Postman Enterprise for API Integration Testing", "Software Installation", "Software", TicketPriority.MEDIUM, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.SERVICE_DESK, "Validating internal REST API endpoints and OAuth security tokens.", "Postman Pro", "10.24.0", None, 1, False),
        ("Install IntelliJ IDEA Ultimate for Backend Engineers", "Software Installation", "Software", TicketPriority.HIGH, ServiceRequestStatus.APPROVED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Java Spring microservices codebase maintenance and profiling.", "IntelliJ IDEA", "2024.1", None, 2, False),
        ("Install PyCharm Professional for Data Science Models", "Software Installation", "Software", TicketPriority.MEDIUM, ServiceRequestStatus.ASSIGNED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Machine learning pipeline development and regression analysis.", "PyCharm Pro", "2023.3", None, 2, False),
        ("Install Wireshark Network Packet Analyzer", "Software Installation", "Software", TicketPriority.HIGH, ServiceRequestStatus.REJECTED, ServiceRequestApprovalStatus.REJECTED, SupportTeam.SECURITY, "Need packet inspection utility for network latency testing.", "Wireshark", "4.2.3", None, 6, False),
        ("Install Zoom Rooms Client for Executive Boardroom", "Software Installation", "Software", TicketPriority.LOW, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.SERVICE_DESK, "Executive weekly all-hands meeting broadcast.", "Zoom", "5.17.11", None, 11, True),

        # Application Access (8)
        ("Salesforce CRM Access for Enterprise Account Executives", "Application Access", "Access", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Promoted to Senior Account Executive; require pipeline opportunity edit rights.", None, None, "Salesforce CRM", 14, True),
        ("SAP ERP Financial Accounting Module Access", "Application Access", "Access", TicketPriority.CRITICAL, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Month-end general ledger reconciliations and journal voucher approval.", None, None, "SAP ERP", 9, True),
        ("Jira Software Administrator Access for Project Tracking", "Application Access", "Access", TicketPriority.MEDIUM, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Configuring scrum workflows and custom ticket issue types.", None, None, "Jira Software", 4, False),
        ("AWS Production Cloud Console Read-Only Audit Access", "Application Access", "Access", TicketPriority.HIGH, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.SECURITY, "Quarterly SOC2 compliance evidence collection and IAM review.", None, None, "AWS Console", 1, False),
        ("GitHub Enterprise Organization Member Permissions", "Application Access", "Access", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Need access to internal repos and pull request reviews.", None, None, "GitHub Enterprise", 13, True),
        ("Snowflake Data Warehouse Analyst Role Access", "Application Access", "Access", TicketPriority.HIGH, ServiceRequestStatus.APPROVED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Running business intelligence queries on data lake for Q3 KPIs.", None, None, "Snowflake", 2, False),
        ("Workday HR Manager Portal Access for Annual Reviews", "Application Access", "Access", TicketPriority.MEDIUM, ServiceRequestStatus.ASSIGNED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Completing direct report annual compensation and performance evaluations.", None, None, "Workday", 2, False),
        ("Datadog Infrastructure Monitoring Dashboard Access", "Application Access", "Access", TicketPriority.LOW, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Observing real-time latency and error rates for production web servers.", None, None, "Datadog", 7, True),

        # Microsoft 365 License (6)
        ("Microsoft 365 E5 Security & Compliance License Upgrade", "Microsoft 365 License", "Microsoft 365", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.M365_SUPPORT, "Required for Power BI integration, advanced threat protection, and eDiscovery.", None, None, "Microsoft 365 E5", 13, True),
        ("Copilot for Microsoft 365 AI Productivity Add-on", "Microsoft 365 License", "Microsoft 365", TicketPriority.MEDIUM, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.M365_SUPPORT, "Executive pilot participant for generative AI drafting in Word and Excel.", None, None, "Copilot for M365", 1, False),
        ("Microsoft Visio Plan 2 Architecture Diagramming License", "Microsoft 365 License", "Microsoft 365", TicketPriority.LOW, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.M365_SUPPORT, "Creating systems architecture diagrams and process flow schematics.", None, None, "Visio Plan 2", 6, True),
        ("Microsoft Project Plan 3 Enterprise Portfolio Management", "Microsoft 365 License", "Microsoft 365", TicketPriority.MEDIUM, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.M365_SUPPORT, "Managing multi-team resource allocation schedules and milestones.", None, None, "Project Plan 3", 3, False),
        ("Power BI Pro Workspace Publishing License", "Microsoft 365 License", "Microsoft 365", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.M365_SUPPORT, "Publishing operational dashboards to executive workspaces.", None, None, "Power BI Pro", 12, True),
        ("Exchange Online Archiving Expansion for Legal Hold", "Microsoft 365 License", "Microsoft 365", TicketPriority.HIGH, ServiceRequestStatus.APPROVED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.M365_SUPPORT, "Expanding mailbox capacity beyond 100GB for compliance retention.", None, None, "Exchange Archiving", 2, False),

        # VPN Access (5)
        ("Production VPC Secure Remote VPN Gateway Access", "VPN Access", "Network", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.NETWORK_SUPPORT, "Deploying hotfix updates to production servers outside of office network.", None, None, "Production VPN", 14, True),
        ("Offshore Development Partner SSL VPN Tunnel", "VPN Access", "Network", TicketPriority.HIGH, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.NETWORK_SUPPORT, "Contractor team access to non-production staging environments.", None, None, "Contractor VPN", 8, True),
        ("Remote Emergency Operations VPN Profile for On-Call Engineer", "VPN Access", "Network", TicketPriority.CRITICAL, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.NETWORK_SUPPORT, "Primary on-call incident responder required for 24/7 incident coverage.", None, None, "Emergency VPN", 2, False),
        ("Global Partner Secure Gateway Profile", "VPN Access", "Network", TicketPriority.MEDIUM, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.NETWORK_SUPPORT, "Connecting joint venture audit partners to shared reporting repository.", None, None, "Partner VPN", 1, False),
        ("Disaster Recovery Site Secondary VPN Gateway Configuration", "VPN Access", "Network", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.NETWORK_SUPPORT, "Annual business continuity and failover DR site testing.", None, None, "DR VPN", 11, True),

        # New Laptop (5)
        ("New MacBook Pro 16 M3 Max for Mobile iOS Lead", "New Laptop", "Hardware", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "High performance compute needed for iOS Swift compilation and simulator runs.", None, None, "MacBook Pro 16", 14, True),
        ("Lenovo ThinkPad P16s High-Performance Mobile Workstation", "New Laptop", "Hardware", TicketPriority.HIGH, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Senior Data Analyst running intensive local PostgreSQL query simulations.", None, None, "ThinkPad P16s", 7, True),
        ("Dell XPS 15 Corporate Ultrabook for Regional Sales Director", "New Laptop", "Hardware", TicketPriority.MEDIUM, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Replacing 4-year-old laptop out of warranty for frequent client visits.", None, None, "Dell XPS 15", 3, False),
        ("Standard Engineering Laptop for New Graduate Hire", "New Laptop", "Hardware", TicketPriority.MEDIUM, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.HARDWARE_SUPPORT, "Onboarding engineering graduate starting next month in DevOps.", None, None, "Standard Laptop", 1, False),
        ("Lightweight MacBook Air 15 M3 for People Operations", "New Laptop", "Hardware", TicketPriority.LOW, ServiceRequestStatus.APPROVED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Remote People Operations business partner requiring portable device.", None, None, "MacBook Air 15", 2, False),

        # Hardware Replacement (4)
        ("Dual 4K USB-C Monitor Display Setup for Financial Analysts", "Hardware Replacement", "Hardware", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Current monitor flickering intermittently; dual screen needed for financial sheets.", None, None, "Dual 4K Monitors", 12, True),
        ("Ergonomic Split Keyboard & Vertical Mouse Occupational Request", "Hardware Replacement", "Hardware", TicketPriority.MEDIUM, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Occupational health ergonomic assessment recommendation for carpal tunnel.", None, None, "Ergonomic Peripherals", 8, True),
        ("Universal Thunderbolt 4 Docking Station Replacement", "Hardware Replacement", "Hardware", TicketPriority.MEDIUM, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED, SupportTeam.HARDWARE_SUPPORT, "Docking station Ethernet port unresponsive after power surge.", None, None, "Thunderbolt Dock", 3, False),
        ("High-Capacity Laptop Battery Replacement for Dell Latitude", "Hardware Replacement", "Hardware", TicketPriority.HIGH, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.HARDWARE_SUPPORT, "Battery health degraded to 38%; unit will not hold charge beyond 20 minutes.", None, None, "Laptop Battery", 1, False),

        # Password Reset (4)
        ("Executive Active Directory Account Unlock & Password Reset", "Password Reset", "Account", TicketPriority.CRITICAL, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.NONE, SupportTeam.SERVICE_DESK, "Account locked out following multiple password attempts from secondary tablet.", None, None, "Active Directory", 13, True),
        ("Self-Service Portal Authenticator App Re-registration", "Password Reset", "Account", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.NONE, SupportTeam.SERVICE_DESK, "Replaced personal phone; need MFA registration QR code regenerated.", None, None, "MFA Reset", 9, True),
        ("SAP ERP Database Master Password Synchronization", "Password Reset", "Account", TicketPriority.MEDIUM, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.NONE, SupportTeam.SERVICE_DESK, "ERP password out of sync with domain single sign-on token.", None, None, "SAP Login", 5, True),
        ("VPN Client Authentication Token Reseed Assistance", "Password Reset", "Account", TicketPriority.HIGH, ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.NONE, SupportTeam.SERVICE_DESK, "Hardware token code rejected during remote connection handshake.", None, None, "VPN Token", 1, False),

        # Google Workspace Access (3)
        ("Google Workspace Enterprise Shared Drive Creation for Marketing", "Google Workspace Access", "Google Workspace", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.GOOGLE_WORKSPACE_SUPPORT, "Central repository for Q3 product campaign assets and 4K video footage.", None, None, "Google Shared Drive", 10, True),
        ("Google Drive 2TB Cloud Storage Quota Increase", "Google Workspace Access", "Google Workspace", TicketPriority.LOW, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.GOOGLE_WORKSPACE_SUPPORT, "Video editing raw materials exceeding baseline 100GB corporate limit.", None, None, "Google Drive 2TB", 6, True),
        ("Google Meet Hardware Room License for Dublin Office", "Google Workspace Access", "Google Workspace", TicketPriority.MEDIUM, ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING, SupportTeam.GOOGLE_WORKSPACE_SUPPORT, "Equipping Dublin Conference Room B with touch controller and camera license.", None, None, "Meet Room License", 1, False),

        # Mobile Device Request (3)
        ("Apple iPhone 15 Pro Corporate Enrollment for Sales Director", "Mobile Device Request", "Mobile", TicketPriority.HIGH, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Director traveling internationally requires secure corporate phone with eSIM.", None, None, "iPhone 15 Pro", 12, True),
        ("Samsung Galaxy S24 Ultra Android Enterprise Field Testing", "Mobile Device Request", "Mobile", TicketPriority.MEDIUM, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Testing internal warehouse scanning APK compatibility on Android 14.", None, None, "Galaxy S24 Ultra", 7, True),
        ("Apple iPad Pro 12.9 Cellular Tablet for Quality Inspectors", "Mobile Device Request", "Mobile", TicketPriority.LOW, ServiceRequestStatus.ASSIGNED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.ENDPOINT_SUPPORT, "Site inspection checklist auditing on factory manufacturing floor.", None, None, "iPad Pro 12.9", 2, False),

        # Shared Folder & Mailbox Access (2)
        ("Finance Shared Mailbox Send-As & Full Access Rights", "Shared Mailbox Access", "Microsoft 365", TicketPriority.MEDIUM, ServiceRequestStatus.CLOSED, ServiceRequestApprovalStatus.NONE, SupportTeam.M365_SUPPORT, "Need authorization to process vendor accounts payable inquiries from ap@company.com.", None, None, "ap@company.com", 14, True),
        ("Legal Department Confidential Contract Repository Shared Drive", "Shared Folder Access", "Access", TicketPriority.HIGH, ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED, SupportTeam.APPLICATION_SUPPORT, "Corporate governance associate requires read/write access to NDAs and contracts.", None, None, "Legal Confidential", 8, True),
    ]

    base_time = get_utc_now()
    all_seeded_srs: List[ServiceRequest] = []

    for idx, spec in enumerate(raw_sr_specs, start=1):
        (
            title,
            type_name,
            cat,
            pri,
            stat,
            app_stat,
            team,
            justification,
            sw_name,
            sw_ver,
            app_name,
            days_ago,
            is_completed,
        ) = spec

        req_type = sr_types_map[type_name]
        requester = employee_users[idx % len(employee_users)]
        assignee = support_users[idx % len(support_users)] if stat != ServiceRequestStatus.PENDING_APPROVAL else None

        created_dt = base_time - timedelta(days=days_ago, hours=idx % 8, minutes=idx * 7 % 60)
        due_dt = created_dt + timedelta(hours=req_type.default_sla_hours)

        fulfilled_dt = None
        closed_dt = None
        fulfillment_details_str = None
        res_notes = None

        if stat in [ServiceRequestStatus.FULFILLED, ServiceRequestStatus.CLOSED]:
            # Set fulfilled date; 90% within SLA, 10% breached to show realistic charts
            breached = (idx % 11 == 0)
            delay_hours = req_type.default_sla_hours + 4 if breached else max(1, req_type.default_sla_hours - 6)
            fulfilled_dt = created_dt + timedelta(hours=delay_hours)
            if stat == ServiceRequestStatus.CLOSED:
                closed_dt = fulfilled_dt + timedelta(hours=4)

            res_notes = f"Service successfully fulfilled and verified by {assignee.name if assignee else 'IT Support'}."
            f_data: Dict[str, Any] = {
                "status": "Verified & Complete",
                "completed_by": assignee.name if assignee else "IT Support Staff",
                "completed_at": fulfilled_dt.isoformat(),
            }
            if sw_name:
                f_data["software_name"] = sw_name
                f_data["software_version"] = sw_ver or "Latest Stable"
                f_data["license_key"] = f"LIC-ENT-{2026000 + idx}"
            if app_name:
                f_data["application"] = app_name
                f_data["access_level"] = "Standard Role / Authorized"
            if cat == "Hardware":
                f_data["asset_tag"] = f"AST-2026-{100 + idx:04d}"
                f_data["serial_number"] = f"SN{9823412 + idx}"
            fulfillment_details_str = json.dumps(f_data)

        # Asset link for hardware requests
        linked_asset = hardware_assets[idx % len(hardware_assets)] if cat == "Hardware" else None

        sr = ServiceRequest(
            request_number=f"SR-2026-{idx:06d}",
            title=title,
            description=f"{title}. {justification}",
            category=cat,
            priority=pri,
            status=stat,
            request_type_id=req_type.id,
            requester_id=requester.id,
            approval_required=req_type.approval_required,
            approval_status=app_stat,
            approver_id=admin_user.id if app_stat in [ServiceRequestApprovalStatus.APPROVED, ServiceRequestApprovalStatus.REJECTED] else None,
            approval_reason="Approved under standard IT operational delegation of authority." if app_stat == ServiceRequestApprovalStatus.APPROVED else ("Outside approved IT budget threshold." if app_stat == ServiceRequestApprovalStatus.REJECTED else None),
            approved_at=created_dt + timedelta(hours=1) if app_stat in [ServiceRequestApprovalStatus.APPROVED, ServiceRequestApprovalStatus.REJECTED] else None,
            assigned_to=assignee.id if assignee else None,
            assigned_team=team,
            business_justification=justification,
            required_date=(created_dt + timedelta(days=2)).date(),
            asset_id=linked_asset.id if linked_asset else None,
            application_name=app_name,
            access_level="Standard User" if app_name else None,
            software_name=sw_name,
            software_version=sw_ver,
            device_type=linked_asset.device_type if linked_asset else None,
            fulfillment_details=fulfillment_details_str,
            resolution_notes=res_notes,
            due_at=due_dt,
            created_at=created_dt,
            updated_at=closed_dt or fulfilled_dt or created_dt,
            fulfilled_at=fulfilled_dt,
            closed_at=closed_dt,
        )
        db.add(sr)
        db.flush()

        # Seed Timeline
        record_sr_timeline(
            db,
            request_id=sr.id,
            event_type="Submitted",
            title="Service Request Submitted",
            actor_id=requester.id,
            description=f"Submitted by {requester.name} ({requester.department}).",
        )

        if sr.approval_required:
            if sr.approval_status == ServiceRequestApprovalStatus.APPROVED:
                record_sr_timeline(
                    db,
                    request_id=sr.id,
                    event_type="Approved",
                    title="Management Approved",
                    actor_id=admin_user.id,
                    description=f"Approved by IT Operations Manager {admin_user.name}.",
                )
            elif sr.approval_status == ServiceRequestApprovalStatus.REJECTED:
                record_sr_timeline(
                    db,
                    request_id=sr.id,
                    event_type="Rejected",
                    title="Request Rejected",
                    actor_id=admin_user.id,
                    description=sr.approval_reason or "Request rejected.",
                )

        if assignee:
            record_sr_timeline(
                db,
                request_id=sr.id,
                event_type="Assigned",
                title=f"Assigned to {team}",
                actor_id=assignee.id,
                description=f"Assigned to {assignee.name}.",
            )

        if stat in [ServiceRequestStatus.FULFILLED, ServiceRequestStatus.CLOSED]:
            record_sr_timeline(
                db,
                request_id=sr.id,
                event_type="Fulfilled",
                title="Service Request Fulfilled",
                actor_id=assignee.id if assignee else admin_user.id,
                description=res_notes or "Technical service provisioned.",
            )

        if stat == ServiceRequestStatus.CLOSED:
            record_sr_timeline(
                db,
                request_id=sr.id,
                event_type="Confirmed",
                title="User Confirmed Completion",
                actor_id=requester.id,
                description="Requester confirmed successful receipt and access.",
            )

        # Seed Comments
        db.add(
            ServiceRequestComment(
                request_id=sr.id,
                user_id=requester.id,
                comment=f"Please note this is for the {requester.department} departmental workflow.",
                is_internal=False,
                created_at=created_dt + timedelta(minutes=15),
            )
        )
        if assignee:
            db.add(
                ServiceRequestComment(
                    request_id=sr.id,
                    user_id=assignee.id,
                    comment="Acknowledged. Verifying catalog entitlement and software licensing.",
                    is_internal=False,
                    created_at=created_dt + timedelta(hours=1),
                )
            )
            db.add(
                ServiceRequestComment(
                    request_id=sr.id,
                    user_id=assignee.id,
                    comment="[Internal Support Note] Checked available pool; valid seat assigned.",
                    is_internal=True,
                    created_at=created_dt + timedelta(hours=2),
                )
            )

        all_seeded_srs.append(sr)

    db.commit()
    print(f"Database seeding completed successfully! Total 100+ tickets, 16 users, 25 assets, 15 request types, 52 service requests, 8 onboardings.")

