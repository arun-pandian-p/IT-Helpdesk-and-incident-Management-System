package com.servicedesk.service;

import com.servicedesk.model.*;
import com.servicedesk.model.Constants.*;
import com.servicedesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Order(2)
public class SeedService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final TicketRepository ticketRepository;
    private final ServiceRequestRepository srRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final OnboardingRepository onboardingRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedService(UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       AssetRepository assetRepository,
                       TicketRepository ticketRepository,
                       ServiceRequestRepository srRepository,
                       KnowledgeRepository knowledgeRepository,
                       OnboardingRepository onboardingRepository,
                       AccessRequestRepository accessRequestRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.ticketRepository = ticketRepository;
        this.srRepository = srRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.onboardingRepository = onboardingRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedDatabase();
    }

    @Transactional
    public synchronized void seedDatabase() {
        if (userRepository.findByEmail("admin@example.com").isPresent()) {
            log.info("Database already contains seed data.");
            return;
        }

        log.info("Starting enterprise demo dataset initialization...");
        String passwordHash = passwordEncoder.encode("Password123!");
        Instant now = Instant.now();

        // ---------------------------------------------------------------------
        // 1. Users (1 Admin, 5 Support, 10 Employees)
        // ---------------------------------------------------------------------
        User admin = new User(UUID.randomUUID().toString(), "David Miller", "admin@example.com",
                passwordHash, UserRole.ADMIN, "IT Infrastructure", "IT Director & Service Desk Lead",
                "+1-555-0100", "Headquarters - Floor 4", null, true, now, now);
        userRepository.save(admin);

        String[][] supportData = {
                {"Arun Pandian", "support@example.com", "Tier 2 Lead Support Specialist", "+1-555-0101"},
                {"Priya Patel", "priya.patel@example.com", "Senior Systems Support Engineer", "+1-555-0102"},
                {"Marcus Vance", "marcus.vance@example.com", "Network & Security Support Analyst", "+1-555-0103"},
                {"Elena Rostova", "elena.rostova@example.com", "Workplace Technology & Apple Specialist", "+1-555-0104"},
                {"David Kim", "david.kim@example.com", "Service Desk Technician", "+1-555-0105"}
        };
        List<User> supportUsers = new ArrayList<>();
        for (String[] s : supportData) {
            User u = new User(UUID.randomUUID().toString(), s[0], s[1], passwordHash,
                    UserRole.SUPPORT, "IT Support Services", s[2], s[3],
                    "Headquarters - Service Desk", admin.getId(), true, now, now);
            userRepository.save(u);
            supportUsers.add(u);
        }

        String[][] employeeData = {
                {"Jordan Reed", "employee@example.com", "Finance", "Senior Financial Analyst"},
                {"Sarah Chen", "sarah.chen@example.com", "HR", "HR Operations Manager"},
                {"Michael Scott", "michael.scott@example.com", "Sales", "Regional Sales Director"},
                {"Lisa Ray", "lisa.ray@example.com", "Engineering", "Senior Software Engineer"},
                {"Robert Taylor", "robert.taylor@example.com", "Legal", "Corporate Legal Counsel"},
                {"Emily Watson", "emily.watson@example.com", "Marketing", "Content Strategy Lead"},
                {"James Wilson", "james.wilson@example.com", "Engineering", "DevOps Engineer"},
                {"Amanda Clark", "amanda.clark@example.com", "Finance", "Payroll Specialist"},
                {"Kevin Miller", "kevin.miller@example.com", "Product", "Senior Product Manager"},
                {"Rachel Green", "rachel.green@example.com", "Customer Success", "Client Engagement Lead"}
        };
        List<User> employeeUsers = new ArrayList<>();
        for (String[] e : employeeData) {
            User u = new User(UUID.randomUUID().toString(), e[0], e[1], passwordHash,
                    UserRole.EMPLOYEE, e[2], e[3], "+1-555-0199", "Headquarters", null, true, now, now);
            userRepository.save(u);
            employeeUsers.add(u);
        }

        // ---------------------------------------------------------------------
        // 2. Categories
        // ---------------------------------------------------------------------
        String[][] catData = {
                {"Hardware & Workstations", "Laptops, monitors, keyboards, mice, docking stations"},
                {"Software & Applications", "Operating systems, office suites, engineering tools, licensing"},
                {"Network & Connectivity", "Wi-Fi, VPN access, Ethernet drops, DNS issues"},
                {"Access & Identity", "Password resets, MFA tokens, Single Sign-On (SSO)"},
                {"Microsoft 365 & Email", "Outlook, Teams, SharePoint, OneDrive syncing issues"},
                {"Mobile & Remote Work", "Corporate mobile devices, remote desktop, mobile management"}
        };
        List<TicketCategory> categories = new ArrayList<>();
        for (String[] c : catData) {
            TicketCategory cat = new TicketCategory(UUID.randomUUID().toString(), c[0], c[1], true, now);
            categoryRepository.save(cat);
            categories.add(cat);
        }

        // ---------------------------------------------------------------------
        // 3. Assets
        // ---------------------------------------------------------------------
        String[][] assetData = {
                {"AST-2024-001", "Laptop", "Apple", "MacBook Pro 16\" M3", "SN-APL-9281928", "In Use", employeeUsers.get(0).getId()},
                {"AST-2024-002", "Laptop", "Lenovo", "ThinkPad X1 Carbon Gen 11", "SN-LNV-4819201", "In Use", employeeUsers.get(1).getId()},
                {"AST-2024-003", "Laptop", "Dell", "XPS 15 9530", "SN-DEL-1092837", "In Use", employeeUsers.get(2).getId()},
                {"AST-2024-004", "Monitor", "Dell", "UltraSharp 27\" 4K U2723QE", "SN-DEL-8827361", "In Use", employeeUsers.get(0).getId()},
                {"AST-2024-005", "Printer", "Canon", "imageRUNNER ADVANCE DX C3830i", "SN-CAN-3847291", "In Use", null},
                {"AST-2024-006", "Laptop", "Apple", "MacBook Air 15\" M2", "SN-APL-1192834", "In Stock", null}
        };
        List<Asset> assets = new ArrayList<>();
        for (String[] a : assetData) {
            Asset asset = new Asset();
            asset.setId(UUID.randomUUID().toString());
            asset.setAssetTag(a[0]);
            asset.setDeviceType(a[1]);
            asset.setManufacturer(a[2]);
            asset.setModel(a[3]);
            asset.setSerialNumber(a[4]);
            asset.setStatus(a[5]);
            asset.setAssignedUserId(a[6]);
            asset.setPurchaseDate("2024-01-15");
            asset.setWarrantyExpiry("2027-01-15");
            asset.setNotes("Enterprise standard hardware assignment");
            asset.setCreatedAt(now);
            asset.setUpdatedAt(now);
            assetRepository.save(asset);
            assets.add(asset);
        }

        // ---------------------------------------------------------------------
        // 4. Service Request Types (Catalog)
        // ---------------------------------------------------------------------
        Object[][] typeData = {
                {"New Laptop", "Request standard or high-performance corporate laptop", "Hardware", "Medium", true, 48},
                {"Microsoft 365 License", "License allocation for M365 Business Premium or Copilot", "Microsoft 365", "High", true, 24},
                {"Password Reset", "Immediate password synchronization or account unlock", "Account", "Critical", false, 4},
                {"Software Installation", "Request installation of specialized developer or design software", "Software", "Medium", false, 24},
                {"VPN Access", "Provision GlobalProtect or WireGuard remote access profile", "Network", "High", true, 24},
                {"MFA Reset", "Reset authentication token or register new physical device", "Access", "Critical", false, 4},
                {"Monitor Request", "Request secondary 4K or ultra-wide display", "Hardware", "Low", true, 72},
                {"Docking Station", "Request USB-C / Thunderbolt docking hub", "Hardware", "Low", false, 48},
                {"Keyboard & Mouse", "Request ergonomic input devices", "Hardware", "Low", false, 48},
                {"Shared Drive Access", "Permission grant for departmental SharePoint/Drive", "Access", "Medium", true, 24},
                {"AWS Cloud Access", "IAM credentials or console permissions for AWS workload", "Access", "High", true, 24},
                {"GitHub Organization Access", "Invite developer to engineering organization & repositories", "Access", "Medium", true, 24},
                {"Mobile Phone Setup", "Corporate eSIM or MDM setup on enterprise phone", "Mobile", "Medium", false, 24},
                {"Home Office Setup", "Standard remote worker peripherals and power equipment", "Hardware", "Low", true, 72},
                {"Generic IT Support", "General request not covered by specific catalog item", "General IT", "Medium", false, 24}
        };
        List<ServiceRequestType> srTypes = new ArrayList<>();
        for (Object[] t : typeData) {
            ServiceRequestType srt = new ServiceRequestType();
            srt.setId(UUID.randomUUID().toString());
            srt.setName((String) t[0]);
            srt.setDescription((String) t[1]);
            srt.setCategory((String) t[2]);
            srt.setDefaultPriority((String) t[3]);
            srt.setApprovalRequired((Boolean) t[4]);
            srt.setDefaultSlaHours((Integer) t[5]);
            srt.setActive(true);
            srt.setCreatedAt(now);
            srt.setUpdatedAt(now);
            srRepository.saveType(srt);
            srTypes.add(srt);
        }

        // ---------------------------------------------------------------------
        // 5. Knowledge Articles
        // ---------------------------------------------------------------------
        String[][] kbData = {
                {"macOS Sequoia Wi-Fi & DNS Connectivity Troubleshooting", "Network", "macOS",
                        "## Steps to resolve persistent Wi-Fi dropouts on macOS\n\n" +
                        "1. Navigate to **System Settings > Network > Wi-Fi**.\n" +
                        "2. Click **Details** next to company SSID.\n" +
                        "3. Select **DNS** tab and add company internal resolvers: `10.0.0.2` and `10.0.0.3`.\n" +
                        "4. Run `sudo dscacheutil -flushcache; sudo killall -HUP mDNSResponder` in Terminal.\n",
                        "wifi, macos, dns, network"},
                {"GlobalProtect VPN Remote Access Guide", "Access", "All",
                        "## How to connect to corporate VPN securely\n\n" +
                        "1. Launch GlobalProtect from taskbar/menu bar.\n" +
                        "2. Enter portal address: `vpn.company.internal`.\n" +
                        "3. Complete Microsoft Authenticator push notification.\n",
                        "vpn, globalprotect, remote, mfa"},
                {"Microsoft Teams Cache Clear & Audio Fix", "Software", "Windows",
                        "## Fix microphone muting or unresponsive Teams application\n\n" +
                        "1. Quit Teams completely via Task Manager.\n" +
                        "2. Press Win+R, enter `%appdata%\\Microsoft\\Teams`.\n" +
                        "3. Delete cache folders and restart Teams.\n",
                        "teams, cache, microsoft365, audio"},
                {"Canon Copier Sensor & Tray 2 Obstruction Recovery", "Hardware", "All",
                        "## Clearing Paper Jam on Central Multi-Function Printer\n\n" +
                        "1. Open front and right maintenance doors.\n" +
                        "2. Check rollers A1, B2, and feed tray sensor for paper fragments.\n" +
                        "3. Reseat tray firmly until click sound.\n",
                        "printer, canon, jam, hardware"},
                {"Windows 11 BitLocker Recovery Workflow", "Security", "Windows",
                        "## How to recover BitLocker encrypted workstation\n\n" +
                        "1. IT administrators can retrieve recovery keys from Active Directory or Intune portal.\n" +
                        "2. Enter 48-digit numeric recovery key at boot prompt.\n" +
                        "3. Run `manage-bde -status` to verify partition health.\n",
                        "bitlocker, windows, security, recovery"}
        };
        for (String[] kb : kbData) {
            KnowledgeArticle a = new KnowledgeArticle();
            a.setId(UUID.randomUUID().toString());
            a.setTitle(kb[0]);
            a.setCategory(kb[1]);
            a.setOsTarget(kb[2]);
            a.setContent(kb[3]);
            a.setTags(kb[4]);
            a.setViewCount(42);
            a.setHelpfulCount(18);
            a.setNotHelpfulCount(1);
            a.setCreatedBy(supportUsers.get(0).getId());
            a.setCreatedAt(now);
            a.setUpdatedAt(now);
            knowledgeRepository.save(a);
        }

        // ---------------------------------------------------------------------
        // 6. Seed Tickets (Incidents)
        // ---------------------------------------------------------------------
        String[][] sampleIncidents = {
                {"Unable to connect to company VPN from home office",
                 "VPN client drops connection every 5 minutes during remote session.",
                 "Network & Connectivity", "Individual", "High", "Open", null, null},
                {"Dell 4K monitor flickering green lines across display",
                 "DisplayPort connection shows horizontal artifacts after 30 minutes of use.",
                 "Hardware & Workstations", "Individual", "Low", "In Progress", supportUsers.get(0).getId(), assets.get(3).getId()},
                {"Microsoft Outlook crash upon opening shared calendar invitation",
                 "Outlook 365 produces error 0x8004010F when accessing executive calendar.",
                 "Microsoft 365 & Email", "Team", "Medium", "Waiting for User", supportUsers.get(1).getId(), null},
                {"Production accounting server shared folder inaccessible",
                 "All finance team members are getting Access Denied error on shared drive.",
                 "Access & Identity", "Department", "Critical", "In Progress", supportUsers.get(2).getId(), null},
                {"MacBook Pro battery draining in less than 2 hours",
                 "Battery health diagnostics report service recommended.",
                 "Hardware & Workstations", "Individual", "Medium", "Resolved", supportUsers.get(3).getId(), assets.get(0).getId()},
                {"Canon floor 3 copier paper jam sensor stuck",
                 "Tray 2 sensor reporting paper jam even after completely emptying tray.",
                 "Hardware & Workstations", "Department", "Medium", "Closed", supportUsers.get(4).getId(), assets.get(4).getId()}
        };

        int ticketCounter = 1;
        for (int i = 0; i < sampleIncidents.length; i++) {
            String[] inc = sampleIncidents[i];
            TicketCategory cat = categories.stream().filter(c -> c.getName().equals(inc[2])).findFirst().orElse(categories.get(0));
            User creator = employeeUsers.get(i % employeeUsers.size());

            Ticket t = new Ticket();
            t.setId(UUID.randomUUID().toString());
            t.setTicketNumber(String.format("INC-2026-%06d", ticketCounter++));
            t.setTitle(inc[0]);
            t.setDescription(inc[1]);
            t.setCategoryId(cat.getId());
            t.setImpact(inc[3]);
            t.setUrgency(inc[4]);
            t.setPriority(inc[4].equals("Critical") ? TicketPriority.CRITICAL : (inc[4].equals("High") ? TicketPriority.HIGH : TicketPriority.MEDIUM));
            t.setStatus(inc[5]);
            t.setCreatedBy(creator.getId());
            t.setAssignedTo(inc[6]);
            t.setAssetId(inc[7]);
            t.setDeviceType("Laptop");
            t.setOsName("Windows 11");
            t.setLocation("Headquarters");

            Instant ticketCreated = now.minus(3, ChronoUnit.DAYS).plus(i * 4, ChronoUnit.HOURS);
            t.setCreatedAt(ticketCreated);
            t.setUpdatedAt(ticketCreated);
            t.setDueAt(ticketCreated.plus(24, ChronoUnit.HOURS));

            if ("Resolved".equals(inc[5])) {
                t.setFirstResponseAt(ticketCreated.plus(1, ChronoUnit.HOURS));
                t.setResolvedAt(ticketCreated.plus(6, ChronoUnit.HOURS));
                t.setResolutionNotes("Replaced faulty battery cell and verified charging cycle.");
            } else if ("Closed".equals(inc[5])) {
                t.setFirstResponseAt(ticketCreated.plus(30, ChronoUnit.MINUTES));
                t.setResolvedAt(ticketCreated.plus(3, ChronoUnit.HOURS));
                t.setClosedAt(ticketCreated.plus(24, ChronoUnit.HOURS));
                t.setResolutionNotes("Cleaned optical sensor and recalibrated tray 2 roller.");
            }

            ticketRepository.save(t);

            // Add sample comment
            TicketComment comment = new TicketComment();
            comment.setId(UUID.randomUUID().toString());
            comment.setTicketId(t.getId());
            comment.setUserId(creator.getId());
            comment.setComment("Initial report filed. Please assist as soon as possible.");
            comment.setInternal(false);
            comment.setCreatedAt(ticketCreated);
            ticketRepository.saveComment(comment);
        }

        // ---------------------------------------------------------------------
        // 7. Seed Service Requests
        // ---------------------------------------------------------------------
        String[][] srData = {
                {"Microsoft 365 Copilot License Request", "Requesting Copilot AI license for financial modeling workflows.",
                 "Microsoft 365 License", "High", ServiceRequestStatus.PENDING_APPROVAL, ServiceRequestApprovalStatus.PENDING},
                {"Developer MacBook Pro M3 Provisioning", "New engineering hire hardware requirement.",
                 "New Laptop", "Medium", ServiceRequestStatus.APPROVED, ServiceRequestApprovalStatus.APPROVED},
                {"Remote Production VPN Profile", "Need production cluster access for on-call rotation.",
                 "VPN Access", "High", ServiceRequestStatus.IN_PROGRESS, ServiceRequestApprovalStatus.APPROVED},
                {"Dual 4K Monitor Ergonomic Setup", "Requesting dual monitor arms and monitors for data desk.",
                 "Monitor Request", "Low", ServiceRequestStatus.FULFILLED, ServiceRequestApprovalStatus.APPROVED}
        };

        int srCounter = 1;
        for (int i = 0; i < srData.length; i++) {
            String[] s = srData[i];
            ServiceRequestType type = srTypes.stream().filter(t -> t.getName().equals(s[2])).findFirst().orElse(srTypes.get(0));
            User requester = employeeUsers.get(i % employeeUsers.size());

            ServiceRequest sr = new ServiceRequest();
            sr.setId(UUID.randomUUID().toString());
            sr.setRequestNumber(String.format("SR-2026-%06d", srCounter++));
            sr.setTitle(s[0]);
            sr.setDescription(s[1]);
            sr.setCategory(type.getCategory());
            sr.setPriority(s[3]);
            sr.setStatus(s[4]);
            sr.setRequestTypeId(type.getId());
            sr.setRequesterId(requester.getId());
            sr.setApprovalRequired(type.isApprovalRequired());
            sr.setApprovalStatus(s[5]);
            sr.setBusinessJustification("Authorized departmental workflow requirement.");
            sr.setAssignedTeam(SupportTeam.SERVICE_DESK);
            sr.setAssignedTo(supportUsers.get(0).getId());

            Instant srCreated = now.minus(2, ChronoUnit.DAYS).plus(i * 6, ChronoUnit.HOURS);
            sr.setCreatedAt(srCreated);
            sr.setUpdatedAt(srCreated);
            sr.setDueAt(srCreated.plus(type.getDefaultSlaHours(), ChronoUnit.HOURS));

            if (ServiceRequestApprovalStatus.APPROVED.equals(s[5])) {
                sr.setApproverId(admin.getId());
                sr.setApprovalReason("Approved by department director.");
                sr.setApprovedAt(srCreated.plus(2, ChronoUnit.HOURS));
            }

            if (ServiceRequestStatus.FULFILLED.equals(s[4])) {
                sr.setFulfilledAt(srCreated.plus(12, ChronoUnit.HOURS));
                sr.setResolutionNotes("Hardware configured and delivered to employee desk.");
            }

            srRepository.save(sr);
        }

        // ---------------------------------------------------------------------
        // 8. Seed Onboarding Requests
        // ---------------------------------------------------------------------
        OnboardingRequest onb = new OnboardingRequest();
        onb.setId(UUID.randomUUID().toString());
        onb.setEmployeeName("Alexander Wright");
        onb.setDepartment("Engineering");
        onb.setJobTitle("Cloud Platform Engineer");
        onb.setManagerName("David Miller");
        onb.setStartDate("2026-11-01");
        onb.setLocation("Headquarters");
        onb.setDeviceRequirement("Apple MacBook Pro 16\" M3 Max");
        onb.setSoftwareRequirement("IntelliJ IDEA Ultimate, Docker, AWS CLI, Terraform");
        onb.setAccessRequirement("AWS Dev/Staging account, GitHub Enterprise, Jira");
        onb.setStatus("In Progress");
        onb.setProgressPercent(33);
        onb.setCreatedBy(admin.getId());
        onb.setCreatedAt(now);
        onb.setUpdatedAt(now);
        onboardingRepository.saveRequest(onb);

        String[] onbTasks = {
                "Assign hardware & provision laptop",
                "Create Google Workspace / Microsoft 365 account",
                "Configure VPN & SSO credentials",
                "Provision department SaaS software",
                "Security compliance & phishing orientation",
                "Deliver hardware & workstation setup"
        };
        for (int i = 0; i < onbTasks.length; i++) {
            OnboardingTask t = new OnboardingTask();
            t.setId(UUID.randomUUID().toString());
            t.setRequestId(onb.getId());
            t.setTaskName(onbTasks[i]);
            t.setStatus(i < 2 ? OnboardingTaskStatus.COMPLETED : OnboardingTaskStatus.NOT_STARTED);
            t.setDueDate(onb.getStartDate());
            if (i < 2) t.setCompletedAt(now);
            onboardingRepository.saveTask(t);
        }

        // ---------------------------------------------------------------------
        // 9. Seed Access Request
        // ---------------------------------------------------------------------
        AccessRequest acc = new AccessRequest();
        acc.setId(UUID.randomUUID().toString());
        acc.setRequestType("Password Reset");
        acc.setRequestedSystem("Active Directory / Workday");
        acc.setAccessLevel("User");
        acc.setReason("Account lockout after multiple password attempts.");
        acc.setRequestedBy(employeeUsers.get(0).getId());
        acc.setStatus(AccessRequestStatus.REQUESTED);
        acc.setCreatedAt(now);
        acc.setUpdatedAt(now);
        accessRequestRepository.save(acc);

        log.info("Demo database seeded successfully with users, categories, assets, tickets, articles, and catalog items.");
    }
}
